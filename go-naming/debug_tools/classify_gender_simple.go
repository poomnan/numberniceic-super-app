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

	numWorkers = 3 // 3 workers พร้อมกัน
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

func classifyGender(name string) (string, error) {
	prompt := fmt.Sprintf(`ชื่อไทย "%s" เหมาะกับเพศใด?
ตอบเพียงคำเดียว: male, female, หรือ neutral`, name)

	url := "http://localhost:11434/api/generate"
	reqBody := OllamaRequest{
		Model:  "qwen2.5:7b",
		Prompt: prompt,
		Stream: false,
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		return "", err
	}

	resp, err := http.Post(url, "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	var ollamaResp OllamaResponse
	if err := json.NewDecoder(resp.Body).Decode(&ollamaResp); err != nil {
		return "", err
	}

	// Parse response
	response := strings.ToLower(strings.TrimSpace(ollamaResp.Response))

	// Extract gender
	if strings.Contains(response, "male") && !strings.Contains(response, "female") {
		return "male", nil
	} else if strings.Contains(response, "female") {
		return "female", nil
	} else {
		return "neutral", nil
	}
}

func worker(id int, jobs <-chan NameRecord, results chan<- int, db *sql.DB, wg *sync.WaitGroup) {
	defer wg.Done()

	processed := 0
	for record := range jobs {
		gender, err := classifyGender(record.Name)
		if err != nil {
			log.Printf("[Worker %d] AI Error for %s: %v", id, record.Name, err)
			continue
		}

		_, err = db.Exec("UPDATE names_miracle SET gender = $1 WHERE name_id = $2", gender, record.ID)
		if err != nil {
			log.Printf("[Worker %d] DB Update Error for %s: %v", id, record.Name, err)
			continue
		}

		processed++
		if processed%50 == 0 {
			fmt.Printf("[Worker %d] Classified %d names (Latest: %s -> %s)\n", id, processed, record.Name, gender)
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
	fmt.Printf("⚙️  Workers: %d\n\n", numWorkers)

	// สร้าง channels
	jobs := make(chan NameRecord, 100)
	results := make(chan int, numWorkers)
	var wg sync.WaitGroup

	// เริ่ม workers
	for w := 1; w <= numWorkers; w++ {
		wg.Add(1)
		go worker(w, jobs, results, db, &wg)
	}

	// ส่งงานเข้า queue
	go func() {
		rows, err := db.Query("SELECT name_id, thname FROM names_miracle WHERE gender = 'neutral' OR gender IS NULL ORDER BY name_id")
		if err != nil {
			log.Printf("Query Error: %v", err)
			close(jobs)
			return
		}
		defer rows.Close()

		for rows.Next() {
			var record NameRecord
			if err := rows.Scan(&record.ID, &record.Name); err != nil {
				continue
			}
			jobs <- record
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
	fmt.Printf("📈 Speed: %.2f names/second\n", float64(totalProcessed)/duration.Seconds())

	// แสดงสถิติ
	var maleCount, femaleCount, neutralCount int
	db.QueryRow("SELECT COUNT(*) FROM names_miracle WHERE gender = 'male'").Scan(&maleCount)
	db.QueryRow("SELECT COUNT(*) FROM names_miracle WHERE gender = 'female'").Scan(&femaleCount)
	db.QueryRow("SELECT COUNT(*) FROM names_miracle WHERE gender = 'neutral'").Scan(&neutralCount)

	total := maleCount + femaleCount + neutralCount
	fmt.Printf("\n📊 Gender Distribution:\n")
	fmt.Printf("   Male: %d (%.1f%%)\n", maleCount, float64(maleCount)/float64(total)*100)
	fmt.Printf("   Female: %d (%.1f%%)\n", femaleCount, float64(femaleCount)/float64(total)*100)
	fmt.Printf("   Neutral: %d (%.1f%%)\n", neutralCount, float64(neutralCount)/float64(total)*100)
}
