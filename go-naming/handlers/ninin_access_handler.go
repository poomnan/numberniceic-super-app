package handlers

import (
	"encoding/json"
	"fmt"
	"go-naming/database"
	"net/http"
	"strconv"
	"strings"
	"time"
)

type NininAccessRedeemRequest struct {
	MemberID    string `json:"member_id"`
	GuestID     string `json:"guest_id"`
	PackageCode string `json:"package_code"`
	SecretCode  string `json:"secret_code"`
}

func mapPackageToProduct(packageCode string) (int, string, float64, bool) {
	switch strings.ToLower(strings.TrimSpace(packageCode)) {
	case "dream_monthly":
		return 1, "ทำนายฝันรายเดือน (Secret Code)", 39.0, true
	case "dream_yearly":
		return 2, "ทำนายฝันรายปี (Secret Code)", 299.0, true
	case "dream_lifetime":
		return 3, "ทำนายฝันตลอดชีพ (Secret Code)", 1599.0, true
	default:
		return 0, "", 0, false
	}
}

// NininRedeemAccessHandler converts a validated Secret Code into a paid dream entitlement.
// This keeps dream access authoritative on backend via shop_orders.
func NininRedeemAccessHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req NininAccessRedeemRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid JSON body", http.StatusBadRequest)
		return
	}

	productID, productDetail, amount, ok := mapPackageToProduct(req.PackageCode)
	if !ok {
		http.Error(w, "Invalid package_code", http.StatusBadRequest)
		return
	}
	if strings.TrimSpace(req.MemberID) == "" && strings.TrimSpace(req.GuestID) == "" {
		http.Error(w, "member_id or guest_id is required", http.StatusBadRequest)
		return
	}

	var userID interface{} = nil
	if strings.TrimSpace(req.MemberID) != "" {
		if parsedID, err := strconv.Atoi(strings.TrimSpace(req.MemberID)); err == nil && parsedID > 0 {
			userID = parsedID
		}
	}

	refNo := fmt.Sprintf("SC%v", time.Now().UnixNano())
	if len(refNo) > 24 {
		refNo = refNo[:24]
	}

	_, err := database.DB.Exec(
		`INSERT INTO shop_orders (ref_no, user_id, guest_id, product_id, product_detail, amount, status, updated_at)
		 VALUES ($1, $2, $3, $4, $5, $6, 'paid', CURRENT_TIMESTAMP)`,
		refNo,
		userID,
		strings.TrimSpace(req.GuestID),
		productID,
		productDetail,
		amount,
	)
	if err != nil {
		http.Error(w, "Failed to activate entitlement", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(map[string]interface{}{
		"ok":           true,
		"ref_no":       refNo,
		"product_id":   productID,
		"package_code": req.PackageCode,
		"source":       "secret_code",
	})
}
