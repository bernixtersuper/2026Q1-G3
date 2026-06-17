#!/usr/bin/env bash
# Ejecuta on-demand el Glue job analytics-enrich (S3 Parquet → enrich DAY# en DynamoDB).
#
# Uso:
#   bash terraform/scripts/run-glue-analytics-enrich.sh
#   RUN_CRAWLER=1 bash terraform/scripts/run-glue-analytics-enrich.sh
#   SKIP_WAIT=1 bash terraform/scripts/run-glue-analytics-enrich.sh
#   FORCE_NEW=1 bash terraform/scripts/run-glue-analytics-enrich.sh
#
# Nota demo: Firehose escribe Parquet en S3 cada ~5 min (buffering_interval=300).
# Generá eventos en el menú público y esperá unos minutos antes de correr este script.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
AWS_REGION="${AWS_REGION:-us-east-1}"
POLL_INTERVAL_SEC="${POLL_INTERVAL_SEC:-15}"
WAIT_TIMEOUT_SEC="${WAIT_TIMEOUT_SEC:-3600}"

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

normalize_run_id() {
  local id="${1:-}"
  if [[ -z "${id}" || "${id}" == "None" || "${id}" == "null" ]]; then
    return 1
  fi
  echo "${id}"
}

find_active_job_run() {
  local raw
  raw="$(aws glue get-job-runs \
    --job-name "${JOB_NAME}" \
    --region "${AWS_REGION}" \
    --max-results 10 \
    --query "JobRuns[?JobRunState=='RUNNING' || JobRunState=='STARTING' || JobRunState=='STOPPING' || JobRunState=='WAITING'].Id | [0]" \
    --output text 2>/dev/null | tr -d '\r' || true)"
  normalize_run_id "${raw}" || return 1
}

start_new_job_run() {
  local err_file run_id
  err_file="$(mktemp)"
  if run_id="$(aws glue start-job-run \
    --job-name "${JOB_NAME}" \
    --region "${AWS_REGION}" \
    --query 'JobRunId' \
    --output text 2>"${err_file}")"; then
    rm -f "${err_file}"
    normalize_run_id "${run_id}"
    return 0
  fi

  local err
  err="$(tr -d '\r' < "${err_file}")"
  rm -f "${err_file}"

  if [[ "${err}" == *ConcurrentRunsExceededException* ]]; then
    local attempt active_id
    for attempt in 1 2 3 4 5; do
      if active_id="$(find_active_job_run)"; then
        echo "${active_id}"
        return 0
      fi
      sleep 3
    done
    echo "ERROR: hay un job en curso pero no aparece en get-job-runs; reintentá en unos segundos." >&2
    echo "${err}" >&2
    return 1
  fi

  echo "ERROR: ${err}" >&2
  return 1
}

get_job_run_state() {
  local run_id="$1"
  aws glue get-job-run \
    --job-name "${JOB_NAME}" \
    --run-id "${run_id}" \
    --region "${AWS_REGION}" \
    --query 'JobRun.JobRunState' \
    --output text
}

wait_for_job_run() {
  local run_id="$1"
  local deadline=$((SECONDS + WAIT_TIMEOUT_SEC))

  echo "==> Esperando finalización (poll cada ${POLL_INTERVAL_SEC}s, timeout ${WAIT_TIMEOUT_SEC}s)..."

  while true; do
    local state
    state="$(get_job_run_state "${run_id}")"

    case "${state}" in
      SUCCEEDED)
        echo "Estado final: ${state}"
        return 0
        ;;
      FAILED|STOPPED|TIMEOUT|ERROR)
        echo "Estado final: ${state}" >&2
        echo "ERROR: el job no terminó en SUCCEEDED" >&2
        echo "Logs: consola Glue → ${JOB_NAME} → run ${run_id}" >&2
        return 1
        ;;
      RUNNING|STARTING|STOPPING|WAITING)
        if (( SECONDS >= deadline )); then
          echo "ERROR: timeout esperando el job (${WAIT_TIMEOUT_SEC}s)" >&2
          echo "Seguí el estado con:" >&2
          echo "  aws glue get-job-run --job-name ${JOB_NAME} --run-id ${run_id} --region ${AWS_REGION}" >&2
          return 1
        fi
        echo "    ... ${state} ($(date +%H:%M:%S))"
        sleep "${POLL_INTERVAL_SEC}"
        ;;
      *)
        echo "ERROR: estado desconocido '${state}' para run ${run_id}" >&2
        return 1
        ;;
    esac
  done
}

RUN_ID=""
if [[ "${FORCE_NEW:-0}" != "1" ]]; then
  if RUN_ID="$(find_active_job_run)"; then
    echo "==> Ya hay un job en curso; se reutiliza el run existente"
    echo "    JobRunId: ${RUN_ID}"
    echo "    (Usá FORCE_NEW=1 para intentar lanzar otro; fallará si MaxConcurrentRuns=1.)"
  fi
fi

if [[ -z "${RUN_ID}" ]]; then
  echo "==> start-job-run"
  RUN_ID="$(start_new_job_run)" || exit 1
  echo "    JobRunId: ${RUN_ID}"
fi

if [[ "${SKIP_WAIT:-0}" == "1" ]]; then
  echo "SKIP_WAIT=1: job en curso. Seguí el estado con:"
  echo "  aws glue get-job-run --job-name ${JOB_NAME} --run-id ${RUN_ID} --region ${AWS_REGION}"
  exit 0
fi

if wait_for_job_run "${RUN_ID}"; then
  echo "Listo. Refrescá el dashboard de analytics en el admin."
else
  exit 1
fi
