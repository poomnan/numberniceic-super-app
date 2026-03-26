package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
	"os"
	"sync"
	"time"

	_ "github.com/lib/pq"
)

// Define job structure
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

	log.Println("🚀 Starting Parallel Migration: Calculating sat_sum / sha_sum...")
	startTime := time.Now()

	// Configuration
	workerCount := 20
	batchSize := 2000

	// Channels
	jobs := make(chan MigrationJob, batchSize*2)
	var wg sync.WaitGroup

	// Start Workers
	for i := 0; i < workerCount; i++ {
		wg.Add(1)
		go func(workerID int) {
			defer wg.Done()

			// Each worker processes jobs and updates DB individually or in small batches
			// For simplicity and speed, let's update individually but with prepared statement cache?
			// Identifying transactions per update is slow.
			// BETTER: Worker builds a batch of updates and commits.

			// Let's keep it simple: Workers process calculations, but maybe one 'Persister' goroutine writes to DB?
			// Or just update directly. Direct update is 20x faster than 1 thread.

			for job := range jobs {
				res, err := services.DecodeName(job.Name, "")
				if err != nil {
					continue
				}

				// Execute Update
				_, err = db.Exec("UPDATE names_miracle SET sat_sum = $1, sha_sum = $2 WHERE name_id = $3",
					res.TotalSat, res.TotalSha, job.ID)
				if err != nil {
					log.Printf("[Worker %d] Error updating ID %d: %v", workerID, job.ID, err)
				}
			}
		}(i)
	}

	// Producer: Fetch rows needing update
	// We use Cursor-like logic or Limit/Offset loop (careful with offset if updates change order/visibility)
	// Since we update sat_sum from 0/NULL to Value, we can just query WHERE sat_sum=0 LIMIT 2000 repeatedly

	totalDispatched := 0

	for {
		rows, err := db.Query(`
			SELECT name_id, thname 
			FROM names_miracle 
			WHERE sat_sum = 0 OR sat_sum IS NULL 
			LIMIT $1
		`, batchSize)
		if err != nil {
			log.Fatal(err)
		}

		count := 0
		for rows.Next() {
			var job MigrationJob
			if err := rows.Scan(&job.ID, &job.Name); err != nil {
				continue
			}
			jobs <- job
			count++
		}
		rows.Close()

		if count == 0 {
			break
		}

		totalDispatched += count
		fmt.Printf("📦 Dispatched batch of %d (Total: %d)...\r", count, totalDispatched)
	}

	close(jobs)
	wg.Wait()

	duration := time.Since(startTime)
	fmt.Printf("\n✅ Migration Complete! Processed %d records in %v\n", totalDispatched, duration)
}
