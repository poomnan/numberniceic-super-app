package handlers

import (
	"go-naming/models"
	"go-naming/services"
	"net/http"
	"strconv"
)

// RecommendSpellingHandler handles the request to find better spellings for a given name.
func RecommendSpellingHandler(w http.ResponseWriter, r *http.Request) {
	name := r.URL.Query().Get("name")
	if name == "" {
		http.Error(w, "Query parameter 'name' is required", http.StatusBadRequest)
		return
	}

	day := r.URL.Query().Get("day")

	limitParam := r.URL.Query().Get("limit")
	limit := 10
	if l, err := strconv.Atoi(limitParam); err == nil && l > 0 {
		limit = l
	}

	results, err := services.RecommendSpelling(name, day, limit)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	if results == nil {
		results = []models.SpellingRecommendation{}
	}

	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"original_name": name,
		"phonetic_code": services.GetThaiPhoneticCode(name),
		"matches":       results,
		"count":         len(results),
	})
}
