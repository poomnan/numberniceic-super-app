<?php

namespace App\Managers;

use App\Managers\PersonManager;
use App\Managers\NotificationManager;
use PDO;

class NotificationController extends Manager
{
    public function sendCustomNotify($request, $response)
    {
        $post = $request->getParsedBody();
        $memberid = $post['memberid'] ?? '';
        $title = $post['title'] ?? '';
        $body = $post['body'] ?? '';
        $type = $post['type'] ?? 'custom';
        $url = $post['url'] ?? null;
        $note = $post['note'] ?? null;

        if (empty($memberid) || empty($title) || empty($body)) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Invalid input']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(400);
        }

        if ($memberid === 'all') {
            // Send to ALL members
            $stmt = $this->db->query("SELECT memberid, fcm_token FROM membertb WHERE fcm_token IS NOT NULL AND fcm_token != ''");
            $users = $stmt->fetchAll(\PDO::FETCH_OBJ);

            $successCount = 0;
            $total = count($users);
            $errors = [];

            foreach ($users as $u) {
                $fcmRes = '';
                $res = $this->sendFcm($u->fcm_token, $title, $body, [
                    'type' => $type,
                    'memberid' => (string) $u->memberid,
                    'url' => $url,
                    'note' => $note
                ], $fcmRes);
                if ($res) {
                    $successCount++;
                    // Save to database
                    $this->saveNotificationToDb((string) $u->memberid, $type, $title, $body, $url, $note);
                } else {
                    $errors[] = ["memberid" => $u->memberid, "error" => $fcmRes];
                }
            }

            $response->getBody()->write(json_encode([
                'status' => 'success',
                'message' => "Sent to $successCount / $total people",
                'success_count' => $successCount,
                'total_count' => $total,
                'errors' => $errors
            ]));
            return $response->withHeader('Content-Type', 'application/json');
        }

        // Fetch Single User Token
        $stmt = $this->db->prepare("SELECT * FROM membertb WHERE memberid = :mid");
        $stmt->execute([':mid' => $memberid]);
        $user = $stmt->fetch(\PDO::FETCH_OBJ);

        if ($user && !empty($user->fcm_token)) {
            $fcmRes = '';
            $res = $this->sendFcm($user->fcm_token, $title, $body, [
                'type' => $type,
                'memberid' => (string) $user->memberid,
                'url' => $url,
                'note' => $note
            ], $fcmRes);

            if ($res) {
                // Save to database
                $this->saveNotificationToDb($memberid, $type, $title, $body, $url, $note);
                $response->getBody()->write(json_encode(['status' => 'success', 'message' => 'Notification sent successfully']));
            } else {
                $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'FCM failed', 'details' => $fcmRes]));
                return $response->withHeader('Content-Type', 'application/json')->withStatus(200); // Keep 200 to see error in app
            }
        } else {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'User not found or no FCM token']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(200);
        }

        return $response;
    }

    public function viewCustomNotify($request, $response)
    {
        $queryParams = $request->getQueryParams();
        $search = $queryParams['search'] ?? '';
        $users = [];

        if (!empty($search)) {
            $sql = "SELECT * FROM membertb WHERE username LIKE :s OR realname LIKE :s OR memberid = :exact LIMIT 50";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([':s' => "%$search%", ':exact' => $search]);
            $users = $stmt->fetchAll(\PDO::FETCH_OBJ);
        }

        ob_start();
        include __DIR__ . '/../../views/web_admin_notify_custom.php';
        $html = ob_get_clean();
        $response->getBody()->write($html);
        return $response;
    }

    public function cronWanPra($request, $response)
    {
        // Set Timezone
        date_default_timezone_set('Asia/Bangkok');
        $hour = (int) date('H');
        $today = date('Y-m-d');
        $tomorrow = date('Y-m-d', strtotime('+1 day'));

        $isTodayWanPra = \App\Managers\ThaiCalendarHelper::isWanPra($today);
        $isTomorrowWanPra = \App\Managers\ThaiCalendarHelper::isWanPra($tomorrow);

        $msgTitle = "";
        $msgBody = "";
        $shouldSend = false;

        // Debug/Force Mode via Query Params
        $params = $request->getQueryParams();
        if (isset($params['force'])) {
            if ($params['force'] == 'today') {
                $msgTitle = "วันนี้วันพระ";
                $msgBody = "วันนี้วันพระ ขอให้ท่านมีความสุขกาย สบายใจ";
                $shouldSend = true;
            } elseif ($params['force'] == 'tomorrow') {
                $msgTitle = "พรุ่งนี้วันพระ";
                $msgBody = "พรุ่งนี้วันพระ อย่าลืมเตรียมตัวทำบุญตักบาตร";
                $shouldSend = true;
            } elseif ($params['force'] == 'test') { // Force Test
                $msgTitle = "ทดสอบแจ้งเตือนวันพระ";
                $msgBody = "นี่คือข้อความทดสอบระบบแจ้งเตือนวันพระ";
                $shouldSend = true;
            }
        } else {
            // Auto Mode based on Time
            if ($hour >= 6 && $hour < 9 && $isTodayWanPra) {
                // Check if already sent? (Wait, simple cron assumes calling ONCE per window)
                // To prevent double send, usually we need DB log.
                // But user simply asked for logic. Assuming Cron runs once at 7:00 and 18:00
                $msgTitle = "วันนี้วันพระ";
                $msgBody = "วันนี้วันพระ ขอให้ท่านมีความสุขกาย สบายใจ";
                $shouldSend = true;
            } elseif ($hour >= 17 && $hour < 20 && $isTomorrowWanPra) {
                $msgTitle = "พรุ่งนี้วันพระ";
                $msgBody = "พรุ่งนี้วันพระ อย่าลืมเตรียมตัวทำบุญตักบาตร";
                $shouldSend = true;
            }
        }

        if ($shouldSend) {
            // Fetch All Tokens & MemberID
            $stmt = $this->db->query("SELECT memberid, fcm_token FROM membertb WHERE fcm_token IS NOT NULL AND fcm_token != ''");
            $users = $stmt->fetchAll(\PDO::FETCH_OBJ);

            $total = count($users);
            $successCount = 0;

            if ($total > 0) {
                foreach ($users as $u) {
                    $res = '';
                    // Use existing sendFcm (HTTP v1 loop)
                    $result = $this->sendFcm($u->fcm_token, $msgTitle, $msgBody, ['type' => 'wanpra', 'memberid' => (string) $u->memberid], $res);
                    if ($result) {
                        $successCount++;
                        $this->saveNotificationToDb((string) $u->memberid, 'wanpra', $msgTitle, $msgBody);
                    }
                }

                $response->getBody()->write(json_encode([
                    'status' => 'success',
                    'message' => "Sent '$msgTitle' to $successCount / $total devices."
                ]));
            } else {
                $response->getBody()->write(json_encode(['status' => 'skipped', 'message' => 'No tokens found']));
            }

        } else {
            $response->getBody()->write(json_encode([
                'status' => 'skipped',
                'message' => 'Not matched time or condition',
                'debug' => [
                    'hour' => $hour,
                    'isTodayWanPra' => $isTodayWanPra,
                    'isTomorrowWanPra' => $isTomorrowWanPra
                ]
            ]));
        }

        return $response->withHeader('Content-Type', 'application/json');
    }

    public function sendBagColors($request, $response)
    {
        try {
            /*
            if (session_status() == PHP_SESSION_NONE)
                session_start();
            $userSession = $_SESSION['user'] ?? null;
            if (!$userSession || (!in_array(strtolower($userSession->vipcode ?? ''), ['admin', 'administrator']))) {
                $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Unauthorized']));
                return $response->withHeader('Content-Type', 'application/json')->withStatus(403);
            }
            */

            $queryParams = $request->getQueryParams();
            $targetId = $queryParams['memberid'] ?? null;

            // Get users with Token
            $sql = "SELECT memberid, realname, birthday, fcm_token FROM membertb WHERE fcm_token IS NOT NULL AND fcm_token != ''";

            if ($targetId) {
                $sql .= " AND memberid = :mid";
                $stmt = $this->db->prepare($sql);
                $stmt->execute([':mid' => $targetId]);
            } else {
                $stmt = $this->db->query($sql);
            }

            $users = $stmt->fetchAll(PDO::FETCH_OBJ);

            $personManager = new PersonManager();
            $results = [];
            $sentCount = 0;

            foreach ($users as $user) {
                if (!$user->birthday)
                    continue;

                // Calculate Age
                $birthdayTs = strtotime($user->birthday);
                $ages = $personManager->age($birthdayTs, time());
                $ageYear = $ages['year'] ?? 0;

                // Check Bag Color for this age
                $sqlBag = "SELECT * FROM bagcolortb WHERE memberid = :mid AND age = :age";
                $stmtBag = $this->db->prepare($sqlBag);
                $stmtBag->execute([':mid' => $user->memberid, ':age' => $ageYear]);
                $bag = $stmtBag->fetch(PDO::FETCH_OBJ);

                // Check Bag Color for this age
                $debugInfo = [
                    'memberid' => $user->memberid,
                    'age' => $ageYear,
                    'has_token' => !empty($user->fcm_token),
                    'token_preview' => substr($user->fcm_token ?? '', 0, 10),
                    'bag_found' => ($bag ? true : false),
                    'service_acc_exists' => file_exists(__DIR__ . '/../../configs/service-account.json')
                ];

                if ($bag) {
                    $title = "สีกระเป๋ามงคล (อายุ $ageYear)";
                    $body = "คลิกเพื่อดูสีกระเป๋าเสริมดวงของคุณวันนี้!";

                    $fcmResponse = '';
                    $res = $this->sendFcm($user->fcm_token, $title, $body, ['type' => 'bag_color', 'memberid' => (string) $user->memberid], $fcmResponse);

                    $debugInfo['fcm_result'] = $res;
                    $debugInfo['fcm_response_raw'] = $fcmResponse;

                    $status = $res ? 'sent' : 'failed';
                    if ($res) {
                        $sentCount++;
                        $this->saveNotificationToDb((string) $user->memberid, 'bag_color', $title, $body);
                    }

                    $results[] = [
                        'memberid' => $user->memberid,
                        'age' => $ageYear,
                        'status' => $status,
                        'fcm_response' => $fcmResponse,
                        'debug' => $debugInfo
                    ];
                } else {
                    $results[] = [
                        'memberid' => $user->memberid,
                        'age' => $ageYear,
                        'status' => 'no_bag_color_found',
                        'debug' => $debugInfo
                    ];
                }
            }

            $response->getBody()->write(json_encode(['status' => 'completed', 'sent_count' => $sentCount, 'details' => $results]));
            return $response->withHeader('Content-Type', 'application/json');

        } catch (\Throwable $e) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => $e->getMessage(), 'trace' => $e->getTraceAsString()]));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(500);
        }
    }

    private function sendFcm($token, $title, $body, $data = [], &$rawResponse = null)
    {
        $serviceAccountPath = __DIR__ . '/../../configs/service-account.json';

        if (!file_exists($serviceAccountPath)) {
            error_log("FCM Error: service-account.json not found at " . $serviceAccountPath);
            return false;
        }

        try {
            // 1. Get Access Token
            $scopes = ['https://www.googleapis.com/auth/firebase.messaging'];
            $credentials = new \Google\Auth\Credentials\ServiceAccountCredentials($scopes, $serviceAccountPath);
            $accessToken = $credentials->fetchAuthToken(\Google\Auth\HttpHandler\HttpHandlerFactory::build());

            if (!isset($accessToken['access_token'])) {
                error_log("FCM Error: Failed to fetch access token - " . json_encode($accessToken));
                return false;
            }

            $tokenValue = $accessToken['access_token'];

            // 2. Build HTTP v1 Payload
            $url = "https://fcm.googleapis.com/v1/projects/" . $credentials->getProjectId() . "/messages:send";

            $dataPayload = array_merge([
                'title' => $title,
                'body' => $body
            ], $data);

            $message = [
                'message' => [
                    'token' => $token,
                    'data' => $dataPayload
                ]
            ];

            // 3. Send Request
            $json = json_encode($message);
            $headers = [
                'Authorization: Bearer ' . $tokenValue,
                'Content-Type: application/json'
            ];

            $ch = curl_init();
            curl_setopt($ch, CURLOPT_URL, $url);
            curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "POST");
            curl_setopt($ch, CURLOPT_POSTFIELDS, $json);
            curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
            curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
            curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false); // Try disabling SSL verify if there are cert issues

            $rawResponse = curl_exec($ch);
            $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
            $curlError = curl_error($ch);
            curl_close($ch);

            // Log the attempt
            $logMsg = date('[Y-m-d H:i:s] ') . "Target: " . substr($token, 0, 10) . "... | Status: $httpCode | Res: $rawResponse | CurlErr: $curlError\n";
            file_put_contents(__DIR__ . '/../../fcm_log.txt', $logMsg, FILE_APPEND);

            return $httpCode === 200;

        } catch (\Exception $e) {
            error_log("FCM Exception: " . $e->getMessage());
            file_put_contents(__DIR__ . '/../../fcm_log.txt', date('[Y-m-d H:i:s] ') . "Exception: " . $e->getMessage() . "\n", FILE_APPEND);
            return false;
        }
    }

    /**
     * Save notification to database
     * Helper method called after FCM is sent successfully
     */
    private function saveNotificationToDb($memberId, $type, $title, $body, $url = null, $note = null)
    {
        try {
            $notificationManager = new NotificationManager();
            return $notificationManager->saveNotification($memberId, $type, $title, $body, $url, $note);
        } catch (\Exception $e) {
            error_log("NotificationController::saveNotificationToDb Error: " . $e->getMessage());
            return false;
        }
    }
}
