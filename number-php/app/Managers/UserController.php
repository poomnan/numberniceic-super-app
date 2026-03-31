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
        $strColor = array();
        $dayListStr = (string) ($request->getAttribute('days') ?? '');
        // Allow only expected color IDs (0-9) to avoid malformed inputs.
        $numbDays = preg_replace('/[^0-9]/', '', $dayListStr);

        $fetchById = function ($id) {
            $sql = "SELECT * FROM colortb WHERE colorid = ?";
            $result = $this->db->prepare($sql);
            if (!$result) {
                return null;
            }
            $result->execute([$id]);
            $row = $result->fetch(\PDO::FETCH_ASSOC);
            return is_array($row) ? $row : null;
        };

        // 1) Fetch requested IDs first (preserve order)
        for ($i = 0; $i < strlen($numbDays); $i++) {
            $char = $numbDays[$i];
            $row = $fetchById($char);
            if ($row !== null) {
                $strColor[] = $row;
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
        $queryParams = $request->getQueryParams();
        $requestedYear = isset($queryParams['year']) ? (int) $queryParams['year'] : (int) date('Y');
        $requestedMonth = isset($queryParams['month']) ? (int) $queryParams['month'] : (int) date('n');
        $monthRange = \App\Managers\ThaiCalendarHelper::getMonthRange($requestedYear, $requestedMonth);
        $calendarStartDate = $monthRange['start'];
        $endDate = $monthRange['end'];

        $pdo = $this->db;
        $todayStatus = \App\Managers\ThaiCalendarHelper::getAuspiciousStatus($presentDay);
        $wanTongchai = $todayStatus['is_tongchai'] ? "1" : "0";
        $wanAtipbadee = $todayStatus['is_atipbadee'] ? "1" : "0";
        $wanPraStr = $todayStatus['is_wanpra'] ? "1" : "0";

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

        $wanKating = $todayStatus['is_kating'] ? "1" : "0";
        $descParts = [];
        if ($wanPraStr == "1")
            $descParts[] = "วันพระ";
        if ($wanTongchai == "1")
            $descParts[] = "วันธงชัย";
        if ($wanAtipbadee == "1")
            $descParts[] = "วันอธิบดี";

        $wanDesc = !empty($descParts) ? "วันนี้" . implode(", ", $descParts) : "";
        $wanDetail = "";
        $dayId = "1";

        if (is_object($dbSpecial)) {
            $dayId = $dbSpecial->dayid ?? "1";
            // Append special description if it exists and isn't already there
            if (!empty($dbSpecial->wan_desc)) {
                $cleanSpecialDesc = str_replace("วันนี้", "", $dbSpecial->wan_desc);
                if (!empty($cleanSpecialDesc) && !in_array($cleanSpecialDesc, $descParts)) {
                    if (empty($wanDesc)) {
                        $wanDesc = "วันนี้" . $cleanSpecialDesc;
                    } else {
                        $wanDesc .= ", " . $cleanSpecialDesc;
                    }
                }
            }
            if (!empty($dbSpecial->wan_detail))
                $wanDetail = $dbSpecial->wan_detail;
        }

        $chokTodayL = $todayStatus;
        
        $WanSpecial = [
            'dayid' => $dayId,
            'wan_date' => $presentDay,
            'wan_desc' => $wanDesc,
            'wan_detail' => $wanDetail,
            'wan_pra' => $wanPraStr,
            'wan_kating' => $wanKating,
            'wan_tongchai' => $wanTongchai,
            'wan_atipbadee' => $wanAtipbadee,
            'wan_sittichok' => (string) ($chokTodayL['is_sittichok'] ? "1" : "0"),
            'wan_mahasittichok' => (string) ($chokTodayL['is_mahasittichok'] ? "1" : "0"),
            'wan_ammarit' => (string) ($chokTodayL['is_ammarit'] ? "1" : "0"),
            'wan_rachachok' => (string) ($chokTodayL['is_rachachok'] ? "1" : "0"),
            'wan_chaichok' => (string) ($chokTodayL['is_chaichok'] ? "1" : "0")
        ];

        $filteredWanpras = [];
        $currentDate = new \DateTime($calendarStartDate);
        $finalDate = new \DateTime($endDate);
        while ($currentDate <= $finalDate) {
            $dateStr = $currentDate->format('Y-m-d');
            $status = \App\Managers\ThaiCalendarHelper::getAuspiciousStatus($dateStr);
            $isKating = !empty($status['is_kating']);

            $filteredWanpras[] = [
                'wanpra_date' => $dateStr,
                'is_wanpra' => $status['is_wanpra'] ? "1" : "0",
                'is_tongchai' => $status['is_tongchai'] ? "1" : "0",
                'is_atipbadee' => $status['is_atipbadee'] ? "1" : "0",
                'is_kating' => $isKating ? "1" : "0",
                'is_loy' => !empty($status['is_loy']),
                'is_fu' => !empty($status['is_fu']),
                'is_jom' => !empty($status['is_jom']),
                'display_tags' => $status['display_tags'] ?? [],
                'display_tags_prioritized' => $status['display_tags_prioritized'] ?? [],
                'calendar_display_tags' => $status['calendar_display_tags'] ?? [],
                'kal_tags' => $status['kal_tags'] ?? [],
                'dithi_tags' => $status['dithi_tags'] ?? [],
                'day_type_tags' => $status['day_type_tags'] ?? [],
                'warning_tags' => $status['warning_tags'] ?? [],
                'tag_details' => $status['tag_details'] ?? [],
                'myhora_display_tags' => $status['myhora_display_tags'] ?? [],
                'myhora_display_tags_prioritized' => $status['myhora_display_tags_prioritized'] ?? [],
                'myhora_tag_details' => $status['myhora_tag_details'] ?? [],
                'mahamodo_display_tags' => $status['mahamodo_display_tags'] ?? [],
                'mahamodo_display_tags_prioritized' => $status['mahamodo_display_tags_prioritized'] ?? [],
                'mahamodo_tag_details' => $status['mahamodo_tag_details'] ?? [],
                'has_positive_tags' => !empty($status['has_positive_tags']),
                'has_negative_tags' => !empty($status['has_negative_tags']),
                'is_conflict_day' => !empty($status['is_conflict_day']),
                'is_sittichok' => $status['is_sittichok'],
                'is_mahasittichok' => $status['is_mahasittichok'],
                'is_ammarit' => $status['is_ammarit'],
                'is_rachachok' => $status['is_rachachok'],
                'is_chaichok' => $status['is_chaichok'],
                'is_riangmon' => $status['is_riangmon'],
                'is_ubath' => $status['is_ubath'],
                'is_lokawinat' => $status['is_lokawinat']
            ];

            $currentDate->modify('+1 day');
        }

        $arrWanpras = $filteredWanpras;


        // Efficiently find the next wanpra once, instead of in a loop
        $nextWanpra = $this->nextWanpra($presentDay);

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

    private function nextWanpra(string $strWanpra, int $lookaheadDays = 120): string
    {
        $cursor = new \DateTimeImmutable($strWanpra);
        $end = $cursor->modify("+{$lookaheadDays} days");

        while ($cursor <= $end) {
            if (\App\Managers\ThaiCalendarHelper::isWanPra($cursor)) {
                return $cursor->format('Y-m-d');
            }
            $cursor = $cursor->modify('+1 day');
        }

        return "";
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

        $status = \App\Managers\ThaiCalendarHelper::getAuspiciousStatus($formatted);
        return [
            'is_wanpra' => $status['is_wanpra'] ? 1 : 0,
            'is_tongchai' => $status['is_tongchai'] ? 1 : 0,
            'is_atipbadee' => $status['is_atipbadee'] ? 1 : 0
        ];
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
            $chokToday = \App\Managers\ThaiCalendarHelper::queryMahaChok($dtFormatted);
            $chokTomorrow = \App\Managers\ThaiCalendarHelper::queryMahaChok($tomorro);

            $wanpraObj = ($statusToday['is_wanpra'] == 1) ? (object) [
                'wanpra_date' => $dtFormatted,
                'is_sittichok' => $chokToday['is_sittichok'],
                'is_mahasittichok' => $chokToday['is_mahasittichok'],
                'is_ammarit' => $chokToday['is_ammarit'],
                'is_rachachok' => $chokToday['is_rachachok'],
                'is_chaichok' => $chokToday['is_chaichok']
            ] : null;

            $data = array(
                'activity' => 'wanpra',
                'tomorrow' => (bool) ($statusTomorrow['is_wanpra'] == 1),
                'wanpra' => $wanpraObj,
                'wan_special' => [
                    'wan_date' => $dtFormatted,
                    'wan_tongchai' => (string) ($chokToday['is_tongchai'] ? "1" : "0"),
                    'wan_atipbadee' => (string) ($chokToday['is_atipbadee'] ? "1" : "0"),
                    'wan_sittichok' => (string) ($chokToday['is_sittichok'] ? "1" : "0"),
                    'wan_mahasittichok' => (string) ($chokToday['is_mahasittichok'] ? "1" : "0"),
                    'wan_ammarit' => (string) ($chokToday['is_ammarit'] ? "1" : "0"),
                    'wan_rachachok' => (string) ($chokToday['is_rachachok'] ? "1" : "0"),
                    'wan_chaichok' => (string) ($chokToday['is_chaichok'] ? "1" : "0"),
                    'wan_riangmon' => (string) ($chokToday['is_riangmon'] ? "1" : "0"),
                    'wan_lokawinat' => (string) ($chokToday['is_lokawinat'] ? "1" : "0")
                ],
                'wan_special_tomorrow' => [
                    'wan_tongchai' => (string) ($chokTomorrow['is_tongchai'] ? "1" : "0"),
                    'wan_atipbadee' => (string) ($chokTomorrow['is_atipbadee'] ? "1" : "0"),
                    'wan_sittichok' => (string) ($chokTomorrow['is_sittichok'] ? "1" : "0"),
                    'wan_mahasittichok' => (string) ($chokTomorrow['is_mahasittichok'] ? "1" : "0"),
                    'wan_ammarit' => (string) ($chokTomorrow['is_ammarit'] ? "1" : "0"),
                    'wan_rachachok' => (string) ($chokTomorrow['is_rachachok'] ? "1" : "0"),
                    'wan_chaichok' => (string) ($chokTomorrow['is_chaichok'] ? "1" : "0")
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
            // Passive Cleanup: Remove old records for past ages
            try {
                $stmtUser = $this->db->prepare("SELECT birthday FROM membertb WHERE memberid = :mid");
                $stmtUser->execute([':mid' => $mid]);
                $u = $stmtUser->fetch(\PDO::FETCH_OBJ);
                if ($u && !empty($u->birthday)) {
                    $manager = new PersonManager();
                    $ages = $manager->age(strtotime($u->birthday), time());
                    $currentAge = $ages['year'] ?? 0;
                    if ($currentAge > 0) {
                        $stmtDel = $this->db->prepare("DELETE FROM bagcolortb WHERE memberid = :mid AND age < :age");
                        $stmtDel->execute([':mid' => $mid, ':age' => $currentAge]);
                    }
                }
            } catch (\Exception $e) {
                // Ignore cleanup errors
            }

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
        $shour = filter_var($body['shour'] ?? '0', FILTER_SANITIZE_STRING);
        $sminute = filter_var($body['sminute'] ?? '0', FILTER_SANITIZE_STRING);
        $sprovince = filter_var($body['sprovince'] ?? '', FILTER_SANITIZE_STRING);
        $sgender = filter_var($body['sgender'] ?? '', FILTER_SANITIZE_STRING);
        $avatar = filter_var($body['avatar'] ?? '10', FILTER_SANITIZE_STRING);
        $realname = filter_var($body['realname'] ?? '', FILTER_SANITIZE_STRING);
        $surname = filter_var($body['surname'] ?? '', FILTER_SANITIZE_STRING);
        $address = filter_var($body['address'] ?? '', FILTER_SANITIZE_STRING);

        // แปลงค่าว่างเป็น 0 สำหรับฟิลด์ตัวเลข
        $shour = (empty($shour) || !is_numeric($shour)) ? '0' : $shour;
        $sminute = (empty($sminute) || !is_numeric($sminute)) ? '0' : $sminute;

        $syear_int = (int) $syear;
        $cYear = ($syear_int > 2400) ? $syear_int - 543 : $syear_int;

        $m = (int) $smonth;
        $d = (int) $sday;

        // ตรวจสอบค่าว่างของวันเกิด
        if (empty($smonth) || empty($sday) || empty($syear) || $syear_int == 0) {
            $sBirthday = null;
            $ageyear = 0;
            $agemonth = 0;
            $ageweek = 0;
            $ageday = 0;
        } else {
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
        }

        // Get old age before updating
        $stmtOld = $this->db->prepare("SELECT ageyear FROM membertb WHERE memberid = ?");
        $stmtOld->execute([$memberid]);
        $oldUser = $stmtOld->fetch(\PDO::FETCH_OBJ);
        $oldAge = $oldUser ? (int)$oldUser->ageyear : 0;

        $sql = "UPDATE membertb SET realname='{$realname}', surname='{$surname}', birthday=" . ($sBirthday ? "'{$sBirthday}'" : "NULL") . ", shour='{$shour}', sminute='{$sminute}', ageyear='{$ageyear}', agemonth='{$agemonth}', ageweek='{$ageweek}', ageday='{$ageday}', sprovince='{$sprovince}', sgender='{$sgender}', avatar='{$avatar}', address='{$address}' WHERE memberid = '{$memberid}'";

        $result = $this->db->prepare($sql);
        if ($result->execute()) {
            // --- AGE RESET LOGIC (New Year/อายุย่าง) ---
            // If the user's age has increased, reset assignments like a new registration
            if ($oldAge > 0 && $ageyear > $oldAge) {
                try {
                    // Reset annual items
                    $this->db->prepare("DELETE FROM bagcolortb WHERE memberid = ?")->execute([$memberid]);
                    $this->db->prepare("DELETE FROM user_merit_assign WHERE memberid = ?")->execute([$memberid]);
                    $this->db->prepare("DELETE FROM user_temple_assign WHERE memberid = ?")->execute([$memberid]);
                    // NOTE: EXCLUDING (พระพุทธรูป, วันมงคล, วันอัปมงคล) which are For Life.
                    // DO NOT DELETE: user_buddha_assign, user_auspicious_assign, user_inauspicious_assign  
                } catch (\Exception $e) {
                    error_log("Age Reset Error: " . $e->getMessage());
                }
            }

            // Fetch updated user data to return to the app
            $sqlUser = "SELECT * FROM membertb WHERE memberid = '{$memberid}'";
            $stmtUser = $this->db->prepare($sqlUser);
            $stmtUser->execute();
            $updatedUser = $stmtUser->fetch(\PDO::FETCH_OBJ);

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
        $data = $result->fetchAll(\PDO::FETCH_OBJ);

        if (count($data) > 0) {
            $user = $data[0];
            $rengyamAccess = array(
                'granted' => false,
                'viptype' => null,
                'codename' => null,
                'dateadd' => null,
                'expire_at' => null,
                'expired' => false
            );

            if (!empty($user->memberid)) {
                $sqlAccess = "SELECT mu.viptype, mu.codename, mu.dateadd, sc.codetype AS secret_codetype
                              FROM memberuse mu
                              LEFT JOIN secretcode sc ON sc.codename = mu.codename
                              WHERE mu.memberid = :mid
                                AND (
                                  mu.viptype IN ('rengyam_yearly', 'rengyam_vip')
                                  OR sc.codetype IN ('rengyam_yearly', 'rengyam_vip')
                                )
                              ORDER BY mu.memuseid DESC
                              LIMIT 1";
                $resultAccess = $this->db->prepare($sqlAccess);
                $resultAccess->execute([':mid' => $user->memberid]);
                $access = $resultAccess->fetch(\PDO::FETCH_ASSOC);

                if ($access) {
                    $effectiveViptype = $access['viptype'] ?? null;
                    if (!in_array($effectiveViptype, ['rengyam_yearly', 'rengyam_vip'], true)) {
                        $effectiveViptype = $access['secret_codetype'] ?? null;
                    }

                    $expireAt = null;
                    $expired = false;
                    if (!empty($access['dateadd'])) {
                        try {
                            $rawDate = trim((string) $access['dateadd']);
                            $startedAt =
                                \DateTime::createFromFormat('Y-m-d', $rawDate) ?:
                                \DateTime::createFromFormat('Y-F-j', $rawDate) ?:
                                new \DateTime($rawDate);
                            if ($effectiveViptype === 'rengyam_yearly') {
                                $startedAt->modify('+1 year');
                            } else {
                                $startedAt->modify('+50 years');
                            }
                            $expireAt = $startedAt->format('Y-m-d');
                            $expired = $startedAt < new \DateTime('today');
                        } catch (\Exception $inner) {
                            $expireAt = null;
                            $expired = false;
                        }
                    }

                    $rengyamAccess = array(
                        'granted' => !$expired,
                        'viptype' => $effectiveViptype,
                        'codename' => $access['codename'] ?? null,
                        'dateadd' => $access['dateadd'] ?? null,
                        'expire_at' => $expireAt,
                        'expired' => $expired
                    );
                }
            }

            $resData = array(
                'serverx' => array('activity' => 'userlogin', 'message' => 'success'),
                'userx' => $user,
                'rengyam_access' => $rengyamAccess
            );
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
        $datef = date('Y-m-d');

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
        $resData = array('member' => 'fail', 'vipcode' => null);
        if ($code != '') {
            $sql = "SELECT * FROM vipcode WHERE vipcode = :code LIMIT 1";
            $result = $this->db->prepare($sql);
            $result->execute([':code' => $code]);
            $object = $result->fetch(\PDO::FETCH_OBJ);
            if (is_object($object)) {
                $resData = array(
                    'member' => 'vip',
                    'vipcode' => array(
                        'vipid' => $object->vipid,
                        'vipcode' => $object->vipcode,
                        'userdetial' => $object->userdetial,
                        'viptype' => $object->viptype,
                        'vipstatus' => $object->vipstatus
                    )
                );
            }
        }
        $response->getBody()->write(json_encode($resData));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function vipCode($request, $response)
    {
        $code = $request->getAttribute('vipcode');
        $resData = array('member' => 'fail', 'vipcode' => null, 'message' => 'code not found or already used');

        if ($code != '') {
            $sql = "SELECT * FROM vipcode WHERE vipcode = :code LIMIT 1";
            $result = $this->db->prepare($sql);
            $result->execute([':code' => $code]);
            $object = $result->fetch(\PDO::FETCH_OBJ);

            if (is_object($object)) {
                $resData = array(
                    'member' => 'vip',
                    'vipcode' => array(
                        'vipid' => $object->vipid,
                        'vipcode' => $object->vipcode,
                        'userdetial' => $object->userdetial,
                        'viptype' => $object->viptype,
                        'vipstatus' => $object->vipstatus
                    )
                );
                $response->getBody()->write(json_encode($resData));
                return $response->withHeader('Content-Type', 'application/json');
            }

            // Fallback: one-time admin secret codes
            $sql2 = "SELECT * FROM secretcode WHERE codename = :code AND codestatus = 'active' LIMIT 1";
            $result2 = $this->db->prepare($sql2);
            $result2->execute([':code' => $code]);
            $object2 = $result2->fetch(\PDO::FETCH_OBJ);
            if (is_object($object2)) {
                $updateSql = "UPDATE secretcode SET codestatus = 'used' WHERE codename = :code";
                $updateResult = $this->db->prepare($updateSql);
                $updateResult->execute([':code' => $code]);

                $resData = array(
                    'member' => 'vip',
                    'vipcode' => array(
                        'vipid' => $object2->codeid,
                        'vipcode' => $object2->codename,
                        'userdetial' => 'Admin Secret Code',
                        'viptype' => $object2->codetype,
                        'vipstatus' => 'used'
                    )
                );
            }
        }

        $response->getBody()->write(json_encode($resData));
        return $response->withHeader('Content-Type', 'application/json');
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
