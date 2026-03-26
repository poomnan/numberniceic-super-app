package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
	"os"
	"sync"
	"sync/atomic"
	"time"

	_ "github.com/lib/pq"
)

// Fix all sha_sum values in names_miracle table
// The existing sha_sum values were pre-computed incorrectly
// This script recalculates them using DecodeName() which gives correct total_sha

type MigrationJob struct {
	ID   int
	Name string
}

func main() {
	// 1. Connect
	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		dbURL = "postgres://tayap:IntelliP24.X@localhost/tayap?sslmode=disable"
	}
	os.Setenv("DATABASE_URL", dbURL)
	database.Connect()
	db := database.DB

	// Count total records
	var totalRecords int
	err := db.QueryRow("SELECT COUNT(*) FROM names_miracle").Scan(&totalRecords)
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("🚀 Starting SHA Sum Fix Migration for %d records...", totalRecords)
	startTime := time.Now()

	// Configuration
	workerCount := 20
	batchSize := 2000

	// Channels & counters
	jobs := make(chan MigrationJob, batchSize*2)
	var wg sync.WaitGroup
	var processed int64
	var fixed int64

	// Start Workers
	for i := 0; i < workerCount; i++ {
		wg.Add(1)
		go func(workerID int) {
			defer wg.Done()

			for job := range jobs {
				res, err := services.DecodeName(job.Name, "")
				if err != nil {
					atomic.AddInt64(&processed, 1)
					continue
				}

				// Update sha_sum (and sat_sum for good measure)
				_, err = db.Exec("UPDATE names_miracle SET sat_sum = $1, sha_sum = $2 WHERE name_id = $3",
					res.TotalSat, res.TotalSha, job.ID)
				if err != nil {
					log.Printf("[Worker %d] Error updating ID %d: %v", workerID, job.ID, err)
				} else {
					atomic.AddInt64(&fixed, 1)
				}
				atomic.AddInt64(&processed, 1)
			}
		}(i)
	}

	// Producer: Fetch ALL rows using offset-based pagination
	totalDispatched := 0
	lastID := 0

	for {
		rows, err := db.Query(`
			SELECT name_id, thname 
			FROM names_miracle 
			WHERE name_id > $1
			ORDER BY name_id ASC
			LIMIT $2
		`, lastID, batchSize)
		if err != nil {
			log.Fatal(err)
		}

		count := 0
		for rows.Next() {
			var job MigrationJob
			if err := rows.Scan(&job.ID, &job.Name); err != nil {
				continue
			}
			lastID = job.ID
			jobs <- job
			count++
		}
		rows.Close()

		if count == 0 {
			break
		}

		totalDispatched += count
		p := atomic.LoadInt64(&processed)
		fmt.Printf("\r📦 Dispatched: %d/%d | Processed: %d | Fixed: %d",
			totalDispatched, totalRecords, p, atomic.LoadInt64(&fixed))
	}

	close(jobs)
	wg.Wait()

	duration := time.Since(startTime)
	fmt.Printf("\n✅ SHA Sum Fix Complete! Processed %d, Fixed %d records in %v\n",
		atomic.LoadInt64(&processed), atomic.LoadInt64(&fixed), duration)
}
