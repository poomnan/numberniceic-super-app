package main

import (
	"database/sql"
	"fmt"
	"log"

	_ "github.com/lib/pq"
)

func main() {
	db, err := sql.Open("postgres", "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable")
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	rows, err := db.Query("SELECT id, pang_name, image_url FROM buddha_pangs")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	fmt.Println("ID | Name | URL")
	for rows.Next() {
		var id int
		var name, url string
		rows.Scan(&id, &name, &url)
		fmt.Printf("%d | %s | %s\n", id, name, url)
	}
}
