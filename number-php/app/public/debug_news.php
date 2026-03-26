<?php
header('Content-Type: application/json; charset=utf-8');
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

try {
    // 1. Config & Connect DB
    if (!file_exists(__DIR__ . '/../configs/config.php')) {
        throw new Exception("Config file not found");
    }
    require __DIR__ . '/../configs/config.php';

    $dbConfig = $config['db'];
    $dsn = "mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8";
    $pdo = new PDO($dsn, $dbConfig['user'], $dbConfig['pass']);
    $pdo->exec("SET NAMES utf8");

    // 2. Define Parameters (Hardcoded as per NewsController)
    $host = "https://numberniceic.online";
    $cols = "newsid, news_headline, news_pic_header, news_headline as news_title_short, news_desc, news_date, fix, hashtag1, hashtag2, hashtag3, hashtag4, hashtag5, hashtag6, news_detail";

    // 3. Helper Function to Map Data
    $mapArticle = function ($article) use ($host) {
        $id = $article['newsid'] ?? '';
        $headline = $article['news_headline'] ?? '';
        $short = $article['news_title_short'] ?? $headline;
        // Truncate desc logic
        $rawDesc = strip_tags($article['news_detail'] ?? '');
        $desc = $article['news_desc'] ?? mb_substr($rawDesc, 0, 100);

        $img = $article['news_pic_header'] ?? '';
        if (!empty($img) && strpos($img, 'http') !== 0) {
            $img = $host . '/' . ltrim($img, '/');
        }

        return [
            'newsid' => (string) $id,
            'news_headline' => $headline,
            'news_title_short' => $short,
            'news_desc' => $desc,
            'news_detail' => $article['news_detail'] ?? '',
            'news_pic_header' => $img,
            'news_date' => $article['news_date'] ?? '',
            'category' => $article['category_name'] ?? 'ทั่วไป',
            'fix' => (string) ($article['fix'] ?? '0')
        ];
    };

    // 4. Fetch Category Names
    $catStmt = $pdo->query("SELECT category_id, category_name FROM news_categories");
    $categories = $catStmt->fetchAll(PDO::FETCH_KEY_PAIR);

    // 5. Query Function
    $processCategory = function ($sql, $limit, $catName) use ($pdo, $mapArticle) {
        $stmt = $pdo->query($sql);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
        $uniqueData = [];
        $seen = [];
        foreach ($rows as $r) {
            $h = $r['news_headline'];
            if (!in_array($h, $seen)) {
                $seen[] = $h;
                $r['category_name'] = $catName;
                $uniqueData[] = $mapArticle($r);
            }
            if (count($uniqueData) >= $limit)
                break;
        }
        return $uniqueData;
    };

    // 6. Execute Logic (Same as NewsController::newsTop24)
    $dataHot = $processCategory(
        "SELECT $cols FROM news WHERE fix IN ('1', '2', '3', '4', '5') ORDER BY fix ASC LIMIT 20",
        7,
        $categories[7] ?? 'ข่าวและบทความที่น่าสนใจ'
    );

    $dataFeedback = $processCategory(
        "SELECT $cols FROM news WHERE hashtag1 = 1 ORDER BY newsid DESC LIMIT 20",
        4,
        $categories[1] ?? 'Review จากลูกค้า'
    );

    $dataPhoneNum = $processCategory(
        "SELECT $cols FROM news WHERE hashtag2 = 1 ORDER BY newsid DESC LIMIT 20",
        4,
        $categories[2] ?? 'วิธีเลือกซื้อเบอร์โทรศัพท์มงคล'
    );

    $dataNameSur = $processCategory(
        "SELECT $cols FROM news WHERE hashtag3 = 1 ORDER BY newsid DESC LIMIT 20",
        4,
        $categories[3] ?? 'ทำนายดวงชะตาจากชื่อ-สกุล'
    );

    $dataTabian = $processCategory(
        "SELECT $cols FROM news WHERE hashtag4 = 1 ORDER BY newsid DESC LIMIT 20",
        4,
        $categories[4] ?? 'ทำนายดวงชะตาจากทะเบียนรถ'
    );

    $dataHomeNum = $processCategory(
        "SELECT $cols FROM news WHERE hashtag5 = 1 ORDER BY newsid DESC LIMIT 20",
        4,
        $categories[5] ?? 'ทำนายดวงชะตาจากบ้านเลขที่'
    );

    $dataConcept = $processCategory(
        "SELECT $cols FROM news WHERE hashtag6 = 1 ORDER BY newsid DESC LIMIT 20",
        4,
        $categories[6] ?? 'หลักการเลือกทะเบียนรถ'
    );

    // 7. Output Final JSON
    $finalData = [
        "news_hot" => $dataHot,
        "news_feedback" => $dataFeedback,
        "news_phonenum" => $dataPhoneNum,
        "news_namesur" => $dataNameSur,
        "news_tabian" => $dataTabian,
        "news_homenum" => $dataHomeNum,
        "news_concept" => $dataConcept
    ];

    echo json_encode($finalData, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT | JSON_INVALID_UTF8_IGNORE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}
?>