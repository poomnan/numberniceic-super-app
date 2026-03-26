package main

import (
	"database/sql"
	"fmt"
	"log"

	_ "github.com/lib/pq"
)

func main() {
	connStr := "postgres://tayap:IntelliP24.X@43.228.85.200/tayap?sslmode=disable"
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("Error opening database connection: %v", err)
	}
	defer db.Close()

	rows, err := db.Query("SELECT gender, count(*) FROM names_miracle GROUP BY gender")
	if err != nil {
		fmt.Printf("Error: %v\n", err)
		return
	}
	defer rows.Close()

	for rows.Next() {
		var g string
		var c int
		rows.Scan(&g, &c)
		fmt.Printf("Gender '%s': %d names\n", g, c)
	}
}
