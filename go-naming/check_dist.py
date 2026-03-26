import sys
import os
import json
import psycopg2

def check_dist(vector_str):
    conn = psycopg2.connect("postgres://tayap:IntelliP24.X@localhost/tayap")
    cur = conn.cursor()
    
    # Cast vector_str to vector type
    cur.execute("SELECT dream_keyword, meaning_vector <=> %s as dist FROM dreams WHERE meaning_vector IS NOT NULL ORDER BY dist LIMIT 10", (vector_str,))
    rows = cur.fetchall()
    
    for row in rows:
        print(f"{row[0]}: {row[1]}")
    
    cur.close()
    conn.close()

if __name__ == "__main__":
    with open("vector.json", "r") as f:
        data = json.load(f)
    
    vector = data["vector"]
    vector_str = "[" + ",".join(map(str, vector)) + "]"
    check_dist(vector_str)
