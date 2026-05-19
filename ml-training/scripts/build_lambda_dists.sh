#!/usr/bin/env bash
# Empaqueta Lambdas ML (orquestador + worker) para runtime Python 3.12 / Linux x86_64.
#
# Por defecto corre dentro de Docker (misma imagen que Amazon Linux) para evitar
# psycopg2 compilado para el SO del host (error: No module named 'psycopg2._psycopg').
#
# Requisito: Docker en PATH.
# Escape hatch sin Docker: LAMBDA_BUILD_NATIVE=1 bash scripts/build_lambda_dists.sh (SOLO EN LINUX)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IMAGE="${LAMBDA_BUILD_IMAGE:-public.ecr.aws/sam/build-python3.12}"

if [[ "${LAMBDA_BUILD_NATIVE:-0}" != "1" ]]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: Docker es obligatorio para empaquetar las Lambdas ML." >&2
    echo "       Instalá Docker o usá: LAMBDA_BUILD_NATIVE=1 bash scripts/build_lambda_dists.sh" >&2
    exit 1
  fi
  echo "==> Empaquetando Lambdas en Docker (${IMAGE})..."
  exec docker run --rm \
    -v "${ROOT}:/opt/ml" \
    -w /opt/ml \
    -e LAMBDA_BUILD_NATIVE=1 \
    "${IMAGE}" \
    bash scripts/build_lambda_dists.sh
fi

DIST="${ROOT}/lambda_dist"
rm -rf "${DIST}"
mkdir -p "${DIST}/orchestrator" "${DIST}/worker"

cp "${ROOT}/orchestrator_lambda.py" "${DIST}/orchestrator/"
cp "${ROOT}/worker_lambda.py" "${DIST}/worker/"

echo "Instalando psycopg2-binary en orquestador..."
if [[ "${LAMBDA_BUILD_NATIVE:-0}" == "1" ]] && [[ -f /etc/os-release ]] && grep -q "Amazon Linux" /etc/os-release 2>/dev/null; then
  pip install --disable-pip-version-check -q -t "${DIST}/orchestrator" 'psycopg2-binary>=2.9'
else
  LAMBDA_PLATFORM="${LAMBDA_PLATFORM:-manylinux2014_x86_64}"
  LAMBDA_PY="${LAMBDA_PY:-3.12}"
  pip install \
    --disable-pip-version-check \
    -q \
    -t "${DIST}/orchestrator" \
    --upgrade \
    --platform "${LAMBDA_PLATFORM}" \
    --implementation cp \
    --python-version "${LAMBDA_PY}" \
    --only-binary=:all: \
    'psycopg2-binary>=2.9'
fi

if ! find "${DIST}/orchestrator" -name '_psycopg*.so' | grep -q .; then
  echo "ERROR: no se encontró el binario _psycopg. Revisá el build (Docker recomendado)." >&2
  exit 1
fi

echo "Worker: sin pip (boto3/botocore vienen en el runtime Lambda)"
echo "Listo: ${DIST}/orchestrator (orchestrator_lambda.handler)"
echo "       ${DIST}/worker (worker_lambda.handler)"
