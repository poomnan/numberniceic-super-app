package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"os"

	_ "github.com/lib/pq"
)

func main() {
	connStr := os.Getenv("DATABASE_URL")
	if connStr == "" {
		connStr = "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable"
	}

	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	rows, err := db.QueryContext(context.Background(), "SELECT message_id, session_id, message_text, image_url, created_at FROM chat_messages WHERE image_url IS NOT NULL AND image_url != '' ORDER BY created_at DESC LIMIT 10")
	if err != nil {
		log.Fatalf("Query failed: %v", err)
	}
	defer rows.Close()

	fmt.Println("Recent Chat Messages with Images:")
	for rows.Next() {
		var id int
		var sid, text, url, created string
		if err := rows.Scan(&id, &sid, &text, &url, &created); err != nil {
			log.Fatal(err)
		}
		fmt.Printf("ID: %d | URL: %q | Msg: %q\n", id, url, text)
	}
}
