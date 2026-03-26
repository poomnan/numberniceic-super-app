<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

$dir = "uploads/buddha/temple";
if (!is_dir($dir))
    mkdir($dir, 0777, true);

file_put_contents("$dir/test.txt", "Hello temple inside buddha");
chmod("$dir/test.txt", 0666);
?>