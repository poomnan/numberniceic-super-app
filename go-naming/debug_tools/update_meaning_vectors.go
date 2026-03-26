package main

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"

	_ "github.com/lib/pq"
)

// เชื่อมต่อไปยัง Ubuntu Server โดยตรง
const (
	serverIP   = "43.228.85.200"
	serverPort = "5432"
	dbUser     = "tayap"
	dbPass     = "IntelliP24.X"
	dbName     = "tayap"
)

type OllamaRequest struct {
	Model string `json:"model"`
	Input string `json:"input"`
}

type OllamaResponse struct {
	Embeddings [][]float64 `json:"embeddings"`
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

func main() {
	// เชื่อมต่อไปยัง Ubuntu (ใช้ User/Pass ที่คุณให้มา)
	connStr := fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=disable",
		dbUser, dbPass, serverIP, serverPort, dbName)
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("DB Connect Error: %v", err)
	}
	defer db.Close()

	fmt.Println("🚀 Processing Meaning Vectors from Mac to Ubuntu...")

	rows, err := db.Query("SELECT name_id, thname, COALESCE(meaning, '') FROM names_miracle WHERE meaning != '' AND meaning_vector IS NULL LIMIT 10000")
	if err != nil {
		log.Fatalf("Query Error: %v", err)
	}
	defer rows.Close()

	count := 0
	for rows.Next() {
		var id int
		var name, meaning string
		if err := rows.Scan(&id, &name, &meaning); err != nil {
			continue
		}

		// สร้างบริบทความหมายให้ AI เข้าใจ (Context Injection)
		contextText := fmt.Sprintf("Thai Name: %s (Means: %s)", name, meaning)

		vector, err := getEmbedding(contextText)
		if err != nil {
			log.Printf("Ollama Error for %s: %v", name, err)
			continue
		}

		vectorStr := formatVector(vector)
		_, err = db.Exec("UPDATE names_miracle SET meaning_vector = $1 WHERE name_id = $2", vectorStr, id)
		if err != nil {
			log.Printf("DB Update Error for %s: %v", name, err)
			continue
		}

		count++
		if count%50 == 0 {
			fmt.Printf("✅ Updated %d vectors... (Latest: %s)\n", count, name)
		}
	}

	fmt.Printf("\n🎉 Success! Updated %d meaning vectors.\n", count)
}
