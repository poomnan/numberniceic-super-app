<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>🛠️ Ananya Image & Path Fixer (v2)</h2>";

// 1. Check and Fix Symlink
$rootUploads = __DIR__ . '/uploads';
$publicUploads = __DIR__ . '/public/uploads';

echo "<h3>🔗 Symlink Diagnosis:</h3>";
if (!is_link($rootUploads)) {
    echo "<p style='color: orange;'>Symlink 'uploads' is missing in root.</p>";
    if (is_dir($publicUploads)) {
        if (@symlink('public/uploads', 'uploads')) {
            echo "<p style='color: green;'>✅ Successfully created symlink 'uploads' -> 'public/uploads'</p>";
        } else {
            echo "<p style='color: red;'>❌ Failed to create symlink. You might need to ask your hosting to enable symlink or do it via Terminal.</p>";
        }
    }
} else {
    echo "<p style='color: green;'>✅ Symlink 'uploads' already exists.</p>";
}

// 2. Permission Fix
$targetDirs = ['public/uploads/buddha', 'public/uploads/spells', 'public/uploads/temple'];
foreach ($targetDirs as $dir) {
    $fullPath = __DIR__ . '/' . $dir;
    if (is_dir($fullPath)) {
        $files = scandir($fullPath);
        foreach ($files as $file) {
            if ($file !== '.' && $file !== '..')
                @chmod($fullPath . '/' . $file, 0644);
        }
        @chmod($fullPath, 0755);
    }
}
echo "<p style='color: green;'>✅ Standardized file permissions (0644).</p>";

// 3. URL Comparison Test
echo "<h3>🖼️ URL Display Test:</h3>";
echo "<p>Please check which image below is visible:</p>";

// Find a random image to test
$testImg = "";
if (is_dir(__DIR__ . '/public/uploads/buddha')) {
    $bFiles = scandir(__DIR__ . '/public/uploads/buddha');
    foreach ($bFiles as $f) {
        if (preg_match('/\.(jpg|jpeg|png|gif)$/i', $f)) {
            $testImg = $f;
            break;
        }
    }
}

if ($testImg) {
    echo "<div style='display: flex; gap: 20px;'>";
    echo "<div style='text-align: center;'>";
    echo "<b>Option A (No /public)</b><br>";
    echo "<img src='/uploads/buddha/$testImg' style='width: 150px; border: 2px solid #ccc;'><br>";
    echo "<code>/uploads/buddha/$testImg</code>";
    echo "</div>";

    echo "<div style='text-align: center;'>";
    echo "<b>Option B (With /public)</b><br>";
    echo "<img src='/public/uploads/buddha/$testImg' style='width: 150px; border: 2px solid #ccc;'><br>";
    echo "<code>/public/uploads/buddha/$testImg</code>";
    echo "</div>";
    echo "</div>";
} else {
    echo "<p>No images found in buddha folder to test.</p>";
}

echo "<hr><a href='/admin/buddha' style='padding: 10px 20px; background: #20c997; color: white; text-decoration: none; border-radius: 5px;'>Back to Admin</a>";
