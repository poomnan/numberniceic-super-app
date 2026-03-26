import os
import psycopg2
import requests

DB_HOST = "43.228.85.200"
DB_NAME = "tayap"
DB_USER = "tayap"
DB_PASS = "IntelliP24.X"
API_KEY = os.environ.get("OPENAI_API_KEY")
MODEL = "text-embedding-3-small"

if not API_KEY:
    print("Error: OPENAI_API_KEY not set.")
    exit(1)

def get_embedding(text):
    try:
        url = "https://api.openai.com/v1/embeddings"
        headers = { "Content-Type": "application/json", "Authorization": f"Bearer {API_KEY}" }
        res = requests.post(url, headers=headers, json={"input": text, "model": MODEL})
        res.raise_for_status()
        return res.json()['data'][0]['embedding']
    except Exception as e:
        print(f"Embedding error: {e}")
        return None

def main():
    try:
        conn = psycopg2.connect(host=DB_HOST, database=DB_NAME, user=DB_USER, password=DB_PASS)
        cur = conn.cursor()
        
        target_ids = [623, 643, 645, 636, 619, 631]
        
        cur.execute("SELECT dream_id, dream_keyword FROM dreams WHERE dream_id IN %s", (tuple(target_ids),))
        rows = cur.fetchall()
        
        for did, kw in rows:
            print(f"Re-embedding ID {did}: {kw}")
            emb = get_embedding(kw)
            if emb:
                vector_string = "[" + ",".join(map(str, emb)) + "]"
                cur.execute("UPDATE dreams SET keyword_vector = %s, updated_at = NOW() WHERE dream_id = %s", (vector_string, did))
                print(f"Updated ID {did}")
            else:
                print(f"Failed ID {did}")
                
        conn.commit()
        
    except Exception as e:
        print(f"DB Error: {e}")
    finally:
        if conn: conn.close()

if __name__ == "__main__":
    main()
