<?php
namespace App\Managers;

class HomePageManager extends Manager
{

    public function index($req, $res)
    {
        // Fetch real articles for the landing page
        $pinnedArticles = [];
        try {
            $stmt = $this->db->query("SELECT * FROM articles WHERE is_published = 1 ORDER BY published_at DESC LIMIT 3");
            $pinnedArticles = $stmt->fetchAll(\PDO::FETCH_OBJ);
        } catch (\Exception $e) {
            // Fallback if table doesn't exist or other error
        }

        $vars = [
            'pinnedArticles' => $pinnedArticles,
            'appStats' => [
                'downloads' => '50K+',
                'rating' => '4.8'
            ]
        ];

        return $this->view->render($res, 'web_index.php', $vars);
    }





    public function home($req, $res)
    {

        return $this->view->render($res, 'changenum/home.phtml');
    }

    public function tabian($req, $res)
    {

        return $this->view->render($res, 'changenum/tabian.phtml');
    }

    public function phone($req, $res)
    {

        return $this->view->render($res, 'changenum/phone.phtml');
    }

    public function namenick($req, $res)
    {

        return $this->view->render($res, 'changenum/namenick.phtml');
    }

    public function namesur($req, $res)
    {

        return $this->view->render($res, 'changenum/namesur.phtml');
    }

    public function love($req, $res)
    {

        return $this->view->render($res, 'tambon/love.phtml');
    }

    public function chataa($req, $res)
    {

        return $this->view->render($res, 'tambon/chataa.phtml');
    }




    public function sunday($req, $res)
    {

        return $this->view->render($res, 'tambon/sunday.phtml');
    }

    public function monday($req, $res)
    {

        return $this->view->render($res, 'tambon/monday.phtml');
    }

    public function tuesday($req, $res)
    {

        return $this->view->render($res, 'tambon/tuesday.phtml');
    }

    public function wednesday($req, $res)
    {

        return $this->view->render($res, 'tambon/wednesday.phtml');
    }

    public function thursday($req, $res)
    {

        return $this->view->render($res, 'tambon/thursday.phtml');
    }

    public function friday($req, $res)
    {

        return $this->view->render($res, 'tambon/friday.phtml');
    }




    public function saturday($req, $res)
    {

        return $this->view->render($res, 'tambon/saturday.phtml');
    }

    public function rahuu($req, $res)
    {

        return $this->view->render($res, 'tambon/rahuu.phtml');
    }

    public function privacyPolicy($req, $res)
    {

        return $this->view->render($res, 'privacy-policy.phtml');
    }

    public function deleteAccount($req, $res)
    {

        return $this->view->render($res, 'delete-account.phtml');
    }

    public function miracle($req, $res)
    {
        $queryParams = $req->getQueryParams();
        $day = $queryParams['day'] ?? 'sunday';
        $sql = "SELECT m.*, md.mira_desc FROM miracledo m 
                LEFT JOIN miracledo_desc md ON m.mira_id = md.mira_id 
                WHERE m.dayx = :day 
                ORDER BY CASE 
                    WHEN m.activity = 'สระผม' THEN 1
                    WHEN m.activity = 'ตัดผม' THEN 2
                    WHEN m.activity = 'ตัดเล็บ' THEN 3
                    WHEN m.activity = 'ผ้าใหม่' THEN 4
                    ELSE 5 END,
                CASE 
                    WHEN m.dayy = 'sunday' THEN 1
                    WHEN m.dayy = 'monday' THEN 2
                    WHEN m.dayy = 'tuesday' THEN 3
                    WHEN m.dayy = 'wednesday' THEN 4
                    WHEN m.dayy = 'thursday' THEN 5
                    WHEN m.dayy = 'friday' THEN 6
                    WHEN m.dayy = 'saturday' THEN 7
                    ELSE 8 END";

        $stmt = $this->db->prepare($sql);
        $stmt->bindParam(':day', $day);
        $stmt->execute();
        $vars['miracles'] = $stmt->fetchAll();
        $vars['selected_day'] = $day;

        $vars['days'] = [
            'sunday' => 'วันอาทิตย์',
            'monday' => 'วันจันทร์',
            'tuesday' => 'วันอังคาร',
            'wednesday' => 'วันพุธ',
            'thursday' => 'วันพฤหัสบดี',
            'friday' => 'วันศุกร์',
            'saturday' => 'วันเสาร์'
        ];

        $vars['wan_pras'] = \App\Managers\ThaiCalendarHelper::getUpcomingWanPras(6);
        $vars['auspicious_days'] = \App\Managers\ThaiCalendarHelper::getUpcomingAuspiciousDays(6);

        return $this->view->render($res, 'miracle.phtml', $vars);
    }

}