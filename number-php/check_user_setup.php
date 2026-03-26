<?php
$config = require_once 'configs/config.php';
$db = $config['db'];
try {
    $pdo = new PDO("mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'], $db['user'], $db['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    echo "--- Latest 5 Users ---\n";
    $stmt = $pdo->query("SELECT memberid, username, realname, birthday, fcm_token, vipcode FROM membertb ORDER BY memberid DESC LIMIT 5");
    $users = $stmt->fetchAll(PDO::FETCH_ASSOC);
    foreach ($users as $u) {
        echo "ID: {$u['memberid']} | User: {$u['username']} | Name: {$u['realname']} | BDay: {$u['birthday']} | Role: {$u['vipcode']} | Token: " . (empty($u['fcm_token']) ? "EMPTY" : substr($u['fcm_token'], 0, 10) . "...") . "\n";

        $mid = $u['memberid'];
        $bagStmt = $pdo->prepare("SELECT * FROM bagcolortb WHERE memberid = ?");
        $bagStmt->execute([$mid]);
        $bags = $bagStmt->fetchAll(PDO::FETCH_ASSOC);
        if (empty($bags)) {
            echo "  -> NO BAG COLORS FOUND\n";
        } else {
            foreach ($bags as $b) {
                echo "  -> Age: {$b['age']} | C1: {$b['bag_color1']} | Updated: {$b['date_color_updated']}\n";
            }
        }
        echo "----------------------\n";
    }

} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
