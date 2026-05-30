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

	var minID, maxID int
	err = db.QueryRow("SELECT MIN(name_id), MAX(name_id) FROM names_miracle").Scan(&minID, &maxID)
	if err != nil {
		log.Fatal("Failed to get ID range:", err)
	}

	fmt.Printf("Deep Repair Scan Started: Rows %d to %d\n", minID, maxID)

	chunkSize := 5000
	totalUpdated := int64(0)

	for id := minID; id <= maxID; id += chunkSize {
		endID := id + chunkSize - 1

		// Efficient chunk update using PK index
		query := `
			UPDATE names_miracle 
			SET sat_sum = COALESCE((SELECT sum(s::int) FROM unnest(satnum) s), 0), 
                sha_sum = COALESCE((SELECT sum(s::int) FROM unnest(shanum) s), 0)
			WHERE name_id BETWEEN $1 AND $2 
			  AND (
                  (sha_sum IS DISTINCT FROM (SELECT sum(s::int) FROM unnest(shanum) s))
               OR (sat_sum IS DISTINCT FROM (SELECT sum(s::int) FROM unnest(satnum) s))
              )
		`
		res, err := db.Exec(query, id, endID)
		if err != nil {
			log.Printf("Error processing chunk %d-%d: %v", id, endID, err)
			continue
		}

		affected, _ := res.RowsAffected()
		if affected > 0 {
			totalUpdated += affected
			fmt.Printf("Repaired Chunk %d-%d: Fixed %d inconsistencies\n", id, endID, affected)
		}
	}

	fmt.Printf("Deep Repair Completed. Total Updated: %d\n", totalUpdated)
}
