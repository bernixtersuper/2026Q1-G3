#!/usr/bin/env bash
# Ejecuta on-demand el Glue job analytics-enrich (S3 Parquet → enrich DAY# en DynamoDB).
#
# Uso:
#   bash terraform/scripts/run-glue-analytics-enrich.sh
#   RUN_CRAWLER=1 bash terraform/scripts/run-glue-analytics-enrich.sh
#   SKIP_WAIT=1 bash terraform/scripts/run-glue-analytics-enrich.sh
#
# Nota demo: Firehose escribe Parquet en S3 cada ~5 min (buffering_interval=300).
# Generá eventos en el menú público y esperá unos minutos antes de correr este script.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
AWS_REGION="${AWS_REGION:-us-east-1}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: falta el comando '$1' en PATH" >&2
    exit 1
  }
}

require_cmd terraform
require_cmd aws

if [[ ! -d "${TF_DIR}/.terraform" ]]; then
  echo "==> terraform init (${TF_DIR})"
  terraform -chdir="${TF_DIR}" init -input=false
fi

JOB_NAME="$(terraform -chdir="${TF_DIR}" output -raw glue_analytics_enrich_job_name)"
CRAWLER_NAME="$(terraform -chdir="${TF_DIR}" output -raw glue_events_crawler_name)"

echo "==> Región: ${AWS_REGION}"
echo "    Glue job: ${JOB_NAME}"

if [[ "${RUN_CRAWLER:-0}" == "1" ]]; then
  echo "==> Iniciando crawler ${CRAWLER_NAME} (catálogo Athena; el job enrich lee S3 directo)"
  aws glue start-crawler --name "${CRAWLER_NAME}" --region "${AWS_REGION}" || true
  echo "    (El crawler corre en background; no bloquea el job enrich.)"
fi

echo "==> start-job-run"
RUN_ID="$(aws glue start-job-run \
  --job-name "${JOB_NAME}" \
  --region "${AWS_REGION}" \
  --query 'JobRunId' \
  --output text)"

echo "    JobRunId: ${RUN_ID}"

if [[ "${SKIP_WAIT:-0}" == "1" ]]; then
  echo "SKIP_WAIT=1: job lanzado. Seguí el estado en consola Glue o con:"
  echo "  aws glue get-job-run --job-name ${JOB_NAME} --run-id ${RUN_ID} --region ${AWS_REGION}"
  exit 0
fi

echo "==> Esperando finalización (timeout waiter ~1 h)..."
if aws glue wait job-run-completed \
  --job-name "${JOB_NAME}" \
  --run-id "${RUN_ID}" \
  --region "${AWS_REGION}"; then
  STATE="$(aws glue get-job-run \
    --job-name "${JOB_NAME}" \
    --run-id "${RUN_ID}" \
    --region "${AWS_REGION}" \
    --query 'JobRun.JobRunState' \
    --output text)"
  echo "Estado final: ${STATE}"
  if [[ "${STATE}" != "SUCCEEDED" ]]; then
    echo "ERROR: el job no terminó en SUCCEEDED" >&2
    exit 1
  fi
  echo "Listo. Refrescá el dashboard de analytics en el admin."
else
  echo "ERROR: timeout o fallo esperando el job" >&2
  exit 1
fi
