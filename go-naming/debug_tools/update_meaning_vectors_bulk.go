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

// เชื่อมต่อไปยัง Ubuntu Server โดยตรง
const (
	serverIP   = "43.228.85.200"
	serverPort = "5432"
	dbUser     = "tayap"
	dbPass     = "IntelliP24.X"
	dbName     = "tayap"

	batchSize  = 100 // จำนวนชื่อที่ดึงมาทีละครั้ง
	numWorkers = 3   // จำนวน goroutines ที่ทำงานพร้อมกัน
)

type OllamaRequest struct {
	Model string `json:"model"`
	Input string `json:"input"`
}

type OllamaResponse struct {
	Embeddings [][]float64 `json:"embeddings"`
}

type NameRecord struct {
	ID      int
	Name    string
	Meaning string
}

func getEmbedding(text string) ([]float64, error) {
	url := "http://localhost:11434/api/embed"
	reqBody := OllamaRequest{
		Model: "nomic-embed-text",
		Input: text,
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

	if len(ollamaResp.Embeddings) > 0 {
		return ollamaResp.Embeddings[0], nil
	}
	return nil, fmt.Errorf("empty embedding from ollama")
}

func formatVector(v []float64) string {
	strV := make([]string, len(v))
	for i, val := range v {
		strV[i] = fmt.Sprintf("%f", val)
	}
	return "[" + strings.Join(strV, ",") + "]"
}

func worker(id int, jobs <-chan NameRecord, results chan<- int, db *sql.DB, wg *sync.WaitGroup) {
	defer wg.Done()

	processed := 0
	for record := range jobs {
		// สร้างบริบทความหมายให้ AI เข้าใจ
		contextText := fmt.Sprintf("Thai Name: %s (Means: %s)", record.Name, record.Meaning)

		vector, err := getEmbedding(contextText)
		if err != nil {
			log.Printf("[Worker %d] Ollama Error for %s: %v", id, record.Name, err)
			continue
		}

		vectorStr := formatVector(vector)
		_, err = db.Exec("UPDATE names_miracle SET meaning_vector = $1 WHERE name_id = $2", vectorStr, record.ID)
		if err != nil {
			log.Printf("[Worker %d] DB Update Error for %s: %v", id, record.Name, err)
			continue
		}

		processed++
		if processed%10 == 0 {
			fmt.Printf("[Worker %d] Processed %d vectors (Latest: %s)\n", id, processed, record.Name)
		}
	}

	results <- processed
}

func main() {
	startTime := time.Now()

	// เชื่อมต่อไปยัง Ubuntu
	connStr := fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=disable",
		dbUser, dbPass, serverIP, serverPort, dbName)
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("DB Connect Error: %v", err)
	}
	defer db.Close()

	// ตรวจสอบจำนวนที่ต้องทำ
	var totalCount int
	err = db.QueryRow("SELECT COUNT(*) FROM names_miracle WHERE meaning != '' AND meaning_vector IS NULL").Scan(&totalCount)
	if err != nil {
		log.Fatalf("Count Error: %v", err)
	}

	fmt.Printf("🚀 Starting Meaning Vector Generation\n")
	fmt.Printf("📊 Total names to process: %d\n", totalCount)
	fmt.Printf("⚙️  Workers: %d, Batch Size: %d\n\n", numWorkers, batchSize)

	// สร้าง channels
	jobs := make(chan NameRecord, batchSize)
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
			query := fmt.Sprintf("SELECT name_id, thname, COALESCE(meaning, '') FROM names_miracle WHERE meaning != '' AND meaning_vector IS NULL ORDER BY name_id LIMIT %d OFFSET %d", batchSize, offset)
			rows, err := db.Query(query)
			if err != nil {
				log.Printf("Query Error: %v", err)
				break
			}

			hasData := false
			for rows.Next() {
				hasData = true
				var record NameRecord
				if err := rows.Scan(&record.ID, &record.Name, &record.Meaning); err != nil {
					continue
				}
				jobs <- record
			}
			rows.Close()

			if !hasData {
				break
			}
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
	fmt.Printf("✅ Updated %d meaning vectors\n", totalProcessed)
	fmt.Printf("⏱️  Time taken: %s\n", duration.Round(time.Second))
	fmt.Printf("📈 Speed: %.2f vectors/second\n", float64(totalProcessed)/duration.Seconds())
}
