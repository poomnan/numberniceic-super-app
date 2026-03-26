<?php
header('Content-Type: application/json; charset=utf-8');

// Manual Mock Data for Android Testing
$mockItem = [
    "newsid" => "100",
    "news_headline" => "ทดสอบข่าว Manual",
    "news_title_short" => "ทดสอบข่าว Manual Short",
    "news_desc" => "นี่คือข้อมูลทดสอบจากไฟล์ Static JSON เพื่อเช็คว่าแอพ Android ทำงานปกติหรือไม่",
    "news_detail" => "<p>รายละเอียดข่าวทดสอบ...</p>",
    "news_pic_header" => "https://numberniceic.online/uploads/test.jpg", // Ensure valid URL structure
    "news_date" => "2024-01-27 10:00:00",
    "category" => "หมวดทดสอบ",
    "fix" => "1"
];

$data = [
    "news_hot" => [$mockItem, $mockItem],
    "news_feedback" => [$mockItem],
    "news_phonenum" => [$mockItem],
    "news_namesur" => [$mockItem],
    "news_tabian" => [$mockItem],
    "news_homenum" => [$mockItem],
    "news_concept" => [$mockItem]
];

echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
?>