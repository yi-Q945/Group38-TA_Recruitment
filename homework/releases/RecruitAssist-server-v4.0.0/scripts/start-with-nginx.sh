#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOG_DIR="${RELEASE_DIR}/logs/server"
JETTY_PID_FILE="${LOG_DIR}/jetty.pid"
NGINX_PID_FILE="/run/nginx.pid"

mkdir -p "${LOG_DIR}"

if [[ -f "${JETTY_PID_FILE}" ]]; then
  OLD_PID="$(cat "${JETTY_PID_FILE}" || true)"
  if [[ -n "${OLD_PID}" ]] && kill -0 "${OLD_PID}" 2>/dev/null; then
    kill "${OLD_PID}" || true
    sleep 3
    if kill -0 "${OLD_PID}" 2>/dev/null; then
      kill -9 "${OLD_PID}" || true
    fi
  fi
fi

if [[ -f "${NGINX_PID_FILE}" ]]; then
  OLD_NGINX_PID="$(cat "${NGINX_PID_FILE}" || true)"
  if [[ -n "${OLD_NGINX_PID}" ]] && kill -0 "${OLD_NGINX_PID}" 2>/dev/null; then
    nginx -s quit || true
    sleep 2
  fi
fi

nohup env JAVA_HOME=/usr/lib/jvm/java-17-openjdk \
  PATH=/usr/lib/jvm/java-17-openjdk/bin:/usr/share/maven/bin:$PATH \
  PORT=8080 \
  BIND_HOST=127.0.0.1 \
  "${SCRIPT_DIR}/start-maven-jetty.sh" > "${LOG_DIR}/jetty.log" 2>&1 &

echo $! > "${JETTY_PID_FILE}"

sleep 5
nginx
sleep 2

BASE_URL="http://127.0.0.1" "${SCRIPT_DIR}/check-health.sh"
