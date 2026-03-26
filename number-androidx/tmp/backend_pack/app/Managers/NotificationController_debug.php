<?php

namespace App\Managers;

use App\Managers\PersonManager;
use PDO;

class NotificationController extends Manager
{
    public function sendBagColors($request, $response)
    {
        // Check session (Redundant if middleware does it, but safe)
        if (session_status() == PHP_SESSION_NONE)
            session_start();
        $userSession = $_SESSION['user'] ?? null;
        if (!$userSession || (!in_array(strtolower($userSession->vipcode ?? ''), ['admin', 'administrator']))) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Unauthorized']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(403);
        }

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

            if ($bag) {
                $title = "สีกระเป๋ามงคล (อายุ $ageYear)";
                $body = "คลิกเพื่อดูสีกระเป๋าเสริมดวงของคุณวันนี้!";

                // Sending logic
                $res = $this->sendFcm($user->fcm_token, $title, $body, ['type' => 'bag_color', 'memberid' => (string) $user->memberid]);
                $status = $res ? 'sent' : 'failed';
                if ($res)
                    $sentCount++;

                $results[] = [
                    'memberid' => $user->memberid,
                    'age' => $ageYear,
                    'status' => $status
                ];
            } else {
                $results[] = [
                    'memberid' => $user->memberid,
                    'age' => $ageYear,
                    'status' => 'no_bag_color_found'
                ];
            }
        }

        $response->getBody()->write(json_encode(['status' => 'completed', 'sent_count' => $sentCount, 'details' => $results]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    private function sendFcm($token, $title, $body, $data = [])
    {
        $serviceAccountPath = __DIR__ . '/../../configs/service-account.json';

        if (!file_exists($serviceAccountPath)) {
            // Log error
            return false;
        }

        try {
            // 1. Get Access Token
            $scopes = ['https://www.googleapis.com/auth/firebase.messaging'];
            $credentials = \Google\Auth\Credentials\ServiceAccountCredentials::fromJsonFile($serviceAccountPath, $scopes);
            $accessToken = $credentials->fetchAuthToken(\Google\Auth\HttpHandler\HttpHandlerFactory::build());
            $tokenValue = $accessToken['access_token'];

            // 2. Build HTTP v1 Payload
            // Note: HTTP v1 uses a different payload structure than Legacy
            $url = "https://fcm.googleapis.com/v1/projects/" . $credentials->getProjectId() . "/messages:send";

            $message = [
                'message' => [
                    'token' => $token,
                    'notification' => [
                        'title' => $title,
                        'body' => $body
                    ],
                    // Data must be all strings
                    'data' => array_map('strval', $data)
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

            $response = curl_exec($ch);
            $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
            curl_close($ch);

            return $httpCode === 200;

        } catch (\Exception $e) {
            // Log exception
            return false;
        }
    }
}
