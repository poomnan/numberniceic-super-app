<?php

namespace App\Managers;


class AdminController extends Manager
{

    public function topicList($request, $response)
    {
        $sql = "SELECT * FROM topictb ORDER BY topic_id DESC";
        $result = $this->db->prepare($sql);
        if ($result->execute()) {
            $data = $result->fetchAll(\PDO::FETCH_OBJ);

            $realArray = array();

            foreach ($data as $row) {

                if ($row->photo1 != null || !empty($row->photo1)) {
                    $photoReal1 = 'https://www.ananya.in.th/public/photo/' . $row->photo1 . '.png';
                } else {
                    $photoReal1 = null;
                }
                ;

                if ($row->photo2 != null || !empty($row->photo1)) {
                    $photoReal2 = 'https://www.ananya.in.th/public/photo/' . $row->photo2 . '.png';
                } else {
                    $photoReal2 = null;
                }
                ;

                if ($row->photo3 != null || !empty($row->photo1)) {
                    $photoReal3 = 'https://www.ananya.in.th/public/photo/' . $row->photo3 . '.png';
                } else {
                    $photoReal3 = null;
                }
                ;


                array_push($realArray, array('topic_id' => $row->topic_id, 'head_text' => $row->head_text, 'tag_phone' => $row->tag_phone, 'tag_tabian' => $row->tag_tabian, 'tag_home' => $row->tag_home, 'tag_namesur' => $row->tag_namesur, 'paragraph1' => $row->paragraph1, 'paragraph2' => $row->paragraph2, 'paragraph3' => $row->paragraph3, 'photo1' => $photoReal1, 'photo2' => $photoReal2, 'photo3' => $photoReal3, 'topic_date' => $row->topic_date, 'topic_date_update' => $row->topic_date_update, 'public_status' => $row->public_status, 'auth_name' => $row->topic_auth));
            }

            $data_array = array("topic_list" => $realArray);

            echo json_encode($data_array);
        }
    }

    public function topicUpdate($request, $response)
    {

        $body = $request->getParsedBody();

        $topic_id = filter_var($body['topic_id'], FILTER_SANITIZE_STRING);
        $header_text = filter_var($body['header_text'], FILTER_SANITIZE_STRING);
        $desc_text = filter_var($body['desc_text'], FILTER_SANITIZE_STRING);

        $tag_phone = filter_var($body['hag_phone'], FILTER_SANITIZE_STRING);
        $tag_tabian = filter_var($body['hag_tabian'], FILTER_SANITIZE_STRING);
        $tag_home = filter_var($body['hag_home'], FILTER_SANITIZE_STRING);
        $tag_namesur = filter_var($body['hag_namesur'], FILTER_SANITIZE_STRING);

        $photo1 = filter_var($body['photo1'], FILTER_SANITIZE_STRING);
        $photo2 = filter_var($body['photo2'], FILTER_SANITIZE_STRING);
        $photo3 = filter_var($body['photo3'], FILTER_SANITIZE_STRING);

        $paragraph1 = filter_var($body['paragraph1_text'], FILTER_SANITIZE_STRING);
        $paragraph2 = filter_var($body['paragraph2_text'], FILTER_SANITIZE_STRING);
        $paragraph3 = filter_var($body['paragraph3_text'], FILTER_SANITIZE_STRING);

        $auth_name = filter_var($body['auth_name'], FILTER_SANITIZE_STRING);
        $state_topic = filter_var($body['state_topic'], FILTER_SANITIZE_STRING);

        $topicDate = date("Y-m-d h:i:s");

        $photoName1 = '';
        $photoName2 = '';
        $photoName3 = '';

        if ($photo1 != "") {
            $imageName = time();//or Anything You Need
            $path = PHOTO_DIR . '/' . $imageName . ".png";
            if (!is_dir(PHOTO_DIR))
                mkdir(PHOTO_DIR, 0777, true);
            $status = file_put_contents($path, base64_decode($photo1));

            if ($status) {
                $photoName1 = $imageName;
            }
        }

        if ($photo2 != "") {
            $imageName = time() + 1;//or Anything You Need
            $path = PHOTO_DIR . '/' . $imageName . ".png";
            if (!is_dir(PHOTO_DIR))
                mkdir(PHOTO_DIR, 0777, true);
            $status = file_put_contents($path, base64_decode($photo2));

            if ($status) {
                $photoName2 = $imageName;
            }
        }

        if ($photo3 != "") {
            $imageName = time() + 2;//or Anything You Need
            $path = PHOTO_DIR . '/' . $imageName . ".png";
            if (!is_dir(PHOTO_DIR))
                mkdir(PHOTO_DIR, 0777, true);
            $status = file_put_contents($path, base64_decode($photo3));

            if ($status) {
                $photoName3 = $imageName;
            }
        }


        $sql = "UPDATE topictb SET head_text = '$header_text', desc_text = '$desc_text', tag_phone = '$tag_phone', tag_tabian = '$tag_tabian', tag_home = '$tag_home', tag_namesur = '$tag_namesur', paragraph1 = '$paragraph1', paragraph2 = '$paragraph2', paragraph3 = '$paragraph3', photo1 = '$photoName1', photo2 = '$photoName2', photo3 = '$photoName3', topic_auth = '$auth_name', topic_date = '$topicDate', public_status = '$state_topic' WHERE topic_id = '$topic_id'";
        $result = $this->db->prepare($sql);

        if ($result->execute()) {

            $sql = "SELECT * FROM topictb ORDER BY topic_id DESC LIMIT 0, 1";
            $result = $this->db->prepare($sql);

            if ($result->execute()) {

                $rowObj = $result->fetch(\PDO::FETCH_OBJ);
                if (is_object($rowObj)) {
                    if ($rowObj->photo1 != null)
                        $photo1Url = 'https://www.ananya.in.th/public/photo/' . $rowObj->photo1 . '.png';
                    else
                        $photo1Url = null;
                    if ($rowObj->photo2 != null)
                        $photo2Url = 'https://www.ananya.in.th/public/photo/' . $rowObj->photo2 . '.png';
                    else
                        $photo2Url = null;
                    if ($rowObj->photo3 != null)
                        $photo3Url = 'https://www.ananya.in.th/public/photo/' . $rowObj->photo3 . '.png';
                    else
                        $photo3Url = null;

                    $arrObj = array('topic_id' => $rowObj->topic_id, 'topic_date' => $rowObj->topic_date, 'topic_auth' => $rowObj->topic_auth, 'heade_text' => $rowObj->head_text, 'desc_text' => $rowObj->desc_text, 'tag_phone' => $rowObj->tag_phone, 'tag_tabian' => $rowObj->tag_tabian, 'tag_home' => $rowObj->tag_home, 'tag_namesur' => $rowObj->tag_namesur, 'paragraph1' => $rowObj->paragraph1, 'paragraph2' => $rowObj->paragraph2, 'paragraph3' => $rowObj->paragraph3, 'photo1' => $photo1Url, 'photo2' => $photo2Url, 'photo3' => $photo3Url, 'state_topic' => $rowObj->public_status);


                    $response->getBody()->write(json_encode($arrObj));
                    return $response->withHeader('Content-Type', 'application/json');
                }
            }
        }


    }

    public function topicUpload($request, $response)
    {

        $body = $request->getParsedBody();
        $header_text = filter_var($body['header_text'], FILTER_SANITIZE_STRING);
        $desc_text = filter_var($body['desc_text'], FILTER_SANITIZE_STRING);

        $tag_phone = filter_var($body['hag_phone'], FILTER_SANITIZE_STRING);
        $tag_tabian = filter_var($body['hag_tabian'], FILTER_SANITIZE_STRING);
        $tag_home = filter_var($body['hag_home'], FILTER_SANITIZE_STRING);
        $tag_namesur = filter_var($body['hag_namesur'], FILTER_SANITIZE_STRING);

        $photo1 = filter_var($body['photo1'], FILTER_SANITIZE_STRING);
        $photo2 = filter_var($body['photo2'], FILTER_SANITIZE_STRING);
        $photo3 = filter_var($body['photo3'], FILTER_SANITIZE_STRING);


        $auth_name = filter_var($body['auth_name'], FILTER_SANITIZE_STRING);

        $state_topic = filter_var($body['state_topic'], FILTER_SANITIZE_STRING);


        $topicDate = date("Y-m-d h:i:s");


        $photoName1 = '';
        $photoName2 = '';
        $photoName3 = '';

        if ($photo1 != "") {
            $imageName = time();//or Anything You Need
            $path = PHOTO_DIR . '/' . $imageName . ".png";
            if (!is_dir(PHOTO_DIR))
                mkdir(PHOTO_DIR, 0777, true);
            $status = file_put_contents($path, base64_decode($photo1));

            if ($status) {
                $photoName1 = $imageName;
            }
        }

        if ($photo2 != "") {
            $imageName = time() + 1;//or Anything You Need
            $path = PHOTO_DIR . '/' . $imageName . ".png";
            if (!is_dir(PHOTO_DIR))
                mkdir(PHOTO_DIR, 0777, true);
            $status = file_put_contents($path, base64_decode($photo2));

            if ($status) {
                $photoName2 = $imageName;
            }
        }

        if ($photo3 != "") {
            $imageName = time() + 2;//or Anything You Need
            $path = PHOTO_DIR . '/' . $imageName . ".png";
            if (!is_dir(PHOTO_DIR))
                mkdir(PHOTO_DIR, 0777, true);
            $status = file_put_contents($path, base64_decode($photo3));

            if ($status) {
                $photoName3 = $imageName;
            }
        }

        $paragraph1 = filter_var($body['paragraph1_text'], FILTER_SANITIZE_STRING);
        $paragraph2 = filter_var($body['paragraph2_text'], FILTER_SANITIZE_STRING);
        $paragraph3 = filter_var($body['paragraph3_text'], FILTER_SANITIZE_STRING);

        $sql = "INSERT INTO topictb (head_text, desc_text, tag_phone, tag_tabian, tag_home, tag_namesur, paragraph1, paragraph2, paragraph3, photo1, photo2, photo3, topic_date, topic_auth, public_status) VALUES ('$header_text', '$desc_text', '$tag_phone', '$tag_tabian','$tag_home', '$tag_namesur','$paragraph1','$paragraph2','$paragraph3', '$photoName1', '$photoName2', '$photoName3', '$topicDate', '$auth_name', '$state_topic')";

        $result = $this->db->prepare($sql);
        if ($result->execute()) {

            $sql = "SELECT * FROM topictb ORDER BY topic_id DESC LIMIT 0, 1";
            $result = $this->db->prepare($sql);

            if ($result->execute()) {

                $rowObj = $result->fetch(\PDO::FETCH_OBJ);
                if (is_object($rowObj)) {

                    if ($rowObj->photo1 != null)
                        $photo1Url = 'https://www.ananya.in.th/public/photo/' . $rowObj->photo1 . '.png';
                    else
                        $photo1Url = null;
                    if ($rowObj->photo2 != null)
                        $photo2Url = 'https://www.ananya.in.th/public/photo/' . $rowObj->photo2 . '.png';
                    else
                        $photo2Url = null;
                    if ($rowObj->photo3 != null)
                        $photo3Url = 'https://www.ananya.in.th/public/photo/' . $rowObj->photo3 . '.png';
                    else
                        $photo3Url = null;

                    $arrObj = array('topic_id' => $rowObj->topic_id, 'topic_date' => $rowObj->topic_date, 'topic_auth' => $rowObj->topic_auth, 'heade_text' => $rowObj->head_text, 'desc_text' => $rowObj->desc_text, 'tag_phone' => $rowObj->tag_phone, 'tag_tabian' => $rowObj->tag_tabian, 'tag_home' => $rowObj->tag_home, 'tag_namesur' => $rowObj->tag_namesur, 'paragraph1' => $rowObj->paragraph1, 'paragraph2' => $rowObj->paragraph2, 'paragraph3' => $rowObj->paragraph3, 'photo1' => $photo1Url, 'photo2' => $photo2Url, 'photo3' => $photo3Url, 'state_topic' => $rowObj->public_status);


                    $response->getBody()->write(json_encode($arrObj));
                    return $response->withHeader('Content-Type', 'application/json');
                }

            }

        } else {
            return null;
        }

        return null;

    }

    public function photoUpload($request, $response)
    {

        $body = $request->getParsedBody();
        $base64Img = filter_var($body['base64Img'], FILTER_SANITIZE_STRING);
        $image_no = time();//or Anything You Need
        $path = PHOTO_DIR . '/' . $image_no . ".png";
        if (!is_dir(PHOTO_DIR))
            mkdir(PHOTO_DIR, 0777, true);

        $status = file_put_contents($path, base64_decode($base64Img));

        if ($status) {
            $msg = "Successfully Uploaded";
        } else {
            $msg = "Upload failed";
        }

        $response->getBody()->write(json_encode(array("photo_message" => $msg)));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function updateBagColorById($request, $response)
    {
        $logPath = dirname(dirname(__DIR__)) . '/cache/requests.log';
        file_put_contents($logPath, date('[Y-m-d H:i:s] ') . "ADMIN_CONTROLLER: START updateBagColorById\n", FILE_APPEND);

        // Clear any previous output (warnings/notices) to ensure valid JSON
        if (ob_get_level() > 0)
            ob_clean();

        try {
            $body = $request->getParsedBody();
            if (empty($body)) {
                $raw = file_get_contents('php://input');
                $body = json_decode($raw, true);
            }

            $memberid = (string) ($body['memberid'] ?? '');
            $age = (string) ($body['age'] ?? '');

            // Colors A & B
            $ca = [$body['color0'] ?? '', $body['color1'] ?? '', $body['color2'] ?? '', $body['color3'] ?? '', $body['color4'] ?? '', $body['color5'] ?? ''];
            $cb = [$body['colorb0'] ?? '', $body['colorb1'] ?? '', $body['colorb2'] ?? '', $body['colorb3'] ?? '', $body['colorb4'] ?? '', $body['colorb5'] ?? ''];

            $age1 = $age;
            $age2 = (string) ((int) $age + 1);
            $db = $this->db;

            // Helper for Upsert
            $upsert = function ($mid, $a, $c) use ($db) {
                $stmt = $db->prepare("SELECT bag_id FROM bagcolortb WHERE memberid = :mid AND age = :age LIMIT 1");
                $stmt->execute([':mid' => $mid, ':age' => $a]);
                $row = $stmt->fetch(\PDO::FETCH_OBJ);

                if ($row) {
                    $sql = "UPDATE bagcolortb SET bag_color1=:c1, bag_color2=:c2, bag_color3=:c3, bag_color4=:c4, bag_color5=:c5, bag_color6=:c6, date_color_updated=NOW() WHERE bag_id=:id";
                    return $db->prepare($sql)->execute([':c1' => $c[0], ':c2' => $c[1], ':c3' => $c[2], ':c4' => $c[3], ':c5' => $c[4], ':c6' => $c[5], ':id' => $row->bag_id]);
                } else {
                    $sql = "INSERT INTO bagcolortb (memberid, age, bag_color1, bag_color2, bag_color3, bag_color4, bag_color5, bag_color6, date_color_updated) VALUES (:mid, :age, :c1, :c2, :c3, :c4, :c5, :c6, NOW())";
                    return $db->prepare($sql)->execute([':mid' => $mid, ':age' => $a, ':c1' => $c[0], ':c2' => $c[1], ':c3' => $c[2], ':c4' => $c[3], ':c5' => $c[4], ':c6' => $c[5]]);
                }
            };

            $successA = $upsert($memberid, $age1, $ca);
            $successB = $upsert($memberid, $age2, $cb);

            file_put_contents($logPath, "ADMIN_CONTROLLER: Success Status A=" . ($successA ? "Y" : "N") . " B=" . ($successB ? "Y" : "N") . "\n", FILE_APPEND);

            $response->getBody()->write(json_encode([
                'success_update_a' => $successA ? "true" : "false",
                'success_update_b' => $successB ? "true" : "false"
            ]));
            return $response->withHeader('Content-Type', 'application/json');

        } catch (\Throwable $e) {
            file_put_contents($logPath, "ADMIN_CONTROLLER ERROR: " . $e->getMessage() . "\n", FILE_APPEND);
            $response->getBody()->write(json_encode([
                'success_update_a' => "false",
                'success_update_b' => "false",
                'error' => $e->getMessage()
            ]));
            return $response->withHeader('Content-Type', 'application/json');
        }
    }

    public function insertBagColorByUserId($request, $response)
    {
        if (ob_get_level() > 0)
            ob_clean();
        try {
            $body = $request->getParsedBody();
            if (empty($body)) {
                $raw = file_get_contents('php://input');
                $body = json_decode($raw, true);
            }

            $memberid = $body['memberid'] ?? '';
            $age = $body['age'] ?? '';

            $c1 = $body['bag_color1a'] ?? '';
            $c2 = $body['bag_color2a'] ?? '';
            $c3 = $body['bag_color3a'] ?? '';
            $c4 = $body['bag_color4a'] ?? '';
            $c5 = $body['bag_color5a'] ?? '';
            $c6 = $body['bag_color6a'] ?? '';

            $sql = "INSERT INTO bagcolortb (memberid, age, bag_color1, bag_color2, bag_color3, bag_color4, bag_color5, bag_color6, date_color_updated) 
                    VALUES (:mid, :age, :c1, :c2, :c3, :c4, :c5, :c6, NOW())";

            $stmt = $this->db->prepare($sql);
            $success = $stmt->execute([
                ':mid' => $memberid,
                ':age' => $age,
                ':c1' => $c1,
                ':c2' => $c2,
                ':c3' => $c3,
                ':c4' => $c4,
                ':c5' => $c5,
                ':c6' => $c6
            ]);

            $response->getBody()->write(json_encode([
                'insert_color' => $success ? 'success' : 'fail'
            ]));
            return $response->withHeader('Content-Type', 'application/json');
        } catch (\Exception $e) {
            $response->getBody()->write(json_encode([
                'insert_color' => 'fail',
                'error' => $e->getMessage()
            ]));
            return $response->withHeader('Content-Type', 'application/json');
        }
    }

    public function colorBagByUserId($request, $response)
    {
        $userId = $request->getAttribute('userid');

        $sql = "SELECT * FROM bagcolortb WHERE memberid = :mid ORDER BY bag_id ASC LIMIT 2";
        $stmt = $this->db->prepare($sql);
        $stmt->execute([':mid' => $userId]);
        $rows = $stmt->fetchAll(\PDO::FETCH_OBJ);

        $colorSixA = $rows[0] ?? null;
        $colorSixB = $rows[1] ?? null;

        $response->getBody()->write(json_encode([
            'user_id' => $userId,
            'color_six_a' => $colorSixA,
            'color_six_b' => $colorSixB
        ]));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function findUserBagColor($request, $response)
    {
        try {
            $queryParams = $request->getQueryParams();
            $username = $request->getAttribute('username');
            if (empty($username)) {
                $username = $queryParams['username'] ?? '';
            }

            // Simple validation to prevent SQL injection if raw (though we use prepare)
            // But strict cleaning for display
            $username = trim($username);

            if (empty($username)) {
                // Return latest 50 users
                $sql = "SELECT memberid, username, realname, surname, birthday, fcm_token 
                        FROM membertb 
                        ORDER BY memberid DESC 
                        LIMIT 20";
                $stmt = $this->db->prepare($sql);
                $stmt->execute();
            } else {
                // Search
                $sql = "SELECT memberid, username, realname, surname, birthday, fcm_token,
                        (CASE 
                            WHEN username = :exact THEN 1
                            WHEN username LIKE :start THEN 2
                            WHEN realname = :exact THEN 3
                            WHEN realname LIKE :start THEN 4
                            WHEN memberid = :exact THEN 5
                            ELSE 6
                        END) AS relevance
                        FROM membertb 
                        WHERE username LIKE :query 
                           OR realname LIKE :query 
                           OR surname LIKE :query 
                           OR memberid LIKE :query
                        ORDER BY relevance ASC, memberid DESC 
                        LIMIT 20";
                $stmt = $this->db->prepare($sql);
                $stmt->execute([
                    ':query' => "%$username%",
                    ':exact' => $username,
                    ':start' => "$username%"
                ]);
            }

            $data = $stmt->fetchAll(\PDO::FETCH_OBJ);

            // Format response
            $usernamex = array_map(function ($user) {
                return [
                    'member_id' => (string) $user->memberid,
                    'username' => (string) $user->username,
                    'realname' => (string) ($user->realname ?? ''),
                    'surname' => (string) ($user->surname ?? ''),
                    'birthday' => (string) ($user->birthday ?? ''),
                    'fcm_token' => (string) ($user->fcm_token ?? '')
                ];
            }, $data);

            $json = json_encode(['result_userz' => $usernamex], JSON_UNESCAPED_UNICODE | JSON_PARTIAL_OUTPUT_ON_ERROR);

            if ($json === false) {
                $errorMsg = [
                    'status' => 'error',
                    'message' => 'JSON Encode Error: ' . json_last_error_msg()
                ];
                $response->getBody()->write(json_encode($errorMsg));
                return $response->withHeader('Content-Type', 'application/json; charset=utf-8')->withStatus(500);
            }

            $response->getBody()->write($json);
            return $response->withHeader('Content-Type', 'application/json; charset=utf-8');

        } catch (\Throwable $e) {
            $errorMsg = [
                'status' => 'error',
                'message' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine()
            ];
            $response->getBody()->write(json_encode($errorMsg));
            return $response->withHeader('Content-Type', 'application/json; charset=utf-8')->withStatus(500);
        }
    }

    public function addVipcode($request, $response)
    {
        $body = $request->getParsedBody();
        $codetype = filter_var($body['codetype'], FILTER_SANITIZE_STRING);
        $codename = filter_var($body['codename'], FILTER_SANITIZE_STRING);

        $sqlx = "SELECT * FROM secretcode WHERE codename LIKE '{$codename}'";

        $resultx = $this->db->prepare($sqlx);
        if ($resultx->execute()) {
            $data = $resultx->fetchAll(\PDO::FETCH_OBJ);

            if (count($data) > 0) {
                $response->getBody()->write(json_encode(array('activity' => 'insert', 'message' => 'dup')));
            } else {
                $sql = "INSERT INTO secretcode (codetype, codename) VALUES ('{$codetype}','{$codename}')";
                $result = $this->db->prepare($sql);
                if ($result->execute()) {
                    $response->getBody()->write(json_encode(array('activity' => 'insert', 'message' => 'success')));
                } else {
                    $response->getBody()->write(json_encode(array('activity' => 'insert', 'message' => 'fail')));
                }
            }
        }
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function listVipcode($request, $response)
    {
        $sql = "SELECT * FROM secretcode";
        $result = $this->db->prepare($sql);
        if ($result->execute()) {
            $data = $result->fetchAll(\PDO::FETCH_OBJ);

            $data_array = array("data_secret_code_list" => $data);
            $response->getBody()->write(json_encode($data_array));
        }
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function addNickName($request, $response)
    {

        $body = $request->getParsedBody();
        $thainame = filter_var($body['thainame'], FILTER_SANITIZE_STRING);
        $reangthai = filter_var($body['reangthai'], FILTER_SANITIZE_STRING);

        $leksat_thai = filter_var($body['leksat_thai'], FILTER_SANITIZE_STRING);
        $shadow = filter_var($body['shadow'], FILTER_SANITIZE_STRING);

        $sqlx = "SELECT thainame FROM nickname WHERE thainame = :thainame";

        $resultx = $this->db->prepare($sqlx);
        if ($resultx->execute([':thainame' => $thainame])) {
            $data = $resultx->fetchAll(\PDO::FETCH_OBJ);

            if (count($data) > 0) {
                $response->getBody()->write(json_encode(array('activity' => 'insert', 'message' => 'duplicate data')));
            } else {
                $sql = "INSERT INTO nickname (thainame, reangthai, leksat_thai, shadow) VALUES (:thainame, :reangthai, :leksat_thai, :shadow)";
                $result = $this->db->prepare($sql);
                if (
                    $result->execute([
                        ':thainame' => $thainame,
                        ':reangthai' => $reangthai,
                        ':leksat_thai' => $leksat_thai,
                        ':shadow' => $shadow
                    ])
                ) {
                    $response->getBody()->write(json_encode(array('activity' => 'insert', 'message' => 'success')));
                } else {
                    $errorInfo = $result->errorInfo();
                    $response->getBody()->write(json_encode(array('activity' => 'insert', 'message' => 'fail', 'debug' => $errorInfo)));
                }
            }
        }
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function addRealName($request, $response)
    {

        $body = $request->getParsedBody();
        $thainame = filter_var($body['thainame'], FILTER_SANITIZE_STRING);
        $reangthai = filter_var($body['reangthai'], FILTER_SANITIZE_STRING);

        $leksat_thai = filter_var($body['leksat_thai'], FILTER_SANITIZE_STRING);
        $shadow = filter_var($body['shadow'], FILTER_SANITIZE_STRING);


        $sqlx = "SELECT thainame FROM realname WHERE thainame = :thainame";

        $resultx = $this->db->prepare($sqlx);
        if ($resultx->execute([':thainame' => $thainame])) {
            $data = $resultx->fetchAll(\PDO::FETCH_OBJ);

            if (count($data) > 0) {
                $response->getBody()->write(json_encode(array('activity' => 'insert', 'message' => 'dup')));
            } else {
                $sql = "INSERT INTO realname (thainame, reangthai, leksat_thai, shadow) VALUES (:thainame, :reangthai, :leksat_thai, :shadow)";
                $result = $this->db->prepare($sql);
                if (
                    $result->execute([
                        ':thainame' => $thainame,
                        ':reangthai' => $reangthai,
                        ':leksat_thai' => $leksat_thai,
                        ':shadow' => $shadow
                    ])
                ) {
                    $response->getBody()->write(json_encode(array('activity' => 'insert', 'message' => 'success')));
                } else {
                    $errorInfo = $result->errorInfo();
                    $response->getBody()->write(json_encode(array('activity' => 'insert', 'message' => 'fail', 'debug' => $errorInfo)));
                }
            }
        }
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function listArticlesJson($request, $response)
    {
        $sql = "SELECT * FROM articles ORDER BY art_id DESC";
        $stmt = $this->db->query($sql);
        $articles = $stmt->fetchAll(\PDO::FETCH_ASSOC);

        $response->getBody()->write(json_encode($articles));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function saveArticleJson($request, $response)
    {
        $body = $request->getParsedBody();
        $id = $body['art_id'] ?? null;
        $title = $body['title'] ?? '';
        $slug = $body['slug'] ?? '';
        if (empty($slug))
            $slug = uniqid('art_');
        $excerpt = $body['excerpt'] ?? '';
        $category = $body['category'] ?? '';
        $content = $body['content'] ?? '';
        $is_published = ($body['is_published'] ?? 1) == 1 ? 1 : 0;
        $published_at = !empty($body['published_at']) ? $body['published_at'] : date('Y-m-d H:i:s');
        $title_short = $body['title_short'] ?? '';
        $image_url = $body['image_url'] ?? '';

        if ($id) {
            $sql = "UPDATE articles SET slug=:slug, title=:title, excerpt=:excerpt, category=:category, content=:content, is_published=:is_published, title_short=:title_short, image_url=:image_url WHERE art_id=:id";
            $stmt = $this->db->prepare($sql);
            $success = $stmt->execute([
                ':slug' => $slug,
                ':title' => $title,
                ':excerpt' => $excerpt,
                ':category' => $category,
                ':content' => $content,
                ':is_published' => $is_published,
                ':title_short' => $title_short,
                ':image_url' => $image_url,
                ':id' => $id
            ]);
        } else {
            $sql = "INSERT INTO articles (slug, title, excerpt, category, content, is_published, published_at, title_short, image_url) 
                    VALUES (:slug, :title, :excerpt, :category, :content, :is_published, :published_at, :title_short, :image_url)";
            $stmt = $this->db->prepare($sql);
            $success = $stmt->execute([
                ':slug' => $slug,
                ':title' => $title,
                ':excerpt' => $excerpt,
                ':category' => $category,
                ':content' => $content,
                ':is_published' => $is_published,
                ':published_at' => $published_at,
                ':title_short' => $title_short,
                ':image_url' => $image_url
            ]);
        }

        $response->getBody()->write(json_encode(['status' => $success ? 'success' : 'fail']));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function deleteArticleJson($request, $response)
    {
        $body = $request->getParsedBody();
        $id = $body['art_id'] ?? null;
        if ($id) {
            $stmt = $this->db->prepare("DELETE FROM articles WHERE art_id = :id");
            $success = $stmt->execute([':id' => $id]);
            $response->getBody()->write(json_encode(['status' => $success ? 'success' : 'fail']));
        } else {
            $response->getBody()->write(json_encode(['status' => 'fail', 'message' => 'Missing ID']));
        }
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function uploadArticleImage($request, $response)
    {
        $body = $request->getParsedBody();
        $base64Img = $body['base64Img'] ?? '';
        if (empty($base64Img)) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'No image data']));
            return $response->withHeader('Content-Type', 'application/json');
        }

        $image_name = 'art_' . time() . '.jpg';
        $path = UPLOAD_DIR . '/' . $image_name;

        // Ensure directory exists
        if (!is_dir(UPLOAD_DIR)) {
            mkdir(UPLOAD_DIR, 0777, true);
        }
        if (!is_writable(UPLOAD_DIR)) {
            chmod(UPLOAD_DIR, 0777);
        }

        $status = file_put_contents($path, base64_decode($base64Img));

        if ($status) {
            $url = '/uploads/' . $image_name;
            $response->getBody()->write(json_encode(['status' => 'success', 'url' => $url]));
        } else {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => 'Upload failed']));
        }
        return $response->withHeader('Content-Type', 'application/json');
    }

    /* --- Bag Colors Web Management --- */

    public function viewBagColorsMain($request, $response)
    {
        $queryParams = $request->getQueryParams();
        $search = $queryParams['search'] ?? '';
        $users = [];

        if (!empty($search)) {
            $sql = "SELECT memberid, username, realname, surname, birthday, fcm_token FROM membertb 
                    WHERE username LIKE :s OR realname LIKE :s OR surname LIKE :s OR memberid = :exact 
                    LIMIT 50";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([':s' => "%$search%", ':exact' => $search]);
            $users = $stmt->fetchAll(\PDO::FETCH_OBJ);
        } else {
            // Default show latest 20
            $sql = "SELECT memberid, username, realname, surname, birthday, fcm_token FROM membertb 
                    ORDER BY memberid DESC LIMIT 20";
            $stmt = $this->db->query($sql);
            $users = $stmt->fetchAll(\PDO::FETCH_OBJ);
        }

        return $this->container->get('view')->render($response, 'web_admin_bag_colors.php', [
            'users' => $users,
            'search' => $search
        ]);
    }

    public function viewBagColorEdit($request, $response, $args)
    {
        $memberid = $args['memberid'];

        // Get User
        $stmt = $this->db->prepare("SELECT * FROM membertb WHERE memberid = :mid");
        $stmt->execute([':mid' => $memberid]);
        $targetUser = $stmt->fetch(\PDO::FETCH_OBJ);

        if (!$targetUser) {
            return $response->withHeader('Location', '/web/admin/bag-colors')->withStatus(302);
        }

        // Get History
        $stmtBag = $this->db->prepare("SELECT * FROM bagcolortb WHERE memberid = :mid ORDER BY age ASC");
        $stmtBag->execute([':mid' => $memberid]);
        $bagColors = $stmtBag->fetchAll(\PDO::FETCH_OBJ);

        return $this->container->get('view')->render($response, 'web_admin_bag_color_edit.php', [
            'targetUser' => $targetUser,
            'bagColors' => $bagColors
        ]);
    }

    public function saveBagColorWeb($request, $response)
    {
        $body = $request->getParsedBody();
        $memberid = $body['memberid'] ?? '';
        $age = $body['age'] ?? '';

        if (empty($memberid) || empty($age)) {
            return $response->withHeader('Location', '/web/admin/bag-colors')->withStatus(302);
        }

        $c1 = $body['c1'] ?? '#FFFFFF';
        $c2 = $body['c2'] ?? '#FFFFFF';
        $c3 = $body['c3'] ?? '#FFFFFF';
        $c4 = $body['c4'] ?? '#FFFFFF';
        $c5 = $body['c5'] ?? '#FFFFFF';
        $c6 = $body['c6'] ?? '#FFFFFF';

        $db = $this->db;
        $stmt = $db->prepare("SELECT bag_id FROM bagcolortb WHERE memberid = :mid AND age = :age LIMIT 1");
        $stmt->execute([':mid' => $memberid, ':age' => $age]);
        $row = $stmt->fetch(\PDO::FETCH_OBJ);

        if ($row) {
            $sql = "UPDATE bagcolortb SET bag_color1=:c1, bag_color2=:c2, bag_color3=:c3, bag_color4=:c4, bag_color5=:c5, bag_color6=:c6, date_color_updated=NOW() WHERE bag_id=:id";
            $db->prepare($sql)->execute([':c1' => $c1, ':c2' => $c2, ':c3' => $c3, ':c4' => $c4, ':c5' => $c5, ':c6' => $c6, ':id' => $row->bag_id]);
        } else {
            $sql = "INSERT INTO bagcolortb (memberid, age, bag_color1, bag_color2, bag_color3, bag_color4, bag_color5, bag_color6, date_color_updated) VALUES (:mid, :age, :c1, :c2, :c3, :c4, :c5, :c6, NOW())";
            $db->prepare($sql)->execute([':mid' => $memberid, ':age' => $age, ':c1' => $c1, ':c2' => $c2, ':c3' => $c3, ':c4' => $c4, ':c5' => $c5, ':c6' => $c6]);
        }

        return $response->withHeader('Location', '/web/admin/bag-colors/edit/' . $memberid)->withStatus(302);
    }
}
