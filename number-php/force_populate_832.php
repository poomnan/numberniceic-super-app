<?php
require_once 'vendor/autoload.php';
require_once 'configs/config.php';

$dbConfig = $config['db'];
$pdo = new PDO(
    "mysql:host=" . $dbConfig['host'] . ";dbname=" . $dbConfig['dbname'],
    $dbConfig['user'],
    $dbConfig['pass']
);
$pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
$pdo->exec("set names utf8mb4");

echo "<pre>";
echo "<h3>Force Populating user_merit_assign for User 832</h3>";

$mid = 832;

// 1. Clear existing for clean state
$pdo->exec("DELETE FROM user_merit_assign WHERE memberid = $mid");
echo "Cleared existing data for $mid.\n";

// 2. Prepare Data to Insert (Based on Notification Logs)
$data = [
    // Merit
    [
        'mid' => $mid,
        'type' => 'merit',
        'title' => 'วิธีการทำบุญ',
        'body' => 'คุณนินแนะนำ : ปางวันเสาร์',
        'url' => '',
        'at' => '2026-02-02 02:34:10'
    ],
    [
        'mid' => $mid,
        'type' => 'merit',
        'title' => 'วิธีการทำบุญ',
        'body' => 'คุณนินแนะนำ : ปางวันจันทร์',
        'url' => '',
        'at' => '2026-02-02 02:33:56'
    ],
    // Change
    [
        'mid' => $mid,
        'type' => 'changenum',
        'title' => 'ขั้นตอนการเปลี่ยนแปลง',
        'body' => 'คุณนินแนะนำ : ขั้นตอนการเปลี่ยนแปลงเบอร์โทรศัพท์',
        'url' => '',
        'at' => '2026-02-02 02:34:48'
    ],
    [
        'mid' => $mid,
        'type' => 'changenum',
        'title' => 'ขั้นตอนการเปลี่ยนแปลง',
        'body' => 'คุณนินแนะนำ : ขั้นตอนการเปลี่ยนแปลงชื่อจริง นามสกุล',
        'url' => '',
        'at' => '2026-02-02 02:34:31'
    ],
    // Spell (Just in case, though app might use another table/logic, but we support spell type in API)
    [
        'mid' => $mid,
        'type' => 'spell',
        'title' => 'คาถาและคำเตือนพิเศษ',
        'body' => 'คุณนินแนะนำ : คาถาชินบัญชร โดยสมเด็จพระพุฒาจารย์ (โต พรหมรังสี)',
        'url' => '',
        'at' => '2026-02-02 02:15:07'
    ]
];

$sql = "INSERT INTO user_merit_assign (memberid, merit_type, title, body, url, assigned_at) 
        VALUES (:mid, :type, :title, :body, :url, :at)";
$stmt = $pdo->prepare($sql);

$count = 0;
foreach ($data as $row) {
    $stmt->execute([
        ':mid' => $row['mid'],
        ':type' => $row['type'],
        ':title' => $row['title'],
        ':body' => $row['body'],
        ':url' => $row['url'],
        ':at' => $row['at']
    ]);
    $count++;
}

echo "Successfully inserted $count records.\n";

// 3. Verify
$stmt = $pdo->prepare("SELECT * FROM user_merit_assign WHERE memberid = :mid ORDER BY merit_type, assigned_at DESC");
$stmt->execute([':mid' => $mid]);
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

echo "<h4>Current Data in DB:</h4>";
print_r($rows);

echo "</pre>";
