package main

import (
	"bytes"
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"regexp"
	"strings"
	"sync"
	"time"

	_ "github.com/lib/pq"
)

const (
	numWorkers  = 4 // ลดเหลือ 4 เพื่อให้ 7b วิ่งนิ่งที่สุดบน M1
	ollamaURL   = "http://localhost:11434/api/generate"
	ollamaModel = "qwen2:7b" // กลับมาใช้ 7b เพื่อความถูกต้องของความหมาย
	dbConnStr   = "postgres://tayap:IntelliP24.X@43.228.85.200:5432/tayap?sslmode=disable"
)

var (
	thaiOnlyRegex = regexp.MustCompile(`[^\p{Thai}\s]`)
	replacer      = strings.NewReplacer("หมายถึง", "", "แปลว่า", "", "คือ", "", "ชื่อนี้", "", ",", " ", "，", " ", "、", " ")
)

type OllamaReq struct {
	Model  string `json:"model"`
	Prompt string `json:"prompt"`
	Stream bool   `json:"stream"`
}

type OllamaRes struct {
	Response string `json:"response"`
}

type NameJob struct {
	ID   int
	Name string
}

type Result struct {
	ID      int
	Meaning string
	Name    string
}

func main() {
	db, err := sql.Open("postgres", dbConnStr)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	fmt.Printf("🚀 M1 QUALITY MODE STARTED [%s | %d Workers]\n", ollamaModel, numWorkers)

	for {
		fmt.Printf("🔍 Fetching names... ")
		rows, err := db.Query("SELECT name_id, thname FROM names_miracle WHERE (meaning IS NULL OR meaning = '') LIMIT 60")
		if err != nil {
			log.Printf("\n❌ DB Error: %v", err)
			time.Sleep(5 * time.Second)
			continue
		}

		jobs := make(chan NameJob, 60)
		results := make(chan Result, 60)
		var wg sync.WaitGroup

		for w := 1; w <= numWorkers; w++ {
			wg.Add(1)
			go func() {
				defer wg.Done()
				for job := range jobs {
					meaning, err := askAI(job.Name)
					if err != nil {
						continue
					}
					results <- Result{ID: job.ID, Meaning: meaning, Name: job.Name}
				}
			}()
		}

		count := 0
		for rows.Next() {
			var r NameJob
			rows.Scan(&r.ID, &r.Name)
			jobs <- r
			count++
		}
		rows.Close()
		close(jobs)

		fmt.Printf("found %d names. 🧠 Thinking... \n", count)

		if count == 0 {
			fmt.Println("🎊 MISSION COMPLETE!")
			break
		}

		go func() {
			wg.Wait()
			close(results)
		}()

		tx, _ := db.Begin()
		batchDone := 0
		for res := range results {
			_, err := tx.Exec("UPDATE names_miracle SET meaning = $1 WHERE name_id = $2", res.Meaning, res.ID)
			if err == nil {
				batchDone++
				fmt.Printf("\r✨ Progress: [%d/%d] %s -> %s          ", batchDone, count, res.Name, res.Meaning)
			}
		}
		tx.Commit()
		fmt.Printf("\n✅ Batch Finished.\n")
	}
}

func askAI(name string) (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()

	// แบบจำลองตัวอย่าง (Few-Shot) เพื่อให้ AI เข้าใจรูปแบบที่ต้องการ
	prompt := fmt.Sprintf(`### ระบบ: คุณคือผู้เชี่ยวชาญด้านรากศัพท์บาลี-สันสกฤต ให้ความหมายมงคลสั้นๆ 1-3 คำเท่านั้น
ชื่อ: สมชาย -> ผู้ชายที่มีบารมี
ชื่อ: จารุรัชช -> ความเจริญรุ่งเรืองด้วยสมบัติทอง
ชื่อ: ทักษิณ -> ทิศใต้หรือเบื้องขวา
ชื่อ: %s -> `, name)

	reqBody := OllamaReq{Model: ollamaModel, Prompt: prompt, Stream: false}
	jsonRaw, _ := json.Marshal(reqBody)

	req, _ := http.NewRequestWithContext(ctx, "POST", ollamaURL, bytes.NewBuffer(jsonRaw))
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	var res OllamaRes
	json.NewDecoder(resp.Body).Decode(&res)

	meaning := strings.Split(res.Response, "\n")[0]
	meaning = strings.TrimSpace(meaning)

	// ล้างชื่อตัวเองทิ้งถ้า AI แอบใส่มา
	meaning = strings.ReplaceAll(meaning, name, "")
	meaning = replacer.Replace(meaning)
	meaning = thaiOnlyRegex.ReplaceAllString(meaning, " ")
	meaning = strings.TrimSpace(meaning)

	if len(meaning) > 100 {
		meaning = meaning[:100]
	}
	return meaning, nil
}
