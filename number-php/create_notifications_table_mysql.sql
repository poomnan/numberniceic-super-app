-- Create notifications table for MySQL
-- This table stores all notifications sent to users via FCM
-- Supports analytics (read status, read time) and multi-device sync

CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_id VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,  -- 'webview_merit', 'webview_changenum', 'webview_spell', 'bag_color', 'lucky_number', etc.
    title VARCHAR(255) NOT NULL,
    body TEXT,
    url TEXT,
    note TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notifications_member_type (member_id, type),
    INDEX idx_notifications_created (created_at DESC),
    INDEX idx_notifications_member_created (member_id, created_at DESC),
    INDEX idx_notifications_unread (member_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Note: MySQL doesn't need separate trigger for updated_at
-- It's handled by ON UPDATE CURRENT_TIMESTAMP in the column definition
