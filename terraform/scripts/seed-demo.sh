#!/usr/bin/env bash
# Demo seeding (manual trigger). Populates a tenant so every analytics panel looks full:
#   1) Relational side (PostgreSQL): backdated orders/menu/tables via the backend endpoint
#      POST /api/admin/demo/seed  (runs in-VPC; RDS is private and unreachable from a laptop).
#   2) Views/traffic side (DynamoDB): simulated daily/hourly/item aggregates with proper dates
#      via analytics-processor/scripts/seed_analytics_dynamo.py (direct writes).
#
# Requires the hidden endpoint to be enabled: terraform var demo_seed_enabled=true
# (env DEMO_SEED_ENABLED=true on the ECS task), and an admin JWT for the tenant to seed.
#
# Usage:
#   TOKEN=<admin-jwt> bash terraform/scripts/seed-demo.sh
#   TOKEN=<jwt> API_URL=http://my-alb DAYS=45 bash terraform/scripts/seed-demo.sh
#   SKIP_ORDERS=1 TOKEN=<jwt> bash terraform/scripts/seed-demo.sh   # only DynamoDB
#   SKIP_DYNAMO=1 TOKEN=<jwt> bash terraform/scripts/seed-demo.sh   # only orders
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
REPO_ROOT="$(cd "${TF_DIR}/.." && pwd)"
AWS_REGION="${AWS_REGION:-us-east-1}"
DAYS="${DAYS:-30}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || { echo "ERROR: missing command '$1' in PATH" >&2; exit 1; }
}
require_cmd curl
require_cmd python3

if [[ -z "${TOKEN:-}" ]]; then
  echo "ERROR: set TOKEN=<admin JWT> (Authorization bearer for the tenant to seed)." >&2
  exit 1
fi

# Resolve the backend URL from Terraform output if not provided.
if [[ -z "${API_URL:-}" ]]; then
  require_cmd terraform
  API_URL="$(terraform -chdir="${TF_DIR}" output -raw backend_api_url)"
fi
API_URL="${API_URL%/}"

echo "==> API: ${API_URL}"
echo "    Region: ${AWS_REGION}   Days: ${DAYS}"

if [[ "${SKIP_ORDERS:-0}" != "1" ]]; then
  echo "==> 1/2 Relational seed (orders/menu/tables) — POST /api/admin/demo/seed"
  HTTP_CODE="$(curl -sS -o /tmp/demo-seed-orders.json -w '%{http_code}' \
    -X POST "${API_URL}/api/admin/demo/seed?days=${DAYS}" \
    -H "Authorization: Bearer ${TOKEN}")"
  if [[ "${HTTP_CODE}" == "404" ]]; then
    echo "ERROR: endpoint returned 404 — demo seeding is disabled." >&2
    echo "       Set demo_seed_enabled=true (tfvars) / DEMO_SEED_ENABLED=true and redeploy." >&2
    exit 1
  fi
  if [[ "${HTTP_CODE}" != "200" ]]; then
    echo "ERROR: relational seed failed (HTTP ${HTTP_CODE}):" >&2
    cat /tmp/demo-seed-orders.json >&2; echo >&2
    exit 1
  fi
  echo "    $(cat /tmp/demo-seed-orders.json)"
else
  echo "==> 1/2 Relational seed SKIPPED (SKIP_ORDERS=1)"
fi

if [[ "${SKIP_DYNAMO:-0}" != "1" ]]; then
  echo "==> 2/2 DynamoDB views seed — seed_analytics_dynamo.py"
  python3 "${REPO_ROOT}/analytics-processor/scripts/seed_analytics_dynamo.py" \
    --api-url "${API_URL}" \
    --token "${TOKEN}" \
    --region "${AWS_REGION}" \
    --days "${DAYS}"
else
  echo "==> 2/2 DynamoDB views seed SKIPPED (SKIP_DYNAMO=1)"
fi

echo "Done. Refresh the analytics dashboard in the admin SPA."
