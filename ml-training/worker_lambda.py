"""
Lambda worker: un mensaje SQS = un tenant; lee eventos S3 Parquet y sube MREC (.bin) a S3.
Handler: worker_lambda.handler
"""
from __future__ import annotations

import datetime
import io
import json
import os
import struct
import sys
from collections import defaultdict
from typing import Any

import boto3

try:
    import pyarrow.parquet as pq
except ImportError:
    pq = None  # type: ignore

# Clave fija; debe coincidir con RecommendationModelLoader (Java) y recommendations_etl.py.
MODEL_S3_KEY_PATTERN = "recommendations/{tenantId}/model.bin"
MREC_MAGIC = 0x4D524543
MREC_VERSION = 4


def aws_region() -> str:
    return os.environ.get("AWS_REGION", os.environ.get("AWS_DEFAULT_REGION", "us-east-1"))


def analytics_events_bucket() -> str:
    return (os.environ.get("ANALYTICS_EVENTS_BUCKET") or "").strip()


def s3_client():
    return boto3.client("s3", region_name=aws_region())


def default_source_day_utc() -> str:
    yesterday = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=1)
    return yesterday.strftime("%Y-%m-%d")


def read_events_from_s3(tenant_id: str, date_str: str) -> tuple[dict[str, int], dict[str, int]]:
    """Returns (item_view_counts, order_item_boosts) from Parquet."""
    bucket = analytics_events_bucket()
    if not bucket or pq is None:
        return {}, {}

    year, month, day = date_str.split("-")
    prefix = f"events/year={year}/month={int(month):02d}/day={int(day):02d}/"
    view_counts: dict[str, int] = defaultdict(int)
    order_boosts: dict[str, int] = defaultdict(int)
    seen_event_ids: set[str] = set()
    s3 = s3_client()

    paginator = s3.get_paginator("list_objects_v2")
    for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
        for obj in page.get("Contents", []):
            key = obj.get("Key", "")
            if not key.endswith(".parquet"):
                continue
            body = s3.get_object(Bucket=bucket, Key=key)["Body"].read()
            table = pq.read_table(io.BytesIO(body))
            data = table.to_pydict()
            n = len(data.get("eventType") or data.get("eventtype") or [])
            event_types = data.get("eventType") or data.get("eventtype") or [None] * n
            tenant_ids = data.get("tenantId") or data.get("tenantid") or [None] * n
            event_ids = data.get("eventId") or data.get("eventid") or [None] * n
            item_ids = data.get("itemId") or data.get("itemid") or [None] * n
            metadata_col = data.get("metadata") or [None] * n

            for i in range(n):
                if tenant_ids[i] != tenant_id:
                    continue
                eid = event_ids[i]
                if eid and eid in seen_event_ids:
                    continue
                if eid:
                    seen_event_ids.add(eid)

                etype = event_types[i]
                if etype == "ITEM_VIEW":
                    iid = item_ids[i]
                    if iid:
                        view_counts[str(iid)] += 1
                elif etype == "ORDER_SUBMITTED":
                    meta = metadata_col[i] if i < len(metadata_col) else None
                    item_ids_str = ""
                    if isinstance(meta, dict):
                        item_ids_str = str(meta.get("itemIds") or meta.get("itemids") or "")
                    for iid in item_ids_str.split(","):
                        iid = iid.strip()
                        if iid:
                            order_boosts[iid] += 2

    return dict(view_counts), dict(order_boosts)


def query_item_views_from_s3(tenant_id: str, date_str: str) -> dict[str, int]:
    views, boosts = read_events_from_s3(tenant_id, date_str)
    if not views and not boosts:
        return {}
    combined: dict[str, int] = defaultdict(int)
    for iid, c in views.items():
        combined[iid] += c
    for iid, boost in boosts.items():
        combined[iid] += boost
    return dict(combined)


def query_item_views(tenant_id: str, date_str: str) -> dict[str, int]:
    counts = query_item_views_from_s3(tenant_id, date_str)
    if not counts:
        raise ValueError(
            f"No hay eventos en S3 para tenant={tenant_id} day={date_str}. "
            "Verifica Firehose y el prefijo events/ en el bucket analytics."
        )
    return counts


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
    counts = query_item_views(tenant_id, date_str)
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
