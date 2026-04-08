package handlers

import (
	"encoding/json"
	"go-naming/database"
	"go-naming/services"
	"net/http"
	"strings"
)

type nameIntentRequest struct {
	Input string `json:"input"`
}

type nameIntentResponse struct {
	Mode       string   `json:"mode"`
	Confidence float64  `json:"confidence"`
	Candidates []string `json:"candidates"`
	BestScore  float64  `json:"best_score"`
}

func NameIntentHandler(w http.ResponseWriter, r *http.Request) {
	setMobileCORSHeaders(w)

	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}

	var input string

	switch r.Method {
	case http.MethodGet:
		input = strings.TrimSpace(r.URL.Query().Get("input"))
	case http.MethodPost:
		var req nameIntentRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			jsonResponse(w, http.StatusBadRequest, map[string]any{"error": map[string]any{"message": "Invalid JSON body"}})
			return
		}
		input = strings.TrimSpace(req.Input)
	default:
		jsonResponse(w, http.StatusMethodNotAllowed, map[string]any{"error": map[string]any{"message": "Method not allowed"}})
		return
	}

	result, err := services.DetectNameIntent(r.Context(), database.DB, input)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]any{"error": map[string]any{"message": "Failed to detect name intent"}})
		return
	}

	jsonResponse(w, http.StatusOK, nameIntentResponse{
		Mode:       result.Mode,
		Confidence: result.Confidence,
		Candidates: result.Candidates,
		BestScore:  result.BestScore,
	})
}
