#!/usr/bin/env bash
set -euo pipefail

cd /root/go-naming

mkdir -p /root/go-naming/.go-build

export GOCACHE=/root/go-naming/.go-build
export DATABASE_URL="postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable"
export TYPHOON_API_KEY="sk-9Rqp9KK37uk97T8T4q5HXk74QbZikoD6hQSMcwG7melhxzpK"
export TYPHOON_MODEL="typhoon-v2.5-30b-a3b-instruct"
export TYPHOON_PHONETIC_VERSION="typhoon_v2_name_euphony_json"

nohup /usr/bin/go run debug_tools/phonetic_batch_worker.go \
  -batch-size 50 \
  -request-batch-size 5 \
  -request-timeout 90s \
  -worker-id phonetic-server-1 \
  -checkpoint-file phonetic_server_checkpoint_1.json \
  >/root/go-naming/phonetic_server_1.log 2>&1 </dev/null &
echo $! > /root/go-naming/.phonetic_server_1.pid

nohup /usr/bin/go run debug_tools/phonetic_batch_worker.go \
  -batch-size 50 \
  -request-batch-size 5 \
  -request-timeout 90s \
  -worker-id phonetic-server-2 \
  -checkpoint-file phonetic_server_checkpoint_2.json \
  >/root/go-naming/phonetic_server_2.log 2>&1 </dev/null &
echo $! > /root/go-naming/.phonetic_server_2.pid

echo "started:"
cat /root/go-naming/.phonetic_server_1.pid
cat /root/go-naming/.phonetic_server_2.pid
