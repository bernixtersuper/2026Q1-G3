"""
Lambda orquestador (un solo fichero): lista tenants en PostgreSQL y encola un mensaje por tenant en SQS.
Invocación típica: EventBridge cron. Handler: orchestrator_lambda.handler
"""
from __future__ import annotations

import datetime
import json
import os
import urllib.parse
from typing import Any

import boto3
import psycopg2
from botocore.exceptions import ClientError


def aws_region() -> str:
    return os.environ.get("AWS_REGION", os.environ.get("AWS_DEFAULT_REGION", "us-east-1"))


def sqs_client():
    return boto3.client("sqs", region_name=aws_region())


def _load_db_secret() -> dict[str, Any] | None:
    arn = (os.environ.get("DB_SECRET_ARN") or "").strip()
    if not arn:
        return None
    sm = boto3.client("secretsmanager", region_name=aws_region())
    try:
        resp = sm.get_secret_value(SecretId=arn)
    except ClientError as e:
        raise RuntimeError(f"No se pudo leer DB_SECRET_ARN ({arn}): {e}") from e
    raw = resp.get("SecretString")
    if not raw:
        raise RuntimeError(f"El secreto {arn} no tiene SecretString.")
    try:
        data = json.loads(raw)
    except json.JSONDecodeError as e:
        raise RuntimeError(f"DB_SECRET_ARN no contiene JSON válido: {e}") from e
    if not isinstance(data, dict):
        raise RuntimeError("El secreto JSON debe ser un objeto (p. ej. credenciales RDS).")
    return data


def _postgres_connect_kwargs() -> dict[str, Any] | None:
    secret = _load_db_secret()

    db_url = (os.environ.get("DB_URL") or "").strip()
    user = (os.environ.get("DB_USER") or os.environ.get("POSTGRES_USER") or "").strip()
    password = os.environ.get("DB_PASS") or os.environ.get("POSTGRES_PASSWORD") or ""

    if secret:
        su = secret.get("username") or secret.get("user")
        if su is not None and str(su).strip():
            user = str(su).strip()
        if "password" in secret and secret["password"] is not None:
            password = str(secret["password"])

    if db_url.startswith("jdbc:"):
        db_url = db_url[5:]

    host: str | None = None
    port = 5432
    dbname = ""
    sslmode: str | None = None

    if db_url:
        parsed = urllib.parse.urlparse(db_url)
        host = parsed.hostname
        port = parsed.port or 5432
        path = (parsed.path or "").lstrip("/")
        dbname = path.split("?")[0] if path else ""
        q = urllib.parse.parse_qs(parsed.query)
        if "sslmode" in q and q["sslmode"]:
            sslmode = q["sslmode"][0]
    elif secret:
        host = str(secret.get("host") or secret.get("hostname") or "").strip() or None
        pe = secret.get("port")
        if pe is not None:
            try:
                port = int(pe)
            except (TypeError, ValueError):
                port = 5432
        dbname = str(
            secret.get("dbname") or secret.get("database") or secret.get("dbInstanceIdentifier") or "postgres"
        ).strip()
        smode = secret.get("sslmode")
        if isinstance(smode, str) and smode.strip():
            sslmode = smode.strip()
    else:
        host = (os.environ.get("POSTGRES_HOST") or "").strip() or None
        port_s = (os.environ.get("POSTGRES_PORT") or "5432").strip()
        port = int(port_s) if port_s.isdigit() else 5432
        dbname = (os.environ.get("POSTGRES_DB") or "menuqr_db").strip()

    if not host or not dbname or not user:
        return None

    out: dict[str, Any] = {
        "host": host,
        "port": port,
        "dbname": dbname,
        "user": user,
        "password": password,
        "connect_timeout": 30,
    }
    if sslmode:
        out["sslmode"] = sslmode
    return out


def fetch_tenant_ids_from_postgres() -> list[str]:
    kw = _postgres_connect_kwargs()
    if kw is None:
        raise RuntimeError(
            "Falta configuración PostgreSQL: define DB_URL y credenciales (DB_USER/DB_PASS o DB_SECRET_ARN), "
            "o solo DB_SECRET_ARN si el JSON incluye host/port/dbname (formato RDS), "
            "o POSTGRES_HOST + POSTGRES_DB + usuario/contraseña. Alternativa: TENANT_IDS=uuid1,uuid2."
        )
    try:
        with psycopg2.connect(**kw) as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT id::text FROM restaurants ORDER BY created_at, id")
                return [row[0] for row in cur.fetchall()]
    except Exception as e:
        raise RuntimeError(
            f"No se pudo conectar a PostgreSQL o leer la tabla restaurants: {e}"
        ) from e


def get_all_tenants() -> list[str]:
    raw = os.environ.get("TENANT_IDS", "").strip()
    if raw:
        return [t.strip() for t in raw.split(",") if t.strip()]
    return fetch_tenant_ids_from_postgres()


def default_source_day_utc() -> str:
    yesterday = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(days=1)
    return yesterday.strftime("%Y-%m-%d")


def enqueue_tenant_jobs(queue_url: str, tenant_ids: list[str], source_day: str) -> int:
    sqs = sqs_client()
    for i in range(0, len(tenant_ids), 10):
        chunk = tenant_ids[i : i + 10]
        entries = [
            {
                "Id": str(j),
                "MessageBody": json.dumps({"tenant_id": tid, "source_day": source_day}),
            }
            for j, tid in enumerate(chunk)
        ]
        sqs.send_message_batch(QueueUrl=queue_url, Entries=entries)
    return len(tenant_ids)


def handler(event: dict[str, Any], context: Any) -> dict[str, Any]:
    queue_url = (os.environ.get("TRAINING_JOB_QUEUE_URL") or "").strip()
    if not queue_url:
        raise RuntimeError("TRAINING_JOB_QUEUE_URL no está definido")

    source_day = default_source_day_utc()
    if isinstance(event, dict):
        d = event.get("detail") if isinstance(event.get("detail"), dict) else None
        if d and isinstance(d.get("source_day"), str) and d["source_day"].strip():
            source_day = d["source_day"].strip()

    tenant_ids = get_all_tenants()
    if not tenant_ids:
        return {"ok": False, "error": "no_tenants", "enqueued": 0, "source_day": source_day}

    n = enqueue_tenant_jobs(queue_url, tenant_ids, source_day)
    return {"ok": True, "enqueued": n, "source_day": source_day, "tenants": len(tenant_ids)}
