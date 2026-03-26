package handlers

import (
	"encoding/json"
	"fmt"
	"go-naming/database"
	"go-naming/services"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"time"
)

// InitChatHandler starts a new chat session
func InitChatHandler(w http.ResponseWriter, r *http.Request) {
	var body map[string]interface{}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		jsonResponse(w, http.StatusBadRequest, map[string]string{"error": "invalid json"})
		return
	}

	name, _ := body["name"].(string)
	if name == "" {
		name = "Guest"
	}

	var userID int
	if val, ok := body["user_id"]; ok {
		switch v := val.(type) {
		case float64:
			userID = int(v)
		case string:
			userID, _ = strconv.Atoi(v)
		}
	}

	clientSessionID, _ := body["session_id"].(string)
	deviceID, _ := body["device_id"].(string)
	fcmToken, _ := body["fcm_token"].(string)

	fmt.Printf("DEBUG: InitChat Final - Name: %s, UserID: %d, Sess: %s, Dev: %s, Token: %s\n", name, userID, clientSessionID, deviceID, fcmToken)

	sessionID, err := services.InitSession(name, userID, clientSessionID, deviceID, fcmToken)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]string{"session_id": sessionID})
}

// SendMessageHandler handles sending a message
func SendMessageHandler(w http.ResponseWriter, r *http.Request) {
	var req struct {
		SessionID string `json:"session_id"`
		Sender    string `json:"sender"` // customer or admin
		Message   string `json:"message"`
		ImageURL  string `json:"image_url"`
		// 🆕 Optional: Piggyback Init Info
		GuestName string `json:"guest_name"`
		DeviceID  string `json:"device_id"`
		FcmToken  string `json:"fcm_token"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.SessionID == "" {
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}
	if req.Message == "" && req.ImageURL == "" {
		http.Error(w, "Message or Image required", http.StatusBadRequest)
		return
	}

	// 🆕 Piggyback Init: If guest info is provided, ensure session exists FIRST
	if req.Sender == "customer" && len(req.SessionID) > 0 && req.SessionID[0] == 'g' {
		// Just call InitSession internally. It deals with upserts/conflicts efficiently.
		// We pass user_id = 0 since it's a guest message.
		if req.GuestName != "" || req.DeviceID != "" {
			fmt.Printf("DEBUG: Piggyback Init for %s (Name: %s, Device: %s)\n", req.SessionID, req.GuestName, req.DeviceID)
			// Force Init - Ignore return, trust side effects
			_, err := services.InitSession(req.GuestName, 0, req.SessionID, req.DeviceID, req.FcmToken)
			if err != nil {
				fmt.Printf("ERROR: Piggyback Init Failed: %v\n", err)
			}
		}
	}

	msg, err := services.SendMessage(req.SessionID, req.Sender, req.Message, req.ImageURL)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	// ส่ง Firebase notification
	if req.Sender == "admin" {
		go services.SendChatNotification(req.SessionID, req.Message)
	} else if req.Sender == "customer" {
		go services.NotifyAdminsOfCustomerMessage(req.SessionID, msg.SenderName, req.Message)
	}

	jsonResponse(w, http.StatusOK, msg)
}

// PollMessageHandler implements the long polling endpoint for customers
func PollMessageHandler(w http.ResponseWriter, r *http.Request) {
	sessionID := r.URL.Query().Get("session_id")
	if sessionID == "" {
		http.Error(w, "session_id required", http.StatusBadRequest)
		return
	}

	msgs, err := services.SubscribeCustomer(sessionID, 25*time.Second)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, msgs)
}

// AdminPollHandler implements the long polling endpoint for admin
func AdminPollHandler(w http.ResponseWriter, r *http.Request) {
	// In real app, check if user is admin here
	msgs, err := services.SubscribeAdmin(25 * time.Second)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, msgs)
}

// GetHistoryHandler returns previous messages
func GetHistoryHandler(w http.ResponseWriter, r *http.Request) {
	sessionID := r.URL.Query().Get("session_id")
	limitStr := r.URL.Query().Get("limit")
	limit := 50
	if l, err := strconv.Atoi(limitStr); err == nil && l > 0 {
		limit = l
	}

	log.Printf("DEBUG: GetHistoryHandler called - Session: %s, Limit: %d\n", sessionID, limit)
	msgs, err := services.GetChatHistory(sessionID, limit)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, msgs)
}

// GetRecentAdminMessagesHandler returns global chat history for admin console initialization
func GetRecentAdminMessagesHandler(w http.ResponseWriter, r *http.Request) {
	limitStr := r.URL.Query().Get("limit")
	offsetStr := r.URL.Query().Get("offset")

	limit := 20
	if l, err := strconv.Atoi(limitStr); err == nil && l > 0 {
		limit = l
	}

	offset := 0
	if o, err := strconv.Atoi(offsetStr); err == nil && o >= 0 {
		offset = o
	}

	msgs, err := services.GetRecentGlobalMessages(limit, offset)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	jsonResponse(w, http.StatusOK, msgs)
}

// DeleteMessageHandler deletes a specific message
func DeleteMessageHandler(w http.ResponseWriter, r *http.Request) {
	// In real app, check admin authentication header here!

	msgIDStr := r.URL.Query().Get("message_id")
	if msgIDStr == "" {
		http.Error(w, "message_id required", http.StatusBadRequest)
		return
	}

	msgID, err := strconv.Atoi(msgIDStr)
	if err != nil {
		http.Error(w, "invalid message_id", http.StatusBadRequest)
		return
	}

	err = services.DeleteMessage(msgID)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]string{"status": "deleted", "message_id": msgIDStr})
}

// GetUnreadCountHandler returns unread count for a member
func GetUnreadCountHandler(w http.ResponseWriter, r *http.Request) {
	memberIDStr := r.URL.Query().Get("member_id")
	if memberIDStr == "" {
		http.Error(w, "member_id required", http.StatusBadRequest)
		return
	}

	memberID, err := strconv.Atoi(memberIDStr)
	if err != nil {
		http.Error(w, "invalid member_id", http.StatusBadRequest)
		return
	}

	count, err := services.GetUnreadCount(memberID)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	// 🆕 Fetch VIP code for auto-updating UI dynamically
	vipCode := "normal"
	mysqlDB := database.GetMySQLDB()
	if mysqlDB != nil {
		_ = mysqlDB.QueryRow("SELECT COALESCE(vipcode, 'normal') FROM membertb WHERE memberid = ?", memberID).Scan(&vipCode)
		if vipCode == "" {
			vipCode = "normal"
		}
	}

	jsonResponse(w, http.StatusOK, map[string]interface{}{
		"unread_count": count,
		"vipcode":      vipCode,
	})
}

// MarkReadHandler marks admin messages as read (called by Customer)
func MarkReadHandler(w http.ResponseWriter, r *http.Request) {
	sessionID := r.URL.Query().Get("session_id")
	if sessionID == "" {
		http.Error(w, "session_id required", http.StatusBadRequest)
		return
	}

	err := services.MarkMessagesRead(sessionID)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

// AdminMarkReadHandler marks customer messages as read (called by Admin)
func AdminMarkReadHandler(w http.ResponseWriter, r *http.Request) {
	sessionID := r.URL.Query().Get("session_id")
	if sessionID == "" {
		http.Error(w, "session_id required", http.StatusBadRequest)
		return
	}

	err := services.MarkCustomerMessagesRead(sessionID)
	if err != nil {
		jsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	jsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

// UploadHandler handles image uploads for chat
func UploadHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	// 1. Limit size (5MB)
	r.ParseMultipartForm(5 << 20)

	file, handler, err := r.FormFile("file")
	if err != nil {
		fmt.Printf("DEBUG: Upload error: %v\n", err)
		http.Error(w, "Error retrieving file", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// 2. Create target directory
	uploadDir := "./uploads"
	if _, err := os.Stat(uploadDir); os.IsNotExist(err) {
		os.Mkdir(uploadDir, 0755)
	}

	// 3. Generate unique name
	ext := filepath.Ext(handler.Filename)
	fileName := fmt.Sprintf("%d%s", time.Now().UnixNano(), ext)
	filePath := filepath.Join(uploadDir, fileName)

	// 4. Save file
	dst, err := os.Create(filePath)
	if err != nil {
		http.Error(w, "Error saving file", http.StatusInternalServerError)
		return
	}
	defer dst.Close()

	if _, err := io.Copy(dst, file); err != nil {
		http.Error(w, "Error saving file content", http.StatusInternalServerError)
		return
	}

	// 5. Return relative URL
	jsonResponse(w, http.StatusOK, map[string]string{
		"url": "/uploads/" + fileName,
	})
}

// InitiateCallHandler handles signaling for starting a call
func InitiateCallHandler(w http.ResponseWriter, r *http.Request) {
	var req struct {
		SessionID string `json:"session_id" `
		Caller    string `json:"caller_name"`
		CallerID  string `json:"caller_id"` // Add CallerID to exclude self from noti
		Target    string `json:"target"`    // "admin" or "customer"
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.SessionID == "" {
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}

	// For simple signaling, we use SessionID as the Agora Channel Name
	channelName := req.SessionID

	// Determine signaling direction
	log.Printf("CALL DEBUG: InitiateCall - Session: %s, Caller: %s, ID: %s, Target: %s", req.SessionID, req.Caller, req.CallerID, req.Target)
	if req.Target == "admin" {

		go services.NotifyAdminsOfCall(req.SessionID, req.Caller, req.CallerID, channelName)
	} else {
		// Default to customer signaling (for Admin calling Customer)
		go services.SendCallNotification(req.SessionID, req.Caller, channelName)
	}

	jsonResponse(w, http.StatusOK, map[string]string{
		"status":       "signaling_sent",
		"channel_name": channelName,
		"target":       req.Target,
	})
}

// RejectCallHandler handles call rejection signaling
func RejectCallHandler(w http.ResponseWriter, r *http.Request) {
	var req struct {
		SessionID string `json:"session_id"`
		Caller    string `json:"caller_name"`
		Role      string `json:"role"` // "admin" or "customer" who is rejecting
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}

	log.Printf("CALL DEBUG: RejectCall - Session: %s, Rejecter: %s, Role: %s", req.SessionID, req.Caller, req.Role)

	// Send rejection signal via FCM
	go services.SendCallRejectNotification(req.SessionID, req.Caller, req.Role)

	jsonResponse(w, http.StatusOK, map[string]string{
		"status": "rejected_sent",
	})
}
