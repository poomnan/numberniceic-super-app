<?php

namespace App\Managers;

class HomeController extends Manager
{

    private $apiHome = array();
    private $homeId;
    private $pairsA = array();
    private $pairsB = array();
    private $sumNumber;
    private $pairUnique = array();
    private $pairMiracle = array();
    private $scoreRD = array();
    private $continueDR = array();

    private $pairConDuo = 0;


    public function main($request, $response, $args)
    {

        if ($args['homeid'] != null) {
            // if ($request->getAttribute('homeid') != null) {

            $this->setHomeId($request, $args);
            //$this->setPairsA($this->homeId);
            $this->setPairsAV2($this->homeId);
            $this->setPairsBV2($this->homeId);
            //$this->setPairsB($this->homeId);
            $this->setSum($this->homeId);
            $this->setPairUnique($this->sumNumber, $this->pairsA, $this->pairsB);
            $this->setPairMiracle($this->pairsA, $this->pairsB, $this->sumNumber);
            $this->setContinueDR($this->pairsA, $this->pairsB, $this->pairMiracle);
            $this->setPairConDuo($this->continueDR);
            $this->setScoreRD($this->pairsA, $this->pairsB, $this->sumNumber, $this->pairMiracle);


        }


        $scoreTotal = $this->scoreByPosition($this->pairsA, $this->pairsB, $this->sumNumber);


        $this->apiHome = array(
            'scoreV2' => $scoreTotal,
            'scoreRD' => $this->scoreRD,
            'homeId' => $this->homeId,
            'pairsA' => $this->pairsA,
            'pairsB' => $this->pairsB,

            'continueDR' => $this->continueDR,
            'pairConDuo' => $this->pairConDuo,

            'pairSumNumber' => (string) $this->sumNumber,
            'pairUnique' => $this->pairUnique,
            'homeReport' => $this->getMiracleSummary($scoreTotal['scoreD'], $scoreTotal['scoreR']),
            'pairMiracle' => $this->pairMiracle,
        );

        $response->getBody()->write(json_encode($this->apiHome));
        return $response->withHeader('Content-Type', 'application/json');

    }



    private function getMiracleSummary($scoreTotalD, $scoreTotalR): array
    {

        $scoreD = $scoreTotalD;

        if ($scoreD >= 800) {
            $miracleD = "บ้านเลขที่นี้ดี = " . number_format($scoreD) . " คะแนน บ้านเลขที่ดีมาก บ้านเลขที่มหามงคลหายากเสริมดวงดีแท้ ***แต่ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนบ้านเลขที่ทันที";
        } elseif ($scoreD >= 724) {
            $miracleD = "บ้านเลขที่นี้ดี = " . number_format($scoreD) . " คะแนน บ้านเลขที่ดีมาก บ้านเลขที่มหามงคลหายากเสริมดวงดีแท้ ***แต่ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนบ้านเลขที่ทันที";
        } elseif ($scoreD >= 554) {
            $miracleD = "บ้านเลขที่นี้ดี = " . number_format($scoreD) . " คะแนน ดีเป็นบ้านเลขที่ มงคล แต่ควรดูความหมายเลขและเลขผลร้ายประกอบให้ดี ***ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนบ้านเลขที่ทันที";
        } elseif ($scoreD >= 440) {
            $miracleD = "บ้านเลขที่นี้ดี = " . number_format($scoreD) . " คะแนน ดี แต่เสี่ยง ควรดูความหมายตัวเลขและคะแนนร้ายประกอบให้ดี ***ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนบ้านเลขที่ทันที";
        } elseif ($scoreD >= 350) {
            $miracleD = "บ้านเลขที่นี้ดี = " . number_format($scoreD) . " คะแนน ปานกลาง เสี่ยงมาก ***ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนบ้านเลขที่ทันที";
        } elseif ($scoreD >= 250) {
            $miracleD = "บ้านเลขที่นี้ดี = " . number_format($scoreD) . " คะแนน พอใช้ เสี่ยงมาก ***ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนบ้านเลขที่ทันที";
        } else {
            $miracleD = "อันตรายมากๆ ควรสวดมนต์ทำบุญบ่อยๆและควรรีบเปลี่ยนบ้านเลขที่";
        }


        $scoreR = $scoreTotalR;
        if ($scoreR >= 400) {
            $miracleR = "บ้านเลขที่นี้ร้าย = " . number_format($scoreR) . " คะแนน อันตรายมาก! หายนะรออยู่เบื้องหน้า ควรเปลี่ยนบ้านเลขที่ทันที";
        } elseif ($scoreR >= 350) {
            $miracleR = "บ้านเลขที่นี้ร้าย = " . number_format($scoreR) . " คะแนน อันตรายมาก! ควรเปลี่ยนบ้านเลขที่ทันที";
        } elseif ($scoreR >= 200) {
            $miracleR = "บ้านเลขที่นี้ร้าย = " . number_format($scoreR) . " คะแนน อันตราย! เสี่ยงมาก! จงระวังอย่าประมาท ต้องรอบคอบหมั่นทำบุญ";
        } elseif ($scoreR >= 150) {
            $miracleR = "บ้านเลขที่นี้ร้าย = " . number_format($scoreR) . " คะแนน อันตราย! เสี่ยง! อย่าประมาท ทำบุญเสริมจะดีไม่ตก";
        } elseif ($scoreR >= 100) {
            $miracleR = "บ้านเลขที่นี้ร้าย = " . number_format($scoreR) . " คะแนน สมดุลชีวิตทำบุญเสริมจะได้ดีราบรื่นตลอด";
        } elseif ($scoreR >= 20) {
            $miracleR = "บ้านเลขที่นี้ร้าย = " . number_format($scoreR) . " คะแนน ไม่มีอิทธิพล ทำบุญเสริมราบรื่นตลอด";
        } else {
            $miracleR = "บ้านเลขที่นี้ร้าย = " . number_format($scoreR) . " คะแนน ไม่มีอิทธิพล ทำบุญเสริมราบรื่นตลอด";
        }

        return array('miracleD' => $miracleD, 'miracleR' => $miracleR);

    }


    private function scoreByPosition($pairsA, $pairsB, $pairSum): array
    {
        $sql = "SELECT * FROM numbers ORDER BY pairnumberid ASC";
        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);

        $countPairsA = count($pairsA);
        $countPairsB = count($pairsB);

        $countAB = $countPairsA + $countPairsB;

        $scoreD = 0;
        $scoreR = 0;

        switch ($countAB) {
            case 5:
                $pointA0 = 15;
                $pointA1 = 20;
                $pointA2 = 20;
                $pointB0 = 10;
                $pointB1 = 10;
                $pointSum = 25;


                foreach ($data as $pairX) {
                    if ($pairX->pairnumber == $pairSum) {

                        if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                            $scoreD += $pointSum;
                        }
                        if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                            $scoreR += $pointSum;
                        }

                        //echo "scorePsum : " . $pointSum . '<br>';

                        break;
                    }
                }


                foreach ($pairsA as $key => $pair) {
                    foreach ($data as $pairX) {
                        if ($pairX->pairnumber == $pair) {

                            switch ($key) {
                                case 0:
                                    if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                                        $scoreD += $pointA0;
                                    }

                                    if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                                        $scoreR += $pointA0;
                                    }

                                    //echo 'scoreA0 : ' . $pointA0 . '<br>';

                                    break;
                                case 1:
                                    if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                                        $scoreD += $pointA1;
                                    }

                                    if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                                        $scoreR += $pointA1;
                                    }

                                    //echo 'scoreA1 : ' . $pointA1 . '<br>';
                                    break;
                                case 2:
                                    if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                                        $scoreD += $pointA2;
                                    }

                                    if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                                        $scoreR += $pointA2;
                                    }
                                    //echo 'scoreA2 : ' . $pointA2 . '<br>';
                                    break;
                            }

                            break;
                        }
                    }
                }

                foreach ($pairsB as $key => $pair) {
                    foreach ($data as $pairX) {
                        if ($pairX->pairnumber == $pair) {
                            switch ($key) {
                                case 0:
                                    if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                                        $scoreD += $pointB0;
                                    }

                                    if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                                        $scoreR += $pointB0;
                                    }
                                    //echo 'scoreB0 : ' . $pointB0 . '<br>';
                                    break;
                                case 1:
                                    if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                                        $scoreD += $pointB1;
                                    }

                                    if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                                        $scoreR += $pointB1;
                                    }

                                    //echo 'scoreB1 : ' . $pointB1 . '<br>';
                                    break;

                            }

                            break;
                        }
                    }
                }

                break;
            case 3:
                $pointA0 = 20;
                $pointA1 = 30;

                $pointB0 = 15;

                $pointSum = 35;


                foreach ($data as $pairX) {
                    if ($pairX->pairnumber == $pairSum) {

                        if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                            $scoreD += $pointSum;
                        }
                        if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                            $scoreR += $pointSum;
                        }

                        //echo "scorePsum : " . $pointSum . '<br>';

                        break;
                    }
                }


                foreach ($pairsA as $key => $pair) {
                    foreach ($data as $pairX) {
                        if ($pairX->pairnumber == $pair) {

                            switch ($key) {
                                case 0:
                                    if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                                        $scoreD += $pointA0;
                                    }

                                    if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                                        $scoreR += $pointA0;
                                    }

                                    //echo 'scoreA0 : ' . $pointA0 . '<br>';

                                    break;
                                case 1:
                                    if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                                        $scoreD += $pointA1;
                                    }

                                    if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                                        $scoreR += $pointA1;
                                    }

                                    //echo 'scoreA1 : ' . $pointA1 . '<br>';
                                    break;

                            }

                            break;
                        }
                    }
                }

                foreach ($pairsB as $key => $pair) {
                    foreach ($data as $pairX) {
                        if ($pairX->pairnumber == $pair) {
                            switch ($key) {
                                case 0:
                                    if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                                        $scoreD += $pointB0;
                                    }

                                    if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                                        $scoreR += $pointB0;
                                    }
                                    //echo 'scoreB0 : ' . $pointB0 . '<br>';
                                    break;


                            }

                            break;
                        }
                    }
                }

                break;
            case 1:
                $pointA0 = 50;
                $pointSum = 50;


                foreach ($data as $pairX) {
                    if ($pairX->pairnumber == $pairSum) {

                        if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                            $scoreD += $pointSum;
                        }
                        if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                            $scoreR += $pointSum;
                        }

                        //echo "scorePsum : " . $pointSum . '<br>';

                        break;
                    }
                }


                foreach ($pairsA as $key => $pair) {
                    foreach ($data as $pairX) {
                        if ($pairX->pairnumber == $pair) {

                            switch ($key) {
                                case 0:
                                    if (mb_substr($pairX->pairtype, 0, 1) == 'D') {
                                        $scoreD += $pointA0;
                                    }

                                    if (mb_substr($pairX->pairtype, 0, 1) == 'R') {
                                        $scoreR += $pointA0;
                                    }

                                    //echo 'scoreA0 : ' . $pointA0 . '<br>';

                                    break;

                            }

                            break;
                        }
                    }
                }

                break;
        }

        return array('scoreD' => $scoreD * 10, 'scoreR' => $scoreR * 10);

    }

    private function setHomeId($request, $args)
    {
        $this->homeId = trim($args['homeid']);
        // $this->homeId = trim($request->getAttribute('homeid'));
    }

    private function setPairsA($homeId)
    {
        $countL = strlen($homeId);
        $pairAarr = array();


        if ($countL % 2 == 0) {
            $n = 2;
            for ($i = 0; $i < $countL / 2; $i++) {
                $pair = substr($homeId, $countL - $n, 2);
                array_push($pairAarr, $pair);
                $n += 2;
            }
        }

        if ($countL % 2 == 1) {
            $n = 2;

            for ($i = 0; $i < floor($countL / 2); $i++) {
                $pair = substr($homeId, $countL - $n, 2);
                array_push($pairAarr, $pair);
                $n += 2;
            }
            array_push($pairAarr, substr($homeId, 0, 1));

        }
        $this->pairsA = array_reverse($pairAarr);
    }

    private function setPairsAV2($homeId)
    {
        $pairsA = array();
        $n = 0;
        for ($i = 0; $i < ceil(mb_strlen($homeId) / 2); $i++) {
            $pair = substr($homeId, $n, 2);
            if ($pair != 0 && mb_strlen($pair) % 2 == 0) {
                array_push($pairsA, $pair);
            } else {
                array_push($pairsA, mb_substr($homeId, $n - 1, 2));
            }

            $n += 2;
        }

        $this->pairsA = $pairsA;
    }

    private function setPairsBV2($homeId)
    {
        $pairsB = array();
        $n = 1;
        for ($i = 0; $i < ceil(mb_strlen($homeId) / 2); $i++) {
            $pair = substr($homeId, $n, 2);
            if ($pair != 0 && mb_strlen($pair) % 2 == 0) {

                if (!in_array($pair, $this->pairsA)) {
                    array_push($pairsB, $pair);
                }

            } else {
                break;
            }

            $n += 2;
        }

        $this->pairsB = $pairsB;
    }


    private function setPairsB($homeId)
    {
        $countL = strlen($homeId);
        $pairBarr = array();


        if ($countL % 2 == 0) {

            array_push($pairBarr, substr($homeId, $countL - 1, 1));

            $n = 3;
            for ($i = 0; $i < ($countL / 2) - 1; $i++) {

                $pair = substr($homeId, $countL - $n, 2);
                array_push($pairBarr, $pair);
                $n += 2;
            }

            array_push($pairBarr, substr($homeId, 0, 1));


        }

        if ($countL % 2 == 1) {
            $n = 3;

            for ($i = 0; $i < floor($countL / 2); $i++) {
                $pair = substr($homeId, $countL - $n, 2);
                array_push($pairBarr, $pair);
                $n += 2;
            }


        }


        $this->pairsB = array_reverse($pairBarr);
    }

    private function setSum($homeId)
    {
        $n = 0;
        for ($i = 0; $i < strlen($homeId); $i++) {
            $mChar = (int) substr($homeId, $i, 1);

            $n += $mChar;
        }

        $this->sumNumber = $n;
    }

    private function setPairUnique($sumNumber, $pairsA, $pairsB)
    {
        $allPairsArr = array();
        $pairsOut = array();

        foreach ($pairsA as $value) {
            array_push($allPairsArr, $value);
        }
        foreach ($pairsB as $value) {
            array_push($allPairsArr, $value);
        }

        array_push($allPairsArr, (string) $sumNumber);


        foreach (array_count_values($allPairsArr) as $key => $value) {
            array_push($pairsOut, (string) $key);
        }


        $this->pairUnique = $pairsOut;

    }

    private function setPairMiracle($pairsA, $pairsB, $pairSum)
    {

        $pairsD = array();
        $pairsR = array();
        $pairsAll = array();
        $pairMiracleD = array();
        $pairMiracleR = array();


        $pairMiracle = array();

        $sql = "SELECT * FROM numbers ORDER BY pairnumberid ASC";
        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);


        foreach ($data as $v) {
            foreach ($pairsA as $value) {
                if ($v->pairnumber === $value && $v->pairtype[0] === "D") {
                    array_push($pairsD, $value);
                    array_push($pairsAll, $value);
                }

                if ($v->pairnumber === $value && $v->pairtype[0] === "R") {
                    array_push($pairsR, $value);
                    array_push($pairsAll, $value);
                }


            }

            foreach ($pairsB as $value) {
                if ($v->pairnumber === $value && $v->pairtype[0] === "D") {
                    array_push($pairsD, $value);
                    array_push($pairsAll, $value);
                }

                if ($v->pairnumber === $value && $v->pairtype[0] === "R") {
                    array_push($pairsR, $value);
                    array_push($pairsAll, $value);
                }

            }


            if ($v->pairnumber === (string) $pairSum && $v->pairtype[0] === "D") {

                array_push($pairsD, (string) $pairSum);
                array_push($pairsAll, (string) $pairSum);
            }

            if ($v->pairnumber === (string) $pairSum && $v->pairtype[0] === "R") {
                array_push($pairsR, (string) $pairSum);
                array_push($pairsAll, (string) $pairSum);
            }

        }


        foreach ($data as $value) {
            foreach (array_count_values($pairsAll) as $keyNum => $num) {

                if ($value->pairnumber === (string) $keyNum) {
                    if ($value->pairtype[0] === 'D') {
                        array_push(
                            $pairMiracleD,
                            array(
                                "pairnumber" => $keyNum,
                                "pairtype" => $value->pairtype,
                                "pairpoint" => $value->pairpoint,
                                "miracledesc" => $value->miracledesc,
                                "miracledetail" => $value->detail_vip
                            )
                        );

                        break;
                    }


                    if ($value->pairtype[0] === 'R') {
                        array_push(
                            $pairMiracleR,
                            array(
                                "pairnumber" => $keyNum,
                                "pairtype" => $value->pairtype,
                                "pairpoint" => $value->pairpoint,
                                "miracledesc" => $value->miracledesc,
                                "miracledetail" => $value->detail_vip
                            )
                        );

                        break;
                    }

                }


            }


        }





        foreach ($pairMiracleD as $v) {
            array_push($pairMiracle, $v);
        }

        foreach ($pairMiracleR as $v) {
            array_push($pairMiracle, $v);
        }

        $this->pairMiracle = $pairMiracle;

    }

    private function setScoreRD(array $pairsA, array $pairsB, $sumNumber, array $pairMiracle)
    {
        $pairsAll = array();
        foreach ($pairsA as $k => $v) {
            array_push($pairsAll, $v);
        }

        foreach ($pairsB as $k => $v) {
            array_push($pairsAll, $v);
        }

        array_push($pairsAll, $sumNumber);


        $scored = 0;
        $scorer = 0;

        foreach (array_count_values($pairsAll) as $k => $value) {

            foreach ($pairMiracle as $key => $v) {
                if ((string) $k === (string) $v['pairnumber']) {
                    if ((string) $v['pairtype'][0] === 'D') {
                        $scored = $scored + $v['pairpoint'];
                    }

                    if ((string) $v['pairtype'][0] === 'R') {
                        $scorer = $scorer + $v['pairpoint'];
                    }
                }
            }
        }


        $this->scoreRD = array('scoreD' => $scored, 'scoreR' => $scorer);

    }

    private function setContinueDR(array $pairsA, array $pairsB, array $pairMiracle)
    {

        $conA = 0;
        $conB = 0;

        $statusDa = 0;
        $statusDb = 0;


        foreach (array_reverse($pairsA) as $value) {

            if ($statusDa == 0) {

                foreach ($pairMiracle as $v) {

                    if ($value == $v['pairnumber']) {


                        if ((string) $v['pairtype'][0] == (string) 'D') {

                            $conA++;
                            break;
                        }

                        if ((string) $v['pairtype'][0] == (string) 'R') {
                            $statusDa = 1;
                            break;
                        }
                    }
                }
            }

        }


        foreach (array_reverse($pairsB) as $value) {

            if ($statusDb == 0) {

                foreach ($pairMiracle as $v) {

                    if ($value == $v['pairnumber']) {


                        if ((string) $v['pairtype'][0] == (string) 'D') {

                            $conB++;
                            break;
                        }

                        if ((string) $v['pairtype'][0] == (string) 'R') {
                            $statusDb = 1;
                            break;
                        }
                    }
                }
            }

        }

        $this->continueDR = array('conA' => $conA, 'conB' => $conB);
    }

    private function setPairConDuo(array $continueDR)
    {


        if ($continueDR['conA'] > 0 && $continueDR['conB'] > 0) {

            $this->pairConDuo = 2;
        }

    }


}