<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Load Config
$config = include __DIR__ . '/configs/config.php';

echo "<style>body{font-family:sans-serif;background:#f4f7f6;padding:20px;} table{width:100%;border-collapse:collapse;margin-top:20px;background:white;} th,td{border:1px solid #ddd;padding:12px;text-align:left;} th{background:#20c997;color:white;} .status-ok{color:green;font-weight:bold;} .status-err{color:red;font-weight:bold;} .status-warn{color:orange;font-weight:bold;}</style>";

echo "<h2>🚀 Ananya Upload & System Diagnostic (v3)</h2>";
echo "PHP Version: " . phpversion() . "<br>";
echo "Current User: " . get_current_user() . "<br>";
echo "Error Log Target: " . ini_get('error_log') . "<br>";

// 1. Check Symlink
$symlink = __DIR__ . '/uploads';
echo "<h3>🔗 Symlink Check:</h3>";
if (is_link($symlink)) {
    $target = readlink($symlink);
    echo "Symlink 'uploads' -> <mark>$target</mark> (Valid: " . (is_dir($target) ? "✅" : "❌ No Target Dir") . ")<br>";
} else {
    echo "Symlink 'uploads' <span class='status-warn'>Does Not Exist or is a Real Dir</span>. (If ID 1-4 work, this is fine).<br>";
}

// 2. Database & File Consistency
echo "<h3>📊 Database vs File Content:</h3>";
try {
    $dsn = "mysql:host=" . $config['db']['host'] . ";dbname=" . $config['db']['dbname'] . ";charset=utf8mb4";
    $pdo = new PDO($dsn, $config['db']['user'], $config['db']['pass']);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $stmt = $pdo->query("SELECT id, pang_name, image_url FROM buddha_pang_tb ORDER BY id ASC");
    echo "<table>";
    echo "<tr><th>ID</th><th>Pang Name</th><th>Image URL</th><th>Diagnostic Result</th><th>Action</th></tr>";

    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $url = $row['image_url'];
        // The URL now should include /public based on our fix
        // If it already starts with /public, we use it directly, otherwise prepend public
        $realFile = (strpos($url, '/public') === 0) ? __DIR__ . $url : __DIR__ . '/public' . $url;

        $statusClass = "status-err";
        $statusMsg = "❌ File Missing";

        if (file_exists($realFile)) {
            $size = filesize($realFile);
            $readable = is_readable($realFile);
            $perms = substr(sprintf('%o', fileperms($realFile)), -4);

            if ($size == 0) {
                $statusClass = "status-warn";
                $statusMsg = "⚠️ Corrupt (0 Bytes)";
            } elseif (!$readable) {
                $statusClass = "status-err";
                $statusMsg = "❌ Unreadable (Perms: $perms)";
            } else {
                $statusClass = "status-ok";
                $sizeStr = ($size > 1024 * 1024) ? round($size / (1024 * 1024), 2) . " MB" : round($size / 1024, 2) . " KB";
                $statusMsg = "✅ OK ($sizeStr, $perms)";
            }
        }

        echo "<tr>";
        echo "<td>{$row['id']}</td>";
        echo "<td>{$row['pang_name']}</td>";
        echo "<td><code>" . htmlspecialchars($url) . "</code></td>";
        echo "<td class='$statusClass'>$statusMsg</td>";
        echo "<td><a href='https://ananya.in.th" . htmlspecialchars($url) . "' target='_blank'>Try Open</a></td>";
        echo "</tr>";
    }
    echo "</table>";

} catch (Exception $e) {
    echo "<div class='status-err'>DB Error: " . $e->getMessage() . "</div>";
}

echo "<h3>⚙️ Server Limits:</h3>";
echo "upload_max_filesize: " . ini_get('upload_max_filesize') . "<br>";
echo "post_max_size: " . ini_get('post_max_size') . "<br>";

echo "<h3>📝 Recent Logs:</h3>";
$logFile = ini_get('error_log') ?: __DIR__ . '/error_log';
if (file_exists($logFile) && is_readable($logFile)) {
    $lines = array_slice(file($logFile), -5);
    echo "<pre style='background: #333; color: #fff; padding: 15px; border-radius: 5px;'>" . htmlspecialchars(implode("", $lines)) . "</pre>";
} else {
    echo "No readable error log found at $logFile";
}
?>