<?php

namespace App\Managers;

use PDO;
use Slim\Psr7\Response;
use Slim\Views\PhpRenderer;

class BuddhaPangController extends Manager
{
    public function viewList($request, $response)
    {
        if (session_status() == PHP_SESSION_NONE)
            session_start();
        $status = $_SESSION['status'] ?? null;
        unset($_SESSION['status']);

        // Sort by Day (1-7), then 8, then others (90+)
        $stmt = $this->db->query("SELECT * FROM buddha_pang_tb ORDER BY CASE WHEN buddha_day BETWEEN 1 AND 7 THEN buddha_day WHEN buddha_day = 8 THEN 8 ELSE 99 END, buddha_day ASC");
        $items = $stmt->fetchAll(PDO::FETCH_OBJ);

        return $this->container->get('view')->render($response, 'admin_buddha_list.php', [
            'items' => $items,
            'status' => $status
        ]);
    }

    public function viewAdd($request, $response)
    {
        return $this->container->get('view')->render($response, 'admin_buddha_form.php', [
            'item' => null
        ]);
    }

    public function viewEdit($request, $response, $args)
    {
        $id = $args['id'];
        $stmt = $this->db->prepare("SELECT * FROM buddha_pang_tb WHERE id = :id");
        $stmt->execute(['id' => $id]);
        $item = $stmt->fetch(PDO::FETCH_OBJ);

        if (!$item) {
            return $response->withHeader('Location', '/admin/buddha')->withStatus(302);
        }

        return $this->container->get('view')->render($response, 'admin_buddha_form.php', [
            'item' => $item
        ]);
    }

    public function save($request, $response)
    {
        $post = $request->getParsedBody();
        $id = $post['id'] ?? null;
        $pang_name = $post['pang_name'] ?? '';
        $buddha_day = !empty($post['buddha_day']) ? $post['buddha_day'] : null;
        $description = $post['description'] ?? '';
        $image_url = $post['image_url'] ?? '';

        // Handle file upload if any
        $files = $request->getUploadedFiles();
        if (isset($files['image_file'])) {
            $uploadedFile = $files['image_file'];
            if ($uploadedFile->getError() === UPLOAD_ERR_OK) {
                $extension = strtolower(pathinfo($uploadedFile->getClientFilename(), PATHINFO_EXTENSION));
                $filename = sprintf('%s.%s', bin2hex(random_bytes(8)), $extension);

                // Use UPLOAD_DIR constant from constant.php if available, fallback to relative
                $directory = defined('UPLOAD_DIR') ? UPLOAD_DIR . '/buddha' : dirname(__DIR__, 2) . '/public/uploads/buddha';

                if (!is_dir($directory)) {
                    @mkdir($directory, 0755, true);
                }

                if (is_writable($directory)) {
                    $targetPath = $directory . DIRECTORY_SEPARATOR . $filename;
                    $uploadedFile->moveTo($targetPath);
                    @chmod($targetPath, 0644); // Ensure file is readable by browser
                    $image_url = '/uploads/buddha/' . $filename;
                } else {
                    error_log("BUDDHA UPLOAD: Directory $directory is not writable.");
                }
            } elseif ($uploadedFile->getError() !== UPLOAD_ERR_NO_FILE) {
                error_log("BUDDHA UPLOAD ERROR: Code " . $uploadedFile->getError());
            }
        }

        if (session_status() == PHP_SESSION_NONE)
            session_start();
        if ($id) {
            $sql = "UPDATE buddha_pang_tb SET pang_name = :n, buddha_day = :d, description = :desc, image_url = :img WHERE id = :id";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([
                'n' => $pang_name,
                'd' => $buddha_day,
                'desc' => $description,
                'img' => $image_url,
                'id' => $id
            ]);
            $_SESSION['status'] = ['type' => 'success', 'message' => 'แก้ไขข้อมูลเรียบร้อยแล้ว'];
        } else {
            $sql = "INSERT INTO buddha_pang_tb (pang_name, buddha_day, description, image_url) VALUES (:n, :d, :desc, :img)";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([
                'n' => $pang_name,
                'd' => $buddha_day,
                'desc' => $description,
                'img' => $image_url
            ]);
            $_SESSION['status'] = ['type' => 'success', 'message' => 'เพิ่มข้อมูลใหม่เรียบร้อยแล้ว'];
        }

        return $response->withHeader('Location', '/admin/buddha')->withStatus(302);
    }

    public function delete($request, $response, $args)
    {
        $id = $args['id'];
        $stmt = $this->db->prepare("DELETE FROM buddha_pang_tb WHERE id = :id");
        $stmt->execute(['id' => $id]);

        return $response->withHeader('Location', '/admin/buddha')->withStatus(302);
    }

    // API: Assign Buddha Pang to User
    public function assignToUser($request, $response)
    {
        $body = $request->getParsedBody();
        $memberid = $body['memberid'] ?? $body['member_id'] ?? null;
        $buddha_id = $body['buddha_id'] ?? null;
        $custom_desc = $body['custom_description'] ?? '';
        $type = $body['assignment_type'] ?? 'annual';

        if (!$memberid || !$buddha_id) {
            $response->getBody()->write(json_encode(['status' => 'fail', 'message' => 'Missing parameters']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        // Keep MySQL in sync (Primary - Changed to plain INSERT for accumulation)
        $sql = "INSERT INTO user_buddha_assign (memberid, assignment_type, buddha_id, custom_description, assigned_at) 
                VALUES (:mid, :type, :bid, :desc, NOW())";

        $stmt = $this->db->prepare($sql);
        $success = $stmt->execute([
            ':mid' => $memberid,
            ':type' => $type,
            ':bid' => $buddha_id,
            ':desc' => $custom_desc
        ]);

        if ($success) {
            $this->notifyUser($memberid, $type);
        }

        $response->getBody()->write(json_encode(['status' => $success ? 'success' : 'fail']));
        return $response->withHeader('Content-Type', 'application/json');
    }

    private function notifyUser($memberid, $type)
    {
        try {
            // Get Token
            $stmt = $this->db->prepare("SELECT fcm_token FROM membertb WHERE memberid = :mid");
            $stmt->execute([':mid' => $memberid]);
            $user = $stmt->fetch(PDO::FETCH_OBJ);

            $title = ($type == 'lifetime') ? "แนะนำพระพุทธรูปประจำตัวตลอดชีพ" : "แนะนำพระพุทธรูปประจำปี";
            $body = "แอดมินได้เลือกพระพุทธรูปที่เหมาะสมกับดวงชะตาของคุณแล้ว";

            // Save to Database
            $nm = new NotificationManager($this->container);
            $nm->saveNotification(
                $memberid,
                'buddha_assign',
                $title,
                $body,
                '', // URL
                "Type: $type"
            );

            if ($user && !empty($user->fcm_token)) {
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
                            'type' => 'buddha_assign',
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
            // Ignore notification errors
        }
    }

    // API: Get assigned Buddha Pang for a User
    public function getAssigned($request, $response, $args)
    {
        $memberid = $args['memberid'];

        // Fetch user birthday for expiration check
        $userBirthday = null;
        if ($memberid) {
            $stmtUser = $this->db->prepare("SELECT birthday FROM membertb WHERE memberid = :mid");
            $stmtUser->execute([':mid' => $memberid]);
            $user = $stmtUser->fetch(PDO::FETCH_OBJ);
            $userBirthday = $user ? $user->birthday : null;
        }

        $this->db->exec("SET time_zone = '+07:00'");
        $sql = "SELECT b.*, a.custom_description, a.assignment_type, a.assigned_at FROM buddha_pang_tb b 
                JOIN user_buddha_assign a ON b.id = a.buddha_id 
                WHERE a.memberid = :mid
                ORDER BY a.assigned_at DESC";

        $stmt = $this->db->prepare($sql);
        $stmt->execute([':mid' => $memberid]);
        $items = $stmt->fetchAll(PDO::FETCH_OBJ);

        $result = ['annual' => [], 'lifetime' => []];
        $now = new \DateTime();

        foreach ($items as $item) {
            if (isset($item->assignment_type) && $item->assignment_type == 'lifetime') {
                $result['lifetime'][] = $item;
            } else {
                // Annual Assignment Expiration Logic
                $isExpired = false;
                if ($userBirthday) {
                    $assignedAt = new \DateTime($item->assigned_at);
                    $birthDate = new \DateTime($userBirthday);

                    // Create expiration date: Birth month and day in the year of assignment
                    $expirationDate = new \DateTime();
                    $expirationDate->setDate(
                        (int) $assignedAt->format('Y'),
                        (int) $birthDate->format('m'),
                        (int) $birthDate->format('d')
                    );
                    $expirationDate->setTime(23, 59, 59);

                    // If birthday in that year was already before assignedAt, expiration is next year's birthday
                    if ($expirationDate < $assignedAt) {
                        $expirationDate->modify('+1 year');
                    }

                    // If current time is after the expiration date, it's expired
                    if ($now > $expirationDate) {
                        $isExpired = true;
                    }
                }

                if (!$isExpired) {
                    $result['annual'][] = $item;
                }
            }
        }

        // Add defaults if empty
        if (empty($result['annual'])) {
            $item = new \stdClass();
            $item->id = 0;
            $item->buddha_day = 0;
            $item->pang_name = "พระพุทธรูปปางประจำวันเกิด";
            $item->description = "แนะนำให้สรงน้ำหรือถวายพระพุทธรูปปางประจำวันเกิดของท่านเพื่อเสริมความเป็นสิริมงคลและความสุขความเจริญตลอดทั้งปี";
            $item->image_url = "/uploads/buddha/default.png";
            $item->custom_description = "โปรดติดต่อเปิดดวงเพื่อถวายพระพุทธรูปปางที่ถูกโฉลกกับคุณโดยแท้จริง";
            $item->assignment_type = 'annual';
            $result['annual'][] = $item;
        }
        if (empty($result['lifetime'])) {
            $lifeItem = new \stdClass();
            $lifeItem->id = 0;
            $lifeItem->buddha_day = 0;
            $lifeItem->pang_name = "พระพุทธรูปประจำตัวตลอดชีพ";
            $lifeItem->description = "พระพุทธรูปที่เป็นมงคลสูงสุดแก่ชีวิตของคุณ";
            $lifeItem->image_url = "/uploads/buddha/default.png";
            $lifeItem->custom_description = "โปรดติดต่อสอบถามเพื่อรับคำแนะนำพระประจำตัวตลอดชีพ";
            $lifeItem->assignment_type = 'lifetime';
            $result['lifetime'][] = $lifeItem;
        }

        $response->getBody()->write(json_encode($result));
        return $response->withHeader('Content-Type', 'application/json');
    }

    // API: Get all Buddha Pangs for Mobile App
    public function getBuddhaPangsApi($request, $response)
    {
        $stmt = $this->db->query("SELECT * FROM buddha_pang_tb ORDER BY CASE WHEN buddha_day BETWEEN 1 AND 8 THEN buddha_day ELSE 99 END, buddha_day ASC");
        $items = $stmt->fetchAll(PDO::FETCH_OBJ);
        $response->getBody()->write(json_encode($items));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function deleteAssignment($request, $response)
    {
        $body = $request->getParsedBody();
        $memberid = $body['memberid'] ?? null;
        $buddha_id = $body['buddha_id'] ?? null;
        $type = $body['assignment_type'] ?? 'annual';

        if (!$memberid || !$buddha_id) {
            $response->getBody()->write(json_encode(['status' => 'fail', 'message' => 'Missing parameters']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        $sql = "DELETE FROM user_buddha_assign WHERE memberid = :mid AND buddha_id = :bid AND assignment_type = :type";
        $stmt = $this->db->prepare($sql);
        $success = $stmt->execute(['mid' => $memberid, 'bid' => $buddha_id, 'type' => $type]);

        $response->getBody()->write(json_encode(['status' => $success ? 'success' : 'fail']));
        return $response->withHeader('Content-Type', 'application/json');
    }
}
