package models

import (
	"database/sql"
	"time"

	"github.com/lib/pq"
)

// User represents the 'users' table
type User struct {
	ID           int          `json:"id"`
	Username     string       `json:"username"`
	Email        string       `json:"email"`
	PasswordHash string       `json:"-"` // Don't return password hash in JSON
	DisplayName  string       `json:"display_name"`
	CreatedAt    time.Time    `json:"created_at"`
	IsAdmin      bool         `json:"is_admin"`
	UpdatedAt    time.Time    `json:"updated_at"`
	DeletedAt    sql.NullTime `json:"deleted_at,omitempty"`
}

// Member represents the 'member' table
type Member struct {
	ID                     int            `json:"id"`
	Username               string         `json:"username"`
	Email                  sql.NullString `json:"email"`
	Tel                    sql.NullString `json:"tel"`
	Status                 sql.NullInt64  `json:"status"`
	DayOfBirth             sql.NullInt64  `json:"day_of_birth"`
	AssignedColors         sql.NullString `json:"assigned_colors"`
	VipExpiresAt           sql.NullTime   `json:"vip_expires_at"`
	Provider               sql.NullString `json:"provider"`
	ProviderID             sql.NullString `json:"provider_id"`
	AvatarURL              sql.NullString `json:"avatar_url"`
	WalletColorsNotifiedAt sql.NullTime   `json:"wallet_colors_notified_at"`
}

// NameMiracle represents the 'names_miracle' table
type NameMiracle struct {
	NameID      int            `json:"name_id"`
	ThName      string         `json:"th_name"`
	SatNum      pq.Int64Array  `json:"sat_num"` // Expecting array of integers
	ShaNum      pq.Int64Array  `json:"sha_num"` // Expecting array of integers
	KSunday     bool           `json:"k_sunday"`
	KMonday     bool           `json:"k_monday"`
	KTuesday    bool           `json:"k_tuesday"`
	KWednesday1 bool           `json:"k_wednesday1"` // Day
	KWednesday2 bool           `json:"k_wednesday2"` // Night
	KThursday   bool           `json:"k_thursday"`
	KFriday     bool           `json:"k_friday"`
	KSaturday   bool           `json:"k_saturday"`
	TSat        pq.StringArray `json:"t_sat"`
	TSha        pq.StringArray `json:"t_sha"`
	// ThNameVector is omitted for now as it is USER-DEFINED type
}

// DecodeResult represents the output of Step 1: Decoding & Summation
type DecodeResult struct {
	Name       string         `json:"name"`
	Characters []CharValue    `json:"characters"`
	TotalSat   int            `json:"total_sat"`
	TotalSha   int            `json:"total_sha"`
	SatPairs   []string       `json:"sat_pairs"`
	ShaPairs   []string       `json:"sha_pairs"`
	SatDetails []PairAnalysis `json:"sat_details"`
	ShaDetails []PairAnalysis `json:"sha_details"`
}

// PairAnalysis represents the database record from 'numbers' table
type PairAnalysis struct {
	PairNumber string `json:"pair_number"`
	PairType   string `json:"pair_type"`
	PairPoint  int    `json:"pair_point"`
	IsGood     bool   `json:"is_good"` // Helper for UI
}

// CharValue represents values for a single character
type CharValue struct {
	Char     string `json:"char"`
	SatValue int    `json:"sat_value"`
	ShaValue int    `json:"sha_value"`
	IsKaki   bool   `json:"is_kaki"`
}

// RecommendResult represents a name recommendation with distance and scores
type RecommendResult struct {
	NameID        int      `json:"name_id"`
	ThName        string   `json:"th_name"`
	Distance      float64  `json:"distance,omitempty"`
	SatSum        int      `json:"sat_sum"`
	ShaSum        int      `json:"sha_sum"`
	IsGoodSat     bool     `json:"is_good_sat"`
	IsGoodSha     bool     `json:"is_good_sha"`
	SatMeanings   []string `json:"sat_meanings,omitempty"`
	ShaMeanings   []string `json:"sha_meanings,omitempty"`
	NameSatSum    int      `json:"name_sat_sum"`
	SurnameSatSum int      `json:"surname_sat_sum"`

	NameShaSum    int `json:"name_sha_sum"`
	SurnameShaSum int `json:"surname_sha_sum"`

	SurnameSatMeanings []string `json:"surname_sat_meanings,omitempty"`
	SurnameShaMeanings []string `json:"surname_sha_meanings,omitempty"`

	TotalSatMeanings []string `json:"total_sat_meanings,omitempty"`
	TotalShaMeanings []string `json:"total_sha_meanings,omitempty"`
}

// SpellingRecommendation represents the enhanced result with pair meanings
type SpellingRecommendation struct {
	NameMiracle
	SatMeanings []string `json:"sat_meanings"` // e.g., ["Good", "Bad"]
	ShaMeanings []string `json:"sha_meanings"`
}

// ChatSession represents a conversation session
type ChatSession struct {
	SessionID string    `json:"session_id"`
	GuestName string    `json:"guest_name"`
	UserID    int       `json:"user_id,omitempty"`
	CreatedAt time.Time `json:"created_at"`
	LastMsgAt time.Time `json:"last_message_at"`
}

// ChatMessage represents a single chat message
type ChatMessage struct {
	MessageID    int       `json:"message_id"`
	SessionID    string    `json:"session_id"`
	SenderType   string    `json:"sender_type"`   // 'customer' or 'admin'
	SenderName   string    `json:"sender_name"`   // Name of the sender
	SenderAvatar string    `json:"sender_avatar"` // Avatar ID of the sender
	Message      string    `json:"message"`
	ImageURL     string    `json:"image_url"`
	IsRead       bool      `json:"is_read"`
	VipStatus    string    `json:"vip_status"` // 'normal', 'silver', 'gold', 'diamond', 'vvip', etc.
	CreatedAt    time.Time `json:"created_at"`
}

// BuddhaPang represents the 'buddha_pangs' table
type BuddhaPang struct {
	ID          int    `json:"id"`
	PangName    string `json:"pang_name"`
	BuddhaDay   int    `json:"buddha_day"`
	Description string `json:"description"`
	ImageURL    string `json:"image_url"`
}

// NininDreamData represents the dream interpretation result for the chat
type NininDreamData struct {
	Keyword        string `json:"keyword"`
	Interpretation string `json:"interpretation"`
	LuckyNumbers   string `json:"lucky_numbers"`
}

// NininChatResponse represents the unified response for Ninin AI
type NininChatResponse struct {
	Reply             string            `json:"reply"`
	Intent            string            `json:"intent"` // DREAM, NAME, OTHER
	DreamFound        bool              `json:"dream_found"`
	DreamData         *NininDreamData   `json:"dream_data,omitempty"`
	NameFound         bool              `json:"name_found"`
	NameData          []RecommendResult `json:"name_data,omitempty"`
	NininPersona      map[string]string `json:"ninin_persona"`
	ShowConsultButton bool              `json:"show_consult_button"`
	ShowPackages      bool              `json:"show_packages"`
	UsageCount        *int              `json:"usage_count,omitempty"`
	FreeLimit         *int              `json:"free_limit,omitempty"`
	FreeRemaining     *int              `json:"free_remaining,omitempty"`
}

// ShopOrder represents the 'shop_orders' table
type ShopOrder struct {
	ID            int       `json:"id"`
	RefNo         string    `json:"ref_no"`
	UserID        *int      `json:"user_id"`
	ProductDetail string    `json:"product_detail"`
	Amount        float64   `json:"amount"`
	Status        string    `json:"status"`
	CreatedAt     time.Time `json:"created_at"`
	UpdatedAt     time.Time `json:"updated_at"`
}

// NamingExample represents a celebrity or example name with an avatar
type NamingExample struct {
	ID          int       `json:"id"`
	Name        string    `json:"name"`
	AvatarURL   string    `json:"avatar_url"`
	IsCelebrity bool      `json:"is_celebrity"`
	SortOrder   int       `json:"sort_order"`
	CreatedAt   time.Time `json:"created_at"`
}

// UserSavedName represents a name saved by a user for later viewing
type UserSavedName struct {
	ID        int       `json:"id"`
	UserID    int       `json:"user_id"` // 0 if guest/not logged in (we can use device name or just user_id)
	Name      string    `json:"name"`
	SatSum    int       `json:"sat_sum"`
	ShaSum    int       `json:"sha_sum"`
	IsSatGood bool      `json:"is_sat_good"`
	IsShaGood bool      `json:"is_sha_good"`
	RootWord  string    `json:"root_word"`
	Meaning   string    `json:"meaning"`
	Analysis  string    `json:"analysis"`
	CreatedAt time.Time `json:"created_at"`
	DeviceID  string    `json:"device_id"` // To support guests
}

// Article represents the 'articles' table
type Article struct {
	ArtID       int       `json:"art_id"`
	Slug        string    `json:"slug"`
	Title       string    `json:"title"`
	Excerpt     string    `json:"excerpt"`
	Category    string    `json:"category"`
	ImageURL    string    `json:"image_url"`
	PublishedAt time.Time `json:"published_at"`
	IsPublished bool      `json:"is_published"`
	Content     string    `json:"content"`
	TitleShort  string    `json:"title_short"`
	PinOrder    int       `json:"pin_order"`
}
