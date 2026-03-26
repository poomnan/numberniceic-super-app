<?php

namespace App\Managers;

class PhoneController extends Manager
{

    private $phoneNumber = '';

    private $pairsPhoneA = array();
    private $pairsPhoneB = array();
    private $pairSum = '';

    private $pairsUnique = array();
    private $pairsCount = array();
    private $pairMiracle = array();

    private $objPairSum = array();
    private $objPairsA = array();
    private $objPairsB = array();

    private $scoreLastPairA = array();
    private $scoreContinueA = array();
    private $scoreContinueB = array();

    private $scoreTotal = array();
    private $percentPosition = array();

    private $miracleSortPercent = array();

    private $summaryMiracleScore = array();


    private $apiLawOfNumber = array();


    public function main($request, $response): void
    {

        if ($this->checkPhoneNumber($request)) {

            $this->setPairsNumber($this->phoneNumber);
            $this->setPairSum($this->phoneNumber);
            $this->setPairsUnique($this->pairsPhoneA, $this->pairsPhoneB, $this->pairSum);
            $this->setPairsCount($this->pairsPhoneA, $this->pairsPhoneB, $this->pairSum);

            $this->setPairMiracle($this->pairsUnique);
            $this->setObjPairSum($this->pairMiracle, $this->pairSum);
            $this->setObjPairsA($this->pairMiracle, $this->pairsPhoneA);
            $this->setObjPairsB($this->pairMiracle, $this->pairsPhoneB);

            $this->setScoreLastPairA($this->objPairsA);
            $this->setScoreContinueA($this->objPairsA);
            $this->setScoreContinueB($this->objPairsB);

            $this->setScoreTotal($this->scoreContinueA, $this->scoreContinueA, $this->scoreLastPairA, $this->objPairsA, $this->objPairsB, $this->objPairSum);
            $this->setPercentPosition($this->objPairsA, $this->objPairsB, $this->objPairSum);

            $this->setMiracleSortPercent($this->pairMiracle, $this->objPairsA, $this->objPairsB, $this->objPairSum);

            $this->setSummaryMiracleScore($this->scoreTotal);

            $this->setApiLawOfNumber($this->scoreTotal, $this->percentPosition, $this->summaryMiracleScore, $this->scoreLastPairA,
                $this->scoreContinueA, $this->scoreContinueB, $this->objPairsA, $this->objPairsB, $this->objPairSum, $this->miracleSortPercent);


            echo json_encode($this->apiLawOfNumber);


        } else {
            echo 'ERROR PHONE NUMBER!!!';
        }


    }


    private function setApiLawOfNumber($scoreTotal, $percentPosition, $summaryMiracleScore, $scoreLastPairA, $scoreContinueA, $scoreContinueB, $objPairsA, $objPairsB, $objPairSum, $miracleSortPercent)
    {


        $this->apiLawOfNumber = array(

            'scoreTotal' => $scoreTotal,
            'percentPosition' => $percentPosition,
            'summaryMiracleScore' => $summaryMiracleScore,

            'scoreLastPairA' => $scoreLastPairA,
            'scoreContinueA' => $scoreContinueA,
            'scoreContinueB' => $scoreContinueB,
            'pairsA' => $objPairsA,
            'pairsB' => $objPairsB,
            'pairSum' => $objPairSum,
            'pairMiracle' => $miracleSortPercent

        );
    }


    private function setSummaryMiracleScore($scoreTotal)
    {

        if ($scoreTotal['totalScoreD'] >= 540) {
            array_push($this->summaryMiracleScore, array('grade' => 'A+', 'miracle' => 'เบอร์นี้ดีเยี่ยม VIP ผลร้ายไม่มีอิทธิผล มีเลขดีที่มีอิทธิผลต่อชีวิตสูง และมีความแม่นยำสูง ใช้แล้วเจริญรุ่งเรื่องดีนักแล', 'scoreTotal' => $scoreTotal['totalScoreD']));
        } else if ($scoreTotal['totalScoreD'] >= 924) {
            array_push($this->summaryMiracleScore, array('grade' => 'B', 'miracle' => 'เบอร์นี้ดีเยี่ยม ผลร้ายไม่มีอิทธิผล มีเลขดีที่มีอิทธิผลต่อชีวิตสูง ใช้แล้วชีวิตรุ่งเรืองดีมาก', 'scoreTotal' => $scoreTotal['totalScoreD']));
        } else if ($scoreTotal['totalScoreD'] >= 700) {
            array_push($this->summaryMiracleScore, array('grade' => 'C', 'miracle' => 'เบอร์นี้ดี ผลร้ายมีอิทธิผลน้อย มีเลขดีที่มีอิทธิผลต่อชีวิตสูง', 'scoreTotal' => $scoreTotal['totalScoreD']));
        } else if ($scoreTotal['totalScoreD'] >= 654) {
            array_push($this->summaryMiracleScore, array('grade' => 'D', 'miracle' => 'เบอร์นี้มีผลดีต่อชีวิตระดับปานกลาง มีเลขดีที่ส่งผลระดับปานกลาง', 'scoreTotal' => $scoreTotal['totalScoreD']));
        } else if ($scoreTotal['totalScoreD'] >= 540) {
            array_push($this->summaryMiracleScore, array('grade' => 'D-', 'miracle' => 'พอใช้ ขอให้ดูเลขร้ายประกอบ', 'scoreTotal' => $scoreTotal['totalScore']));
        } else {
            array_push($this->summaryMiracleScore, array('grade' => 'F', 'miracle' => 'อันตราย ควรเปลี่ยนเบอร์', 'scoreTotal' => $scoreTotal['totalScoreD']));
        }


        if ($scoreTotal['totalScoreR'] <= -170) {
            array_push($this->summaryMiracleScore, array('grade' => 'xA+', 'miracle' => 'อันตราย ควรเปลี่ยนเบอร์ทันที', 'scoreTotalR' => $scoreTotal['totalScoreR']));
        } else if ($scoreTotal['totalScoreR'] <= -130) {
            array_push($this->summaryMiracleScore, array('grade' => 'xB', 'miracle' => 'อันตราย ระวังให้มากต้องรอบคอบหมั่นทำบุญ', 'scoreTotal' => $scoreTotal['totalScoreR']));
        } else if ($scoreTotal['totalScoreR'] <= -100) {
            array_push($this->summaryMiracleScore, array('grade' => 'xC', 'miracle' => 'อย่าประมาท ทำบุญเสริมบ้างจะดีไม่ตก', 'scoreTotal' => $scoreTotal['totalScoreR']));
        } else if ($scoreTotal['totalScoreR'] <= -40) {
            array_push($this->summaryMiracleScore, array('grade' => 'xD', 'miracle' => 'สมดุลชีวิตทำบุญเสริมราบรื่นตลอด', 'scoreTotal' => $scoreTotal['totalScoreR']));
        } else if ($scoreTotal['totalScoreR'] <= -30) {
            array_push($this->summaryMiracleScore, array('grade' => 'xD-', 'miracle' => 'ไม่มีอิทธิพล ทำบุญเสริมราบรื่นตลอด', 'scoreTotal' => $scoreTotal['totalScoreR']));
        } else {
            array_push($this->summaryMiracleScore, array('grade' => 'xF', 'miracle' => 'ไม่มีอิทธิพล ทำบุญเสริมราบรื่นตลอด', 'scoreTotal' => $scoreTotal['totalScoreR']));
        }


    }


    private function setMiracleSortPercent($pairMiracle, $objPairsA, $objPairsB, $objPairSum)
    {


        foreach ($pairMiracle as $k => $value) {
            $sumPercent = 0;
            foreach ($objPairsA as $v) {
                if ($value['pairnumber'] == $v['pairNumber']) {
                    $sumPercent += $v['percentile'];
                }

            }

            if ($sumPercent != 0) {
                array_push($this->miracleSortPercent,
                    array(
                        'pairNumber' => $value['pairnumber'],
                        'pairType' => $value['pairtype'],
                        'pairPoint' => (int)$value['pairpoint'],
                        'percentile' => $sumPercent,
                        'miracleDesc' => $value['miracledesc'],
                        'miracleDetail' => $value['miracledetail'],
                    ));

            }
        }

        foreach ($pairMiracle as $k => $value) {
            $sumPercent = 0;
            foreach ($objPairsB as $v) {
                if ($value['pairnumber'] == $v['pairNumber']) {
                    $sumPercent += $v['percentile'];
                }

            }

            if ($sumPercent != 0) {
                array_push($this->miracleSortPercent,
                    array(
                        'pairNumber' => $value['pairnumber'],
                        'pairType' => $value['pairtype'],
                        'pairPoint' => (int)$value['pairpoint'],
                        'percentile' => $sumPercent,
                        'miracleDesc' => $value['miracledesc'],
                        'miracleDetail' => $value['miracledetail'],
                    ));
            }
        }

        foreach ($pairMiracle as $k => $value) {
            $sumPercent = 0;
            if ($value['pairnumber'] == $objPairSum['pairNumber']) {
                $sumPercent += $objPairSum['percentile'];
            }

            if ($sumPercent != 0) {
                array_push($this->miracleSortPercent,
                    array(
                        'pairNumber' => $value['pairnumber'],
                        'pairType' => $value['pairtype'],
                        'pairPoint' => (int)$value['pairpoint'],
                        'percentile' => $sumPercent,
                        'miracleDesc' => $value['miracledesc'],
                        'miracleDetail' => $value['miracledetail'],
                    ));
            }

        }


        usort($this->miracleSortPercent, function ($item1, $item2) {
            return $item2['percentile'] <=> $item1['percentile'];
        });


    }


    private function setScoreTotal($scoreContinueA, $scoreContinueB, $scoreLastPairA, $objPairsA, $objPairsB, $objPairSum): void
    {
        $totalScoreD = 0;
        $totalScoreR = 0;

        //var_dump($objPairSum);

        if ($scoreContinueA['score'] >= 0) {
            $totalScoreD += $scoreContinueA['score'];

        } else {
            $totalScoreR += $scoreContinueA['score'];

        }

        if ($scoreContinueB['score'] >= 0) {
            $totalScoreD += $scoreContinueB['score'];

        } else {
            $totalScoreR += $scoreContinueB['score'];

        }

        if ($scoreLastPairA['scoreLastPairA'] >= 0) {
            $totalScoreD += $scoreLastPairA['scoreLastPairA'];
        } else {
            $totalScoreR += $scoreLastPairA['scoreLastPairA'];
        }

        foreach ($objPairsA as $value) {
            if ($value['pairPoint'] >= 0) {
                $totalScoreD += $value['pairPoint'];
            } else {
                $totalScoreR += $value['pairPoint'];
            }
        }

        foreach ($objPairsB as $value) {
            if ($value['pairPoint'] >= 0) {
                $totalScoreD += $value['pairPoint'];
            } else {
                $totalScoreR += $value['pairPoint'];
            }
        }

        if ($objPairSum['pairPoint'] >= 0) {
            $totalScoreD += $objPairSum['pairPoint'];
        } else {
            $totalScoreR += $objPairSum['pairPoint'];
        }


        $this->scoreTotal = array('totalScoreD' => $totalScoreD, 'totalScoreR' => $totalScoreR);

    }


    private function setPercentPosition($objPairsA, $objPairsB, $objPairSum)
    {

        $percentD = 0;
        $percentR = 0;

        foreach ($objPairsA as $value) {
            if ($value['pairType'][0] == 'D') {
                $percentD += $value['percentile'];
            } else {
                $percentR += $value['percentile'];
            }


        }

        foreach ($objPairsB as $value) {
            if ($value['pairType'][0] == 'D') {
                $percentD += $value['percentile'];
            } else {
                $percentR += $value['percentile'];
            }


        }

        if ($objPairSum['pairType'][0] == 'D') {
            $percentD += $objPairSum['percentile'];
        } else {
            $percentR += $objPairSum['percentile'];
        }


        $this->percentPosition = array('percentD' => $percentD, 'percentR' => $percentR);
    }


    private function setScoreContinueB($objPairsB): void
    {

        $n = 0;
        $score = 0;


        if ($objPairsB[3]['pairType'][0] == 'D') {
            for ($i = 3; $i >= 0; $i--) {
                if ($objPairsB[$i]['pairType'][0] == 'D') {
                    $score += $objPairsB[$i]['pairPoint'];
                    $n++;
                } else {
                    break;
                }

            }
        } else {
            for ($i = 3; $i >= 0; $i--) {
                if ($objPairsB[$i]['pairType'][0] == 'R') {
                    $score += $objPairsB[$i]['pairPoint'];
                    $n++;
                } else {
                    break;
                }

            }
        }


        $this->scoreContinueB = array('continue' => $n, 'score' => $score);


    }

    private function setScoreContinueA($objPairsA): void
    {

        $n = 0;
        $score = 0;


        if ($objPairsA[4]['pairType'][0] == 'D') {
            for ($i = 4; $i >= 0; $i--) {
                if ($objPairsA[$i]['pairType'][0] == 'D') {
                    $score += $objPairsA[$i]['pairPoint'];
                    $n++;
                } else {
                    break;
                }

            }
        } else {
            for ($i = 4; $i >= 0; $i--) {
                if ($objPairsA[$i]['pairType'][0] == 'R') {
                    $score += $objPairsA[$i]['pairPoint'];
                    $n++;
                } else {
                    break;
                }

            }
        }


        $this->scoreContinueA = array('continue' => $n, 'score' => $score);


    }

    private function setScoreLastPairA($objPairsA): void
    {
        $this->scoreLastPairA = array('scoreLastPairA' => (int)$objPairsA[4]['pairPoint']);
    }


    private function setObjPairsB($pairMiracle, $pairsPhoneB): void
    {
        $n = 0;
        foreach ($pairsPhoneB as $valueB) {
            foreach ($pairMiracle as $valueM) {

                if ($valueB == $valueM['pairnumber']) {

                    switch ($n) {
                        case 0:

                            array_push($this->objPairsB, array('pairNumber' => $valueM['pairnumber'],
                                'pairType' => $valueM['pairtype'], 'pairPoint' => (int)$valueM['pairpoint'], 'percentile' => 3));
                            break;
                        case 1:

                            array_push($this->objPairsB, array('pairNumber' => $valueM['pairnumber'],
                                'pairType' => $valueM['pairtype'], 'pairPoint' => (int)$valueM['pairpoint'], 'percentile' => 5));
                            break;
                        case 2:

                            array_push($this->objPairsB, array('pairNumber' => $valueM['pairnumber'],
                                'pairType' => $valueM['pairtype'], 'pairPoint' => (int)$valueM['pairpoint'], 'percentile' => 5));
                            break;
                        case 3:

                            array_push($this->objPairsB, array('pairNumber' => $valueM['pairnumber'],
                                'pairType' => $valueM['pairtype'], 'pairPoint' => (int)$valueM['pairpoint'], 'percentile' => 12));
                            break;
                    }


                    break;
                }
            }

            $n++;
        }
    }


    private function setObjPairsA($pairMiracle, $pairsPhoneA): void
    {

        $n = 0;
        foreach ($pairsPhoneA as $valueA) {
            foreach ($pairMiracle as $valueM) {

                if ($valueA == $valueM['pairnumber']) {

                    switch ($n) {
                        case 0:
                            array_push($this->objPairsA, array('pairNumber' => $valueM['pairnumber'],
                                'pairType' => $valueM['pairtype'], 'pairPoint' => (int)$valueM['pairpoint'], 'percentile' => 5));
                            break;
                        case 1:
                            array_push($this->objPairsA, array('pairNumber' => $valueM['pairnumber'],
                                'pairType' => $valueM['pairtype'], 'pairPoint' => (int)$valueM['pairpoint'], 'percentile' => 5));
                            break;
                        case 2:
                            array_push($this->objPairsA, array('pairNumber' => $valueM['pairnumber'],
                                'pairType' => $valueM['pairtype'], 'pairPoint' => (int)$valueM['pairpoint'], 'percentile' => 10));
                            break;
                        case 3:
                            array_push($this->objPairsA, array('pairNumber' => $valueM['pairnumber'],
                                'pairType' => $valueM['pairtype'], 'pairPoint' => (int)$valueM['pairpoint'], 'percentile' => 15));
                            break;
                        case 4:
                            array_push($this->objPairsA, array('pairNumber' => $valueM['pairnumber'],
                                'pairType' => $valueM['pairtype'], 'pairPoint' => (int)$valueM['pairpoint'], 'percentile' => 20));
                            break;
                    }


                    break;
                }
            }

            $n++;
        }
    }


    private function setObjPairSum($pairMiracle, $pairSum): void
    {

        foreach ($pairMiracle as $value) {
            if ($pairSum == $value['pairnumber']) {

                $this->objPairSum = array('pairNumber' => $pairSum, 'pairType' => $value['pairtype'], 'pairPoint' => (int)$value['pairpoint'], 'percentile' => 20);
                break;
            }
        }
    }

    private function setPairMiracle($pairUnique): void
    {


        $sql = "SELECT * FROM numbers ORDER BY pairnumberid ASC";
        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);

        foreach ($data as $value) {
            foreach ($pairUnique as $pair) {

                if ($value->pairnumber === $pair) {
                    array_push($this->pairMiracle,
                        array("pairnumber" => $pair, "pairtype" => $value->pairtype,
                            "pairpoint" => $value->pairpoint, "miracledesc" => $value->miracledesc,
                            "miracledetail" => $value->miracledetail));

                    break;
                }


            }


        }


    }

    private function setPairsUnique(array $pairsPhoneA, array $pairsPhoneB, string $pairSum)
    {

        $unique = array();

        foreach ($pairsPhoneA as $value) {
            array_push($unique, $value);
        }
        foreach ($pairsPhoneB as $value) {
            array_push($unique, $value);
        }
        array_push($unique, $pairSum);

        $this->pairsUnique = array_unique($unique);

    }

    private function setPairsCount(array $pairsPhoneA, array $pairsPhoneB, string $pairSum)
    {
        $allPair = array();

        foreach ($pairsPhoneA as $value) {
            array_push($allPair, $value);
        }
        foreach ($pairsPhoneB as $value) {
            array_push($allPair, $value);
        }

        array_push($allPair, $pairSum);

        $this->pairsCount = array_count_values($allPair);
    }

    private function setPairSum(string $phoneNumber): void
    {
        $charNumber = str_split($phoneNumber, 1);

        $sum = 0;

        foreach ($charNumber as $value) {
            $sum += (int)$value;
        }

        $this->pairSum = (string)$sum;
    }

    private function setPairsNumber(string $phoneNumber): void
    {

        $this->pairsPhoneA = str_split($phoneNumber, 2);

        $pairsB = array();
        array_push($pairsB, substr($phoneNumber, 1, 2));
        array_push($pairsB, substr($phoneNumber, 3, 2));
        array_push($pairsB, substr($phoneNumber, 5, 2));
        array_push($pairsB, substr($phoneNumber, 7, 2));

        $this->pairsPhoneB = $pairsB;


    }


    private function checkPhoneNumber($request): bool
    {
        $phoneNumber = $request->getAttribute('phoneNumber');
        if (is_numeric($phoneNumber) && strlen($phoneNumber) == 10) {
            $this->phoneNumber = $phoneNumber;
            return true;
        }
        return false;

    }

}