package astro

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

// Wanpra represents auspicious day data
type Wanpra struct {
	WanpraID    string `json:"wanpra_id"`
	WanpraDate  string `json:"wanpra_date"`
	IsWanpra    string `json:"is_wanpra"`
	IsTongchai  string `json:"is_tongchai"`
	IsAtipbadee string `json:"is_atipbadee"`
	IsRiangMon  string `json:"is_riang_mon"`
	IsLoy       string `json:"is_loy"`
	IsFu        string `json:"is_fu"`
	WanDesc     string `json:"wan_desc"`
}

// WanpraRequest represents the request for wanpra calculation
type WanpraRequest struct {
	BirthDate  string `json:"birth_date"`
	TargetDate string `json:"target_date"`
	DaysCount  int    `json:"days_count"`
}

// WanpraResponse represents the response for wanpra API
type WanpraResponse struct {
	Success      bool     `json:"success"`
	Message      string   `json:"message"`
	Wanpras      []Wanpra `json:"wanpras"`
	BestDates    []string `json:"best_dates"`
	MarriageDays []string `json:"marriage_days"`
	SurgeryDays  []string `json:"surgery_days"`
	HouseDays    []string `json:"house_days"`
}

// WanpraHandler handles wanpra calculation API
func WanpraHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req WanpraRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Validate required fields
	if req.BirthDate == "" {
		http.Error(w, "Birth date is required", http.StatusBadRequest)
		return
	}

	// Calculate wanpra dates (this is a simplified version)
	// In production, this would use the actual Thai astrology algorithms
	wanpras, err := calculateWanpras(req)
	if err != nil {
		http.Error(w, fmt.Sprintf("Calculation error: %v", err), http.StatusInternalServerError)
		return
	}

	// Prepare response
	response := WanpraResponse{
		Success:      true,
		Message:      "Wanpra calculation successful",
		Wanpras:      wanpras,
		BestDates:    extractBestDates(wanpras),
		MarriageDays: extractMarriageDays(wanpras),
		SurgeryDays:  extractSurgeryDays(wanpras),
		HouseDays:    extractHouseDays(wanpras),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// calculateWanpras calculates auspicious days based on Thai astrology
func calculateWanpras(req WanpraRequest) ([]Wanpra, error) {
	var wanpras []Wanpra

	// Parse dates
	birthDate, err := time.Parse("2006-01-02", req.BirthDate)
	if err != nil {
		return nil, fmt.Errorf("invalid birth date format")
	}

	targetDate := time.Now()
	if req.TargetDate != "" {
		targetDate, err = time.Parse("2006-01-02", req.TargetDate)
		if err != nil {
			return nil, fmt.Errorf("invalid target date format")
		}
	}

	// Calculate days (simplified for demonstration)
	daysCount := 30
	if req.DaysCount > 0 {
		daysCount = req.DaysCount
	}

	for i := 0; i < daysCount; i++ {
		currentDate := targetDate.AddDate(0, 0, i)
		dateStr := currentDate.Format("2006-01-02")

		// Simplified calculation - in real implementation, this would use complex Thai astrology algorithms
		isAuspicious := calculateAuspiciousDay(birthDate, currentDate)

		wanpra := Wanpra{
			WanpraID:    fmt.Sprintf("wp_%s", dateStr),
			WanpraDate:  dateStr,
			IsWanpra:    boolToString(isAuspicious.IsWanpra),
			IsTongchai:  boolToString(isAuspicious.IsTongchai),
			IsAtipbadee: boolToString(isAuspicious.IsAtipbadee),
			IsRiangMon:  boolToString(isAuspicious.IsRiangMon),
			IsLoy:       boolToString(isAuspicious.IsLoy),
			IsFu:        boolToString(isAuspicious.IsFu),
			WanDesc:     isAuspicious.Description,
		}
		wanpras = append(wanpras, wanpra)
	}

	return wanpras, nil
}

type AuspiciousResult struct {
	IsWanpra    bool
	IsTongchai  bool
	IsAtipbadee bool
	IsRiangMon  bool
	IsLoy       bool
	IsFu        bool
	Description string
}

// calculateAuspiciousDay - Simplified Thai astrology calculation
// In production, this would be replaced with actual algorithms
func calculateAuspiciousDay(birthDate, currentDate time.Time) AuspiciousResult {
	// This is a placeholder implementation
	// Real implementation would use complex Thai astrology calculations

	dayOfWeek := int(currentDate.Weekday())
	dayOfMonth := currentDate.Day()

	result := AuspiciousResult{}

	// Simple rules for demonstration
	if dayOfWeek == 0 { // Sunday
		result.IsTongchai = true
		result.Description = "วันธงชัย"
	} else if dayOfWeek == 2 { // Tuesday
		result.IsAtipbadee = true
		result.Description = "วันอธิบดี"
	} else if dayOfWeek == 4 { // Thursday
		result.IsRiangMon = true
		result.Description = "วันเรียงหมอน"
	}

	// Buddhist days (wanpra)
	if dayOfMonth == 8 || dayOfMonth == 15 {
		result.IsWanpra = true
		if result.Description == "" {
			result.Description = "วันพระ"
		} else {
			result.Description += ", วันพระ"
		}
	}

	// Loy and Fu days (simplified)
	if dayOfMonth%7 == 0 {
		result.IsLoy = true
		if result.Description == "" {
			result.Description = "วันลอย"
		} else {
			result.Description += ", วันลอย"
		}
	}
	if dayOfMonth%5 == 0 {
		result.IsFu = true
		if result.Description == "" {
			result.Description = "วันฟู"
		} else {
			result.Description += ", วันฟู"
		}
	}

	return result
}

func boolToString(b bool) string {
	if b {
		return "1"
	}
	return "0"
}

func extractBestDates(wanpras []Wanpra) []string {
	var bestDates []string
	for _, wp := range wanpras {
		if wp.IsTongchai == "1" || wp.IsAtipbadee == "1" || wp.IsRiangMon == "1" {
			bestDates = append(bestDates, wp.WanpraDate)
		}
	}
	return bestDates
}

func extractMarriageDays(wanpras []Wanpra) []string {
	var days []string
	for _, wp := range wanpras {
		if wp.IsRiangMon == "1" {
			days = append(days, wp.WanpraDate)
		}
	}
	return days
}

func extractSurgeryDays(wanpras []Wanpra) []string {
	var days []string
	for _, wp := range wanpras {
		if wp.IsAtipbadee == "1" {
			days = append(days, wp.WanpraDate)
		}
	}
	return days
}

func extractHouseDays(wanpras []Wanpra) []string {
	var days []string
	for _, wp := range wanpras {
		if wp.IsTongchai == "1" || wp.IsAtipbadee == "1" {
			days = append(days, wp.WanpraDate)
		}
	}
	return days
}
