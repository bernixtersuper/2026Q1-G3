#!/usr/bin/env bash
# Build del backend Quarkus, push a ECR y redeploy del servicio ECS Fargate.
#
# Requisitos: terraform apply previo, docker, mvn, aws cli.
# Uso:
#   bash terraform/scripts/deploy-backend.sh
#   IMAGE_TAG=v1 bash terraform/scripts/deploy-backend.sh
#   SKIP_MVN=1 bash terraform/scripts/deploy-backend.sh
#   SKIP_DEPLOY=1 bash terraform/scripts/deploy-backend.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${ROOT}/backend"
IMAGE_TAG="${IMAGE_TAG:-latest}"
AWS_REGION="${AWS_REGION:-us-east-1}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: falta el comando '$1' en PATH" >&2
    exit 1
  }
}

require_cmd terraform
require_cmd docker
require_cmd mvn
require_cmd aws

if [[ ! -d "${TF_DIR}/.terraform" ]]; then
  echo "==> terraform init (${TF_DIR})"
  terraform -chdir="${TF_DIR}" init -input=false
fi

echo "==> Leyendo outputs de Terraform"
ECR_URL="$(terraform -chdir="${TF_DIR}" output -raw backend_ecr_repository_url)"
CLUSTER="$(terraform -chdir="${TF_DIR}" output -raw backend_ecs_cluster_name)"
SERVICE="$(terraform -chdir="${TF_DIR}" output -raw backend_ecs_service_name)"
API_URL="$(terraform -chdir="${TF_DIR}" output -raw backend_api_url)"
API_URL="${API_URL%/}"

IMAGE_URI="${ECR_URL}:${IMAGE_TAG}"
ECR_REGISTRY="${ECR_URL%%/*}"

echo "    Región:    ${AWS_REGION}"
echo "    Imagen:    ${IMAGE_URI}"
echo "    Cluster:   ${CLUSTER}"
echo "    Servicio:  ${SERVICE}"

if [[ "${SKIP_MVN:-0}" != "1" ]]; then
  echo "==> mvn package (backend)"
  mvn -f "${BACKEND_DIR}/pom.xml" -DskipTests package -q
else
  echo "==> SKIP_MVN=1: omitiendo Maven"
fi

echo "==> docker build"
docker build -f "${BACKEND_DIR}/src/main/docker/Dockerfile.jvm" \
  -t "menudigital-backend:${IMAGE_TAG}" \
  "${BACKEND_DIR}"

echo "==> login ECR"
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

echo "==> docker tag + push"
docker tag "menudigital-backend:${IMAGE_TAG}" "${IMAGE_URI}"
docker push "${IMAGE_URI}"

if [[ "${SKIP_DEPLOY:-0}" == "1" ]]; then
  echo "==> SKIP_DEPLOY=1: imagen subida; omitiendo ecs update-service"
else
  echo "==> ECS force-new-deployment"
  aws ecs update-service \
    --region "${AWS_REGION}" \
    --cluster "${CLUSTER}" \
    --service "${SERVICE}" \
    --force-new-deployment \
    --output text \
    --query 'service.serviceName' >/dev/null
fi

echo ""
echo "Listo."
echo "  Imagen:  ${IMAGE_URI}"
echo "  API:     ${API_URL}"
echo "  Health:  ${API_URL}/q/health/ready"
echo ""
