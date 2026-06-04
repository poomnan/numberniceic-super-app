package database

import (
	"database/sql"
	"fmt"
	"log"
	"os"
	"strconv"
	"time"

	_ "github.com/go-sql-driver/mysql"
	_ "github.com/lib/pq"
)

var DB *sql.DB
var MySQLDB *sql.DB

func GetMySQLDB() *sql.DB {
	if MySQLDB == nil {
		ConnectMySQL()
	}
	return MySQLDB
}

func ConnectMySQL() {
	mysqlDSN := os.Getenv("MYSQL_DSN")
	if mysqlDSN == "" {
		mysqlDSN = "zoqlszwh_ananyadb:IntelliP24.X@tcp(127.0.0.1:3306)/zoqlszwh_ananyadb"
	}

	var err error
	MySQLDB, err = sql.Open("mysql", mysqlDSN)
	if err != nil {
		log.Printf("Warning: MySQL connection failed: %v", err)
		return
	}

	if err := MySQLDB.Ping(); err != nil {
		log.Printf("Warning: MySQL ping failed: %v", err)
		MySQLDB = nil
		return
	}

	// Set pool settings for 100+ concurrent users
	MySQLDB.SetMaxOpenConns(30)
	MySQLDB.SetMaxIdleConns(10)
	MySQLDB.SetConnMaxLifetime(10 * time.Minute)
	MySQLDB.SetConnMaxIdleTime(5 * time.Minute)

	// Initialize MySQL tables used by the Android app/admin panel (idempotent)
	initMySQLAstroTermsDictionaryTable()

	fmt.Println("MySQL connection established")
}

func initMySQLAstroTermsDictionaryTable() {
	if MySQLDB == nil {
		return
	}

	// Dictionary for Thai astrology term strings shown in the apps (calendar/bottom sheet/etc).
	// We keep a stable `term_key` and allow admin to edit display_name/description without code changes.
	//
	// Using MySQL here because the app/admin already uses membertb/bagcolortb on the same DB.
	createTable := `
	CREATE TABLE IF NOT EXISTS astro_terms_dictionary (
		id INT AUTO_INCREMENT PRIMARY KEY,
		term_key VARCHAR(80) NOT NULL UNIQUE,
		display_name VARCHAR(255) NOT NULL,
		description TEXT,
		is_active TINYINT(1) NOT NULL DEFAULT 1,
		created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
		updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
		INDEX idx_display_name (display_name)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
	`
	if _, err := MySQLDB.Exec(createTable); err != nil {
		log.Printf("Warning: initMySQLAstroTermsDictionaryTable create failed: %v", err)
		return
	}

	// Seed a minimal baseline to make the admin screen immediately useful.
	// Safe to run on every boot (INSERT IGNORE).
	type seed struct {
		Key, Name, Desc string
	}
	seeds := []seed{
		{Key: "WAN_TONGCHAI", Name: "วันธงชัย", Desc: "เหมาะสำหรับเริ่มต้นสิ่งใหม่ งานมงคล และการตัดสินใจสำคัญ"},
		{Key: "WAN_ATIPBADEE", Name: "วันอธิบดี", Desc: "เหมาะสำหรับงานสำคัญ การเจรจา การเริ่มต้นกิจการ"},
		{Key: "WAN_RIANGMON", Name: "วันเรียงหมอน", Desc: "เหมาะสำหรับพิธีแต่งงาน/มงคลสมรส"},
		{Key: "WAN_WANPRA", Name: "วันพระ", Desc: "วันสำคัญทางพระพุทธศาสนา เหมาะแก่การทำบุญ รักษาศีล และปฏิบัติธรรม"},
		{Key: "WAN_LOY", Name: "วันลอย", Desc: "วันลอย (ตามการคำนวณในระบบ)"},
		{Key: "WAN_FU", Name: "วันฟู", Desc: "วันฟู (ตามการคำนวณในระบบ)"},
	}

	// Avoid heavy multi-row statements to keep compatibility and error isolation.
	for _, s := range seeds {
		_, _ = MySQLDB.Exec(
			"INSERT IGNORE INTO astro_terms_dictionary (term_key, display_name, description, is_active, created_at, updated_at) VALUES (?, ?, ?, 1, ?, ?)",
			s.Key, s.Name, s.Desc, time.Now(), time.Now(),
		)
	}
}

func Connect() {
	connStr := os.Getenv("DATABASE_URL")
	if connStr == "" {
		connStr = "postgres://tayap:IntelliP24.X@localhost/tayap?sslmode=disable"
	}

	var err error
	DB, err = sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("Error opening database connection: %v", err)
	}

	// Keep the app below PostgreSQL's common default max_connections=100.
	// Mobile search can run two DB pools per active search, so 60 open
	// connections supports the guarded search concurrency while leaving room for
	// admin, chat, health, and maintenance queries. Operators can raise this
	// with DB_MAX_OPEN_CONNS only after confirming PostgreSQL capacity.
	DB.SetMaxOpenConns(envInt("DB_MAX_OPEN_CONNS", 60))
	DB.SetMaxIdleConns(envInt("DB_MAX_IDLE_CONNS", 20))
	DB.SetConnMaxLifetime(30 * time.Minute)
	DB.SetConnMaxIdleTime(5 * time.Minute)

	if err := DB.Ping(); err != nil {
		log.Fatalf("Error connecting to database: %v", err)
	}

	// Initialize Chat Tables if not exist
	initChatTables()
	initProductTables()
	initOrderTables()
	initNamingExampleTables()
	initUserSavedNamesTables()
	initArticlesTable()
	initSemanticSearchIdeasTable()

	fmt.Println("Database connection established")
}

func envInt(key string, fallback int) int {
	raw := os.Getenv(key)
	if raw == "" {
		return fallback
	}
	value, err := strconv.Atoi(raw)
	if err != nil || value <= 0 {
		return fallback
	}
	return value
}

func initArticlesTable() {
	query := `
	CREATE TABLE IF NOT EXISTS articles (
		art_id SERIAL PRIMARY KEY,
		slug VARCHAR(255) UNIQUE NOT NULL,
		title VARCHAR(255) NOT NULL,
		excerpt VARCHAR(500) NOT NULL,
		category VARCHAR(50) NOT NULL,
		image_url VARCHAR(255) NOT NULL,
		published_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
		is_published BOOLEAN DEFAULT true,
		content TEXT NOT NULL,
		title_short VARCHAR(255),
		pin_order INTEGER DEFAULT 0
	);
	`
	_, err := DB.Exec(query)
	if err != nil {
		fmt.Printf("Warning: Articles table init failed: %v\n", err)
	}
}

func initUserSavedNamesTables() {
	query := `
	CREATE TABLE IF NOT EXISTS user_saved_names (
		id SERIAL PRIMARY KEY,
		user_id INTEGER DEFAULT 0,
		name TEXT NOT NULL,
		sat_sum INTEGER,
		sha_sum INTEGER,
		is_sat_good BOOLEAN,
		is_sha_good BOOLEAN,
		root_word TEXT,
		meaning TEXT,
		analysis TEXT,
		device_id TEXT,
		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);
	CREATE INDEX IF NOT EXISTS idx_user_saved_names_user_id ON user_saved_names(user_id);
	CREATE INDEX IF NOT EXISTS idx_user_saved_names_device_id ON user_saved_names(device_id);
	`
	_, err := DB.Exec(query)
	if err != nil {
		fmt.Printf("Warning: User saved names tables init failed: %v\n", err)
	}
	// Migration to add meaning column if it doesn't already exist
	DB.Exec("ALTER TABLE user_saved_names ADD COLUMN IF NOT EXISTS meaning TEXT")
}

func initOrderTables() {
	query := `
	CREATE TABLE IF NOT EXISTS shop_orders (
		id SERIAL PRIMARY KEY,
		ref_no TEXT UNIQUE NOT NULL,
		user_id INTEGER,
		product_id INTEGER,
		product_detail TEXT,
		amount DECIMAL(10, 2) NOT NULL,
		status TEXT DEFAULT 'pending', -- pending, paid, cancelled
		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
		updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);
	`
	_, err := DB.Exec(query)
	if err != nil {
		fmt.Printf("Warning: Order tables init failed: %v\n", err)
	}

	// Migration: Add guest_id and product_id if not exists
	DB.Exec("ALTER TABLE shop_orders ADD COLUMN IF NOT EXISTS guest_id TEXT")
	DB.Exec("ALTER TABLE shop_orders ADD COLUMN IF NOT EXISTS product_id INTEGER")
	DB.Exec("ALTER TABLE shop_orders ADD COLUMN IF NOT EXISTS shipping_status TEXT DEFAULT 'none'")
}

func initProductTables() {
	query := `
	CREATE TABLE IF NOT EXISTS shop_product_categories (
		id SERIAL PRIMARY KEY,
		name TEXT NOT NULL,
		description TEXT,
		image_url TEXT,
		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);

	CREATE TABLE IF NOT EXISTS shop_products (
		id SERIAL PRIMARY KEY,
		category_id INTEGER REFERENCES shop_product_categories(id),
		name TEXT NOT NULL,
		description TEXT,
		price DECIMAL(10, 2) NOT NULL,
		image_url TEXT,
		stock_quantity INTEGER DEFAULT 0,
		is_active BOOLEAN DEFAULT true,
		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
		updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);
	`
	_, err := DB.Exec(query)
	if err != nil {
		fmt.Printf("Warning: Product tables init failed: %v\n", err)
	}

	// Ensure missing columns exist (migration workarounds)
	DB.Exec("ALTER TABLE shop_products ADD COLUMN IF NOT EXISTS image_url TEXT")
	DB.Exec("ALTER TABLE shop_products ADD COLUMN IF NOT EXISTS stock_quantity INTEGER DEFAULT 0")
	DB.Exec("ALTER TABLE shop_products ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true")
	DB.Exec("ALTER TABLE shop_products ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	DB.Exec("ALTER TABLE shop_product_categories ADD COLUMN IF NOT EXISTS image_url TEXT")

	// Verify column existence
	var exists bool
	err_check := DB.QueryRow("SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='shop_products' AND column_name='category_id')").Scan(&exists)
	fmt.Printf("DEBUG: shop_products.category_id exists: %v (err: %v)\n", exists, err_check)

	// Insert default categories if empty
	var count int
	DB.QueryRow("SELECT COUNT(*) FROM shop_product_categories").Scan(&count)
	if count == 0 {
		// Use individual inserts to guarantee IDs 1, 2, 3, 4
		DB.Exec("INSERT INTO shop_product_categories (id, name, description) VALUES ($1, $2, $3)", 1, "พลอยเพทาย", "หมวดหมู่สินค้าพลอยเพทาย")
		DB.Exec("INSERT INTO shop_product_categories (id, name, description) VALUES ($1, $2, $3)", 2, "ทำนายฝัน", "หมวดหมู่สินค้าทำนายฝัน")
		DB.Exec("INSERT INTO shop_product_categories (id, name, description) VALUES ($1, $2, $3)", 3, "เบอร์โทร", "หมวดหมู่สินค้าเบอร์โทรศัพท์")
		DB.Exec("INSERT INTO shop_product_categories (id, name, description) VALUES ($1, $2, $3)", 4, "ทะเบียนรถ", "หมวดหมู่สินค้าทะเบียนรถ")

		// Reset serial sequence if needed (PostgreSQL specific)
		DB.Exec("SELECT setval('shop_product_categories_id_seq', (SELECT MAX(id) FROM shop_product_categories))")
	}
}

func initChatTables() {
	query := `
	CREATE TABLE IF NOT EXISTS chat_sessions (
		session_id TEXT PRIMARY KEY,
		guest_name TEXT,
		user_id INTEGER,
		fcm_token TEXT,
		device_id TEXT,
		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
		last_message_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);

	CREATE TABLE IF NOT EXISTS chat_messages (
		message_id SERIAL PRIMARY KEY,
		session_id TEXT REFERENCES chat_sessions(session_id),
		sender_type TEXT,
		message_text TEXT NOT NULL,
		image_url TEXT,
		is_read BOOLEAN DEFAULT false,
		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);

    -- Indexes for performance
    CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);
    CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON chat_messages(created_at);
    CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_id ON chat_sessions(user_id);
    CREATE TABLE IF NOT EXISTS guest_usage (
        guest_id TEXT PRIMARY KEY,
        message_count INTEGER DEFAULT 0,
        last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

	`
	_, err := DB.Exec(query)
	if err != nil {
		fmt.Printf("Warning: Chat tables init failed: %v\n", err)
	}
}

func initNamingExampleTables() {
	query := `
	CREATE TABLE IF NOT EXISTS naming_examples (
		id SERIAL PRIMARY KEY,
		name TEXT NOT NULL,
		avatar_url TEXT,
		is_celebrity BOOLEAN DEFAULT true,
		sort_order INTEGER DEFAULT 0,
		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);
	`
	_, err := DB.Exec(query)
	if err != nil {
		fmt.Printf("Warning: Naming example tables init failed: %v\n", err)
	}
}

func initSemanticSearchIdeasTable() {
	query := `
	CREATE TABLE IF NOT EXISTS semantic_search_ideas (
		id SERIAL PRIMARY KEY,
		text TEXT NOT NULL,
		icon_name VARCHAR(100) DEFAULT 'sparkles',
		icon_color VARCHAR(50) DEFAULT '#E2B237',
		sort_order INTEGER DEFAULT 0,
		is_active BOOLEAN DEFAULT true,
		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
		updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
	);
	`
	_, err := DB.Exec(query)
	if err != nil {
		fmt.Printf("Warning: Semantic search ideas table init failed: %v\n", err)
	}
}

