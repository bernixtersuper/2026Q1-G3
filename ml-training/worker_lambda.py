"""
Lambda worker: un mensaje SQS = un tenant; lee ml_features/ (ML Analytics) y sube MREC (.bin) a S3.
Handler: worker_lambda.handler
"""
from __future__ import annotations

import datetime
import io
import json
import os
import struct
import sys
from typing import Any

import boto3

try:
    import pyarrow.parquet as pq
except ImportError:
    pq = None  # type: ignore

MODEL_S3_KEY_PATTERN = "recommendations/{tenantId}/model.bin"
MREC_MAGIC = 0x4D524543
MREC_VERSION = 4
DEFAULT_ML_PREFIX = "ml_features"


def aws_region() -> str:
    return os.environ.get("AWS_REGION", os.environ.get("AWS_DEFAULT_REGION", "us-east-1"))


def ml_analytics_bucket() -> str:
    return (os.environ.get("ANALYTICS_ML_BUCKET") or "").strip()


def ml_features_prefix() -> str:
    return (os.environ.get("ML_FEATURES_PREFIX") or DEFAULT_ML_PREFIX).strip().strip("/")


def s3_client():
    return boto3.client("s3", region_name=aws_region())


def default_source_day_utc() -> str:
    yesterday = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=1)
    return yesterday.strftime("%Y-%m-%d")


def read_ml_features(tenant_id: str, date_str: str) -> dict[str, int]:
    """Lee ML Analytics: ml_features/day=…/tenant_id=…/ (materializado por Glue enrich)."""
    bucket = ml_analytics_bucket()
    if not bucket or pq is None:
        return {}

    prefix = f"{ml_features_prefix()}/day={date_str}/tenant_id={tenant_id}/"
    counts: dict[str, int] = {}
    s3 = s3_client()

    paginator = s3.get_paginator("list_objects_v2")
    found = False
    for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
        for obj in page.get("Contents", []):
            key = obj.get("Key", "")
            if not key.endswith(".parquet"):
                continue
            found = True
            body = s3.get_object(Bucket=bucket, Key=key)["Body"].read()
            table = pq.read_table(io.BytesIO(body))
            data = table.to_pydict()
            item_ids = data.get("item_id") or data.get("itemId") or []
            scores = data.get("popularity_score") or data.get("popularityScore") or []
            views = data.get("view_count") or data.get("viewCount") or []
            boosts = data.get("order_boost") or data.get("orderBoost") or []

            for i, item_id in enumerate(item_ids):
                if not item_id:
                    continue
                score = None
                if i < len(scores) and scores[i] is not None:
                    score = int(scores[i])
                elif i < len(views) or i < len(boosts):
                    v = int(views[i]) if i < len(views) and views[i] is not None else 0
                    b = int(boosts[i]) if i < len(boosts) and boosts[i] is not None else 0
                    score = v + b
                if score is not None:
                    counts[str(item_id)] = score

    if not found:
        return {}
    return counts


def query_item_views(tenant_id: str, date_str: str) -> dict[str, int]:
    counts = read_ml_features(tenant_id, date_str)
    if not counts:
        raise ValueError(
            f"No hay ml_features para tenant={tenant_id} day={date_str}. "
            f"Verifica Glue enrich y s3://{ml_analytics_bucket()}/{ml_features_prefix()}/"
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
