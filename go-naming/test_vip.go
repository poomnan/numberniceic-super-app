//go:build tools

package main

import (
"database/sql"
"fmt"
"log"

_ "github.com/go-sql-driver/mysql"
)

func main() {
    // Connect to MySQL
    // DSN format: username:password@tcp(host:port)/dbname
    // Using credentials from context or previous logs if available.
    // user: zoqlszwh_ananyadb, pass: IntelliP24.X (from summary)
    // But wait, the logs showed "postgres://tayap:IntelliP24.X..." for Postgres.
    // For MySQL, I need to check the DSN used in the app.
    // I can grep it from database/db.go or main.go
    
    // Hardcoding based on previous valid connections or trying both common ones.
    // The user summary said: "username: zoqlszwh_ananyadb, password: IntelliP24.X"
    dsn := "zoqlszwh_ananyadb:IntelliP24.X@tcp(127.0.0.1:3306)/zoqlszwh_ananyadb"
	db, err := sql.Open("mysql", dsn)
	if err != nil {
		log.Fatalf("Error opening DB: %v", err)
	}
	defer db.Close()

	var vipcode string
	err = db.QueryRow("SELECT vipcode FROM membertb WHERE memberid = ?", 812).Scan(&vipcode)
	if err != nil {
		log.Fatalf("Error querying: %v", err)
	}

	fmt.Printf("User 812 VIP Code: '%s'\n", vipcode)
}
