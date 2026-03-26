package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"
	"time"

	_ "github.com/lib/pq"
)

// Config
const (
	host      = "localhost"
	port      = 5432
	user      = "tayap"
	password  = "IntelliP24.X"
	dbname    = "tayap"
	batchSize = 100 // Process names in batches
)

type OllamaRequest struct {
	Model string `json:"model"`
	Input string `json:"input"`
}

type OllamaResponse struct {
	Embeddings [][]float64 `json:"embeddings"`
}

func main() {
	// Connect DB
	connStr := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable",
		host, port, user, password, dbname)
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("DB Connect Error: %v", err)
	}
	defer db.Close()

	// Configuration - Start from beginning to fix dim 0 issue
	startID := 0
	totalProcessed := 0

	fmt.Printf("Starting Full Update Process from ID: %d using /api/embed\n", startID)

	for {
		// 1. Fetch batch
		rows, err := db.Query("SELECT name_id, thname FROM names_miracle WHERE name_id >= $1 ORDER BY name_id ASC LIMIT $2", startID, batchSize)
		if err != nil {
			log.Fatalf("Query Error: %v", err)
		}

		type NameItem struct {
			ID   int
			Name string
		}
		var items []NameItem

		for rows.Next() {
			var n NameItem
			if err := rows.Scan(&n.ID, &n.Name); err != nil {
				log.Println("Scan Error:", err)
				continue
			}
			items = append(items, n)
		}
		rows.Close()

		if len(items) == 0 {
			fmt.Println("\nAll records processed! Finished.")
			break
		}

		// 2. Process batch
		fmt.Printf("\nProcessing Batch (ID %d - %d)...\n", items[0].ID, items[len(items)-1].ID)

		for _, item := range items {
			embedding, err := getEmbedding(item.Name)
			if err != nil {
				log.Printf(" [!] Ollama Error for ID %d (%s): %v", item.ID, item.Name, err)
				continue
			}

			if len(embedding) == 0 {
				log.Printf(" [!] ID %d (%s): Empty Embedding!", item.ID, item.Name)
				continue
			}

			// Update DB
			_, err = db.Exec("UPDATE names_miracle SET th_name_vector = $1 WHERE name_id = $2",
				formatVector(embedding), item.ID)

			if err != nil {
				log.Printf(" [!] DB Update Error for ID %d: %v", item.ID, err)
			} else {
				fmt.Print(".")
			}

			startID = item.ID + 1
			totalProcessed++
		}

		fmt.Printf(" %d Done.", totalProcessed)
		time.Sleep(50 * time.Millisecond)
	}
}

func getEmbedding(text string) ([]float64, error) {
	url := "http://localhost:11434/api/embed"
	reqBody := OllamaRequest{Model: "nomic-embed-text", Input: text}

	jsonData, _ := json.Marshal(reqBody)
	resp, err := http.Post(url, "application/json", strings.NewReader(string(jsonData)))
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		return nil, fmt.Errorf("bad status: %s", resp.Status)
	}

	var ollamaResp OllamaResponse
	if err := json.NewDecoder(resp.Body).Decode(&ollamaResp); err != nil {
		return nil, err
	}
	if len(ollamaResp.Embeddings) > 0 {
		return ollamaResp.Embeddings[0], nil
	}
	return nil, fmt.Errorf("no embedding returned")
}

func formatVector(v []float64) string {
	strs := make([]string, len(v))
	for i, val := range v {
		strs[i] = fmt.Sprintf("%f", val)
	}
	return "[" + strings.Join(strs, ",") + "]"
}
