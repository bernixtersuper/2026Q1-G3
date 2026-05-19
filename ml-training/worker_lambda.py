"""
Lambda worker (un solo fichero): un mensaje SQS = un tenant; agrega DynamoDB y sube MREC (.bin) a S3.
Sin sklearn/joblib: la API Java solo consume el binario MREC (ver RecommendationModelLoader).
Handler: worker_lambda.handler
"""
from __future__ import annotations

import datetime
import json
import os
import struct
import sys
from collections import defaultdict
from typing import Any

import boto3
from botocore.exceptions import ClientError

# Clave fija; debe coincidir con RecommendationModelLoader (Java) y recommendations_etl.py.
MODEL_S3_KEY_PATTERN = "recommendations/{tenantId}/model.bin"
MREC_MAGIC = 0x4D524543
MREC_VERSION = 4


def aws_region() -> str:
    return os.environ.get("AWS_REGION", os.environ.get("AWS_DEFAULT_REGION", "us-east-1"))


def events_table() -> str:
    return os.environ.get("EVENTS_TABLE", "menuqr-events")


def pk_attr() -> str:
    return os.environ.get("DYNAMODB_PK_ATTR", "PK")


def sk_attr() -> str:
    return os.environ.get("DYNAMODB_SK_ATTR", "SK")


def dynamodb_client():
    return boto3.client("dynamodb", region_name=aws_region())


def s3_client():
    return boto3.client("s3", region_name=aws_region())


def default_source_day_utc() -> str:
    yesterday = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=1)
    return yesterday.strftime("%Y-%m-%d")


def query_item_views_for_day(tenant_id: str, date_str: str) -> dict[str, int]:
    pk = f"TENANT#{tenant_id}"
    start_sk = f"EVENT#{date_str}T00:00:00.000Z"
    end_sk = f"EVENT#{date_str}T23:59:59.999Z"
    counts: dict[str, int] = defaultdict(int)
    dynamodb = dynamodb_client()
    paginator = dynamodb.get_paginator("query")
    try:
        for page in paginator.paginate(
            TableName=events_table(),
            KeyConditionExpression="#p = :pk AND #s BETWEEN :a AND :b",
            ExpressionAttributeNames={"#p": pk_attr(), "#s": sk_attr()},
            ExpressionAttributeValues={
                ":pk": {"S": pk},
                ":a": {"S": start_sk},
                ":b": {"S": end_sk},
            },
        ):
            for item in page.get("Items", []):
                if item.get("eventType", {}).get("S") != "ITEM_VIEW":
                    continue
                iid = item.get("itemId", {}).get("S")
                if iid:
                    counts[iid] += 1
    except ClientError as e:
        err = e.response.get("Error", {})
        code = err.get("Code", "")
        msg = err.get("Message", str(e))
        print(
            "ERROR DynamoDB Query. Comprueba región (AWS_REGION), nombre de tabla (EVENTS_TABLE) "
            f"y que la tabla tenga clave HASH+RANGE con atributos '{pk_attr()}' y '{sk_attr()}'.\n"
            "  aws dynamodb describe-table --table-name "
            f"{events_table()} --region {aws_region()}\n"
            f"  ({code}: {msg})",
            file=sys.stderr,
        )
        raise
    return dict(counts)


def _write_utf(buf: bytearray, s: str) -> None:
    b = s.encode("utf-8")
    buf.extend(struct.pack(">I", len(b)))
    buf.extend(b)


def encode_mrec_binary(artifact: dict[str, Any]) -> bytes:
    buf = bytearray()
    buf.extend(struct.pack(">I", MREC_MAGIC))
    buf.extend(struct.pack(">I", int(artifact.get("artifact_version", MREC_VERSION))))
    _write_utf(buf, str(artifact.get("trained_at", "")))
    _write_utf(buf, str(artifact.get("source_day", "")))
    _write_utf(buf, str(artifact.get("tenant_id", "")))
    pop = artifact.get("item_popularity") or {}
    if not isinstance(pop, dict):
        pop = {}
    buf.extend(struct.pack(">I", len(pop)))
    for item_id, count in sorted(pop.items()):
        _write_utf(buf, str(item_id))
        buf.extend(struct.pack(">i", int(count)))
    return bytes(buf)


def build_artifact_for_tenant(tenant_id: str, date_str: str) -> dict[str, Any]:
    counts = query_item_views_for_day(tenant_id, date_str)
    return {
        "artifact_version": MREC_VERSION,
        "trained_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "source_day": date_str,
        "tenant_id": tenant_id,
        "item_popularity": counts,
    }


def recommendations_bucket() -> str:
    return (os.environ.get("RECOMMENDATIONS_MODEL_S3_BUCKET") or "").strip()


def upload_artifact_for_tenant(tenant_id: str, source_day: str) -> tuple[str, int, int]:
    bucket = recommendations_bucket()
    if not bucket:
        raise ValueError("RECOMMENDATIONS_MODEL_S3_BUCKET no está definido")

    artifact = build_artifact_for_tenant(tenant_id, source_day)
    key_bin = MODEL_S3_KEY_PATTERN.replace("{tenantId}", tenant_id)

    mrec_body = encode_mrec_binary(artifact)
    s3_client().put_object(
        Bucket=bucket,
        Key=key_bin,
        Body=mrec_body,
        ContentType="application/octet-stream",
    )

    n_items = len(artifact["item_popularity"])
    return f"s3://{bucket}/{key_bin}", len(mrec_body), n_items


def handler(event: dict[str, Any], context: Any) -> dict[str, Any]:
    failures: list[dict[str, str]] = []
    for record in event.get("Records", []):
        mid = record.get("messageId", "")
        try:
            body = json.loads(record.get("body", "{}"))
            tenant_id = body.get("tenant_id")
            if not tenant_id or not str(tenant_id).strip():
                raise ValueError("Mensaje sin tenant_id")
            source_day = body.get("source_day") or default_source_day_utc()
            source_day = str(source_day).strip()
            uri_bin, nbytes, nitems = upload_artifact_for_tenant(str(tenant_id).strip(), source_day)
            print(f"OK {tenant_id} -> MREC {uri_bin} ({nbytes} B), {nitems} ítems")
        except Exception as e:
            print(f"ERROR messageId={mid}: {e}")
            if mid:
                failures.append({"itemIdentifier": mid})
    return {"batchItemFailures": failures}
