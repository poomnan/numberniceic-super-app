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

	log.Println("Starting dreams embedding process...")

	var total, nullCount int
	db.QueryRow("SELECT COUNT(*) FROM dreams").Scan(&total)
	db.QueryRow("SELECT COUNT(*) FROM dreams WHERE meaning_vector IS NULL").Scan(&nullCount)
	log.Printf("Total dreams: %d, Need embedding: %d\n", total, nullCount)

	batchSize := 50
	totalProcessed := 0
	startTime := time.Now()

	for {
		rows, err := db.Query(
			`SELECT dream_id, dream_keyword, dream_interpretation 
			 FROM dreams 
			 WHERE meaning_vector IS NULL 
			 ORDER BY dream_id ASC 
			 LIMIT $1`, batchSize)
		if err != nil {
			log.Fatalf("Query error: %v", err)
		}

		rowCount := 0
		for rows.Next() {
			rowCount++
			var id int
			var keyword, interpretation string
			if err := rows.Scan(&id, &keyword, &interpretation); err != nil {
				log.Println("Scan error:", err)
				continue
			}

			// Concatenate dream_keyword + dream_interpretation
			textToEmbed := keyword + ": " + interpretation
			log.Printf("Processing %d: %s (len=%d)\n", id, keyword, len(textToEmbed))

			// Retry logic with exponential backoff
			var vector []float64
			for i := 0; i < 5; i++ {
				vector, err = GetEmbedding(textToEmbed)
				if err == nil {
					break
				}
				log.Printf("Retry %d for %d: %v\n", i+1, id, err)
				time.Sleep(time.Duration(i+1) * time.Second)
			}

			if err != nil {
				log.Printf("Failed to embed %d (%s): %v\n", id, keyword, err)
				continue
			}

			_, err = db.Exec("UPDATE dreams SET meaning_vector = $1 WHERE dream_id = $2", formatVector(vector), id)
			if err != nil {
				log.Printf("Failed to update %d: %v\n", id, err)
				continue
			}

			totalProcessed++
			if totalProcessed%10 == 0 {
				rate := float64(totalProcessed) / time.Since(startTime).Seconds()
				log.Printf("Progress: %d/%d (%.1f%%). Rate: %.2f dreams/sec\n",
					totalProcessed, nullCount, float64(totalProcessed)/float64(nullCount)*100, rate)
			}
		}
		rows.Close()

		if rowCount == 0 {
			log.Println("No more records to process.")
			break
		}

		time.Sleep(100 * time.Millisecond)
	}

	elapsed := time.Since(startTime)
	log.Printf("Finished! Total processed: %d in %s\n", totalProcessed, elapsed.Round(time.Second))
}
