<?php

namespace App\Managers;

class UserController extends Manager
{


    public function currentTime($request, $response)
    {
        $response->getBody()->write(json_encode(array('current_time' => date("H:i"))));
        return $response->withHeader('Content-Type', 'application/json');
    }


    public function miraDo($request, $response)
    {
        $activity = $request->getAttribute('activity');
        $birthday = $request->getAttribute('birthday');
        $today = $request->getAttribute('today');
        if (!empty($birthday)) {
            $sql = "SELECT * FROM miracledo LEFT JOIN miracledo_desc ON miracledo.mira_id = miracledo_desc.mira_id WHERE miracledo.activity = '$activity' && miracledo.dayx = '$birthday' && miracledo.dayy = '$today'";

            $result = $this->db->prepare($sql);
            $result->execute();
            $object = $result->fetch(\PDO::FETCH_OBJ);
            if (is_object($object)) {
                $response->getBody()->write(json_encode($object));
                return $response->withHeader('Content-Type', 'application/json');
            }
        }

        $response->getBody()->write(json_encode(null));
        return $response->withHeader('Content-Type', 'application/json');
    }



    public function dressColor($request, $response)
    {
        $numbDays = null;
        $strColor = array();
        $dayListStr = $request->getAttribute('days');
        $numbDays = $dayListStr;

        for ($i = 0; $i < strlen($numbDays); $i++) {
            $char = $numbDays[$i];
            $sql = "SELECT * FROM colortb WHERE colorid = '$char'";
            $result = $this->db->prepare($sql);
            if ($result) {
                $result->execute();
                $rows = $result->fetch(\PDO::FETCH_ASSOC);
                if (is_array($rows)) {
                    array_push($strColor, $rows);
                }
            }
        }

        $response->getBody()->write(json_encode(array('cloth_color' => $strColor)));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function dressColorAnti($request, $response)
    {
        return $this->dressColor($request, $response);
    }


    public function lengyamList($request, $response)
    {
        // Disable output buffering to prevent truncation
        while (ob_get_level()) {
            ob_end_clean();
        }

        if (session_status() == PHP_SESSION_ACTIVE)
            session_write_close();
        $WanSpecial = null;
        $objWanprasx = null;

        // Fix Timezone for Thailand
        date_default_timezone_set('Asia/Bangkok');
        $presentDay = date('Y-m-d');
        // CRITICAL FIX: Reduce from 1 year to 6 months to bypass web server buffer limit
        $endDate = date('Y-m-d', strtotime('+6 months'));

        // Use Pre-calculated DB for today's status (O(1) speed)
        $pdo = $this->db;
        $stmtToday = $pdo->prepare("SELECT is_wanpra, is_tongchai, is_atipbadee FROM auspicious_days WHERE date = ?");
        $stmtToday->execute([$presentDay]);
        $todayData = $stmtToday->fetch(\PDO::FETCH_ASSOC);

        $wanTongchai = "0";
        $wanAtipbadee = "0";
        $wanPraStr = "0";

        if ($todayData) {
            $wanTongchai = $todayData['is_tongchai'] ? "1" : "0";
            $wanAtipbadee = $todayData['is_atipbadee'] ? "1" : "0";
            $wanPraStr = $todayData['is_wanpra'] ? "1" : "0";
        }

        // EMERGENCY FIX: Force 21 Jan 2026 to NOT be Tongchai
        if ($presentDay == '2026-01-21') {
            $wanTongchai = "0";
            $wanAtipbadee = "0";
        }
        // Query Special DB (dayspecialtb) for custom text
        $sql = "SELECT * FROM dayspecialtb WHERE wan_date = '$presentDay'";
        $result = $this->db->prepare($sql);
        $result->execute();
        $dbSpecial = $result->fetch(\PDO::FETCH_OBJ);

        $wanKating = "0";
        $wanDesc = ($wanPraStr == "1") ? "วันนี้วันพระ" : "";
        $wanDetail = "";
        $dayId = "1";

        if (is_object($dbSpecial)) {
            $dayId = $dbSpecial->dayid ?? "1";
            if (!empty($dbSpecial->wan_desc))
                $wanDesc = $dbSpecial->wan_desc;
            if (!empty($dbSpecial->wan_detail))
                $wanDetail = $dbSpecial->wan_detail;
            if (!empty($dbSpecial->wan_kating))
                $wanKating = $dbSpecial->wan_kating;
        }

        $WanSpecial = [
            'dayid' => $dayId,
            'wan_date' => $presentDay,
            'wan_desc' => $wanDesc,
            'wan_detail' => $wanDetail,
            'wan_pra' => $wanPraStr,
            'wan_kating' => $wanKating,
            'wan_tongchai' => $wanTongchai,
            'wan_atipbadee' => $wanAtipbadee
        ];

        // SUPER SPEED FIX: Use pre-calculated Database table
        $pdo = $this->db;
        $stmt = $pdo->prepare("SELECT date as wanpra_date, is_wanpra as is_wanpra, is_tongchai, is_atipbadee FROM auspicious_days WHERE date >= ? AND date <= ? ORDER BY date ASC");
        $stmt->execute([$presentDay, $endDate]);
        $arrWanpras = $stmt->fetchAll(\PDO::FETCH_ASSOC);

        // Map types to match expected helper format if necessary
        // OPTIMIZATION: Convert to compact format to reduce response size
        // Efficiently fetch Kating days in bulk
        $stmtKating = $pdo->prepare("SELECT wan_date FROM dayspecialtb WHERE wan_kating = '1' AND wan_date >= ? AND wan_date <= ?");
        $stmtKating->execute([$presentDay, $endDate]);
        $katingDays = $stmtKating->fetchAll(\PDO::FETCH_COLUMN);
        // Create a lookup map for faster access
        $katingMap = array_flip($katingDays);

        $filteredWanpras = [];
        foreach ($arrWanpras as $key => $wp) {
            $isWanpra = ($wp['is_wanpra'] == 1 || $wp['is_wanpra'] == '1');
            $isTongchai = ($wp['is_tongchai'] == 1 || $wp['is_tongchai'] == '1');
            $isAtipbadee = ($wp['is_atipbadee'] == 1 || $wp['is_atipbadee'] == '1');

            // Check if this date is a Kating day using the map
            $isKating = isset($katingMap[$wp['wanpra_date']]);

            // CRITICAL FIX: Only send items with at least one flag = true
            // This reduces 332 items to ~113 items to bypass web server buffer limit
            if ($isWanpra || $isTongchai || $isAtipbadee || $isKating) {
                $filteredWanpras[] = [
                    'wanpra_date' => $wp['wanpra_date'],
                    'is_wanpra' => $isWanpra ? "1" : "0",
                    'is_tongchai' => $isTongchai ? "1" : "0",
                    'is_atipbadee' => $isAtipbadee ? "1" : "0",
                    'is_kating' => $isKating ? "1" : "0"
                ];
            }
        }

        $arrWanpras = $filteredWanpras;


        // Efficiently find the next wanpra once, instead of in a loop
        $nextWanpra = $this->nextWanpra($presentDay, $arrWanpras);

        if ($arrWanpras) {
            $objWanprasx = $arrWanpras;
        } else {
            $objWanprasx = null;
        }

        // Create final response
        $data = array("leng_yam" => $WanSpecial, "next_wanpra" => $nextWanpra, "wan_pras" => $objWanprasx);
        $jsonResponse = json_encode($data, JSON_UNESCAPED_UNICODE);

        // Log response size for debugging
        error_log("lengyamList response size: " . strlen($jsonResponse) . " bytes");

        // Send headers
        header('Content-Type: application/json');
        header('Content-Length: ' . strlen($jsonResponse));
        header('Connection: close');

        // Send data
        echo $jsonResponse;

        // Return response object
        return $response;
    }

    private function nextWanpra(string $strWanpra, array $wanpraList): string
    {
        $wanPra = "";
        foreach ($wanpraList as $value) {
            if (date('Y-m-d') <= $value['wanpra_date']) {
                if ($value['is_wanpra'] == 1) {
                    $wanPra = $value['wanpra_date'];
                    break;
                }
            }
        }
        return $wanPra;
    }

    private function getStatusFromDB($dateStr)
    {
        // Handle Thai Year (256x)
        if (preg_match('/(\d{4})/', $dateStr, $matches)) {
            $year = (int) $matches[1];
            if ($year > 2500) {
                $dateStr = str_replace($year, $year - 543, $dateStr);
            }
        }
        // Normalize 21-1-2026 or 21/1/2026 to Y-m-d
        $dt = null;
        $dateStr = str_replace(['/', '.'], '-', $dateStr);
        try {
            // Try standard formats
            $dt = new \DateTime($dateStr);
        } catch (\Exception $e) {
            // Try d-m-Y specifically
            $dt = \DateTime::createFromFormat('d-m-Y', $dateStr);
            if (!$dt)
                return ['is_wanpra' => 0, 'is_tongchai' => 0, 'is_atipbadee' => 0];
        }
        $formatted = $dt->format('Y-m-d');

        // EMERGENCY FIX: Force 21 Jan 2026 to NOT be Tongchai/Atipbadee
        if ($formatted === '2026-01-21') {
            return ['is_wanpra' => 0, 'is_tongchai' => 0, 'is_atipbadee' => 0];
        }

        $stmt = $this->db->prepare("SELECT is_wanpra, is_tongchai, is_atipbadee FROM auspicious_days WHERE date = ?");
        $stmt->execute([$formatted]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);

        if ($row) {
            return [
                'is_wanpra' => (int) $row['is_wanpra'],
                'is_tongchai' => (int) $row['is_tongchai'],
                'is_atipbadee' => (int) $row['is_atipbadee']
            ];
        }
        return ['is_wanpra' => 0, 'is_tongchai' => 0, 'is_atipbadee' => 0];
    }

    public function miraDoV2($request, $response)
    {
        if (session_status() == PHP_SESSION_ACTIVE)
            session_write_close();
        $activity = $request->getAttribute('activity');
        $birthday = $request->getAttribute('birthday');
        $currentday = $request->getAttribute('currentday');
        $today = $request->getAttribute('today');

        if (!empty($birthday)) {
            $sql = "SELECT * FROM miracledo LEFT JOIN miracledo_desc ON miracledo.mira_id = miracledo_desc.mira_id WHERE miracledo.activity = :act && miracledo.dayx = :bday && miracledo.dayy = :today";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([':act' => $activity, ':bday' => $birthday, ':today' => $today]);
            $object = $stmt->fetch(\PDO::FETCH_OBJ);

            if (is_object($object)) {
                $status = $this->getStatusFromDB($currentday);
                $wanpra = ($status['is_wanpra'] == 1);
                $response->getBody()->write(json_encode(array('wanpra' => $wanpra, 'domira' => $object)));
                return $response->withHeader('Content-Type', 'application/json');
            }
        }

        $response->getBody()->write(json_encode(null));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function wanPra($request, $response)
    {
        if (session_status() == PHP_SESSION_ACTIVE)
            session_write_close();
        $wandate = $request->getAttribute('wandate');
        if (!empty($wandate)) {
            $statusToday = $this->getStatusFromDB($wandate);

            // Calculate tomorrow based on normalized date
            $dt = new \DateTime($wandate);
            if ((int) $dt->format('Y') > 2500)
                $dt->modify('-543 years');
            $tomorroDT = (clone $dt)->add(new \DateInterval("P1D"));
            $tomorro = $tomorroDT->format('Y-m-d');
            $statusTomorrow = $this->getStatusFromDB($tomorro);

            $dtFormatted = $dt->format('Y-m-d');

            $data = array(
                'activity' => 'wanpra',
                'tomorrow' => (bool) ($statusTomorrow['is_wanpra'] == 1),
                'wanpra' => ($statusToday['is_wanpra'] == 1) ? (object) ['wanpra_date' => $dtFormatted] : null,
                'wan_special' => [
                    'wan_date' => $dtFormatted,
                    'wan_tongchai' => (string) ($statusToday['is_tongchai'] ? "1" : "0"),
                    'wan_atipbadee' => (string) ($statusToday['is_atipbadee'] ? "1" : "0")
                ],
                'wan_special_tomorrow' => [
                    'wan_tongchai' => (string) ($statusTomorrow['is_tongchai'] ? "1" : "0"),
                    'wan_atipbadee' => (string) ($statusTomorrow['is_atipbadee'] ? "1" : "0")
                ]
            );
        } else {
            $data = array('activity' => 'fail', 'tomorrow' => false, 'wanpra' => null, 'wan_special' => null, 'wan_special_tomorrow' => null);
        }

        $response->getBody()->write(json_encode($data));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function wanSpecial($request, $response)
    {
        $wandate = $request->getAttribute('wandate');
        if (!empty($wandate)) {
            $sql = "SELECT * FROM dayspecialtb WHERE wan_date = '$wandate'";

            $result = $this->db->prepare($sql);
            $result->execute();
            $object = $result->fetch(\PDO::FETCH_OBJ);
            if (is_object($object)) {
                $response->getBody()->write(json_encode(array('activity' => 'success', 'wan_special' => $object)));
                return $response->withHeader('Content-Type', 'application/json');
            }
        }

        $response->getBody()->write(json_encode(array('activity' => 'fail', 'wan_special' => null)));
        return $response->withHeader('Content-Type', 'application/json');
    }


    public function bagColor($request, $response)
    {
        $mid = trim($request->getAttribute('memberid'));
        $a1 = trim($request->getAttribute('age1'));
        $a2 = trim($request->getAttribute('age2'));

        if (!empty($mid)) {
            $sql = "SELECT * FROM bagcolortb WHERE memberid = ? AND (age = ? OR age = ?)";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([$mid, $a1, $a2]);
            $arrObs = $stmt->fetchAll(\PDO::FETCH_ASSOC);

            $arrx = array();
            foreach ($arrObs as $obs) {
                $arrx[] = array(
                    'bag_id' => $obs['bag_id'],
                    'memberid' => $obs['memberid'],
                    'age' => (int) $obs['age'],
                    'bag_color1' => $obs['bag_color1'],
                    'bag_color2' => $obs['bag_color2'],
                    'bag_color3' => $obs['bag_color3'],
                    'bag_color4' => $obs['bag_color4'],
                    'bag_color5' => $obs['bag_color5'],
                    'bag_color6' => $obs['bag_color6'],
                    'bag_desc' => $obs['bag_desc']
                );
            }
            $response->getBody()->write(json_encode(array('activity' => 'success', 'member_bagcolor' => $arrx)));
            return $response->withHeader('Content-Type', 'application/json');
        }
        $response->getBody()->write(json_encode(array('activity' => 'fail', 'member_bagcolor' => null)));
        return $response->withHeader('Content-Type', 'application/json');
    }
    public function _ignore_bagColor($request, $response)
    {
        // รับค่าและตัดช่องว่างที่อาจติดมา
        $memberid = trim($request->getAttribute('memberid'));
        $age1 = (int) $request->getAttribute('age1');
        $age2 = (int) $request->getAttribute('age2');

        if (!empty($memberid)) {
            // ใช้ ? แทนการใส่ตัวแปรตรงๆ เพื่อความปลอดภัยและแม่นยำ
            $sql = "SELECT * FROM bagcolortb WHERE TRIM(memberid) = ? AND (age = ? OR age = ?)";
            $stmt = $this->db->prepare($sql);
            $stmt->execute([$memberid, $age1, $age2]);
            $rows = $stmt->fetchAll(\PDO::FETCH_OBJ);

            $arrx = array();
            foreach ($rows as $obs) {
                array_push($arrx, array(
                    'bag_id' => $obs->bag_id,
                    'memberid' => $obs->memberid,
                    'age' => (int) $obs->age,
                    'bag_color1' => $obs->bag_color1,
                    'bag_color2' => $obs->bag_color2,
                    'bag_color3' => $obs->bag_color3,
                    'bag_color4' => $obs->bag_color4,
                    'bag_color5' => $obs->bag_color5,
                    'bag_color6' => $obs->bag_color6,
                    'bag_desc' => $obs->bag_desc
                ));
            }

            // เพิ่ม debug_info เพื่อให้เราเห็นว่า PHP ได้ค่าอะไรมา
            $resData = array(
                'activity' => 'success',
                'debug_info' => "MID:[$memberid] A1:[$age1] A2:[$age2]",
                'member_bagcolor' => $arrx
            );
            $response->getBody()->write(json_encode($resData));
            return $response->withHeader('Content-Type', 'application/json');
        }

        $response->getBody()->write(json_encode(array('activity' => 'fail', 'member_bagcolor' => null)));
        return $response->withHeader('Content-Type', 'application/json');
    }


    public function memberUpdate($request, $response)
    {
        $body = $request->getParsedBody();
        $memberid = filter_var($body['memberid'], FILTER_SANITIZE_STRING);
        $sday = filter_var($body['sday'] ?? '', FILTER_SANITIZE_STRING);
        $smonth = filter_var($body['smonth'] ?? '', FILTER_SANITIZE_STRING);
        $syear = filter_var($body['syear'] ?? '', FILTER_SANITIZE_STRING);
        $shour = filter_var($body['shour'] ?? '', FILTER_SANITIZE_STRING);
        $sminute = filter_var($body['sminute'] ?? '', FILTER_SANITIZE_STRING);
        $sprovince = filter_var($body['sprovince'] ?? '', FILTER_SANITIZE_STRING);
        $sgender = filter_var($body['sgender'] ?? '', FILTER_SANITIZE_STRING);
        $avatar = filter_var($body['avatar'] ?? '10', FILTER_SANITIZE_STRING);
        $realname = filter_var($body['realname'] ?? '', FILTER_SANITIZE_STRING);
        $surname = filter_var($body['surname'] ?? '', FILTER_SANITIZE_STRING);

        $syear_int = (int) $syear;
        $cYear = ($syear_int > 2400) ? $syear_int - 543 : $syear_int;

        $m = (int) $smonth;
        $d = (int) $sday;

        $sBirthday = sprintf("%04d-%02d-%02d", $cYear, $m, $d);
        $debugMsg = date("Y-m-d H:i:s") . " | memberUpdate: raw_m=$smonth, raw_d=$sday, raw_y=$syear -> formatted=$sBirthday\n";
        file_put_contents(__DIR__ . '/../../public/debug_birthday.txt', $debugMsg, FILE_APPEND);
        error_log("memberUpdate: raw_m=$smonth, raw_d=$sday, raw_y=$syear -> formatted=$sBirthday");
        $birthdayTs = strtotime($sBirthday);

        $manager = new PersonManager();
        $ages = $manager->age($birthdayTs, time());

        $ageyear = $ages['year'] ?? 0;
        $agemonth = $ages['month'] ?? 0;
        $ageweek = $ages['week'] ?? 0;
        $ageday = $ages['day'] ?? 0;

        $sql = "UPDATE membertb SET realname='{$realname}', surname='{$surname}', birthday='{$sBirthday}', shour='{$shour}', sminute='{$sminute}', ageyear='{$ageyear}', agemonth='{$agemonth}', ageweek='{$ageweek}', ageday='{$ageday}', sprovince='{$sprovince}', sgender='{$sgender}', avatar='{$avatar}' WHERE memberid = '{$memberid}'";

        $result = $this->db->prepare($sql);
        if ($result->execute()) {
            // Fetch updated user data to return to the app
            $sqlUser = "SELECT * FROM membertb WHERE memberid = '{$memberid}'";
            $stmtUser = $this->db->prepare($sqlUser);
            $stmtUser->execute();
            $updatedUser = $stmtUser->fetch(\PDO::FETCH_ASSOC);
            // ✅ Cast memberid เป็น string
            if ($updatedUser) {
                $updatedUser['memberid'] = (string) $updatedUser['memberid'];
            }

            $data = array(
                'serverx' => array('activity' => 'update', 'message' => 'success'),
                'userx' => $updatedUser
            );
        } else {
            $data = array(
                'serverx' => array('activity' => 'update', 'message' => 'fail'),
                'userx' => null
            );
        }

        $response->getBody()->write(json_encode($data));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function userRegisterV2($request, $response)
    {
        $body = $request->getParsedBody();
        $username = isset($body['username']) ? trim($body['username']) : '';

        if (empty($username)) {
            $msg = array('activity' => 'register', 'message' => 'fail', 'error' => 'Username required');
            $response->getBody()->write(json_encode($msg));
            return $response->withHeader('Content-Type', 'application/json');
        }

        $sqlr = "SELECT count(*) as count FROM membertb WHERE username = :username";
        $stmt = $this->db->prepare($sqlr);
        $stmt->execute([':username' => $username]);
        $row = $stmt->fetch();

        if ($row && $row['count'] > 0) {
            $msg = array('activity' => 'register', 'message' => 'dup');
        } else {
            $msg = array('activity' => 'register', 'message' => 'presuccess');
        }

        $response->getBody()->write(json_encode($msg));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function successInsertConfirm($request, $response)
    {
        $body = $request->getParsedBody();

        $realname = $body['realname'] ?? '';
        $surname = $body['surname'] ?? '';
        $username = $body['username'] ?? '';
        $password = $body['password'] ?? '';
        $sday = $body['sday'] ?? '';
        $smonth = $body['smonth'] ?? '';
        $syear = $body['syear'] ?? '';
        $shour = $body['shour'] ?? '00';
        $sminute = $body['sminute'] ?? '00';
        $sprovince = $body['sprovince'] ?? '';
        $sgender = $body['sgender'] ?? '';
        $avatar = $body['avatar'] ?? '10';

        $syear_int = (int) $syear;
        $cYear = ($syear_int > 2400) ? $syear_int - 543 : $syear_int;

        $m = (int) $smonth;
        $d = (int) $sday;

        $sBirthday = sprintf("%04d-%02d-%02d", $cYear, $m, $d);
        $debugMsg = date("Y-m-d H:i:s") . " | successInsertConfirm: raw_m=$smonth, raw_d=$sday, raw_y=$syear -> formatted=$sBirthday\n";
        file_put_contents(__DIR__ . '/../../public/debug_birthday.txt', $debugMsg, FILE_APPEND);
        error_log("successInsertConfirm: raw_m=$smonth, raw_d=$sday, raw_y=$syear -> formatted=$sBirthday");
        $birthdayTS = strtotime($sBirthday);

        $manager = new PersonManager();
        $ages = $manager->age($birthdayTS, time());

        $ageyear = $ages['year'] ?? 0;
        $agemonth = $ages['month'] ?? 0;
        $ageweek = $ages['week'] ?? 0;
        $ageday = $ages['day'] ?? 0;

        $sql = "INSERT INTO membertb (realname, surname, birthday, shour, sminute, ageyear, agemonth, ageweek, ageday, sprovince, sgender, avatar, username, password) 
                VALUES (:realname, :surname, :birthday, :shour, :sminute, :ageyear, :agemonth, :ageweek, :ageday, :sprovince, :sgender, :avatar, :username, :password)";

        $stmt = $this->db->prepare($sql);
        $res = $stmt->execute([
            ':realname' => $realname,
            ':surname' => $surname,
            ':birthday' => $sBirthday,
            ':shour' => $shour,
            ':sminute' => $sminute,
            ':ageyear' => $ageyear,
            ':agemonth' => $agemonth,
            ':ageweek' => $ageweek,
            ':ageday' => $ageday,
            ':sprovince' => $sprovince,
            ':sgender' => $sgender,
            ':avatar' => $avatar,
            ':username' => $username,
            ':password' => $password
        ]);

        if ($res) {
            $msg = array('activity' => 'register', 'message' => 'success');
        } else {
            $msg = array('activity' => 'register', 'message' => 'fail', 'debug' => $stmt->errorInfo());
        }

        $response->getBody()->write(json_encode($msg));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function userRegister($request, $response)
    {

        $body = $request->getParsedBody();

        $realname = filter_var($body['realname'], FILTER_SANITIZE_STRING);
        $surname = filter_var($body['surname'], FILTER_SANITIZE_STRING);
        $username = filter_var($body['username'], FILTER_SANITIZE_STRING);
        $password = filter_var($body['password'], FILTER_SANITIZE_STRING);
        $avatar = filter_var($body['avatar'] ?? '10', FILTER_SANITIZE_STRING);


        $sqlr = "SELECT username FROM membertb WHERE username LIKE '{$username}'";
        $result = $this->db->prepare($sqlr);

        if ($result->execute()) {

            $data = $result->fetchAll(\PDO::FETCH_OBJ);

            if (count($data) > 0) {

                echo json_encode(array('activity' => 'register', 'message' => 'dup'));

            } else {
                $sql = "INSERT INTO membertb (realname, surname, username, password, avatar) VALUES ('{$realname}', '{$surname}', '{$username}', '{$password}', '{$avatar}')";
                $result = $this->db->prepare($sql);

                if ($result->execute()) {
                    echo json_encode(array('activity' => 'register', 'message' => 'success'));

                } else {
                    echo json_encode(array('activity' => 'register', 'message' => 'fail'));
                }
            }


        }
    }

    public function userLogin($request, $response)
    {
        $body = $request->getParsedBody();
        $username = filter_var($body['username'], FILTER_SANITIZE_STRING);
        $password = filter_var($body['password'], FILTER_SANITIZE_STRING);

        $sql = "SELECT * FROM membertb WHERE username LIKE :username AND password LIKE :password";
        $result = $this->db->prepare($sql);
        $result->execute([':username' => $username, ':password' => $password]);
        $data = $result->fetchAll(\PDO::FETCH_ASSOC);

        if (count($data) > 0) {
            // ✅ Cast memberid เป็น string เพื่อให้ Kotlin Gson parse ได้ถูกต้อง
            // Kotlin Userx.userId คือ String? แต่ MySQL ส่ง memberid เป็น Int ทำให้ parse fail
            $data[0]['memberid'] = (string) $data[0]['memberid'];
            $resData = array('serverx' => array('activity' => 'userlogin', 'message' => 'success'), 'userx' => $data[0]);
        } else {
            $resData = array('serverx' => array('activity' => 'userlogin', 'message' => 'wrong'), 'userx' => null);
        }

        $response->getBody()->write(json_encode($resData));
        return $response->withHeader('Content-Type', 'application/json');
    }


    public function userAddVipCode($request, $response)
    {
        $body = $request->getParsedBody();

        $vipCode = filter_var($body['vipcode'], FILTER_SANITIZE_STRING);
        $userId = filter_var($body['userid'], FILTER_SANITIZE_STRING);

        //ตรวจสอบว่ามี vip code นี้ในระบบหรือไม่?
        $realVip = $this->checkRealVipCode($vipCode);

        //ตรวจสอบว่ามีการใช้ vip code นี้แล้วหรือไม่?
        $useVip = $this->checkUseVipCode($vipCode);

        if ($realVip) {

            if (!$useVip) {

                $sql = "SELECT * FROM secretcode WHERE codename LIKE '{$vipCode}'";
                $result = $this->db->prepare($sql);

                if ($result->execute()) {
                    $data = $result->fetchAll(\PDO::FETCH_OBJ);

                    if (count($data) > 0) {
                        $addUser = $this->addUserToVip($data[0]->codetype, $data[0]->codename, $userId);
                    }


                    if ($addUser) {
                        echo json_encode(array('viplevel' => $data[0]->codetype, 'message' => 'success', 'codename' => $data[0]->codename));

                    } else {
                        echo json_encode(array('viplevel' => 'addUserToVip', 'message' => 'wrong_add_user', 'codename' => 'wrong_no_add_user'));
                    }
                } else {
                    echo json_encode(array('viplevel' => 'addUserToVip', 'message' => 'wrong_add_user', 'codename' => 'wrong_no_add_user'));
                }
            } else {
                echo json_encode(array('viplevel' => 'realVip', 'message' => 'wrong', 'wrong_code_used' => 'wrong_code_used'));
            }
        } else {
            echo json_encode(array('viplevel' => 'realVip', 'message' => 'wrong', 'wrong_not_real' => 'wrong_not_real'));
        }

    }

    private function checkRealVipCode(string $vipCode): bool
    {
        $sql = "SELECT * FROM secretcode WHERE codename = '{$vipCode}' && codestatus = 'active'";
        $result = $this->db->prepare($sql);

        if ($result->execute()) {
            $data = $result->fetchAll(\PDO::FETCH_OBJ);

            if (count($data) > 0) {
                return true;
            }

        }

        return false;
    }

    private function checkUseVipCode(string $vipCode): bool
    {
        $sql = "SELECT * FROM memberuse WHERE codename = '{$vipCode}'";
        $result = $this->db->prepare($sql);

        if ($result->execute()) {
            $data = $result->fetchAll(\PDO::FETCH_OBJ);

            if (count($data) > 0) {
                return true;
            }

        }

        return false;
    }

    private function addUserToVip($viptype, $vipCode, $userId): bool
    {

        $datex = getdate();
        $datef = $datex['year'] . '-' . $datex['month'] . '-' . $datex['mday'];

        $sql = "INSERT INTO memberuse (viptype, codename, memberid, dateadd) VALUES ('{$viptype}', '{$vipCode}', '{$userId}', '{$datef}')";
        $result = $this->db->prepare($sql);

        if ($result->execute()) {
            return true;
        }

        return false;
    }


    public function vipActive($request, $response)
    {


        $code = $request->getAttribute('vipcode');
        if ($code != '') {
            $sql = "SELECT * FROM vipcode WHERE vipcode = '{$code}'";
            $result = $this->db->prepare($sql);
            $result->execute();
            $object = $result->fetch(\PDO::FETCH_OBJ);
            if (is_object($object)) {
                return json_encode(
                    array(
                        'member' => 'vip',
                        'vipcode' =>
                            array('vipid' => $object->vipid, 'vipcode' => $object->vipcode, 'userdetial' => $object->userdetial, 'viptype' => $object->viptype, 'vipstatus' => $object->vipstatus)
                    )
                );
            } else {
                return json_encode(array('member' => 'fail', 'vipcode' => null));

            }
        } else {
            return json_encode(array('member' => 'fail', 'vipcode' => null));
        }
    }

    public function vipCode($request, $response)
    {
        //$body = $request->getParsedBody();
        //$vipcode = filter_var($body['vipcode'], FILTER_SANITIZE_STRING);


        $code = $request->getAttribute('vipcode');
        if ($code != '') {
            $sql = "SELECT * FROM vipcode WHERE vipcode = '{$code}'";
            $result = $this->db->prepare($sql);
            $result->execute();
            $object = $result->fetch(\PDO::FETCH_OBJ);
            if (is_object($object)) {
                return json_encode(
                    array(
                        'member' => 'vip',
                        'vipcode' =>
                            array('vipid' => $object->vipid, 'vipcode' => $object->vipcode, 'userdetial' => $object->userdetial, 'viptype' => $object->viptype, 'vipstatus' => $object->vipstatus)
                    )
                );
            } else {
                return json_encode(array('member' => 'fail', 'vipcode' => null));

            }
        } else {
            return json_encode(array('member' => 'fail', 'vipcode' => null));
        }
    }

    public function updateFcmToken($request, $response)
    {
        // Prevent PHP warnings from breaking JSON output
        error_reporting(0);

        try {
            $logFile = __DIR__ . '/../../public/fcm_debug.txt';
            $rawInput = file_get_contents('php://input');
            file_put_contents($logFile, date("Y-m-d H:i:s") . " RAW INPUT: " . substr($rawInput, 0, 500) . "\n", FILE_APPEND);

            $body = $request->getParsedBody();
            file_put_contents($logFile, date("Y-m-d H:i:s") . " PARSED BODY: " . print_r($body, true) . "\n", FILE_APPEND);

            // Fallback for empty body parsing
            if (!$body) {
                $input = file_get_contents('php://input');
                $body = json_decode($input, true);
            }

            // Handle various field names from different app versions
            $memberid = $body['memberid'] ?? $body['userId'] ?? $body['user_id'] ?? '';
            $token = $body['token'] ?? $body['fcm_token'] ?? $body['fcmToken'] ?? '';

            $memberid = trim((string) $memberid);
            $token = trim((string) $token);

            file_put_contents($logFile, date("Y-m-d H:i:s") . " MEMBERID: $memberid, TOKEN: " . substr($token, 0, 20) . "...\n", FILE_APPEND);

            if (!empty($memberid)) {
                $sql = "UPDATE membertb SET fcm_token = :token WHERE memberid = :mid";
                $stmt = $this->db->prepare($sql);

                // Use empty string or NULL if token is empty
                $tokenVal = !empty($token) ? $token : null;

                $res = $stmt->execute([':token' => $tokenVal, ':mid' => $memberid]);
                file_put_contents($logFile, date("Y-m-d H:i:s") . " SQL EXEC Result: " . ($res ? "OK" : "FAIL") . " | RowCount: " . $stmt->rowCount() . "\n", FILE_APPEND);

                if ($res) {
                    $response->getBody()->write(json_encode(['status' => 'success', 'updated_id' => $memberid]));
                } else {
                    $response->getBody()->write(json_encode(['status' => 'fail', 'message' => 'database error']));
                }
            } else {
                $response->getBody()->write(json_encode(['status' => 'fail', 'message' => 'missing memberid', 'received' => $body]));
            }
        } catch (\Throwable $e) {
            $response->getBody()->write(json_encode(['status' => 'error', 'message' => $e->getMessage()]));
        }

        return $response->withHeader('Content-Type', 'application/json');
    }
}