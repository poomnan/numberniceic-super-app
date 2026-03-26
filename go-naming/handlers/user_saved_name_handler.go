package handlers

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strconv"

	"go-naming/database"
	"go-naming/models"
)

// SaveUserNameHandler saves a name analysis for a user
func SaveUserNameHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var saved models.UserSavedName
	if err := json.NewDecoder(r.Body).Decode(&saved); err != nil {
		http.Error(w, "Invalid input", http.StatusBadRequest)
		return
	}

	query := `
		INSERT INTO user_saved_names (user_id, name, sat_sum, sha_sum, is_sat_good, is_sha_good, root_word, meaning, analysis, device_id)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
		RETURNING id, created_at
	`
	err := database.DB.QueryRow(
		query,
		saved.UserID, saved.Name, saved.SatSum, saved.ShaSum,
		saved.IsSatGood, saved.IsShaGood, saved.RootWord, saved.Meaning, saved.Analysis, saved.DeviceID,
	).Scan(&saved.ID, &saved.CreatedAt)

	if err != nil {
		http.Error(w, "Failed to save name", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(saved)
}

// ListUserSavedNamesHandler returns all saved names for a user or device
func ListUserSavedNamesHandler(w http.ResponseWriter, r *http.Request) {
	userIDStr := r.URL.Query().Get("user_id")
	deviceID := r.URL.Query().Get("device_id")

	if userIDStr == "" && deviceID == "" {
		http.Error(w, "Missing user_id or device_id", http.StatusBadRequest)
		return
	}

	userID := 0
	if userIDStr != "" {
		userID, _ = strconv.Atoi(userIDStr)
	}

	var rows *sql.Rows
	var err error

	if userID > 0 {
		rows, err = database.DB.Query("SELECT id, user_id, name, sat_sum, sha_sum, is_sat_good, is_sha_good, root_word, meaning, analysis, device_id, created_at FROM user_saved_names WHERE user_id = $1 ORDER BY created_at DESC", userID)
	} else {
		rows, err = database.DB.Query("SELECT id, user_id, name, sat_sum, sha_sum, is_sat_good, is_sha_good, root_word, meaning, analysis, device_id, created_at FROM user_saved_names WHERE device_id = $1 ORDER BY created_at DESC", deviceID)
	}

	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	savedNames := []models.UserSavedName{}
	for rows.Next() {
		var s models.UserSavedName
		err := rows.Scan(
			&s.ID, &s.UserID, &s.Name, &s.SatSum, &s.ShaSum,
			&s.IsSatGood, &s.IsShaGood, &s.RootWord, &s.Meaning, &s.Analysis, &s.DeviceID, &s.CreatedAt,
		)
		if err != nil {
			continue
		}
		savedNames = append(savedNames, s)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(savedNames)
}

// DeleteUserSavedNameHandler removes a saved name
func DeleteUserSavedNameHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var input struct {
		ID int `json:"id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&input); err != nil {
		http.Error(w, "Invalid input", http.StatusBadRequest)
		return
	}

	_, err := database.DB.Exec("DELETE FROM user_saved_names WHERE id = $1", input.ID)
	if err != nil {
		http.Error(w, "Failed to delete", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"status": "deleted"})
}
