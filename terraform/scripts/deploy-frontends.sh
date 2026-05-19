#!/usr/bin/env bash
# Build de admin + menu (Vite) y sync a buckets S3 website.
#
# Requisitos: terraform apply, node/npm, aws cli.
# Uso:
#   bash terraform/scripts/deploy-frontends.sh
#   VITE_API_URL=https://api.example.com bash terraform/scripts/deploy-frontends.sh
#   SKIP_INSTALL=1 bash terraform/scripts/deploy-frontends.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
AWS_REGION="${AWS_REGION:-us-east-1}"

ADMIN_DIR="${ROOT}/frontend/admin"
MENU_DIR="${ROOT}/frontend/menu"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: falta el comando '$1' en PATH" >&2
    exit 1
  }
}

require_cmd terraform
require_cmd aws
require_cmd npm

if [[ ! -d "${TF_DIR}/.terraform" ]]; then
  echo "==> terraform init (${TF_DIR})"
  terraform -chdir="${TF_DIR}" init -input=false
fi

echo "==> Leyendo outputs de Terraform"
API_URL="${VITE_API_URL:-$(terraform -chdir="${TF_DIR}" output -raw backend_api_url)}"
MENU_URL="${VITE_MENU_URL:-$(terraform -chdir="${TF_DIR}" output -raw frontend_menu_website_url)}"
ADMIN_BUCKET="$(terraform -chdir="${TF_DIR}" output -raw frontend_admin_s3_bucket)"
MENU_BUCKET="$(terraform -chdir="${TF_DIR}" output -raw frontend_menu_s3_bucket)"
ADMIN_SITE="$(terraform -chdir="${TF_DIR}" output -raw frontend_admin_website_url)"
MENU_SITE="$(terraform -chdir="${TF_DIR}" output -raw frontend_menu_website_url)"

COGNITO_USER_POOL_ID="$(terraform -chdir="${TF_DIR}" output -raw cognito_user_pool_id 2>/dev/null || true)"
COGNITO_CLIENT_ID="$(terraform -chdir="${TF_DIR}" output -raw cognito_user_pool_client_id 2>/dev/null || true)"

API_URL="${API_URL%/}"
MENU_URL="${MENU_URL%/}"
ADMIN_SITE="${ADMIN_SITE%/}"

echo "    Región:       ${AWS_REGION}"
echo "    VITE_API_URL: ${API_URL}"
echo "    VITE_MENU_URL (admin): ${MENU_URL}"
echo "    Bucket admin: s3://${ADMIN_BUCKET}"
echo "    Bucket menú:  s3://${MENU_BUCKET}"
if [[ -n "${COGNITO_USER_POOL_ID}" && -n "${COGNITO_CLIENT_ID}" ]]; then
  echo "    Cognito pool: ${COGNITO_USER_POOL_ID}"
  echo "    Cognito client: ${COGNITO_CLIENT_ID}"
fi

build_and_sync() {
  local name="$1"
  local dir="$2"
  local bucket="$3"
  shift 3
  local -a extra_env=("$@")

  echo ""
  echo "==> ${name}: npm ci + build"
  (
    cd "${dir}"
    if [[ "${SKIP_INSTALL:-0}" != "1" ]]; then
      npm ci
    fi
    export VITE_API_URL="${API_URL}"
    for kv in "${extra_env[@]}"; do
      export "${kv?}"
    done
    npm run build
  )

  echo "==> ${name}: aws s3 sync"
  aws s3 sync "${dir}/dist/" "s3://${bucket}/" \
    --region "${AWS_REGION}" \
    --delete
}

ADMIN_COGNITO_ENV=()
if [[ -n "${COGNITO_USER_POOL_ID}" && -n "${COGNITO_CLIENT_ID}" ]]; then
  ADMIN_COGNITO_ENV=(
    "VITE_COGNITO_USER_POOL_ID=${COGNITO_USER_POOL_ID}"
    "VITE_COGNITO_CLIENT_ID=${COGNITO_CLIENT_ID}"
  )
fi

build_and_sync "Admin" "${ADMIN_DIR}" "${ADMIN_BUCKET}" "VITE_MENU_URL=${MENU_URL}" "${ADMIN_COGNITO_ENV[@]}"
build_and_sync "Menú público" "${MENU_DIR}" "${MENU_BUCKET}"

echo ""
echo "Listo."
echo "  Admin:  ${ADMIN_SITE}"
echo "  Menú:   ${MENU_SITE}"
echo "  API:    ${API_URL}"
