package handlers

import (
	"encoding/json"
	"fmt"
	"go-naming/database"
	"go-naming/models"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"
)

// ListNamingExamplesHandler returns all naming examples
func ListNamingExamplesHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	rows, err := database.DB.Query("SELECT id, name, avatar_url, is_celebrity, sort_order, created_at FROM naming_examples ORDER BY sort_order ASC, name ASC")
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	defer rows.Close()

	var examples []models.NamingExample
	for rows.Next() {
		var ex models.NamingExample
		if err := rows.Scan(&ex.ID, &ex.Name, &ex.AvatarURL, &ex.IsCelebrity, &ex.SortOrder, &ex.CreatedAt); err != nil {
			log.Printf("Error scanning naming example: %v", err)
			continue
		}

		if strings.HasPrefix(ex.AvatarURL, "/uploads/") {
			ex.AvatarURL = strings.Replace(ex.AvatarURL, "/uploads/", "/api/uploads/", 1)
		}

		examples = append(examples, ex)
	}

	if examples == nil {
		examples = []models.NamingExample{}
	}

	jsonResponse(w, http.StatusOK, examples)
}

// AddNamingExampleHandler adds a new naming example
func AddNamingExampleHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var ex models.NamingExample
	if err := json.NewDecoder(r.Body).Decode(&ex); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid JSON"})
		return
	}

	if ex.Name == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Name is required"})
		return
	}

	err := database.DB.QueryRow(
		"INSERT INTO naming_examples (name, avatar_url, is_celebrity, sort_order) VALUES ($1, $2, $3, $4) RETURNING id",
		ex.Name, ex.AvatarURL, ex.IsCelebrity, ex.SortOrder,
	).Scan(&ex.ID)

	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, ex)
}

// UpdateNamingExampleHandler updates an existing naming example
func UpdateNamingExampleHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var ex models.NamingExample
	if err := json.NewDecoder(r.Body).Decode(&ex); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid JSON"})
		return
	}

	if ex.ID == 0 {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "ID is required"})
		return
	}

	_, err := database.DB.Exec(
		"UPDATE naming_examples SET name=$1, avatar_url=$2, is_celebrity=$3, sort_order=$4 WHERE id=$5",
		ex.Name, ex.AvatarURL, ex.IsCelebrity, ex.SortOrder, ex.ID,
	)

	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

// DeleteNamingExampleHandler deletes a naming example
func DeleteNamingExampleHandler(w http.ResponseWriter, r *http.Request) {
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

	_, err := database.DB.Exec("DELETE FROM naming_examples WHERE id=$1", req.ID)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

// UploadNamingAvatarHandler handles avatar image uploads
func UploadNamingAvatarHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// 1. Limit size (5MB)
	r.ParseMultipartForm(5 << 20)

	file, handler, err := r.FormFile("file")
	if err != nil {
		http.Error(w, "Error retrieving file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// 2. Create target directory
	uploadDir := "./uploads/avatars"
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		os.MkdirAll(uploadDir, 0755)
	}

	// 3. Generate unique name
	ext := filepath.Ext(handler.Filename)
	fileName := fmt.Sprintf("avatar_%d%s", time.Now().UnixNano(), ext)
	filePath := filepath.Join(uploadDir, fileName)

	// 4. Save file
	dst, err := os.Create(filePath)
	if err != nil {
		http.Error(w, "Error saving file", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	if _, err := io.Copy(dst, file); err != nil {
		http.Error(w, "Error saving file content", http.StatusInternalServerError)
		return
	}

	// 5. Return relative URL
	jsonResponse(w, http.StatusOK, map[string]string{
		"url": "/uploads/avatars/" + fileName,
	})
}
