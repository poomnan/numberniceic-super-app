-- Enable pgvector extension just in case
CREATE EXTENSION IF NOT EXISTS vector;

-- Drop the old column if it exists to avoid type mismatch issues
ALTER TABLE names_miracle DROP COLUMN IF EXISTS meaning_vector;

-- Add the new column with 1536 dimensions for OpenAI embeddings
ALTER TABLE names_miracle ADD COLUMN meaning_vector vector(1536);

-- Create an index for faster similarity search (optional, can be done later)
-- CREATE INDEX ON names_miracle USING hnsw (meaning_vector vector_cosine_ops);
