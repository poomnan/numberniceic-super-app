package services

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"time"
)

type AnthropicMessage struct {
	Role    string `json:"role"`
	Content string `json:"content"`
}

type AnthropicRequest struct {
	Model     string             `json:"model"`
	MaxTokens int                `json:"max_tokens"`
	Messages  []AnthropicMessage `json:"messages"`
	System    string             `json:"system,omitempty"`
}

type AnthropicResponse struct {
	Content []struct {
		Text string `json:"text"`
		Type string `json:"type"`
	} `json:"content"`
	Error struct {
		Message string `json:"message"`
		Type    string `json:"type"`
	} `json:"error,omitempty"`
}

// CallClaude calls Anthropic API (Claude 3.5 Sonnet)
func CallClaude(prompt string, systemPrompt string) (string, error) {
	apiKey := os.Getenv("ANTHROPIC_API_KEY")
	if apiKey == "" {
		return "", fmt.Errorf("ANTHROPIC_API_KEY is not set")
	}

	url := "https://api.anthropic.com/v1/messages"
	reqBody := AnthropicRequest{
		Model:     "claude-3-5-sonnet-latest",
		MaxTokens: 1024,
		Messages: []AnthropicMessage{
			{Role: "user", Content: prompt},
		},
		System: systemPrompt,
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
	req.Header.Set("x-api-key", apiKey)
	req.Header.Set("anthropic-version", "2023-06-01")

	client := &http.Client{
		Timeout: 30 * time.Second,
	}
	resp, err := client.Do(req)
	if err != nil {
		return "", fmt.Errorf("error calling anthropic: %v", err)
	}
	defer resp.Body.Close()

	body, _ := io.ReadAll(resp.Body)
	if resp.StatusCode != 200 {
		return "", fmt.Errorf("status %d: %s", resp.StatusCode, string(body))
	}

	var res AnthropicResponse
	if err := json.Unmarshal(body, &res); err != nil {
		return "", fmt.Errorf("decode error: %v", err)
	}

	if len(res.Content) > 0 {
		return res.Content[0].Text, nil
	}

	if res.Error.Message != "" {
		return "", fmt.Errorf("api error: %s", res.Error.Message)
	}

	return "", fmt.Errorf("empty response from claude")
}
