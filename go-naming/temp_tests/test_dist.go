//go:build tools

package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"math"
	"net/http"
	"os"
)

type OpenAIRequest struct {
	Input string `json:"input"`
	Model string `json:"model"`
}

type OpenAIResponse struct {
	Data []struct {
		Embedding []float64 `json:"embedding"`
	} `json:"data"`
}

func GetEmbedding(apiKey, text string) ([]float64, error) {
	url := "https://api.openai.com/v1/embeddings"
	reqBody := OpenAIRequest{
		Input: text,
		Model: "text-embedding-3-small",
	}
	jsonData, _ := json.Marshal(reqBody)
	req, _ := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+apiKey)
	resp, err := (&http.Client{}).Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	var res OpenAIResponse
	json.NewDecoder(resp.Body).Decode(&res)
	return res.Data[0].Embedding, nil
}

func cosineDistance(v1, v2 []float64) float64 {
	var dot, n1, n2 float64
	for i := range v1 {
		dot += v1[i] * v2[i]
		n1 += v1[i] * v1[i]
		n2 += v2[i] * v2[i]
	}
	sim := dot / (math.Sqrt(n1) * math.Sqrt(n2))
	return 1 - sim
}

func main() {
	apiKey := os.Getenv("OPENAI_API_KEY")
	e1, _ := GetEmbedding(apiKey, "หมวย")
	e2, _ := GetEmbedding(apiKey, "หมวย: ล้ำเลิศ")
	dist := cosineDistance(e1, e2)
	fmt.Printf("Distance between 'หมวย' and 'หมวย: ล้ำเลิศ': %f\n", dist)
	fmt.Printf("Percentage (100 - dist*100): %.1f%%\n", 100-dist*100)
}
