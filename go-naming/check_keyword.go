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

	// Check if meaning_vector has any non-zero values
	var count int
	err = db.QueryRow("SELECT count(*) FROM names_miracle WHERE meaning_vector IS NOT NULL").Scan(&count)
	fmt.Printf("Names with meaning_vector: %d\n", count)

	// Check for names containing 'เศรษฐี' in their meaning
	rows, err := db.Query("SELECT thname, meaning FROM names_miracle WHERE meaning LIKE '%เศรษฐี%' LIMIT 5")
	if err == nil {
		fmt.Println("Names with 'เศรษฐี' in meaning:")
		for rows.Next() {
			var n, m string
			rows.Scan(&n, &m)
			fmt.Printf("- %s: %s\n", n, m)
		}
		rows.Close()
	}
}
