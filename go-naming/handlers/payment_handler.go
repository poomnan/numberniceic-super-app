package handlers

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"strings"

	"go-naming/services"
)

type PaymentHandler struct {
	paymentService *services.PaymentService
}

func NewPaymentHandler(ps *services.PaymentService) *PaymentHandler {
	return &PaymentHandler{paymentService: ps}
}

// CreatePaymentQRHandler generates a QR code for payment
func (h *PaymentHandler) CreatePaymentQRHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		UserID        int     `json:"user_id"`
		GuestID       string  `json:"guest_id"`
		Amount        float64 `json:"amount"`
		ProductDetail string  `json:"product_detail"`
		ProductID     int     `json:"product_id"`
		FCMToken      string  `json:"fcm_token"`
		DeviceID      string  `json:"device_id"`
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request", http.StatusBadRequest)
		return
	}

	// 🔔 Sync Session/FCM Token immediately if provided (Robustness fix)
	if req.FCMToken != "" {
		name := "Guest"
		if req.UserID > 0 {
			name = fmt.Sprintf("User %d", req.UserID)
		}
		_, _ = services.InitSession(name, req.UserID, req.GuestID, req.DeviceID, req.FCMToken)
	}

	var userID *int
	if req.UserID > 0 {
		userID = &req.UserID
	}

	refNo, qrBase64, err := h.paymentService.GeneratePromptPayQR(userID, req.GuestID, req.Amount, req.ProductDetail, req.ProductID)
	if err != nil {
		log.Printf("Error generating QR: %v", err)
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"ref_no":    refNo,
		"qr_base64": qrBase64,
		"amount":    req.Amount,
	})
}

// PaymentStatusHandler checks if an order is paid
func (h *PaymentHandler) PaymentStatusHandler(w http.ResponseWriter, r *http.Request) {
	refNo := r.URL.Query().Get("ref_no")
	if refNo == "" {
		http.Error(w, "Missing ref_no", http.StatusBadRequest)
		return
	}

	status, err := h.paymentService.GetOrderStatus(refNo)
	if err != nil {
		http.Error(w, "Order not found", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"ref_no": refNo,
		"status": status,
	})
}

// PaymentWebhookHandler handles success callback from PaySolutions
func (h *PaymentHandler) PaymentWebhookHandler(w http.ResponseWriter, r *http.Request) {
	log.Printf("💰 WEBHOOK START: Method=%s, URL=%s", r.Method, r.URL.String())
	log.Printf("💰 WEBHOOK Headers: %v", r.Header)

	// 1. Parse Form
	r.ParseForm()
	log.Printf("💰 WEBHOOK Form Data: %v", r.Form)
	log.Printf("💰 WEBHOOK Query Params: %v", r.URL.Query())

	// 2. Read Raw Body
	bodyBytes, _ := io.ReadAll(r.Body)
	bodyStr := string(bodyBytes)
	log.Printf("💰 WEBHOOK RECEIVED Body: %s", bodyStr)

	// Restore body
	r.Body = io.NopCloser(bytes.NewBuffer(bodyBytes))

	// Try to get values from everywhere (Case Insensitive)
	getVal := func(targetKey string) string {
		targetKey = strings.ToLower(targetKey)

		// Check Form
		for k, v := range r.Form {
			if strings.ToLower(k) == targetKey && len(v) > 0 {
				return v[0]
			}
		}

		// Check Query
		for k, v := range r.URL.Query() {
			if strings.ToLower(k) == targetKey && len(v) > 0 {
				return v[0]
			}
		}

		return ""
	}

	refNo := getVal("refno")
	if refNo == "" {
		refNo = getVal("referenceno")
	}
	status := getVal("status")
	totalStr := getVal("total")

	// 4. If still empty, try JSON
	if refNo == "" && bodyStr != "" {
		var jsonMap map[string]interface{}
		if err := json.Unmarshal(bodyBytes, &jsonMap); err == nil {
			log.Printf("DEBUG: Webhook JSON Map: %v", jsonMap)
			// Helper for JSON too
			getJSONVal := func(target string) string {
				target = strings.ToLower(target)
				for k, v := range jsonMap {
					if strings.ToLower(k) == target {
						return fmt.Sprintf("%v", v)
					}
				}
				return ""
			}

			if v := getJSONVal("refno"); v != "" {
				refNo = v
			}
			if refNo == "" {
				if v := getJSONVal("referenceno"); v != "" {
					refNo = v
				}
			}
			if v := getJSONVal("status"); v != "" {
				status = v
			}
			if v := getJSONVal("total"); v != "" {
				totalStr = v
			}
		}
	}

	log.Printf("💰 WEBHOOK FINAL: Ref=%s, Status=%s, Total=%s", refNo, status, totalStr)

	if refNo != "" && (strings.ToUpper(status) == "SUCCESS" || status == "success" || status == "00") {
		err := h.paymentService.ProcessPaymentSuccess(refNo)
		if err != nil {
			log.Printf("❌ Error processing success webhook for Ref=%s: %v", refNo, err)
			// Don't error out to PaySolutions yet, maybe we just didn't find the ref
		} else {
			log.Printf("✅ Webhook Success: Order %s updated to PAID", refNo)
		}
	}

	w.WriteHeader(http.StatusOK)
	w.Write([]byte("OK"))
}
