package main

import (
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
		log.Fatalf("Error opening database connection: %v", err)
	}
	defer db.Close()

	if err := db.Ping(); err != nil {
		log.Fatalf("Error connecting to database: %v", err)
	}

	// List all rows
	rows, err := db.Query("SELECT day, day_th, kakis FROM kakis_day")
	if err != nil {
		log.Fatalf("Error querying kakis_day: %v", err)
	}
	defer rows.Close()

	fmt.Println("--- Kaki Table Content ---")
	for rows.Next() {
		var d, dt, k string
		if err := rows.Scan(&d, &dt, &k); err != nil {
			log.Fatal(err)
		}
		fmt.Printf("Day: %s (%s) -> %s\n", d, dt, k)
	}
	fmt.Println("--------------------------")
}
