<?php

namespace App\Managers;

use App\Managers\NotificationManager;

/**
 * NotificationAPIController
 * Handles API requests for notification management
 * Used by Android app to fetch, mark as read, and get statistics
 */
class NotificationAPIController extends Manager
{
    private $notificationManager;

    public function __construct()
    {
        parent::__construct();
        $this->notificationManager = new NotificationManager();
    }

    /**
     * GET /api/notifications
     * Fetch notifications for a user
     * Query params: memberid, type (optional), limit (optional), offset (optional)
     */
    public function getNotifications($request, $response)
    {
        try {
            $params = $request->getQueryParams();
            $memberId = $params['memberid'] ?? null;
            $type = $params['type'] ?? null;
            $limit = isset($params['limit']) ? (int) $params['limit'] : 50;
            $offset = isset($params['offset']) ? (int) $params['offset'] : 0;

            if (!$memberId) {
                $response->getBody()->write(json_encode([
                    'status' => 'error',
                    'message' => 'memberid is required'
                ]));
                return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
            }

            $notifications = $this->notificationManager->getNotifications($memberId, $type, $limit, $offset);

            $response->getBody()->write(json_encode([
                'status' => 'success',
                'data' => $notifications,
                'count' => count($notifications)
            ]));
            return $response->withHeader('Content-Type', 'application/json');

        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'status' => 'error',
                'message' => $e->getMessage()
            ]));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(500);
        }
    }

    /**
     * GET /api/notifications/by-type
     * Fetch notifications by type for a user
     * Query params: memberid, type, limit (optional)
     */
    public function getNotificationsByType($request, $response)
    {
        try {
            $params = $request->getQueryParams();
            $memberId = $params['memberid'] ?? null;
            $type = $params['type'] ?? null;
            $limit = isset($params['limit']) ? (int) $params['limit'] : 50;

            if (!$memberId || !$type) {
                $response->getBody()->write(json_encode([
                    'status' => 'error',
                    'message' => 'memberid and type are required'
                ]));
                return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
            }

            $notifications = $this->notificationManager->getNotificationsByType($memberId, $type, $limit);

            $response->getBody()->write(json_encode([
                'status' => 'success',
                'type' => $type,
                'data' => $notifications,
                'count' => count($notifications)
            ]));
            return $response->withHeader('Content-Type', 'application/json');

        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'status' => 'error',
                'message' => $e->getMessage()
            ]));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(500);
        }
    }

    /**
     * POST /api/notifications/mark-read
     * Mark notification(s) as read
     * Body: { "memberid": "xxx", "notification_id": 123 } or { "memberid": "xxx", "notification_ids": [1,2,3] }
     */
    public function markAsRead($request, $response)
    {
        try {
            $body = $request->getParsedBody();
            $memberId = $body['memberid'] ?? null;
            $notificationId = $body['notification_id'] ?? null;
            $notificationIds = $body['notification_ids'] ?? null;

            if (!$memberId) {
                $response->getBody()->write(json_encode([
                    'status' => 'error',
                    'message' => 'memberid is required'
                ]));
                return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
            }

            if ($notificationId) {
                // Single notification
                $result = $this->notificationManager->markAsRead($notificationId, $memberId);
            } elseif ($notificationIds && is_array($notificationIds)) {
                // Multiple notifications
                $result = $this->notificationManager->markMultipleAsRead($notificationIds, $memberId);
            } else {
                $response->getBody()->write(json_encode([
                    'status' => 'error',
                    'message' => 'notification_id or notification_ids is required'
                ]));
                return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
            }

            if ($result) {
                $response->getBody()->write(json_encode([
                    'status' => 'success',
                    'message' => 'Marked as read successfully'
                ]));
            } else {
                $response->getBody()->write(json_encode([
                    'status' => 'error',
                    'message' => 'Failed to mark as read'
                ]));
            }
            return $response->withHeader('Content-Type', 'application/json');

        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'status' => 'error',
                'message' => $e->getMessage()
            ]));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(500);
        }
    }

    /**
     * GET /api/notifications/unread-count
     * Get unread notification count
     * Query params: memberid, type (optional)
     */
    public function getUnreadCount($request, $response)
    {
        try {
            $params = $request->getQueryParams();
            $memberId = $params['memberid'] ?? null;
            $type = $params['type'] ?? null;

            if (!$memberId) {
                $response->getBody()->write(json_encode([
                    'status' => 'error',
                    'message' => 'memberid is required'
                ]));
                return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
            }

            $count = $this->notificationManager->getUnreadCount($memberId, $type);

            $response->getBody()->write(json_encode([
                'status' => 'success',
                'unread_count' => $count
            ]));
            return $response->withHeader('Content-Type', 'application/json');

        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'status' => 'error',
                'message' => $e->getMessage()
            ]));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(500);
        }
    }

    /**
     * GET /api/notifications/statistics
     * Get notification statistics for analytics
     * Query params: memberid
     */
    public function getStatistics($request, $response)
    {
        try {
            $params = $request->getQueryParams();
            $memberId = $params['memberid'] ?? null;

            if (!$memberId) {
                $response->getBody()->write(json_encode([
                    'status' => 'error',
                    'message' => 'memberid is required'
                ]));
                return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
            }

            $stats = $this->notificationManager->getStatistics($memberId);

            $response->getBody()->write(json_encode([
                'status' => 'success',
                'data' => $stats
            ]));
            return $response->withHeader('Content-Type', 'application/json');

        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'status' => 'error',
                'message' => $e->getMessage()
            ]));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(500);
        }
    }

    /**
     * POST /api/notifications/save
     * Save notification to database (called internally when FCM is sent)
     * Body: { "memberid": "xxx", "type": "xxx", "title": "xxx", "body": "xxx", "url": "xxx", "note": "xxx" }
     */
    public function saveNotification($request, $response)
    {
        try {
            $body = $request->getParsedBody();
            $memberId = $body['memberid'] ?? null;
            $type = $body['type'] ?? null;
            $title = $body['title'] ?? null;
            $bodyText = $body['body'] ?? null;
            $url = $body['url'] ?? null;
            $note = $body['note'] ?? null;

            if (!$memberId || !$type || !$title) {
                $response->getBody()->write(json_encode([
                    'status' => 'error',
                    'message' => 'memberid, type, and title are required'
                ]));
                return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
            }

            $notificationId = $this->notificationManager->saveNotification(
                $memberId,
                $type,
                $title,
                $bodyText,
                $url,
                $note
            );

            if ($notificationId) {
                $response->getBody()->write(json_encode([
                    'status' => 'success',
                    'notification_id' => $notificationId,
                    'message' => 'Notification saved successfully'
                ]));
            } else {
                $response->getBody()->write(json_encode([
                    'status' => 'error',
                    'message' => 'Failed to save notification'
                ]));
            }
            return $response->withHeader('Content-Type', 'application/json');

        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'status' => 'error',
                'message' => $e->getMessage()
            ]));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(500);
        }
    }
}
