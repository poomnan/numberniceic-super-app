//go:build seed
// +build seed

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

	fmt.Println("Seeding Buddha Pangs into Postgres...")

	data := []struct {
		id   int
		name string
		day  int
		desc string
		img  string
	}{
		{1, "พระประจำวันอาทิตย์ (ปางถวายเนตร)", 1, "มุ่งเน้นการมีสติ ความเพียร และความสำเร็จในหน้าที่การงาน", "/uploads/buddha/pang_sunday.jpg"},
		{2, "พระประจำวันจันทร์ (ปางห้ามสมุทร/ห้ามญาติ)", 2, "เน้นความร่มเย็นเป็นสุข ขจัดปัดเป่าอุปสรรค", "/uploads/buddha/pang_monday.jpg"},
		{3, "พระประจำวันอังคาร (ปางไสยาสน์)", 3, "เน้นความกล้าหาญ การได้รับชัยชนะ และความมีสง่าราศี", "/uploads/buddha/pang_tuesday.jpg"},
		{4, "พระประจำวันพุธกลางวัน (ปางอุ้มบาตร)", 4, "เน้นการเรียกทรัพย์สินโชคลาภ และความอุดมสมบูรณ์", "/uploads/buddha/pang_wednesday.jpg"},
		{5, "พระประจำวันพุธกลางคืน (ปางป่าเลไลยก์)", 8, "เน้นความสงบ ทางออกของปัญหา และการได้รับความช่วยเหลือ", "/uploads/buddha/pang_wednesday.jpg"},
		{6, "พระประจำวันพฤหัสบดี (ปางสมาธิ)", 5, "เน้นปัญญา ความรอบรู้ และความมั่นคงในชีวิต", "/uploads/buddha/pang_thursday.jpg"},
		{9, "พระประจำวันศุกร์ (ปางรำพึง)", 6, "เน้นความรัก ความเมตตา และเสน่ห์มหานิยม", "/uploads/buddha/pang_friday.jpg"},
		{8, "พระประจำวันเสาร์ (ปางนาคปรก)", 7, "เน้นความคุ้มครอง ป้องกันภัย และความหนักแน่นมั่นคง", "/uploads/buddha/pang_saturday.jpg"},
		{10, "พระปางมารวิชัย (ชนะมาร)", 0, "พระพุทธรูปชนะมาร เหมาะสำหรับผู้ที่ต้องการชนะอุปสรรคศัตรูหมู่มารทั้งปวง", "/uploads/buddha/default.jpg"},
		{11, "พระปางประทานพร", 91, "พระพุทธรูปแห่งการให้พร เหมาะสำหรับผู้ที่ต้องการความสำเร็จสมปรารถนา", "/uploads/buddha/default.jpg"},
		{12, "พระปางลีลา", 0, "พระพุทธรูปแห่งความก้าวหน้า เหมาะสำหรับผู้ที่ต้องการความเจริญรุ่งเรือง", "/uploads/buddha/default.jpg"},
		{13, "พระปางเปิดโลก", 0, "พระพุทธรูปแห่งการเปิดทางสว่าง เหมาะสำหรับผู้ที่ต้องการทางออกและโอกาสใหม่ๆ", "/uploads/buddha/default.jpg"},
		{14, "พระพุทธชินราช", 0, "พระพุทธรูปศักดิ์สิทธิ์คู่บ้านคู่เมือง เสริมบารมีและอำนาจวาสนา", "/uploads/buddha/default.jpg"},
		{15, "หลวงพ่อโสธร", 0, "พระพุทธรูปศักเสิทธิ์ที่ประทานความสำเร็จ โชคลาภ และสุขภาพแข็งแรง", "/uploads/buddha/default.jpg"},
	}

	for _, d := range data {
		_, err = db.Exec(`INSERT INTO buddha_pangs (id, pang_name, buddha_day, description, image_url) 
			VALUES ($1, $2, $3, $4, $5) 
			ON CONFLICT (id) DO UPDATE SET 
			pang_name = EXCLUDED.pang_name, 
			buddha_day = EXCLUDED.buddha_day, 
			description = EXCLUDED.description, 
			image_url = EXCLUDED.image_url`,
			d.id, d.name, d.day, d.desc, d.img)
		if err != nil {
			log.Printf("Error inserting %s: %v", d.name, err)
		}
	}

	fmt.Println("Seeding completed.")
}
