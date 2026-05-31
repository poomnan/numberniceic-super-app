package handlers

import (
	"database/sql"
	"go-naming/database"
	"net/http"
	"strings"
)

func GetNameMeaningHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	name := strings.TrimSpace(r.URL.Query().Get("name"))
	if name == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "missing name"})
		return
	}

	var meaning sql.NullString
	err := database.DB.QueryRow(
		`SELECT meaning FROM names_miracle WHERE thname = $1 LIMIT 1`,
		name,
	).Scan(&meaning)
	if err == sql.ErrNoRows {
		jsonResponse(w, http.StatusOK, map[string]string{"name": name, "meaning": ""})
		return
	}
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "database error"})
		return
	}

	if !meaning.Valid {
		jsonResponse(w, http.StatusOK, map[string]string{"name": name, "meaning": ""})
		return
	}
	jsonResponse(w, http.StatusOK, map[string]string{"name": name, "meaning": meaning.String})
}
