package main

import (
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
	"os"

	"github.com/lib/pq"
)

func main() {
	// 1. Connect
	os.Setenv("DATABASE_URL", "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable")
	database.Connect()

	batchSize := 1000

	log.Println("🚀 Starting FULL Decode Migration (satnum, shanum, t_sat, t_sha)...")

	totalUpdated := 0

	for {
		// Find names missing satnum (newly inserted names)
		rows, err := database.DB.Query(`
			SELECT name_id, thname 
			FROM names_miracle 
			WHERE satnum IS NULL OR array_length(satnum, 1) IS NULL
			LIMIT $1
		`, batchSize)
		if err != nil {
			log.Fatal(err)
		}

		type FullUpdate struct {
			ID     int
			Sat    int
			Sha    int
			SatNum []string
			ShaNum []string
			TSat   []string
			TSha   []string
		}

		var updates []FullUpdate
		count := 0

		for rows.Next() {
			var id int
			var name string
			if err := rows.Scan(&id, &name); err != nil {
				continue
			}

			res, err := services.DecodeName(name, "")
			if err != nil {
				log.Printf("Error decoding %s: %v", name, err)
				continue
			}

			// Extract pair types
			var tSat, tSha []string
			for _, d := range res.SatDetails {
				tSat = append(tSat, d.PairType)
			}
			for _, d := range res.ShaDetails {
				tSha = append(tSha, d.PairType)
			}

			updates = append(updates, FullUpdate{
				ID:     id,
				Sat:    res.TotalSat,
				Sha:    res.TotalSha,
				SatNum: res.SatPairs,
				ShaNum: res.ShaPairs,
				TSat:   tSat,
				TSha:   tSha,
			})
			count++
		}
		rows.Close()

		if count == 0 {
			break
		}

		// Batch update
		tx, err := database.DB.Begin()
		if err != nil {
			log.Fatal(err)
		}

		stmt, err := tx.Prepare(`
			UPDATE names_miracle 
			SET sat_sum = $1, sha_sum = $2, 
			    satnum = $3, shanum = $4, 
			    t_sat = $5, t_sha = $6 
			WHERE name_id = $7
		`)
		if err != nil {
			log.Fatal(err)
		}

		for _, u := range updates {
			_, err := stmt.Exec(
				u.Sat, u.Sha,
				pq.Array(u.SatNum), pq.Array(u.ShaNum),
				pq.Array(u.TSat), pq.Array(u.TSha),
				u.ID,
			)
			if err != nil {
				log.Printf("Error updating ID %d: %v", u.ID, err)
			}
		}
		stmt.Close()

		if err := tx.Commit(); err != nil {
			log.Fatal(err)
		}

		totalUpdated += count
		fmt.Printf("✅ Processed batch: %d names (Total: %d)\n", count, totalUpdated)
	}

	log.Printf("🎉 FULL Decode Migration Complete! Updated %d names\n", totalUpdated)
}
