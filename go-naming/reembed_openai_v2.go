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
	"strings"
	"time"

	_ "github.com/lib/pq"
)

const (
	DBConn = "user=tayap password=IntelliP24.X dbname=tayap sslmode=disable"
)

type OpenAIRequest struct {
	Input          string `json:"input"`
	Model          string `json:"model"`
	EncodingFormat string `json:"encoding_format,omitempty"`
}

type OpenAIResponse struct {
	Data []struct {
		Embedding []float64 `json:"embedding"`
	} `json:"data"`
	Error map[string]interface{} `json:"error,omitempty"`
}

var apiKey string

func init() {
	apiKey = os.Getenv("OPENAI_API_KEY")
	if apiKey == "" {
		log.Fatal("OPENAI_API_KEY is not set")
	}
	log.Printf("API Key loaded: %s...%s\n", apiKey[:10], apiKey[len(apiKey)-10:])
}

func GetEmbedding(text string) ([]float64, error) {
	url := "https://api.openai.com/v1/embeddings"
	reqBody := OpenAIRequest{
		Input:          text,
		Model:          "text-embedding-3-small",
		EncodingFormat: "float",
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		return nil, err
	}

	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, err
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+apiKey)

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("error calling openai: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("status %d: %s", resp.StatusCode, string(body))
	}

	var res OpenAIResponse
	if err := json.NewDecoder(resp.Body).Decode(&res); err != nil {
		return nil, fmt.Errorf("decode error: %v", err)
	}

	if len(res.Data) > 0 {
		return res.Data[0].Embedding, nil
	}

	if res.Error != nil {
		return nil, fmt.Errorf("api error: %v", res.Error)
	}

	return nil, fmt.Errorf("empty embedding returned")
}

func formatVector(embedding []float64) string {
	var strs []string
	for _, val := range embedding {
		strs = append(strs, fmt.Sprintf("%f", val))
	}
	return "[" + strings.Join(strs, ",") + "]"
}

func main() {
	db, err := sql.Open("postgres", DBConn)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	log.Println("Starting background re-embedding process...")
	log.Println("Testing database connection...")

	var count int
	err = db.QueryRow("SELECT COUNT(*) FROM names_miracle").Scan(&count)
	if err != nil {
		log.Fatalf("Failed to query database: %v", err)
	}
	log.Printf("Total names in database: %d\n", count)

	// Loop to process all records that need embedding
	batchSize := 100
	totalProcessed := 0
	startTime := time.Now()

	for {
		// Only select records that haven't been processed (meaning_vector IS NULL)
		// Assuming we will clear the vectors or alter the column first for the new dimension
		rows, err := db.Query("SELECT name_id, thname, COALESCE(meaning, '') FROM names_miracle WHERE meaning_vector IS NULL ORDER BY name_id ASC LIMIT $1", batchSize)
		if err != nil {
			log.Fatalf("Query error: %v", err)
		}

		rowCount := 0
		for rows.Next() {
			rowCount++
			var id int
			var name, meaning string
			if err := rows.Scan(&id, &name, &meaning); err != nil {
				log.Println("Scan error:", err)
				continue
			}

			log.Printf("Processing %d: %s\n", id, name)

			textToEmbed := name
			if meaning != "" {
				textToEmbed += ": " + meaning
			}

			// Retry logic
			var vector []float64
			for i := 0; i < 5; i++ { // Increased retries
				vector, err = GetEmbedding(textToEmbed)
				if err == nil {
					break
				}
				log.Printf("Retry %d for %d: %v\n", i+1, id, err)
				time.Sleep(time.Duration(i+1) * time.Second) // Exponential backoff
			}

			if err != nil {
				log.Printf("Failed to embed %d (%s): %v\n", id, name, err)
				continue
			}

			// log.Printf("Got vector of length %d for %d\n", len(vector), id)

			_, err = db.Exec("UPDATE names_miracle SET meaning_vector = $1 WHERE name_id = $2", formatVector(vector), id)
			if err != nil {
				log.Printf("Failed to update %d: %v\n", id, err)
				continue
			}

			totalProcessed++
			if totalProcessed%10 == 0 {
				rate := float64(totalProcessed) / time.Since(startTime).Seconds()
				log.Printf("Total Processed: %d. Rate: %.2f names/sec\n", totalProcessed, rate)
			}
		}
		rows.Close()

		if rowCount == 0 {
			log.Println("No more records to process.")
			break
		}

		// Small delay between batches to be nice to API/DB
		time.Sleep(100 * time.Millisecond)
	}

	log.Printf("Finished re-embedding. Total processed: %d\n", totalProcessed)
}
