package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
)

func main() {
	url := "http://localhost:8095/api/v1/name-search"
	body := map[string]interface{}{
		"keyword":      "เศรษฐีผู้มั่งคั่ง มีทรัพย์สมบัติและบารมี",
		"gender":       "",
		"filter_sat":   false,
		"filter_sha":   false,
		"similar_mode": false,
		"limit":        5,
	}

	jsonData, _ := json.Marshal(body)
	resp, err := http.Post(url, "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		fmt.Printf("HTTP Error: %v\n", err)
		return
	}
	defer resp.Body.Close()

	fmt.Printf("Status: %s\n", resp.Status)
	resBody, _ := io.ReadAll(resp.Body)
	fmt.Printf("Response: %s\n", string(resBody))
}
