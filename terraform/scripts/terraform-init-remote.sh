#!/usr/bin/env bash
# Crea (si hace falta) bucket S3 + tabla DynamoDB para tfstate y ejecuta terraform init.
#
# Uso:
#   bash terraform/scripts/terraform-init-remote.sh
#   MIGRATE_LOCAL_STATE=1 bash terraform/scripts/terraform-init-remote.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BOOTSTRAP_DIR="${TF_DIR}/bootstrap"
BACKEND_HCL="${TF_DIR}/backend.hcl"
PROJECT_NAME="${TF_PROJECT_NAME:-menuqr}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: falta el comando '$1' en PATH" >&2
    exit 1
  }
}

require_cmd terraform
require_cmd aws

echo "==> Bootstrap remoto (S3 + DynamoDB)"
if [[ ! -d "${BOOTSTRAP_DIR}/.terraform" ]]; then
  terraform -chdir="${BOOTSTRAP_DIR}" init -input=false
fi
terraform -chdir="${BOOTSTRAP_DIR}" apply -input=false -auto-approve -var="project_name=${PROJECT_NAME}"

BUCKET="$(terraform -chdir="${BOOTSTRAP_DIR}" output -raw state_bucket_name)"
TABLE="$(terraform -chdir="${BOOTSTRAP_DIR}" output -raw lock_table_name)"
KEY="$(terraform -chdir="${BOOTSTRAP_DIR}" output -raw state_key)"

cat > "${BACKEND_HCL}" <<EOF
bucket         = "${BUCKET}"
dynamodb_table = "${TABLE}"
key            = "${KEY}"
region         = "us-east-1"
encrypt        = true
EOF

echo "    backend.hcl escrito en ${BACKEND_HCL}"
echo "    bucket: ${BUCKET}"
echo "    lock table: ${TABLE}"

echo ""
echo "==> terraform init (backend S3)"
INIT_ARGS=(-backend-config="${BACKEND_HCL}" -input=false)
if [[ -f "${TF_DIR}/terraform.tfstate" && "${MIGRATE_LOCAL_STATE:-0}" == "1" ]]; then
  INIT_ARGS+=(-migrate-state)
fi
terraform -chdir="${TF_DIR}" init "${INIT_ARGS[@]}"

echo ""
echo "Remoto listo. Para CI, añade secrets TF_STATE_BUCKET=${BUCKET} y TF_STATE_DYNAMODB_TABLE=${TABLE}"
