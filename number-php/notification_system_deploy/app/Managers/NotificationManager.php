<?php

namespace App\Managers;

use PDO;

/**
 * NotificationManager
 * Manages notification storage and retrieval from database
 * Supports analytics (read status, read time) and multi-device sync
 */
class NotificationManager extends Manager
{
    /**
     * Save notification to database
     * Called when FCM notification is sent
     */
    public function saveNotification($memberId, $type, $title, $body, $url = null, $note = null)
    {
        try {
            $sql = "INSERT INTO notifications (member_id, type, title, body, url, note, created_at) 
                    VALUES (:member_id, :type, :title, :body, :url, :note, CURRENT_TIMESTAMP)";

            $stmt = $this->db->prepare($sql);
            $result = $stmt->execute([
                ':member_id' => $memberId,
                ':type' => $type,
                ':title' => $title,
                ':body' => $body,
                ':url' => $url,
                ':note' => $note
            ]);

            if ($result) {
                return $this->db->lastInsertId();
            }
            return false;
        } catch (\Exception $e) {
            error_log("NotificationManager::saveNotification Error: " . $e->getMessage());
            return false;
        }
    }

    /**
     * Get notifications for a specific user
     * Supports filtering by type and pagination
     */
    public function getNotifications($memberId, $type = null, $limit = 50, $offset = 0)
    {
        try {
            $sql = "SELECT * FROM notifications WHERE member_id = :member_id";

            if ($type) {
                $sql .= " AND type = :type";
            }

            $sql .= " ORDER BY created_at DESC LIMIT :limit OFFSET :offset";

            $stmt = $this->db->prepare($sql);
            $stmt->bindValue(':member_id', $memberId, PDO::PARAM_STR);

            if ($type) {
                $stmt->bindValue(':type', $type, PDO::PARAM_STR);
            }

            $stmt->bindValue(':limit', (int) $limit, PDO::PARAM_INT);
            $stmt->bindValue(':offset', (int) $offset, PDO::PARAM_INT);

            $stmt->execute();
            return $stmt->fetchAll(PDO::FETCH_OBJ);
        } catch (\Exception $e) {
            error_log("NotificationManager::getNotifications Error: " . $e->getMessage());
            return [];
        }
    }

    /**
     * Get notifications by type for a specific user
     * Used by Merit, Change, Spell sections
     */
    public function getNotificationsByType($memberId, $type, $limit = 50)
    {
        return $this->getNotifications($memberId, $type, $limit);
    }

    /**
     * Mark notification as read
     */
    public function markAsRead($notificationId, $memberId)
    {
        try {
            $sql = "UPDATE notifications 
                    SET is_read = TRUE, read_at = CURRENT_TIMESTAMP 
                    WHERE id = :id AND member_id = :member_id";

            $stmt = $this->db->prepare($sql);
            return $stmt->execute([
                ':id' => $notificationId,
                ':member_id' => $memberId
            ]);
        } catch (\Exception $e) {
            error_log("NotificationManager::markAsRead Error: " . $e->getMessage());
            return false;
        }
    }

    /**
     * Mark multiple notifications as read
     */
    public function markMultipleAsRead($notificationIds, $memberId)
    {
        try {
            if (empty($notificationIds)) {
                return false;
            }

            $placeholders = str_repeat('?,', count($notificationIds) - 1) . '?';
            $sql = "UPDATE notifications 
                    SET is_read = TRUE, read_at = CURRENT_TIMESTAMP 
                    WHERE id IN ($placeholders) AND member_id = ?";

            $stmt = $this->db->prepare($sql);
            $params = array_merge($notificationIds, [$memberId]);
            return $stmt->execute($params);
        } catch (\Exception $e) {
            error_log("NotificationManager::markMultipleAsRead Error: " . $e->getMessage());
            return false;
        }
    }

    /**
     * Get unread count for a user
     */
    public function getUnreadCount($memberId, $type = null)
    {
        try {
            $sql = "SELECT COUNT(*) as count FROM notifications 
                    WHERE member_id = :member_id AND is_read = FALSE";

            if ($type) {
                $sql .= " AND type = :type";
            }

            $stmt = $this->db->prepare($sql);
            $stmt->bindValue(':member_id', $memberId, PDO::PARAM_STR);

            if ($type) {
                $stmt->bindValue(':type', $type, PDO::PARAM_STR);
            }

            $stmt->execute();
            $result = $stmt->fetch(PDO::FETCH_OBJ);
            return $result ? (int) $result->count : 0;
        } catch (\Exception $e) {
            error_log("NotificationManager::getUnreadCount Error: " . $e->getMessage());
            return 0;
        }
    }

    /**
     * Delete old notifications (cleanup)
     * Keep only last N days of notifications
     */
    public function deleteOldNotifications($days = 90)
    {
        try {
            $sql = "DELETE FROM notifications 
                    WHERE created_at < NOW() - INTERVAL '$days days'";

            $stmt = $this->db->prepare($sql);
            return $stmt->execute();
        } catch (\Exception $e) {
            error_log("NotificationManager::deleteOldNotifications Error: " . $e->getMessage());
            return false;
        }
    }

    /**
     * Get notification statistics for analytics
     */
    public function getStatistics($memberId)
    {
        try {
            $sql = "SELECT 
                        type,
                        COUNT(*) as total,
                        SUM(CASE WHEN is_read = TRUE THEN 1 ELSE 0 END) as read_count,
                        SUM(CASE WHEN is_read = FALSE THEN 1 ELSE 0 END) as unread_count
                    FROM notifications 
                    WHERE member_id = :member_id
                    GROUP BY type";

            $stmt = $this->db->prepare($sql);
            $stmt->execute([':member_id' => $memberId]);
            return $stmt->fetchAll(PDO::FETCH_OBJ);
        } catch (\Exception $e) {
            error_log("NotificationManager::getStatistics Error: " . $e->getMessage());
            return [];
        }
    }
}
