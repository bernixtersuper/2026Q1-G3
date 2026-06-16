#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST="${ROOT}/lambda_dist/processor"
rm -rf "${DIST}"
mkdir -p "${DIST}"
cp "${ROOT}/processor_lambda.py" "${DIST}/"
echo "Listo: ${DIST} (processor_lambda.handler)"
