"""
Lambda analytics processor: Kinesis → TransactWrite DynamoDB agregados.
Handler: processor_lambda.handler
"""
from __future__ import annotations

import base64
import json
import os
from datetime import datetime, timezone
from typing import Any

import boto3
from botocore.exceptions import ClientError

AGGREGATE_EVENT_TYPES = frozenset({
    "MENU_VIEW",
    "ITEM_VIEW",
    "SECTION_VIEW",
    "CART_ITEM_ADDED",
})

COUNTER_FIELDS = {
    "MENU_VIEW": ["menuViews"],
    "ITEM_VIEW": ["itemViews"],
    "SECTION_VIEW": ["sectionViews"],
    "CART_ITEM_ADDED": ["cartAdds"],
}

PROC_TTL_SECONDS = 7 * 24 * 3600
HOUR_TTL_SECONDS = 90 * 24 * 3600


def aws_region() -> str:
    return os.environ.get("AWS_REGION", os.environ.get("AWS_DEFAULT_REGION", "us-east-1"))


def analytics_table() -> str:
    return os.environ.get("ANALYTICS_TABLE", "menuqr-analytics")


def dynamodb_client():
    return boto3.client("dynamodb", region_name=aws_region())


def normalize_event_type(event_type: str) -> str:
    if event_type == "FILTER_USED":
        return "FILTER_APPLIED"
    return event_type


def updates_aggregates(event_type: str) -> bool:
    return normalize_event_type(event_type) in AGGREGATE_EVENT_TYPES


def parse_event(payload: dict[str, Any]) -> dict[str, Any]:
    event_type = normalize_event_type(payload.get("eventType", ""))
    return {
        "eventId": payload.get("eventId") or payload.get("id"),
        "eventType": event_type,
        "tenantId": payload.get("tenantId"),
        "sessionId": payload.get("sessionId"),
        "timestamp": payload.get("timestamp"),
        "itemId": payload.get("itemId"),
        "sectionId": payload.get("sectionId"),
        "metadata": payload.get("metadata") or {},
    }


def parse_timestamp(ts: str | None) -> datetime:
    if not ts:
        return datetime.now(timezone.utc)
    if ts.endswith("Z"):
        ts = ts[:-1] + "+00:00"
    return datetime.fromisoformat(ts).astimezone(timezone.utc)


def counter_update(
    pk: str, sk: str, fields: list[str], *, ttl_epoch: int | None = None
) -> dict[str, Any]:
    expr_parts = []
    values: dict[str, dict[str, str]] = {}
    names: dict[str, str] = {}
    for field in fields:
        placeholder = f":v{field}"
        expr_parts.append(f"{field} {placeholder}")
        values[placeholder] = {"N": "1"}
    update_expr = "ADD " + ", ".join(expr_parts)
    if ttl_epoch is not None:
        update_expr += " SET #ttl = :ttl"
        names["#ttl"] = "ttl"
        values[":ttl"] = {"N": str(ttl_epoch)}

    update: dict[str, Any] = {
        "TableName": analytics_table(),
        "Key": {
            "PK": {"S": pk},
            "SK": {"S": sk},
        },
        "UpdateExpression": update_expr,
        "ExpressionAttributeValues": values,
    }
    if names:
        update["ExpressionAttributeNames"] = names
    return {"Update": update}


def build_transaction(event: dict[str, Any]) -> list[dict[str, Any]]:
    pk = f"TENANT#{event['tenantId']}"
    dt = parse_timestamp(event.get("timestamp"))
    day_sk = f"DAY#{dt.strftime('%Y-%m-%d')}"
    hour_sk = f"HOUR#{dt.strftime('%Y-%m-%dT%H')}"
    ttl_proc = int(datetime.now(timezone.utc).timestamp()) + PROC_TTL_SECONDS
    hour_ttl = int(datetime.now(timezone.utc).timestamp()) + HOUR_TTL_SECONDS

    items: list[dict[str, Any]] = [
        {
            "Put": {
                "TableName": analytics_table(),
                "Item": {
                    "PK": {"S": pk},
                    "SK": {"S": f"PROC#{event['eventId']}"},
                    "processedAt": {"S": dt.isoformat()},
                    "ttl": {"N": str(ttl_proc)},
                },
                "ConditionExpression": "attribute_not_exists(SK)",
            }
        }
    ]

    fields = COUNTER_FIELDS.get(event["eventType"], [])
    if fields:
        items.append(counter_update(pk, day_sk, fields))
        items.append(counter_update(pk, hour_sk, fields, ttl_epoch=hour_ttl))

    if event["eventType"] == "ITEM_VIEW" and event.get("itemId"):
        items.append({
            "Update": {
                "TableName": analytics_table(),
                "Key": {
                    "PK": {"S": pk},
                    "SK": {"S": f"ITEM#{event['itemId']}"},
                },
                "UpdateExpression": "ADD views :one SET lastViewedAt = :ts",
                "ExpressionAttributeValues": {
                    ":one": {"N": "1"},
                    ":ts": {"S": dt.isoformat()},
                },
            }
        })

    return items


def is_duplicate_transaction_error(exc: ClientError) -> bool:
    if exc.response.get("Error", {}).get("Code") != "TransactionCanceledException":
        return False
    reasons = exc.response.get("CancellationReasons", [])
    return any(r.get("Code") == "ConditionalCheckFailed" for r in reasons)


def process_event(event: dict[str, Any]) -> None:
    if not updates_aggregates(event["eventType"]):
        return
    if not event.get("eventId") or not event.get("tenantId"):
        return

    transact_items = build_transaction(event)
    try:
        dynamodb_client().transact_write_items(TransactItems=transact_items)
    except ClientError as exc:
        if is_duplicate_transaction_error(exc):
            return
        raise


def handler(event: dict[str, Any], _context: Any) -> dict[str, Any]:
    failures: list[dict[str, str]] = []

    for record in event.get("Records", []):
        seq = record.get("kinesis", {}).get("sequenceNumber", "")
        try:
            raw = base64.b64decode(record["kinesis"]["data"])
            payload = json.loads(raw.decode("utf-8"))
            parsed = parse_event(payload)
            process_event(parsed)
        except Exception:
            failures.append({"itemIdentifier": seq})

    return {"batchItemFailures": failures}
