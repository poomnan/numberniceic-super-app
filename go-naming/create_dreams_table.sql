-- Create dreams table (Basic version without pgvector for now)
CREATE TABLE IF NOT EXISTS dreams (
    dream_id SERIAL PRIMARY KEY,
    dream_keyword TEXT UNIQUE NOT NULL,
    dream_interpretation TEXT NOT NULL,
    lucky_numbers TEXT,
    view_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    meaning_vector TEXT, -- Store as JSON array string for now
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_dreams_keyword ON dreams(dream_keyword);
CREATE INDEX IF NOT EXISTS idx_dreams_active ON dreams(is_active);
