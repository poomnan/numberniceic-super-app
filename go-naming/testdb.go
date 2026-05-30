//go:build tools

package main

import (
	"database/sql"
	"fmt"
	"log"

	_ "github.com/lib/pq"
)

func main() {
	connStr := os.Getenv("DATABASE_URL")
	if connStr == "" {
		log.Fatal("missing DATABASE_URL")
	}
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()
	rows, err := db.Query(`
		SELECT thname, similarity(thname, 'ทญา') as sml, thname % 'ทญา' as matches
		FROM names_miracle
		ORDER BY thname <-> 'ทญา'
		LIMIT 10
	`)
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()
	for rows.Next() {
		var name string
		var sml float64
		var matches bool
		rows.Scan(&name, &sml, &matches)
		fmt.Printf("Found: %s (sml: %.4f, matches: %v)\n", name, sml, matches)
	}
}
