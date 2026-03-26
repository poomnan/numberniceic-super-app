package main

import (
	"bytes"
	"context"
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
	numWorkers  = 4
	bulkSize    = 30
	ollamaURL   = "http://localhost:11434/api/generate"
	ollamaModel = "qwen2:7b"
	dbConnStr   = "postgres://tayap:IntelliP24.X@43.228.85.200:5432/tayap?sslmode=disable"
)

type NameItem struct {
	ID   int
	Name string
}

type OllamaReq struct {
	Model  string `json:"model"`
	Prompt string `json:"prompt"`
	Stream bool   `json:"stream"`
}

type OllamaRes struct {
	Response string `json:"response"`
}

func main() {
	db, err := sql.Open("postgres", dbConnStr)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	fmt.Printf("🚀 STARTING AI STABLE TURBO (Model: %s | Workers: %d)\n", ollamaModel, numWorkers)

	for {
		rows, err := db.Query("SELECT name_id, thname FROM names_miracle WHERE meaning IS NULL OR meaning = '' LIMIT 100")
		if err != nil {
			log.Printf("❌ DB Error: %v", err)
			time.Sleep(5 * time.Second)
			continue
		}

		var items []NameItem
		for rows.Next() {
			var it NameItem
			rows.Scan(&it.ID, &it.Name)
			items = append(items, it)
		}
		rows.Close()

		if len(items) == 0 {
			fmt.Println("🎊 MISSION COMPLETE!")
			break
		}

		var wg sync.WaitGroup
		itemChan := make(chan []NameItem)

		for w := 0; w < numWorkers; w++ {
			wg.Add(1)
			go func() {
				defer wg.Done()
				for batch := range itemChan {
					processBatchStable(db, batch)
				}
			}()
		}

		for i := 0; i < len(items); i += bulkSize {
			end := i + bulkSize
			if end > len(items) {
				end = len(items)
			}
			itemChan <- items[i:end]
		}
		close(itemChan)
		wg.Wait()
		fmt.Printf("✅ Batch Finished.\n")
	}
}

func processBatchStable(db *sql.DB, batch []NameItem) {
	var list []string
	for _, it := range batch {
		list = append(list, fmt.Sprintf("- ID:%d ชื่อ:%s", it.ID, it.Name))
	}

	prompt := fmt.Sprintf(`จงบอกความหมายมงคลสั้นๆ 1-3 คำ ของชื่อไทยต่อไปนี้ ตอบทีละบรรทัดในรูปแบบ "ID=ความหมาย" เท่านั้น ห้ามมีข้อความอื่น
รายชื่อ:
%s
ตัวอย่างคำตอบ:
ID:123=ผู้มีปัญญา
ID:124=ความเจริญ`, strings.Join(list, "\n"))

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	reqBody := OllamaReq{Model: ollamaModel, Prompt: prompt, Stream: false}
	jsonRaw, _ := json.Marshal(reqBody)

	req, _ := http.NewRequestWithContext(ctx, "POST", ollamaURL, bytes.NewBuffer(jsonRaw))
	req.Header.Set("Content-Type", "application/json")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return
	}
	defer resp.Body.Close()

	var res OllamaRes
	json.NewDecoder(resp.Body).Decode(&res)

	// แปลงผลลัพธ์แบบบรรทัดต่อบรรทัด (ทนทานกว่า JSON)
	lines := strings.Split(res.Response, "\n")
	tx, _ := db.Begin()
	for _, line := range lines {
		if !strings.Contains(line, "ID:") || !strings.Contains(line, "=") {
			continue
		}

		// แกะ ID และ ความหมาย (รูปแบบ ID:123=ความหมาย)
		parts := strings.Split(line, "=")
		if len(parts) < 2 {
			continue
		}

		idStr := strings.TrimPrefix(strings.TrimSpace(parts[0]), "ID:")
		meaning := strings.TrimSpace(parts[1])

		var realID int
		fmt.Sscanf(idStr, "%d", &realID)

		if realID > 0 && meaning != "" {
			_, err := tx.Exec("UPDATE names_miracle SET meaning = $1 WHERE name_id = $2", meaning, realID)
			if err == nil {
				fmt.Printf("✨ Saved: ID %d -> %s\n", realID, meaning)
			}
		}
	}
	tx.Commit()
}
