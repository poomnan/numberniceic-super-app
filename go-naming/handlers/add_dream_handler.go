package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"go-naming/database"
	"net/http"
	"strings"
)

// AddDreamRequest represents the request to add a new dream
type AddDreamRequest struct {
	DreamKeyword        string `json:"dream_keyword"`
	DreamInterpretation string `json:"dream_interpretation"`
	LuckyNumbers        string `json:"lucky_numbers"`
	Category            string `json:"category"`
}

// AddDreamHandler handles adding a new dream to the database (Simple Version)
func AddDreamHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req AddDreamRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.DreamKeyword == "" || req.DreamInterpretation == "" {
		http.Error(w, "dream_keyword and dream_interpretation are required", http.StatusBadRequest)
		return
	}

	// Insert into database
	query := `
		INSERT INTO dreams (
			dream_keyword, dream_interpretation, lucky_numbers, category, is_active
		) VALUES ($1, $2, $3, $4, true)
		RETURNING dream_id
	`
	var dreamID int
	err := database.DB.QueryRow(
		query,
		req.DreamKeyword,
		req.DreamInterpretation,
		req.LuckyNumbers,
		req.Category,
	).Scan(&dreamID)

	if err != nil {
		if strings.Contains(err.Error(), "duplicate key") || strings.Contains(err.Error(), "unique constraint") {
			http.Error(w, "ชื่อฝันนี้มีอยู่ในระบบแล้ว (Duplicate Keyword)", http.StatusConflict)
			return
		}
		http.Error(w, fmt.Sprintf("Error inserting dream: %v", err), http.StatusInternalServerError)
		return
	}

	// Return success response
	response := map[string]interface{}{
		"success":  true,
		"dream_id": dreamID,
		"message":  "บันทึกความฝันสำเร็จ",
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetAllDreamsHandler returns all dreams
func GetAllDreamsHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	query := `
		SELECT 
			dream_id, dream_keyword, dream_interpretation, lucky_numbers,
			view_count, is_active, created_at,
			COALESCE(category, 'ความฝัน')
		FROM dreams
		ORDER BY created_at DESC
	`

	rows, err := database.DB.Query(query)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error fetching dreams: %v", err), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var dreams []map[string]interface{}
	for rows.Next() {
		var (
			dreamID             int
			dreamKeyword        string
			dreamInterpretation string
			luckyNumbers        sql.NullString
			viewCount           int
			isActive            bool
			createdAt           string
			category            string
		)

		err := rows.Scan(
			&dreamID, &dreamKeyword, &dreamInterpretation, &luckyNumbers,
			&viewCount, &isActive, &createdAt, &category,
		)
		if err != nil {
			continue
		}

		dream := map[string]interface{}{
			"dream_id":             dreamID,
			"dream_keyword":        dreamKeyword,
			"dream_interpretation": dreamInterpretation,
			"lucky_numbers":        luckyNumbers.String,
			"view_count":           viewCount,
			"is_active":            isActive,
			"created_at":           createdAt,
			"category":             category,
		}

		dreams = append(dreams, dream)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(dreams)
}

// UpdateDreamRequest represents the request to update an existing dream
type UpdateDreamRequest struct {
	DreamID             int    `json:"dream_id"`
	DreamKeyword        string `json:"dream_keyword"`
	DreamInterpretation string `json:"dream_interpretation"`
	LuckyNumbers        string `json:"lucky_numbers"`
	Category            string `json:"category"`
}

// UpdateDreamHandler handles updating an existing dream
func UpdateDreamHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req UpdateDreamRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.DreamID == 0 || req.DreamKeyword == "" || req.DreamInterpretation == "" {
		http.Error(w, "dream_id, dream_keyword and dream_interpretation are required", http.StatusBadRequest)
		return
	}

	// Update database
	query := `
		UPDATE dreams 
		SET dream_keyword = $1, dream_interpretation = $2, lucky_numbers = $3, updated_at = CURRENT_TIMESTAMP, category = $4
		WHERE dream_id = $5
	`
	result, err := database.DB.Exec(query, req.DreamKeyword, req.DreamInterpretation, req.LuckyNumbers, req.Category, req.DreamID)

	if err != nil {
		http.Error(w, fmt.Sprintf("Error updating dream: %v", err), http.StatusInternalServerError)
		return
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected == 0 {
		http.Error(w, "Dream not found", http.StatusNotFound)
		return
	}

	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"success": true,
		"message": "อัปเดตข้อมูลฝันสำเร็จ",
	})
}

// DeleteDreamHandler handles deleting a dream
func DeleteDreamHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		DreamID int `json:"dream_id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	_, err := database.DB.Exec("DELETE FROM dreams WHERE dream_id = $1", req.DreamID)
	if err != nil {
		http.Error(w, fmt.Sprintf("Error deleting dream: %v", err), http.StatusInternalServerError)
		return
	}

	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"success": true,
		"message": "ลบข้อมูลฝันสำเร็จ",
	})
}
