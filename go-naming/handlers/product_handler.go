package handlers

import (
	"database/sql"
	"encoding/json"
	"go-naming/database"
	"go-naming/models"
	"log"
	"net/http"
	"strconv"
)

// Helper to send JSON response (duplicated from main.go or we should move it to a utils package)
func productJsonResponse(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(data); err != nil {
		// Log error if needed
	}
}

// GetProductCategoriesHandler returns all product categories
func GetProductCategoriesHandler(w http.ResponseWriter, r *http.Request) {
	rows, err := database.DB.Query("SELECT id, name, COALESCE(description, ''), COALESCE(image_url, ''), created_at FROM shop_product_categories ORDER BY name")
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	defer rows.Close()

	var categories []models.ProductCategory
	for rows.Next() {
		var c models.ProductCategory
		if err := rows.Scan(&c.ID, &c.Name, &c.Description, &c.ImageURL, &c.CreatedAt); err != nil {
			productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		categories = append(categories, c)
	}
	if categories == nil {
		categories = []models.ProductCategory{}
	}
	productJsonResponse(w, http.StatusOK, categories)
}

// GetProductsHandler returns all products
func GetProductsHandler(w http.ResponseWriter, r *http.Request) {
	categoryID := r.URL.Query().Get("category_id")
	query := `
		SELECT p.id, p.category_id, COALESCE(c.name, 'ไม่มีหมวดหมู่'), p.name, COALESCE(p.description, ''), p.price, COALESCE(p.image_url, ''), p.stock_quantity, p.is_active, p.created_at, p.updated_at,
		COALESCE((SELECT shipping_status FROM shop_orders WHERE product_id = p.id AND status = 'paid' ORDER BY created_at DESC LIMIT 1), 'none') as shipping_status
		FROM shop_products p
		LEFT JOIN shop_product_categories c ON p.category_id = c.id
	`
	var rows *sql.Rows
	var err error

	if categoryID != "" {
		query += " WHERE p.category_id = $1 ORDER BY p.price ASC, p.name ASC"
		rows, err = database.DB.Query(query, categoryID)
	} else {
		query += " ORDER BY p.price ASC, p.name ASC"
		rows, err = database.DB.Query(query)
	}

	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	defer rows.Close()

	var products []models.Product
	for rows.Next() {
		var p models.Product
		if err := rows.Scan(&p.ID, &p.CategoryID, &p.CategoryName, &p.Name, &p.Description, &p.Price, &p.ImageURL, &p.StockQuantity, &p.IsActive, &p.CreatedAt, &p.UpdatedAt, &p.ShippingStatus); err != nil {
			productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		products = append(products, p)
	}
	if products == nil {
		products = []models.Product{}
	}
	productJsonResponse(w, http.StatusOK, products)
}

// AddProductHandler allows admin to add a product
func AddProductHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var p models.Product
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		productJsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}

	query := `
		INSERT INTO shop_products (category_id, name, description, price, image_url, stock_quantity, is_active)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
		RETURNING id, created_at, updated_at
	`
	log.Printf("DEBUG: AddProductHandler query: %s", query)
	err := database.DB.QueryRow(query, p.CategoryID, p.Name, p.Description, p.Price, p.ImageURL, p.StockQuantity, p.IsActive).Scan(&p.ID, &p.CreatedAt, &p.UpdatedAt)
	if err != nil {
		log.Printf("ERROR: AddProductHandler failed: %v", err)
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	productJsonResponse(w, http.StatusCreated, p)
}

// UpdateProductHandler allows admin to update a product
func UpdateProductHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPut && r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var p models.Product
	if err := json.NewDecoder(r.Body).Decode(&p); err != nil {
		productJsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}

	query := `
		UPDATE shop_products 
		SET category_id = $1, name = $2, description = $3, price = $4, image_url = $5, stock_quantity = $6, is_active = $7, updated_at = CURRENT_TIMESTAMP
		WHERE id = $8
	`
	_, err := database.DB.Exec(query, p.CategoryID, p.Name, p.Description, p.Price, p.ImageURL, p.StockQuantity, p.IsActive, p.ID)
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	productJsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

// DeleteProductHandler allows admin to delete a product
func DeleteProductHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodDelete && r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	idStr := r.URL.Query().Get("id")
	id, _ := strconv.Atoi(idStr)

	_, err := database.DB.Exec("DELETE FROM shop_products WHERE id = $1", id)
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	productJsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

// AddCategoryHandler allows admin to add a category
func AddCategoryHandler(w http.ResponseWriter, r *http.Request) {
	var c models.ProductCategory
	if err := json.NewDecoder(r.Body).Decode(&c); err != nil {
		productJsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}

	query := "INSERT INTO shop_product_categories (name, description, image_url) VALUES ($1, $2, $3) RETURNING id, created_at"
	err := database.DB.QueryRow(query, c.Name, c.Description, c.ImageURL).Scan(&c.ID, &c.CreatedAt)
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	productJsonResponse(w, http.StatusCreated, c)
}

// UpdateCategoryHandler allows admin to update a category
func UpdateCategoryHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPut && r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var c models.ProductCategory
	if err := json.NewDecoder(r.Body).Decode(&c); err != nil {
		productJsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request body"})
		return
	}

	query := "UPDATE shop_product_categories SET name = $1, description = $2, image_url = $3 WHERE id = $4"
	_, err := database.DB.Exec(query, c.Name, c.Description, c.ImageURL, c.ID)
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	productJsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

// DeleteCategoryHandler allows admin to delete a category
func DeleteCategoryHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodDelete && r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	idStr := r.URL.Query().Get("id")
	id, _ := strconv.Atoi(idStr)

	_, err := database.DB.Exec("DELETE FROM shop_product_categories WHERE id = $1", id)
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	productJsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

// UpdateShippingStatusHandler marks a product's order as shipped
func UpdateShippingStatusHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		ProductID int    `json:"product_id"`
		Status    string `json:"status"` // e.g., 'shipped'
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		productJsonResponse(w, http.StatusBadRequest, map[string]string{"error": "Invalid request"})
		return
	}

	// Update the latest paid order for this product
	query := `
		UPDATE shop_orders 
		SET shipping_status = $1, updated_at = CURRENT_TIMESTAMP 
		WHERE id = (
			SELECT id FROM shop_orders 
			WHERE product_id = $2 AND status = 'paid' 
			ORDER BY created_at DESC LIMIT 1
		)
	`
	_, err := database.DB.Exec(query, req.Status, req.ProductID)
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}

	productJsonResponse(w, http.StatusOK, map[string]string{"status": "success"})
}

// AdminGetZirconOrdersHandler returns all orders for category 1 (Zircon - เพทาย)
func AdminGetZirconOrdersHandler(w http.ResponseWriter, r *http.Request) {
	log.Printf("DEBUG: AdminGetZirconOrdersHandler called from %s", r.RemoteAddr)
	query := `
		SELECT o.id, o.ref_no, o.user_id, o.product_detail, o.amount, o.status, o.created_at, o.guest_id, o.product_id, p.name as product_name
		FROM shop_orders o
		JOIN shop_products p ON o.product_id = p.id
		WHERE p.category_id = 1
		ORDER BY o.created_at DESC
	`
	rows, err := database.DB.Query(query)
	if err != nil {
		productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	defer rows.Close()

	var orders []map[string]interface{}
	for rows.Next() {
		var id int
		var refNo string
		var userID sql.NullInt64
		var productDetail string
		var amount float64
		var status string
		var createdAt string
		var guestID sql.NullString
		var productID int
		var productName string

		if err := rows.Scan(&id, &refNo, &userID, &productDetail, &amount, &status, &createdAt, &guestID, &productID, &productName); err != nil {
			productJsonResponse(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}

		order := map[string]interface{}{
			"id":             id,
			"ref_no":         refNo,
			"user_id":        userID.Int64,
			"product_detail": productDetail,
			"amount":         amount,
			"status":         status,
			"created_at":     createdAt,
			"guest_id":       guestID.String,
			"product_id":     productID,
			"product_name":   productName,
		}
		orders = append(orders, order)
	}
	if orders == nil {
		orders = []map[string]interface{}{}
	}
	productJsonResponse(w, http.StatusOK, orders)
}
