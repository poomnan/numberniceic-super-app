package main

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"strings"
	"time"

	_ "github.com/lib/pq"
)

const (
	DBConn    = "user=tayap password=IntelliP24.X dbname=tayap host=127.0.0.1 sslmode=disable"
	OllamaURL = "http://127.0.0.1:11434/api/embeddings"
	ModelName = "nomic-embed-text"
)

type OllamaRequest struct {
	Model  string `json:"model"`
	Prompt string `json:"prompt"`
}

type OllamaResponse struct {
	Embedding []float64 `json:"embedding"`
}

func GetEmbedding(text string) ([]float64, error) {
	reqBody := OllamaRequest{
		Model:  ModelName,
		Prompt: text,
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		return nil, err
	}

	resp, err := http.Post(OllamaURL, "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, fmt.Errorf("error calling ollama: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("status %d: %s", resp.StatusCode, string(body))
	}

	var res OllamaResponse
	if err := json.NewDecoder(resp.Body).Decode(&res); err != nil {
		return nil, fmt.Errorf("decode error: %v", err)
	}

	return res.Embedding, nil
}

func formatVector(embedding []float64) string {
	var strs []string
	for _, val := range embedding {
		strs = append(strs, fmt.Sprintf("%g", val))
	}
	return "[" + strings.Join(strs, ",") + "]"
}

func main() {
	db, err := sql.Open("postgres", DBConn)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	rows, err := db.Query(`
		SELECT dream_id, dream_keyword, dream_meaning, 
		       COALESCE(dream_interpretation, ''), 
		       array_to_string(tags, ',') 
		FROM dreams
	`)
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	for rows.Next() {
		var id int
		var keyword, meaning, interpretation, tags string
		if err := rows.Scan(&id, &keyword, &meaning, &interpretation, &tags); err != nil {
			log.Printf("Scan error: %v", err)
			continue
		}

		// Richer context for embedding
		textToEmbed := fmt.Sprintf("หัวข้อ: %s ความหมาย: %s คำทำนาย: %s แท็ก: %s", keyword, meaning, interpretation, tags)
		fmt.Printf("Embedding [%d] %s...\n", id, keyword)

		vector, err := GetEmbedding(textToEmbed)
		if err != nil {
			log.Printf("Failed to embed %d: %v", id, err)
			continue
		}

		_, err = db.Exec("UPDATE dreams SET meaning_vector = $1 WHERE dream_id = $2", formatVector(vector), id)
		if err != nil {
			log.Printf("Failed to update DB for %d: %v", id, err)
		} else {
			fmt.Printf("Success [%d]\n", id)
		}

		time.Sleep(100 * time.Millisecond)
	}

	fmt.Println("All dreams re-embedded with richer context!")
}
