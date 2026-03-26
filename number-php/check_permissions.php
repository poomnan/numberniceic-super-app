<?php
$dirs = [
    'public/uploads',
    'public/uploads/buddha',
    'public/uploads/spells',
    'public/uploads/temple'
];

echo "<h2>Checking Permissions...</h2>";
foreach ($dirs as $dir) {
    $fullPath = __DIR__ . '/' . $dir;
    if (!is_dir($fullPath)) {
        if (@mkdir($fullPath, 0755, true)) {
            echo "<p style='color: green;'>Created directory: $dir</p>";
        } else {
            echo "<p style='color: red;'>Failed to create directory: $dir (Check parent permissions)</p>";
            continue;
        }
    }

    if (is_writable($fullPath)) {
        echo "<p style='color: green;'>Directory is WRITABLE: $dir</p>";
    } else {
        echo "<p style='color: red;'>Directory is NOT WRITABLE: $dir (Trying to fix...)</p>";
        @chmod($fullPath, 0755);
        if (is_writable($fullPath)) {
            echo "<p style='color: green;'>Fixed! Directory is now writable: $dir</p>";
        } else {
            @chmod($fullPath, 0777); // Last resort for some shared hosts
            if (is_writable($fullPath)) {
                echo "<p style='color: orange;'>Fixed with 777! Directory is now writable: $dir</p>";
            } else {
                echo "<p style='color: red;'>CRITICAL: Could not make $dir writable. Please contact server admin.</p>";
            }
        }
    }
}

echo "<h3>PHP Upload Limits:</h3>";
echo "upload_max_filesize: " . ini_get('upload_max_filesize') . "<br>";
echo "post_max_size: " . ini_get('post_max_size') . "<br>";
echo "memory_limit: " . ini_get('memory_limit') . "<br>";
