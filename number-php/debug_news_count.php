<?php
require 'configs/config.php';

// Count total
$stmt = $db->query("SELECT COUNT(*) FROM news");
$count = $stmt->fetchColumn();
echo "Total News Count: " . $count . "\n";

// Check data size of first 5
$stmt = $db->query("SELECT news_headline, LENGTH(news_detail) as detail_len, news_pic_header FROM news LIMIT 5");
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
print_r($rows);
