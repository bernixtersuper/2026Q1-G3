#!/usr/bin/env bash
# Invoca on-demand la Lambda ML orquestadora (lista tenants en RDS → encola jobs en SQS).
#
# Uso:
#   bash terraform/scripts/run-ml-orchestrator.sh
#   SOURCE_DAY=2026-06-16 bash terraform/scripts/run-ml-orchestrator.sh
#   WAIT_FOR_QUEUE=1 bash terraform/scripts/run-ml-orchestrator.sh
#   SKIP_WAIT=1 bash terraform/scripts/run-ml-orchestrator.sh   # solo invoke, sin esperar cola
#
# Por defecto la Lambda usa ayer UTC como source_day (mismo criterio que el cron EventBridge).
# Glue enrich debe haber escrito ml_features/ para ese día; si no, el worker puede fallar o
# entrenar con datos vacíos.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TF_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
AWS_REGION="${AWS_REGION:-us-east-1}"
POLL_INTERVAL_SEC="${POLL_INTERVAL_SEC:-10}"
WAIT_TIMEOUT_SEC="${WAIT_TIMEOUT_SEC:-1800}"
OUT_FILE="${OUT_FILE:-/tmp/menuqr-ml-orchestrator-out.json}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: falta el comando '$1' en PATH" >&2
    exit 1
  }
}

require_cmd terraform
require_cmd aws
require_cmd python3

if [[ ! -d "${TF_DIR}/.terraform" ]]; then
  echo "==> terraform init (${TF_DIR})"
  terraform -chdir="${TF_DIR}" init -input=false
fi

FUNCTION_NAME="$(terraform -chdir="${TF_DIR}" output -raw ml_orchestrator_function_name)"
QUEUE_URL="$(terraform -chdir="${TF_DIR}" output -raw ml_training_queue_url)"
ML_BUCKET="$(terraform -chdir="${TF_DIR}" output -raw backend_ml_s3_bucket)"

echo "==> Región: ${AWS_REGION}"
echo "    Lambda: ${FUNCTION_NAME}"
echo "    Cola SQS: ${QUEUE_URL}"

PAYLOAD='{}'
if [[ -n "${SOURCE_DAY:-}" ]]; then
  if [[ ! "${SOURCE_DAY}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    echo "ERROR: SOURCE_DAY debe ser YYYY-MM-DD (recibido: ${SOURCE_DAY})" >&2
    exit 1
  fi
  PAYLOAD="$(python3 -c "import json; print(json.dumps({'detail': {'source_day': '${SOURCE_DAY}'}}))")"
  echo "    source_day: ${SOURCE_DAY}"
else
  echo "    source_day: (default Lambda = ayer UTC)"
fi

echo "==> lambda invoke"
aws lambda invoke \
  --function-name "${FUNCTION_NAME}" \
  --region "${AWS_REGION}" \
  --payload "${PAYLOAD}" \
  --cli-binary-format raw-in-base64-out \
  "${OUT_FILE}" >/dev/null

echo "==> Respuesta:"
cat "${OUT_FILE}"
echo

INVOKE_ERROR="$(python3 -c "
import json, sys
with open('${OUT_FILE}') as f:
    data = json.load(f)
if isinstance(data, dict) and data.get('errorMessage'):
    print(data['errorMessage'])
    sys.exit(1)
if isinstance(data, dict) and data.get('ok') is False:
    print(data.get('error', 'unknown'))
    sys.exit(1)
" 2>/dev/null || true)"

if [[ -n "${INVOKE_ERROR}" ]]; then
  echo "ERROR: la invocación falló: ${INVOKE_ERROR}" >&2
  exit 1
fi

ENQUEUED="$(python3 -c "
import json
with open('${OUT_FILE}') as f:
    d = json.load(f)
print(d.get('enqueued', 0) if isinstance(d, dict) else 0)
" 2>/dev/null || echo "0")"

if [[ "${ENQUEUED}" == "0" ]]; then
  echo "AVISO: no se encolaron jobs (¿hay restaurants en RDS?)." >&2
fi

if [[ "${SKIP_WAIT:-0}" == "1" ]]; then
  echo "SKIP_WAIT=1: jobs encolados. Seguí la cola con:"
  echo "  aws sqs get-queue-attributes --queue-url ${QUEUE_URL} --attribute-names All --region ${AWS_REGION}"
  exit 0
fi

if [[ "${WAIT_FOR_QUEUE:-0}" != "1" && "${ENQUEUED}" == "0" ]]; then
  exit 0
fi

if [[ "${WAIT_FOR_QUEUE:-0}" != "1" ]]; then
  echo "Tip: WAIT_FOR_QUEUE=1 espera a que el worker vacíe la cola."
  echo "Modelos en s3://${ML_BUCKET}/recommendations/<tenantId>/model.bin"
  exit 0
fi

sqs_depth() {
  aws sqs get-queue-attributes \
    --queue-url "${QUEUE_URL}" \
    --region "${AWS_REGION}" \
    --attribute-names ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible \
    --query 'Attributes' \
    --output json
}

echo "==> Esperando cola ML (visible + en vuelo = 0, timeout ${WAIT_TIMEOUT_SEC}s)..."
deadline=$((SECONDS + WAIT_TIMEOUT_SEC))

while true; do
  attrs="$(sqs_depth)"
  visible="$(python3 -c "import json,sys; print(json.load(sys.stdin).get('ApproximateNumberOfMessages','0'))" <<<"${attrs}")"
  inflight="$(python3 -c "import json,sys; print(json.load(sys.stdin).get('ApproximateNumberOfMessagesNotVisible','0'))" <<<"${attrs}")"
  total=$((visible + inflight))

  if [[ "${total}" -eq 0 ]]; then
    echo "Cola vacía."
    echo "Modelos esperados en s3://${ML_BUCKET}/recommendations/<tenantId>/model.bin"
    exit 0
  fi

  if (( SECONDS >= deadline )); then
    echo "ERROR: timeout esperando cola (${WAIT_TIMEOUT_SEC}s). visible=${visible} inflight=${inflight}" >&2
    echo "Revisá CloudWatch: /aws/lambda/$(terraform -chdir="${TF_DIR}" output -raw ml_worker_function_name)" >&2
    exit 1
  fi

  echo "    ... pendientes=${visible} en_vuelo=${inflight} ($(date +%H:%M:%S))"
  sleep "${POLL_INTERVAL_SEC}"
done
