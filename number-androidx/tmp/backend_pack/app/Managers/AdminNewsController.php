<?php

namespace App\Managers;

use PDO;

class AdminNewsController extends Manager
{
    // List all News
    public function index($request, $response)
    {
        // Fetch all news ordered by latest
        $sql = "SELECT * FROM news ORDER BY newsid DESC";
        $stmt = $this->db->query($sql);
        $newsList = $stmt->fetchAll(PDO::FETCH_OBJ);

        return $this->view->render($response, 'web_admin_news_list.php', [
            'newsList' => $newsList,
            'user' => $_SESSION['user'] ?? null
        ]);
    }

    // Show Create Form
    public function create($request, $response)
    {
        // Fetch Categories
        $catStmt = $this->db->query("SELECT * FROM news_categories ORDER BY sort_order ASC");
        $categories = $catStmt->fetchAll(PDO::FETCH_OBJ);

        return $this->view->render($response, 'web_admin_news_form.php', [
            'categories' => $categories,
            'user' => $_SESSION['user'] ?? null
        ]);
    }

    // Save New News
    public function store($request, $response)
    {
        $body = $request->getParsedBody();

        $title = $body['news_headline'] ?? '';
        $titleShort = $body['news_title_short'] ?? '';
        $desc = $body['news_desc'] ?? '';
        $detail = $body['news_detail'] ?? '';
        $categoryName = $body['category_name'] ?? 'ทั่วไป';
        $imageUrl = $body['news_pic_header'] ?? '';

        // Legacy Flags (Optional: Map category to legacy flags if needed, but we focus on category_name now)
        // Set legacy flags based on category name just in case
        $hashtag1 = (strpos($categoryName, 'Feedback') !== false) ? 1 : 0;
        $hashtag2 = (strpos($categoryName, 'เบอร์โทร') !== false) ? 1 : 0;
        $hashtag3 = (strpos($categoryName, 'ชื่อ') !== false) ? 1 : 0;
        $hashtag4 = (strpos($categoryName, 'ทะเบียน') !== false) ? 1 : 0;
        $hashtag5 = (strpos($categoryName, 'บ้าน') !== false) ? 1 : 0;
        $hashtag6 = (strpos($categoryName, 'หลักการ') !== false) ? 1 : 0;
        $fix = ($categoryName == 'ข่าวและบทความที่น่าสนใจ') ? '1' : '0';

        $sql = "INSERT INTO news (news_headline, news_title_short, news_desc, news_detail, category_name, news_pic_header, news_date, fix, hashtag1, hashtag2, hashtag3, hashtag4, hashtag5, hashtag6) 
                VALUES (:title, :titleShort, :desc, :detail, :catName, :img, NOW(), :fix, :h1, :h2, :h3, :h4, :h5, :h6)";

        $stmt = $this->db->prepare($sql);
        $stmt->execute([
            ':title' => $title,
            ':titleShort' => $titleShort,
            ':desc' => $desc,
            ':detail' => $detail,
            ':catName' => $categoryName,
            ':img' => $imageUrl,
            ':fix' => $fix,
            ':h1' => $hashtag1,
            ':h2' => $hashtag2,
            ':h3' => $hashtag3,
            ':h4' => $hashtag4,
            ':h5' => $hashtag5,
            ':h6' => $hashtag6
        ]);

        return $response->withHeader('Location', '/web/admin/news')->withStatus(302);
    }

    // Show Edit Form
    public function edit($request, $response, $args)
    {
        $id = $args['id'];

        // Fetch News Item
        $stmt = $this->db->prepare("SELECT * FROM news WHERE newsid = :id");
        $stmt->execute([':id' => $id]);
        $newsItem = $stmt->fetch(PDO::FETCH_OBJ);

        if (!$newsItem) {
            return $response->withHeader('Location', '/web/admin/news')->withStatus(302);
        }

        // Fetch Categories
        $catStmt = $this->db->query("SELECT * FROM news_categories ORDER BY sort_order ASC");
        $categories = $catStmt->fetchAll(PDO::FETCH_OBJ);

        return $this->view->render($response, 'web_admin_news_form.php', [
            'newsItem' => $newsItem,
            'categories' => $categories,
            'user' => $_SESSION['user'] ?? null
        ]);
    }

    // Update News
    public function update($request, $response, $args)
    {
        $id = $args['id'];
        $body = $request->getParsedBody();

        $title = $body['news_headline'] ?? '';
        $titleShort = $body['news_title_short'] ?? '';
        $desc = $body['news_desc'] ?? '';
        $detail = $body['news_detail'] ?? '';
        $categoryName = $body['category_name'] ?? 'ทั่วไป';
        $imageUrl = $body['news_pic_header'] ?? '';

        // Legacy Flags mapping
        $hashtag1 = (strpos($categoryName, 'Feedback') !== false) ? 1 : 0;
        $hashtag2 = (strpos($categoryName, 'เบอร์โทร') !== false) ? 1 : 0;
        $hashtag3 = (strpos($categoryName, 'ชื่อ') !== false) ? 1 : 0;
        $hashtag4 = (strpos($categoryName, 'ทะเบียน') !== false) ? 1 : 0;
        $hashtag5 = (strpos($categoryName, 'บ้าน') !== false) ? 1 : 0;
        $hashtag6 = (strpos($categoryName, 'หลักการ') !== false) ? 1 : 0;
        $fix = ($categoryName == 'ข่าวและบทความที่น่าสนใจ') ? '1' : '0';

        $sql = "UPDATE news SET 
                news_headline=:title, 
                news_title_short=:titleShort, 
                news_desc=:desc, 
                news_detail=:detail, 
                category_name=:catName, 
                news_pic_header=:img,
                fix=:fix, 
                hashtag1=:h1, hashtag2=:h2, hashtag3=:h3, hashtag4=:h4, hashtag5=:h5, hashtag6=:h6 
                WHERE newsid=:id";

        $stmt = $this->db->prepare($sql);
        $stmt->execute([
            ':title' => $title,
            ':titleShort' => $titleShort,
            ':desc' => $desc,
            ':detail' => $detail,
            ':catName' => $categoryName,
            ':img' => $imageUrl,
            ':fix' => $fix,
            ':h1' => $hashtag1,
            ':h2' => $hashtag2,
            ':h3' => $hashtag3,
            ':h4' => $hashtag4,
            ':h5' => $hashtag5,
            ':h6' => $hashtag6,
            ':id' => $id
        ]);

        return $response->withHeader('Location', '/web/admin/news')->withStatus(302);
    }

    // Delete News
    public function delete($request, $response, $args)
    {
        $id = $args['id'];

        // Check admin (Middleware should handle this, but double check)
        if (!isset($_SESSION['user']) || (strtolower($_SESSION['user']->vipcode) !== 'admin' && strtolower($_SESSION['user']->vipcode) !== 'administrator')) {
            return $response->withHeader('Location', '/web/login')->withStatus(302);
        }

        $stmt = $this->db->prepare("DELETE FROM news WHERE newsid = :id");
        $stmt->execute([':id' => $id]);

        return $response->withHeader('Location', '/web/admin/news')->withStatus(302);
    }
}
