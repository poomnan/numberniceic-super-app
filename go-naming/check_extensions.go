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

	rows, err := db.Query("SELECT extname FROM pg_extension")
	if err == nil {
		fmt.Println("Extensions in DB:")
		for rows.Next() {
			var e string
			rows.Scan(&e)
			fmt.Printf("- %s\n", e)
		}
		rows.Close()
	}
}
