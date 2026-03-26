//go:build tools

package main

import (
	"database/sql"
	"fmt"
	"log"

	_ "github.com/lib/pq"
)

func main() {
	connStr := "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable"
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	// Check count of needing update
	var count int
	err = db.QueryRow(`
		SELECT COUNT(*) 
		FROM names_miracle 
		WHERE sha_sum != (SELECT sum(s::int) FROM unnest(shanum) s) 
		   OR sat_sum != (SELECT sum(s::int) FROM unnest(satnum) s)
	`).Scan(&count)
	if err != nil {
		log.Printf("Count query failed: %v", err)
	}
	fmt.Printf("Total rows need update: %d\n", count)

	if count == 0 {
		return
	}

	batchSize := 5000
	for {
		// Use subquery to select batch of IDs first (avoids race/long scan issues somewhat)
		query := `
			UPDATE names_miracle 
			SET sat_sum = COALESCE((SELECT sum(s::int) FROM unnest(satnum) s), 0), 
                sha_sum = COALESCE((SELECT sum(s::int) FROM unnest(shanum) s), 0)
			WHERE name_id IN (
				SELECT name_id FROM names_miracle 
				WHERE sha_sum != (SELECT sum(s::int) FROM unnest(shanum) s) 
                   OR sat_sum != (SELECT sum(s::int) FROM unnest(satnum) s) 
				LIMIT $1
			)
		`
		res, err := db.Exec(query, batchSize)
		if err != nil {
			log.Printf("Update error: %v", err)
			break
		}

		affected, _ := res.RowsAffected()
		fmt.Printf("Updated batch: %d rows\n", affected)

		if affected == 0 {
			break
		}
	}
	fmt.Println("All updates completed.")
}
