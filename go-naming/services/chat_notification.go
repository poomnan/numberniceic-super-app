package services

import (
	"database/sql"
	"fmt"
	"go-naming/database"
	"log"
)

// SendChatNotification ส่ง Firebase notification เมื่อ admin ส่งข้อความให้ user
func SendChatNotification(sessionID, message string) {
	// 1. ดึง user_id และ fcm_token จาก session
	var userID sql.NullInt64
	var fcmToken string
	err := database.DB.QueryRow(`
		SELECT user_id, fcm_token
		FROM chat_sessions 
		WHERE session_id = $1
	`, sessionID).Scan(&userID, &fcmToken)

	if err != nil {
		if err == sql.ErrNoRows {
			log.Printf("Session not found: %s", sessionID)
			return
		}
		log.Printf("Error fetching session: %v", err)
		return
	}

	// 2. ถ้าไม่มี fcmToken ใน session ให้ลองหาจากตาราง member (กรณีเป็นสมาชิก)
	if fcmToken == "" && userID.Valid {
		err = database.DB.QueryRow(`
			SELECT fcm_token 
			FROM member 
			WHERE id = $1
		`, userID.Int64).Scan(&fcmToken)
		if err != nil {
			log.Printf("User member token not found for UID %d: %v", userID.Int64, err)
		}
	}

	if fcmToken == "" {
		log.Printf("No FCM token available for session: %s", sessionID)
		return
	}

	// 3. ส่ง Firebase notification
	title := "ข้อความจากคุณนิน"
	body := message
	if len(message) > 50 {
		body = message[:50] + "..."
	}

	// สร้าง payload สำหรับ Firebase
	var uidStr string
	if userID.Valid {
		uidStr = fmt.Sprintf("%d", userID.Int64)
	} else {
		uidStr = "0"
	}

	data := map[string]string{
		"type":       "admin_message",
		"memberid":   uidStr,
		"title":      title,
		"body":       message,
		"session_id": sessionID,
	}

	// ส่งผ่าน FCM V1 API
	err = SendFCMNotificationV1(fcmToken, title, body, data)
	if err != nil {
		log.Printf("Error sending FCM notification: %v", err)
		return
	}

	log.Printf("✅ Sent chat notification to session %s (UserID: %s)", sessionID, uidStr)
}

// NotifyAdminsOfCustomerMessage ส่ง Firebase notification ให้ admin เมื่อมีลูกค้าส่งข้อความ
func NotifyAdminsOfCustomerMessage(sessionID, senderName, message string) {
	// 1. หา FCM tokens ของ admin ทั้งหมด (ID 810, 832) จากตาราง chat_sessions
	rows, err := database.DB.Query(`
		SELECT fcm_token 
		FROM chat_sessions 
		WHERE user_id IN (810, 832) AND fcm_token IS NOT NULL AND fcm_token != ''
	`)
	if err != nil {
		log.Printf("Error fetching admin tokens: %v", err)
		return
	}
	defer rows.Close()

	var tokens []string
	for rows.Next() {
		var t string
		if err := rows.Scan(&t); err == nil {
			tokens = append(tokens, t)
		}
	}

	if len(tokens) == 0 {
		log.Printf("No admin FCM tokens found.")
		return
	}

	title := fmt.Sprintf("แชทใหม่จาก %s", senderName)
	body := message
	if len(message) > 50 {
		body = message[:50] + "..."
	}

	data := map[string]string{
		"type":       "customer_message",
		"title":      title,
		"body":       message,
		"session_id": sessionID,
		"sender":     senderName,
	}

	// 2. ส่งถึง admin ทุกคน
	for _, token := range tokens {
		err = SendFCMNotificationV1(token, title, body, data)
		if err != nil {
			log.Printf("Error sending notification to admin: %v", err)
		} else {
			log.Printf("✅ Sent chat notification to admin token: %s...", token[:10])
		}
	}
}

// SendCallNotification sends a "Call Invitation" notification via FCM to the CUSTOMER of the session
func SendCallNotification(sessionID, callerName, channelName string) {
	// 1. Fetch target session info (The Customer)
	var userID sql.NullInt64
	var fcmToken string
	err := database.DB.QueryRow(`
		SELECT user_id, fcm_token
		FROM chat_sessions 
		WHERE session_id = $1
	`, sessionID).Scan(&userID, &fcmToken)

	if err != nil || fcmToken == "" {
		log.Printf("Call Signaling (Customer) Failed: Target token not found for session %s", sessionID)
		return
	}

	// 2. Prepare Call Invitation Data
	title := "สายเรียกเข้าจาก " + callerName
	body := "กดยืนยันเพื่อรับสายผ่านแอป"

	data := map[string]string{
		"type":         "call_invite",
		"title":        title,
		"body":         body,
		"session_id":   sessionID,
		"caller_name":  callerName,
		"channel_name": channelName,
	}

	// 3. Send High Priority FCM
	err = SendFCMNotificationV1(fcmToken, title, body, data)
	if err != nil {
		log.Printf("Error sending Call Signaling FCM to Customer: %v", err)
	} else {
		log.Printf("✅ Call Signaling sent to Customer in session %s (Channel: %s)", sessionID, channelName)
	}
}

// NotifyAdminsOfCall sends a call invitation notification to all admin tokens (excluding the caller)
func NotifyAdminsOfCall(sessionID, callerName, callerID, channelName string) {
	log.Printf("🔔 NotifyAdminsOfCall called - Session: %s, Caller: %s, CallerID: %s", sessionID, callerName, callerID)

	// 1. Fetch Admin tokens
	rows, err := database.DB.Query(`
		SELECT fcm_token 
		FROM chat_sessions 
		WHERE user_id IN (810, 832) 
		  AND user_id::text != $1
		  AND fcm_token IS NOT NULL 
		  AND fcm_token != ''
	`, callerID)
	if err != nil {
		log.Printf("❌ Error fetching admin tokens for call: %v", err)
		return
	}
	defer rows.Close()

	var tokens []string
	for rows.Next() {
		var t string
		if err := rows.Scan(&t); err == nil {
			tokens = append(tokens, t)
			log.Printf("📱 Found admin token: %s...", t[:20])
		}
	}

	if len(tokens) == 0 {
		log.Printf("⚠️ No Admin tokens found for call signaling. CallerID excluded: %s", callerID)
		return
	}

	log.Printf("✅ Found %d admin token(s) for call notification", len(tokens))

	// 2. Prepare Data
	title := "สายเรียกเข้าจาก " + callerName
	body := "ลูกค้าต้องการโทรหาคุณ"

	data := map[string]string{
		"type":         "call_invite",
		"title":        title,
		"body":         body,
		"session_id":   sessionID,
		"caller_name":  callerName,
		"channel_name": channelName,
	}

	// 3. Send to all admins (Use Notification + Data for visibility)
	for _, token := range tokens {
		log.Printf("📤 Sending call notification to admin token: %s...", token[:20])
		err = SendFCMNotificationV1(token, title, body, data)

		if err != nil {
			log.Printf("❌ Error sending call signaling to admin: %v", err)
		} else {
			log.Printf("✅ Call Signaling sent to Admin token: %s...", token[:10])
		}
	}
}

// SendCallRejectNotification sends a rejection signal to the Caller
func SendCallRejectNotification(sessionID, rejecterName, role string) {
	// Determine who needs to receive the rejection
	// If Admin rejects, send to Customer (Session Owner)
	// If Customer rejects, send to Admin(s) - But usually only 1-on-1 for now.

	// For now, assume Admin rejected -> Send to Customer
	// Or simplistic approach: Send to the OTHER party in the session.

	// Fetch session details
	var userID sql.NullInt64
	var fcmToken string
	err := database.DB.QueryRow(`
		SELECT user_id, fcm_token
		FROM chat_sessions 
		WHERE session_id = $1
	`, sessionID).Scan(&userID, &fcmToken)

	if err != nil {
		log.Printf("Error fetching session for rejection: %v", err)
		return
	}

	// Logic: If Admin rejected, we need to notify the Customer (who started the call?)
	// Actually, session_id usually belongs to the customer.
	// So if Admin rejects, we send to fcmToken of the session.

	data := map[string]string{
		"type":       "call_reject",
		"session_id": sessionID,
	}

	if fcmToken != "" {
		SendFCMDataMessageV1(fcmToken, data)
		log.Printf("✅ Call Rejection sent to Customer token: %s...", fcmToken[:10])
	}

	// If Customer rejected, we need to notify Admins?
	// This part is tricky without knowing who initiates.
	// But usually, Admin initiates.
	// If Admin initiates, they are sending TO customer.
	// If Customer rejects, we send to Admin tokens.

	if role == "customer" {
		// Notify Admins
		NotifyAdminsOfRejection(sessionID)
	}
}

// NotifyAdminsOfRejection sends call rejection to admins
func NotifyAdminsOfRejection(sessionID string) {
	rows, err := database.DB.Query(`
		SELECT fcm_token 
		FROM chat_sessions 
		WHERE user_id IN (810, 832) AND fcm_token IS NOT NULL AND fcm_token != ''
	`)
	if err != nil {
		return
	}
	defer rows.Close()

	data := map[string]string{
		"type":       "call_reject",
		"session_id": sessionID,
	}

	for rows.Next() {
		var t string
		if err := rows.Scan(&t); err == nil {
			SendFCMDataMessageV1(t, data)
		}
	}
}

// SendOrderSuccessNotification sends a notification to the user after successful payment
func SendOrderSuccessNotification(refNo string) {
	log.Printf("🔔 Processing Order Success Notification for Ref: %s", refNo)

	// 1. Get User/Guest ID from order
	var userID sql.NullInt64
	var guestID sql.NullString
	err := database.DB.QueryRow(`
		SELECT user_id, guest_id
		FROM shop_orders
		WHERE ref_no = $1
	`, refNo).Scan(&userID, &guestID)

	if err != nil {
		log.Printf("❌ Error fetching order for notification (Ref=%s): %v", refNo, err)
		return
	}

	// 2. Find FCM Token from chat_sessions (most reliable source of active tokens)
	var fcmToken string
	var sessionID string

	if userID.Valid && userID.Int64 > 0 {
		// Member Case
		err = database.DB.QueryRow(`
			SELECT fcm_token, session_id
			FROM chat_sessions 
			WHERE user_id = $1 AND fcm_token IS NOT NULL AND fcm_token != ''
			ORDER BY last_message_at DESC LIMIT 1
		`, userID.Int64).Scan(&fcmToken, &sessionID)

		// Fallback to member table if not in chat_sessions
		if err != nil || fcmToken == "" {
			log.Printf("ℹ️ FCM not in session for UID %d, checking member table", userID.Int64)
			_ = database.DB.QueryRow(`SELECT fcm_token FROM member WHERE id = $1`, userID.Int64).Scan(&fcmToken)
			sessionID = fmt.Sprintf("u%d", userID.Int64)
		}
	} else if guestID.Valid && guestID.String != "" {
		// Guest Case: search by session_id OR device_id associated with that session
		err = database.DB.QueryRow(`
			WITH target_device AS (
				SELECT device_id FROM chat_sessions WHERE session_id = $1
			)
			SELECT fcm_token, session_id
			FROM chat_sessions 
			WHERE (session_id = $1 OR device_id = $1 OR (device_id IS NOT NULL AND device_id = (SELECT device_id FROM target_device WHERE device_id IS NOT NULL AND device_id != '')))
			  AND fcm_token IS NOT NULL AND fcm_token != ''
			ORDER BY last_message_at DESC LIMIT 1
		`, guestID.String).Scan(&fcmToken, &sessionID)
	}

	if fcmToken == "" {
		log.Printf("⚠️ No FCM token available for order notification (Ref=%s, UID=%v, GID=%v)",
			refNo, userID.Int64, guestID.String)
		return
	}

	// 3. Send Firebase notification
	title := "การสั่งซื้อสำเร็จแล้ว"
	body := "การสั่งซื้อของคุณเรียบร้อยแล้ว ขอบคุณที่ใช้บริการค่ะ"

	data := map[string]string{
		"type":       "order_success",
		"ref_no":     refNo,
		"title":      title,
		"body":       body,
		"session_id": sessionID,
	}

	// Send via FCM V1 API
	err = SendFCMNotificationV1(fcmToken, title, body, data)
	if err != nil {
		log.Printf("❌ Error sending Order Success notification (Ref=%s): %v", refNo, err)
		return
	}

	log.Printf("✅ Order Success notification sent to %s (Ref: %s)", sessionID, refNo)
}

// NotifyAdminsOfNewOrder sends a notification to all admin tokens when a new order is paid
func NotifyAdminsOfNewOrder(refNo string) {
	log.Printf("🔔 NotifyAdminsOfNewOrder called for Ref: %s", refNo)

	// 1. Get order details to show in notification
	var amount float64
	var productDetail string
	err := database.DB.QueryRow(`
		SELECT amount, product_detail
		FROM shop_orders
		WHERE ref_no = $1
	`, refNo).Scan(&amount, &productDetail)
	if err != nil {
		log.Printf("❌ Error fetching order details for admin notification: %v", err)
		return
	}

	// 2. Fetch Admin tokens
	rows, err := database.DB.Query(`
		SELECT fcm_token 
		FROM chat_sessions 
		WHERE user_id IN (810, 832) AND fcm_token IS NOT NULL AND fcm_token != ''
	`)
	if err != nil {
		log.Printf("❌ Error fetching admin tokens for new order: %v", err)
		return
	}
	defer rows.Close()

	var tokens []string
	for rows.Next() {
		var t string
		if err := rows.Scan(&t); err == nil {
			tokens = append(tokens, t)
		}
	}

	if len(tokens) == 0 {
		log.Printf("⚠️ No Admin tokens found for new order notification.")
		return
	}

	title := "มีรายการสั่งซื้อใหม่"
	body := fmt.Sprintf("รายการ: %s\nยอดเงิน: %.2f บาท (Ref: %s)", productDetail, amount, refNo)

	data := map[string]string{
		"type":           "new_order",
		"ref_no":         refNo,
		"title":          title,
		"body":           body,
		"amount":         fmt.Sprintf("%.2f", amount),
		"product_detail": productDetail,
	}

	// 3. Send to all admins
	for _, token := range tokens {
		err = SendFCMNotificationV1(token, title, body, data)
		if err != nil {
			log.Printf("❌ Error sending order notification to admin: %v", err)
		} else {
			log.Printf("✅ Sent order notification to admin token: %s...", token[:10])
		}
	}
}
