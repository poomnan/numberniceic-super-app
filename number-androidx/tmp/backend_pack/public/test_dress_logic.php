<?php
require_once __DIR__ . '/../configs/config.php';
$config = require __DIR__ . '/../configs/config.php';

try {
    $dsn = "mysql:host=127.0.0.1;dbname=" . $config['db']['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $config['db']['user'], $config['db']['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    die("Connection failed: " . $e->getMessage());
}

function getDressColorMock($pdo, $dayListStr)
{
    $strColor = array();

    // 1. Fetch specifically requested IDs first
    for ($i = 0; $i < strlen($dayListStr); $i++) {
        $char = $dayListStr[$i];
        $sql = "SELECT * FROM colortb WHERE colorid = '$char'";
        $stmt = $pdo->prepare($sql);
        $stmt->execute();
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($row) {
            array_push($strColor, $row);
        }
    }

    echo "Initial count for '$dayListStr': " . count($strColor) . "\n";

    // 2. Fill to 8
    if (count($strColor) < 8) {
        $existingIds = array_column($strColor, 'colorid');
        for ($id = 1; $id <= 8; $id++) {
            if (count($strColor) >= 8)
                break;
            if (!in_array((string) $id, $existingIds)) {
                $sql = "SELECT * FROM colortb WHERE colorid = '$id'";
                $stmt = $pdo->prepare($sql);
                $stmt->execute();
                $row = $stmt->fetch(PDO::FETCH_ASSOC);
                if ($row) {
                    array_push($strColor, $row);
                }
            }
        }
    }

    echo "Final count: " . count($strColor) . "\n";
    foreach ($strColor as $item) {
        echo " - ID: " . $item['colorid'] . " Code: " . $item['color_code1'] . "\n";
    }
}

// Test with a short input (e.g., 5 days)
echo "--- Testing with input '12245' (5 chars) ---\n";
getDressColorMock($pdo, "12245");

// Test with very short input
echo "\n--- Testing with input '1' (1 char) ---\n";
getDressColorMock($pdo, "1");

?>