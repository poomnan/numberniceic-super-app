<?php
namespace App\Managers;

use PDO;

class SpellAPIController extends Manager
{
    public function latest($request, $response)
    {
        $params = $request->getQueryParams();
        $memberId = $params['memberid'] ?? null;

        $sql = "SELECT * FROM spells_warnings ORDER BY id DESC LIMIT 1";
        $stmt = $this->db->query($sql);
        $item = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($item) {
            $item['note'] = "";
            if ($memberId) {
                $noteSql = "SELECT note FROM member_spell_notes WHERE memberid = :mid AND spell_id = :sid";
                $nStmt = $this->db->prepare($noteSql);
                $nStmt->execute([':mid' => $memberId, ':sid' => $item['id']]);
                $pNote = $nStmt->fetchColumn();
                if ($pNote !== false) {
                    $item['note'] = $pNote;
                }
            }

            if (!empty($item['photo'])) {
                $domain = "https://numberniceic.online";
                $item['photo_url'] = $item['photo'];
                if (strpos($item['photo'], 'http') !== 0) {
                    $item['photo_url'] = $domain . (strpos($item['photo'], '/') === 0 ? '' : '/') . $item['photo'];
                }
            }
            $data = [
                'status' => 'success',
                'data' => $item
            ];
        } else {
            $data = [
                'status' => 'empty',
                'data' => null
            ];
        }

        $response->getBody()->write(json_encode($data));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function getById($request, $response, $args)
    {
        $id = $args['id'];
        $params = $request->getQueryParams();
        $memberId = $params['memberid'] ?? null;

        $sql = "SELECT * FROM spells_warnings WHERE id = :id";
        $stmt = $this->db->prepare($sql);
        $stmt->execute([':id' => $id]);
        $item = $stmt->fetch(PDO::FETCH_ASSOC);

        if ($item) {
            $item['note'] = "";
            if ($memberId) {
                $noteSql = "SELECT note FROM member_spell_notes WHERE memberid = :mid AND spell_id = :sid";
                $nStmt = $this->db->prepare($noteSql);
                $nStmt->execute([':mid' => $memberId, ':sid' => $item['id']]);
                $pNote = $nStmt->fetchColumn();
                if ($pNote !== false) {
                    $item['note'] = $pNote;
                }
            }

            if (!empty($item['photo'])) {
                $domain = "https://numberniceic.online";
                $item['photo_url'] = $item['photo'];
                if (strpos($item['photo'], 'http') !== 0) {
                    $item['photo_url'] = $domain . (strpos($item['photo'], '/') === 0 ? '' : '/') . $item['photo'];
                }
            }
            $data = ['status' => 'success', 'data' => $item];
        } else {
            $data = ['status' => 'error', 'message' => 'Not found'];
        }
        $response->getBody()->write(json_encode($data));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function getAll($request, $response)
    {
        $params = $request->getQueryParams();
        $memberId = $params['memberid'] ?? null;

        $sql = "SELECT * FROM spells_warnings ORDER BY id DESC";
        $stmt = $this->db->query($sql);
        $items = $stmt->fetchAll(PDO::FETCH_ASSOC);

        foreach ($items as &$item) {
            $item['note'] = "";
            if ($memberId) {
                $noteSql = "SELECT note FROM member_spell_notes WHERE memberid = :mid AND spell_id = :sid";
                $nStmt = $this->db->prepare($noteSql);
                $nStmt->execute([':mid' => $memberId, ':sid' => $item['id']]);
                $pNote = $nStmt->fetchColumn();
                if ($pNote !== false) {
                    $item['note'] = $pNote;
                }
            }

            if (!empty($item['photo'])) {
                $domain = "https://numberniceic.online";
                $item['photo_url'] = $item['photo'];
                if (strpos($item['photo'], 'http') !== 0) {
                    $item['photo_url'] = $domain . (strpos($item['photo'], '/') === 0 ? '' : '/') . $item['photo'];
                }
            } else {
                $item['photo_url'] = "";
            }
        }

        $response->getBody()->write(json_encode(['status' => 'success', 'data' => $items]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function getAssigned($request, $response, $args)
    {
        $memberId = $args['memberid'];

        // Find all spells that have notes for this member (our assignment table)
        $sql = "SELECT s.*, n.note, n.created_at as assigned_at 
                FROM spells_warnings s
                JOIN member_spell_notes n ON s.id = n.spell_id
                WHERE n.memberid = :mid
                ORDER BY n.created_at DESC";

        $stmt = $this->db->prepare($sql);
        $stmt->execute([':mid' => $memberId]);
        $items = $stmt->fetchAll(PDO::FETCH_ASSOC);

        $domain = "https://numberniceic.online";
        foreach ($items as &$item) {
            if (!empty($item['photo'])) {
                if (strpos($item['photo'], 'http') !== 0) {
                    $item['photo_url'] = $domain . (strpos($item['photo'], '/') === 0 ? '' : '/') . $item['photo'];
                } else {
                    $item['photo_url'] = $item['photo'];
                }
            } else {
                $item['photo_url'] = "";
            }
        }

        $response->getBody()->write(json_encode(['status' => 'success', 'data' => $items]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function assign($request, $response)
    {
        $body = $request->getParsedBody();
        $memberId = $body['memberid'] ?? null;
        $spellId = $body['spell_id'] ?? null;
        $note = $body['note'] ?? '';

        if (!$memberId || !$spellId) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Missing param']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        $sql = "SELECT * FROM spells_warnings WHERE id = :id";
        $stmt = $this->db->prepare($sql);
        $stmt->execute([':id' => $spellId]);
        $spell = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$spell) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Spell not found']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        // Save personalized note
        $noteSql = "INSERT INTO member_spell_notes (memberid, spell_id, note) VALUES (:mid, :sid, :note) 
                    ON DUPLICATE KEY UPDATE note = :note2";
        $nStmt = $this->db->prepare($noteSql);
        $nStmt->execute([':mid' => $memberId, ':sid' => $spellId, ':note' => $note, ':note2' => $note]);

        $userSql = "SELECT fcm_token FROM membertb WHERE memberid = :mid";
        $uStmt = $this->db->prepare($userSql);
        $uStmt->execute([':mid' => $memberId]);
        $user = $uStmt->fetch(PDO::FETCH_ASSOC);

        // Save Notification to DB
        $title = ($spell['type'] == 'warning') ? "คำเตือนพิเศษ" : "คาถาและคำเตือนพิเศษ";
        $bodyText = "คุณนินแนะนำ : " . $spell['title'] . ' คุณดูได้ที่แถบ "คาถาและคำเตือนพิเศษ" หน้า VIP';

        $nm = new NotificationManager($this->container);
        $nm->saveNotification(
            $memberId,
            'spell_assign',
            $title,
            $bodyText,
            (string) $spellId,
            $note
        );

        if ($user && !empty($user['fcm_token'])) {
            // Re-define title/bodyText if needed, or use variables above

            $dataPayload = [
                'type' => 'webview_spell',
                'url' => (string) $spellId,
                'spell_id' => (string) $spellId,
                'memberid' => (string) $memberId,
                'title' => $title,
                'body' => $bodyText,
                'note' => $note
            ];

            $res = $this->_sendFcmReal($user['fcm_token'], $title, $bodyText, $dataPayload);
            if ($res) {
                $response->getBody()->write(json_encode(['status' => 'success']));
            } else {
                $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'FCM Failed']));
            }
        } else {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'User has no token']));
        }
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function updateNote($request, $response)
    {
        $body = $request->getParsedBody();
        $id = $body['id'] ?? null; // spell_id
        $memberId = $body['memberid'] ?? null;
        $note = $body['note'] ?? '';

        if (!$id) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Missing Spell ID']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        if ($memberId) {
            // Update personalized note
            $sql = "INSERT INTO member_spell_notes (memberid, spell_id, note) VALUES (:mid, :sid, :note) 
                    ON DUPLICATE KEY UPDATE note = :note2";
            $stmt = $this->db->prepare($sql);
            $res = $stmt->execute([':mid' => $memberId, ':sid' => $id, ':note' => $note, ':note2' => $note]);
        } else {
            // If no memberId, we can't update a global note anymore since the column is dropped.
            // Or we could have a default note? The user said "ลบ field 'note' ออก".
            // So we'll return error or just do nothing if no memberId is provided.
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Member ID required for personal note']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        if ($res) {
            $response->getBody()->write(json_encode(['status' => 'success']));
        } else {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Database update failed']));
        }
        return $response->withHeader('Content-Type', 'application/json');
    }

    private function _sendFcmReal($token, $title, $body, $data = [])
    {
        $serviceAccountPath = __DIR__ . '/../../configs/service-account.json';
        if (!file_exists($serviceAccountPath))
            return false;

        try {
            $scopes = ['https://www.googleapis.com/auth/firebase.messaging'];
            $credentials = new \Google\Auth\Credentials\ServiceAccountCredentials($scopes, $serviceAccountPath);
            $accessToken = $credentials->fetchAuthToken(\Google\Auth\HttpHandler\HttpHandlerFactory::build());
            if (!isset($accessToken['access_token']))
                return false;

            $tokenValue = $accessToken['access_token'];
            $jsonKey = json_decode(file_get_contents($serviceAccountPath), true);
            $projectId = $jsonKey['project_id'];
            $url = "https://fcm.googleapis.com/v1/projects/" . $projectId . "/messages:send";

            $dataPayload = array_merge(['title' => $title, 'body' => $body], $data);
            foreach ($dataPayload as $key => $val) {
                $dataPayload[$key] = (string) $val;
            }

            $message = [
                'message' => [
                    'token' => $token,
                    'data' => $dataPayload
                ]
            ];
            $json = json_encode($message);
            $headers = ['Authorization: Bearer ' . $tokenValue, 'Content-Type: application/json'];

            $ch = curl_init();
            curl_setopt($ch, CURLOPT_URL, $url);
            curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "POST");
            curl_setopt($ch, CURLOPT_POSTFIELDS, $json);
            curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
            curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
            $rawResponse = curl_exec($ch);
            $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
            curl_close($ch);
            return $httpCode === 200;
        } catch (\Exception $e) {
            return false;
        }
    }
    public function deleteAssignment($request, $response)
    {
        $body = $request->getParsedBody();
        $memberId = $body['memberid'] ?? null;
        $spellId = $body['spell_id'] ?? null;

        if (!$memberId || !$spellId) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Missing param']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        $sql = "DELETE FROM member_spell_notes WHERE memberid = :mid AND spell_id = :sid";
        $stmt = $this->db->prepare($sql);
        $success = $stmt->execute(['mid' => $memberId, 'sid' => $spellId]);

        $response->getBody()->write(json_encode(['status' => $success ? 'success' : 'error']));
        return $response->withHeader('Content-Type', 'application/json');
    }
}
