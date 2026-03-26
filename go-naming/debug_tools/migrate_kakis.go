package main

import (
	"fmt"
	"go-naming/database"
	"log"
	"os"
	"strings"
)

func main() {
	// 1. Connect
	os.Setenv("DATABASE_URL", "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable")
	database.Connect()

	// 2. Load Rules: KakiMap
	// Map Day -> Set of Restricted Chars
	dayRules := make(map[string]map[rune]bool)
	days := []string{
		"Sunday", "Monday", "Tuesday", "Wednesday1", "Wednesday2", "Thursday", "Friday", "Saturday",
	}
	// DB Columns for each day
	dayCols := map[string]string{
		"Sunday":     "k_sunday",
		"Monday":     "k_monday",
		"Tuesday":    "k_tuesday",
		"Wednesday1": "k_wednesday1",
		"Wednesday2": "k_wednesday2",
		"Thursday":   "k_thursday",
		"Friday":     "k_friday",
		"Saturday":   "k_saturday",
	}

	thaiDays := map[string]string{
		"Sunday":     "อาทิตย์",
		"Monday":     "จันทร์",
		"Tuesday":    "อังคาร",
		"Wednesday1": "พุธกลางวัน",
		"Wednesday2": "พุธกลางคืน",
		"Thursday":   "พฤหัสบดี",
		"Friday":     "ศุกร์",
		"Saturday":   "เสาร์",
	}

	log.Println("Loading Kaki Rules...")
	for _, day := range days {
		row, err := database.DB.Query("SELECT kakis FROM kakis_day WHERE day = $1 OR day_th = $2", day, thaiDays[day])
		if err != nil {
			log.Fatal(err)
		}

		ruleSet := make(map[rune]bool)
		for row.Next() {
			var k string
			if err := row.Scan(&k); err == nil {
				for _, r := range strings.TrimSpace(k) {
					ruleSet[r] = true
				}
			}
		}
		row.Close()
		dayRules[day] = ruleSet
		fmt.Printf(" - %s: %d forbidden chars\n", day, len(ruleSet))
	}

	// 3. Process Batch
	batchSize := 2000
	offset := 0
	totalProcessed := 0

	log.Println("Starting Kaki Flag Migration...")

	for {
		// Fetch names
		rows, err := database.DB.Query(`
			SELECT name_id, thname
			FROM names_miracle
			ORDER BY name_id
			LIMIT $1 OFFSET $2
		`, batchSize, offset)
		if err != nil {
			log.Printf("Query error at offset %d: %v", offset, err)
			break
		}

		type Update struct {
			ID    int
			Flags map[string]bool
		}
		var updates []Update

		count := 0
		for rows.Next() {
			var id int
			var name string
			if err := rows.Scan(&id, &name); err != nil {
				continue
			}

			// Analyze Name vs All Days
			flags := make(map[string]bool)
			runes := []rune(name)

			for _, day := range days {
				isKaki := false
				rules := dayRules[day]
				for _, r := range runes {
					if rules[r] {
						isKaki = true
						break
					}
				}
				flags[dayCols[day]] = isKaki
			}

			updates = append(updates, Update{ID: id, Flags: flags})
			count++
		}
		rows.Close()

		if count == 0 {
			break
		}

		// Update DB
		tx, err := database.DB.Begin()
		if err != nil {
			log.Fatal(err)
		}

		// Prepare Update Statement
		// UPDATE names_miracle SET k_sunday=$1, k_monday=$2, ... WHERE name_id=$9
		query := `UPDATE names_miracle SET 
			k_sunday=$1, k_monday=$2, k_tuesday=$3, k_wednesday1=$4, 
			k_wednesday2=$5, k_thursday=$6, k_friday=$7, k_saturday=$8 
			WHERE name_id=$9`

		stmt, err := tx.Prepare(query)
		if err != nil {
			log.Fatal(err)
		}

		for _, u := range updates {
			_, err := stmt.Exec(
				u.Flags["k_sunday"], u.Flags["k_monday"], u.Flags["k_tuesday"],
				u.Flags["k_wednesday1"], u.Flags["k_wednesday2"], u.Flags["k_thursday"],
				u.Flags["k_friday"], u.Flags["k_saturday"],
				u.ID,
			)
			if err != nil {
				log.Printf("Error updating ID %d: %v", u.ID, err)
			}
		}
		stmt.Close()
		tx.Commit()

		totalProcessed += count
		offset += count
		fmt.Printf("Processed Kaki flags: %d names...\n", totalProcessed)
	}

	log.Println("Kaki Flag Migration Complete!")
}
