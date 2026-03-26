<?php
if ($argc < 2) {
    die("Usage: php add_kating_day.php <YYYY-MM-DD> [Description]\nExample: php add_kating_day.php 2026-02-12 'วันกระทิงวัน'\n");
}

$date = $argv[1];
$desc = isset($argv[2]) ? $argv[2] : 'วันกระทิงวัน';
$detail = isset($argv[3]) ? $argv[3] : '';

// Validate date format
if (!preg_match("/^\d{4}-\d{2}-\d{2}$/", $date)) {
    die("Error: Invalid date format. Use YYYY-MM-DD.\n");
}

require __DIR__ . '/vendor/autoload.php';
$configFile = __DIR__ . '/configs/config.php';
$config = require $configFile;
$db = $config['db'];

try {
    $pdo = new PDO("mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'], $db['user'], $db['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->exec("set names utf8mb4");

    // Check if date already exists
    $stmt = $pdo->prepare("SELECT count(*) FROM dayspecialtb WHERE wan_date = ?");
    $stmt->execute([$date]);
    if ($stmt->fetchColumn() > 0) {
        // Update existing
        $sql = "UPDATE dayspecialtb SET wan_kating = '1', wan_desc = COALESCE(NULLIF(?, ''), wan_desc) WHERE wan_date = ?";
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$desc, $date]);
        echo "Updated existing record for $date to be a Wan Kating.\n";
    } else {
        // Insert new
        $sql = "INSERT INTO dayspecialtb (wan_date, wan_desc, wan_detail, wan_pra, wan_kating, wan_tongchai, wan_atipbadee) VALUES (?, ?, ?, '0', '1', '0', '0')";
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$date, $desc, $detail]);
        echo "Inserted new Wan Kating for $date.\n";
    }

    // Also ensure it exists in auspicious_days so it shows up in the calendar list (API relies on this table for the main loop)
    $stmt2 = $pdo->prepare("SELECT count(*) FROM auspicious_days WHERE date = ?");
    $stmt2->execute([$date]);
    if ($stmt2->fetchColumn() == 0) {
        $pdo->prepare("INSERT INTO auspicious_days (date, is_wanpra, is_tongchai, is_atipbadee) VALUES (?, 0, 0, 0)")->execute([$date]);
        echo "Added date entry to auspicious_days table as well.\n";
    }

} catch (PDOException $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
