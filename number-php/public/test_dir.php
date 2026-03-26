<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

$dir = "uploads/testdir";
if (!is_dir($dir)) {
    mkdir($dir, 0777, true);
    chmod($dir, 0777);
}

file_put_contents("$dir/test.txt", "Hello testdir");
chmod("$dir/test.txt", 0666);

echo "Created $dir/test.txt";
?>