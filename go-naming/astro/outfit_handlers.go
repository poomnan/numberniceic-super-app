package astro

import (
	"encoding/json"
	"net/http"
	"strconv"
)

type OutfitDayPaletteUpsertRequest struct {
	DayNumber   int                 `json:"day_number"`
	Shades      []string            `json:"shades"`
	DayPalettes map[string][]string `json:"day_palettes"`
}

func OutfitByAgeHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
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
	if err != nil || age <= 0 {
		http.Error(w, "Invalid age parameter", http.StatusBadRequest)
		return
	}

	result, ok := CalculateOutfitByAge(birthDay, age)
	if !ok {
		http.Error(w, "Invalid birth_day parameter", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(result)
}

func OutfitByBirthDayHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	birthDay := r.URL.Query().Get("birth_day")
	if birthDay == "" {
		http.Error(w, "Missing birth_day parameter", http.StatusBadRequest)
		return
	}

	result, ok := CalculateOutfitByBirthDay(birthDay)
	if !ok {
		http.Error(w, "Invalid birth_day parameter", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(result)
}

func OutfitByCurrentDayHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	day := r.URL.Query().Get("day")
	if day == "" {
		day = CurrentThaiDayName()
	}

	result, ok := CalculateOutfitByCurrentDay(day)
	if !ok {
		http.Error(w, "Invalid day parameter", http.StatusBadRequest)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(result)
}

func OutfitColorSetsHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		CurrentDayName string `json:"current_day_name"`
		BirthDayName   string `json:"birth_day_name"`
		AgeYears       int    `json:"age_years"`
		CurrentDay     string `json:"current_day"`
		BirthDay       string `json:"birth_day"`
		Age            int    `json:"age"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid JSON body", http.StatusBadRequest)
		return
	}

	currentDay := req.CurrentDayName
	if currentDay == "" {
		currentDay = req.CurrentDay
	}
	if currentDay == "" {
		currentDay = CurrentThaiDayName()
	}

	birthDay := req.BirthDayName
	if birthDay == "" {
		birthDay = req.BirthDay
	}
	if birthDay == "" {
		birthDay = "อาทิตย์"
	}

	ageYears := req.AgeYears
	if ageYears <= 0 {
		ageYears = req.Age
	}
	if ageYears <= 0 {
		ageYears = 45
	}

	response, ok := CalculateOutfitColorSets(currentDay, birthDay, ageYears)
	if !ok {
		// Fallback for dynamic clients: always try safe defaults instead of returning 400.
		response, ok = CalculateOutfitColorSets(CurrentThaiDayName(), "อาทิตย์", 45)
		if !ok {
			http.Error(w, "Invalid request parameters", http.StatusBadRequest)
			return
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

func OutfitDayPalettesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	switch r.Method {
	case http.MethodGet:
		palettes := GetOutfitDayPalettes()
		json.NewEncoder(w).Encode(OutfitDayPalettesPayload{
			DayPalettes: serializeDayPalettes(palettes),
		})
	case http.MethodPost:
		var req OutfitDayPaletteUpsertRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, "Invalid JSON body", http.StatusBadRequest)
			return
		}
		var palettes map[int][]string
		if len(req.DayPalettes) > 0 {
			palettes = SetOutfitDayPalettes(parseDayPalettesPayload(req.DayPalettes))
		} else {
			palettes = SetSingleOutfitDayPalette(req.DayNumber, req.Shades)
		}
		json.NewEncoder(w).Encode(OutfitDayPalettesPayload{
			DayPalettes: serializeDayPalettes(palettes),
		})
	default:
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
	}
}
