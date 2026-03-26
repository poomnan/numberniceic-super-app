package main

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"
	"sync"
	"time"

	_ "github.com/lib/pq"
)

const (
	serverIP   = "43.228.85.200"
	serverPort = "5432"
	dbUser     = "tayap"
	dbPass     = "IntelliP24.X"
	dbName     = "tayap"

	batchSize  = 20 // เพิ่มเป็น 20 เพื่อความเร็วบน Mac M1
	numWorkers = 8  // เพิ่มเป็น 8 workers (Mac M1 32GB รับไหวสบายๆ)
)

type OllamaRequest struct {
	Model  string `json:"model"`
	Prompt string `json:"prompt"`
	Stream bool   `json:"stream"`
}

type OllamaResponse struct {
	Response string `json:"response"`
}

type NameRecord struct {
	ID   int
	Name string
}

func classifyGender(names []NameRecord) (map[int]string, error) {
	// สร้าง prompt สำหรับ bulk classification
	nameList := make([]string, len(names))
	for i, n := range names {
		nameList[i] = n.Name
	}

	prompt := fmt.Sprintf(`จงจำแนกเพศของชื่อไทยต่อไปนี้ว่าเป็น "male", "female", หรือ "neutral"

ชื่อที่ต้องจำแนก:
%s

กฎการจำแนก:
1. "male" สำหรับชื่อที่ฟังดูเป็นผู้ชาย (เช่น ลงท้ายด้วย ชัย, พล, ศักดิ์, วัฒน์, กร, เดช)
2. "female" สำหรับชื่อที่ฟังดูเป็นผู้หญิง (เช่น ลงท้ายด้วย พร, ศรี, วรรณ, รัตน์, ภรณ์, นภา, ญา)
3. "neutral" สำหรับชื่อที่ใช้ได้ทั้งสองเพศจริงๆ หรือชื่อนามสกุล/ชื่อวัด/ชื่อสถานที่
4. พยายามเลือก male หรือ female หากมีแนวโน้มชัดเจน อย่าตอบ neutral พร่ำเพรื่อ

ตอบเป็น JSON array ของ string เท่านั้น ในรูปแบบ: ["male", "female", "neutral", ...]
จำนวนคำตอบต้องเท่ากับจำนวนชื่อที่ส่งไป (%d ชื่อ)
ไม่ต้องอธิบาย ตอบแค่ JSON array`, strings.Join(nameList, "\n"), len(names))

	url := "http://localhost:11434/api/generate"
	reqBody := OllamaRequest{
		Model:  "qwen2:7b",
		Prompt: prompt,
		Stream: false,
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		return nil, err
	}

	resp, err := http.Post(url, "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	var ollamaResp OllamaResponse
	if err := json.NewDecoder(resp.Body).Decode(&ollamaResp); err != nil {
		return nil, err
	}

	// Parse JSON array response
	response := strings.TrimSpace(ollamaResp.Response)

	// ลบ markdown code block ถ้ามี
	response = strings.TrimPrefix(response, "```json")
	response = strings.TrimPrefix(response, "```")
	response = strings.TrimSuffix(response, "```")
	response = strings.TrimSpace(response)

	var genders []string
	if err := json.Unmarshal([]byte(response), &genders); err != nil {
		log.Printf("Failed to parse response: %s", response)
		return nil, err
	}

	// Map results back to IDs
	result := make(map[int]string)
	for i, gender := range genders {
		if i < len(names) {
			// Normalize gender value
			g := strings.ToLower(strings.TrimSpace(gender))
			if g != "male" && g != "female" && g != "neutral" {
				g = "neutral" // default
			}
			result[names[i].ID] = g
		}
	}

	return result, nil
}

func worker(id int, jobs <-chan []NameRecord, results chan<- int, db *sql.DB, wg *sync.WaitGroup) {
	defer wg.Done()

	processed := 0
	for batch := range jobs {
		genderMap, err := classifyGender(batch)
		if err != nil {
			log.Printf("[Worker %d] AI Error: %v", id, err)
			continue
		}

		// Update database
		for nameID, gender := range genderMap {
			_, err := db.Exec("UPDATE names_miracle SET gender = $1 WHERE name_id = $2", gender, nameID)
			if err != nil {
				log.Printf("[Worker %d] DB Update Error for ID %d: %v", id, nameID, err)
				continue
			}
			processed++
		}

		if processed > 0 && processed%100 == 0 {
			fmt.Printf("[Worker %d] Classified %d names\n", id, processed)
		}
	}

	results <- processed
}

func main() {
	startTime := time.Now()

	connStr := fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=disable",
		dbUser, dbPass, serverIP, serverPort, dbName)
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("DB Connect Error: %v", err)
	}
	defer db.Close()

	// ตรวจสอบจำนวนที่ต้องทำ
	var totalCount int
	err = db.QueryRow("SELECT COUNT(*) FROM names_miracle WHERE gender = 'neutral' OR gender IS NULL").Scan(&totalCount)
	if err != nil {
		log.Fatalf("Count Error: %v", err)
	}

	fmt.Printf("🚀 Starting Gender Classification\n")
	fmt.Printf("📊 Total names to classify: %d\n", totalCount)
	fmt.Printf("⚙️  Workers: %d, Batch Size: %d\n\n", numWorkers, batchSize)

	// สร้าง channels
	jobs := make(chan []NameRecord, 10)
	results := make(chan int, numWorkers)
	var wg sync.WaitGroup

	// เริ่ม workers
	for w := 1; w <= numWorkers; w++ {
		wg.Add(1)
		go worker(w, jobs, results, db, &wg)
	}

	// ส่งงานเข้า queue
	go func() {
		offset := 0
		for {
			query := fmt.Sprintf("SELECT name_id, thname FROM names_miracle WHERE gender = 'neutral' OR gender IS NULL ORDER BY name_id LIMIT %d OFFSET %d", batchSize, offset)
			rows, err := db.Query(query)
			if err != nil {
				log.Printf("Query Error: %v", err)
				break
			}

			batch := []NameRecord{}
			for rows.Next() {
				var record NameRecord
				if err := rows.Scan(&record.ID, &record.Name); err != nil {
					continue
				}
				batch = append(batch, record)
			}
			rows.Close()

			if len(batch) == 0 {
				break
			}

			jobs <- batch
			offset += batchSize
		}
		close(jobs)
	}()

	// รอ workers เสร็จ
	wg.Wait()
	close(results)

	// รวมผลลัพธ์
	totalProcessed := 0
	for count := range results {
		totalProcessed += count
	}

	duration := time.Since(startTime)
	fmt.Printf("\n🎉 Success!\n")
	fmt.Printf("✅ Classified %d names\n", totalProcessed)
	fmt.Printf("⏱️  Time taken: %s\n", duration.Round(time.Second))

	// แสดงสถิติ
	var maleCount, femaleCount, neutralCount int
	db.QueryRow("SELECT COUNT(*) FROM names_miracle WHERE gender = 'male'").Scan(&maleCount)
	db.QueryRow("SELECT COUNT(*) FROM names_miracle WHERE gender = 'female'").Scan(&femaleCount)
	db.QueryRow("SELECT COUNT(*) FROM names_miracle WHERE gender = 'neutral'").Scan(&neutralCount)

	fmt.Printf("\n📊 Gender Distribution:\n")
	fmt.Printf("   Male: %d (%.1f%%)\n", maleCount, float64(maleCount)/float64(totalCount)*100)
	fmt.Printf("   Female: %d (%.1f%%)\n", femaleCount, float64(femaleCount)/float64(totalCount)*100)
	fmt.Printf("   Neutral: %d (%.1f%%)\n", neutralCount, float64(neutralCount)/float64(totalCount)*100)
}
