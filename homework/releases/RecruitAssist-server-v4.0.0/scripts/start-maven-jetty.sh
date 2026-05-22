#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
PORT="${PORT:-8080}"
BIND_HOST="${BIND_HOST:-127.0.0.1}"

export RECRUITASSIST_BASE_DIR="${RELEASE_DIR}"

echo "Starting RecruitAssist from ${RELEASE_DIR}"
echo "HTTP bind: ${BIND_HOST}:${PORT}"
echo "RECRUITASSIST_BASE_DIR=${RECRUITASSIST_BASE_DIR}"

exec mvn -f "${RELEASE_DIR}/framework/recruitassist-web/pom.xml" \
  org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.0.15:run \
  -Djetty.host="${BIND_HOST}" \
  -Djetty.http.host="${BIND_HOST}" \
  -Djetty.http.port="${PORT}"
