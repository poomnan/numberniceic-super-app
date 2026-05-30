package handlers

import (
	"encoding/json"
	"go-naming/database"
	"go-naming/models"
	"log"
	"net/http"
	"strings"
)

// ListSemanticSearchIdeasHandler returns active examples for Flutter.
func ListSemanticSearchIdeasHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	listSemanticSearchIdeas(w, false)
}

// AdminListSemanticSearchIdeasHandler returns every managed example.
func AdminListSemanticSearchIdeasHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	listSemanticSearchIdeas(w, true)
}

func listSemanticSearchIdeas(w http.ResponseWriter, includeInactive bool) {
	query := `SELECT id, text, icon_name, icon_color, sort_order, is_active, created_at, updated_at
		FROM semantic_search_ideas`
	if !includeInactive {
		query += " WHERE is_active = true"
	}
	query += " ORDER BY sort_order ASC, id ASC"

	rows, err := database.DB.Query(query)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	defer rows.Close()

	items := []models.SemanticSearchIdea{}
	for rows.Next() {
		var item models.SemanticSearchIdea
		if err := rows.Scan(
			&item.ID,
			&item.Text,
			&item.IconName,
			&item.IconColor,
			&item.SortOrder,
			&item.IsActive,
			&item.CreatedAt,
			&item.UpdatedAt,
		); err != nil {
			log.Printf("Error scanning semantic search idea: %v", err)
			continue
		}
		items = append(items, item)
	}

	jsonResponse(w, http.StatusOK, items)
}

// AddSemanticSearchIdeaHandler creates a managed semantic-search prompt.
func AddSemanticSearchIdeaHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var item models.SemanticSearchIdea
	if err := json.NewDecoder(r.Body).Decode(&item); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid JSON"})
		return
	}
	normalizeSemanticSearchIdea(&item)
	if item.Text == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Text is required"})
		return
	}

	err := database.DB.QueryRow(
		`INSERT INTO semantic_search_ideas (text, icon_name, icon_color, sort_order, is_active, updated_at)
		 VALUES ($1, $2, $3, $4, $5, CURRENT_TIMESTAMP)
		 RETURNING id, created_at, updated_at`,
		item.Text, item.IconName, item.IconColor, item.SortOrder, item.IsActive,
	).Scan(&item.ID, &item.CreatedAt, &item.UpdatedAt)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, item)
}

// UpdateSemanticSearchIdeaHandler updates a managed semantic-search prompt.
func UpdateSemanticSearchIdeaHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var item models.SemanticSearchIdea
	if err := json.NewDecoder(r.Body).Decode(&item); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid JSON"})
		return
	}
	normalizeSemanticSearchIdea(&item)
	if item.ID == 0 {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "ID is required"})
		return
	}
	if item.Text == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Text is required"})
		return
	}

	result, err := database.DB.Exec(
		`UPDATE semantic_search_ideas
		 SET text=$1, icon_name=$2, icon_color=$3, sort_order=$4, is_active=$5, updated_at=CURRENT_TIMESTAMP
		 WHERE id=$6`,
		item.Text, item.IconName, item.IconColor, item.SortOrder, item.IsActive, item.ID,
	)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	if affected, _ := result.RowsAffected(); affected == 0 {
		jsonResponse(w, http.StatusNotFound, map[string]string{"error": "Item not found"})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

// DeleteSemanticSearchIdeaHandler deletes a managed semantic-search prompt.
func DeleteSemanticSearchIdeaHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		ID int `json:"id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid JSON"})
		return
	}
	if req.ID == 0 {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "ID is required"})
		return
	}

	_, err := database.DB.Exec("DELETE FROM semantic_search_ideas WHERE id=$1", req.ID)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

func normalizeSemanticSearchIdea(item *models.SemanticSearchIdea) {
	item.Text = strings.TrimSpace(item.Text)
	item.IconName = strings.TrimSpace(item.IconName)
	item.IconColor = strings.TrimSpace(item.IconColor)
	if item.IconName == "" {
		item.IconName = "sparkles"
	}
	if item.IconColor == "" {
		item.IconColor = "#E2B237"
	}
	if !strings.HasPrefix(item.IconColor, "#") {
		item.IconColor = "#" + item.IconColor
	}
}
