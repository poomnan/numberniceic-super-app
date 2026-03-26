<?php
date_default_timezone_set('Asia/Bangkok');
require __DIR__ . '/vendor/autoload.php';
$config = include __DIR__ . '/configs/config.php';

// Check if today is really Wan Pra?
require_once __DIR__ . '/app/Managers/ThaiCalendarHelper.php';
use App\Managers\ThaiCalendarHelper;

$today = date('Y-m-d');
$isWanPra = ThaiCalendarHelper::isWanPra($today);

echo "Today ($today) is Wan Pra? " . ($isWanPra ? 'YES' : 'NO') . "\n";

// Force flag
$force = true; // User requested we fix/send it now.

if (!$isWanPra && !$force) {
    echo "Not Wan Pra. Exiting.\n";
    exit;
}

try {
    $db = new PDO("mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'], $config['db']['user'], $config['db']['pass']);
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $msgTitle = "วันนี้วันพระ";
    $msgBody = "วันนี้วันพระ ขอให้ท่านมีความสุขกาย สบายใจ";

    echo "Fetching tokens...\n";
    $stmt = $db->query("SELECT memberid, fcm_token FROM membertb WHERE fcm_token IS NOT NULL AND fcm_token != ''");
    $users = $stmt->fetchAll(PDO::FETCH_OBJ);

    echo "Found " . count($users) . " users with tokens.\n";

    $success = 0;
    $serviceAccount = __DIR__ . '/configs/service-account.json';

    if (!file_exists($serviceAccount)) {
        die("Error: Service account not found at $serviceAccount\n");
    }

    // Google Auth Setup
    $scopes = ['https://www.googleapis.com/auth/firebase.messaging'];
    $credentials = new \Google\Auth\Credentials\ServiceAccountCredentials($scopes, $serviceAccount);
    $accessToken = $credentials->fetchAuthToken(\Google\Auth\HttpHandler\HttpHandlerFactory::build());
    $tokenValue = $accessToken['access_token'];
    $projectId = $credentials->getProjectId();

    foreach ($users as $u) {
        $data = [
            'type' => 'wanpra',
            'memberid' => (string) $u->memberid,
            'title' => $msgTitle,
            'body' => $msgBody
        ];

        $payload = [
            'message' => [
                'token' => $u->fcm_token,
                'data' => $data
            ]
        ];

        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, "https://fcm.googleapis.com/v1/projects/$projectId/messages:send");
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            "Authorization: Bearer $tokenValue",
            "Content-Type: application/json"
        ]);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        $res = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($httpCode == 200) {
            $success++;
            echo ".";
        } else {
            echo "x";
        }
    }

    echo "\nCompleted. Sent to $success users.\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
