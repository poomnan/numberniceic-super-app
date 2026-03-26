package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"
)

// CheckAssignmentHandler checks and returns a valid assignment, or creates a new one if expired.
// It stores birth_date in member table and uses year-based expiration (birthday to birthday).
func CheckAssignmentHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		UserID    int    `json:"user_id"`
		Type      string `json:"type"`       // e.g. "inauspicious", "buddha"
		BirthDate string `json:"birth_date"` // YYYY-MM-DD (optional if already stored)
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}

	if req.UserID == 0 || req.Type == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "user_id and type are required"})
		return
	}

	// 1. Get or Update birth_date in member table
	var storedBirthDate sql.NullString
	err := database.DB.QueryRow("SELECT birth_date FROM member WHERE id = $1", req.UserID).Scan(&storedBirthDate)

	if err != nil && err != sql.ErrNoRows {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to fetch user: " + err.Error()})
		return
	}

	// Update birth_date if provided and different from stored
	if req.BirthDate != "" {
		if !storedBirthDate.Valid || storedBirthDate.String != req.BirthDate {
			_, err = database.DB.Exec("UPDATE member SET birth_date = $1 WHERE id = $2", req.BirthDate, req.UserID)
			if err != nil {
				jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to update birth_date: " + err.Error()})
				return
			}
			storedBirthDate = sql.NullString{String: req.BirthDate, Valid: true}
		}
	}

	// Check if we have a birth_date
	if !storedBirthDate.Valid {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "birth_date is required (not stored in database)"})
		return
	}

	birthDateStr := storedBirthDate.String

	// Parse birth date
	birthDate, err := time.Parse("2006-01-02", birthDateStr)
	if err != nil {
		// Try timestamp format
		birthDate, err = time.Parse(time.RFC3339, birthDateStr)
		if err != nil {
			jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid birth_date format: " + err.Error()})
			return
		}
	}

	// 2. Calculate which year this assignment should be for
	now := time.Now()
	currentYear := now.Year()

	// Birthday this year
	birthdayThisYear := time.Date(currentYear, birthDate.Month(), birthDate.Day(), 0, 0, 0, 0, time.Local)

	// Determine assignment year: if birthday hasn't passed yet, use last year's assignment
	assignmentYear := currentYear
	if now.Before(birthdayThisYear) {
		assignmentYear = currentYear - 1
	}

	// 3. Check Existing Assignment for this year
	var currentVal string
	var storedYear int

	err = database.DB.QueryRow(`
		SELECT assignment_value, assignment_year 
		FROM user_assignment_items 
		WHERE user_id = $1 AND assignment_type = $2
	`, req.UserID, req.Type).Scan(&currentVal, &storedYear)

	if err == nil {
		// Found existing. Check if it's for the correct year
		if storedYear == assignmentYear {
			// Still valid for this year!
			jsonResponse(w, http.StatusOK, map[string]interface{}{
				"status":          "valid",
				"value":           currentVal,
				"assignment_year": storedYear,
			})
			return
		}
		// Different year, need to generate new
	} else if err != sql.ErrNoRows {
		// DB Error
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "DB Error: " + err.Error()})
		return
	}

	// 4. Generate NEW Assignment for this year
	newValue := generateAssignmentValue(req.Type, req.UserID, assignmentYear)

	// 5. Save/Update DB
	_, err = database.DB.Exec(`
		INSERT INTO user_assignment_items (user_id, assignment_type, assignment_value, assignment_year, assigned_at)
		VALUES ($1, $2, $3, $4, NOW())
		ON CONFLICT (user_id, assignment_type) 
		DO UPDATE SET assignment_value = EXCLUDED.assignment_value, 
		              assignment_year = EXCLUDED.assignment_year,
		              assigned_at = NOW()
	`, req.UserID, req.Type, newValue, assignmentYear)

	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to save assignment: " + err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"status":          "updated",
		"value":           newValue,
		"assignment_year": assignmentYear,
	})
}

// GetBuddhaPangsHandler returns the list of all available Buddha pangs from Database
func GetBuddhaPangsHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	rows, err := database.MySQLDB.Query("SELECT id, pang_name, buddha_day, description, image_url FROM buddha_pang_tb ORDER BY id ASC")
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to fetch pangs: " + err.Error()})
		return
	}
	defer rows.Close()

	var response []map[string]interface{}
	for rows.Next() {
		var id, day int
		var name, desc, img string
		if err := rows.Scan(&id, &name, &day, &desc, &img); err != nil {
			log.Printf("Error scanning pang: %v", err)
			continue
		}

		response = append(response, map[string]interface{}{
			"id":          id,
			"pang_name":   name,
			"description": desc,
			"image_url":   img,
			"buddha_day":  day,
		})
	}

	if response == nil {
		response = []map[string]interface{}{}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetAssignedInauspiciousHandler returns the current inauspicious day assignment for a user
func GetAssignedInauspiciousHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	if len(parts) < 5 {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid URL format"})
		return
	}
	memberIDStr := parts[4]

	// Fetch from user_inauspicious_assign
	rows, err := database.MySQLDB.Query(`
		SELECT id, type, title, description, image_url, assigned_at 
		FROM user_inauspicious_assign 
		WHERE memberid = ?
		ORDER BY assigned_at DESC
	`, memberIDStr)

	if err != nil {
		log.Printf("Inauspicious: DB error for user %s: %v", memberIDStr, err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Database error"})
		return
	}

	defer rows.Close()

	var response []map[string]interface{}
	for rows.Next() {
		var id int
		var aType, title, desc, img sql.NullString
		var assignedAt time.Time

		if err := rows.Scan(&id, &aType, &title, &desc, &img, &assignedAt); err != nil {
			log.Printf("Inauspicious: Scan error: %v", err)
			continue
		}

		response = append(response, map[string]interface{}{
			"id":          id,
			"type":        aType.String,
			"title":       title.String,
			"description": desc.String,
			"image_url":   img.String,
			"assigned_at": assignedAt.Format("2006-01-02 15:04:05"),
		})
	}

	// Fallback if empty (deterministic)
	if len(response) == 0 {
		mID, _ := strconv.Atoi(memberIDStr)
		val := generateAssignmentValue("inauspicious", mID, time.Now().Year())
		response = []map[string]interface{}{
			{
				"id":          mID + 2000,
				"type":        "year",
				"title":       "วันอัปมงคลประจำปี",
				"description": val,
				"image_url":   nil,
				"assigned_at": time.Now().Format("2006-01-02 15:04:05"),
			},
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// AssignBuddhaHandler manually assigns a buddha pang to a user
func AssignBuddhaHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		UserID         int    `json:"user_id"`
		MemberID       int    `json:"memberid"`
		AssignmentType string `json:"assignment_type"` // "annual", "lifetime"
		BuddhaID       int    `json:"buddha_id"`
		Description    string `json:"description"`
		CustomDesc     string `json:"custom_description"`
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}

	uid := req.UserID
	if uid == 0 {
		uid = req.MemberID
	}
	if uid == 0 {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "user_id or memberid is required"})
		return
	}

	aType := req.AssignmentType
	if aType == "" {
		aType = "annual"
	}

	customMsg := req.Description
	if customMsg == "" {
		customMsg = req.CustomDesc
	}

	// 1. Verify BuddhaID
	var pangName string
	err := database.MySQLDB.QueryRow("SELECT pang_name FROM buddha_pang_tb WHERE id = ?", req.BuddhaID).Scan(&pangName)
	if err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid buddha_id"})
		return
	}

	// 2. Save to user_buddha_assign
	_, err = database.MySQLDB.Exec(`
		INSERT INTO user_buddha_assign (memberid, assignment_type, buddha_id, custom_description, assigned_at)
		VALUES (?, ?, ?, ?, NOW())
	`, strconv.Itoa(uid), aType, req.BuddhaID, customMsg)

	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Failed to save assignment: " + err.Error()})
		return
	}

	// Notify
	go func(targetUid int, name string) {
		var token string
		err := database.MySQLDB.QueryRow("SELECT fcm_token FROM membertb WHERE memberid = ?", targetUid).Scan(&token)

		title := "✨ มีการอัปเดตข้อมูลมงคลของคุณ"
		body := fmt.Sprintf("แอดมินได้จัดสำรับพระพุทธรูปปางใหม่ให้คุณแล้ว: %s", name)

		// Insert into notifications for the app to see
		database.MySQLDB.Exec(`
			INSERT INTO notifications (member_id, type, title, body, note, created_at)
			VALUES (?, 'buddha_assign', ?, ?, ?, NOW())
		`, strconv.Itoa(targetUid), title, body, "Type: "+aType)

		if err == nil && token != "" {
			services.SendFCMNotificationV1(token, title, body, map[string]string{"type": "buddha_assign"})
		}
	}(uid, pangName)

	jsonResponse(w, http.StatusOK, map[string]string{"status": "success", "message": "จัดสำรับพระพุทธรูปเรียบร้อยแล้ว"})
}

// AddBuddhaPangHandler adds or updates a Buddha Pang definition
func AddBuddhaPangHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		ID          int    `json:"id"`
		PangName    string `json:"pang_name"`
		BuddhaDay   int    `json:"buddha_day"`
		Description string `json:"description"`
		ImageURL    string `json:"image_url"`
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}

	if req.PangName == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "pang_name is required"})
		return
	}

	if req.ID > 0 {
		// Update
		query := `UPDATE buddha_pang_tb SET pang_name = ?, buddha_day = ?, description = ?, image_url = ? WHERE id = ?`
		_, err := database.MySQLDB.Exec(query, req.PangName, req.BuddhaDay, req.Description, req.ImageURL, req.ID)
		if err != nil {
			jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
	} else {
		// Insert
		query := `INSERT INTO buddha_pang_tb (pang_name, buddha_day, description, image_url) VALUES (?, ?, ?, ?)`
		_, err := database.MySQLDB.Exec(query, req.PangName, req.BuddhaDay, req.Description, req.ImageURL)
		if err != nil {
			jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
	}

	jsonResponse(w, http.StatusOK, map[string]string{"status": "success", "message": "บันทึกข้อมูลพระปางเรียบร้อยแล้ว"})
}

// GetAssignedBuddhaHandler returns both annual and lifetime buddha pang assignments for a user
func GetAssignedBuddhaHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	parts := strings.Split(r.URL.Path, "/")
	if len(parts) < 5 {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid URL format"})
		return
	}
	memberIDStr := parts[4]
	log.Printf("GetAssignedBuddha: Requested for memberID: %s", memberIDStr)

	// Fetch assignments from user_buddha_assign joined with buddha_pang_tb
	query := `
		SELECT 
			a.id, a.assignment_type, a.custom_description, a.assigned_at,
			p.pang_name, p.description, p.image_url
		FROM user_buddha_assign a
		JOIN buddha_pang_tb p ON a.buddha_id = p.id
		WHERE a.memberid = ?
		ORDER BY a.assigned_at DESC
	`
	rows, err := database.MySQLDB.Query(query, memberIDStr)
	if err != nil {
		log.Printf("GetAssignedBuddha: DB error: %v", err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Database error"})
		return
	}

	defer rows.Close()

	annuals := []map[string]interface{}{}
	lifetimes := []map[string]interface{}{}

	for rows.Next() {
		var id int
		var aType, customDesc, pangName, desc, img, assignedAt sql.NullString

		if err := rows.Scan(&id, &aType, &customDesc, &assignedAt, &pangName, &desc, &img); err != nil {
			log.Printf("GetAssignedBuddha: Scan error: %v", err)
			continue
		}

		item := map[string]interface{}{
			"id":                 id,
			"pang_name":          pangName.String,
			"description":        desc.String,
			"image_url":          img.String,
			"custom_description": customDesc.String,
			"assignment_type":    aType.String,
			"assigned_at":        assignedAt.String,
		}

		if aType.String == "lifetime" || aType.String == "buddha_lifetime" {
			lifetimes = append(lifetimes, item)
		} else {
			annuals = append(annuals, item)
		}
	}

	response := map[string]interface{}{
		"annual":   annuals,
		"lifetime": lifetimes,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetAssignedSpellsHandler returns spells assigned to a user
func GetAssignedSpellsHandler(w http.ResponseWriter, r *http.Request) {
	log.Printf("GetAssignedSpells: URL: %s", r.URL.Path)
	parts := strings.Split(r.URL.Path, "/")
	if len(parts) < 5 {

		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid URL"})
		return
	}
	memberID := parts[4]

	query := `
		SELECT s.id, s.title, s.content, s.type, s.photo, n.note, n.created_at
		FROM member_spell_notes n
		JOIN spells_warnings s ON n.spell_id = s.id
		WHERE n.memberid = ?
		ORDER BY n.created_at DESC
	`
	rows, err := database.MySQLDB.Query(query, memberID)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	defer rows.Close()

	var spells []map[string]interface{}
	for rows.Next() {
		var id int
		var title, content, sType, photo, note string
		var createdAt time.Time
		rows.Scan(&id, &title, &content, &sType, &photo, &note, &createdAt)
		spells = append(spells, map[string]interface{}{
			"id":          strconv.Itoa(id),
			"title":       title,
			"content":     content,
			"type":        sType,
			"photo":       photo,
			"note":        note,
			"assigned_at": createdAt.Format("2006-01-02 15:04:05"),
		})
	}
	if spells == nil {
		spells = []map[string]interface{}{}
	}
	jsonResponse(w, http.StatusOK, map[string]interface{}{"data": spells})
}

// GetAssignedMeritHandler returns merit/change-number assignments
func GetAssignedMeritHandler(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	if len(parts) < 5 {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid URL"})
		return
	}
	memberID := parts[4]
	mType := r.URL.Query().Get("type") // "merit", "changenum", "spell"

	query := `
		SELECT id, merit_type, title, body, url, assigned_at
		FROM user_merit_assign
		WHERE memberid = ?
	`
	if mType != "" {
		query += " AND merit_type = '" + mType + "'"
	}
	query += " ORDER BY assigned_at DESC"

	rows, err := database.MySQLDB.Query(query, memberID)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	defer rows.Close()

	var list []map[string]interface{}
	for rows.Next() {
		var id int
		var aType, title, body, url string
		var assignedAt time.Time
		rows.Scan(&id, &aType, &title, &body, &url, &assignedAt)
		list = append(list, map[string]interface{}{
			"id":          id,
			"type":        aType,
			"title":       title,
			"body":        body,
			"url":         url,
			"assigned_at": assignedAt.Format("2006-01-02 15:04:05"),
		})
	}
	if list == nil {
		list = []map[string]interface{}{}
	}
	jsonResponse(w, http.StatusOK, list)
}

// GetAssignedSacredTempleHandler returns assigned temples
func GetAssignedSacredTempleHandler(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(r.URL.Path, "/")
	if len(parts) < 5 {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid URL"})
		return
	}
	memberID := parts[4]

	query := `
		SELECT t.id, t.temple_name, t.description, t.address, t.image_url, t.latitude, t.longitude, a.custom_description, a.assigned_at
		FROM user_temple_assign a
		JOIN sacred_temple_tb t ON a.temple_id = t.id
		WHERE a.memberid = ?
		ORDER BY a.assigned_at DESC
	`
	rows, err := database.MySQLDB.Query(query, memberID)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	defer rows.Close()

	var list []map[string]interface{}
	for rows.Next() {
		var id int
		var name, desc, addr, img, customDesc string
		var lat, lng float64
		var assignedAt time.Time
		rows.Scan(&id, &name, &desc, &addr, &img, &lat, &lng, &customDesc, &assignedAt)
		list = append(list, map[string]interface{}{
			"id":                 id,
			"temple_name":        name,
			"description":        desc,
			"address":            addr,
			"image_url":          img,
			"latitude":           lat,
			"longitude":          lng,
			"custom_description": customDesc,
			"assigned_at":        assignedAt.Format("2006-01-02 15:04:05"),
		})
	}
	if list == nil {
		list = []map[string]interface{}{}
	}
	jsonResponse(w, http.StatusOK, list)
}

// DeleteBuddhaAssignmentHandler deletes a specific Buddha pang assignment for a user
func DeleteBuddhaAssignmentHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		MemberID       string `json:"memberid"`
		BuddhaID       string `json:"buddha_id"`
		AssignmentType string `json:"assignment_type"` // "annual" or "lifetime"
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}

	if req.MemberID == "" || req.BuddhaID == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "memberid and buddha_id are required"})
		return
	}

	log.Printf("DeleteBuddhaAssignment: member=%s, assignment_id=%s, type=%s", req.MemberID, req.BuddhaID, req.AssignmentType)

	// Delete by assignment row id (buddha_id here is actually the assignment row id from user_buddha_assign.id)
	var result sql.Result
	var err error

	if req.AssignmentType != "" {
		result, err = database.MySQLDB.Exec(
			`DELETE FROM user_buddha_assign WHERE id = ? AND memberid = ? AND assignment_type = ?`,
			req.BuddhaID, req.MemberID, req.AssignmentType,
		)
	} else {
		result, err = database.MySQLDB.Exec(
			`DELETE FROM user_buddha_assign WHERE id = ? AND memberid = ?`,
			req.BuddhaID, req.MemberID,
		)
	}

	if err != nil {
		log.Printf("DeleteBuddhaAssignment: DB error: %v", err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Database error: " + err.Error()})
		return
	}

	rowsAffected, _ := result.RowsAffected()
	log.Printf("DeleteBuddhaAssignment: Deleted %d row(s) for member=%s, assignment_id=%s", rowsAffected, req.MemberID, req.BuddhaID)

	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"status":        "success",
		"rows_affected": rowsAffected,
	})
}

// DeleteMeritAssignmentHandler deletes a merit or change number assignment
func DeleteMeritAssignmentHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	var req struct {
		MemberID string `json:"memberid"`
		ID       string `json:"id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}
	if req.MemberID == "" || req.ID == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "memberid and id are required"})
		return
	}
	result, err := database.MySQLDB.Exec(`DELETE FROM user_merit_assign WHERE id = ? AND memberid = ?`, req.ID, req.MemberID)
	if err != nil {
		log.Printf("DeleteMeritAssignment: DB error: %v", err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Database error: " + err.Error()})
		return
	}
	rowsAffected, _ := result.RowsAffected()
	jsonResponse(w, http.StatusOK, map[string]interface{}{"status": "success", "rows_affected": rowsAffected})
}

// DeleteTempleAssignmentHandler deletes a sacred temple assignment
func DeleteTempleAssignmentHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	var req struct {
		MemberID string `json:"memberid"`
		TempleID string `json:"temple_id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}
	if req.MemberID == "" || req.TempleID == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "memberid and temple_id are required"})
		return
	}
	result, err := database.MySQLDB.Exec(`DELETE FROM user_temple_assign WHERE temple_id = ? AND memberid = ?`, req.TempleID, req.MemberID)
	if err != nil {
		log.Printf("DeleteTempleAssignment: DB error: %v", err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Database error: " + err.Error()})
		return
	}
	rowsAffected, _ := result.RowsAffected()
	jsonResponse(w, http.StatusOK, map[string]interface{}{"status": "success", "rows_affected": rowsAffected})
}

// DeleteSpellAssignmentHandler deletes a spell assignment
func DeleteSpellAssignmentHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	var req struct {
		MemberID string `json:"memberid"`
		SpellID  string `json:"spell_id"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}
	if req.MemberID == "" || req.SpellID == "" {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "memberid and spell_id are required"})
		return
	}
	result, err := database.MySQLDB.Exec(`DELETE FROM member_spell_notes WHERE spell_id = ? AND memberid = ?`, req.SpellID, req.MemberID)
	if err != nil {
		log.Printf("DeleteSpellAssignment: DB error: %v", err)
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": "Database error: " + err.Error()})
		return
	}
	rowsAffected, _ := result.RowsAffected()
	jsonResponse(w, http.StatusOK, map[string]interface{}{"status": "success", "rows_affected": rowsAffected})
}

// jsonResponse helper removed - already defined in package handlers (e.g., in name_handler.go)

// Helper: Calculate Next Birthday
func calculateNextBirthday(birthDateStr string) (time.Time, error) {
	// Try parsing as DATE first (YYYY-MM-DD)
	birth, err := time.Parse("2006-01-02", birthDateStr)
	if err != nil {
		// Try parsing as TIMESTAMP (YYYY-MM-DDTHH:MM:SSZ)
		birth, err = time.Parse(time.RFC3339, birthDateStr)
		if err != nil {
			return time.Time{}, err
		}
	}

	now := time.Now()
	year := now.Year()

	// Construct birthday for this year
	nextBirthday := time.Date(year, birth.Month(), birth.Day(), 0, 0, 0, 0, time.Local)

	// If already passed this year, add 1 year
	if nextBirthday.Before(now) {
		nextBirthday = nextBirthday.AddDate(1, 0, 0)
	}

	return nextBirthday, nil
}

// Helper: Generate Value based on UserID + Assignment Year
func generateAssignmentValue(assignType string, userID int, assignmentYear int) string {
	// Deterministic rotation based on UserID + Year
	seed := userID + assignmentYear

	switch assignType {
	case "inauspicious":
		// Return a day (e.g., 'Monday', 'Tuesday')
		days := []string{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"}
		return days[seed%len(days)]
	case "buddha", "buddha_lifetime":
		// Return a Buddha Pang Name matching buddha_pangs table
		pangs := []string{
			"พระประจำวันอาทิตย์ (ปางถวายเนตร)",
			"พระประจำวันจันทร์ (ปางห้ามสมุทร/ห้ามญาติ)",
			"พระประจำวันอังคาร (ปางไสยาสน์)",
			"พระประจำวันพุธกลางวัน (ปางอุ้มบาตร)",
			"พระประจำวันพุธกลางคืน (ปางป่าเลไลยก์)",
			"พระประจำวันพฤหัสบดี (ปางสมาธิ)",
			"พระประจำวันศุกร์ (ปางรำพึง)",
			"พระประจำวันเสาร์ (ปางนาคปรก)",
		}
		// For lifetime, we use a different seed offset to ensure it's different from annual
		if assignType == "buddha_lifetime" {
			seed += 123
		}
		return pangs[seed%len(pangs)]
	default:
		return "DefaultValue"
	}
}
