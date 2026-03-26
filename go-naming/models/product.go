package models

import "time"

type ProductCategory struct {
	ID          int       `json:"id"`
	Name        string    `json:"name"`
	Description string    `json:"description"`
	ImageURL    string    `json:"image_url"`
	CreatedAt   time.Time `json:"created_at"`
}

type Product struct {
	ID             int       `json:"id"`
	CategoryID     int       `json:"category_id"`
	CategoryName   string    `json:"category_name,omitempty"`
	Name           string    `json:"name"`
	Description    string    `json:"description"`
	Price          float64   `json:"price"`
	ImageURL       string    `json:"image_url"`
	StockQuantity  int       `json:"stock_quantity"`
	IsActive       bool      `json:"is_active"`
	CreatedAt      time.Time `json:"created_at"`
	UpdatedAt      time.Time `json:"updated_at"`
	ShippingStatus string    `json:"shipping_status,omitempty"`
}
