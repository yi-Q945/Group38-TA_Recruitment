#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1}"

echo "Checking ${BASE_URL}/health"
curl -fsS "${BASE_URL}/health"
echo
