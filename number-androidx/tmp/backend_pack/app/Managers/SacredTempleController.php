<?php

namespace App\Managers;

use PDO;
use Slim\Psr7\Response;
use Slim\Views\PhpRenderer;

class SacredTempleController extends Manager
{
    public function viewList($request, $response)
    {
        if (session_status() == PHP_SESSION_NONE)
            session_start();
        $status = $_SESSION['status'] ?? null;
        unset($_SESSION['status']);

        $stmt = $this->db->query("SELECT * FROM sacred_temple_tb ORDER BY id DESC");
        $items = $stmt->fetchAll(PDO::FETCH_OBJ);

        return $this->container->get('view')->render($response, 'admin_temple_list.php', [
            'items' => $items,
            'status' => $status
        ]);
    }

    public function viewAdd($request, $response)
    {
        return $this->container->get('view')->render($response, 'admin_temple_form.php', [
            'item' => null
        ]);
    }

    public function viewEdit($request, $response, $args)
    {
        $id = $args['id'];
        $stmt = $this->db->prepare("SELECT * FROM sacred_temple_tb WHERE id = :id");
        $stmt->execute(['id' => $id]);
        $item = $stmt->fetch(PDO::FETCH_OBJ);

        if (!$item) {
            return $response->withHeader('Location', '/admin/temple')->withStatus(302);
        }

        return $this->container->get('view')->render($response, 'admin_temple_form.php', [
            'item' => $item
        ]);
    }

    public function save($request, $response)
    {
        $post = $request->getParsedBody();
        $id = $post['id'] ?? null;
        $temple_name = $post['temple_name'] ?? '';
        $description = $post['description'] ?? '';
        $address = $post['address'] ?? '';
        $image_url = $post['image_url'] ?? '';

        // Handle file upload
        $files = $request->getUploadedFiles();
        if (isset($files['image_file'])) {
            $uploadedFile = $files['image_file'];
            if ($uploadedFile->getError() === UPLOAD_ERR_OK) {
                $extension = strtolower(pathinfo($uploadedFile->getClientFilename(), PATHINFO_EXTENSION));
                $filename = sprintf('temple_%s.%s', bin2hex(random_bytes(8)), $extension);

                // Use absolute path relative to project root
                $baseDir = dirname(__DIR__, 2);
                $directory = $baseDir . '/public/uploads/temple';

                if (!is_dir($directory)) {
                    @mkdir($directory, 0755, true);
                }

                if (is_writable($directory)) {
                    $targetPath = $directory . DIRECTORY_SEPARATOR . $filename;
                    $uploadedFile->moveTo($targetPath);
                    @chmod($targetPath, 0644); // Ensure web server can read it
                    $image_url = '/public/uploads/temple/' . $filename;
                } else {
                    error_log("Upload failed: Directory $directory is not writable.");
                }
            } elseif ($uploadedFile->getError() !== UPLOAD_ERR_NO_FILE) {
                $errorMsg = "SACRED TEMPLE UPLOAD ERROR: Code " . $uploadedFile->getError();
                error_log($errorMsg);
            }
        }

        if (session_status() == PHP_SESSION_NONE)
            session_start();
        if ($id) {
            $sql = "UPDATE sacred_temple_tb SET temple_name = :n, description = :desc, address = :addr, image_url = :img WHERE id = :id";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([
                'n' => $temple_name,
                'desc' => $description,
                'addr' => $address,
                'img' => $image_url,
                'id' => $id
            ]);
            $_SESSION['status'] = ['type' => 'success', 'message' => 'แก้ไขข้อมูลวัดเรียบร้อยแล้ว'];
        } else {
            $sql = "INSERT INTO sacred_temple_tb (temple_name, description, address, image_url) VALUES (:n, :desc, :addr, :img)";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([
                'n' => $temple_name,
                'desc' => $description,
                'addr' => $address,
                'img' => $image_url
            ]);
            $_SESSION['status'] = ['type' => 'success', 'message' => 'เพิ่มข้อมูลวัดใหม่เรียบร้อยแล้ว'];
        }

        return $response->withHeader('Location', '/admin/temple')->withStatus(302);
    }

    public function delete($request, $response, $args)
    {
        $id = $args['id'];
        $stmt = $this->db->prepare("DELETE FROM sacred_temple_tb WHERE id = :id");
        $stmt->execute(['id' => $id]);

        return $response->withHeader('Location', '/admin/temple')->withStatus(302);
    }

    // API: Get all Temples for Mobile App
    public function getTemplesApi($request, $response)
    {
        $stmt = $this->db->query("SELECT * FROM sacred_temple_tb ORDER BY id DESC");
        $items = $stmt->fetchAll(PDO::FETCH_OBJ);

        $response->getBody()->write(json_encode($items));
        return $response->withHeader('Content-Type', 'application/json');
    }

    // API: Assign Temple to User
    public function assignToUser($request, $response)
    {
        $body = $request->getParsedBody();
        $memberid = $body['memberid'] ?? $body['member_id'] ?? null;
        $temple_id = $body['temple_id'] ?? null;
        $custom_desc = $body['custom_description'] ?? '';

        // Lazy ensure table exists
        $this->ensureAssignTable();

        if (!$memberid || !$temple_id) {
            $response->getBody()->write(json_encode(['status' => 'fail', 'message' => 'Missing parameters']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        // Check if already assigned
        $checkSql = "SELECT id FROM user_temple_assign WHERE memberid = :mid AND temple_id = :tid";
        $checkStmt = $this->db->prepare($checkSql);
        $checkStmt->execute([':mid' => $memberid, ':tid' => $temple_id]);
        $existing = $checkStmt->fetch(PDO::FETCH_OBJ);

        if ($existing) {
            // Update timestamp only
            $sql = "UPDATE user_temple_assign SET assigned_at = NOW(), custom_description = :desc WHERE id = :id";
            $stmt = $this->db->prepare($sql);
            $success = $stmt->execute([
                ':desc' => $custom_desc,
                ':id' => $existing->id
            ]);
        } else {
            // Insert new
            $sql = "INSERT INTO user_temple_assign (memberid, temple_id, custom_description, assigned_at) 
                    VALUES (:mid, :tid, :desc, NOW())";
            $stmt = $this->db->prepare($sql);
            $success = $stmt->execute([
                ':mid' => $memberid,
                ':tid' => $temple_id,
                ':desc' => $custom_desc
            ]);
        }

        if ($success) {
            $this->notifyUser($memberid, $temple_id);
        }

        $response->getBody()->write(json_encode(['activity' => $success ? 'success' : 'fail']));
        return $response->withHeader('Content-Type', 'application/json');
    }



    private function notifyUser($memberid, $temple_id)
    {
        try {
            // Get Token
            $stmt = $this->db->prepare("SELECT fcm_token FROM membertb WHERE memberid = :mid");
            $stmt->execute([':mid' => $memberid]);
            $user = $stmt->fetch(PDO::FETCH_OBJ);

            // Get Temple Name
            $stmt = $this->db->prepare("SELECT temple_name FROM sacred_temple_tb WHERE id = :id");
            $stmt->execute([':id' => $temple_id]);
            $temple = $stmt->fetch(PDO::FETCH_OBJ);
            $templeName = $temple ? $temple->temple_name : "วัดศักดิ์สิทธิ์";

            $title = "แนะนำวัดเก่าวัดศักดิ์สิทธิ์";
            $body = "คุณนินให้ท่านไปสักการะ $templeName เพื่อความเป็นสิริมงคล";

            // Save to Database
            $nm = new NotificationManager($this->container);
            $nm->saveNotification(
                $memberid,
                'temple_assign',
                $title,
                $body,
                '', // URL
                "Temple ID: $temple_id"
            );

            if ($user && !empty($user->fcm_token)) {

                // FCM Logic (Simplified Reuse)
                $serviceAccountPath = __DIR__ . '/../../configs/service-account.json';
                if (!file_exists($serviceAccountPath))
                    return;

                $scopes = ['https://www.googleapis.com/auth/firebase.messaging'];
                $credentials = new \Google\Auth\Credentials\ServiceAccountCredentials($scopes, $serviceAccountPath);
                $accessToken = $credentials->fetchAuthToken(\Google\Auth\HttpHandler\HttpHandlerFactory::build());
                $tokenValue = $accessToken['access_token'] ?? null;
                if (!$tokenValue)
                    return;

                $url = "https://fcm.googleapis.com/v1/projects/" . $credentials->getProjectId() . "/messages:send";
                $message = [
                    'message' => [
                        'token' => $user->fcm_token,
                        'data' => [
                            'type' => 'temple_assign',
                            'memberid' => (string) $memberid,
                            'title' => $title,
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

    // API: Assign Merit (Notification + WebView)
    public function assignMerit($request, $response)
    {
        $body = $request->getParsedBody();
        $memberid = $body['memberid'] ?? null;
        $title = $body['title'] ?? 'วิธีการทำบุญ';
        $msgBody = $body['body'] ?? '';
        $url = $body['url'] ?? '';
        $type = $body['type'] ?? 'webview_merit';

        if (!$memberid || !$url) {
            $response->getBody()->write(json_encode(['status' => 'fail', 'message' => 'Missing params']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        // Get Token
        $stmt = $this->db->prepare("SELECT fcm_token FROM membertb WHERE memberid = :mid");
        $stmt->execute([':mid' => $memberid]);
        $user = $stmt->fetch(PDO::FETCH_OBJ);

        if ($user && !empty($user->fcm_token)) {
            // FCM Logic
            $serviceAccountPath = __DIR__ . '/../../configs/service-account.json';
            if (file_exists($serviceAccountPath)) {
                try {
                    $scopes = ['https://www.googleapis.com/auth/firebase.messaging'];
                    $credentials = new \Google\Auth\Credentials\ServiceAccountCredentials($scopes, $serviceAccountPath);
                    $accessToken = $credentials->fetchAuthToken(\Google\Auth\HttpHandler\HttpHandlerFactory::build());
                    $tokenValue = $accessToken['access_token'] ?? null;

                    if ($tokenValue) {
                        $fcmUrl = "https://fcm.googleapis.com/v1/projects/" . $credentials->getProjectId() . "/messages:send";
                        $message = [
                            'message' => [
                                'token' => $user->fcm_token,
                                'data' => [
                                    'type' => $type,
                                    'title' => $title,
                                    'body' => $msgBody,
                                    'url' => $url,
                                    'memberid' => (string) $memberid
                                ]
                            ]
                        ];

                        $ch = curl_init();
                        curl_setopt($ch, CURLOPT_URL, $fcmUrl);
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
            $response->getBody()->write(json_encode(['status' => 'success']));
        } else {
            $response->getBody()->write(json_encode(['status' => 'fail', 'message' => 'User token not found']));
        }

        return $response->withHeader('Content-Type', 'application/json');
    }

    // API: Get Assigned Temple (or Latest Default)
    public function getAssigned($request, $response, $args)
    {
        $memberid = $args['memberid'];

        // 1. Try to fetch assigned
        $sql = "SELECT t.*, a.custom_description as assign_desc, a.assigned_at FROM sacred_temple_tb t 
                JOIN user_temple_assign a ON t.id = a.temple_id 
                WHERE a.memberid = :mid";
        $stmt = $this->db->prepare($sql);
        $stmt->execute([':mid' => $memberid]);
        $items = $stmt->fetchAll(PDO::FETCH_OBJ);

        // 2. If not assigned, get latest
        if (empty($items)) {
            $item = new \stdClass();
            $item->id = 0;
            $item->temple_name = "ยังไม่มีวัดแนะนำ";
            $item->description = "โปรดรอคำแนะนำจากแอดมินหรือเลือกสักการะวัดใกล้บ้านท่าน";
            $item->image_url = "/uploads/temple/default.png";
            $item->address = "";
            $item->assign_desc = "";
            $item->is_default = true;
            $items[] = $item;
        }

        $response->getBody()->write(json_encode($items));
        return $response->withHeader('Content-Type', 'application/json');
    }

    private function ensureAssignTable()
    {
        $sql = "CREATE TABLE IF NOT EXISTS user_temple_assign (
            id INT AUTO_INCREMENT PRIMARY KEY,
            memberid VARCHAR(50) NOT NULL,
            temple_id INT NOT NULL,
            custom_description TEXT,
            assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            KEY (memberid),
            FOREIGN KEY (temple_id) REFERENCES sacred_temple_tb(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        $this->db->exec($sql);
    }
    public function deleteAssignment($request, $response)
    {
        $body = $request->getParsedBody();
        $memberid = $body['memberid'] ?? null;
        $temple_id = $body['temple_id'] ?? null;

        if (!$memberid || !$temple_id) {
            $response->getBody()->write(json_encode(['status' => 'fail', 'message' => 'Missing parameters']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        $sql = "DELETE FROM user_temple_assign WHERE memberid = :mid AND temple_id = :tid";
        $stmt = $this->db->prepare($sql);
        $success = $stmt->execute(['mid' => $memberid, 'tid' => $temple_id]);

        $response->getBody()->write(json_encode(['status' => $success ? 'success' : 'fail']));
        return $response->withHeader('Content-Type', 'application/json');
    }
}
