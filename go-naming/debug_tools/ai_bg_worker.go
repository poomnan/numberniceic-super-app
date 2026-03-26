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

const (
	ollamaURL   = "http://127.0.0.1:11434/api/generate"
	ollamaModel = "qwen2:7b" // โมเดลนี้เก่งภาษาไทยและทำงานเร็วบน CPU
	dbConnStr   = "postgres://tayap:IntelliP24.X@127.0.0.1/tayap?sslmode=disable"
)

type OllamaReq struct {
	Model  string `json:"model"`
	Prompt string `json:"prompt"`
	Stream bool   `json:"stream"`
}

type OllamaRes struct {
	Response string `json:"response"`
}

func getMeaningFromAI(name string) (string, error) {
	prompt := fmt.Sprintf("จงบอกความหมายมงคลของชื่อ '%s' สั้นๆ 1-3 คำ ตามหลักบาลี-สันสกฤต ตอบเฉพาะคำแปลภาษาไทยเท่านั้น", name)

	reqBody := OllamaReq{Model: ollamaModel, Prompt: prompt, Stream: false}
	jsonRaw, _ := json.Marshal(reqBody)

	resp, err := http.Post(ollamaURL, "application/json", bytes.NewBuffer(jsonRaw))
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	var res OllamaRes
	json.NewDecoder(resp.Body).Decode(&res)
	return strings.TrimSpace(res.Response), nil
}

func main() {
	db, err := sql.Open("postgres", dbConnStr)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	fmt.Println("🛰️  AI Worker started in background mode...")

	for {
		// ดึงชื่อมาทำทีละ 50 ชื่อ
		rows, _ := db.Query("SELECT name_id, thname FROM names_miracle WHERE meaning IS NULL OR meaning = '' LIMIT 50")

		hasData := false
		tx, _ := db.Begin()

		for rows.Next() {
			hasData = true
			var id int
			var name string
			rows.Scan(&id, &name)

			meaning, err := getMeaningFromAI(name)
			if err != nil {
				fmt.Printf("⚠️  Skipping %s: %v\n", name, err)
				continue
			}

			_, _ = tx.Exec("UPDATE names_miracle SET meaning = $1 WHERE name_id = $2", meaning, id)
			fmt.Printf("✅ %s -> %s\n", name, meaning)
		}
		rows.Close()

		if !hasData {
			fmt.Println("🎊 ALL DONE!")
			break
		}

		tx.Commit()
		time.Sleep(200 * time.Millisecond)
	}
}
