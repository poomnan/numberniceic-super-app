<?php

namespace App\Managers;

use Psr\Container\ContainerInterface;

class NewsController extends Manager
{
    private $startTime;

    public function __construct(ContainerInterface $container)
    {
        parent::__construct($container);
        $this->startTime = microtime(true);
    }

    public function wanpraTomoro($request, $response)
    {
        $data = json_encode(array('current_time' => date("H:i")));
        $response->getBody()->write($data);
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function getLuckyNumber($request, $response)
    {
        // 1. Fetch the latest entry from luckynumber_v2 (Ordered by date)
        $sqlV2 = "SELECT * FROM luckynumber_v2 ORDER BY lucky_date DESC LIMIT 1";
        $stmtV2 = $this->db->prepare($sqlV2);
        $stmtV2->execute();
        $manual = $stmtV2->fetch(\PDO::FETCH_ASSOC);

        $lucky_date = '';
        $numbers_str = '';

        if ($manual) {
            $lucky_date = $manual['lucky_date'];
            $lucky_numbers = [];
            for ($i = 1; $i <= 6; $i++) {
                if (!empty($manual["num$i"])) {
                    $lucky_numbers[] = $manual["num$i"];
                }
            }
            $numbers_str = implode(' ', $lucky_numbers);
        } else {
            // No record found in V2 - Do NOT fallback to V1 or auto-generate
            // Return empty to indicate no lucky number set by admin
            $lucky_date = "";
            $numbers_str = "";
        }

        $vars = [
            'lucky_date' => $lucky_date,
            'numbers' => $numbers_str,
            'active' => '1'
        ];

        // Check if browser request
        $accept = $request->getHeaderLine('Accept');
        if (strpos($accept, 'text/html') !== false) {
            return $this->view->render($response, 'lucky_number.phtml', $vars);
        }

        $data = json_encode(array('lucky_date' => $lucky_date, 'numbers' => $numbers_str, 'active' => '1'));
        $response->getBody()->write($data);
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function postLuckyNumber($request, $response)
    {
        $body = $request->getParsedBody();
        $numbers = filter_var($body['numbers'], FILTER_SANITIZE_STRING);
        $active = filter_var($body['active'], FILTER_SANITIZE_STRING);

        $sql = "INSERT INTO luckynumber (numbers, active) VALUES ('{$numbers}','{$active}')";
        $result = $this->db->prepare($sql);
        if ($result->execute()) {
            $data = json_encode(array('activity' => 'insert_lucky', 'message' => 'success'));
        } else {
            $data = json_encode(array('activity' => 'insert_lucky', 'message' => 'fail'));
        }
        $response->getBody()->write($data);
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function postLuckyNumberV2($request, $response)
    {
        if (ob_get_level() > 0)
            ob_clean();
        try {
            $body = $request->getParsedBody();
            if (empty($body)) {
                $raw = file_get_contents('php://input');
                $body = json_decode($raw, true);
            }

            $date = $body['date'] ?? '';
            $num1 = $body['num1'] ?? '';
            $num2 = $body['num2'] ?? '';
            $num3 = $body['num3'] ?? '';
            $num4 = $body['num4'] ?? '';
            $num5 = $body['num5'] ?? '';
            $num6 = $body['num6'] ?? '';

            // UPSERT logic: UPDATE if exists, else INSERT
            $sql = "INSERT INTO luckynumber_v2 (lucky_date, num1, num2, num3, num4, num5, num6) 
                    VALUES (:date, :n1, :n2, :n3, :n4, :n5, :n6)
                    ON DUPLICATE KEY UPDATE num1=:n1, num2=:n2, num3=:n3, num4=:n4, num5=:n5, num6=:n6";

            $stmt = $this->db->prepare($sql);
            $success = $stmt->execute([
                ':date' => $date,
                ':n1' => $num1,
                ':n2' => $num2,
                ':n3' => $num3,
                ':n4' => $num4,
                ':n5' => $num5,
                ':n6' => $num6
            ]);

            if ($success) {
                $response->getBody()->write(json_encode(['activity' => 'insert_lucky_v2', 'message' => 'success']));
            } else {
                $response->getBody()->write(json_encode(['activity' => 'insert_lucky_v2', 'message' => 'fail']));
            }
            return $response->withHeader('Content-Type', 'application/json');
        } catch (\Exception $e) {
            $response->getBody()->write(json_encode(['activity' => 'insert_lucky_v2', 'message' => 'fail', 'error' => $e->getMessage()]));
            return $response->withHeader('Content-Type', 'application/json');
        }
    }

    public function getLuckyNumberV2($request, $response)
    {
        $params = $request->getQueryParams();
        $date = $params['date'] ?? date('Y-m-d');

        $sql = "SELECT * FROM luckynumber_v2 WHERE lucky_date = :date";
        $stmt = $this->db->prepare($sql);
        $stmt->execute([':date' => $date]);
        $data = $stmt->fetch(\PDO::FETCH_ASSOC);

        if ($data) {
            $response->getBody()->write(json_encode(['status' => 'success', 'data' => $data]));
        } else {
            // Fallback to V1 logic or return empty
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'No numbers for this date']));
        }
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function newsTypeAll($req, $res)
    {
        $newsIdType = $req->getAttribute('newsidtype');

        // Hardcoded host to ensure correct image URLs behind proxies/IP access
        $host = "https://numberniceic.online";

        // Force UTF-8 for Thai characters
        $this->db->exec("SET NAMES utf8");

        $mapArticle = $this->_getMapArticleCallback($host);

        if ($newsIdType != null && $newsIdType != '') {
            // Fetch dynamic category name
            $catId = 0;
            $defaultName = "ทั่วไป";

            switch ($newsIdType) {
                case "0":
                    $catId = 7;
                    $defaultName = 'ข่าวและบทความที่น่าสนใจ';
                    break;
                case "1":
                    $catId = 1;
                    $defaultName = 'Review จากลูกค้า';
                    break;
                case "2":
                    $catId = 2;
                    $defaultName = 'วิธีเลือกซื้อเบอร์โทรศัพท์มงคล';
                    break;
                case "3":
                    $catId = 3;
                    $defaultName = 'ทำนายดวงชะตาจากชื่อ-สกุล';
                    break;
                case "4":
                    $catId = 4;
                    $defaultName = 'ทำนายดวงชะตาจากทะเบียนรถ';
                    break;
                case "5":
                    $catId = 5;
                    $defaultName = 'ทำนายดวงชะตาจากบ้านเลขที่';
                    break;
                case "6":
                    $catId = 6;
                    $defaultName = 'หลักการใช้เลขมงคล';
                    break;
            }

            $catStmt = $this->db->prepare("SELECT category_name FROM news_categories WHERE category_id = :id");
            $catStmt->execute([':id' => $catId]);
            $fetchedName = $catStmt->fetchColumn();
            $categoryName = $fetchedName ? $fetchedName : $defaultName;

            // Reverting to SELECT * as requested by user
            // $cols = "newsid, news_headline, news_pic_header, news_title_short, news_desc, news_date, category, fix, hashtag1, hashtag2, hashtag3, hashtag4, hashtag5, hashtag6, LEFT(news_detail, 300) as news_detail";

            // Corrected Config - Only existing tables
            // news_title_short does NOT exist -> use news_headline AS dummy or handle in map
            // category does NOT exist -> handled by PHP logic
            $cols = "newsid, news_headline, news_pic_header, news_headline as news_title_short, news_desc, news_date, fix, hashtag1, hashtag2, hashtag3, hashtag4, hashtag5, hashtag6, news_detail";

            $sql = "";
            switch ($newsIdType) {
                case "0":
                    $sql = "SELECT $cols FROM news WHERE fix IN ('1', '2', '3', '4', '5') ORDER BY fix ASC LIMIT 100";
                    break;
                case "1":
                    $sql = "SELECT $cols FROM news WHERE hashtag1 = 1 ORDER BY newsid DESC LIMIT 100";
                    break;
                case "2":
                    $sql = "SELECT $cols FROM news WHERE hashtag2 = 1 ORDER BY newsid DESC LIMIT 100";
                    break;
                case "3":
                    $sql = "SELECT $cols FROM news WHERE hashtag3 = 1 ORDER BY newsid DESC LIMIT 100";
                    break;
                case "4":
                    $sql = "SELECT $cols FROM news WHERE hashtag4 = 1 ORDER BY newsid DESC LIMIT 100";
                    break;
                case "5":
                    $sql = "SELECT $cols FROM news WHERE hashtag5 = 1 ORDER BY newsid DESC LIMIT 100";
                    break;
                case "6":
                    $sql = "SELECT $cols FROM news WHERE hashtag6 = 1 ORDER BY newsid DESC LIMIT 100";
                    break;
                default:
                    $sql = "SELECT $cols FROM news WHERE 1=0";
                    break;
            }

            $result = $this->db->prepare($sql);
            $result->execute();
            $dataRaw = $result->fetchAll(\PDO::FETCH_ASSOC);

            // Deduplicate in PHP
            $dataRawUnique = [];
            $seenHeadlines = [];
            foreach ($dataRaw as $r) {
                $h = $r['news_headline'];
                if (!in_array($h, $seenHeadlines)) {
                    $seenHeadlines[] = $h;
                    $dataRawUnique[] = $r;
                }
                if (count($dataRawUnique) >= 50)
                    break; // Limit to 50 unique items
            }

            // Inject category name explicitly
            $dataWithCat = [];
            foreach ($dataRawUnique as $r) {
                $r['category_name'] = $categoryName;
                $dataWithCat[] = $mapArticle($r);
            }

            $json = json_encode(
                array("type_id" => "$newsIdType", "news_all_type" => $dataWithCat),
                JSON_UNESCAPED_UNICODE | JSON_INVALID_UTF8_IGNORE
            );

            if ($json === false) {
                $errorMsg = json_last_error_msg();
                $res->getBody()->write(json_encode(['error' => "JSON Encoding Error: $errorMsg"]));
                return $res->withHeader('Content-Type', 'application/json')->withStatus(500);
            }

            $res->getBody()->write($json);

            // Add Debug Header
            $duration = microtime(true) - $this->startTime;
            return $res->withHeader('Content-Type', 'application/json')
                ->withHeader('X-Execution-Time', number_format($duration, 4));
        }
        return $res;
    }

    public function newsTop24($request, $response)
    {
        // Hardcoded host to ensure correct image URLs behind proxies/IP access
        $host = "https://ananya.in.th";

        // Force UTF-8 for Thai characters
        $this->db->exec("SET NAMES utf8");

        $mapArticle = $this->_getMapArticleCallback($host);

        // Fetch All Category Names
        $catStmt = $this->db->query("SELECT category_id, category_name FROM news_categories");
        $categories = $catStmt->fetchAll(\PDO::FETCH_KEY_PAIR); // [1 => 'Name', ...]

        // Helper for deduplication
        $processCategory = function ($sql, $limit, $catName) use ($mapArticle) {
            $stmt = $this->db->query($sql);
            $rows = $stmt->fetchAll(\PDO::FETCH_ASSOC);
            $uniqueData = [];
            $seen = [];
            foreach ($rows as $r) {
                if (!in_array($r['news_headline'], $seen)) {
                    $seen[] = $r['news_headline'];
                    $r['category_name'] = $catName;
                    $uniqueData[] = $mapArticle($r);
                }
                if (count($uniqueData) >= $limit)
                    break;
            }
            return $uniqueData;
        };

        // Corrected Config - Only existing tables
        $cols = "newsid, news_headline, news_pic_header, news_headline as news_title_short, news_desc, news_date, fix, hashtag1, hashtag2, hashtag3, hashtag4, hashtag5, hashtag6, news_detail";

        // 1. Hot News (Latest 7) - Check 'fix' flag
        $dataHot = $processCategory(
            "SELECT $cols FROM news WHERE fix IN ('1', '2', '3', '4', '5') ORDER BY fix ASC LIMIT 20",
            7,
            $categories[7] ?? 'ข่าวและบทความที่น่าสนใจ'
        );

        // 2. Feedback - Check hashtag1 (Category 1)
        $dataFeedback = $processCategory(
            "SELECT $cols FROM news WHERE hashtag1 = 1 ORDER BY newsid DESC LIMIT 20",
            4,
            $categories[1] ?? 'Review จากลูกค้า'
        );

        // 3. Phone - hashtag2 (Category 2)
        $dataPhoneNum = $processCategory(
            "SELECT $cols FROM news WHERE hashtag2 = 1 ORDER BY newsid DESC LIMIT 20",
            4,
            $categories[2] ?? 'วิธีเลือกซื้อเบอร์โทรศัพท์มงคล'
        );

        // 4. NameSur - hashtag3 (Category 3)
        $dataNameSur = $processCategory(
            "SELECT $cols FROM news WHERE hashtag3 = 1 ORDER BY newsid DESC LIMIT 20",
            4,
            $categories[3] ?? 'ทำนายดวงชะตาจากชื่อ-สกุล'
        );

        // 5. Tabian - hashtag4 (Category 4)
        $dataTabian = $processCategory(
            "SELECT $cols FROM news WHERE hashtag4 = 1 ORDER BY newsid DESC LIMIT 20",
            4,
            $categories[4] ?? 'ทำนายดวงชะตาจากทะเบียนรถ'
        );

        // 6. Home - hashtag5 (Category 5)
        $dataHomeNum = $processCategory(
            "SELECT $cols FROM news WHERE hashtag5 = 1 ORDER BY newsid DESC LIMIT 20",
            4,
            $categories[5] ?? 'ทำนายดวงชะตาจากบ้านเลขที่'
        );

        // 7. Concept - hashtag6 (Category 6)
        $dataConcept = $processCategory(
            "SELECT $cols FROM news WHERE hashtag6 = 1 ORDER BY newsid DESC LIMIT 20",
            4,
            $categories[6] ?? 'หลักการเลือกทะเบียนรถ'
        );

        $data = json_encode(
            array(
                "news_hot" => $dataHot,
                "news_feedback" => $dataFeedback,
                "news_phonenum" => $dataPhoneNum,
                "news_namesur" => $dataNameSur,
                "news_tabian" => $dataTabian,
                "news_homenum" => $dataHomeNum,
                "news_concept" => $dataConcept
            ),
            JSON_UNESCAPED_UNICODE | JSON_INVALID_UTF8_IGNORE
        );

        $response->getBody()->write($data);
        return $response->withHeader('Content-Type', 'application/json');
    }

    // Helper function for mapping
    private function _getMapArticleCallback($host)
    {
        return function ($article) use ($host) {
            // Robust check for keys
            $id = $article['newsid'] ?? $article['news_id'] ?? $article['id'] ?? $article['ID'] ?? '';
            $headline = $article['news_headline'] ?? $article['news_topic'] ?? $article['topic'] ?? $article['news_header'] ?? $article['head_text'] ?? $article['title'] ?? $article['name'] ?? $article['subject'] ?? '';
            $short = $article['news_title_short'] ?? $article['news_short'] ?? $headline;
            $desc = $article['news_desc'] ?? $article['intro'] ?? $article['description'] ?? $article['excerpt'] ?? mb_substr(strip_tags($article['news_detail'] ?? $article['detail'] ?? $article['content'] ?? $article['body'] ?? ''), 0, 100);
            $img = $article['news_pic_header'] ?? $article['news_picture'] ?? $article['photo'] ?? $article['photo1'] ?? $article['cover'] ?? $article['image'] ?? $article['img'] ?? $article['file_name'] ?? $article['url'] ?? '';
            $date = $article['news_date'] ?? $article['created_at'] ?? $article['date'] ?? $article['published_at'] ?? '';
            $cat = $article['category_name'] ?? $article['category'] ?? 'ทั่วไป';

            $fix = $article['fix'] ?? '0';
            $detail = $article['news_detail'] ?? $article['detail'] ?? $article['content'] ?? '';

            if (!empty($img) && strpos($img, 'http') !== 0) {
                $img = $host . '/' . ltrim($img, '/');
            }
            return [
                'newsid' => $id,
                'news_headline' => $headline,
                'news_title_short' => $short,
                'news_desc' => $desc,
                'news_detail' => $detail,
                'news_pic_header' => $img,
                'news_date' => $date,
                'category' => $cat,
                'fix' => $fix
            ];
        };
    }

    public function newsNumberViewDetail($request, $response)
    {
        $newsid = $request->getAttribute('number');
        $sql = "SELECT * FROM news WHERE newsid = :id";

        $this->db->exec("SET NAMES utf8"); // Ensure Vew Detail is also UTF8

        $result = $this->db->prepare($sql);
        $result->execute([':id' => $newsid]);
        $data = $result->fetch(\PDO::FETCH_ASSOC);

        if (!$data) {
            $response->getBody()->write(json_encode(['error' => 'News not found']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(404);
        }

        // Fix Image Path for View
        // Fix Image Path for View - Hardcoded Host
        $host = "https://ananya.in.th";
        if (!empty($data['news_pic_header']) && strpos($data['news_pic_header'], 'http') !== 0) {
            $path = ltrim($data['news_pic_header'], '/');
            $data['news_pic_header'] = $host . '/' . $path;
        }

        // Map fields if necessary, or return data directly if columns match serialization
        // Android expects: news_detail, news_header, news_pic_header, etc.
        // Assuming table columns match these names (as implied by _getMapArticleCallback)

        $response->getBody()->write(json_encode($data));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function getArticleJson($request, $response)
    {
        $art_id = $request->getAttribute('id');
        $sql = "SELECT art_id as newsid, 
                       image_url as news_pic_header, 
                       title as news_headline, 
                       title_short as news_title_short, 
                       excerpt as news_desc, 
                       content as news_detail,
                       category,
                       published_at as news_date
                FROM articles 
                WHERE art_id = :id AND is_published = 1";

        $result = $this->db->prepare($sql);
        $result->execute([':id' => $art_id]);
        $article = $result->fetch(\PDO::FETCH_ASSOC);

        if (!$article) {
            $response->getBody()->write(json_encode(['error' => 'Article not found']));
            return $response->withHeader('Content-Type', 'application/json')->withStatus(404);
        }

        // Convert relative image URL to absolute
        // Convert relative image URL to absolute - Hardcoded Host
        $host = "https://ananya.in.th";
        if (!empty($article['news_pic_header']) && strpos($article['news_pic_header'], 'http') !== 0) {
            $path = ltrim($article['news_pic_header'], '/');
            $article['news_pic_header'] = $host . '/' . $path;
        }

        $response->getBody()->write(json_encode($article));
        return $response->withHeader('Content-Type', 'application/json');
    }
}
