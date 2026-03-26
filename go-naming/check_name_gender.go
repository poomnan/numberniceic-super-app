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

	var g string
	err = db.QueryRow("SELECT gender FROM names_miracle WHERE thname = $1", "เศรษฐภัทร์").Scan(&g)
	if err != nil {
		fmt.Printf("Error: %v\n", err)
	} else {
		fmt.Printf("Gender of 'เศรษฐภัทร์': '%v'\n", g)
	}
}
