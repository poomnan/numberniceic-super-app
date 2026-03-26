<?php
$config = include 'configs/config.php';
try {
    $db = new PDO("mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'], $config['db']['user'], $config['db']['pass']);
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Delete rows that have empty or incorrect buddha_day for special pangs
    // These are the old duplicates we found: IDs 10, 12, 13, 14, 15
    $ids_to_delete = [10, 12, 13, 14, 15];

    $stmt = $db->prepare("DELETE FROM buddha_pang_tb WHERE id = ?");
    foreach ($ids_to_delete as $id) {
        $stmt->execute([$id]);
        echo "Deleted old record ID: $id\n";
    }

    echo "Cleanup completed.\n";
} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
