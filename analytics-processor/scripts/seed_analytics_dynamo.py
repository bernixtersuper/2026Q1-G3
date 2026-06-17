#!/usr/bin/env python3
"""
Seed the DynamoDB analytics table (``menuqr-analytics``) with simulated traffic history
for a demo. Writes the same item shapes the backend read-path expects:

  PK=TENANT#<id>  SK=DAY#<yyyy-MM-dd>   -> menuViews, itemViews, sectionViews, cartAdds,
                                           uniqueMenuSessions, batchCompletedAt, topItemIds,
                                           filterBreakdown, sectionBreakdown
  PK=TENANT#<id>  SK=HOUR#<yyyy-MM-ddTHH> -> menuViews, itemViews, cartAdds
  PK=TENANT#<id>  SK=ITEM#<itemId>        -> views, lastViewedAt

Why a direct-write script (vs. emitting events): the DynamoDB items key the date into the
sort key, so any past day can be written directly. Interaction events, by contrast, are always
stamped "now", so they cannot backfill history. Setting batchCompletedAt also makes the
dashboard show FINAL conversion + the trends "batch" panels without running the Glue job.

The real menu item/section IDs are fetched from the admin API (GET /api/admin/menu) so the
"top viewed" and "section engagement" panels resolve real names. Run the relational seed first
(POST /api/admin/demo/seed) so the menu exists.

These items carry NO ttl (only the processor's PROC# dedup records do), so seeded history does
not expire. PutItem overwrites any existing DAY#/HOUR#/ITEM# for the tenant in the window —
intended for demo tenants, not for tenants with real traffic you want to keep.

Requires: boto3 and valid AWS credentials (e.g. AWS CloudShell, or `aws configure`). Dates are
computed in UTC, matching ECS Fargate's default timezone.

Usage:
  python3 seed_analytics_dynamo.py --api-url http://<alb-host> --token <JWT> [--days 30]
  python3 seed_analytics_dynamo.py --api-url ... --token ... --dry-run      # preview, no writes
  python3 seed_analytics_dynamo.py --api-url ... --token ... --tz America/Argentina/Buenos_Aires
"""
import argparse
import json
import random
import sys
import urllib.request
from datetime import datetime, timedelta, timezone

import boto3

SEED = 42
FILTER_WEIGHTS = {"VEGETARIAN": 0.40, "GLUTEN_FREE": 0.25, "VEGAN": 0.20, "DAIRY_FREE": 0.15}
SERVICE_HOURS = [12, 13, 14, 20, 21, 22]  # lunch + dinner peaks


def fetch_menu(api_url, token):
    """GET /api/admin/menu and return (tenant_id, sections) where sections carry item ids."""
    req = urllib.request.Request(
        api_url.rstrip("/") + "/api/admin/menu",
        headers={"Authorization": "Bearer " + token, "Accept": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = json.loads(resp.read().decode("utf-8"))

    tenant_id = data.get("tenantId")
    sections = []
    for s in data.get("sections", []):
        items = [{"id": it["id"], "name": it.get("name", "")} for it in s.get("items", [])]
        sections.append({"id": s["id"], "name": s.get("name", ""), "items": items})
    if not tenant_id or not sections:
        sys.exit("Menu has no tenant/sections — run the relational seed (POST /api/admin/demo/seed) first.")
    return tenant_id, sections


def jitter(rnd, lo=0.85, hi=1.15):
    return lo + rnd.random() * (hi - lo)


def n(value):
    return {"N": str(int(max(0, value)))}


def split_counts(rnd, total, weights):
    """Distribute `total` across keys proportional to weight (normalized), with light jitter."""
    sum_w = sum(weights.values())
    if sum_w <= 0:
        return {}
    out = {}
    for key, w in weights.items():
        out[key] = int(total * (w / sum_w) * jitter(rnd))
    return {k: v for k, v in out.items() if v > 0}


def resolve_tz(name):
    """Timezone for DAY#/HOUR# keys. Default UTC avoids needing the zoneinfo database."""
    if name.upper() == "UTC":
        return timezone.utc
    from zoneinfo import ZoneInfo  # stdlib; only needed for non-UTC zones
    try:
        return ZoneInfo(name)
    except Exception as exc:  # ZoneInfoNotFoundError or missing tzdata
        sys.exit(f"Unknown --tz '{name}': {exc} (try 'pip install tzdata')")


def make_put(ddb, table, dry_run):
    """Returns a put(item) that writes to DynamoDB, or prints a preview in dry-run mode."""
    def put(item):
        if dry_run:
            sk = item["SK"]["S"]
            nums = {k: v["N"] for k, v in item.items() if isinstance(v, dict) and "N" in v}
            extra = f" top={len(item['topItemIds']['L'])}" if "topItemIds" in item else ""
            print(f"  [dry-run] PUT {sk}  {nums}{extra}")
        else:
            ddb.put_item(TableName=table, Item=item)
    return put


def main():
    ap = argparse.ArgumentParser(description="Seed DynamoDB analytics with simulated history.")
    ap.add_argument("--api-url", required=True, help="Backend base URL (ALB), e.g. http://host")
    ap.add_argument("--token", required=True, help="Admin JWT (same tenant you want to seed)")
    ap.add_argument("--table", default="menuqr-analytics", help="DynamoDB table name")
    ap.add_argument("--region", default="us-east-1", help="AWS region")
    ap.add_argument("--days", type=int, default=30, help="Days of history (1-90)")
    ap.add_argument("--top-n", type=int, default=10, help="topItemIds length per day")
    ap.add_argument("--tz", default="UTC",
                    help="Timezone for DAY#/HOUR# keys; match the backend (Fargate = UTC). e.g. America/Argentina/Buenos_Aires")
    ap.add_argument("--dry-run", action="store_true",
                    help="Print the items that would be written without touching DynamoDB")
    args = ap.parse_args()

    days = max(1, min(90, args.days))
    rnd = random.Random(SEED)
    tz = resolve_tz(args.tz)
    ddb = None if args.dry_run else boto3.client("dynamodb", region_name=args.region)
    put = make_put(ddb, args.table, args.dry_run)

    tenant_id, sections = fetch_menu(args.api_url, args.token)
    items = [it for s in sections for it in s["items"]]
    pk = "TENANT#" + tenant_id
    print(f"==> tenant {tenant_id} | tz={args.tz} | days={days} | "
          f"{'DRY-RUN (no writes)' if args.dry_run else 'writing to ' + args.table}")

    # Deterministic per-item popularity (spread so a clear long tail emerges).
    item_weight = {it["id"]: 1 + rnd.randint(0, 9) for it in items}
    section_weight = {s["id"]: max(1, len(s["items"])) for s in sections}
    item_total_views = {it["id"]: 0 for it in items}

    # DAY#/HOUR# keys use the requested timezone; batchCompletedAt/lastViewedAt are real
    # UTC instants ("...Z") so Java's Instant.parse accepts them.
    now_local = datetime.now(tz)
    today = now_local.date()
    current_hour = now_local.hour
    prev_hour = (now_local - timedelta(hours=1)).hour
    iso_now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    written_days = written_hours = 0

    for i in range(days):
        day = today - timedelta(days=(days - 1 - i))
        date_str = day.isoformat()
        weekend = 1.4 if day.weekday() >= 5 else 1.0
        trend = 0.7 + 0.3 * (i / max(1, days - 1))  # gentle growth toward today

        menu_views = int(60 * trend * weekend * jitter(rnd))
        item_views = int(menu_views * 2.3 * jitter(rnd))
        cart_adds = int(item_views * 0.28 * jitter(rnd))
        section_views = int(menu_views * 0.8 * jitter(rnd))
        unique_sessions = int(menu_views * 0.72 * jitter(rnd))

        # Allocate this day's item views across items by popularity weight.
        day_item_views = split_counts(rnd, item_views, item_weight)
        for iid, c in day_item_views.items():
            item_total_views[iid] += c
        top_items = [iid for iid, _ in sorted(day_item_views.items(), key=lambda kv: kv[1], reverse=True)][: args.top_n]

        filter_breakdown = split_counts(rnd, int(menu_views * 0.35), FILTER_WEIGHTS)
        section_breakdown = split_counts(rnd, section_views, section_weight)

        day_item = {
            "PK": {"S": pk},
            "SK": {"S": "DAY#" + date_str},
            "menuViews": n(menu_views),
            "itemViews": n(item_views),
            "sectionViews": n(section_views),
            "cartAdds": n(cart_adds),
            "uniqueMenuSessions": n(unique_sessions),
            "batchCompletedAt": {"S": iso_now},
            "topItemIds": {"L": [{"S": iid} for iid in top_items]},
        }
        if filter_breakdown:
            day_item["filterBreakdown"] = {"M": {k: n(v) for k, v in filter_breakdown.items()}}
        if section_breakdown:
            day_item["sectionBreakdown"] = {"M": {k: n(v) for k, v in section_breakdown.items()}}
        put(day_item)
        written_days += 1

        # Hourly buckets across peaks; include the current hour for "today" so realtime lights up.
        hours = set(SERVICE_HOURS)
        if day == today:
            hours.add(current_hour)
            hours.add(prev_hour)
        for hour in sorted(h for h in hours if not (day == today and h > current_hour)):
            share = jitter(rnd, 0.10, 0.22)
            put({
                "PK": {"S": pk},
                "SK": {"S": f"HOUR#{date_str}T{hour:02d}"},
                "menuViews": n(menu_views * share),
                "itemViews": n(item_views * share),
                "cartAdds": n(cart_adds * share),
            })
            written_hours += 1

    # Cumulative per-item view totals.
    for iid, total in item_total_views.items():
        put({
            "PK": {"S": pk},
            "SK": {"S": "ITEM#" + iid},
            "views": n(total),
            "lastViewedAt": {"S": iso_now},
        })

    verb = "Would seed" if args.dry_run else "Seeded"
    print(
        f"{verb} DynamoDB analytics for tenant {tenant_id}: "
        f"{written_days} DAY#, {written_hours} HOUR#, {len(items)} ITEM# items "
        f"({date_str_range(today, days)})."
    )


def date_str_range(today, days):
    start = today - timedelta(days=days - 1)
    return f"{start.isoformat()} → {today.isoformat()}"


if __name__ == "__main__":
    main()
