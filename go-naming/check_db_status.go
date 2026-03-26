package main

import (
	"database/sql"
	"fmt"
	"log"

	_ "github.com/lib/pq"
)

func main() {
	connStr := "postgres://tayap:IntelliP24.X@43.228.85.200/tayap?sslmode=disable"
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("Error opening database connection: %v", err)
	}
	defer db.Close()

	var count int
	err = db.QueryRow("SELECT COUNT(*) FROM names_miracle").Scan(&count)
	if err != nil {
		fmt.Printf("Error counting names_miracle: %v\n", err)
	} else {
		fmt.Printf("Table names_miracle has %d rows\n", count)
	}

	var hasVector bool
	err = db.QueryRow("SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'names_miracle' AND column_name = 'meaning_vector')").Scan(&hasVector)
	if err == nil {
		fmt.Printf("meaning_vector col exists: %v\n", hasVector)
	}
}
