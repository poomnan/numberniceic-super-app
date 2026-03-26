<?php

namespace App\Managers;

use PDO;
use Slim\Views\PhpRenderer;

class InauspiciousController extends Manager
{
    // View: Admin Assignment Form
    public function viewAssign($request, $response)
    {
        return $this->container->get('view')->render($response, 'web_admin_inauspicious_assign.php', []);
    }

    // API: Assign Inauspicious Info to User
    public function assignToUser($request, $response)
    {
        $post = $request->getParsedBody();
        $memberid = $post['memberid'] ?? '';
        $type = $post['type'] ?? 'year'; // year or life
        $title = $post['title'] ?? '';
        $description = $post['description'] ?? '';
        $image_url = $post['image_url'] ?? ''; // If manually entered? 

        // 1. Lazy Create Table
        $this->ensureTable();

        // 2. Handle File Upload (If provided)
        $files = $request->getUploadedFiles();
        if (isset($files['image_file']) && $files['image_file']->getError() === UPLOAD_ERR_OK) {
            $uploadedFile = $files['image_file'];
            $extension = pathinfo($uploadedFile->getClientFilename(), PATHINFO_EXTENSION);
            $filename = sprintf('inauspicious_%s_%s.%s', $memberid, $type, $extension);
            $directory = __DIR__ . '/../../public/uploads/inauspicious';

            if (!is_dir($directory)) {
                mkdir($directory, 0777, true);
            }

            $uploadedFile->moveTo($directory . DIRECTORY_SEPARATOR . $filename);
            $image_url = '/uploads/inauspicious/' . $filename;
        }

        if (!$memberid || !$title) {
            // If submitted via Form, return view with error
            // If API, return JSON. Assuming Form for now based on "Create System at Admin Menu"
            // But actually, we probably want to return to the form with success/fail message.
            // Let's assume standard POST-Redirect or JSON.
            // If this is an AJAX form, JSON is better.
        }

        // 3. Upsert (One record per user per type)
        // Check existing
        $sqlCheck = "SELECT id FROM user_inauspicious_assign WHERE memberid = :mid AND type = :t";


        $existing = false;
        $existingId = 0;

        // Re-execute correctly
        $stmt = $this->db->prepare($sqlCheck);
        $stmt->execute(['mid' => $memberid, 't' => $type]);
        if ($row = $stmt->fetch(PDO::FETCH_OBJ)) {
            $existing = true;
            $existingId = $row->id;
        }

        if ($existing) {
            // Update
            $sql = "UPDATE user_inauspicious_assign SET title = :title, description = :desc, image_url = :img, assigned_at = NOW() WHERE id = :id";
            $arr = [
                'title' => $title,
                'desc' => $description,
                'img' => $image_url,
                'id' => $existingId
            ];
            // If no new image uploaded and no url provided, keep old?
            // Simple logic: If $image_url is empty, do we overwrite with empty?
            // Only update image if $image_url is provided.
            if (empty($image_url)) {
                $sql = "UPDATE user_inauspicious_assign SET title = :title, description = :desc, assigned_at = NOW() WHERE id = :id";
                unset($arr['img']);
            }
            $this->db->prepare($sql)->execute($arr);

        } else {
            // Insert
            $sql = "INSERT INTO user_inauspicious_assign (memberid, type, title, description, image_url, assigned_at) VALUES (:mid, :t, :title, :desc, :img, NOW())";
            $this->db->prepare($sql)->execute([
                'mid' => $memberid,
                't' => $type,
                'title' => $title,
                'desc' => $description,
                'img' => $image_url
            ]);
        }

        // 4. Notify User
        $this->notifyUser($memberid, $type, $title);

        // Redirect back or return JSON
        // If 'is_ajax'... let's assume standard Form Submit Redirect
        return $response->withHeader('Location', '/web/admin/inauspicious?status=success&memberid=' . $memberid)->withStatus(302);
    }

    public function assignToUserApi($request, $response)
    {
        $post = $request->getParsedBody();
        $memberid = $post['memberid'] ?? '';
        $type = $post['type'] ?? 'year';
        $title = $post['title'] ?? '';
        $description = $post['description'] ?? '';
        $expiry_duration = $post['expiry_duration'] ?? null; // In minutes
        $image_url = ''; // No image for API version as requested

        if (!$memberid || !$title) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Missing fields']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        $this->ensureTable();

        // Attempt to remove legacy Unique Key if it exists
        try {
            $this->db->exec("ALTER TABLE user_inauspicious_assign DROP INDEX user_type");
        } catch (\Exception $e) {
            // Index might not exist, ignore
        }

        // Calculate expires_at if expiry_duration is provided
        $expires_at = null;
        if ($expiry_duration !== null && $type === 'year') {
            // Convert minutes to MySQL INTERVAL
            $expires_at = "DATE_ADD(NOW(), INTERVAL $expiry_duration MINUTE)";
        }

        // Always Insert (Add new item)
        $this->db->exec("SET time_zone = '+07:00'");

        if ($expires_at) {
            $sql = "INSERT INTO user_inauspicious_assign (memberid, type, title, description, image_url, assigned_at, expires_at) VALUES (:mid, :t, :title, :desc, :img, NOW(), $expires_at)";
        } else {
            $sql = "INSERT INTO user_inauspicious_assign (memberid, type, title, description, image_url, assigned_at) VALUES (:mid, :t, :title, :desc, :img, NOW())";
        }

        $this->db->prepare($sql)->execute([
            'mid' => $memberid,
            't' => $type,
            'title' => $title,
            'desc' => $description,
            'img' => ''
        ]);

        $this->notifyUser($memberid, $type, $title);

        $response->getBody()->write(json_encode(['status' => 'success']));
        return $response->withHeader('Content-Type', 'application/json');
    }

    // API: Get Assigned Info for Client
    public function getAssigned($request, $response, $args)
    {
        $memberid = $args['memberid'];
        $type = $request->getQueryParams()['type'] ?? null; // Optional filter

        // Trigger passive cleanup for all expired assignments (if applicable)
        try {
            // Attempt to trigger cleanup if NotificationController exists and handles generic cleanups
            $nc = new NotificationController($this->container);
            if (method_exists($nc, 'runCleanupLogic')) {
                $nc->runCleanupLogic();
            }
        } catch (\Exception $e) {
            // Ignore errors
        }

        $this->ensureTable();

        $this->db->exec("SET time_zone = '+07:00'");

        $sql = "SELECT * FROM user_inauspicious_assign WHERE memberid = :mid";
        $params = ['mid' => $memberid];

        if ($type) {
            $sql .= " AND type = :t";
            $params['t'] = $type;

            if ($type == 'year') {
                // Check if expires_at exists and is in the future, or if no expires_at, use old logic
                $sql .= " AND (expires_at IS NULL OR expires_at >= NOW())";
            }
        } else {
            // If fetching all, apply logic: 
            // Return 'life' records OR 'year' records that are not expired
            $sql .= " AND (type = 'life' OR (type = 'year' AND (expires_at IS NULL OR expires_at >= NOW())))";
        }

        $stmt = $this->db->prepare($sql);
        $stmt->execute($params);
        $items = $stmt->fetchAll(PDO::FETCH_OBJ);

        $response->getBody()->write(json_encode($items));
        return $response->withHeader('Content-Type', 'application/json');
    }

    // Helper: Ensure Table Exists
    private function ensureTable()
    {
        $sql = "CREATE TABLE IF NOT EXISTS user_inauspicious_assign (
            id INT AUTO_INCREMENT PRIMARY KEY,
            memberid VARCHAR(50) NOT NULL,
            type VARCHAR(20) NOT NULL, -- 'year' or 'life'
            title VARCHAR(255),
            description TEXT,
            image_url TEXT,
            assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            expires_at TIMESTAMP NULL DEFAULT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        $this->db->exec($sql);

        // Add expires_at column if it doesn't exist (for existing tables)
        try {
            $this->db->exec("ALTER TABLE user_inauspicious_assign ADD COLUMN expires_at TIMESTAMP NULL DEFAULT NULL");
        } catch (\Exception $e) {
            // Column might already exist, ignore
        }
    }

    private function notifyUser($memberid, $type, $title)
    {
        $label = ($type == 'year') ? 'ปีนี้' : 'ตลอดชีวิต';
        $notifTitle = "แจ้งเตือนวันอัปมงคล ($label)";
        $body = "คุณนินได้ส่งข้อมูล $title ให้คุณแล้ว";

        // Save to DB
        $nm = new NotificationManager($this->container);
        $nm->saveNotification(
            $memberid,
            'inauspicious', // Custom type for app
            $notifTitle,
            $body,
            '',
            "Type: $type"
        );

        // Send FCM
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
                                'type' => 'inauspicious',
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
                    curl_exec($ch);
                    curl_close($ch);
                }
            } catch (\Exception $e) {
            }
        }
    }
}
