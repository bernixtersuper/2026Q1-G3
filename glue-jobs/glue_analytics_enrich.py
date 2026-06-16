"""
Glue ETL nocturno: dedup eventos S3 → enriquece DAY# en DynamoDB.
Materializa uniqueMenuSessions, topItemIds, filterBreakdown, sectionBreakdown, batchCompletedAt.
"""
import json
import sys
from datetime import datetime, timedelta, timezone

import boto3
from awsglue.context import GlueContext
from awsglue.job import Job
from awsglue.utils import getResolvedOptions
from pyspark.context import SparkContext
from pyspark.sql import functions as F
from pyspark.sql.window import Window

args = getResolvedOptions(
    sys.argv,
    ["JOB_NAME", "ANALYTICS_TABLE", "ANALYTICS_BUCKET", "TOP_N"],
)

optional = {}
for key in ("GLUE_DATABASE", "EVENTS_TABLE", "AWS_REGION"):
    if f"--{key}" in sys.argv:
        optional[key] = getResolvedOptions(sys.argv, [key])[key]

analytics_table = args["ANALYTICS_TABLE"]
bucket = args["ANALYTICS_BUCKET"]
top_n = int(args.get("TOP_N", "10"))
region = optional.get("AWS_REGION", "us-east-1")

sc = SparkContext()
glue_context = GlueContext(sc)
spark = glue_context.spark_session
job = Job(glue_context)
job.init(args["JOB_NAME"], args)


def normalize_columns(df):
    renames = {}
    aliases = {
        "eventid": "eventId",
        "eventtype": "eventType",
        "tenantid": "tenantId",
        "sessionid": "sessionId",
        "itemid": "itemId",
        "sectionid": "sectionId",
    }
    for col in df.columns:
        key = col.lower()
        if key in aliases and col != aliases[key]:
            renames[col] = aliases[key]
    for old, new in renames.items():
        df = df.withColumnRenamed(old, new)
    return df


def read_day_parquet(target_date):
    prefix = (
        f"s3://{bucket}/events/year={target_date.year}/"
        f"month={target_date.month:02d}/day={target_date.day:02d}/"
    )
    try:
        df = spark.read.parquet(prefix)
        if df.rdd.isEmpty():
            return None
        return normalize_columns(df)
    except Exception as exc:
        print(f"No Parquet data for {target_date}: {exc}")
        return None


def dedup_events(df):
    w = Window.partitionBy("eventId").orderBy(F.col("timestamp").desc_nulls_last())
    return df.withColumn("rn", F.row_number().over(w)).filter(F.col("rn") == 1).drop("rn")


def enrich_date(target_date):
    df = read_day_parquet(target_date)
    if df is None:
        return

    df = dedup_events(df)
    date_str = target_date.isoformat()
    batch_completed_at = datetime.now(timezone.utc).isoformat()
    ddb = boto3.client("dynamodb", region_name=region)

    tenants = [r.tenantId for r in df.select("tenantId").distinct().collect() if r.tenantId]
    print(f"Enriching {len(tenants)} tenants for {date_str}")

    for tenant_id in tenants:
        tdf = df.filter(F.col("tenantId") == tenant_id)

        menu_views = tdf.filter(F.col("eventType") == "MENU_VIEW")
        unique_sessions = menu_views.select("sessionId").distinct().count()

        item_views = (
            tdf.filter((F.col("eventType") == "ITEM_VIEW") & F.col("itemId").isNotNull())
            .groupBy("itemId")
            .count()
            .orderBy(F.col("count").desc())
            .limit(top_n)
        )
        top_items = [r.itemId for r in item_views.collect() if r.itemId]

        filter_rows = (
            tdf.filter(F.col("eventType").isin("FILTER_APPLIED", "FILTER_USED"))
            .select(F.coalesce(F.col("metadata").getItem("filterTag"), F.lit("UNKNOWN")).alias("tag"))
            .groupBy("tag")
            .count()
            .collect()
        )
        filter_breakdown = {r.tag: int(r["count"]) for r in filter_rows if r.tag}

        section_rows = (
            tdf.filter((F.col("eventType") == "SECTION_VIEW") & F.col("sectionId").isNotNull())
            .groupBy("sectionId")
            .count()
            .collect()
        )
        section_breakdown = {r.sectionId: int(r["count"]) for r in section_rows if r.sectionId}

        expr_parts = [
            "uniqueMenuSessions = :ums",
            "batchCompletedAt = :bca",
            "topItemIds = :top",
        ]
        values = {
            ":ums": {"N": str(unique_sessions)},
            ":bca": {"S": batch_completed_at},
            ":top": {"L": [{"S": i} for i in top_items]},
        }

        if filter_breakdown:
            expr_parts.append("filterBreakdown = :fb")
            values[":fb"] = {
                "M": {k: {"N": str(v)} for k, v in filter_breakdown.items()}
            }

        if section_breakdown:
            expr_parts.append("sectionBreakdown = :sb")
            values[":sb"] = {
                "M": {k: {"N": str(v)} for k, v in section_breakdown.items()}
            }

        ddb.update_item(
            TableName=analytics_table,
            Key={
                "PK": {"S": f"TENANT#{tenant_id}"},
                "SK": {"S": f"DAY#{date_str}"},
            },
            UpdateExpression="SET " + ", ".join(expr_parts),
            ExpressionAttributeValues=values,
        )
        print(f"Updated DAY#{date_str} tenant={tenant_id} sessions={unique_sessions}")


today = datetime.now(timezone.utc).date()
yesterday = today - timedelta(days=1)
enrich_date(yesterday)
enrich_date(today)

job.commit()
