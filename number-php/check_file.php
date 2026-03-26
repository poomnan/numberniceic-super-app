<?php
echo "File: views/changenum/phone.phtml\n";
echo "-------------------------------\n";
if (file_exists('views/changenum/phone.phtml')) {
    echo "Content contains web_menu: " . (strpos(file_get_contents('views/changenum/phone.phtml'), 'web_menu') !== false ? 'YES' : 'NO') . "\n";
    echo "First 100 characters: " . substr(file_get_contents('views/changenum/phone.phtml'), 0, 100) . "\n";
} else {
    echo "File not found\n";
}
?>