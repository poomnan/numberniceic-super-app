//go:build tools

package main

import (
	"database/sql"
	"fmt"
	"log"

	"go-naming/database"
	"go-naming/services"

	_ "github.com/lib/pq"
)

func main() {
	// Connect to DB manually as services rely on database.DB
	connStr := os.Getenv("DATABASE_URL")
	if connStr == "" {
		log.Fatal("missing DATABASE_URL")
	}
	var err error
	database.DB, err = sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal(err)
	}
	defer database.DB.Close()

	name := "วิกรม"
	day := "Monday"

	fmt.Printf("Calling services.DecodeName('%s', '%s')...\n", name, day)
	result, err := services.DecodeName(name, day)
	if err != nil {
		log.Fatal(err)
	}

	fmt.Printf("Result for %s:\n", result.Name)
	for i, c := range result.Characters {
		fmt.Printf("[%d] %s (IsKaki: %v)\n", i, c.Char, c.IsKaki)
	}
}
