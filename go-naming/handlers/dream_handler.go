package handlers

import (
	"database/sql"
	"encoding/json"
	"go-naming/database"
	"log"
	"net/http"
	"strings"
)

// Dream represents a dream interpretation record (matching database schema)
type Dream struct {
	DreamID             int    `json:"dream_id"`
	DreamKeyword        string `json:"dream_keyword"`
	DreamInterpretation string `json:"dream_interpretation"`
	LuckyNumbers        string `json:"lucky_numbers,omitempty"`
	Category            string `json:"category"`
}

// SearchDreamHandler searches for dream interpretations
func SearchDreamHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Get search query
	query := r.URL.Query().Get("q")
	if query == "" {
		http.Error(w, "Missing query parameter 'q'", http.StatusBadRequest)
		return
	}

	query = strings.TrimSpace(query)
	// Strip common prefixes to improve matching
	query = strings.TrimPrefix(query, "ฝันว่า")
	query = strings.TrimPrefix(query, "ฝันเห็น")
	query = strings.TrimSpace(query)

	// Search in database
	var dream Dream

	// Normalize for string matching
	lq := strings.ToLower(query)

	// Try exact match first
	matchType := "exact"
	err := database.DB.QueryRow(`
		SELECT dream_id, dream_keyword, dream_interpretation, COALESCE(lucky_numbers, ''), COALESCE(category, 'ความฝัน')
		FROM dreams
		WHERE is_active = true 
		  AND (
		      lower(dream_keyword) = $1
		      OR $1 = ANY(string_to_array(replace(lower(dream_keyword), ' ', ''), ','))
		  )
		ORDER BY length(dream_keyword) ASC
		LIMIT 1
	`, lq).Scan(
		&dream.DreamID,
		&dream.DreamKeyword,
		&dream.DreamInterpretation,
		&dream.LuckyNumbers,
		&dream.Category,
	)

	// If exact match not found, try keyword-based substring search
	if err == sql.ErrNoRows {
		matchType = "keyword"
		err = database.DB.QueryRow(`
			SELECT dream_id, dream_keyword, dream_interpretation, COALESCE(lucky_numbers, ''), COALESCE(category, 'ความฝัน')
			FROM dreams
			WHERE is_active = true 
			  AND (
			      position($1 in lower(dream_keyword)) > 0
			      OR position(lower(dream_keyword) in $1) > 0
			  )
			ORDER BY 
			  CASE 
			      WHEN position($1 in lower(dream_keyword)) > 0 THEN 1 
			      ELSE 2 
			  END ASC,
			  CASE 
			      WHEN position($1 in lower(dream_keyword)) > 0 THEN length(dream_keyword)
			      ELSE -length(dream_keyword) 
			  END ASC
			LIMIT 1
			`, lq).Scan(
			&dream.DreamID,
			&dream.DreamKeyword,
			&dream.DreamInterpretation,
			&dream.LuckyNumbers,
			&dream.Category,
		)
	}

	// If after all attempts we still haven't found a match
	if err == sql.ErrNoRows {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"found": false,
			"query": query,
		})
		return
	}

	if err != nil {
		log.Printf("Database error in SearchDreamHandler: %v", err)
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	// Update view count
	go func() {
		database.DB.Exec("UPDATE dreams SET view_count = view_count + 1 WHERE dream_id = $1", dream.DreamID)
	}()

	// Return result
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"found":      true,
		"dream":      dream,
		"match_type": matchType,
	})
}

// GetRandomDreamHandler returns a random dream for inspiration
func GetRandomDreamHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var dream Dream
	err := database.DB.QueryRow(`
		SELECT dream_id, dream_keyword, dream_interpretation, COALESCE(lucky_numbers, ''), COALESCE(category, 'ความฝัน')
		FROM dreams
		WHERE is_active = true
		ORDER BY RANDOM()
		LIMIT 1
	`).Scan(
		&dream.DreamID,
		&dream.DreamKeyword,
		&dream.DreamInterpretation,
		&dream.LuckyNumbers,
		&dream.Category,
	)

	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(dream)
}
