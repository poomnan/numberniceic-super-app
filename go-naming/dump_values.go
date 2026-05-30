//go:build tools

package main

import (
	"encoding/json"
	"fmt"
	"go-naming/database"
	"log"
	"os"
)

func main() {
	// Initialize Database (adjust ENV if needed or rely on hardcoded default in Connect)
	// Note: User's deploy.sh sets DB URL, we might need to set it here if Connect() relies on it.
	// Assuming Connect() works or we set ENV.
	// Error handling omitted for brevity in casual script.

	// Set ENV for Local Dev if needed (based on deploy.sh knowledge)
	os.Setenv("DATABASE_URL", "postgres://tayap:IntelliP24.X@localhost/tayap?sslmode=disable")

	database.Connect()
	defer database.DB.Close()

	// 1. Fetch Sat Values
	satMap := make(map[string]int)
	rows, err := database.DB.Query("SELECT char_key, sat_value FROM sat_nums")
	if err != nil {
		log.Fatalf("Sat Query Error: %v", err)
	}
	defer rows.Close()
	for rows.Next() {
		var k string
		var v int
		rows.Scan(&k, &v)
		satMap[k] = v
	}

	// 2. Fetch Sha Values
	shaMap := make(map[string]int)
	rows2, err := database.DB.Query("SELECT char_key, sha_value FROM sha_nums")
	if err != nil {
		log.Fatalf("Sha Query Error: %v", err)
	}
	defer rows2.Close()
	for rows2.Next() {
		var k string
		var v int
		rows2.Scan(&k, &v)
		shaMap[k] = v
	}

	result := map[string]interface{}{
		"sat": satMap,
		"sha": shaMap,
	}

	bytes, _ := json.MarshalIndent(result, "", "  ")
	fmt.Println(string(bytes))
}
