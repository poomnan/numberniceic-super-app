<?php

namespace App\Managers;

use PDO;

class AdminNewsController extends Manager
{
    // List all News
    public function index($request, $response)
    {
        // Prioritize fixed positions 1-5, then show others by latest
        $sql = "SELECT * FROM news 
                ORDER BY CASE 
                    WHEN fix BETWEEN 1 AND 5 THEN 0 
                    ELSE 1 
                END ASC, 
                CASE 
                    WHEN fix BETWEEN 1 AND 5 THEN fix 
                END ASC,
                newsid DESC";
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

        $title = isset($body['news_headline']) ? mb_substr($body['news_headline'], 0, 255, 'UTF-8') : '';
        $titleShort = isset($body['news_title_short']) ? mb_substr($body['news_title_short'], 0, 255, 'UTF-8') : '';
        $desc = isset($body['news_desc']) ? mb_substr($body['news_desc'], 0, 255, 'UTF-8') : '';
        $detail = $body['news_detail'] ?? '';
        $categoryName = isset($body['category_name']) ? mb_substr($body['category_name'], 0, 255, 'UTF-8') : 'ทั่วไป';
        $imageUrl = isset($body['news_pic_header']) ? mb_substr($body['news_pic_header'], 0, 255, 'UTF-8') : '';

        // Flags mapping
        $hashtag1 = isset($body['hashtag1']) ? $body['hashtag1'] : 0;
        $hashtag2 = isset($body['hashtag2']) ? $body['hashtag2'] : 0;
        $hashtag3 = isset($body['hashtag3']) ? $body['hashtag3'] : 0;
        $hashtag4 = isset($body['hashtag4']) ? $body['hashtag4'] : 0;
        $hashtag5 = isset($body['hashtag5']) ? $body['hashtag5'] : 0;
        $hashtag6 = isset($body['hashtag6']) ? $body['hashtag6'] : 0;
        $fix = isset($body['fix']) ? $body['fix'] : 0;

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

        $title = isset($body['news_headline']) ? mb_substr($body['news_headline'], 0, 255, 'UTF-8') : '';
        $titleShort = isset($body['news_title_short']) ? mb_substr($body['news_title_short'], 0, 255, 'UTF-8') : '';
        $desc = isset($body['news_desc']) ? mb_substr($body['news_desc'], 0, 255, 'UTF-8') : '';
        $detail = $body['news_detail'] ?? '';
        $categoryName = isset($body['category_name']) ? mb_substr($body['category_name'], 0, 255, 'UTF-8') : 'ทั่วไป';
        $imageUrl = isset($body['news_pic_header']) ? mb_substr($body['news_pic_header'], 0, 255, 'UTF-8') : '';

        // Flags mapping
        $hashtag1 = isset($body['hashtag1']) ? $body['hashtag1'] : 0;
        $hashtag2 = isset($body['hashtag2']) ? $body['hashtag2'] : 0;
        $hashtag3 = isset($body['hashtag3']) ? $body['hashtag3'] : 0;
        $hashtag4 = isset($body['hashtag4']) ? $body['hashtag4'] : 0;
        $hashtag5 = isset($body['hashtag5']) ? $body['hashtag5'] : 0;
        $hashtag6 = isset($body['hashtag6']) ? $body['hashtag6'] : 0;
        $fix = isset($body['fix']) ? $body['fix'] : 0;

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
        try {
            $this->db->prepare("DELETE FROM news WHERE newsid = ?")->execute([$id]);
        } catch (\Exception $e) {
        }
        return $response->withHeader('Location', '/web/admin/news')->withStatus(302);
    }
}
