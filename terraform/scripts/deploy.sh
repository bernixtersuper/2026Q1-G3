#!/usr/bin/env bash
# Despliegue completo: Lambdas ML → Terraform apply → backend (ECR+ECS) → frontends (S3).
#
# Uso:
#   bash terraform/scripts/deploy.sh
#   SKIP_TERRAFORM_APPLY=1 bash terraform/scripts/deploy.sh   # solo app (infra ya aplicada)
#   TERRAFORM_PLAN_ONLY=1 bash terraform/scripts/deploy.sh    # plan en lugar de apply
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: falta el comando '$1' en PATH" >&2
    exit 1
  }
}

require_cmd terraform
require_cmd aws

echo "==> 1/4 Lambda artifacts (ml-training)"
bash "${ROOT}/ml-training/scripts/build_lambda_dists.sh"

echo ""
echo "==> 2/4 Terraform"
if [[ -f "${TF_DIR}/backend.hcl" ]]; then
  echo "    terraform init (remote state)"
  terraform -chdir="${TF_DIR}" init -backend-config=backend.hcl -input=false
elif [[ ! -d "${TF_DIR}/.terraform" ]]; then
  echo "    terraform init (local state; remoto: bash terraform/scripts/terraform-init-remote.sh)"
  terraform -chdir="${TF_DIR}" init -input=false
fi

if [[ "${SKIP_TERRAFORM_APPLY:-0}" == "1" ]]; then
  echo "    SKIP_TERRAFORM_APPLY=1: omitiendo plan/apply"
elif [[ "${TERRAFORM_PLAN_ONLY:-0}" == "1" ]]; then
  echo "    terraform plan"
  terraform -chdir="${TF_DIR}" plan -var-file=terraform.tfvars -input=false
  echo ""
  echo "Plan listo (TERRAFORM_PLAN_ONLY=1). Para aplicar: terraform -chdir=terraform apply"
  exit 0
else
  echo "    terraform apply"
  terraform -chdir="${TF_DIR}" apply -var-file=terraform.tfvars -input=false -auto-approve
fi

echo ""
echo "==> 3/4 Backend (ECR + ECS)"
bash "${SCRIPT_DIR}/deploy-backend.sh"

echo ""
echo "==> 4/4 Frontends (S3)"
bash "${SCRIPT_DIR}/deploy-frontends.sh"

echo ""
echo "Despliegue completo."
