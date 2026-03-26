package main

import (
	"database/sql"
	"fmt"
	"log"

	_ "github.com/lib/pq"
)

func main() {
	connStr := "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable"
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	var id int
	var name string
	err = db.QueryRow("SELECT name_id, thname FROM names_miracle WHERE thname = 'ทักษิณ'").Scan(&id, &name)
	if err != nil {
		if err == sql.ErrNoRows {
			fmt.Println("Result: Name 'ทักษิณ' not found in database.")
		} else {
			fmt.Println("DB Error:", err)
		}
		return
	}

	fmt.Printf("Result: Found 'ทักษิณ' with ID: %d\n", id)
}
