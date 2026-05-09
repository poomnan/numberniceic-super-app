-- Add offline phonetic-precompute columns for Thai name fluency / euphony scoring.
-- Safe to run on production PostgreSQL as a standalone script.
-- Note: CREATE INDEX CONCURRENTLY must not run inside a transaction block.

ALTER TABLE names_miracle
    ADD COLUMN IF NOT EXISTS phonetic_score INTEGER,
    ADD COLUMN IF NOT EXISTS pronunciation_ease INTEGER,
    ADD COLUMN IF NOT EXISTS euphony_score INTEGER,
    ADD COLUMN IF NOT EXISTS rhythm_score INTEGER,
    ADD COLUMN IF NOT EXISTS phonetic_summary TEXT,
    ADD COLUMN IF NOT EXISTS phonetic_labels JSONB,
    ADD COLUMN IF NOT EXISTS phonetic_issues JSONB,
    ADD COLUMN IF NOT EXISTS phonetic_style_tone JSONB,
    ADD COLUMN IF NOT EXISTS phonetic_raw JSONB,
    ADD COLUMN IF NOT EXISTS phonetic_processed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS phonetic_error TEXT,
    ADD COLUMN IF NOT EXISTS phonetic_retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS phonetic_version TEXT,
    ADD COLUMN IF NOT EXISTS phonetic_updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS phonetic_next_retry_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS phonetic_processing_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS phonetic_processing_by TEXT;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_phonetic_pending
    ON names_miracle (name_id, phonetic_next_retry_at, phonetic_processing_started_at)
    WHERE phonetic_processed IS DISTINCT FROM TRUE;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_phonetic_score
    ON names_miracle (phonetic_score DESC)
    WHERE phonetic_score IS NOT NULL;
