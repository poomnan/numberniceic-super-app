package handlers

import (
	"go-naming/services"
	"net/http"
)

// PhoneticHandler handles the request to generate a phonetic code for a name.
func PhoneticHandler(w http.ResponseWriter, r *http.Request) {
	name := r.URL.Query().Get("name")
	if name == "" {
		http.Error(w, "Missing 'name' parameter", http.StatusBadRequest)
		return
	}

	code := services.GetThaiPhoneticCode(name)

	response := map[string]string{
		"name":          name,
		"phonetic_code": code,
	}

	jsonResponse(w, http.StatusOK, response)
}

// CheckPhoneticMatchHandler handles the request to check if two names sound alike.
func CheckPhoneticMatchHandler(w http.ResponseWriter, r *http.Request) {
	name1 := r.URL.Query().Get("name1")
	name2 := r.URL.Query().Get("name2")

	if name1 == "" || name2 == "" {
		http.Error(w, "Missing 'name1' or 'name2' parameter", http.StatusBadRequest)
		return
	}

	match := services.CheckPhoneticMatch(name1, name2)
	code1 := services.GetThaiPhoneticCode(name1)
	code2 := services.GetThaiPhoneticCode(name2)

	response := map[string]interface{}{
		"name1":    name1,
		"code1":    code1,
		"name2":    name2,
		"code2":    code2,
		"is_match": match,
	}

	jsonResponse(w, http.StatusOK, response)
}
