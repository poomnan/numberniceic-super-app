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

	// Replicate the broad search query
	// Using a ZERO vector (1536 zeros) to simulate fallback/broad search
	vectorStr := "["
	for i := 0; i < 1536; i++ {
		if i > 0 {
			vectorStr += ","
		}
		vectorStr += "0.0"
	}
	vectorStr += "]"

	// EXACT query from MobileSearchHandler logic
	query := `
		SELECT name_id, thname, meaning, gender,
		       sat_sum, sha_sum,
		       COALESCE((meaning_vector <=> $1), 0) as distance
		FROM names_miracle 
		WHERE 1=1
		ORDER BY distance ASC LIMIT 5
	`
	// Note: MobileSearchHandler actually uses order by distance ASC

	rows, err := db.Query(query, vectorStr)
	if err != nil {
		fmt.Printf("Query error: %v\n", err)
		return
	}
	defer rows.Close()

	fmt.Println("Broad Search Results (Simulated):")
	for rows.Next() {
		var id int
		var name, meaning, gender string
		var sat, sha int
		var dist float64
		rows.Scan(&id, &name, &meaning, &gender, &sat, &sha, &dist)
		fmt.Printf("- %s (ID: %d, Gender: %s, Dist: %f)\n", name, id, gender, dist)
	}
}
