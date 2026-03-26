<?php
// Check the actual code in UserController.php
$file = __DIR__ . '/app/Managers/UserController.php';
$content = file_get_contents($file);

// Check if the 6 months fix is present
if (strpos($content, '+6 months') !== false) {
    echo "✅ UserController.php has been updated with 6 months fix\n";
} else {
    echo "❌ UserController.php still has old code (1 year)\n";
}

// Find the line
preg_match('/strtotime\(\'\+([^\']+)\'\)/', $content, $matches);
if (isset($matches[1])) {
    echo "Current setting: +" . $matches[1] . "\n";
}

// Check file modification time
echo "File modified: " . date('Y-m-d H:i:s', filemtime($file)) . "\n";
?>
