package handlers

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"go-naming/database"
	"log"
	"net/http"
	"sort"
	"time"
)

// GuestOrder represents the joined order data
type GuestOrder struct {
	ID              int     `json:"id"`
	GuestID         string  `json:"guest_id"`
	MemberID        *string `json:"member_id"`
	OrderData       string  `json:"order_data"`
	ShippingStatus  string  `json:"shipping_status"`
	ShippingAddress string  `json:"shipping_address"`
	ServerCreatedAt string  `json:"server_created_at"`

	// Joined fields
	AndroidID      *string `json:"android_id"`
	DeviceInfo     *string `json:"device_info"`
	AppVersion     *string `json:"app_version"`
	MemberRealName *string `json:"realname"`
	MemberSurname  *string `json:"surname"`
	MemberAddress  *string `json:"member_address"`

	// Computed for JSON response
	ProductName   string  `json:"product_name"`
	ProductPrice  float64 `json:"product_price"`
	Category      string  `json:"category"`
	PaymentStatus string  `json:"payment_status"`
	AddressSource string  `json:"address_source"`
	CustomerName  string  `json:"customer_name"`
	CustomerType  string  `json:"customer_type"`
}

// GuestAddress represents the address entry
type GuestAddress struct {
	ID              int    `json:"id"`
	GuestID         string `json:"guest_id"`
	Address         string `json:"address"`
	Phone           string `json:"phone"`
	ServerCreatedAt string `json:"server_created_at"`

	// Joined fields
	AndroidID  *string `json:"android_id"`
	DeviceInfo *string `json:"device_info"`
	AppVersion *string `json:"app_version"`

	// Computed
	Orders       []GuestOrder `json:"orders"`
	OrderCount   int          `json:"order_count"`
	OrderSummary string       `json:"order_summary"`
}

// GuestAddressResponse represents the final JSON structure
type GuestAddressResponse struct {
	Success bool           `json:"success"`
	Data    []GuestAddress `json:"data"`
	Orders  []GuestOrder   `json:"orders"`
	Count   int            `json:"count"`
	Stats   Stats          `json:"stats"`
	Message string         `json:"message,omitempty"`
}

type Stats struct {
	TotalAddresses   int `json:"total_addresses"`
	UniqueGuests     int `json:"unique_guests"`
	TotalOrders      int `json:"total_orders"`
	PendingShipments int `json:"pending_shipments"`
}

// GetGuestAddressesHandler replicates the logic of admin_guest_addresses.php
func GetGuestAddressesHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Access-Control-Allow-Origin", "*")

	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		json.NewEncoder(w).Encode(GuestAddressResponse{Success: false, Message: "MySQL connection unavailable"})
		return
	}

	// 1. Get ALL orders
	orderQuery := `
        SELECT go.id, go.guest_id, go.member_id, go.order_data, 
			   COALESCE(go.shipping_status, 'pending'), COALESCE(go.shipping_address, ''), go.server_created_at,
               gu.android_id, gu.device_info, gu.app_version,
               m.realname, m.surname, m.shipping_address
        FROM guest_orders go
        LEFT JOIN guest_users gu ON go.guest_id = gu.guest_id
        LEFT JOIN membertb m ON go.member_id = m.memberid
        ORDER BY go.server_created_at DESC
    `
	orderRows, err := mysqlDB.Query(orderQuery)
	if err != nil {
		log.Printf("GetGuestAddresses: order query error: %v", err)
		json.NewEncoder(w).Encode(GuestAddressResponse{Success: false, Message: "Database error (orders)"})
		return
	}
	defer orderRows.Close()

	var allOrders []GuestOrder
	pendingCount := 0

	for orderRows.Next() {
		var o GuestOrder
		// Temporary scan variables
		var memberID, androidID, deviceInfo, appVersion, realName, surname, memAddr sql.NullString

		if err := orderRows.Scan(&o.ID, &o.GuestID, &memberID, &o.OrderData, &o.ShippingStatus, &o.ShippingAddress, &o.ServerCreatedAt,
			&androidID, &deviceInfo, &appVersion, &realName, &surname, &memAddr); err != nil {
			log.Printf("GetGuestAddresses: scan error: %v", err)
			continue
		}

		// Assign from nullable vars
		o.MemberID = nullStrPtr(memberID)
		o.AndroidID = nullStrPtr(androidID)
		o.DeviceInfo = nullStrPtr(deviceInfo)
		o.AppVersion = nullStrPtr(appVersion)
		o.MemberRealName = nullStrPtr(realName)
		o.MemberSurname = nullStrPtr(surname)
		o.MemberAddress = nullStrPtr(memAddr)

		// Parse OrderData JSON
		var orderDetail map[string]interface{}
		if err := json.Unmarshal([]byte(o.OrderData), &orderDetail); err == nil {
			o.ProductName = getString(orderDetail, "product_name")
			if price, ok := orderDetail["product_price"].(float64); ok {
				o.ProductPrice = price
			}
			o.Category = getString(orderDetail, "category")
			o.PaymentStatus = getString(orderDetail, "payment_status")
		} else {
			o.ProductName = "N/A"
			o.PaymentStatus = "unknown"
		}

		if o.ShippingStatus == "" {
			o.ShippingStatus = "pending"
		}
		if o.ShippingStatus == "pending" {
			pendingCount++
		}

		// Address Source Logic
		memAddressStr := ""
		if o.MemberAddress != nil {
			memAddressStr = *o.MemberAddress
		}

		if o.ShippingAddress == "" && memAddressStr != "" {
			o.ShippingAddress = memAddressStr
			o.AddressSource = "registration"
		} else if o.ShippingAddress != "" {
			o.AddressSource = "order"
		} else {
			o.AddressSource = "none"
		}

		// Customer Name Logic
		realNameStr := ""
		if o.MemberRealName != nil {
			realNameStr = *o.MemberRealName
		}
		surnameStr := ""
		if o.MemberSurname != nil {
			surnameStr = *o.MemberSurname
		}

		if realNameStr != "" {
			o.CustomerName = realNameStr + " " + surnameStr
			o.CustomerType = "member"
		} else {
			o.CustomerName = ""
			o.CustomerType = "guest"
		}

		allOrders = append(allOrders, o)
	}

	// 2. Get Guest Addresses
	addrQuery := `
        SELECT ga.id, ga.guest_id, ga.address, ga.server_created_at,
               gu.android_id, gu.device_info, gu.app_version
        FROM guest_addresses ga
        LEFT JOIN guest_users gu ON ga.guest_id = gu.guest_id
        ORDER BY ga.server_created_at DESC
    `
	addrRows, err := mysqlDB.Query(addrQuery)
	if err != nil {
		log.Printf("GetGuestAddresses: address query error: %v", err)
		json.NewEncoder(w).Encode(GuestAddressResponse{Success: false, Message: "Database error (addresses)"})
		return
	}
	defer addrRows.Close()

	var addresses []GuestAddress
	guestIdsWithAddress := make(map[string]bool)

	for addrRows.Next() {
		var a GuestAddress
		var androidID, deviceInfo, appVersion sql.NullString

		if err := addrRows.Scan(&a.ID, &a.GuestID, &a.Address, &a.ServerCreatedAt,
			&androidID, &deviceInfo, &appVersion); err != nil {
			log.Printf("GetGuestAddresses: address scan error: %v", err)
			continue
		}

		a.AndroidID = nullStrPtr(androidID)
		a.DeviceInfo = nullStrPtr(deviceInfo)
		a.AppVersion = nullStrPtr(appVersion)

		addresses = append(addresses, a)
		guestIdsWithAddress[a.GuestID] = true
	}

	// 3. Group Orders by GuestID
	ordersByGuest := make(map[string][]GuestOrder)
	for _, o := range allOrders {
		ordersByGuest[o.GuestID] = append(ordersByGuest[o.GuestID], o)
	}

	// 4. Attach Orders to Addresses
	var combined []GuestAddress

	// Existing addresses
	for _, addr := range addresses {
		addr.Orders = ordersByGuest[addr.GuestID]
		addr.OrderCount = len(addr.Orders)
		addr.OrderSummary = buildOrderSummary(addr.Orders)
		combined = append(combined, addr)
	}

	// 5. Find Orphans (Orders without Address)
	for gid, orders := range ordersByGuest {
		if !guestIdsWithAddress[gid] {
			if len(orders) == 0 {
				continue
			}
			firstOrder := orders[0]

			// Pick best address
			displayAddress := "(ยังไม่ได้กรอกที่อยู่)"
			if firstOrder.ShippingAddress != "" {
				displayAddress = firstOrder.ShippingAddress
			}

			orphan := GuestAddress{
				ID:              0, // Virtual
				GuestID:         gid,
				Address:         displayAddress,
				AndroidID:       firstOrder.AndroidID,
				DeviceInfo:      firstOrder.DeviceInfo,
				AppVersion:      firstOrder.AppVersion,
				ServerCreatedAt: firstOrder.ServerCreatedAt,
				Orders:          orders,
				OrderCount:      len(orders),
				OrderSummary:    buildOrderSummary(orders),
			}
			combined = append(combined, orphan)
		}
	}

	// 6. Sort by Latest Order Date
	if len(combined) > 0 {
		sort.Slice(combined, func(i, j int) bool {
			dateA := getLatestDate(combined[i])
			dateB := getLatestDate(combined[j])
			return dateA.After(dateB)
		})
	}

	// 7. Calculate Unique Guests
	uniqueMap := make(map[string]bool)
	for _, c := range combined {
		uniqueMap[c.GuestID] = true
	}

	resp := GuestAddressResponse{
		Success: true,
		Data:    combined,
		Orders:  allOrders,
		Count:   len(combined),
		Stats: Stats{
			TotalAddresses:   len(combined),
			UniqueGuests:     len(uniqueMap),
			TotalOrders:      len(allOrders),
			PendingShipments: pendingCount,
		},
	}

	json.NewEncoder(w).Encode(resp)
}

func getString(m map[string]interface{}, key string) string {
	if v, ok := m[key].(string); ok {
		return v
	}
	return ""
}

func nullStrPtr(ns sql.NullString) *string {
	if ns.Valid {
		return &ns.String
	}
	return nil
}

func buildOrderSummary(orders []GuestOrder) string {
	summary := ""
	for i, o := range orders {
		icon := "⏳"
		if o.ShippingStatus == "shipped" {
			icon = "✅"
		}
		line := fmt.Sprintf("%s %s (%.0f บาท)", icon, o.ProductName, o.ProductPrice)
		if i > 0 {
			summary += "\n"
		}
		summary += line
	}
	return summary
}

func getLatestDate(a GuestAddress) time.Time {
	layout := "2006-01-02 15:04:05" // MySQL datetime format

	// Default to address created at
	// Handle empty string just in case
	if a.ServerCreatedAt == "" {
		return time.Time{}
	}

	latest, _ := time.Parse(layout, a.ServerCreatedAt)

	// Check orders for newer date
	for _, o := range a.Orders {
		if o.ServerCreatedAt == "" {
			continue
		}
		t, err := time.Parse(layout, o.ServerCreatedAt)
		if err == nil && t.After(latest) {
			latest = t
		}
	}
	return latest
}

// UpdateGuestAddressHandler handles address updates
func UpdateGuestAddressHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Access-Control-Allow-Origin", "*")

	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "MySQL connection unavailable"})
		return
	}

	var req struct {
		GuestID string `json:"guest_id"`
		Address string `json:"address"`
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "Invalid request body"})
		return
	}

	// Update guest_addresses table
	// Check if exists first
	var exists int
	err := mysqlDB.QueryRow("SELECT COUNT(*) FROM guest_addresses WHERE guest_id = ?", req.GuestID).Scan(&exists)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "Database error"})
		return
	}

	if exists > 0 {
		_, err = mysqlDB.Exec("UPDATE guest_addresses SET address = ? WHERE guest_id = ?", req.Address, req.GuestID)
	} else {
		_, err = mysqlDB.Exec("INSERT INTO guest_addresses (guest_id, address, server_created_at) VALUES (?, ?, NOW())", req.GuestID, req.Address)
	}

	if err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "Update failed: " + err.Error()})
		return
	}

	// Also update shipping_address in guest_orders for this guest
	_, _ = mysqlDB.Exec("UPDATE guest_orders SET shipping_address = ? WHERE guest_id = ?", req.Address, req.GuestID)

	json.NewEncoder(w).Encode(map[string]interface{}{"success": true, "message": "Address updated"})
}

// ToggleShippingStatusHandler handles status updates
func ToggleShippingStatusHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Access-Control-Allow-Origin", "*")

	mysqlDB := database.GetMySQLDB()
	if mysqlDB == nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "MySQL connection unavailable"})
		return
	}

	var req struct {
		OrderID int    `json:"order_id"`
		Status  string `json:"status"` // 'pending' or 'shipped'
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "Invalid request body"})
		return
	}

	_, err := mysqlDB.Exec("UPDATE guest_orders SET shipping_status = ? WHERE id = ?", req.Status, req.OrderID)
	if err != nil {
		json.NewEncoder(w).Encode(map[string]interface{}{"success": false, "message": "Update failed: " + err.Error()})
		return
	}

	json.NewEncoder(w).Encode(map[string]interface{}{
		"success":         true,
		"message":         "Status updated",
		"shipping_status": req.Status,
		"order_id":        req.OrderID,
	})
}
