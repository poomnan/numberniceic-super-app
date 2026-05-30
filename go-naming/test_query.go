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

	// Try a query that replicates buildQuery
	// Using a ZERO vector as a test
	vector := make([]float64, 1536)
	vectorStr := "["
	for i, v := range vector {
		if i > 0 {
			vectorStr += ","
		}
		vectorStr += fmt.Sprintf("%f", v)
	}
	vectorStr += "]"

	query := `
		SELECT name_id, thname FROM names_miracle 
		WHERE 1=1
		ORDER BY meaning_vector <=> $1 ASC LIMIT 5
	`
	rows, err := db.Query(query, vectorStr)
	if err != nil {
		fmt.Printf("Query error: %v\n", err)
		return
	}
	defer rows.Close()

	count := 0
	for rows.Next() {
		var id int
		var name string
		rows.Scan(&id, &name)
		fmt.Printf("Result %d: %d - %s\n", count, id, name)
		count++
	}
	fmt.Printf("Total found: %d\n", count)
}
