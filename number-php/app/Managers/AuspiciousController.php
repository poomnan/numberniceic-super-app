<?php

namespace App\Managers;

use PDO;

class AuspiciousController extends Manager
{
    // API: Assign Auspicious Info to User
    public function assignToUserApi($request, $response)
    {
        $post = $request->getParsedBody();
        $id = $post['id'] ?? null;
        $memberid = $post['memberid'] ?? '';
        $type = $post['type'] ?? 'year'; // year or life
        $title = $post['title'] ?? '';
        $description = $post['description'] ?? '';
        $expiry_duration = $post['expiry_duration'] ?? null;
        $image_url = '';

        if (!$memberid || !$title) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Missing fields']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        $this->ensureTable();
        $this->db->exec("SET time_zone = '+07:00'");

        // Calculate expires_at
        $expires_at_val = null;
        if ($type === 'year') {
            if ($expiry_duration !== null) {
                $expires_at_val = date('Y-m-d H:i:s', strtotime("+$expiry_duration minutes"));
            } else {
                // Default: Reset on Birthday (New Age Year)
                $stmtUser = $this->db->prepare("SELECT birthday FROM membertb WHERE memberid = :mid");
                $stmtUser->execute([':mid' => $memberid]);
                $userProfile = $stmtUser->fetch(PDO::FETCH_OBJ);

                if ($userProfile && !empty($userProfile->birthday)) {
                    $dob = new \DateTime($userProfile->birthday);
                    $now = new \DateTime();
                    // Project birthday onto current year, expire at START of birthday (00:00:00)
                    $nextBday = new \DateTime($now->format('Y') . '-' . $dob->format('m-d') . ' 00:00:00');

                    // If birthday already passed this year, it expires next year
                    if ($nextBday < $now) {
                        $nextBday->modify('+1 year');
                    }
                    $expires_at_val = $nextBday->format('Y-m-d H:i:s');
                }
            }
        }

        if ($id) {
            // UPDATE EXISTING
            if ($expires_at_val) {
                $sql = "UPDATE user_auspicious_assign SET type = :t, title = :title, description = :desc, assigned_at = NOW(), expires_at = :expires WHERE id = :id";
                $params = ['t' => $type, 'title' => $title, 'desc' => $description, 'expires' => $expires_at_val, 'id' => $id];
            } else {
                $sql = "UPDATE user_auspicious_assign SET type = :t, title = :title, description = :desc, assigned_at = NOW(), expires_at = NULL WHERE id = :id";
                $params = ['t' => $type, 'title' => $title, 'desc' => $description, 'id' => $id];
            }
            $this->db->prepare($sql)->execute($params);
        } else {
            // INSERT NEW
            if ($expires_at_val) {
                $sql = "INSERT INTO user_auspicious_assign (memberid, type, title, description, image_url, assigned_at, expires_at) VALUES (:mid, :t, :title, :desc, :img, NOW(), :expires)";
                $params = ['mid' => $memberid, 't' => $type, 'title' => $title, 'desc' => $description, 'img' => '', 'expires' => $expires_at_val];
            } else {
                $sql = "INSERT INTO user_auspicious_assign (memberid, type, title, description, image_url, assigned_at) VALUES (:mid, :t, :title, :desc, :img, NOW())";
                $params = ['mid' => $memberid, 't' => $type, 'title' => $title, 'desc' => $description, 'img' => ''];
            }
            $this->db->prepare($sql)->execute($params);
        }

        $this->notifyUser($memberid, $type, $title);

        $response->getBody()->write(json_encode(['status' => 'success']));
        return $response->withHeader('Content-Type', 'application/json');
    }

    // API: Get Assigned Info for Client
    public function getAssigned($request, $response, $args)
    {
        $memberid = $args['memberid'];
        $type = $request->getQueryParams()['type'] ?? null;

        $this->ensureTable();
        $this->db->exec("SET time_zone = '+07:00'");

        // Trigger passive cleanup for all expired assignments & past age bag colors
        try {
            $nc = new NotificationController($this->container);
            $nc->runCleanupLogic();
        } catch (\Exception $e) {
            // Ignore errors
        }

        $sql = "SELECT * FROM user_auspicious_assign WHERE memberid = :mid";
        $params = ['mid' => $memberid];

        if ($type) {
            $sql .= " AND type = :t";
            $params['t'] = $type;
            if ($type == 'year') {
                $sql .= " AND (expires_at IS NULL OR expires_at >= NOW())";
            }
        } else {
            $sql .= " AND (type = 'life' OR (type = 'year' AND (expires_at IS NULL OR expires_at >= NOW())))";
        }

        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);
        $items = $stmt->fetchAll(PDO::FETCH_OBJ);

        $response->getBody()->write(json_encode($items));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function getAssignedHistoryAll($request, $response, $args)
    {
        $memberid = $args['memberid'];
        $type = $request->getQueryParams()['type'] ?? null;

        $this->ensureTable();
        $this->db->exec("SET time_zone = '+07:00'");

        $sql = "SELECT * FROM user_auspicious_assign WHERE memberid = :mid";
        $params = ['mid' => $memberid];
        if ($type) {
            $sql .= " AND type = :t";
            $params['t'] = $type;
        }
        $sql .= " ORDER BY assigned_at DESC, id DESC";

        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);
        $items = $stmt->fetchAll(PDO::FETCH_OBJ);

        $response->getBody()->write(json_encode($items));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function deleteAssignment($request, $response)
    {
        $post = $request->getParsedBody();
        $id = $post['id'] ?? null;

        if (!$id) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Missing assignment id']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        $stmt = $this->db->prepare("DELETE FROM user_auspicious_assign WHERE id = :id");
        $stmt->execute([':id' => $id]);

        $response->getBody()->write(json_encode(['status' => 'success']));
        return $response->withHeader('Content-Type', 'application/json');
    }

    private function ensureTable()
    {
        $sql = "CREATE TABLE IF NOT EXISTS user_auspicious_assign (
            id INT AUTO_INCREMENT PRIMARY KEY,
            memberid VARCHAR(50) NOT NULL,
            type VARCHAR(20) NOT NULL,
            title VARCHAR(255),
            description TEXT,
            image_url TEXT,
            assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            expires_at TIMESTAMP NULL DEFAULT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        $this->db->exec($sql);

        try {
            $this->db->exec("ALTER TABLE user_auspicious_assign ADD COLUMN expires_at TIMESTAMP NULL DEFAULT NULL");
        } catch (\Exception $e) {
        }
    }

    private function notifyUser($memberid, $type, $title)
    {
        $label = ($type == 'year') ? 'ปีนี้' : 'ตลอดชีวิต';
        $notifTitle = "แจ้งเตือนวันมงคล ($label)";
        $body = "คุณนินได้ส่งข้อมูล $title ให้คุณแล้ว";

        // 1. Save to Database Notification System
        $nm = new NotificationManager($this->container);
        $nm->saveNotification(
            $memberid,
            'auspicious', // Custom type for app
            $notifTitle,
            $body,
            '',
            "Type: $type"
        );

        // 2. Send FCM Push Notification
        $stmt = $this->db->prepare("SELECT fcm_token FROM membertb WHERE memberid = :mid");
        $stmt->execute([':mid' => $memberid]);
        $user = $stmt->fetch(PDO::FETCH_OBJ);

        if ($user && !empty($user->fcm_token)) {
            $serviceAccountPath = __DIR__ . '/../../configs/service-account.json';
            if (!file_exists($serviceAccountPath))
                return;

            try {
                $scopes = ['https://www.googleapis.com/auth/firebase.messaging'];
                $credentials = new \Google\Auth\Credentials\ServiceAccountCredentials($scopes, $serviceAccountPath);
                $accessToken = $credentials->fetchAuthToken(\Google\Auth\HttpHandler\HttpHandlerFactory::build());
                $tokenValue = $accessToken['access_token'] ?? null;

                if ($tokenValue) {
                    $url = "https://fcm.googleapis.com/v1/projects/" . $credentials->getProjectId() . "/messages:send";
                    $message = [
                        'message' => [
                            'token' => $user->fcm_token,
                            'data' => [
                                'type' => 'auspicious',
                                'sub_type' => $type,
                                'memberid' => (string) $memberid,
                                'title' => $notifTitle,
                                'body' => $body
                            ]
                        ]
                    ];

                    $ch = curl_init();
                    curl_setopt($ch, CURLOPT_URL, $url);
                    curl_setopt($ch, CURLOPT_POST, true);
                    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($message));
                    curl_setopt($ch, CURLOPT_HTTPHEADER, ['Authorization: Bearer ' . $tokenValue, 'Content-Type: application/json']);
                    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
                    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
                    $fcmRes = curl_exec($ch);
                    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
                    curl_close($ch);

                    // Log the attempt
                    $logMsg = date('[Y-m-d H:i:s] ') . "Auspicious FCM | Target: " . substr($user->fcm_token, 0, 10) . "... | Status: $httpCode | Res: $fcmRes\n";
                    file_put_contents(__DIR__ . '/../../fcm_log.txt', $logMsg, FILE_APPEND);
                }
            } catch (\Exception $e) {
                file_put_contents(__DIR__ . '/../../fcm_log.txt', date('[Y-m-d H:i:s] ') . "Auspicious FCM Exception: " . $e->getMessage() . "\n", FILE_APPEND);
            }
        }
    }
}
