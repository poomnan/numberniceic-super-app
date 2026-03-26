<?php
$dir = dirname(__DIR__) . '/cache';
echo "Checking directory: $dir\n";
if (is_dir($dir)) {
    echo "Directory exists.\n";
    if (is_writable($dir)) {
        echo "Directory is writable.\n";
        $file = $dir . '/test.txt';
        if (file_put_contents($file, "test " . date('Y-m-d H:i:s')) !== false) {
            echo "Successfully wrote to file: $file\n";
            echo "Content: " . file_get_contents($file) . "\n";
        } else {
            echo "Failed to write to file.\n";
        }
    } else {
        echo "Directory is NOT writable.\n";
        echo "Owner: " . posix_getpwuid(posix_geteuid())['name'] . "\n";
        echo "Perms: " . substr(sprintf('%o', fileperms($dir)), -4) . "\n";
    }
} else {
    echo "Directory does NOT exist.\n";
}
