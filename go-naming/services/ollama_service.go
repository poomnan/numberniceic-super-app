package services

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"
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
var embeddingCache = struct {
	sync.RWMutex
	items map[string]cachedEmbedding
}{
	items: map[string]cachedEmbedding{},
}

type cachedEmbedding struct {
	vector    []float64
	expiresAt time.Time
}

const embeddingCacheTTL = 10 * time.Minute

func init() {
	apiKey = os.Getenv("OPENAI_API_KEY")
}

// GetEmbedding now calls OpenAI API (text-embedding-3-small)
func GetEmbedding(text string) ([]float64, error) {
	cacheKey := strings.TrimSpace(strings.ToLower(text))
	if cacheKey != "" {
		embeddingCache.RLock()
		cached, ok := embeddingCache.items[cacheKey]
		embeddingCache.RUnlock()
		if ok && time.Now().Before(cached.expiresAt) {
			clone := append([]float64(nil), cached.vector...)
			return clone, nil
		}
	}

	if apiKey == "" {
		// Try to read it again in case it was set later
		apiKey = os.Getenv("OPENAI_API_KEY")
		if apiKey == "" {
			return nil, fmt.Errorf("OPENAI_API_KEY is not set")
		}
	}

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

	client := &http.Client{
		Timeout: 10 * time.Second,
	}
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
		vector := res.Data[0].Embedding
		if cacheKey != "" && len(vector) > 0 {
			embeddingCache.Lock()
			embeddingCache.items[cacheKey] = cachedEmbedding{
				vector:    append([]float64(nil), vector...),
				expiresAt: time.Now().Add(embeddingCacheTTL),
			}
			embeddingCache.Unlock()
		}
		return vector, nil
	}

	if res.Error != nil {
		return nil, fmt.Errorf("api error: %v", res.Error)
	}

	return nil, fmt.Errorf("empty embedding returned")
}

// GetOllamaEmbedding calls local Ollama for embeddings
func GetOllamaEmbedding(text string) ([]float64, error) {
	url := "http://localhost:11434/api/embeddings"
	reqBody := map[string]interface{}{
		"model":  "nomic-embed-text",
		"prompt": text,
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		return nil, err
	}

	client := &http.Client{
		Timeout: 10 * time.Second,
	}
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("error calling ollama: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("status %d: %s", resp.StatusCode, string(body))
	}

	var res struct {
		Embedding []float64 `json:"embedding"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&res); err != nil {
		return nil, fmt.Errorf("decode error: %v", err)
	}

	return res.Embedding, nil
}

// CallOpenAI calls OpenAI API (gpt-4o-mini) for chat
func CallOpenAI(prompt string, systemPrompt string) (string, error) {
	if apiKey == "" {
		apiKey = os.Getenv("OPENAI_API_KEY")
		if apiKey == "" {
			return "", fmt.Errorf("OPENAI_API_KEY is not set")
		}
	}

	url := "https://api.openai.com/v1/chat/completions"
	reqBody := map[string]interface{}{
		"model": "gpt-4o-mini",
		"messages": []map[string]string{
			{"role": "system", "content": systemPrompt},
			{"role": "user", "content": prompt},
		},
		"max_tokens": 1000,
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		return "", err
	}

	req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
	if err != nil {
		return "", err
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+apiKey)

	client := &http.Client{
		Timeout: 20 * time.Second, // Allow more time for chat completions
	}
	resp, err := client.Do(req)
	if err != nil {
		return "", fmt.Errorf("error calling openai chat: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		body, _ := io.ReadAll(resp.Body)
		return "", fmt.Errorf("status %d: %s", resp.StatusCode, string(body))
	}

	var res struct {
		Choices []struct {
			Message struct {
				Content string `json:"content"`
			} `json:"message"`
		} `json:"choices"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&res); err != nil {
		return "", fmt.Errorf("decode error: %v", err)
	}

	if len(res.Choices) > 0 {
		return res.Choices[0].Message.Content, nil
	}

	return "", fmt.Errorf("empty chat response from openai")
}
