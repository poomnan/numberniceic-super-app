<?php
// check_size.php - Full Logic Simulation

// 1. Load Config
if (file_exists(__DIR__ . '/../configs/config.php')) {
    require __DIR__ . '/../configs/config.php';
} else {
    die("Error: config.php not found.");
}

try {
    // 2. Connect DB
    $db = $config['db'];
    $pdo = new PDO("mysql:host={$db['host']};dbname={$db['dbname']};charset=utf8", $db['user'], $db['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->exec("set names utf8");

    // 3. Simulate Query (News Type 0 - Hot News)
    $sql = "SELECT * FROM news WHERE fix IN ('1', '2', '3', '4', '5') ORDER BY fix ASC LIMIT 100";
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $dataRaw = $stmt->fetchAll(PDO::FETCH_ASSOC);

    // 4. Simulate Mapping Logic (EXACTLY as in NewsController)
    $protocol = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http");
    $host = $protocol . "://" . $_SERVER['HTTP_HOST'];
    $categoryName = 'ข่าวและบทความที่น่าสนใจ'; // Default for Type 0

    // Callback Function (The logic we just fixed)
    $mapArticle = function ($article) use ($host, $categoryName) {
        $id = $article['newsid'] ?? $article['news_id'] ?? $article['id'] ?? $article['ID'] ?? '';
        $headline = $article['news_headline'] ?? $article['news_topic'] ?? $article['topic'] ?? $article['news_header'] ?? $article['head_text'] ?? $article['title'] ?? $article['name'] ?? $article['subject'] ?? '';
        $short = $article['news_title_short'] ?? $article['news_short'] ?? $headline;
        // Logic check: strip_tags might be heavy if content is huge
        $desc = $article['news_desc'] ?? $article['intro'] ?? $article['description'] ?? $article['excerpt'] ?? mb_substr(strip_tags($article['news_detail'] ?? $article['detail'] ?? $article['content'] ?? $article['body'] ?? ''), 0, 100);
        $img = $article['news_pic_header'] ?? $article['news_picture'] ?? $article['photo'] ?? $article['photo1'] ?? $article['cover'] ?? $article['image'] ?? $article['img'] ?? $article['file_name'] ?? $article['url'] ?? '';
        $date = $article['news_date'] ?? $article['created_at'] ?? $article['date'] ?? $article['published_at'] ?? '';
        $cat = $article['category_name'] ?? $article['category'] ?? $categoryName;

        // The Missing Fields we added
        $fix = $article['fix'] ?? '0';
        $detail = $article['news_detail'] ?? $article['detail'] ?? $article['content'] ?? '';

        if (!empty($img) && strpos($img, 'http') !== 0) {
            $img = $host . '/' . ltrim($img, '/');
        }

        return [
            'newsid' => $id,
            'news_headline' => $headline,
            'news_title_short' => $short,
            'news_desc' => $desc,
            'news_detail' => $detail,     // Added field
            'news_pic_header' => $img,
            'news_date' => $date,
            'category' => $cat,
            'fix' => $fix                 // Added field
        ];
    };

    // 5. Deduplicate and Map
    $dataRawUnique = [];
    $seenHeadlines = [];
    foreach ($dataRaw as $r) {
        $h = $r['news_headline'];
        if (!in_array($h, $seenHeadlines)) {
            $seenHeadlines[] = $h;
            $dataRawUnique[] = $r;
        }
        if (count($dataRawUnique) >= 50)
            break;
    }

    $dataWithCat = [];
    foreach ($dataRawUnique as $r) {
        $dataWithCat[] = $mapArticle($r);
    }

    $finalStructure = array("type_id" => "0", "news_all_type" => $dataWithCat);

    // 6. Encode JSON
    $json = json_encode($finalStructure, JSON_UNESCAPED_UNICODE | JSON_INVALID_UTF8_IGNORE);

    // 7. Output Result
    echo "<html><body style='font-family: monospace; padding: 20px;'>";

    if ($json === false) {
        echo "<h1 style='color:red;'>❌ JSON ENCODING ERROR: " . json_last_error_msg() . "</h1>";
        // Find which item is causing utf8 error
        echo "<h3>Finding corrupted character...</h3>";
        foreach ($dataWithCat as $idx => $item) {
            if (json_encode($item) === false) {
                echo "Error at Item Index $idx: " . $item['news_headline'] . "<br>";
            }
        }
    } else {
        echo "<h1 style='color:green;'>✅ JSON Encoding Successful</h1>";
        echo "Items: " . count($dataWithCat) . "<br>";
        echo "Total Size: " . number_format(strlen($json) / 1024, 2) . " KB<br>";
        echo "<hr>";
        echo "<h3>First Item Structure (What Android Gets):</h3>";
        echo "<pre style='background:#eee; padding:10px;'>" . htmlspecialchars(json_encode($dataWithCat[0], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE)) . "</pre>";
        echo "<hr>";
        echo "<h3>Full Raw JSON (Copy this to check in JSON Validator):</h3>";
        echo "<textarea style='width:100%; height:300px;'>" . htmlspecialchars($json) . "</textarea>";
    }
    echo "</body></html>";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage();
}
?>