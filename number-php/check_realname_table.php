<?php
require_once __DIR__ . '/configs/config.php';
try {
    $db = $config['db'];
    $pdo = new PDO(
        "mysql:host=" . $db['host'] . ";dbname=" . $db['dbname'] . ";charset=utf8",
        $db['user'],
        $db['pass']
    );
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $thainame = "ชัญชนันท์";
    $reangthai = "244254519";
    $leksat_thai = "36";
    $shadow = "54";

    $sqlx = "SELECT thainame FROM realname WHERE thainame = :thainame";
    $resultx = $pdo->prepare($sqlx);
    if ($resultx->execute([':thainame' => $thainame])) {
        $data = $resultx->fetchAll(PDO::FETCH_OBJ);
        if (count($data) > 0) {
            echo "Duplicate\n";
        } else {
            $sql = "INSERT INTO realname (thainame, reangthai, leksat_thai, shadow) VALUES (:thainame, :reangthai, :leksat_thai, :shadow)";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([
                ':thainame' => $thainame,
                ':reangthai' => $reangthai,
                ':leksat_thai' => $leksat_thai,
                ':shadow' => $shadow
            ]);
            echo "Insert successful!\n";
        }
    }
} catch (\Exception $e) {
    echo "Query failed: " . $e->getMessage() . "\n";
}
