<?php
// Clear all caches
if (function_exists('opcache_reset')) {
    opcache_reset();
    echo "✅ OPcache cleared\n";
} else {
    echo "⚠️ OPcache not available\n";
}

if (function_exists('apc_clear_cache')) {
    apc_clear_cache();
    echo "✅ APC cache cleared\n";
}

// Touch the file to force reload
$file = __DIR__ . '/app/Managers/UserController.php';
touch($file);
echo "✅ Touched UserController.php\n";

// Verify the change is in the file
$content = file_get_contents($file);
if (strpos($content, '+6 months') !== false) {
    echo "✅ File contains: +6 months\n";
} else {
    echo "❌ File still contains: +1 year\n";
}

echo "\nNow test the API again!\n";
?>
