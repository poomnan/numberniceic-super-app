package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"

	_ "github.com/lib/pq"
)

// Config Database (Hardcoded for debugging)
const (
	host     = "localhost"
	port     = 5432
	user     = "tayap"
	password = "IntelliP24.X"
	dbname   = "tayap"
)

type OllamaRequest struct {
	Model  string `json:"model"`
	Prompt string `json:"prompt"`
}

type OllamaResponse struct {
	Embedding []float64 `json:"embedding"`
}

func main() {
	// 1. Get Embedding from Ollama
	fmt.Println("1. Getting embedding for 'แมว'...")
	embedding, err := getEmbedding("แมว")
	if err != nil {
		log.Fatalf("Ollama Error: %v", err)
	}
	fmt.Printf("   Got embedding with %d dimensions.\n", len(embedding))

	// 2. Connect DB
	connStr := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable",
		host, port, user, password, dbname)
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("DB Connect Error: %v", err)
	}
	defer db.Close()

	// 3. Query
	fmt.Println("2. Querying database...")
	query := `
		SELECT name_id, thname, (th_name_vector <=> $1) as distance
		FROM names_miracle
		ORDER BY distance ASC
		LIMIT 5;
	`
	rows, err := db.Query(query, formatVector(embedding))
	if err != nil {
		log.Fatalf("Query Error: %v", err)
	}
	defer rows.Close()

	fmt.Println("3. Top 5 Results:")
	for rows.Next() {
		var id int
		var name string
		var dist float64
		if err := rows.Scan(&id, &name, &dist); err != nil {
			log.Printf("Scan Error: %v", err)
			continue
		}
		fmt.Printf("   - [%d] %s (Distance: %f)\n", id, name, dist)
	}
}

func getEmbedding(text string) ([]float64, error) {
	url := "http://localhost:11434/api/embeddings"
	reqBody := OllamaRequest{Model: "nomic-embed-text", Prompt: text}

	jsonData, _ := json.Marshal(reqBody)
	resp, err := http.Post(url, "application/json", strings.NewReader(string(jsonData)))
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	var ollamaResp OllamaResponse
	if err := json.NewDecoder(resp.Body).Decode(&ollamaResp); err != nil {
		return nil, err
	}
	return ollamaResp.Embedding, nil
}

func formatVector(v []float64) string {
	strs := make([]string, len(v))
	for i, val := range v {
		strs[i] = fmt.Sprintf("%f", val)
	}
	return "[" + strings.Join(strs, ",") + "]"
}
