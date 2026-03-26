//go:build tools

package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"os"
	"strings"

	"github.com/lib/pq"
)

func main() {
	os.Setenv("DATABASE_URL", "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable")
	database.Connect()

	// Mock Data
	goodSums, _ := services.GetGoodSums()
	fmt.Printf("GoodSums count: %d\n", len(goodSums))

	// Mock Vector (1536 dims)
	embedding := make([]float64, 1536)
	for i := range embedding {
		embedding[i] = 0.001
	}

	// Build Query Logic from DemoSearchHandler (Strict Mode - FilterSat Only)
	// ---------------------------------------------------------
	// buildQuery func equivalent
	buildQuery := func() (string, []interface{}) {
		query := `
			SELECT name_id, COALESCE(thname, ''),
			       sat_sum, sha_sum,
			       (meaning_vector <=> $1) as distance
			FROM names_miracle 
			WHERE 1=1
		`
		// Note: I use meaning_vector <=> $1.
		// And I use sat_sum, sha_sum columns as per recent confirmation.

		args := []interface{}{formatVector(embedding)}
		argCounter := 2

		// FilterSat = True
		allowedSats := goodSums
		satCond := fmt.Sprintf("sat_sum = ANY($%d::int[])", argCounter)
		args = append(args, pq.Array(allowedSats))
		argCounter++

		// FilterSha = False (Empty shaCond)
		shaCond := ""

		// Apply Filters (Logic from Handler)
		if satCond != "" && shaCond != "" {
			// ...
		} else {
			if satCond != "" {
				query += " AND " + satCond
			}
			if shaCond != "" {
				query += " AND " + shaCond
			}
		}

		query += " ORDER BY distance ASC LIMIT 5"
		return query, args
	}
	// ---------------------------------------------------------

	query, args := buildQuery()
	fmt.Printf("Generated Query: %s\n", query)

	// Execute
	rows, err := database.DB.Query(query, args...)
	if err != nil {
		fmt.Printf("Query Error: %v\n", err)
		return
	}
	defer rows.Close()

	count := 0
	for rows.Next() {
		var id int
		var name string
		var sat, sha int
		var dist float64
		err = rows.Scan(&id, &name, &sat, &sha, &dist)
		if err != nil {
			fmt.Printf("Scan Error: %v\n", err)
			continue
		}
		fmt.Printf("Result: %s (Sat: %d, Sha: %d, Dist: %f)\n", name, sat, sha, dist)
		count++
	}
	fmt.Printf("Total Results: %d\n", count)
}

func formatVector(v []float64) string {
	strs := make([]string, len(v))
	for i, val := range v {
		strs[i] = fmt.Sprintf("%f", val)
	}
	return "[" + strings.Join(strs, ",") + "]"
}
