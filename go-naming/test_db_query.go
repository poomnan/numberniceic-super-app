//go:build tools

package main

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"

	_ "github.com/lib/pq"
)

func GetEmbedding(text string) ([]float64, error) {
	apiKey := os.Getenv("OPENAI_API_KEY")
	url := "https://api.openai.com/v1/embeddings"
	reqBody := map[string]interface{}{
		"input": text,
		"model": "text-embedding-3-small",
	}
	jsonData, _ := json.Marshal(reqBody)
	req, _ := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+apiKey)
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)
	var res struct {
		Data []struct{ Embedding []float64 }
	}
	json.Unmarshal(body, &res)
	if len(res.Data) > 0 {
		return res.Data[0].Embedding, nil
	}
	return nil, fmt.Errorf("failed")
}

func main() {
	connStr := "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable"
	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	emb, _ := GetEmbedding("สายลมและแสงแดด")
	vectorStr := "["
	for i, v := range emb {
		vectorStr += fmt.Sprintf("%f", v)
		if i < len(emb)-1 {
			vectorStr += ","
		}
	}
	vectorStr += "]"

	query := `
		SELECT name_id, thname, (meaning_vector <=> $1) as distance
		FROM names_miracle
		WHERE meaning_vector IS NOT NULL
		  AND k_monday = false
		ORDER BY distance ASC
		LIMIT 5
	`

	rows, err := db.Query(query, vectorStr)
	if err != nil {
		log.Fatal(err)
	}
	defer rows.Close()

	fmt.Println("Query Results for 'สายลมและแสงแดด' on Monday:")
	for rows.Next() {
		var id int
		var name string
		var dist float64
		rows.Scan(&id, &name, &dist)
		fmt.Printf("ID: %d, Name: %s, Distance: %f\n", id, name, dist)
	}
}
