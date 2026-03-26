package main

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"
	"time"

	_ "github.com/lib/pq"
)

const (
	DB_HOST     = "43.228.85.200"
	DB_PORT     = 5432
	DB_USER     = "tayap"
	DB_PASSWORD = "IntelliP24.X"
	DB_NAME     = "tayap"
	OLLAMA_URL  = "http://localhost:11434/api/generate"
	MODEL_NAME  = "qwen2:7b"
	BATCH_SIZE  = 3 // Even smaller for retry
)

type NameRecord struct {
	ID   int
	Name string
}

type OllamaRequest struct {
	Model  string `json:"model"`
	Prompt string `json:"prompt"`
	Stream bool   `json:"stream"`
	Format string `json:"format"`
}

type OllamaResponse struct {
	Response string `json:"response"`
}

func main() {
	psqlInfo := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable",
		DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME)

	db, err := sql.Open("postgres", psqlInfo)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	err = db.Ping()
	if err != nil {
		log.Fatal(err)
	}
	fmt.Println("Successfully connected to database!")

	// Fetch ONLY neutral names (skipped ones)
	fmt.Println("Fetching names that are still 'neutral'...")
	rows, err := db.Query("SELECT name_id, thname FROM names_miracle WHERE gender = 'neutral' ORDER BY name_id")
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	var records []NameRecord
	for rows.Next() {
		var r NameRecord
		if err := rows.Scan(&r.ID, &r.Name); err != nil {
			log.Fatal(err)
		}
		records = append(records, r)
	}
	fmt.Printf("Found %d neutral names to retry.\n", len(records))

	if len(records) == 0 {
		fmt.Println("✅ All names have been classified!")
		return
	}

	// Process in batches
	total := len(records)
	for i := 0; i < total; i += BATCH_SIZE {
		end := i + BATCH_SIZE
		if end > total {
			end = total
		}
		batch := records[i:end]
		processBatch(db, batch, i, total)
		time.Sleep(500 * time.Millisecond) // Slow down to give AI more time
	}
}

func processBatch(db *sql.DB, batch []NameRecord, current int, total int) {
	idMap := make(map[string]int)
	for _, r := range batch {
		idMap[r.Name] = r.ID
	}

	// Build numbered list with EXTRA emphasis
	var numberedList []string
	for i, r := range batch {
		numberedList = append(numberedList, fmt.Sprintf("%d. %s", i+1, r.Name))
	}

	prompt := fmt.Sprintf(`You are classifying Thai names by gender.

Names to classify:
%s

For EACH name above, output ONE line in this format:
Number|Name|Gender

Gender options: Male, Female, Neutral

Example output:
1|สมชาย|Male
2|สมหญิง|Female
3|สมนึก|Neutral

NOW classify ALL %d names:`,
		strings.Join(numberedList, "\n"),
		len(batch))

	reqBody := OllamaRequest{
		Model:  MODEL_NAME,
		Prompt: prompt,
		Stream: false,
		Format: "",
	}
	jsonData, _ := json.Marshal(reqBody)

	client := &http.Client{Timeout: 180 * time.Second} // Longer timeout
	resp, err := client.Post(OLLAMA_URL, "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		log.Printf("Error calling Ollama: %v", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		log.Printf("Ollama returned status: %d", resp.StatusCode)
		return
	}

	var ollamaResp OllamaResponse
	if err := json.NewDecoder(resp.Body).Decode(&ollamaResp); err != nil {
		log.Printf("Error decoding Ollama response: %v", err)
		return
	}

	// Parse response
	lines := strings.Split(ollamaResp.Response, "\n")
	resultMap := make(map[string]string)

	for _, line := range lines {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}

		line = strings.TrimPrefix(line, "- ")
		line = strings.TrimPrefix(line, "* ")

		parts := strings.Split(line, "|")
		if len(parts) >= 3 {
			n := strings.TrimSpace(parts[1])
			g := strings.TrimSpace(parts[2])
			resultMap[n] = g
		} else if len(parts) >= 2 {
			n := strings.TrimSpace(parts[0])
			g := strings.TrimSpace(parts[1])
			resultMap[n] = g
		}
	}

	if len(resultMap) < len(batch) {
		log.Printf("⚠️ AI returned %d/%d names", len(resultMap), len(batch))
		log.Printf("Raw response: %s", ollamaResp.Response)
	}

	// Update DB
	tx, err := db.Begin()
	if err != nil {
		log.Println("Tx Begin error:", err)
		return
	}

	stmt, err := tx.Prepare("UPDATE names_miracle SET gender = $1 WHERE name_id = $2")
	if err != nil {
		log.Println("Prepare error:", err)
		tx.Rollback()
		return
	}
	defer stmt.Close()

	updates := 0
	for _, r := range batch {
		name := strings.TrimSpace(r.Name)
		if gender, ok := resultMap[name]; ok {
			g := strings.ToLower(gender)
			if strings.Contains(g, "male") && !strings.Contains(g, "female") {
				g = "male"
			} else if strings.Contains(g, "female") {
				g = "female"
			} else {
				g = "neutral"
			}

			_, err := stmt.Exec(g, r.ID)
			if err != nil {
				log.Printf("Update error for %s: %v", r.Name, err)
			} else {
				updates++
			}
		} else {
			log.Printf("⚠️ Skipped: %s", name)
		}
	}

	err = tx.Commit()
	if err != nil {
		log.Println("Commit error:", err)
	}

	fmt.Printf("[%d/%d] Retry batch. Updated %d/%d names.\n", current+len(batch), total, updates, len(batch))
}
