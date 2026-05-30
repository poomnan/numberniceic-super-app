#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATE_FILE="${PHONETIC_PROGRESS_STATE_FILE:-${ROOT_DIR}/phonetic_progress_state.tsv}"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  source "${ROOT_DIR}/.env"
  set +a
fi

if [[ -z "${DATABASE_URL:-}" ]]; then
  echo "DATABASE_URL is not set" >&2
  exit 1
fi

now_epoch="$(date +%s)"
read -r total processed pending failed claimed percent <<<"$(
  psql "$DATABASE_URL" -At -F $'\t' -c "
    SELECT
      COUNT(*) AS total_names,
      COUNT(*) FILTER (WHERE phonetic_processed = TRUE) AS processed_count,
      COUNT(*) FILTER (WHERE phonetic_processed IS DISTINCT FROM TRUE) AS pending_count,
      COUNT(*) FILTER (WHERE phonetic_error IS NOT NULL AND phonetic_processed IS DISTINCT FROM TRUE) AS failed_waiting_retry,
      COUNT(*) FILTER (WHERE phonetic_processing_started_at IS NOT NULL) AS currently_claimed,
      ROUND(100.0 * COUNT(*) FILTER (WHERE phonetic_processed = TRUE) / NULLIF(COUNT(*), 0), 2) AS processed_percent
    FROM names_miracle;
  "
)"

rate_per_min="n/a"
eta_text="n/a"
window_text="first sample"

if [[ -f "$STATE_FILE" ]]; then
  IFS=$'\t' read -r prev_epoch prev_processed < "$STATE_FILE" || true
  if [[ -n "${prev_epoch:-}" && -n "${prev_processed:-}" ]]; then
    delta_time=$(( now_epoch - prev_epoch ))
    delta_processed=$(( processed - prev_processed ))
    if (( delta_time > 0 && delta_processed > 0 )); then
      rate_per_sec="$(awk "BEGIN { printf \"%.4f\", ${delta_processed}/${delta_time} }")"
      rate_per_min="$(awk "BEGIN { printf \"%.2f\", (${delta_processed}/${delta_time})*60 }")"
      eta_seconds="$(awk "BEGIN { printf \"%.0f\", ${pending}/(${delta_processed}/${delta_time}) }")"
      eta_hours="$(awk "BEGIN { printf \"%.2f\", ${eta_seconds}/3600 }")"
      eta_days="$(awk "BEGIN { printf \"%.2f\", ${eta_seconds}/86400 }")"
      window_minutes="$(awk "BEGIN { printf \"%.2f\", ${delta_time}/60 }")"
      eta_text="${eta_hours} hours (~${eta_days} days)"
      window_text="${delta_processed} names / ${window_minutes} min (${rate_per_sec}/sec)"
    fi
  fi
fi

printf "%s\t%s\n" "$now_epoch" "$processed" > "$STATE_FILE"

cat <<EOF
Phonetic Progress
root_dir: ${ROOT_DIR}
total_names: ${total}
processed: ${processed}
pending: ${pending}
failed_waiting_retry: ${failed}
currently_claimed: ${claimed}
processed_percent: ${percent}%
recent_window: ${window_text}
rate_per_min: ${rate_per_min}
eta: ${eta_text}
state_file: ${STATE_FILE}
EOF
