-- Speed up mobile Double Lucky ranking with day-based kalakini filtering.
-- Run this as a standalone script because CREATE INDEX CONCURRENTLY
-- cannot run inside a transaction block.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_sat_sha_k_sunday
    ON names_miracle (sat_sum, sha_sum, name_id)
    WHERE char_length(thname) BETWEEN 2 AND 8 AND k_sunday = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_sat_sha_k_monday
    ON names_miracle (sat_sum, sha_sum, name_id)
    WHERE char_length(thname) BETWEEN 2 AND 8 AND k_monday = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_sat_sha_k_tuesday
    ON names_miracle (sat_sum, sha_sum, name_id)
    WHERE char_length(thname) BETWEEN 2 AND 8 AND k_tuesday = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_sat_sha_k_wednesday1
    ON names_miracle (sat_sum, sha_sum, name_id)
    WHERE char_length(thname) BETWEEN 2 AND 8 AND k_wednesday1 = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_sat_sha_k_wednesday2
    ON names_miracle (sat_sum, sha_sum, name_id)
    WHERE char_length(thname) BETWEEN 2 AND 8 AND k_wednesday2 = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_sat_sha_k_thursday
    ON names_miracle (sat_sum, sha_sum, name_id)
    WHERE char_length(thname) BETWEEN 2 AND 8 AND k_thursday = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_sat_sha_k_friday
    ON names_miracle (sat_sum, sha_sum, name_id)
    WHERE char_length(thname) BETWEEN 2 AND 8 AND k_friday = false;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_sat_sha_k_saturday
    ON names_miracle (sat_sum, sha_sum, name_id)
    WHERE char_length(thname) BETWEEN 2 AND 8 AND k_saturday = false;
