package main

import (
	"context"
	"database/sql"
	"encoding/json"
	"flag"
	"fmt"
	"go-naming/services"
	"log"
	"os"
	"strings"
	"syscall"
	"time"

	_ "github.com/lib/pq"
)

type workerConfig struct {
	DatabaseURL      string
	BatchSize        int
	RequestBatchSize int
	Limit            int
	MaxRetries       int
	ClaimTimeout     time.Duration
	RequestTimeout   time.Duration
	RequestDelay     time.Duration
	BatchDelay       time.Duration
	RetryBackoff     time.Duration
	DryRun           bool
	WorkerID         string
	CheckpointFile   string
	Version          string
	LogEvery         int
}

type claimedName struct {
	ID   int64
	Name string
}

type workerStats struct {
	Claimed      int       `json:"claimed"`
	Processed    int       `json:"processed"`
	Failed       int       `json:"failed"`
	Retried      int       `json:"retried"`
	Skipped      int       `json:"skipped"`
	LastNameID   int64     `json:"last_name_id"`
	LastName     string    `json:"last_name"`
	LastError    string    `json:"last_error,omitempty"`
	UpdatedAt    time.Time `json:"updated_at"`
	WorkerID     string    `json:"worker_id"`
	Version      string    `json:"version"`
	DryRun       bool      `json:"dry_run"`
	LimitReached bool      `json:"limit_reached"`
}

func main() {
	cfg := loadWorkerConfig()

	db, err := openPostgres(cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("open postgres failed: %v", err)
	}
	defer db.Close()

	var client *services.TyphoonPhoneticClient
	if !cfg.DryRun {
		client, err = services.NewTyphoonPhoneticClientFromEnv(cfg.RequestTimeout)
		if err != nil {
			log.Fatalf("create typhoon client failed: %v", err)
		}
		if cfg.Version == "" {
			cfg.Version = client.Version
		}
	}

	stats := &workerStats{
		WorkerID: cfg.WorkerID,
		Version:  cfg.Version,
		DryRun:   cfg.DryRun,
	}

	log.Printf("[phonetic-worker] start worker_id=%s batch_size=%d max_retries=%d dry_run=%v limit=%d version=%s",
		cfg.WorkerID, cfg.BatchSize, cfg.MaxRetries, cfg.DryRun, cfg.Limit, cfg.Version)

	remaining := cfg.Limit
	for {
		batchSize := cfg.BatchSize
		if remaining > 0 && remaining < batchSize {
			batchSize = remaining
		}

		var batch []claimedName
		if cfg.DryRun {
			batch, err = previewBatch(db, batchSize, cfg.MaxRetries, cfg.ClaimTimeout)
		} else {
			batch, err = claimBatch(db, batchSize, cfg.MaxRetries, cfg.ClaimTimeout, cfg.WorkerID)
		}
		if err != nil {
			log.Fatalf("claim batch failed: %v", err)
		}
		if len(batch) == 0 {
			log.Printf("[phonetic-worker] no pending names left")
			break
		}

		stats.Claimed += len(batch)
		log.Printf("[phonetic-worker] claimed batch=%d", len(batch))

		for start := 0; start < len(batch); start += cfg.RequestBatchSize {
			end := start + cfg.RequestBatchSize
			if end > len(batch) {
				end = len(batch)
			}
			group := batch[start:end]

			if cfg.DryRun {
				for _, item := range group {
					stats.LastNameID = item.ID
					stats.LastName = item.Name
					log.Printf("[phonetic-worker] dry-run id=%d name=%s", item.ID, item.Name)
					stats.Skipped++
					writeCheckpoint(cfg.CheckpointFile, stats)
				}
				continue
			}

			rawResponse, resultMap, attempts, err := evaluateBatchWithRetry(client, group, cfg)
			if attempts > 1 {
				stats.Retried += attempts - 1
			}

			if err != nil {
				log.Printf("[phonetic-worker] batch fallback to single-name mode names=%d error=%v", len(group), err)
				for _, item := range group {
					evaluation, rawSingle, singleAttempts, singleErr := evaluateWithRetry(client, item.Name, cfg)
					if singleAttempts > 1 {
						stats.Retried += singleAttempts - 1
					}
					stats.LastNameID = item.ID
					stats.LastName = item.Name
					if singleErr != nil {
						stats.Failed++
						stats.LastError = singleErr.Error()
						log.Printf("[phonetic-worker] failed id=%d name=%s attempts=%d error=%v", item.ID, item.Name, singleAttempts, singleErr)
						if updateErr := markFailure(db, item.ID, cfg.WorkerID, rawSingle, singleErr, cfg.RetryBackoff); updateErr != nil {
							log.Printf("[phonetic-worker] mark failure error id=%d: %v", item.ID, updateErr)
						}
						writeCheckpoint(cfg.CheckpointFile, stats)
						continue
					}
					stats.Processed++
					stats.LastError = ""
					log.Printf("[phonetic-worker] processed id=%d name=%s overall=%d ease=%d euphony=%d rhythm=%d",
						item.ID, item.Name, evaluation.Score.Overall, evaluation.Score.PronunciationEase, evaluation.Score.Euphony, evaluation.Score.Rhythm)
					if updateErr := markSuccess(db, item.ID, cfg.WorkerID, cfg.Version, evaluation, rawSingle); updateErr != nil {
						log.Printf("[phonetic-worker] mark success error id=%d: %v", item.ID, updateErr)
					}
					writeCheckpoint(cfg.CheckpointFile, stats)
				}
				continue
			}

			if err == nil {
				for _, item := range group {
					stats.LastNameID = item.ID
					stats.LastName = item.Name
					evaluation, ok := resultMap[item.Name]
					if !ok {
						errMissing := fmt.Errorf("missing batch result for name=%s", item.Name)
						stats.Failed++
						stats.LastError = errMissing.Error()
						log.Printf("[phonetic-worker] failed id=%d name=%s attempts=%d error=%v", item.ID, item.Name, attempts, errMissing)
						if updateErr := markFailure(db, item.ID, cfg.WorkerID, rawResponse, errMissing, cfg.RetryBackoff); updateErr != nil {
							log.Printf("[phonetic-worker] mark failure error id=%d: %v", item.ID, updateErr)
						}
						writeCheckpoint(cfg.CheckpointFile, stats)
						continue
					}

					stats.Processed++
					stats.LastError = ""
					log.Printf("[phonetic-worker] processed id=%d name=%s overall=%d ease=%d euphony=%d rhythm=%d",
						item.ID, item.Name, evaluation.Score.Overall, evaluation.Score.PronunciationEase, evaluation.Score.Euphony, evaluation.Score.Rhythm)
					if updateErr := markSuccess(db, item.ID, cfg.WorkerID, cfg.Version, evaluation, rawResponse); updateErr != nil {
						log.Printf("[phonetic-worker] mark success error id=%d: %v", item.ID, updateErr)
					}
					writeCheckpoint(cfg.CheckpointFile, stats)
				}
			}

			if cfg.RequestDelay > 0 && end < len(batch) {
				time.Sleep(cfg.RequestDelay)
			}
		}

		if remaining > 0 {
			remaining -= len(batch)
			if remaining <= 0 {
				stats.LimitReached = true
				log.Printf("[phonetic-worker] limit reached")
				break
			}
		}

		if cfg.BatchDelay > 0 {
			time.Sleep(cfg.BatchDelay)
		}
	}

	writeCheckpoint(cfg.CheckpointFile, stats)
	log.Printf("[phonetic-worker] done processed=%d failed=%d retried=%d skipped=%d claimed=%d",
		stats.Processed, stats.Failed, stats.Retried, stats.Skipped, stats.Claimed)
}

func loadWorkerConfig() workerConfig {
	var cfg workerConfig

	flag.StringVar(&cfg.DatabaseURL, "database-url", getenvDefault("DATABASE_URL", "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable"), "PostgreSQL connection string")
	flag.IntVar(&cfg.BatchSize, "batch-size", getenvInt("PHONETIC_BATCH_SIZE", 50), "Number of names to claim per batch")
	flag.IntVar(&cfg.RequestBatchSize, "request-batch-size", getenvInt("PHONETIC_REQUEST_BATCH_SIZE", 3), "Number of names to send to Typhoon per request")
	flag.IntVar(&cfg.Limit, "limit", getenvInt("PHONETIC_LIMIT", 0), "Optional max number of records to process in this run (0 = unlimited)")
	flag.IntVar(&cfg.MaxRetries, "max-retries", getenvInt("PHONETIC_MAX_RETRIES", 3), "Max DB retry count before record is skipped")
	flag.DurationVar(&cfg.ClaimTimeout, "claim-timeout", getenvDuration("PHONETIC_CLAIM_TIMEOUT", 20*time.Minute), "How long before a claimed row can be resumed by another run")
	flag.DurationVar(&cfg.RequestTimeout, "request-timeout", getenvDuration("PHONETIC_REQUEST_TIMEOUT", 45*time.Second), "Typhoon request timeout")
	flag.DurationVar(&cfg.RequestDelay, "request-delay", getenvDuration("PHONETIC_REQUEST_DELAY", 750*time.Millisecond), "Sleep between names")
	flag.DurationVar(&cfg.BatchDelay, "batch-delay", getenvDuration("PHONETIC_BATCH_DELAY", 2*time.Second), "Sleep between batches")
	flag.DurationVar(&cfg.RetryBackoff, "retry-backoff", getenvDuration("PHONETIC_RETRY_BACKOFF", 15*time.Minute), "How long to wait before retrying failed rows")
	flag.BoolVar(&cfg.DryRun, "dry-run", false, "Preview pending rows without writing DB")
	flag.StringVar(&cfg.WorkerID, "worker-id", defaultWorkerID(), "Worker identifier stored in DB")
	flag.StringVar(&cfg.CheckpointFile, "checkpoint-file", getenvDefault("PHONETIC_CHECKPOINT_FILE", "phonetic_worker_checkpoint.json"), "Checkpoint file path")
	flag.StringVar(&cfg.Version, "version", getenvDefault("TYPHOON_PHONETIC_VERSION", services.DefaultTyphoonPhoneticVersion), "Version tag stored in DB")
	flag.IntVar(&cfg.LogEvery, "log-every", getenvInt("PHONETIC_LOG_EVERY", 1), "Reserved for future log grouping")
	flag.Parse()

	return cfg
}

func openPostgres(databaseURL string) (*sql.DB, error) {
	db, err := sql.Open("postgres", databaseURL)
	if err != nil {
		return nil, err
	}
	db.SetMaxOpenConns(5)
	db.SetMaxIdleConns(2)
	db.SetConnMaxLifetime(5 * time.Minute)
	if err := db.Ping(); err != nil {
		db.Close()
		return nil, err
	}
	return db, nil
}

func previewBatch(db *sql.DB, batchSize, maxRetries int, claimTimeout time.Duration) ([]claimedName, error) {
	rows, err := db.Query(`
		SELECT name_id, thname
		FROM names_miracle
		WHERE COALESCE(thname, '') <> ''
		  AND phonetic_processed IS DISTINCT FROM TRUE
		  AND COALESCE(phonetic_retry_count, 0) < $1
		  AND (phonetic_next_retry_at IS NULL OR phonetic_next_retry_at <= NOW())
		  AND (
		        phonetic_processing_started_at IS NULL
		        OR phonetic_processing_started_at < NOW() - ($2 * INTERVAL '1 second')
		      )
		ORDER BY name_id
		LIMIT $3
	`, maxRetries, int(claimTimeout.Seconds()), batchSize)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var batch []claimedName
	for rows.Next() {
		var item claimedName
		if err := rows.Scan(&item.ID, &item.Name); err != nil {
			return nil, err
		}
		batch = append(batch, item)
	}
	return batch, rows.Err()
}

func claimBatch(db *sql.DB, batchSize, maxRetries int, claimTimeout time.Duration, workerID string) ([]claimedName, error) {
	tx, err := db.Begin()
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()

	rows, err := tx.Query(`
		WITH candidates AS (
			SELECT name_id
			FROM names_miracle
			WHERE COALESCE(thname, '') <> ''
			  AND phonetic_processed IS DISTINCT FROM TRUE
			  AND COALESCE(phonetic_retry_count, 0) < $1
			  AND (phonetic_next_retry_at IS NULL OR phonetic_next_retry_at <= NOW())
			  AND (
			        phonetic_processing_started_at IS NULL
			        OR phonetic_processing_started_at < NOW() - ($2 * INTERVAL '1 second')
			      )
			ORDER BY name_id
			LIMIT $3
			FOR UPDATE SKIP LOCKED
		)
		UPDATE names_miracle nm
		SET phonetic_processing_started_at = NOW(),
		    phonetic_processing_by = $4
		FROM candidates
		WHERE nm.name_id = candidates.name_id
		RETURNING nm.name_id, nm.thname
	`, maxRetries, int(claimTimeout.Seconds()), batchSize, workerID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var batch []claimedName
	for rows.Next() {
		var item claimedName
		if err := rows.Scan(&item.ID, &item.Name); err != nil {
			return nil, err
		}
		batch = append(batch, item)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}

	if err := tx.Commit(); err != nil {
		return nil, err
	}
	return batch, nil
}

func evaluateWithRetry(client *services.TyphoonPhoneticClient, name string, cfg workerConfig) (services.PhoneticEvaluation, json.RawMessage, int, error) {
	var (
		evaluation services.PhoneticEvaluation
		raw        json.RawMessage
		err        error
	)

	for attempt := 1; attempt <= cfg.MaxRetries; attempt++ {
		ctx, cancel := context.WithTimeout(context.Background(), cfg.RequestTimeout)
		evaluation, raw, err = client.EvaluateThaiName(ctx, name)
		cancel()
		if err == nil {
			return evaluation, raw, attempt, nil
		}

		if attempt < cfg.MaxRetries {
			sleepFor := time.Duration(attempt) * time.Second
			log.Printf("[phonetic-worker] retry name=%s attempt=%d/%d after=%s error=%v", name, attempt, cfg.MaxRetries, sleepFor, err)
			time.Sleep(sleepFor)
		}
	}

	return evaluation, raw, cfg.MaxRetries, err
}

func evaluateBatchWithRetry(client *services.TyphoonPhoneticClient, items []claimedName, cfg workerConfig) (json.RawMessage, map[string]services.PhoneticEvaluation, int, error) {
	resultMap := make(map[string]services.PhoneticEvaluation, len(items))
	names := make([]string, 0, len(items))
	for _, item := range items {
		names = append(names, item.Name)
	}

	var (
		raw     json.RawMessage
		err     error
		results []services.BatchPhoneticEvaluation
	)

	for attempt := 1; attempt <= cfg.MaxRetries; attempt++ {
		ctx, cancel := context.WithTimeout(context.Background(), cfg.RequestTimeout)
		results, raw, err = client.EvaluateThaiNamesBatch(ctx, names)
		cancel()
		if err == nil {
			for _, result := range results {
				resultMap[result.Name] = result.PhoneticEvaluation
			}
			return raw, resultMap, attempt, nil
		}

		if attempt < cfg.MaxRetries {
			sleepFor := time.Duration(attempt) * time.Second
			log.Printf("[phonetic-worker] retry batch names=%d attempt=%d/%d after=%s error=%v", len(items), attempt, cfg.MaxRetries, sleepFor, err)
			time.Sleep(sleepFor)
		}
	}

	return raw, resultMap, cfg.MaxRetries, err
}

func markSuccess(db *sql.DB, nameID int64, workerID, version string, evaluation services.PhoneticEvaluation, rawResponse json.RawMessage) error {
	labelsJSON, _ := json.Marshal(evaluation.Labels)
	issuesJSON, _ := json.Marshal(evaluation.Issues)
	styleJSON, _ := json.Marshal(evaluation.StyleTone)
	rawJSON, _ := json.Marshal(map[string]interface{}{
		"worker_id":         workerID,
		"version":           version,
		"provider_response": json.RawMessage(rawResponse),
		"parsed":            evaluation,
		"saved_at":          time.Now().UTC(),
	})

	_, err := db.Exec(`
		UPDATE names_miracle
		SET phonetic_score = $1,
		    pronunciation_ease = $2,
		    euphony_score = $3,
		    rhythm_score = $4,
		    phonetic_summary = $5,
		    phonetic_labels = $6::jsonb,
		    phonetic_issues = $7::jsonb,
		    phonetic_style_tone = $8::jsonb,
		    phonetic_raw = $9::jsonb,
		    phonetic_processed = TRUE,
		    phonetic_error = NULL,
		    phonetic_next_retry_at = NULL,
		    phonetic_processing_started_at = NULL,
		    phonetic_processing_by = NULL,
		    phonetic_version = $10,
		    phonetic_updated_at = NOW()
		WHERE name_id = $11
	`, evaluation.Score.Overall, evaluation.Score.PronunciationEase, evaluation.Score.Euphony,
		evaluation.Score.Rhythm, evaluation.Summary, string(labelsJSON), string(issuesJSON),
		string(styleJSON), string(rawJSON), version, nameID)
	return err
}

func markFailure(db *sql.DB, nameID int64, workerID string, rawResponse json.RawMessage, cause error, retryBackoff time.Duration) error {
	rawJSON, _ := json.Marshal(map[string]interface{}{
		"worker_id":         workerID,
		"provider_response": json.RawMessage(rawResponse),
		"error":             cause.Error(),
		"failed_at":         time.Now().UTC(),
	})

	_, err := db.Exec(`
		UPDATE names_miracle
		SET phonetic_error = $1,
		    phonetic_retry_count = COALESCE(phonetic_retry_count, 0) + 1,
		    phonetic_raw = $2::jsonb,
		    phonetic_processed = FALSE,
		    phonetic_next_retry_at = NOW() + ($3 * INTERVAL '1 second'),
		    phonetic_processing_started_at = NULL,
		    phonetic_processing_by = NULL,
		    phonetic_updated_at = NOW()
		WHERE name_id = $4
	`, cause.Error(), string(rawJSON), int(retryBackoff.Seconds()), nameID)
	return err
}

func writeCheckpoint(path string, stats *workerStats) {
	if strings.TrimSpace(path) == "" {
		return
	}
	stats.UpdatedAt = time.Now().UTC()
	data, err := json.MarshalIndent(stats, "", "  ")
	if err != nil {
		log.Printf("[phonetic-worker] checkpoint marshal error: %v", err)
		return
	}
	if err := os.WriteFile(path, data, 0o644); err != nil {
		log.Printf("[phonetic-worker] checkpoint write error: %v", err)
	}
}

func defaultWorkerID() string {
	host, _ := os.Hostname()
	return fmt.Sprintf("%s-%d", host, syscall.Getpid())
}

func getenvDefault(key, fallback string) string {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	return value
}

func getenvInt(key string, fallback int) int {
	var value int
	if _, err := fmt.Sscanf(strings.TrimSpace(os.Getenv(key)), "%d", &value); err == nil && value > 0 {
		return value
	}
	return fallback
}

func getenvDuration(key string, fallback time.Duration) time.Duration {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	parsed, err := time.ParseDuration(value)
	if err != nil {
		return fallback
	}
	return parsed
}
