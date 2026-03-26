package astro

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"time"
)

func FooDaysHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Extract year from URL path
	yearStr := r.URL.Path[len("/api/foo-days/"):]
	year, err := strconv.Atoi(yearStr)
	if err != nil {
		http.Error(w, "Invalid year format", http.StatusBadRequest)
		return
	}

	// Validate year range (current year to +2 years)
	currentYear := time.Now().Year()
	if year < currentYear || year > currentYear+2 {
		http.Error(w, "Year must be between current year and 2 years ahead", http.StatusBadRequest)
		return
	}

	// Get Foo days for the requested year
	fooDays := GetFooDaysForYear(year)

	response := map[string]interface{}{
		"year":     year,
		"foo_days": fooDays,
		"count":    len(fooDays),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func FooDaysCurrentHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Redirect to current year
	currentYear := time.Now().Year()
	http.Redirect(w, r, fmt.Sprintf("/api/foo-days/%d", currentYear), http.StatusFound)
}

func HealthHandler(w http.ResponseWriter, r *http.Request) {
	response := map[string]interface{}{
		"status":    "healthy",
		"timestamp": time.Now().Format(time.RFC3339),
		"service":   "Astro Calendar API",
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// KalagniHandler handles kalagni day calculation
func KalagniHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	birthDay := r.URL.Query().Get("birth_day")
	if birthDay == "" {
		http.Error(w, "Missing birth_day parameter", http.StatusBadRequest)
		return
	}

	day, position, err := CalculateKalagniDay(birthDay)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	response := map[string]interface{}{
		"birth_day":   birthDay,
		"kalagni_day": day,
		"position":    position,
		"calculation": "counter_clockwise",
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// KalagniByAgeHandler handles kalagni day calculation with age
func KalagniByAgeHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	birthDay := r.URL.Query().Get("birth_day")
	ageStr := r.URL.Query().Get("age")

	if birthDay == "" || ageStr == "" {
		http.Error(w, "Missing birth_day or age parameter", http.StatusBadRequest)
		return
	}

	age, err := strconv.Atoi(ageStr)
	if err != nil {
		http.Error(w, "Invalid age format", http.StatusBadRequest)
		return
	}

	day, position, err := CalculateKalagniByAge(birthDay, age)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	response := map[string]interface{}{
		"birth_day":   birthDay,
		"age":         age,
		"kalagni_day": day,
		"position":    position,
		"calculation": "clockwise_with_age",
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// SittiChokHandler handles Sitti Chok days calculation
func SittiChokHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	yearStr := r.URL.Query().Get("year")
	year := time.Now().Year()

	if yearStr != "" {
		if y, err := strconv.Atoi(yearStr); err == nil {
			year = y
		}
	}

	sittiChokDays := GetSittiChokDays(year)

	response := map[string]interface{}{
		"year":            year,
		"sitti_chok_days": sittiChokDays,
		"count":           len(sittiChokDays),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// UbathHandler handles Ubath days calculation
func UbathHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	yearStr := r.URL.Query().Get("year")
	year := time.Now().Year()
	if yearStr != "" {
		if y, err := strconv.Atoi(yearStr); err == nil {
			year = y
		}
	}
	days := GetKalaYokDays(year, "ubath")
	response := map[string]interface{}{
		"year":       year,
		"ubath_days": days,
		"count":      len(days),
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// LokawinatHandler handles Lokawinat days calculation
func LokawinatHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	yearStr := r.URL.Query().Get("year")
	year := time.Now().Year()
	if yearStr != "" {
		if y, err := strconv.Atoi(yearStr); err == nil {
			year = y
		}
	}
	days := GetKalaYokDays(year, "lokawinat")
	response := map[string]interface{}{
		"year":           year,
		"lokawinat_days": days,
		"count":          len(days),
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// SriPositionHandler handles Sri position calculation
func SriPositionHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != "GET" {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// ตัวอย่าง implementation - ต้องเพิ่มฟังก์ชัน CalculateSriPosition ใน advanced_calculations.go
	response := map[string]interface{}{
		"message": "Sri position calculation will be implemented soon",
		"status":  "development",
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}
