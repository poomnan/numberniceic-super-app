<?php

namespace App\Managers;

/**
 * Class PlayStoreScraper
 * 
 * ดึงข้อมูลยอดดาวน์โหลดและคะแนนจาก Google Play Store อัตโนมัติ
 * พร้อมระบบ Cache 24 ชั่วโมงเพื่อไม่ให้โหลดหน้าเว็บ Google บ่อยเกินไป
 */
class PlayStoreScraper
{
    private static $cacheFile = __DIR__ . '/../../cache/playstore_stats.json';
    private static $cacheDuration = 86400; // 24 ชั่วโมง

    /**
     * ดึงข้อมูลสถิติแอป
     * 
     * @param string $appId เช่น 'com.numberniceic'
     * @return array [downloads, rating, ratingCount]
     */
    public static function getStats($appId = 'com.numberniceic')
    {
        // ตรวจสอบ Cache
        if (file_exists(self::$cacheFile)) {
            $cache = json_decode(file_get_contents(self::$cacheFile), true);
            if ($cache && (time() - $cache['timestamp'] < self::$cacheDuration)) {
                return $cache['data'];
            }
        }

        // ถ้าไม่มี Cache หรือ Cache หมดอายุ ให้ดึงใหม่
        $stats = self::fetchFromPlayStore($appId);

        if ($stats) {
            // สร้างโฟลเดอร์ Cache ถ้ายังไม่มี
            if (!is_dir(dirname(self::$cacheFile))) {
                @mkdir(dirname(self::$cacheFile), 0777, true);
            }
            // เขียนไฟล์ Cache
            @file_put_contents(self::$cacheFile, json_encode([
                'timestamp' => time(),
                'data' => $stats
            ]));
        }

        // กลับค่าข้อมูล (ถ้าดึงไม่ได้ให้ใช้ค่า Default)
        return $stats ?: [
            'downloads' => '1+',
            'rating' => '0',
            'ratingCount' => '0'
        ];
    }

    /**
     * ดึงข้อมูลโดยตรงจากหน้าเว็บ Google Play
     */
    private static function fetchFromPlayStore($appId)
    {
        $url = "https://play.google.com/store/apps/details?id=" . $appId . "&hl=en";

        $opts = [
            'http' => [
                'method' => "GET",
                'header' => "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36\r\n"
            ]
        ];

        $context = stream_context_create($opts);
        $html = @file_get_contents($url, false, $context);

        if (!$html)
            return null;

        $stats = [
            'downloads' => '1+',
            'rating' => '0',
            'ratingCount' => '0'
        ];

        // ใช้ DOMDocument ในการแกะข้อมูล
        $dom = new \DOMDocument();
        // ปิด Error รายงาน HTML ที่ไม่สมบูรณ์
        @$dom->loadHTML('<?xml encoding="UTF-8">' . $html);
        $xpath = new \DOMXPath($dom);

        // 1. ดึงข้อมูล Downloads
        // ค้นหา div ที่มีคำว่า "Downloads" แล้วดึงตัวเลขจาก sibling หรือ parent
        $query = "//*[contains(text(), 'Downloads')]";
        $nodes = $xpath->query($query);

        foreach ($nodes as $node) {
            $parent = $node->parentNode;
            if ($parent) {
                // ปกติ Google Play จะวางตัวเลขไว้ใน div แรกของ container เดียวกัน
                $valNodes = $xpath->query(".//div", $parent);
                if ($valNodes->length > 0) {
                    $val = trim($valNodes->item(0)->nodeValue);
                    // ตรวจสอบว่าเป็นรูปแบบตัวเลข (เช่น 1+, 100+, 1K+)
                    if (preg_match('/^[\d,.]+[\w+]*$/', $val)) {
                        $stats['downloads'] = $val;
                        break;
                    }
                }
            }
        }

        // 2. ดึงข้อมูล Rating
        // Google Play มักใส่ rating ไว้ใน aria-label ของ div บางตัว
        $ratingQuery = "//div[@aria-label[contains(., 'Rated')]]";
        $ratingNodes = $xpath->query($ratingQuery);
        if ($ratingNodes->length > 0) {
            $label = $ratingNodes->item(0)->getAttribute('aria-label');
            if (preg_match('/Rated ([\d.]+) stars/', $label, $matches)) {
                $stats['rating'] = $matches[1];
            }
        }

        return $stats;
    }
}
