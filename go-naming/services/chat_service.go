package services

import (
	"database/sql"
	"fmt"
	"go-naming/database"
	"go-naming/models"
	"log"
	"strconv"
	"strings"
	"sync"
	"time"
)

// ChatHub manages active long-polling connections
type ChatHub struct {
	sync.RWMutex
	// channels for customers: session_id -> channel of messages
	customerPool map[string]chan models.ChatMessage
	// channels for all active admin pollers
	adminSubs []chan models.ChatMessage
}

var Hub = &ChatHub{
	customerPool: make(map[string]chan models.ChatMessage),
	adminSubs:    []chan models.ChatMessage{},
}

var (
	vipCache      = make(map[int]string)
	vipCacheTime  = make(map[int]time.Time)
	vipCacheMutex sync.RWMutex
)

func getUserVipStatus(userID int) string {
	if userID <= 0 {
		return "guest"
	}

	// 1. Check Cache
	vipCacheMutex.RLock()
	cached, ok := vipCache[userID]
	cacheTime := vipCacheTime[userID]
	vipCacheMutex.RUnlock()

	if ok && time.Since(cacheTime) < 30*time.Second { // 30 seconds for faster DB propagation
		return cached
	}

	// 2. Query MySQL
	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		log.Printf("ERROR: MySQL DB connection is NIL in getUserVipStatus")
		return "normal"
	}

	var rawVip sql.NullString
	// Using membertb as specified by user
	err := mysqlDB.QueryRow("SELECT vipcode FROM membertb WHERE memberid = ?", userID).Scan(&rawVip)

	vipStatus := "normal"
	if err != nil {
		log.Printf("DEBUG: getUserVipStatus(%d) query error: %v", userID, err)
	} else if rawVip.Valid {
		rawVal := strings.ToLower(strings.TrimSpace(rawVip.String))
		if rawVal == "vvip" {
			vipStatus = "VVIP"
		} else if rawVal == "mvp" {
			vipStatus = "MVP"
		} else if rawVal == "vip" || rawVal == "silver" || rawVal == "gold" || rawVal == "diamond" {
			vipStatus = "VIP"
		} else if rawVal == "admin" {
			vipStatus = "admin"
		} else if rawVal != "" && rawVal != "normal" {
			vipStatus = "VIP" // Default any non-normal status to VIP
		} else {
			vipStatus = "normal"
		}
		log.Printf("DEBUG: getUserVipStatus(ID: %d) -> Raw: '%s' -> Mapped: '%s'", userID, rawVip.String, vipStatus)
	} else {
		log.Printf("DEBUG: getUserVipStatus(ID: %d) -> NULL value in DB", userID)
	}

	// 3. Update Cache
	vipCacheMutex.Lock()
	vipCache[userID] = vipStatus
	vipCacheTime[userID] = time.Now()
	vipCacheMutex.Unlock()

	return vipStatus
}

// CheckDreamChatAccess verifies if a user has paid for dream interpretation access
// returns: hasAccess bool, message string, expiryTime time.Time
func CheckDreamChatAccess(memberID string, guestID string) (bool, string, time.Time) {
	var query string
	var args []interface{}

	if memberID != "" {
		uid, _ := strconv.Atoi(memberID)
		query = "SELECT product_id, updated_at FROM shop_orders WHERE user_id = $1 AND status = 'paid' AND product_id IN (1, 2, 3, 4) ORDER BY updated_at DESC"
		args = append(args, uid)
	} else if guestID != "" {
		query = "SELECT product_id, updated_at FROM shop_orders WHERE guest_id = $1 AND status = 'paid' AND product_id IN (1, 2, 3, 4) ORDER BY updated_at DESC"
		args = append(args, guestID)
	} else {
		return false, "กรุณาเข้าสู่ระบบเพื่อใช้งาน", time.Time{}
	}

	rows, err := database.DB.Query(query, args...)
	if err != nil {
		log.Printf("ERROR: CheckDreamChatAccess query failed: %v", err)
		return false, "เกิดข้อผิดพลาดในการตรวจสอบสิทธิ์", time.Time{}
	}
	defer rows.Close()

	var latestExpiredAt time.Time
	var hadPaidOrder bool

	for rows.Next() {
		var productID int
		var updatedAt time.Time
		if err := rows.Scan(&productID, &updatedAt); err != nil {
			continue
		}
		hadPaidOrder = true

		switch productID {
		case 1: // 1 Month
			expiry := updatedAt.AddDate(0, 1, 0)
			if time.Now().Before(expiry) {
				return true, "คุณมีสิทธิ์ใช้งานแชททำนายฝัน (1 เดือน)", expiry
			}
			if expiry.After(latestExpiredAt) {
				latestExpiredAt = expiry
			}
		case 2: // 1 Year
			expiry := updatedAt.AddDate(1, 0, 0)
			if time.Now().Before(expiry) {
				return true, "คุณมีสิทธิ์ใช้งานแชททำนายฝัน (1 ปี)", expiry
			}
			if expiry.After(latestExpiredAt) {
				latestExpiredAt = expiry
			}
		case 3: // Lifetime
			return true, "คุณมีสิทธิ์ใช้งานแชททำนายฝัน (ไม่จำกัดเวลา)", time.Time{}
		case 4: // Test: 1 Minute
			expiry := updatedAt.Add(1 * time.Minute)
			if time.Now().Before(expiry) {
				return true, "คุณมีสิทธิ์ใช้งานแชททำนายฝัน (ทดสอบระบบ 1 นาที)", expiry
			}
			if expiry.After(latestExpiredAt) {
				latestExpiredAt = expiry
			}
		}
	}

	if hadPaidOrder && !latestExpiredAt.IsZero() {
		return false, fmt.Sprintf("สิทธิ์ทำนายฝันของคุณหมดอายุเมื่อ %s", latestExpiredAt.Format("02/01/2006 15:04")), latestExpiredAt
	}

	return false, "ไม่มีสิทธิ์เข้าถึง หรือสิทธิ์การใช้งานหมดอายุแล้ว", time.Time{}
}

func RunMigrations() {
	_, err := database.DB.Exec("ALTER TABLE chat_sessions ADD COLUMN IF NOT EXISTS fcm_token TEXT")
	if err != nil {
		fmt.Printf("Migration Error (chat_sessions fcm): %v\n", err)
	}
	_, err = database.DB.Exec("ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS image_url TEXT")
	if err != nil {
		fmt.Printf("Migration Error (chat_messages): %v\n", err)
	}
}

// InitSession ensures a chat session exists
func InitSession(guestName string, userID int, clientSessionID string, deviceID string, fcmToken string) (string, error) {
	var sessionID string

	// Ensure migration runs (idempotent)
	RunMigrations()

	// 1. For Members: Session ID is User ID
	if userID > 0 {
		sessionID = fmt.Sprintf("u%d", userID)

		var realUsername string
		err := database.DB.QueryRow("SELECT username FROM member WHERE id = $1", userID).Scan(&realUsername)
		if err == nil && realUsername != "" {
			guestName = realUsername
		}

		// 🛡️ CLEANUP LOGIC: If this device had a guest session, DELETE IT (Requirement: Guest must not interact with Member)
		var guestSessToCleanup string
		if clientSessionID != "" && clientSessionID != sessionID && len(clientSessionID) > 0 && clientSessionID[0] == 'g' {
			guestSessToCleanup = clientSessionID
		} else if deviceID != "" {
			// Find guest session for this device
			_ = database.DB.QueryRow("SELECT session_id FROM chat_sessions WHERE device_id = $1 AND session_id LIKE 'g%' ORDER BY last_message_at DESC LIMIT 1", deviceID).Scan(&guestSessToCleanup)
		}

		// Create/Update Member Session first (to satisfy FK)
		query := `
			INSERT INTO chat_sessions (session_id, guest_name, user_id, last_message_at, device_id, fcm_token) 
			VALUES ($1, $2, $3, NOW(), $4, $5)
			ON CONFLICT (session_id) DO UPDATE 
			SET guest_name = $2, last_message_at = NOW(), device_id = $4, fcm_token = $5
		`
		_, err = database.DB.Exec(query, sessionID, guestName, userID, deviceID, fcmToken)

		// Perform Cleanup if guest session found
		if guestSessToCleanup != "" && guestSessToCleanup != sessionID {
			fmt.Printf("DEBUG: Cleaning up Guest Session %s (User Logged In as %s)\n", guestSessToCleanup, sessionID)
			// Delete messages for guest
			_, _ = database.DB.Exec("DELETE FROM chat_messages WHERE session_id = $1", guestSessToCleanup)
			// Delete guest session record
			_, _ = database.DB.Exec("DELETE FROM chat_sessions WHERE session_id = $1", guestSessToCleanup)
		}

		return sessionID, err
	}

	// 2. Client sent explicit Session ID (Resume or Optimistic New)
	if clientSessionID != "" {
		// A. Check if it already exists (Resume)
		var exists bool
		err := database.DB.QueryRow("SELECT EXISTS(SELECT 1 FROM chat_sessions WHERE session_id = $1)", clientSessionID).Scan(&exists)
		if err == nil && exists {
			fmt.Printf("DEBUG: InitSession - Resuming by ClientID: %s\n", clientSessionID)
			_, err = database.DB.Exec("UPDATE chat_sessions SET guest_name = $1, last_message_at = NOW(), device_id = $3, fcm_token = $4 WHERE session_id = $2", guestName, clientSessionID, deviceID, fcmToken)
			return clientSessionID, err
		}

		// B. New Optimistic Guest Session (starts with 'g') -> Trust Client
		if clientSessionID[0] == 'g' {
			sessionID = clientSessionID
			fmt.Printf("DEBUG: InitSession - Accepting New Optimistic Guest Session: %s\n", sessionID)
			query := `
				INSERT INTO chat_sessions (session_id, guest_name, user_id, device_id, fcm_token) 
				VALUES ($1, $2, NULL, $3, $4)
				ON CONFLICT (session_id) DO UPDATE SET last_message_at = NOW(), device_id = $3, fcm_token = $4
			`
			_, err := database.DB.Exec(query, sessionID, guestName, deviceID, fcmToken)
			return sessionID, err
		}
	}

	// 3. Fallback: Lookup by Name + Device ID
	// If the user clears app data, they lose sessionId, but we can match DeviceID
	// 3. Fallback: Lookup by Device ID
	// 🛡️ SECURITY: If Guest (userID 0), only allow resuming GUEST sessions (starting with 'g')
	// If User (userID > 0), they already handled in step 1, but we keep this robust.
	if deviceID != "" {
		var existingSessionID string
		var query string
		if userID == 0 {
			// Guest: ONLY look for 'g%' sessions
			query = "SELECT session_id FROM chat_sessions WHERE device_id = $1 AND session_id LIKE 'g%' ORDER BY last_message_at DESC LIMIT 1"
		} else {
			// Member: Look for any session (though step 1 usually catches them)
			query = "SELECT session_id FROM chat_sessions WHERE device_id = $1 ORDER BY last_message_at DESC LIMIT 1"
		}

		err := database.DB.QueryRow(query, deviceID).Scan(&existingSessionID)
		if err == nil && existingSessionID != "" {
			fmt.Printf("DEBUG: Resuming session by DeviceID %s -> %s (IsGuestReq: %v)\n", deviceID, existingSessionID, userID == 0)
			// Update the name if they provided a new one (e.g. they typed a name this time)
			if guestName != "Guest" && guestName != "" {
				_, _ = database.DB.Exec("UPDATE chat_sessions SET last_message_at = NOW(), guest_name = $1, fcm_token = $3 WHERE session_id = $2", guestName, existingSessionID, fcmToken)
			} else {
				_, _ = database.DB.Exec("UPDATE chat_sessions SET last_message_at = NOW(), fcm_token = $2 WHERE session_id = $1", existingSessionID, fcmToken)
			}
			return existingSessionID, nil
		}
	}

	// 4. Fallback: Lookup by Name ONLY (if no Device ID or not found)
	// ONLY if Guest (userID 0), only allow 'g%' sessions.
	if guestName != "" && guestName != "Guest" && userID == 0 {
		var existingSessionID string
		err := database.DB.QueryRow("SELECT session_id FROM chat_sessions WHERE guest_name = $1 AND session_id LIKE 'g%' ORDER BY last_message_at DESC LIMIT 1", guestName).Scan(&existingSessionID)
		if err == nil && existingSessionID != "" {
			fmt.Printf("DEBUG: Resuming session by Name %s -> %s\n", guestName, existingSessionID)
			_, _ = database.DB.Exec("UPDATE chat_sessions SET last_message_at = NOW(), device_id = $2, fcm_token = $3 WHERE session_id = $1", existingSessionID, deviceID, fcmToken)
			return existingSessionID, nil
		}
	}

	// 5. Default: Create Random Session (if no ID provided and no fallback match)
	sessionID = fmt.Sprintf("g%d", time.Now().UnixNano())
	fmt.Printf("DEBUG: InitSession - Creating New Generated Guest Session: %s\n", sessionID)

	query := `
		INSERT INTO chat_sessions (session_id, guest_name, user_id, device_id, fcm_token) 
		VALUES ($1, $2, NULL, $3, $4)
		ON CONFLICT (session_id) DO UPDATE SET last_message_at = NOW(), device_id = $3, fcm_token = $4
	`
	_, err := database.DB.Exec(query, sessionID, guestName, deviceID, fcmToken)
	if err != nil {
		fmt.Printf("ERROR: InitSession (Guest) Failed: %v\n", err)
	}
	return sessionID, err
}

// SendMessage saves message to DB and broadcasts
func SendMessage(sessionID string, senderType string, text string, imageURL string) (models.ChatMessage, error) {
	fmt.Printf("DEBUG: SendMessage called - Session: %s, Sender: %s, Text: %s, Image: %s\n", sessionID, senderType, text, imageURL)

	var senderName string
	var guestName string
	err := database.DB.QueryRow("SELECT guest_name FROM chat_sessions WHERE session_id = $1", sessionID).Scan(&guestName)
	if err != nil {
		fmt.Printf("DEBUG: SendMessage - Session Query Error: %v\n", err)
	}

	if err != nil {
		// ⚠️ Session Missing (Deleted by Admin?) -> Auto-Restore
		// This prevents Foreign Key Violation (500 Error)
		fmt.Printf("DEBUG: Session %s missing. Auto-restoring...\n", sessionID)

		if len(sessionID) > 1 && sessionID[0] == 'u' {
			// Restore Member Session
			uidStr := sessionID[1:]
			uid, _ := strconv.Atoi(uidStr)

			var realName string
			_ = database.DB.QueryRow("SELECT username FROM member WHERE id = $1", uid).Scan(&realName)
			if realName == "" {
				guestName = fmt.Sprintf("Member %d", uid)
			} else {
				guestName = realName
			}

			_, err = database.DB.Exec("INSERT INTO chat_sessions (session_id, guest_name, user_id, last_message_at) VALUES ($1, $2, $3, NOW()) ON CONFLICT (session_id) DO UPDATE SET last_message_at = NOW()", sessionID, guestName, uid)
		} else {
			// Restore Guest Session
			// Try to use SessionID as name if it's numeric/short, otherwise just Guest
			if len(sessionID) > 5 {
				guestName = fmt.Sprintf("Guest (%s...)", sessionID[:5])
			} else {
				guestName = "Guest " + sessionID
			}
			_, err = database.DB.Exec("INSERT INTO chat_sessions (session_id, guest_name, last_message_at) VALUES ($1, $2, NOW()) ON CONFLICT (session_id) DO UPDATE SET last_message_at = NOW()", sessionID, guestName)
		}

		if err != nil {
			fmt.Printf("ERROR: Failed to restore session %s: %v\n", sessionID, err)
			return models.ChatMessage{}, fmt.Errorf("failed to restore session: %v", err)
		}
	}

	if senderType == "admin" {
		if guestName == "Guest" || guestName == "" {
			shortID := sessionID
			if len(sessionID) > 5 {
				shortID = sessionID[:5] + "..."
			}
			senderName = fmt.Sprintf("คุณนิน [ตอบกลับ Guest (ID: %s)]", shortID)
		} else {
			senderName = fmt.Sprintf("คุณนิน [ตอบกลับ %s]", guestName)
		}
	} else {
		if guestName == "Guest" || guestName == "" {
			shortID := sessionID
			if len(sessionID) > 5 {
				shortID = sessionID[:5] + "..."
			}
			senderName = shortID
		} else {
			senderName = guestName
		}
	}

	var senderAvatar string
	if senderType == "admin" {
		senderAvatar = "admin" // special ID or path for admin avatar
	} else if len(sessionID) > 1 && sessionID[0] == 'u' {
		uidStr := sessionID[1:]
		uid, _ := strconv.Atoi(uidStr)
		_ = database.DB.QueryRow("SELECT COALESCE(avatar_url, '1') FROM member WHERE id = $1", uid).Scan(&senderAvatar)
	}
	if senderAvatar == "" {
		senderAvatar = "1" // Default avatar
	}

	var vipStatus string
	if len(sessionID) > 1 && sessionID[0] == 'u' {
		uid, _ := strconv.Atoi(sessionID[1:])
		vipStatus = getUserVipStatus(uid)
	}

	msg := models.ChatMessage{
		SessionID:    sessionID,
		SenderType:   senderType,
		SenderName:   senderName,
		SenderAvatar: senderAvatar,
		Message:      text,
		ImageURL:     imageURL,
		IsRead:       false,
		VipStatus:    vipStatus,
		CreatedAt:    time.Now(),
	}

	query := `
		INSERT INTO chat_messages (session_id, sender_type, message_text, image_url)
		VALUES ($1, $2, $3, $4)
		RETURNING message_id, created_at
	`
	err = database.DB.QueryRow(query, sessionID, senderType, text, imageURL).Scan(&msg.MessageID, &msg.CreatedAt)
	if err != nil {
		fmt.Printf("ERROR: Failed to insert message: %v\n", err)
		return msg, err
	}
	fmt.Printf("DEBUG: SendMessage Success - ID: %d, Session: %s\n", msg.MessageID, sessionID)

	// Broadcast to Admin and Customer
	Hub.Broadcast(msg)
	return msg, nil
}

// Broadcast sends message to all relevant pollers
func (h *ChatHub) Broadcast(msg models.ChatMessage) {
	h.Lock()
	defer h.Unlock()

	// 1. Send to specific customer if polling
	if ch, ok := h.customerPool[msg.SessionID]; ok {
		select {
		case ch <- msg:
		default:
		}
	}

	// 2. Send to all admins
	for _, ch := range h.adminSubs {
		select {
		case ch <- msg:
		default:
		}
	}
}

// SubscribeCustomer starts long polling for a session
func SubscribeCustomer(sessionID string, timeout time.Duration) ([]models.ChatMessage, error) {
	ch := make(chan models.ChatMessage, 1)
	Hub.Lock()
	Hub.customerPool[sessionID] = ch
	Hub.Unlock()

	defer func() {
		Hub.Lock()
		delete(Hub.customerPool, sessionID)
		Hub.Unlock()
	}()

	select {
	case msg := <-ch:
		return []models.ChatMessage{msg}, nil
	case <-time.After(timeout):
		return []models.ChatMessage{}, nil
	}
}

// SubscribeAdmin starts long polling for all messages
func SubscribeAdmin(timeout time.Duration) ([]models.ChatMessage, error) {
	ch := make(chan models.ChatMessage, 1)
	Hub.Lock()
	Hub.adminSubs = append(Hub.adminSubs, ch)
	Hub.Unlock()

	defer func() {
		Hub.Lock()
		for i, sub := range Hub.adminSubs {
			if sub == ch {
				Hub.adminSubs = append(Hub.adminSubs[:i], Hub.adminSubs[i+1:]...)
				break
			}
		}
		Hub.Unlock()
	}()

	select {
	case msg := <-ch:
		return []models.ChatMessage{msg}, nil
	case <-time.After(timeout):
		return []models.ChatMessage{}, nil
	}
}

// GetChatHistory returns last N messages for a session
func GetChatHistory(sessionID string, limit int) ([]models.ChatMessage, error) {
	query := `
		SELECT 
			m.message_id, 
			m.session_id, 
			m.sender_type, 
			CASE 
				WHEN m.sender_type = 'admin' THEN 
					'คุณนิน [ตอบกลับ ' || 
					CASE 
						WHEN s.guest_name IS NULL OR s.guest_name = '' OR s.guest_name = 'Guest' THEN 'Guest (ID: ' || LEFT(m.session_id, 5) || '...)'
						ELSE s.guest_name
					END || ']'
				ELSE 
					CASE 
						WHEN s.guest_name IS NULL OR s.guest_name = '' OR s.guest_name = 'Guest' THEN LEFT(m.session_id, 5) || '...'
						ELSE s.guest_name
					END
			END as sender_name,
			CASE 
				WHEN m.sender_type = 'admin' THEN 'admin'
				WHEN m.session_id LIKE 'u%' THEN COALESCE((SELECT COALESCE(avatar_url, '1') FROM member WHERE id = CAST(SUBSTRING(m.session_id, 2) AS INTEGER)), '1')
				ELSE '1'
			END as sender_avatar,
			m.message_text, 
			COALESCE(m.image_url, '') as image_url,
			m.is_read, 
			m.created_at
		FROM chat_messages m
		LEFT JOIN chat_sessions s ON m.session_id = s.session_id
		WHERE m.session_id = $1
		ORDER BY m.created_at DESC
		LIMIT $2
	`
	rows, err := database.DB.Query(query, sessionID, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var msgs []models.ChatMessage
	for rows.Next() {
		var m models.ChatMessage
		if err := rows.Scan(&m.MessageID, &m.SessionID, &m.SenderType, &m.SenderName, &m.SenderAvatar, &m.Message, &m.ImageURL, &m.IsRead, &m.CreatedAt); err == nil {
			if len(m.SessionID) > 1 && m.SessionID[0] == 'u' {
				uid, _ := strconv.Atoi(m.SessionID[1:])
				m.VipStatus = getUserVipStatus(uid)
			}
			msgs = append(msgs, m)
		}
	}
	// Reverse to get chronological order
	for i, j := 0, len(msgs)-1; i < j; i, j = i+1, j-1 {
		msgs[i], msgs[j] = msgs[j], msgs[i]
	}
	return msgs, nil
}

// GetRecentGlobalMessages returns last N messages from ALL sessions for admin console
func GetRecentGlobalMessages(limit int, offset int) ([]models.ChatMessage, error) {
	query := `
		WITH LatestMessages AS (
			SELECT 
				m.message_id, 
				m.session_id, 
				m.sender_type,
				m.message_text,
				m.image_url,
				m.is_read,
				m.created_at,
				ROW_NUMBER() OVER (PARTITION BY m.session_id ORDER BY m.created_at DESC) as rn
			FROM chat_messages m
		)
		SELECT 
			lm.message_id, 
			lm.session_id, 
			lm.sender_type, 
			CASE 
				WHEN lm.sender_type = 'admin' THEN 
					'คุณนิน [ตอบกลับ ' || 
					CASE 
						WHEN s.guest_name IS NOT NULL AND s.guest_name != '' AND s.guest_name != 'Guest' THEN s.guest_name
						WHEN lm.session_id LIKE 'u%' THEN 'Member ' || SUBSTRING(lm.session_id, 2)
						ELSE 'Guest (ID: ' || LEFT(lm.session_id, 5) || '...)'
					END || ']'
				ELSE 
					CASE 
						WHEN s.guest_name IS NOT NULL AND s.guest_name != '' AND s.guest_name != 'Guest' THEN s.guest_name
						WHEN lm.session_id LIKE 'u%' THEN 'Member ' || SUBSTRING(lm.session_id, 2)
						ELSE LEFT(lm.session_id, 5) || '...'
					END
			END as sender_name,
			CASE 
				WHEN lm.sender_type = 'admin' THEN 'admin'
				WHEN lm.session_id LIKE 'u%' THEN COALESCE((SELECT COALESCE(avatar_url, '1') FROM member WHERE id = CAST(SUBSTRING(lm.session_id, 2) AS INTEGER)), '1')
				ELSE '1'
			END as sender_avatar,
			lm.message_text, 
			COALESCE(lm.image_url, '') as image_url,
			lm.is_read, 
			lm.created_at
		FROM LatestMessages lm
		LEFT JOIN chat_sessions s ON lm.session_id = s.session_id
		WHERE lm.rn = 1
		ORDER BY lm.created_at DESC
		LIMIT $1 OFFSET $2
	`
	rows, err := database.DB.Query(query, limit, offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var msgs []models.ChatMessage
	for rows.Next() {
		var m models.ChatMessage
		if err := rows.Scan(&m.MessageID, &m.SessionID, &m.SenderType, &m.SenderName, &m.SenderAvatar, &m.Message, &m.ImageURL, &m.IsRead, &m.CreatedAt); err == nil {
			if len(m.SessionID) > 1 && m.SessionID[0] == 'u' {
				uid, _ := strconv.Atoi(m.SessionID[1:])
				m.VipStatus = getUserVipStatus(uid)
			}
			msgs = append(msgs, m)
		}
	}
	// Reverse to get chronological order (oldest first for display)
	for i, j := 0, len(msgs)-1; i < j; i, j = i+1, j-1 {
		msgs[i], msgs[j] = msgs[j], msgs[i]
	}

	for _, m := range msgs {
		if m.SessionID == "u812" {
			log.Printf("DEBUG: Admin response for u812 - Name: %s, VIP: %s", m.SenderName, m.VipStatus)
		}
	}
	return msgs, nil
}

// DeleteMessage removes a message from the database
func DeleteMessage(messageID int) error {
	tx, err := database.DB.Begin()
	if err != nil {
		return err
	}
	defer func() {
		_ = tx.Rollback()
	}()

	var sessionID string
	err = tx.QueryRow("SELECT session_id FROM chat_messages WHERE message_id = $1", messageID).Scan(&sessionID)
	if err != nil {
		return fmt.Errorf("message not found or already deleted")
	}

	res, err := tx.Exec("DELETE FROM chat_messages WHERE message_id = $1", messageID)
	if err != nil {
		return err
	}
	rows, _ := res.RowsAffected()
	if rows == 0 {
		return fmt.Errorf("message not found or already deleted")
	}

	var remaining int
	err = tx.QueryRow("SELECT COUNT(*) FROM chat_messages WHERE session_id = $1", sessionID).Scan(&remaining)
	if err != nil {
		return err
	}
	if remaining == 0 {
		if _, err := tx.Exec("DELETE FROM chat_sessions WHERE session_id = $1", sessionID); err != nil {
			return err
		}
	}

	if err := tx.Commit(); err != nil {
		return err
	}
	return nil
}

// GetUnreadCount returns the number of unread admin messages for a member ID
func GetUnreadCount(userID int) (int, error) {
	sessionID := fmt.Sprintf("u%d", userID)
	query := `
		SELECT COUNT(*) 
		FROM chat_messages 
		WHERE session_id = $1 
		AND sender_type = 'admin' 
		AND is_read = false
	`
	var count int
	err := database.DB.QueryRow(query, sessionID).Scan(&count)
	return count, err
}

// MarkMessagesRead sets all admin messages in a session to read (Used by Customer)
func MarkMessagesRead(sessionID string) error {
	query := `
		UPDATE chat_messages 
		SET is_read = true 
		WHERE session_id = $1 
		AND sender_type = 'admin' 
		AND is_read = false
	`
	res, err := database.DB.Exec(query, sessionID)
	if err != nil {
		return err
	}

	affected, _ := res.RowsAffected()
	if affected > 0 {
		// Broadcast the update so Admin UI sees it immediately
		// We fetch the latest admin message to send as "signal"
		// The Admin UI (if fixed) should update this message's status
		hist, _ := GetChatHistory(sessionID, 1) // Reuse existing function
		if len(hist) > 0 {
			lastMsg := hist[0]
			lastMsg.IsRead = true // Force true just in case DB lag/replica
			Hub.Broadcast(lastMsg)
		}
	}

	return nil
}

// MarkCustomerMessagesRead sets all customer messages in a session to read (Used by Admin)
func MarkCustomerMessagesRead(sessionID string) error {
	query := `
		UPDATE chat_messages 
		SET is_read = true 
		WHERE session_id = $1 
		AND sender_type != 'admin' 
		AND is_read = false
	`
	_, err := database.DB.Exec(query, sessionID)
	return err
}
