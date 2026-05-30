package handlers

import (
	"go-naming/database"
	"go-naming/services"
	"net/http"
	"strings"
)

func InputClassifyHandler(w http.ResponseWriter, r *http.Request) {
	setMobileCORSHeaders(w)

	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}

	if r.Method != http.MethodGet {
		jsonResponse(w, http.StatusMethodNotAllowed, map[string]any{"error": map[string]any{"message": "Method not allowed"}})
		return
	}

	input := strings.TrimSpace(r.URL.Query().Get("input"))
	if input == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]any{"error": map[string]any{"message": "missing input"}})
		return
	}

	result := services.ClassifyInput(input, database.DB)
	jsonResponse(w, http.StatusOK, result)
}
