# Phonetic Batch Worker

Offline precompute job for Thai-name phonetic quality. This job reads names from `names_miracle`, sends each Thai name to Typhoon for pronunciation / euphony scoring, and stores the structured result back into PostgreSQL for later reranking.

## What it writes

The migration adds these columns directly on `names_miracle`:

- `phonetic_score`
- `pronunciation_ease`
- `euphony_score`
- `rhythm_score`
- `phonetic_summary`
- `phonetic_labels`
- `phonetic_issues`
- `phonetic_style_tone`
- `phonetic_raw`
- `phonetic_processed`
- `phonetic_error`
- `phonetic_retry_count`
- `phonetic_version`
- `phonetic_updated_at`
- `phonetic_next_retry_at`
- `phonetic_processing_started_at`
- `phonetic_processing_by`

This keeps future reranking simple because the main search query can read the phonetic layer from the same row without adding a new join path.

## 1. Apply migration

Run this SQL on PostgreSQL:

```bash
psql "$DATABASE_URL" -f scripts/sql/20260409_add_names_miracle_phonetic_columns.sql
```

## 2. Configure env

Copy `.env.example` to `.env` and set:

- `DATABASE_URL`
- `TYPHOON_API_KEY`
- `TYPHOON_API_URL`
- `TYPHOON_MODEL`

## 3. Dry run

Preview pending names without writing DB:

```bash
./scripts/run_phonetic_worker.sh -dry-run -limit 20
```

## 4. Normal run

```bash
./scripts/run_phonetic_worker.sh -batch-size 50
```

Useful flags:

- `-batch-size 50`
- `-limit 200`
- `-max-retries 3`
- `-request-timeout 45s`
- `-request-delay 750ms`
- `-batch-delay 2s`
- `-retry-backoff 15m`
- `-claim-timeout 20m`
- `-checkpoint-file phonetic_worker_checkpoint.json`
- `-version typhoon_v1_promptA`

## 5. Background run on server

Recommended:

```bash
cd /home/tayap/go-naming
nohup ./scripts/run_phonetic_worker.sh -batch-size 50 > phonetic_worker.log 2>&1 &
tail -f phonetic_worker.log
```

Or build first:

```bash
cd /home/tayap/go-naming
go build -o phonetic-worker ./debug_tools/phonetic_batch_worker.go
nohup ./phonetic-worker -batch-size 50 > phonetic_worker.log 2>&1 &
```

## 6. Progress / ETA

Check live batch progress and rough ETA:

```bash
cd /home/tayap/go-naming
./scripts/phonetic_progress.sh
```

This script stores the last sample in `phonetic_progress_state.tsv` and computes:

- `processed`
- `pending`
- `failed_waiting_retry`
- `currently_claimed`
- `processed_percent`
- recent throughput window
- ETA based on the last sample delta

Run it again after a few minutes for a more accurate ETA.

## 7. Worker watchdog

Keep a fixed number of workers alive:

```bash
cd /home/tayap/go-naming
PHONETIC_WORKER_COUNT=3 ./scripts/ensure_phonetic_workers.sh
```

This script:

- keeps PID files in `.runtime/`
- restarts missing workers
- restarts workers whose log has gone stale
- uses `.env` for batch size and timeout config

You can put it in cron every 5 minutes:

```bash
*/5 * * * * cd /home/tayap/go-naming && ./scripts/ensure_phonetic_workers.sh >> watchdog.log 2>&1
```

## Resume behavior

- A row is processed once `phonetic_processed = true`
- Failed rows increment `phonetic_retry_count`
- Failed rows are delayed by `phonetic_next_retry_at`
- Claimed-but-stuck rows become available again after `phonetic_processing_started_at` is older than `claim-timeout`
- Progress is also written to the checkpoint file for quick inspection

This makes the worker safe to stop and start again without reprocessing already-finished rows.
