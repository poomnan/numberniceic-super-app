//go:build tools

package main

import (
	"database/sql"
	"fmt"
	"log"

	_ "github.com/lib/pq"
)

func main() {
	// Connect to DB
	connStr := os.Getenv("DATABASE_URL")
	if connStr == "" {
		log.Fatal("missing DATABASE_URL")
	}
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	day := "Monday"
	dayTh := "จันทร์"

	fmt.Printf("Checking kakis_day for %s / %s\n", day, dayTh)

	rows, err := db.Query("SELECT kakis FROM kakis_day WHERE LOWER(day) = LOWER($1) OR day_th = $2", day, dayTh)
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	for rows.Next() {
		var kakis string
		if err := rows.Scan(&kakis); err != nil {
			log.Fatal(err)
		}
		fmt.Printf("Raw Kakis: [%s]\n", kakis)
		fmt.Printf("Runes: ")
		for _, r := range kakis {
			fmt.Printf("%c (U+%04X) ", r, r)
		}
		fmt.Println()
	}
}
