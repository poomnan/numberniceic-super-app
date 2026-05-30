#!/usr/bin/env python3
"""
Embed Dreams using OpenAI text-embedding-3-small (1536 dimensions)
Combines dream_keyword + interpretation for rich semantic vectors
"""
import os
import time
import psycopg2
from openai import OpenAI

# Configuration
DB_HOST = "43.228.85.200"
DB_NAME = "tayap"
DB_USER = "tayap"
DB_PASS = "IntelliP24.X"
DB_PORT = "5432"

OPENAI_API_KEY = "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"
EMBED_MODEL = "text-embedding-3-small"

client = OpenAI(api_key=OPENAI_API_KEY)

def get_db_connection():
    return psycopg2.connect(
        host=DB_HOST,
        database=DB_NAME,
        user=DB_USER,
        password=DB_PASS,
        port=DB_PORT
    )

def process_embed_batch(rows):
    """Process a batch of dreams for embedding"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # Prepare input texts combining keyword + interpretation
    # Format: "ฝัน: [keyword] ความหมาย: [interpretation]"
    inputs = []
    ids = []
    
    for dream_id, keyword, interpretation in rows:
        rich_text = f"ฝัน: {keyword} ความหมาย: {interpretation}"
        inputs.append(rich_text)
        ids.append(dream_id)
    
    try:
        response = client.embeddings.create(
            input=inputs,
            model=EMBED_MODEL
        )
        
        for i, data in enumerate(response.data):
            vector = data.embedding
            dream_id = ids[i]
            vector_str = "[" + ",".join(map(str, vector)) + "]"
            cursor.execute(
                "UPDATE dreams SET meaning_vector = %s WHERE dream_id = %s", 
                (vector_str, dream_id)
            )
        
        conn.commit()
        print(f"[OpenAI-Dreams] Encoded {len(rows)} dreams", flush=True)
        
    except Exception as e:
        print(f"Error embedding dreams batch: {e}", flush=True)
        time.sleep(5)
    
    conn.close()

def main():
    print("=" * 80)
    print("🌙 Dream Embedding with OpenAI (1536 dimensions)")
    print("=" * 80)
    
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # Select dreams needing keyword_vector
    cursor.execute(
        """
        SELECT dream_id, dream_keyword 
        FROM dreams 
        WHERE keyword_vector IS NULL
          AND is_active = true
        """
    )
    rows = cursor.fetchall()
    conn.close()
    
    if not rows:
        print("✅ All dreams already have keyword embeddings!")
        return
    
    print(f"📊 Found {len(rows)} dreams needing keyword embeddings")
    print(f"⏳ Starting embedding process...")
    print()
    
    # Process in batches
    batch_size = 100
    batches = [rows[i:i + batch_size] for i in range(0, len(rows), batch_size)]
    
    for idx, batch in enumerate(batches, 1):
        print(f"Processing batch {idx}/{len(batches)}...", flush=True)
        conn = get_db_connection()
        cursor = conn.cursor()
        
        # Prepare batch input
        ids = [r[0] for r in batch]
        keywords = [r[1] for r in batch]
        
        try:
            response = client.embeddings.create(
                input=keywords,
                model=EMBED_MODEL
            )
            
            for i, data in enumerate(response.data):
                vector = data.embedding
                rid = ids[i]
                vector_str = "[" + ",".join(map(str, vector)) + "]"
                cursor.execute(
                    "UPDATE dreams SET keyword_vector = %s WHERE dream_id = %s",
                    (vector_str, rid)
                )
            
            conn.commit()
            print(f"Batch {idx} committed.")
        except Exception as e:
            print(f"Error in batch {idx}: {e}")
            
        conn.close()
    
    print()
    print("=" * 80)
    print("🎉 All dreams embedded successfully!")
    print("=" * 80)

if __name__ == "__main__":
    main()
