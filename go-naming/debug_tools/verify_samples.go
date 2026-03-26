package main

import (
	"database/sql"
	"fmt"
	"log"

	_ "github.com/lib/pq"
)

func main() {
	db, err := sql.Open("postgres", "postgres://tayap:IntelliP24.X@43.228.85.200:5432/tayap?sslmode=disable")
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	fmt.Println("🔎 ตรวจสอบคุณภาพการแปล (50 รายชื่อจากชุดพจนานุกรม 90,000 ชื่อแรก)")
	fmt.Println("------------------------------------------------------------")

	// สุ่มดึงชื่อที่มีความหมายแล้วมาดู 30 ชื่อ
	rows, err := db.Query("SELECT thname, meaning FROM names_miracle WHERE meaning IS NOT NULL AND meaning != '' ORDER BY RANDOM() LIMIT 30")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	for rows.Next() {
		var name, meaning string
		rows.Scan(&name, &meaning)
		fmt.Printf("ชื่อ: %-15s | ความหมาย: %s\n", name, meaning)
	}
}
