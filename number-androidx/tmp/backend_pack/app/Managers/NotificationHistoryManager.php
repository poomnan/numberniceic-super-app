<?php
namespace App\Managers;

use App\Managers\Manager;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

class NotificationHistoryManager extends Manager {

    // POST /api/notifications/save
    public function saveNotification(Request $request, Response $response) {
        $body = $request->getParsedBody();
        $userId = $body['user_id'] ?? '';
        $title = $body['title'] ?? '';
        $message = $body['body'] ?? $body['message'] ?? ''; // Support both keys
        $type = $body['type'] ?? null;
        $url = $body['url'] ?? null;

        if (empty($userId) || empty($title)) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Missing parameter: user_id or title']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        try {
            $sql = "INSERT INTO notification_history (user_id, title, body, type, url, created_at) VALUES (:uid, :title, :body, :type, :url, NOW())";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([
                ':uid' => $userId,
                ':title' => $title,
                ':body' => $message,
                ':type' => $type,
                ':url' => $url
            ]);

            $response->getBody()->write(json_encode(['status' => 'success']));
        } catch (\Exception $e) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => $e->getMessage()]));
        }
        return $response->withHeader('Content-Type', 'application/json');
    }

    // GET /api/notifications/list/{userId}
    public function getHistory(Request $request, Response $response, $args) {
        $userId = $args['userId'];
        
        try {
            // Check global notifications + user specific notifications
            // For simplicity, now we only check user specific or 'all'
            // We can check 'all' as user_id or handle it in query
            
            $sql = "SELECT * FROM notification_history WHERE user_id = :uid OR user_id = 'all' ORDER BY created_at DESC LIMIT 50";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([':uid' => $userId]);
            $list = $stmt->fetchAll();

            $response->getBody()->write(json_encode(['status' => 'success', 'data' => $list]));
        } catch (\Exception $e) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => $e->getMessage()]));
        }
        return $response->withHeader('Content-Type', 'application/json');
    }

    // POST /api/notifications/delete
    public function deleteNotification(Request $request, Response $response) {
        $body = $request->getParsedBody();
        $id = $body['id'] ?? 0;
        $userId = $body['user_id'] ?? ''; // Optional security check

        try {
            if (!empty($userId)) {
                $sql = "DELETE FROM notification_history WHERE id = :id AND user_id = :uid";
                $stmt = $this->db->prepare($sql);
                $stmt->execute([':id' => $id, ':uid' => $userId]);
            } else {
                $sql = "DELETE FROM notification_history WHERE id = :id";
                $stmt = $this->db->prepare($sql);
                $stmt->execute([':id' => $id]);
            }
            
            $response->getBody()->write(json_encode(['status' => 'success']));
        } catch (\Exception $e) {
             $response->getBody()->write(json_encode(['status' => 'error', 'message' => $e->getMessage()]));
        }
        return $response->withHeader('Content-Type', 'application/json');
    }
}
