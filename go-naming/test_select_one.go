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

	var id int
	var name string
	err = db.QueryRow("SELECT name_id, thname FROM names_miracle LIMIT 1").Scan(&id, &name)
	if err != nil {
		fmt.Printf("Select error: %v\n", err)
	} else {
		fmt.Printf("Row 1: %d - %s\n", id, name)
	}
}
