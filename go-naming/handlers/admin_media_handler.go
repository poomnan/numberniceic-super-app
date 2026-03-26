package handlers

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"
)

type MediaFile struct {
	Name string    `json:"name"`
	URL  string    `json:"url"`
	Size int64     `json:"size"`
	Time time.Time `json:"time"`
}

func ListMediaHandler(w http.ResponseWriter, r *http.Request) {
	// Use absolute path for reliability
	uploadDir := "/home/tayap/go-naming/uploads"

	files, err := os.ReadDir(uploadDir)
	if err != nil {
		log.Printf("Error reading uploads dir %s: %v", uploadDir, err)
		http.Error(w, "Failed to read uploads directory", http.StatusInternalServerError)
		return
	}

	var mediaList []MediaFile
	for _, f := range files {
		if !f.IsDir() {
			info, err := f.Info()
			if err != nil {
				continue
			}
			// Only list common image extensions
			ext := strings.ToLower(filepath.Ext(f.Name()))
			if ext == ".jpg" || ext == ".jpeg" || ext == ".png" || ext == ".gif" || ext == ".webp" {
				mediaList = append(mediaList, MediaFile{
					Name: f.Name(),
					URL:  "/uploads/" + f.Name(),
					Size: info.Size(),
					Time: info.ModTime(),
				})
			}
		}
	}

	// Sort by newest first
	sort.Slice(mediaList, func(i, j int) bool {
		return mediaList[i].Time.After(mediaList[j].Time)
	})

	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "no-store, no-cache, must-revalidate")
	json.NewEncoder(w).Encode(mediaList)
}

func UploadMediaHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Limit size (10MB)
	if err := r.ParseMultipartForm(10 << 20); err != nil {
		http.Error(w, "File too large", http.StatusBadRequest)
		return
	}

	file, handler, err := r.FormFile("file")
	if err != nil {
		http.Error(w, "Error retrieving file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	uploadDir := "/home/tayap/go-naming/uploads"
	// Ensure directory exists
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		os.Mkdir(uploadDir, 0755)
	}

	// Generate unique name or keep original with timestamp to avoid collision
	ext := filepath.Ext(handler.Filename)
	originalName := strings.TrimSuffix(handler.Filename, ext)
	// Sanitize name: remove spaces and special chars
	sanitized := ""
	for _, r := range originalName {
		if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') || r == '-' || r == '_' {
			sanitized += string(r)
		}
	}
	if sanitized == "" {
		sanitized = "upload"
	}

	fileName := fmt.Sprintf("%s_%d%s", sanitized, time.Now().Unix(), ext)
	filePath := filepath.Join(uploadDir, fileName)

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

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"url": "/uploads/" + fileName,
	})
}

func DeleteMediaHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var body struct {
		Name string `json:"name"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || body.Name == "" {
		http.Error(w, "Internal name required", http.StatusBadRequest)
		return
	}

	// Prevent directory traversal
	fileName := filepath.Base(body.Name)
	uploadDir := "/home/tayap/go-naming/uploads"
	filePath := filepath.Join(uploadDir, fileName)

	log.Printf("Deleting media file: %q", filePath)

	// Check if file exists first
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		log.Printf("File already missing (skip): %q", filePath)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "success", "info": "file already missing"})
		return
	}

	if err := os.Remove(filePath); err != nil {
		log.Printf("Error removing file %q: %v", filePath, err)
		http.Error(w, "Failed to delete file: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"status": "success"})
}
