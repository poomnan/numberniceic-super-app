package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"go-naming/database"
	"log"
	"net/http"
	"strconv"
)

// NumberMeaningResponse represents the structure of the number meaning response
type NumberMeaningResponse struct {
	Number      string `json:"number"`
	Description string `json:"description"`
	Detail      string `json:"detail"`
	PairType    string `json:"pair_type"`
}

// GetNumberMeaningHandler retrieves the meaning of a number from the 'numbers' table
func GetNumberMeaningHandler(w http.ResponseWriter, r *http.Request) {
	// Enable CORS
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type")

	if r.Method == http.MethodOptions {
		return
	}

	numberStr := r.URL.Query().Get("number")
	if numberStr == "" {
		http.Error(w, "Number is required", http.StatusBadRequest)
		return
	}

	// Validate that input is a number
	// Validate that input is a number
	_, err := strconv.Atoi(numberStr)
	if err != nil {
		http.Error(w, "Invalid number format", http.StatusBadRequest)
		return
	}

	// Query database
	// Try strict match first
	// Query database
	// Try strict match first
	var description sql.NullString
	var detail sql.NullString
	var pairType sql.NullString

	query := "SELECT miracledesc, miracledetail, pairtype FROM numbers WHERE pairnumber = $1"
	err = database.DB.QueryRow(query, numberStr).Scan(&description, &detail, &pairType)

	if err == sql.ErrNoRows {
		// Try zero-padding if input is like "5" -> "05"
		// Only if needed (integers < 10)
		if len(numberStr) == 1 {
			padded := fmt.Sprintf("0%s", numberStr)
			err = database.DB.QueryRow(query, padded).Scan(&description, &detail, &pairType)
		}
	}

	if err != nil {
		log.Printf("Database Error querying number meaning: %v", err)
		if err == sql.ErrNoRows {
			// If not found in DB
			json.NewEncoder(w).Encode(NumberMeaningResponse{
				Number:      numberStr,
				Description: "ไม่พบข้อมูลคำทำนาย",
				Detail:      "ไม่มีรายละเอียดสำหรับเลขนี้ในฐานข้อมูล",
			})
			return
		}
		// Return JSON error
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{
			"error": fmt.Sprintf("Database error: %v", err),
		})
		return
	}

	response := NumberMeaningResponse{
		Number:      numberStr,
		Description: description.String,
		Detail:      detail.String,
		PairType:    pairType.String,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}
