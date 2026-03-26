<?php
require_once 'configs/config.php';

try {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'],
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    $pdo->exec("set names utf8mb4");

    $sql = "SELECT m.activity, m.dayx as birth_day, m.dayy as target_day, m.action, md.mira_desc 
            FROM miracledo m 
            LEFT JOIN miracledo_desc md ON m.mira_id = md.mira_id";

    $stmt = $pdo->query($sql);
    $results = $stmt->fetchAll();

    $structuredData = [
        "metadata" => [
            "title" => "วันมงคลในการประกอบกิจกรรม",
            "version" => "1.0",
            "last_updated" => date('Y-m-d H:i:s'),
            "description" => "ข้อมูลวันมงคลแยกตามวันเกิดและกิจกรรมต่างๆ"
        ],
        "days_translation" => [
            "sunday" => "วันอาทิตย์",
            "monday" => "วันจันทร์",
            "tuesday" => "วันอังคาร",
            "wednesday" => "วันพุธ",
            "wednesday1" => "วันพุธหัวค่ำ/กลางวัน",
            "wednesday2" => "วันพุธกลางคืน",
            "thursday" => "วันพฤหัสบดี",
            "friday" => "วันศุกร์",
            "saturday" => "วันเสาร์"
        ],
        "data" => []
    ];

    foreach ($results as $row) {
        $birthDay = $row['birth_day'];
        $activity = $row['activity'];
        $targetDay = $row['target_day'];

        if (!isset($structuredData['data'][$birthDay])) {
            $structuredData['data'][$birthDay] = [];
        }

        if (!isset($structuredData['data'][$birthDay][$activity])) {
            $structuredData['data'][$birthDay][$activity] = [];
        }

        $structuredData['data'][$birthDay][$activity][$targetDay] = [
            "is_good" => (int) $row['action'] === 1,
            "description" => $row['mira_desc']
        ];
    }

    file_put_contents('miracle_data.json', json_encode($structuredData, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE));
    echo "JSON file created successfully: miracle_data.json\n";

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
