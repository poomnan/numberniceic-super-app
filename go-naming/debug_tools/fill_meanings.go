package main

import (
	"database/sql"
	"fmt"
	"log"
	"strings"
	"time"

	_ "github.com/lib/pq"
)

var dictionary = map[string]string{
	"ทักษิณ":   "ทิศใต้, เบื้องขวา, ความเจริญ, ผู้ฉลาด",
	"ทักษ":     "ทิศใต้, เบื้องขวา, ผู้ฉลาด",
	"สิริ":     "มิ่งขวัญ, มงคล, ความสวยงาม, ความดี",
	"ศิริ":     "มิ่งขวัญ, มงคล, ความสวยงาม, ความดี",
	"เดช":      "อำนาจ, พลัง, ความร้อน, แสงสว่าง",
	"รัตน":     "แก้ว, ของมีค่า, ประเสริฐที่สุด",
	"รัตน์":    "แก้ว, ของมีค่า, ประเสริฐที่สุด",
	"ณัฐ":      "ผู้รู้, นักปราชญ์, ผู้มีปัญญา",
	"พงศ์":     "ตระกูล, เผ่าพันธุ์, เหล่ากอ",
	"พงศ":      "ตระกูล, เผ่าพันธุ์, เหล่ากอ",
	"ชัย":      "ชัยชนะ, ความชนะ",
	"ชย":       "ชัยชนะ, ความชนะ",
	"วีร":      "กล้าหาญ, ผู้แกล้วกล้า",
	"วร":       "ประเสริฐ, เลิศ, พร",
	"ธน":       "ทรัพย์สิน, เงินทอง, ความร่ำรวย",
	"พิชญ":     "นักปราชญ์, ผู้รู้แล้ว",
	"กิตติ":    "เกียรติยศ, ชื่อเสียง",
	"จิร":      "นาน, ยาวนาน",
	"วิจิตร":   "สวยงาม, งดงาม, แปลกตา",
	"มงคล":     "ความดี, ความสุข, สิ่งนำโชค",
	"ปัญญา":    "ความรู้, ความฉลาด",
	"ภูมิ":     "ที่ดิน, แผ่นดิน, ความรู้",
	"พัชร":     "เพชร, สายฟ้า",
	"อุดม":     "สูงสุด, มากมาย, เต็มเปี่ยม",
	"โชค":      "ลาภ, สิ่งดีที่จะเกิดขึ้น",
	"นร":       "คน, ผู้นำ",
	"อมร":      "ไม่แก่ไม่ตาย, เทวดา",
	"กมล":      "บัว, หัวใจ",
	"อิทธิ":    "ความสำเร็จ, พลังอำนาจ",
	"โสภณ":     "งดงาม, สวย",
	"ภัทร":     "ดี, เจริญ, มงคล",
	"จตุ":      "สี่ (4)",
	"สิทธิ":    "ความสำเร็จ, พลังอำนาจ",
	"สุ":       "ดี, งดงาม, ง่าย",
	"บุญ":      "กุศล, ความดี, ความสุข",
	"ธรรม":     "ความดี, หลักการ, ธรรมชาติ",
	"ประเสริฐ": "เลิศ, ดีที่สุด",
	"ศักดิ์":   "อำนาจ, ความสามารถ, เกียรติ",
	"ศรี":      "มงคล, ความสวยงาม, มิ่งขวัญ",
	"ลักษณ์":   "เครื่องหมาย, คุณสมบัติ",
	"เมธ":      "ปัญญา, ความรู้",
	"วิมล":     "สะอาด, บริสุทธิ์, ไม่มีมลทิน",
	"จรัส":     "รุ่งเรือง, สว่างใส",
	"พิมล":     "สะอาด, บริสุทธิ์",
	"มณี":      "แก้วมณี, ของล้ำค่า",
	"กัญญา":    "หญิงสาว, ชะตาดี",
	"ดนัย":     "ลูกชาย",
	"ดนยา":     "ลูกสาว",
}

func main() {
	// เชื่อมต่อ Server ทางไกล
	connStr := "postgres://tayap:IntelliP24.X@43.228.85.200:5432/tayap?sslmode=disable"
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("DB Connect Error: %v", err)
	}
	defer db.Close()

	fmt.Println("🚀 Starting HIGH-SPEED Batch Update (extended dictionary)...")

	// ดึงข้อมูลทั้งหมดมาไว้ใน Memory ก่อนเพื่อความเร็ว (ประมาณ 3 แสนตัว)
	rows, err := db.Query("SELECT name_id, thname FROM names_miracle")
	if err != nil {
		log.Fatalf("Query Error: %v", err)
	}
	defer rows.Close()

	type NameRec struct {
		ID      int
		Name    string
		Meaning string
	}

	var allNames []NameRec
	for rows.Next() {
		var r NameRec
		if err := rows.Scan(&r.ID, &r.Name); err == nil {
			// วิเคราะห์ความหมายล่วงหน้า
			meanings := []string{}
			for root, mean := range dictionary {
				if strings.Contains(r.Name, root) {
					meanings = append(meanings, mean)
				}
			}
			if len(meanings) > 0 {
				r.Meaning = strings.Join(meanings, ", ")
				allNames = append(allNames, r)
			}
		}
	}

	total := len(allNames)
	fmt.Printf("Total names to update: %d\n", total)

	// แบ่งอัปเดตเป็นชุด (Batch) ละ 1,000 รายชื่อ
	batchSize := 1000
	startTime := time.Now()

	for i := 0; i < total; i += batchSize {
		end := i + batchSize
		if end > total {
			end = total
		}

		// เริ่ม Transaction สำหรับ Batch นี้
		tx, err := db.Begin()
		if err != nil {
			log.Fatal(err)
		}

		for _, r := range allNames[i:end] {
			_, err = tx.Exec("UPDATE names_miracle SET meaning = $1 WHERE name_id = $2", r.Meaning, r.ID)
			if err != nil {
				tx.Rollback()
				log.Fatalf("Batch Update Error at ID %d: %v", r.ID, err)
			}
		}

		// ยืนยันการบันทึกทั้ง 1,000 รายชื่อในครั้งเดียว
		if err := tx.Commit(); err != nil {
			log.Fatal(err)
		}

		pct := float64(end) / float64(total) * 100
		fmt.Printf("⚡️ Speed: Processed %d/%d (%.2f%%) - Time elapsed: %v\n", end, total, pct, time.Since(startTime))
	}

	fmt.Printf("\n🎉 MISSION COMPLETE! Processed %d names in %v\n", total, time.Since(startTime))
}
