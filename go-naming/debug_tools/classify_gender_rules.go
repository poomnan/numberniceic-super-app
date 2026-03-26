package main

import (
	"database/sql"
	"fmt"
	"log"
	"strings"

	_ "github.com/lib/pq"
)

const (
	serverIP   = "43.228.85.200"
	serverPort = "5432"
	dbUser     = "tayap"
	dbPass     = "IntelliP24.X"
	dbName     = "tayap"
)

// คำลงท้ายที่บ่งบอกเพศ
var (
	maleEndings = []string{
		"ชัย", "พล", "ศักดิ์", "สิทธิ์", "ชาติ", "วัฒน์", "ภัทร", "ภูมิ",
		"เดช", "วีร์", "ยุทธ", "ธนา", "พงศ์", "พัฒน์", "ราช", "กร",
		"สรร", "สันต์", "ธร", "นันท์", "ชาญ", "ชนะ", "เจริญ", "วิทย์",
	}

	femaleEndings = []string{
		"ญา", "นภา", "ลักษณ์", "วรรณ", "ศรี", "สุดา", "รัตน์", "พร",
		"ทิพย์", "นิตย์", "ลดา", "วดี", "วิภา", "สิริ", "กาญจน์", "กานต์",
		"นิภา", "นันท์", "ธิดา", "ธิรา", "ภัทร์", "มณี", "รินทร์", "ลิน",
		"ชญา", "ญาณ์", "ญาดา", "ญานี", "ญาภา", "ญาพร",
	}
)

func classifyByRules(name string) string {
	// ตรวจสอบคำลงท้าย
	for _, ending := range maleEndings {
		if strings.HasSuffix(name, ending) {
			return "male"
		}
	}

	for _, ending := range femaleEndings {
		if strings.HasSuffix(name, ending) {
			return "female"
		}
	}

	// ตรวจสอบคำในชื่อ
	if strings.Contains(name, "ชาย") || strings.Contains(name, "บุรุษ") {
		return "male"
	}

	if strings.Contains(name, "หญิง") || strings.Contains(name, "สตรี") || strings.Contains(name, "นาง") {
		return "female"
	}

	return "neutral"
}

func main() {
	connStr := fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=disable",
		dbUser, dbPass, serverIP, serverPort, dbName)
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("DB Connect Error: %v", err)
	}
	defer db.Close()

	fmt.Println("🚀 Starting Rule-Based Gender Classification")

	// ดึงชื่อทั้งหมด
	rows, err := db.Query("SELECT name_id, thname FROM names_miracle")
	if err != nil {
		log.Fatalf("Query Error: %v", err)
	}
	defer rows.Close()

	maleCount := 0
	femaleCount := 0
	neutralCount := 0
	total := 0

	// Batch update
	tx, _ := db.Begin()

	for rows.Next() {
		var id int
		var name string
		if err := rows.Scan(&id, &name); err != nil {
			continue
		}

		gender := classifyByRules(name)

		_, err = tx.Exec("UPDATE names_miracle SET gender = $1 WHERE name_id = $2", gender, id)
		if err != nil {
			log.Printf("Update Error for %s: %v", name, err)
			continue
		}

		switch gender {
		case "male":
			maleCount++
		case "female":
			femaleCount++
		case "neutral":
			neutralCount++
		}

		total++
		if total%10000 == 0 {
			fmt.Printf("✅ Processed %d names...\n", total)
			tx.Commit()
			tx, _ = db.Begin()
		}
	}

	tx.Commit()

	fmt.Printf("\n🎉 Success! Classified %d names\n", total)
	fmt.Printf("\n📊 Gender Distribution:\n")
	fmt.Printf("   Male: %d (%.1f%%)\n", maleCount, float64(maleCount)/float64(total)*100)
	fmt.Printf("   Female: %d (%.1f%%)\n", femaleCount, float64(femaleCount)/float64(total)*100)
	fmt.Printf("   Neutral: %d (%.1f%%)\n", neutralCount, float64(neutralCount)/float64(total)*100)
}
