CREATE TABLE IF NOT EXISTS articles (
    art_id INT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(255) UNIQUE,
    title VARCHAR(255) NOT NULL,
    excerpt TEXT,
    category VARCHAR(255),
    image_url VARCHAR(255),
    published_at DATETIME,
    is_published TINYINT(1) DEFAULT 0,
    content LONGTEXT,
    title_short VARCHAR(255),
    pin_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
