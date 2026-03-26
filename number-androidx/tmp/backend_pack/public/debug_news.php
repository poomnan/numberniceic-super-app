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

    $cols = "newsid, news_headline, news_pic_header, news_headline as news_title_short, news_desc, news_date, fix, hashtag1, hashtag2, hashtag3, hashtag4, hashtag5, hashtag6, news_detail";
    $host = "https://numberniceic.online";

    $mapFunc = function ($article) use ($host) {
        $id = $article['newsid'];
        $headline = $article['news_headline'];
        $short = $article['news_title_short'] ?? $headline;
        $desc = $article['news_desc'] ?? '';
        $img = $article['news_pic_header'] ?? '';
        $detail = $article['news_detail'] ?? '';
        $fix = $article['fix'] ?? '0';
        $cat = 'ทั่วไป';

        if (!empty($img) && strpos($img, 'http') !== 0) {
            $img = $host . '/' . ltrim($img, '/');
        }

        return [
            'newsid' => (string) $id,
            'news_headline' => (string) $headline,
            'news_title_short' => (string) $short,
            'news_desc' => (string) $desc,
            'news_detail' => (string) $detail,
            'news_pic_header' => (string) $img,
            'category' => (string) $cat,
            'fix' => (string) $fix
        ];
    };

    // 1. Hot News
    $stmt = $pdo->prepare("SELECT $cols FROM news WHERE fix IN ('1', '2', '3', '4', '5') ORDER BY fix ASC LIMIT 10");
    $stmt->execute();
    $dataHot = array_map($mapFunc, $stmt->fetchAll(PDO::FETCH_ASSOC));

    // 2. Feedback
    $stmt = $pdo->prepare("SELECT $cols FROM news WHERE hashtag1 = 1 ORDER BY newsid DESC LIMIT 10");
    $stmt->execute();
    $dataFeedback = array_map($mapFunc, $stmt->fetchAll(PDO::FETCH_ASSOC));

    // 3. Phonenum
    $stmt = $pdo->prepare("SELECT $cols FROM news WHERE hashtag2 = 1 ORDER BY newsid DESC LIMIT 10");
    $stmt->execute();
    $dataPhone = array_map($mapFunc, $stmt->fetchAll(PDO::FETCH_ASSOC));

    echo json_encode([
        "news_hot" => $dataHot,
        "news_feedback" => $dataFeedback,
        "news_phonenum" => $dataPhone,
        "news_namesur" => [],
        "news_tabian" => [],
        "news_homenum" => [],
        "news_concept" => []
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}
?>