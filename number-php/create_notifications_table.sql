-- Create notifications table for storing user notifications
-- This table stores all notifications sent to users via FCM
-- Supports analytics (read status, read time) and multi-device sync

CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    member_id VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,  -- 'webview_merit', 'webview_changenum', 'webview_spell', 'bag_color', 'lucky_number', etc.
    title VARCHAR(255) NOT NULL,
    body TEXT,
    url TEXT,
    note TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_notifications_member_type ON notifications(member_id, type);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);
CREATE INDEX idx_notifications_member_created ON notifications(member_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(member_id, is_read) WHERE is_read = FALSE;

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_notifications_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_notifications_timestamp
    BEFORE UPDATE ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_notifications_updated_at();

-- Grant permissions (adjust as needed for your database user)
-- GRANT SELECT, INSERT, UPDATE ON notifications TO your_db_user;
-- GRANT USAGE, SELECT ON SEQUENCE notifications_id_seq TO your_db_user;
