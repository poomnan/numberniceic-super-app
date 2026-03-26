<?php
header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
ini_set('display_errors', 1);
error_reporting(E_ALL);

try {
    if (!file_exists(__DIR__ . '/../configs/config.php')) {
        throw new Exception("Config file not found");
    }
    require __DIR__ . '/../configs/config.php';

    $dbConfig = $config['db'];
    $dsn = "mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8";
    $pdo = new PDO($dsn, $dbConfig['user'], $dbConfig['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->exec("SET NAMES utf8");

    $sql = "SELECT * FROM tabian_sell WHERE tabian_status = 'available' ORDER BY order_no ASC, tabian_id DESC";
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $data = $stmt->fetchAll(PDO::FETCH_OBJ);

    $output = [];
    foreach ($data as $row) {
        $output[] = [
            'tabian_id' => (int) $row->tabian_id,
            'tabian_number' => (string) $row->tabian_number,
            'tabian_province' => (string) ($row->tabian_province ?? 'กรุงเทพมหานคร'),
            'tabian_price' => (int) $row->tabian_price,
            'tabian_status' => (string) $row->tabian_status,
            'tabian_category' => (string) ($row->tabian_category ?? ''),
            'tabian_tag' => (string) ($row->tabian_tag ?? ''),
            'order_no' => (int) ($row->order_no ?? 0),
            'created_at' => (string) ($row->created_at ?? ''),
            'updated_at' => (string) ($row->updated_at ?? '')
        ];
    }
    echo json_encode($output, JSON_UNESCAPED_UNICODE);
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}
?>