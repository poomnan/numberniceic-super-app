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
)

// UserZ represents a member from MySQL membertb (matches Android UserZ data class)
type UserZ struct {
	MemberID string `json:"member_id"`
	Username string `json:"username"`
	RealName string `json:"realname"`
	Surname  string `json:"surname"`
	Birthday string `json:"birthday"`
	FcmToken string `json:"fcm_token"`
}

// UsersResponse matches the Android Users data class
type UsersResponse struct {
	ResultUserz []UserZ `json:"result_userz"`
}

// FindUserBagColorHandler handles GET /admin/finduser/bagcolor/{username}
func FindUserBagColorHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// Extract username from path: /admin/finduser/bagcolor/{username}
	path := strings.TrimPrefix(r.URL.Path, "/admin/finduser/bagcolor/")
	username := strings.TrimSpace(path)

	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		log.Println("FindUserBagColor: MySQL not available")
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		json.NewEncoder(w).Encode(UsersResponse{ResultUserz: []UserZ{}})
		return
	}

	var rows *sql.Rows
	var err error

	if username == "" || username == "/" {
		// Return latest 20 users
		rows, err = mysqlDB.Query(
			`SELECT memberid, username, COALESCE(realname,''), COALESCE(surname,''), COALESCE(birthday,''), COALESCE(fcm_token,'')
			 FROM membertb
			 ORDER BY memberid DESC
			 LIMIT 20`)
	} else {
		// Search by username, realname, surname, or memberid
		query := "%" + username + "%"
		rows, err = mysqlDB.Query(
			`SELECT memberid, username, COALESCE(realname,''), COALESCE(surname,''), COALESCE(birthday,''), COALESCE(fcm_token,'')
			 FROM membertb
			 WHERE username LIKE ?
			    OR realname LIKE ?
			    OR surname LIKE ?
			    OR memberid LIKE ?
			 ORDER BY
			   CASE
			     WHEN username = ? THEN 1
			     WHEN username LIKE ? THEN 2
			     WHEN realname = ? THEN 3
			     WHEN realname LIKE ? THEN 4
			     WHEN memberid = ? THEN 5
			     ELSE 6
			   END ASC,
			   memberid DESC
			 LIMIT 20`,
			query, query, query, query,
			username, username+"%", username, username+"%", username)
	}

	if err != nil {
		log.Printf("FindUserBagColor: query error: %v", err)
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		json.NewEncoder(w).Encode(UsersResponse{ResultUserz: []UserZ{}})
		return
	}
	defer rows.Close()

	users := []UserZ{}
	for rows.Next() {
		var u UserZ
		if err := rows.Scan(&u.MemberID, &u.Username, &u.RealName, &u.Surname, &u.Birthday, &u.FcmToken); err != nil {
			log.Printf("FindUserBagColor: scan error: %v", err)
			continue
		}
		users = append(users, u)
	}

	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	json.NewEncoder(w).Encode(UsersResponse{ResultUserz: users})
}

// --- Colorx matches Android Colorx data class ---
type Colorx struct {
	BagID            *string `json:"bag_id"`
	MemberID         *string `json:"memberid"`
	Age              *string `json:"age"`
	BagColor1        *string `json:"bag_color1"`
	BagColor2        *string `json:"bag_color2"`
	BagColor3        *string `json:"bag_color3"`
	BagColor4        *string `json:"bag_color4"`
	BagColor5        *string `json:"bag_color5"`
	BagColor6        *string `json:"bag_color6"`
	DateColorUpdated *string `json:"date_color_updated"`
}

type ColorSixResponse struct {
	UserID    string  `json:"user_id"`
	ColorSixA *Colorx `json:"color_six_a"`
	ColorSixB *Colorx `json:"color_six_b"`
}

// GetColorSixHandler handles GET /admin/bagcolor/{userId}
func GetColorSixHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	userId := strings.TrimPrefix(r.URL.Path, "/admin/bagcolor/")
	userId = strings.TrimSpace(userId)

	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		json.NewEncoder(w).Encode(ColorSixResponse{UserID: userId})
		return
	}

	rows, err := mysqlDB.Query(
		`SELECT bag_id, memberid, age, bag_color1, bag_color2, bag_color3, bag_color4, bag_color5, bag_color6, date_color_updated
		 FROM bagcolortb WHERE memberid = ? ORDER BY bag_id ASC LIMIT 2`, userId)
	if err != nil {
		log.Printf("GetColorSix: query error: %v", err)
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		json.NewEncoder(w).Encode(ColorSixResponse{UserID: userId})
		return
	}
	defer rows.Close()

	var colors []Colorx
	for rows.Next() {
		var c Colorx
		if err := rows.Scan(&c.BagID, &c.MemberID, &c.Age, &c.BagColor1, &c.BagColor2, &c.BagColor3, &c.BagColor4, &c.BagColor5, &c.BagColor6, &c.DateColorUpdated); err != nil {
			log.Printf("GetColorSix: scan error: %v", err)
			continue
		}
		colors = append(colors, c)
	}

	resp := ColorSixResponse{UserID: userId}
	if len(colors) >= 1 {
		resp.ColorSixA = &colors[0]
	}
	if len(colors) >= 2 {
		resp.ColorSixB = &colors[1]
	}

	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	json.NewEncoder(w).Encode(resp)
}

// InsertBagColorHandler handles POST /admin/bagcolor
func InsertBagColorHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var body map[string]interface{}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"insert_color": "fail", "error": err.Error()})
		return
	}

	memberid := fmt.Sprintf("%v", body["memberid"])
	age := fmt.Sprintf("%v", body["age"])
	c1 := getStr(body, "bag_color1a", "#FFFFFF")
	c2 := getStr(body, "bag_color2a", "#FFFFFF")
	c3 := getStr(body, "bag_color3a", "#FFFFFF")
	c4 := getStr(body, "bag_color4a", "#FFFFFF")
	c5 := getStr(body, "bag_color5a", "#FFFFFF")
	c6 := getStr(body, "bag_color6a", "#FFFFFF")

	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"insert_color": "fail", "error": "MySQL not available"})
		return
	}

	_, err := mysqlDB.Exec(
		`INSERT INTO bagcolortb (memberid, age, bag_color1, bag_color2, bag_color3, bag_color4, bag_color5, bag_color6, date_color_updated)
		 VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())`,
		memberid, age, c1, c2, c3, c4, c5, c6)

	result := "success"
	if err != nil {
		log.Printf("InsertBagColor: %v", err)
		result = "fail"
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"insert_color": result})
}

// UpdateBagColorHandler handles PUT /admin/bagcolor
func UpdateBagColorHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPut {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var body map[string]interface{}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"success_update_a": "false", "success_update_b": "false"})
		return
	}

	memberid := fmt.Sprintf("%v", body["memberid"])
	ageFloat, _ := body["age"].(float64)
	age1 := strconv.Itoa(int(ageFloat))
	age2 := strconv.Itoa(int(ageFloat) + 1)

	ca := [6]string{getStr(body, "color0", ""), getStr(body, "color1", ""), getStr(body, "color2", ""), getStr(body, "color3", ""), getStr(body, "color4", ""), getStr(body, "color5", "")}
	cb := [6]string{getStr(body, "colorb0", ""), getStr(body, "colorb1", ""), getStr(body, "colorb2", ""), getStr(body, "colorb3", ""), getStr(body, "colorb4", ""), getStr(body, "colorb5", "")}

	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"success_update_a": "false", "success_update_b": "false"})
		return
	}

	successA := upsertBagColor(mysqlDB, memberid, age1, ca)
	successB := upsertBagColor(mysqlDB, memberid, age2, cb)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"success_update_a": boolStr(successA),
		"success_update_b": boolStr(successB),
	})
}

func upsertBagColor(db *sql.DB, memberid, age string, colors [6]string) bool {
	var bagID int
	err := db.QueryRow("SELECT bag_id FROM bagcolortb WHERE memberid = ? AND age = ? LIMIT 1", memberid, age).Scan(&bagID)

	if err == sql.ErrNoRows {
		// INSERT
		_, err = db.Exec(
			`INSERT INTO bagcolortb (memberid, age, bag_color1, bag_color2, bag_color3, bag_color4, bag_color5, bag_color6, date_color_updated)
			 VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())`,
			memberid, age, colors[0], colors[1], colors[2], colors[3], colors[4], colors[5])
	} else if err == nil {
		// UPDATE
		_, err = db.Exec(
			`UPDATE bagcolortb SET bag_color1=?, bag_color2=?, bag_color3=?, bag_color4=?, bag_color5=?, bag_color6=?, date_color_updated=NOW() WHERE bag_id=?`,
			colors[0], colors[1], colors[2], colors[3], colors[4], colors[5], bagID)
	}
	return err == nil
}

// SendBagColorNotificationHandler handles GET /admin/notifications/send-bag-colors?memberid=xxx
func SendBagColorNotificationHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	memberid := r.URL.Query().Get("memberid")
	if memberid == "" {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": "missing memberid"})
		return
	}

	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": "MySQL not available"})
		return
	}

	// Get user's FCM token
	var fcmToken string
	err := mysqlDB.QueryRow("SELECT COALESCE(fcm_token,'') FROM membertb WHERE memberid = ?", memberid).Scan(&fcmToken)
	if err != nil || fcmToken == "" {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"status":     "completed",
			"sent_count": 0,
			"details": []map[string]interface{}{{
				"debug": map[string]interface{}{
					"bag_found":          true,
					"has_token":          false,
					"service_acc_exists": true,
					"fcm_response_raw":   "no token",
				},
			}},
		})
		return
	}

	// Send FCM notification
	sentCount := 0
	fcmErr := services.SendFCMNotificationV1(fcmToken, "สีกระเป๋า", "คุณนินอัพเดทสีกระเป๋าให้คุณแล้ว กดเข้าดูได้เลย", map[string]string{"type": "bag_color"})
	if fcmErr == nil {
		sentCount = 1
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"status":     "completed",
		"sent_count": sentCount,
		"details": []map[string]interface{}{{
			"debug": map[string]interface{}{
				"bag_found":          true,
				"has_token":          fcmToken != "",
				"service_acc_exists": true,
				"fcm_response_raw":   fmt.Sprintf("sent:%d", sentCount),
			},
		}},
	})
}

// Helper functions
func getStr(m map[string]interface{}, key, fallback string) string {
	if v, ok := m[key]; ok {
		if s, ok := v.(string); ok && s != "" {
			return s
		}
	}
	return fallback
}

func boolStr(b bool) string {
	if b {
		return "true"
	}
	return "false"
}

// --- Admin User Management ---

type AdminUser struct {
	MemberID string `json:"memberid"`
	Username string `json:"username"`
	RealName string `json:"realname"`
	Surname  string `json:"surname"`
	VipCode  string `json:"vipcode"`
}

type AdminUserListResponse struct {
	Status string      `json:"status"`
	Users  []AdminUser `json:"users"`
}

func ListUsersHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	search := r.URL.Query().Get("search")
	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": "Database error"})
		return
	}

	var rows *sql.Rows
	var err error

	queryBase := `SELECT memberid, username, COALESCE(realname,''), COALESCE(surname,''), COALESCE(vipcode,'normal') FROM membertb`

	if search == "" {
		rows, err = mysqlDB.Query(queryBase + " ORDER BY memberid DESC LIMIT 50")
	} else {
		searchParam := "%" + search + "%"
		rows, err = mysqlDB.Query(queryBase+" WHERE username LIKE ? OR realname LIKE ? OR memberid LIKE ? OR surname LIKE ? ORDER BY memberid DESC LIMIT 50",
			searchParam, searchParam, searchParam, searchParam)
	}

	if err != nil {
		log.Printf("ListUsersHandler error: %v", err)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": err.Error()})
		return
	}
	defer rows.Close()

	users := []AdminUser{}
	for rows.Next() {
		var u AdminUser
		if err := rows.Scan(&u.MemberID, &u.Username, &u.RealName, &u.Surname, &u.VipCode); err != nil {
			log.Printf("ListUsersHandler scan error: %v", err)
			continue
		}
		users = append(users, u)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(AdminUserListResponse{
		Status: "success",
		Users:  users,
	})
}

func UpdateUserStatusHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var body map[string]string
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	memberID := body["memberid"]
	vipCode := body["vipcode"]

	if memberID == "" {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": "Missing memberid"})
		return
	}

	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": "Database error"})
		return
	}

	_, err := mysqlDB.Exec("UPDATE membertb SET vipcode = ? WHERE memberid = ?", vipCode, memberID)
	if err != nil {
		log.Printf("UpdateUserStatusHandler error: %v", err)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": err.Error()})
		return
	}

	// 🆕 Send Push Notification to user immediately
	go func() {
		var fcmToken string
		err := mysqlDB.QueryRow("SELECT COALESCE(fcm_token,'') FROM membertb WHERE memberid = ?", memberID).Scan(&fcmToken)
		if err == nil && fcmToken != "" {
			title := "อัพเดทสถานะสมาชิก"
			body := "คุณนินได้ทำการปรับปรุงสถานะสมาชิกของคุณแล้วในขณะนี้"
			data := map[string]string{
				"type":    "vip_update",
				"vipcode": strings.ToLower(vipCode),
			}
			services.SendFCMNotificationV1(fcmToken, title, body, data)
		}
	}()

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"status": "success", "message": "อัพเดทสถานะเรียบร้อย"})
}

func EditUserHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var body map[string]string
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	memberID := body["memberid"]
	realName := body["realname"]
	surName := body["surname"]
	birthDay := body["birthday"]

	if memberID == "" {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": "Missing memberid"})
		return
	}

	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": "Database error"})
		return
	}

	_, err := mysqlDB.Exec("UPDATE membertb SET realname = ?, surname = ?, birthday = ? WHERE memberid = ?", realName, surName, birthDay, memberID)
	if err != nil {
		log.Printf("EditUserHandler error: %v", err)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": err.Error()})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"status": "success", "message": "แก้ไขข้อมูลเรียบร้อย"})
}

func DeleteMemberHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var body map[string]string
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	memberID := body["memberid"]
	if memberID == "" {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": "Missing memberid"})
		return
	}

	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": "Database error"})
		return
	}

	_, err := mysqlDB.Exec("DELETE FROM membertb WHERE memberid = ?", memberID)
	if err != nil {
		log.Printf("DeleteMemberHandler error: %v", err)
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "error", "message": err.Error()})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"status": "success", "message": "ลบข้อมูลเรียบร้อย"})
}
