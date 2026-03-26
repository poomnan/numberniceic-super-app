import os
import psycopg2
import requests
import json
import time

# Configuration
DB_HOST = "43.228.85.200"
DB_NAME = "tayap"
DB_USER = "tayap"
DB_PASS = "IntelliP24.X"
API_KEY = os.environ.get("OPENAI_API_KEY")
MODEL = "text-embedding-3-small" # consistent with 1536 dimensions

if not API_KEY:
    print("Error: OPENAI_API_KEY environment variable not set.")
    exit(1)

def get_embedding(text):
    url = "https://api.openai.com/v1/embeddings"
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {API_KEY}"
    }
    data = {
        "input": text,
        "model": MODEL
    }
    
    try:
        response = requests.post(url, headers=headers, json=data)
        response.raise_for_status()
        return response.json()['data'][0]['embedding']
    except Exception as e:
        print(f"Error getting embedding: {e}")
        return None

def main():
    try:
        conn = psycopg2.connect(
            host=DB_HOST,
            database=DB_NAME,
            user=DB_USER,
            password=DB_PASS
        )
        cur = conn.cursor()
        
        # Select all dreams
        cur.execute("SELECT dream_id, dream_keyword FROM dreams WHERE is_active = true")
        rows = cur.fetchall()
        
        print(f"Found {len(rows)} active dreams to process.")
        
        updated_count = 0
        
        for row in rows:
            dream_id, keyword = row
            
            if not keyword:
                print(f"Skipping empty keyword for dream_id {dream_id}")
                continue
                
            embedding = get_embedding(keyword)
            
            if embedding:
                vector_string = "[" + ",".join(map(str, embedding)) + "]"
                
                cur.execute(
                    "UPDATE dreams SET keyword_vector = %s, updated_at = NOW() WHERE dream_id = %s",
                    (vector_string, dream_id)
                )
                updated_count += 1
                if updated_count % 10 == 0:
                    conn.commit()
                    print(f"Processed {updated_count}/{len(rows)}...")
            else:
                print(f"Failed to get embedding for dream_id {dream_id}")
                
        conn.commit()
        print(f"Successfully updated {updated_count} dreams.")
        
    except Exception as e:
        print(f"Database error: {e}")
    finally:
        if conn:
            cur.close()
            conn.close()

if __name__ == "__main__":
    main()
