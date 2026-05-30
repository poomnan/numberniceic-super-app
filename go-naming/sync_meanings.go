//go:build tools

package main

import (
	"database/sql"
	"fmt"
	"log"

	"github.com/lib/pq"
	_ "github.com/lib/pq"
)

const (
	LocalConn  = "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable"
	RemoteConn = "postgres://tayap:IntelliP24.X@43.228.85.200/tayap?sslmode=disable"
	BatchSize  = 1000
)

func main() {
	localDB, err := sql.Open("postgres", LocalConn)
	if err != nil {
		log.Fatal(err)
	}
	defer localDB.Close()

	remoteDB, err := sql.Open("postgres", RemoteConn)
	if err != nil {
		log.Fatal(err)
	}
	defer remoteDB.Close()

	fmt.Println("Starting FAST synchronization of meanings...")

	rows, err := remoteDB.Query("SELECT thname, meaning FROM names_miracle WHERE meaning IS NOT NULL AND meaning != ''")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	var batchNames []string
	var batchMeanings []string
	totalSynced := 0
	count := 0

	for rows.Next() {
		var name, meaning string
		if err := rows.Scan(&name, &meaning); err != nil {
			continue
		}

		batchNames = append(batchNames, name)
		batchMeanings = append(batchMeanings, meaning)
		count++

		if len(batchNames) >= BatchSize {
			synced, err := bulkUpdate(localDB, batchNames, batchMeanings)
			if err != nil {
				log.Printf("Batch update error: %v", err)
			} else {
				totalSynced += synced
			}
			batchNames = nil
			batchMeanings = nil
			fmt.Printf("Processed %d rows, Total synced: %d\n", count, totalSynced)
		}
	}

	if len(batchNames) > 0 {
		synced, _ := bulkUpdate(localDB, batchNames, batchMeanings)
		totalSynced += synced
	}

	fmt.Printf("Finished. Total rows: %d, Total synced: %d\n", count, totalSynced)
}

func bulkUpdate(db *sql.DB, names, meanings []string) (int, error) {
	if len(names) == 0 {
		return 0, nil
	}

	// Build bulk insert with ON CONFLICT UPDATE
	// Using temporary table or complex query
	// Since we have a lot of items, the fastest is to use UNNEST
	query := `
		INSERT INTO names_miracle (thname, meaning)
		SELECT * FROM UNNEST($1::text[], $2::text[])
		ON CONFLICT (thname) DO UPDATE SET meaning = EXCLUDED.meaning
		WHERE names_miracle.meaning IS NULL OR names_miracle.meaning = ''
	`
	res, err := db.Exec(query, pq.Array(names), pq.Array(meanings))
	if err != nil {
		return 0, err
	}
	affected, _ := res.RowsAffected()
	return int(affected), nil
}
