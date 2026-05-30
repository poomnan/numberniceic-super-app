-- Speed up semantic ranking on names_miracle.
-- Run this as a standalone script because CREATE INDEX CONCURRENTLY
-- cannot run inside a transaction block.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_names_miracle_meaning_vector_hnsw
    ON names_miracle
    USING hnsw (meaning_vector vector_cosine_ops)
    WHERE meaning_vector IS NOT NULL;
