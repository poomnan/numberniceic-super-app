<?php

$config = require __DIR__ . '/configs/config.php';
$dbConfig = $config['db'];

function normalizeImageUrl(string $url): string
{
    $url = trim($url);
    if ($url === '') {
        return '';
    }
    if (strpos($url, '//') === 0) {
        return 'https:' . $url;
    }
    if (strpos($url, 'http://') === 0 || strpos($url, 'https://') === 0) {
        return $url;
    }
    return 'http://43.228.85.200:81/' . ltrim($url, '/');
}

function extensionFromUrl(string $url): string
{
    $path = parse_url($url, PHP_URL_PATH) ?? '';
    $ext = strtolower(pathinfo($path, PATHINFO_EXTENSION));
    if (!in_array($ext, ['jpg', 'jpeg', 'png', 'gif', 'webp'], true)) {
        return 'jpg';
    }
    return $ext;
}

try {
    $pdo = new PDO(
        "mysql:host={$dbConfig['host']};dbname={$dbConfig['dbname']};charset=utf8",
        $dbConfig['user'],
        $dbConfig['pass'],
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
    );

    $basePublicDir = is_dir(__DIR__ . '/public') ? (__DIR__ . '/public') : (__DIR__ . '/app/public');
    $uploadDir = $basePublicDir . '/uploads/news';
    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0755, true);
    }

    $stmt = $pdo->query("SELECT newsid, news_pic_header, photo FROM news ORDER BY newsid DESC");
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $update = $pdo->prepare("UPDATE news SET photo = :photo WHERE newsid = :id");

    $ok = 0;
    $skip = 0;
    $fail = 0;

    foreach ($rows as $row) {
        $id = (string)($row['newsid'] ?? '');
        $existing = trim((string)($row['photo'] ?? ''));
        if ($id === '') {
            $skip++;
            continue;
        }
        if ($existing !== '' && file_exists($uploadDir . '/' . $existing)) {
            $skip++;
            continue;
        }

        $source = normalizeImageUrl((string)($row['news_pic_header'] ?? ''));
        if ($source === '') {
            $skip++;
            continue;
        }

        $context = stream_context_create([
            'http' => ['timeout' => 20, 'follow_location' => 1, 'user_agent' => 'NewsPhotoSync/1.0'],
            'ssl' => ['verify_peer' => false, 'verify_peer_name' => false]
        ]);

        $content = @file_get_contents($source, false, $context);
        if ($content === false || strlen($content) < 256) {
            $fail++;
            continue;
        }

        $ext = extensionFromUrl($source);
        $filename = "news_{$id}.{$ext}";
        $target = $uploadDir . '/' . $filename;
        if (@file_put_contents($target, $content) === false) {
            $fail++;
            continue;
        }

        $update->execute([':photo' => $filename, ':id' => $id]);
        $ok++;
    }

    echo "sync complete: ok={$ok}, skip={$skip}, fail={$fail}\n";
} catch (Throwable $e) {
    fwrite(STDERR, "Sync failed: " . $e->getMessage() . "\n");
    exit(1);
}
