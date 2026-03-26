package main

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"
	"time"

	_ "github.com/lib/pq"
)

// ตั้งค่าโมเดลที่ใช้แปลความหมาย
const (
	ollamaModel = "llama3" // หรือเปลี่ยนเป็น mistral, gemma ตามที่มีในเครื่อง
	serverConn  = "postgres://tayap:IntelliP24.X@43.228.85.200:5432/tayap?sslmode=disable"
)

type OllamaChatRequest struct {
	Model  string `json:"model"`
	Prompt string `json:"prompt"`
	Stream bool   `json:"stream"`
}

type OllamaChatResponse struct {
	Response string `json:"response"`
}

func askOllama(name string) (string, error) {
	url := "http://localhost:11434/api/generate"

	prompt := fmt.Sprintf("จงบอกความหมายมงคลสั้นๆ ของชื่อคนไทยว่า '%s' โดยอิงตามรากศัพท์บาลี-สันสกฤต ตอบเฉพาะคำแปลภาษาไทยสั้นๆ ไม่ต้องเกริ่น", name)

	reqBody := OllamaChatRequest{
		Model:  ollamaModel,
		Prompt: prompt,
		Stream: false,
	}

	jsonData, _ := json.Marshal(reqBody)
	resp, err := http.Post(url, "application/json", bytes.NewBuffer(jsonData))
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	var result OllamaChatResponse
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return "", err
	}

	return strings.TrimSpace(result.Response), nil
}

func main() {
	db, err := sql.Open("postgres", serverConn)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	fmt.Println("🤖 AI Meaning Generator identifies and translates all remaining names...")

	for {
		// 1. ดึงชื่อที่ยังว่างอยู่มาทีละ Batch (100 ชื่อ)
		rows, err := db.Query("SELECT name_id, thname FROM names_miracle WHERE (meaning IS NULL OR meaning = '') LIMIT 100")
		if err != nil {
			log.Fatal(err)
		}

		hasData := false
		tx, _ := db.Begin()

		for rows.Next() {
			hasData = true
			var id int
			var name string
			rows.Scan(&id, &name)

			// 2. ถาม AI ว่าแปลว่าอะไร
			meaning, err := askOllama(name)
			if err != nil {
				fmt.Printf("Error skipping %s: %v\n", name, err)
				continue
			}

			// 3. บันทึกผล
			_, err = tx.Exec("UPDATE names_miracle SET meaning = $1 WHERE name_id = $2", meaning, id)
			if err == nil {
				fmt.Printf("✨ Translated [%s]: %s\n", name, meaning)
			}
		}
		rows.Close()

		if !hasData {
			break // จบงานเมื่อไม่มีชื่อว่างแล้ว
		}

		tx.Commit()
		time.Sleep(100 * time.Millisecond) // พักเครื่องเล็กน้อย
	}

	fmt.Println("🎉 ALL 300,000 NAMES HAVE BEEN PROCESSED BY AI!")
}
