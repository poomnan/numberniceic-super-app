package handlers

import (
	"encoding/json"
	"go-naming/database"
	"net/http"
)

func DebugNumbersHandler(w http.ResponseWriter, r *http.Request) {
	rows, err := database.MySQLDB.Query("SELECT pairnumber, description FROM numbers LIMIT 10")
	if err != nil {
		http.Error(w, err.Error(), 500)
		return
	}
	defer rows.Close()

	var results []map[string]string
	for rows.Next() {
		var num, desc string
		if err := rows.Scan(&num, &desc); err != nil {
			continue
		}
		results = append(results, map[string]string{"pairnumber": num, "desc": desc})
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(results)
}
