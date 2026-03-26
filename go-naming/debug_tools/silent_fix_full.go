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

	"github.com/lib/pq"
	_ "github.com/lib/pq"
)

type MigrationJob struct {
	ID   int
	Name string
}

func main() {
	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		dbURL = "postgres://tayap:IntelliP24.X@localhost/tayap?sslmode=disable"
	}
	os.Setenv("DATABASE_URL", dbURL)
	database.Connect()
	db := database.DB

	var totalRecords int
	err := db.QueryRow("SELECT COUNT(*) FROM names_miracle").Scan(&totalRecords)
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("🚀 Starting FULL SILENT Migration for %d records (Fixing Sums + Pairs)...", totalRecords)
	startTime := time.Now()

	workerCount := 20
	batchSize := 2000

	jobs := make(chan MigrationJob, batchSize*2)
	var wg sync.WaitGroup
	var processed int64
	var fixed int64

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

				// Update EVERYTHING: sums, pairs (t_sat/t_sha), and num arrays (satnum/shanum)
				// We use pq.Array to convert Go slice to Postgres array
				_, err = db.Exec(`
					UPDATE names_miracle 
					SET sat_sum = $1, sha_sum = $2,
					    t_sat = $3, t_sha = $4,
					    satnum = $5, shanum = $6
					WHERE name_id = $7`,
					res.TotalSat, res.TotalSha,
					pq.Array(res.SatPairs), pq.Array(res.ShaPairs),
					pq.Array(res.SatPairs), pq.Array(res.ShaPairs), // Using pairs for num arrays too as they are varchar[]
					job.ID)

				if err == nil {
					atomic.AddInt64(&fixed, 1)
				}
				atomic.AddInt64(&processed, 1)
			}
		}(i)
	}

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
		log.Printf("📦 Dispatched up to ID %d...", lastID)
	}

	close(jobs)
	wg.Wait()

	duration := time.Since(startTime)
	fmt.Printf("\n✅ FULL Migration Complete! Processed %d, Fixed %d records in %v\n",
		atomic.LoadInt64(&processed), atomic.LoadInt64(&fixed), duration)
}
