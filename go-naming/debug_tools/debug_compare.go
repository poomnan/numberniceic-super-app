package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"math"
	"net/http"
	"strconv"
	"strings"

	_ "github.com/lib/pq"
)

const (
	host     = "localhost"
	port     = 5432
	user     = "tayap"
	password = "IntelliP24.X"
	dbname   = "tayap"
)

func main() {
	// 1. Connect DB
	connStr := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable",
		host, port, user, password, dbname)
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("DB Error: %v", err)
	}
	defer db.Close()

	// 2. Get Data for ID 563
	var name string
	var vecStr string
	err = db.QueryRow("SELECT thname, cast(th_name_vector as text) FROM names_miracle WHERE name_id = 563").Scan(&name, &vecStr)
	if err != nil {
		log.Fatalf("Query ID 563 Error: %v", err)
	}

	fmt.Printf("=== ID 563 Data ===\n")
	fmt.Printf("Name: '%s' (Len: %d)\n", name, len(name))
	fmt.Printf("Name Hex: %x\n", name)

	if vecStr == "" {
		log.Fatal("Vector in DB is empty!")
	}

	// Parse DB Vector
	dbVector := parseVector(vecStr)
	fmt.Printf("DB Vector Len: %d\n", len(dbVector))

	// 3. Get Live Vector from Ollama
	fmt.Printf("\n=== Live Ollama 'กนก' ===\n")
	liveVector, err := getEmbedding("กนก") // Hardcode clean string
	if err != nil {
		log.Fatalf("Ollama Error: %v", err)
	}
	fmt.Printf("Live Vector Len: %d\n", len(liveVector))

	// 4. Calculate Cosine Distance
	dist := cosineDistance(dbVector, liveVector)
	fmt.Printf("\n=== Result ===\n")
	fmt.Printf("Cosine Distance: %f\n", dist)

	if dist < 0.0001 {
		fmt.Println(">> MATCH! Vectors are identical.")
	} else {
		fmt.Println(">> MISMATCH! Vectors are different.")
	}
}

func parseVector(s string) []float64 {
	s = strings.Trim(s, "[]")
	parts := strings.Split(s, ",")
	var v []float64
	for _, p := range parts {
		f, _ := strconv.ParseFloat(p, 64)
		v = append(v, f)
	}
	return v
}

func cosineDistance(a, b []float64) float64 {
	if len(a) != len(b) {
		return 1.0
	}
	var dot, normA, normB float64
	for i := range a {
		dot += a[i] * b[i]
		normA += a[i] * a[i]
		normB += b[i] * b[i]
	}
	if normA == 0 || normB == 0 {
		return 1.0
	}
	cosineSim := dot / (math.Sqrt(normA) * math.Sqrt(normB))
	return 1.0 - cosineSim
}

func getEmbedding(text string) ([]float64, error) {
	url := "http://localhost:11434/api/embeddings"
	reqBody := map[string]string{
		"model":  "nomic-embed-text",
		"prompt": text,
	}
	jsonData, _ := json.Marshal(reqBody)
	resp, err := http.Post(url, "application/json", strings.NewReader(string(jsonData)))
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	var res struct {
		Embedding []float64 `json:"embedding"`
	}
	json.NewDecoder(resp.Body).Decode(&res)
	return res.Embedding, nil
}
