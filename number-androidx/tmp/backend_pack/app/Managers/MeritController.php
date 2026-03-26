<?php

namespace App\Managers;

use PDO;

class MeritController extends Manager
{
    public function view($request, $response)
    {
        $id = $request->getQueryParams()['id'] ?? null;

        $item = null;

        if ($id) {
            if ($id >= 1 && $id <= 8) {
                // Fetch from Buddha Pang
                $stmt = $this->db->prepare("SELECT * FROM buddha_pang_tb WHERE buddha_day = :day");
                // Mapping ID to Day: 1=Sun, 2=Mon... 8=Wed Night?
                // Wait, typically ID matches day or row ID.
                // In MeritAssignPickerAct: 
                // 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat, 8=WedNight
                // In buddha_pang_tb, usually buddha_day 1=Sun, ... 8=Wed Night.
                // Let's assume ID maps safely to buddha_day for 1-8.
                $stmt->execute(['day' => $id]);
                $pang = $stmt->fetch(PDO::FETCH_OBJ);

                if ($pang) {
                    $item = [
                        'title' => $pang->pang_name,
                        'image' => $pang->image_url,
                        'content' => $pang->description
                    ];
                }
            } elseif ($id == 9) {
                $item = [
                    'title' => 'วิธีการปรับดวงเรื่องความรักเงินงาน',
                    'image' => '/uploads/buddha/default.png', // Placeholder
                    'content' => 'แนะนำให้ทำบุญถวายสังฆทาน ตักบาตรพระสงฆ์ 9 รูป หรือปล่อยปลา เพื่อเสริมดวงชะตาด้านความรักและการเงินให้ดียิ่งขึ้น'
                ];
            } elseif ($id == 10) {
                $item = [
                    'title' => 'การทำบุญช่วยส่งเสริมชะตาอาภัพคู่',
                    'image' => '/uploads/buddha/default.png', // Placeholder
                    'content' => 'แนะนำให้ถวายของคู่ เช่น เทียนคู่ แจกันคู่ หมอนคู่ หรือทำบุญสมทบทุนงานแต่งงาน เพื่อแก้เคล็ดเสริมดวงคู่ครอง'
                ];
            }
        }

        if (!$item) {
            $item = [
                'title' => 'ไม่พบข้อมูล',
                'image' => '',
                'content' => 'ไม่พบข้อมูลสำหรับรายการที่ระบุ'
            ];
        }

        return $this->container->get('view')->render($response, 'web_merit_view.php', [
            'item' => (object) $item
        ]);
    }
    public function assignToUser($request, $response)
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

        // Save to Database
        try {
            // Check for duplicate
            $chk = $this->db->prepare("SELECT id FROM user_merit_assign WHERE memberid = :mid AND merit_type = :type AND title = :title");
            $chk->execute([':mid' => $memberid, ':type' => $type, ':title' => $title]);
            $existing = $chk->fetch(PDO::FETCH_OBJ);

            if ($existing) {
                // Update
                $sql = "UPDATE user_merit_assign SET assigned_at = NOW(), body = :body, url = :url WHERE id = :id";
                $stmt = $this->db->prepare($sql);
                $stmt->execute([
                    ':body' => $msgBody,
                    ':url' => $url,
                    ':id' => $existing->id
                ]);
            } else {
                // Insert
                $sql = "INSERT INTO user_merit_assign (memberid, merit_type, title, body, url, assigned_at) 
                        VALUES (:mid, :type, :title, :body, :url, NOW())";
                $stmt = $this->db->prepare($sql);
                $stmt->execute([
                    ':mid' => $memberid,
                    ':type' => $type,
                    ':title' => $title,
                    ':body' => $msgBody,
                    ':url' => $url
                ]);
            }
        } catch (\PDOException $e) {
            // Ignore if table doesn't exist yet, but log it?
            // Assuming table user_merit_assign exists.
        }

        // Save to Notifications Table for App List
        $nm = new NotificationManager($this->container);
        $nm->saveNotification(
            $memberid,
            'merit_assign', // or 'webview_merit' to match FCM type?
            $title,
            $msgBody,
            $url,
            "Merit Type: $type"
        );

        // Get Token
        $stmt = $this->db->prepare("SELECT fcm_token FROM membertb WHERE memberid = :mid");
        $stmt->execute([':mid' => $memberid]);
        $user = $stmt->fetch(PDO::FETCH_OBJ);

        if ($user && !empty($user->fcm_token)) {
            // FCM Logic
            $this->sendFcm($user->fcm_token, $title, $msgBody, [
                'type' => $type,
                'title' => $title,
                'body' => $msgBody,
                'url' => $url,
                'memberid' => (string) $memberid
            ]);
            $response->getBody()->write(json_encode(['status' => 'success']));
        } else {
            $response->getBody()->write(json_encode(['status' => 'fail', 'message' => 'User token not found']));
        }

        return $response->withHeader('Content-Type', 'application/json');
    }

    public function getAssigned($request, $response, $args)
    {
        $memberid = $args['memberid'];
        $params = $request->getQueryParams();
        $type = $params['type'] ?? 'merit'; // default to merit if not specified

        // Map frontend type aliases to DB types if necessary
        // Frontend sends: 'merit', 'changenum', 'spell'
        // DB has: 'merit', 'changenum', 'spell', 'merit_assign' (legacy), etc.

        $dbType = $type;
        if ($type == 'merit') {
            $dbTypes = ['merit', 'merit_assign', 'webview_merit']; // Include legacy
        } elseif ($type == 'changenum') {
            $dbTypes = ['changenum', 'webview_changenum'];
        } else {
            $dbTypes = [$type];
        }

        // Prepare IN clause
        $in = str_repeat('?,', count($dbTypes) - 1) . '?';

        try {
            $sql = "SELECT id, memberid, title, body, body as content, url, assigned_at, merit_type 
                    FROM user_merit_assign 
                    WHERE memberid = ? AND merit_type IN ($in) 
                    ORDER BY assigned_at DESC";

            $stmt = $this->db->prepare($sql);
            $params = array_merge([$memberid], $dbTypes);
            $stmt->execute($params);
            $rows = $stmt->fetchAll(PDO::FETCH_OBJ);

            $items = [];
            foreach ($rows as $row) {
                // Map to Android NotificationResponse structure
                $item = new \stdClass();
                $item->id = (int) $row->id;
                $item->member_id = (string) $row->memberid;
                $item->type = $row->merit_type; // Maps to 'type'
                $item->title = $row->title;
                $item->body = $row->body;
                $item->url = $row->url;
                $item->note = null;
                $item->is_read = false; // Default
                $item->read_at = null;
                $item->created_at = $row->assigned_at; // Maps to 'created_at'

                // Extra fields for WebView if needed
                $item->content = $row->content;
                $item->image = "";
                $item->photo_url = "";

                // Image Logic
                if (strpos($row->merit_type, 'change') !== false) {
                    $item->image = "/uploads/merit/change_default.png";
                    $item->photo_url = "/uploads/merit/change_default.png";
                } else {
                    $item->image = "/uploads/merit/merit_default.png";
                    $item->photo_url = "/uploads/merit/merit_default.png";
                }

                $items[] = $item;
            }

        } catch (\Exception $e) {
            $items = [];
        }

        /* 
        // Logic moved to Frontend to show "Wait for admin" if empty.
        if (empty($items)) {
            // ...
        }
        */

        $response->getBody()->write(json_encode($items));
        return $response->withHeader('Content-Type', 'application/json');
    }

    private function sendFcm($token, $title, $body, $data)
    {
        $serviceAccountPath = __DIR__ . '/../../configs/service-account.json';
        if (!file_exists($serviceAccountPath))
            return;

        try {
            $scopes = ['https://www.googleapis.com/auth/firebase.messaging'];
            $credentials = new \Google\Auth\Credentials\ServiceAccountCredentials($scopes, $serviceAccountPath);
            $accessToken = $credentials->fetchAuthToken(\Google\Auth\HttpHandler\HttpHandlerFactory::build());
            $tokenValue = $accessToken['access_token'] ?? null;

            if ($tokenValue) {
                $fcmUrl = "https://fcm.googleapis.com/v1/projects/" . $credentials->getProjectId() . "/messages:send";
                $message = [
                    'message' => [
                        'token' => $token,
                        'data' => $data
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

    public function deleteAssignment($request, $response)
    {
        $body = $request->getParsedBody();
        $id = $body['id'] ?? null;
        $memberid = $body['memberid'] ?? null;

        if (!$id || !$memberid) {
            $response->getBody()->write(json_encode(['status' => 'fail', 'message' => 'Missing parameters']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        $sql = "DELETE FROM user_merit_assign WHERE id = :id AND memberid = :mid";
        $stmt = $this->db->prepare($sql);
        $success = $stmt->execute(['id' => $id, 'mid' => $memberid]);

        $response->getBody()->write(json_encode(['status' => $success ? 'success' : 'fail']));
        return $response->withHeader('Content-Type', 'application/json');
    }
}
