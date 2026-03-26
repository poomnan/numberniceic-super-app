package handlers

import (
	"go-naming/services"
	"net/http"
)

// GetGoodSumsHandler returns the list of auspicious numbers
func GetGoodSumsHandler(w http.ResponseWriter, r *http.Request) {
	sums, err := services.GetGoodSums()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	jsonResponse(w, http.StatusOK, sums)
}

// GetAllPairStatusHandler returns a map of pairnumber -> boolean (isGood)
func GetAllPairStatusHandler(w http.ResponseWriter, r *http.Request) {
	meanings, err := services.GetPairMeaningsMap()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	statusMap := make(map[string]bool)
	for pair, m := range meanings {
		// D-series are considered Good (as defined in GetPairMeaningsMap)
		if len(m) >= 4 && m[:4] == "Good" {
			statusMap[pair] = true
		} else {
			statusMap[pair] = false
		}
	}
	jsonResponse(w, http.StatusOK, statusMap)
}
