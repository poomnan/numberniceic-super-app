-- Speed up mobile double-good keyword search.
-- Safe to run on production PostgreSQL as a standalone script.
-- Note: CREATE INDEX CONCURRENTLY must not run inside a transaction block.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_sat_sha
    ON names_miracle (sat_sum, sha_sum)
    WHERE char_length(thname) BETWEEN 2 AND 8;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_sat_sum_len
    ON names_miracle (sat_sum)
    WHERE char_length(thname) BETWEEN 2 AND 8;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_sha_sum_len
    ON names_miracle (sha_sum)
    WHERE char_length(thname) BETWEEN 2 AND 8;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_thname_trgm
    ON names_miracle USING gin (thname gin_trgm_ops);
