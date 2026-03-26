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
echo "<h3>Migrating Notifications to user_merit_assign table...</h3>";

// 1. Ensure table schema is correct/exists
$sql = "CREATE TABLE IF NOT EXISTS user_merit_assign (
    id INT AUTO_INCREMENT PRIMARY KEY,
    memberid INT NOT NULL,
    merit_type VARCHAR(50) NOT NULL COMMENT 'merit, changenum, spell, etc',
    title VARCHAR(255),
    body TEXT,
    url TEXT,
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_member (memberid),
    KEY idx_type (merit_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

try {
    $pdo->exec($sql);
    echo "Table 'user_merit_assign' checked/created.\n";
} catch (Exception $e) {
    echo "Error creating table: " . $e->getMessage() . "\n";
}

// 2. Fetch Notifications that haven't been migrated (Or just simple sync for user 832 first)
// Focusing on user 832 as requested
$memberIds = [832];

foreach ($memberIds as $mid) {
    echo "Processing Member ID: $mid\n";

    // Clear existing to prevent duplicates during this fix
    $stmt = $pdo->prepare("DELETE FROM user_merit_assign WHERE memberid = :mid");
    $stmt->execute([':mid' => (string) $mid]);

    $stmt = $pdo->prepare("SELECT * FROM notifications WHERE member_id = :mid AND type IN ('merit_assign', 'spell_assign', 'changenum') ORDER BY created_at ASC");
    $stmt->execute([':mid' => (string) $mid]);
    $notis = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $count = 0;
    foreach ($notis as $noti) {
        $type = 'merit'; // default
        $title = $noti['title'];
        $body = $noti['body'];
        $content = strtolower($title . ' ' . $body);

        // Smart Classification
        if ($noti['type'] == 'spell_assign') {
            $type = 'spell';
        } elseif ($noti['type'] == 'merit_assign') {
            if (strpos($content, 'เปลี่ยน') !== false || strpos($content, 'change') !== false || strpos($content, 'ขั้นตอน') !== false) {
                $type = 'changenum';
            } else {
                $type = 'merit';
            }
        }

        // Insert
        // Assuming URL is not in notification table easily, we might leave it empty or try to extract?
        // Notifications table usually no URL column unless JSON data.
        $url = "";

        $sqlIns = "INSERT INTO user_merit_assign (memberid, merit_type, title, body, url, assigned_at) 
                   VALUES (:mid, :type, :title, :body, :url, :at)";
        $stmtIns = $pdo->prepare($sqlIns);
        $stmtIns->execute([
            ':mid' => $mid,
            ':type' => $type,
            ':title' => $title,
            ':body' => $body,
            ':url' => $url,
            ':at' => $noti['created_at']
        ]);
        $count++;
    }
    echo "Migrated $count records for User $mid.\n";
}

// Check Result
echo "<h4>Check Query Result:</h4>";
$stmt = $pdo->prepare("SELECT * FROM user_merit_assign WHERE memberid = 832");
$stmt->execute();
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));

echo "</pre>";
