package handlers

import (
	"encoding/json"
	"go-naming/database"
	"go-naming/models"
	"net/http"
	"strconv"
)

// GetPhoneSellListHandler returns all phone numbers for sale
func GetPhoneSellListHandler(w http.ResponseWriter, r *http.Request) {
	rows, err := database.DB.Query("SELECT pnumber_id, pnumber_position, pnumber_num, pnumber_sum, pnumber_price, phone_group, sell_status, prefix_group FROM phonenumber_sell ORDER BY pnumber_id DESC")
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	defer rows.Close()

	var list []models.PhoneSell
	for rows.Next() {
		var p models.PhoneSell
		if err := rows.Scan(&p.ID, &p.Position, &p.Number, &p.Sum, &p.Price, &p.PhoneGroup, &p.SellStatus, &p.PrefixGroup); err != nil {
			productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		list = append(list, p)
	}
	if list == nil {
		list = []models.PhoneSell{}
	}
	productJsonResponse(w, http.StatusOK, map[string]interface{}{"phonenumber_sell": list})
}

// AddPhoneSellHandler adds a new phone number
func AddPhoneSellHandler(w http.ResponseWriter, r *http.Request) {
	var p models.PhoneSell
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		productJsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}

	// Basic validation/defaults
	if p.PhoneGroup == "" {
		p.PhoneGroup = "vip"
	}
	if p.SellStatus == "" {
		p.SellStatus = "online"
	}

	query := `
		INSERT INTO phonenumber_sell (pnumber_position, pnumber_num, pnumber_sum, pnumber_price, phone_group, sell_status, prefix_group)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
		RETURNING pnumber_id
	`
	err := database.DB.QueryRow(query, p.Position, p.Number, p.Sum, p.Price, p.PhoneGroup, p.SellStatus, p.PrefixGroup).Scan(&p.ID)
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	productJsonResponse(w, http.StatusCreated, p)
}

// UpdatePhoneSellHandler updates a phone number
func UpdatePhoneSellHandler(w http.ResponseWriter, r *http.Request) {
	var p models.PhoneSell
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		productJsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}

	query := `
		UPDATE phonenumber_sell 
		SET pnumber_position = $1, pnumber_num = $2, pnumber_sum = $3, pnumber_price = $4, phone_group = $5, sell_status = $6, prefix_group = $7
		WHERE pnumber_id = $8
	`
	_, err := database.DB.Exec(query, p.Position, p.Number, p.Sum, p.Price, p.PhoneGroup, p.SellStatus, p.PrefixGroup, p.ID)
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	productJsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

// DeletePhoneSellHandler deletes a phone number
func DeletePhoneSellHandler(w http.ResponseWriter, r *http.Request) {
	idStr := r.URL.Query().Get("id")
	id, _ := strconv.Atoi(idStr)

	_, err := database.DB.Exec("DELETE FROM phonenumber_sell WHERE pnumber_id = $1", id)
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	productJsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}
