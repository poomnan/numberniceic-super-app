package services

import (
	"bytes"
	"database/sql"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"strings"
	"time"

	"go-naming/database"
)

type PaymentService struct{}

func NewPaymentService() *PaymentService {
	return &PaymentService{}
}

// GeneratePromptPayQR calls PaySolutions API to get QR Base64
func (s *PaymentService) GeneratePromptPayQR(userID *int, guestID string, amount float64, productDetail string, productID int) (string, string, error) {
	merchantID := os.Getenv("MERCHANT_ID")
	if merchantID == "" {
		merchantID = "15351059"
	}
	apiKey := os.Getenv("API_KEY")
	if apiKey == "" {
		// User provided Auth Key (JWT)
		apiKey = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiIyIiwiZXhwIjozMzE4Mzk0NDQ4LCJpYXQiOjE3NDE1OTQ0NDgsImp0aSI6IjE3NDE1OTQ0NDg0ODNjOGJjNTJiOWQ3ZDRhMTI5MDE0MWQzNTA1OTMzMTdhNWFlNDlkN2E5NjNiMWY1MjVjMjc2MjIyMzVlOGNjZDVhNTkyIiwibmJmIjoxNzQxNTk0NDQ4LCJzdWIiOiI1MTA1OSJ9.PbsAFWoVnhnjVjmhc0TQWAYmPeIpHbPhPUbeEo4ck_1eszh8V3wZ8GDNeUlMbEIcFoSVI9KK5X7-rsq6Or3ExNJQq-nZ09AbIbk80QZ4ZCCS1B7ipBt8dWSdpeiX5C_xvrV3ta51uEw8ETn88uiCamiacLck6t5YUZt5a-hFVxNXLOldGXYkibvia_sOetI-d9zS2wNv7ngl493u4_SHoxgErONVChMDV-4A-RPh10L_kNsv1OpDdkIL4xwiTM2WHGkd-Ow9oUHFUFtQXGnPEZYeXl52uKAYdckcLUuRfrwQZXGXiFZmq9XtF5bcRetUfYx7h8oot8Xl_zVF-rE4NQ"
	}
	postbackURL := os.Getenv("POST_BACK_URL")
	if postbackURL == "" {
		postbackURL = "https://xn--b3cu8e7ah6h.com/api/pay/willback"
	}

	// 1. Generate Unique Ref No
	refNo := fmt.Sprintf("%d", time.Now().UnixNano())
	if len(refNo) > 12 {
		refNo = refNo[len(refNo)-12:]
	}

	// 2. Create Order in Database
	_, err := database.DB.Exec(
		"INSERT INTO shop_orders (ref_no, user_id, guest_id, product_id, product_detail, amount, status) VALUES ($1, $2, $3, $4, $5, $6, $7)",
		refNo, userID, guestID, productID, productDetail, amount, "pending",
	)
	if err != nil {
		return "", "", err
	}

	// 3. Call PaySolutions
	baseURL := "https://apis.paysolutions.asia/tep/api/v2/promptpaynew"

	payload := map[string]interface{}{
		"merchantID":    merchantID,
		"productDetail": productDetail,
		"customerEmail": "customer@numberniceic.com",
		"customerName":  "Guest User",
		"total":         fmt.Sprintf("%.2f", amount),
		"referenceNo":   refNo,
	}
	if postbackURL != "" {
		payload["postbackurl"] = postbackURL  // lowercase
		payload["postBackURL"] = postbackURL  // camelCase with capital URL (Correct for modern API)
		payload["postback_url"] = postbackURL // snake_case
		payload["callback_url"] = postbackURL // some versions use this
	}

	jsonPayload, err := json.Marshal(payload)
	if err != nil {
		return "", "", fmt.Errorf("failed to marshal payload: %v", err)
	}

	log.Printf("DEBUG: PaySolutions Payload: %s", string(jsonPayload))

	req, err := http.NewRequest("POST", baseURL, bytes.NewBuffer(jsonPayload))
	if err != nil {
		return "", "", err
	}

	req.Header.Set("Authorization", "Bearer "+apiKey)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")

	client := &http.Client{Timeout: 15 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		return "", "", err
	}
	defer resp.Body.Close()

	body, _ := io.ReadAll(resp.Body)
	if resp.StatusCode != http.StatusOK {
		log.Printf("❌ PaySolutions Error Status=%d: %s", resp.StatusCode, string(body))
		return "", "", fmt.Errorf("PaySolutions error: %s", string(body))
	}

	var resultMap map[string]interface{}
	if err := json.Unmarshal(body, &resultMap); err != nil {
		log.Printf("❌ Failed to parse PaySolutions response: %v", err)
		return "", "", err
	}

	var qrBase64 string
	if dataMap, ok := resultMap["data"].(map[string]interface{}); ok {
		if val, ok := dataMap["image"].(string); ok {
			qrBase64 = val
		} else if val, ok := dataMap["image_base64"].(string); ok {
			qrBase64 = val
		}
	} else if val, ok := resultMap["image"].(string); ok {
		qrBase64 = val
	} else if val, ok := resultMap["image_base64"].(string); ok {
		qrBase64 = val
	}

	if qrBase64 == "" {
		log.Printf("❌ PaySolutions response missing image: %s", string(body))
		return "", "", fmt.Errorf("failed to get QR image from PaySolutions")
	}

	// Clean Base64 string
	qrBase64 = strings.ReplaceAll(qrBase64, "\n", "")
	qrBase64 = strings.ReplaceAll(qrBase64, "\r", "")
	qrBase64 = strings.ReplaceAll(qrBase64, " ", "")

	if !strings.HasPrefix(qrBase64, "data:image") {
		qrBase64 = "data:image/png;base64," + qrBase64
	}

	prefix := ""
	if len(qrBase64) > 50 {
		prefix = qrBase64[:50]
	} else {
		prefix = qrBase64
	}
	log.Printf("✅ QR Generated Success: Ref=%s, QR_len=%d, Prefix=%s", refNo, len(qrBase64), prefix)
	return refNo, qrBase64, nil
}

// ProcessPaymentSuccess updates order status to 'paid'
func (s *PaymentService) ProcessPaymentSuccess(refNo string) error {
	// 1. Get Guest ID and Product ID, and Category ID
	var guestID sql.NullString
	var productID sql.NullInt64
	var categoryID sql.NullInt64
	err := database.DB.QueryRow(`
		SELECT o.guest_id, o.product_id, p.category_id 
		FROM shop_orders o
		LEFT JOIN shop_products p ON o.product_id = p.id
		WHERE o.ref_no = $1
	`, refNo).Scan(&guestID, &productID, &categoryID)

	if err == nil && guestID.Valid && guestID.String != "" {
		// Only reward 10 messages if it's NOT one of the Ninin time-based packages (1, 2, 3, 4)
		isTimeBased := false
		if productID.Valid {
			pid := productID.Int64
			if pid >= 1 && pid <= 4 {
				isTimeBased = true
			}
		}

		if !isTimeBased {
			log.Printf("🎁 Rewarding 10 messages to Guest: %s", guestID.String)
			_, errBonus := database.DB.Exec(`
				INSERT INTO guest_usage (guest_id, message_count, paid_messages) VALUES ($1, 0, 10)
				ON CONFLICT (guest_id) DO UPDATE SET paid_messages = guest_usage.paid_messages + 10, last_used_at = CURRENT_TIMESTAMP
			`, guestID.String)
			if errBonus != nil {
				log.Printf("❌ Failed to reward guest: %v", errBonus)
			}
		} else {
			log.Printf("ℹ️ Time-based package (ID %v) purchased. No bonus messages added.", productID.Int64)
		}
	}

	// Update status and shipping_status if category_id is 1
	shippingStatus := "none"
	if categoryID.Valid && categoryID.Int64 == 1 {
		shippingStatus = "ready"
	}

	_, err = database.DB.Exec("UPDATE shop_orders SET status = 'paid', shipping_status = $1, updated_at = CURRENT_TIMESTAMP WHERE ref_no = $2", shippingStatus, refNo)

	if err == nil {
		// 🔔 Send notification to User via Goroutine to avoid blocking
		go SendOrderSuccessNotification(refNo)
		// 🔔 Notify Admins
		go NotifyAdminsOfNewOrder(refNo)
	}

	return err
}

// GetOrderStatus retrieves current status, optionally polling PaySolutions directly
func (s *PaymentService) GetOrderStatus(refNo string) (string, error) {
	// 1. Check local database first
	var status string
	err := database.DB.QueryRow("SELECT status FROM shop_orders WHERE ref_no = $1", refNo).Scan(&status)
	if err != nil {
		return "", err
	}

	// 2. If already paid, return immediately
	if status == "paid" {
		return "paid", nil
	}

	// 3. Proactively check PaySolutions Inquiry API
	apiKey := os.Getenv("PS_API_KEY") // Use short API Key
	if apiKey == "" {
		apiKey = "hxYH185V"
	}
	secretKey := os.Getenv("PS_SECRET_KEY")
	if secretKey == "" {
		secretKey = "G4idCuvbZpjjdR79"
	}
	merchantID := os.Getenv("MERCHANT_ID")
	if merchantID == "" || len(merchantID) < 5 {
		merchantID = "15351059"
	}
	// Merchant ID for Inquiry API should be the last 5 digits according to some docs,
	// but let's try to get it from the full ID if possible or just use what we have.
	shortMerchantID := merchantID
	if len(merchantID) > 5 {
		shortMerchantID = merchantID[len(merchantID)-5:]
	}

	// PaySolutions Inquiry API
	inquiryURL := "https://apis.paysolutions.asia/order/orderdetailpost"

	payload := map[string]interface{}{
		"merchantID":    shortMerchantID,
		"refno":         refNo,
		"orderNo":       "X",
		"productDetail": "QWERTY",
	}

	jsonData, _ := json.Marshal(payload)
	req, err := http.NewRequest("POST", inquiryURL, bytes.NewBuffer(jsonData))
	if err != nil {
		return status, nil // Fallback to DB status
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("apikey", apiKey)
	req.Header.Set("merchantSecretKey", secretKey)
	req.Header.Set("merchantID", shortMerchantID)

	client := &http.Client{Timeout: 10 * time.Second}
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("⚠️ Inquiry API error (network): %v", err)
		return status, nil
	}
	defer resp.Body.Close()

	body, _ := io.ReadAll(resp.Body)
	log.Printf("🔍 INQUIRY RESPONSE for Ref=%s: %s", refNo, string(body))

	if resp.StatusCode == http.StatusOK {
		// PaySolutions returns an array of orders
		var orders []map[string]interface{}
		if err := json.Unmarshal(body, &orders); err == nil && len(orders) > 0 {
			order := orders[0]
			psStatus, _ := order["Status"].(string)
			log.Printf("🔍 INQUIRY STATUS: %s (Raw: %v)", psStatus, order)

			// "CP", "Y", "SUCCESS" or "Paid" mean Complete/Paid in PaySolutions
			if psStatus == "CP" || psStatus == "Y" || psStatus == "SUCCESS" || strings.ToLower(psStatus) == "paid" {
				log.Printf("✅ INQUIRY confirmed PAID for Ref=%s. Updating DB.", refNo)
				s.ProcessPaymentSuccess(refNo)
				return "paid", nil
			}
		} else {
			// Some versions return a single object or different field names
			var singleOrder map[string]interface{}
			if err := json.Unmarshal(body, &singleOrder); err == nil {
				psStatus, _ := singleOrder["Status"].(string)
				if psStatus == "" {
					psStatus, _ = singleOrder["status"].(string)
				}
				if psStatus == "CP" || psStatus == "Y" || psStatus == "SUCCESS" || strings.ToLower(psStatus) == "paid" {
					log.Printf("✅ INQUIRY confirmed PAID for Ref=%s (single obj). Updating DB.", refNo)
					s.ProcessPaymentSuccess(refNo)
					return "paid", nil
				}
			}
		}
	}

	return status, nil
}
