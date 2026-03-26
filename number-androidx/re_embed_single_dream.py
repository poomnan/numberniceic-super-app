import os
import psycopg2
import requests

# Configuration
DB_HOST = "43.228.85.200"
DB_NAME = "tayap"
DB_USER = "tayap"
DB_PASS = "IntelliP24.X"
API_KEY = os.environ.get("OPENAI_API_KEY")
MODEL = "text-embedding-3-small"

if not API_KEY:
    print("Error: OPENAI_API_KEY environment variable not set.")
    exit(1)

def get_embedding(text):
    url = "https://api.openai.com/v1/embeddings"
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {API_KEY}"
    }
    data = {"input": text, "model": MODEL}
    
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
        
        # Target specific dream ID
        dream_id = 518
        
        cur.execute("SELECT dream_keyword FROM dreams WHERE dream_id = %s", (dream_id,))
        row = cur.fetchone()
        
        if not row:
            print(f"Dream ID {dream_id} not found.")
            return

        keyword = row[0]
        print(f"Processing Dream ID {dream_id}: {keyword}")
        
        embedding = get_embedding(keyword)
        
        if embedding:
            vector_string = "[" + ",".join(map(str, embedding)) + "]"
            cur.execute(
                "UPDATE dreams SET keyword_vector = %s, updated_at = NOW() WHERE dream_id = %s",
                (vector_string, dream_id)
            )
            conn.commit()
            print(f"Successfully updated Dream ID {dream_id}")
        else:
            print("Failed to get embedding.")

    except Exception as e:
        print(f"Database error: {e}")
    finally:
        if conn:
            cur.close()
            conn.close()

if __name__ == "__main__":
    main()
