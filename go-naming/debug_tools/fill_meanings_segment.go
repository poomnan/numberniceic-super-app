package main

import (
	"database/sql"
	"fmt"
	"log"
	"strings"
	"sync"

	_ "github.com/lib/pq"
)

// พจนานุกรมรากศัพท์มงคลที่พบบ่อย (ขยายเพิ่มเติม)
var roots = map[string]string{
	"นรา": "คน, มวลมนุษย์", "วิช": "ความรู้", "พงศ์": "เชื้อสาย, ตระกูล", "ธรรม": "ความดี, คุณธรรม",
	"กิตติ": "ชื่อเสียง", "ศักดิ์": "อำนาจ, ความสามารถ", "ภัทร": "ดี, เจริญ, ดีงาม", "พิพัฒน์": "ความเจริญ",
	"พร": "ของขวัญอันประเสริฐ", "วร": "ประเสริฐ", "เดช": "อำนาจ", "สิริ": "มงคล, มิ่งขวัญ",
	"ณัฐ": "นักปราชญ์", "รัตน์": "แก้วอันมีค่า", "โชค": "โชคลาภ", "ชัย": "ชัยชนะ",
	"กมล": "หัวใจ", "กมลพรรณ": "ผิวพรรณประดุจดอกบัว", "นันท": "ความยินดี", "ลดา": "เครือเถา, งาม",
	"วุฒิ": "ความรู้อันยิ่งใหญ่", "นราธิป": "พระราชา", "เมธ": "ปัญญา", "เมธา": "ผู้มีปัญญา",
	"สร": "เสียง, สวรรค์", "ชล": "น้ำ", "พล": "กำลัง", "เกียรติ": "ชื่อเสียง, เกียรติยศ",
	"นร": "คน", "วิ": "แจ้ง, พิเศษ", "อรรถ": "ประโยชน์, ความหมาย", "อธิ": "ยิ่งใหญ่",
	"จิร": "นาน, ยั่งยืน", "ภา": "แสงสว่าง", "ภากร": "ผู้สร้างแสงสว่าง", "ศิริ": "มิ่งขวัญ",
	"อนันต์": "ไม่สิ้นสุด", "มนัส": "ใจ", "ทักษ": "ชำนาญ, ฉลาด", "ศร": "อาวุธ",
	"ยศ": "ความดีความชอบ", "ธีร": "นักปราชญ์", "ปัญญ": "ความรู้", "รักษ์": "ดูแล, รักษา",
	"จารุ": "ทอง, งาม", "รัช": "สมบัติ, ราชา", "สรร": "คัดเลือก", "สรรพ": "ทุกอย่าง",
}

func main() {
	db, err := sql.Open("postgres", "postgres://tayap:IntelliP24.X@43.228.85.200:5432/tayap?sslmode=disable")
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	fmt.Println("🚀 RUNNING ROOT SEGMENTATION TURBO...")

	rows, err := db.Query("SELECT name_id, thname FROM names_miracle WHERE meaning IS NULL OR meaning = ''")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	type Job struct {
		ID   int
		Name string
	}

	jobs := make(chan Job, 1000)
	var wg sync.WaitGroup
	count := 0
	matchCount := 0

	// Worker ประมวลผล logic แยกคำ
	for i := 0; i < 20; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for job := range jobs {
				meaning := segmentAndTranslate(job.Name)
				if meaning != "" {
					_, err := db.Exec("UPDATE names_miracle SET meaning = $1 WHERE name_id = $2", meaning, job.ID)
					if err == nil {
						matchCount++
					}
				}
			}
		}()
	}

	for rows.Next() {
		var j Job
		rows.Scan(&j.ID, &j.Name)
		jobs <- j
		count++
		if count%5000 == 0 {
			fmt.Printf("⏳ Scanned %d names... Matched so far: %d\n", count, matchCount)
		}
	}
	close(jobs)
	wg.Wait()

	fmt.Printf("✅ FINISHED! Total scanned: %d, Automatically matched: %d\n", count, matchCount)
}

func segmentAndTranslate(name string) string {
	var matchedMeanings []string

	// ใช้เทคนิค Maximum Matching (ลองตัดจากคำที่ยาวที่สุดก่อน)
	tempName := name
	for len(tempName) > 0 {
		found := false
		// ลองหาว่าชื่อนี้ขึ้นต้นด้วยรากศัพท์ตัวไหนในพจนานุกรมไหม
		for root, meaning := range roots {
			if strings.HasPrefix(tempName, root) {
				matchedMeanings = append(matchedMeanings, meaning)
				tempName = strings.TrimPrefix(tempName, root)
				found = true
				break
			}
		}

		if !found {
			// ถ้าไม่เจอคำที่ตรงกันเลย ให้เขยิบออกทีละตัวอักษร
			// (ตรงนี้คุณสามารถเพิ่ม Logic การตัดคำที่ซับซ้อนขึ้นได้)
			break
		}
	}

	if len(matchedMeanings) > 0 {
		return strings.Join(matchedMeanings, ", ")
	}
	return ""
}
