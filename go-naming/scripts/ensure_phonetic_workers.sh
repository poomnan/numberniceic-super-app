#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="${ROOT_DIR}/.runtime"
mkdir -p "$RUNTIME_DIR"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  source "${ROOT_DIR}/.env"
  set +a
fi

WORKER_COUNT="${PHONETIC_WORKER_COUNT:-3}"
BATCH_SIZE="${PHONETIC_BATCH_SIZE:-60}"
REQUEST_BATCH_SIZE="${PHONETIC_REQUEST_BATCH_SIZE:-5}"
STALE_LOG_MINUTES="${PHONETIC_STALE_LOG_MINUTES:-20}"

start_worker() {
  local idx="$1"
  local log_file="${ROOT_DIR}/phonetic_batch_${idx}.log"
  local pid_file="${RUNTIME_DIR}/phonetic_batch_${idx}.pid"

  nohup bash -lc "
    cd '${ROOT_DIR}'
    set -a
    source '${ROOT_DIR}/.env'
    set +a
    exec go run ./debug_tools/phonetic_batch_worker.go -batch-size ${BATCH_SIZE} -request-batch-size ${REQUEST_BATCH_SIZE}
  " >"${log_file}" 2>&1 < /dev/null &

  echo $! > "${pid_file}"
  echo "started worker ${idx} pid=$(cat "${pid_file}") log=$(basename "${log_file}")"
}

stop_worker() {
  local idx="$1"
  local pid_file="${RUNTIME_DIR}/phonetic_batch_${idx}.pid"
  if [[ -f "${pid_file}" ]]; then
    local pid
    pid="$(cat "${pid_file}")"
    if kill -0 "${pid}" 2>/dev/null; then
      kill "${pid}" 2>/dev/null || true
      sleep 1
      kill -9 "${pid}" 2>/dev/null || true
    fi
    rm -f "${pid_file}"
  fi
}

for idx in $(seq 1 "${WORKER_COUNT}"); do
  pid_file="${RUNTIME_DIR}/phonetic_batch_${idx}.pid"
  log_file="${ROOT_DIR}/phonetic_batch_${idx}.log"
  restart_reason=""

  if [[ ! -f "${pid_file}" ]]; then
    restart_reason="missing pidfile"
  else
    pid="$(cat "${pid_file}")"
    if ! kill -0 "${pid}" 2>/dev/null; then
      restart_reason="dead pid"
    elif [[ -f "${log_file}" ]] && find "${log_file}" -mmin +"${STALE_LOG_MINUTES}" | grep -q .; then
      restart_reason="stale log"
    fi
  fi

  if [[ -n "${restart_reason}" ]]; then
    echo "worker ${idx}: ${restart_reason}, restarting"
    stop_worker "${idx}"
    start_worker "${idx}"
  else
    echo "worker ${idx}: healthy pid=$(cat "${pid_file}")"
  fi
done
