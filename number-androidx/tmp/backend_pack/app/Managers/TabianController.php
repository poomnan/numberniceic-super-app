<?php

namespace App\Managers;


class TabianController extends Manager
{


    private $scoreTotalD = 0;
    private $scoreTotalR = 0;

    private $a0Status = false;
    private $a1Status = false;
    private $percentTabian = array();
    private $percentPairMi = 0;
    private $case = '';


    private $pairAngsol = '';
    private $pairAngsolA0 = '';
    private $pairAngsolA1 = '';
    private $pairNumMud = '';


    private $sumPercentMud = array();
    private $sumPercentNum = array();
    private $pairPercentAngSon = array();
    private $pairPercentNumMud = array();

    private $percentByCaseA = array();
    private $percentByCaseB = array();


    private $realNumber = array('1', '2', '3', '4', '5', '6', '7', '8', '9', '0');
    private $firstCharTabian = '';

    private $firsCharStatus = false;
    private $tabianNoNFirstNum = '';

    private $tabianPealNum = '';

    private $txt = array();

    private $apiTabian;
    private $fullCarId = '';

    private $pairsCarA = array();
    private $pairsCarB = array();
    private $pairSum = '';
    private $pairSumNumLast = '';
    private $pairsUnique = array();

    private $pairsMiracle = array();
    private $numLast = '';
    private $carRAW;

    private $percentSiang = 0;

    public function main($request, $response, $args)
    {
        $carid = $args['carid'];
        // $carid = $request->getAttribute('carid');

        $this->carRAW = $carid;

        $this->initSetPairTabianV2($carid);

        $this->initTabianNonFirstNum($this->firsCharStatus, $carid);

        $this->setCharKeyV();
        $this->changeTextToNumber($carid);

        /*$this->setCarPairsA($carid);
        $this->setCarPairsB($carid);*/

        $this->pairsCarA = $this->getPairsA($this->firsCharStatus, $this->tabianPealNum, $this->firstCharTabian);
        $this->pairsCarB = $this->getPairsB($this->firsCharStatus, $this->tabianPealNum, $this->firstCharTabian);

        $this->setPairSum($this->fullCarId);
        $this->setPairsNum($carid);
        $this->setPairSumLastNum($this->numLast);
        $this->setPairsUnique($this->pairsCarA, $this->pairsCarB, $this->pairSum, $this->pairSumNumLast);

        $this->setCasePercentTabian($this->carRAW);

        $this->setPercentByCase($this->case, $this->pairSum, $this->pairSumNumLast, $this->numLast, $this->pairsCarA, $this->pairsCarB);
        $this->setPairsMiracle($this->pairsUnique);


        $this->setScoreTotalDR();

        $scorePairBonus = $this->getScoreBonusByPosition($this->pairsCarA, $this->pairsCarB, $this->pairNumMud, $this->pairSumNumLast, $this->pairsMiracle);
        $scorePrefixNumMud = $this->getScorePrefixNumber($this->case, $this->pairsCarA, $this->pairsMiracle);

        $scorePairPosition0B = $this->getPairPosition0B($this->pairsCarB, $this->pairsMiracle);

        $scoreMiDAngsol = $this->getScoreMiDAngsolMud($this->case, $this->pairsCarA, $this->pairsMiracle);

        $scoreMiDSumNumSecond = $this->getScoreMiDSumNumSecond($this->pairSumNumLast, $this->pairsMiracle);
        $scoreMiDSumMud = $this->getScoreMiDSumMud($this->pairSum, $this->pairsMiracle);

        $scorePairAngsolX = $this->getScoreDAngsolMud($this->carRAW);


        $scoreBonusR1991 = $this->getScoreBonusR1991($this->pairSumNumLast, $this->pairSum);


        $scoreTotalOfTotalD = $this->scoreTotalD + $scorePairBonus['scoreBonusD'] + $scorePrefixNumMud['scoreD'] - $scorePairPosition0B['score0BR'] - $scoreMiDAngsol['scoreAngsolMiD'] - $scoreMiDSumNumSecond - $scoreMiDSumMud + $scorePairAngsolX - $scoreBonusR1991['scorePair1991MiD'];


        $scoreTotalOfTotalR = $this->scoreTotalR + $scorePairBonus['scoreBonusR'] + $scorePrefixNumMud['scoreR'] + $scorePairBonus['scoreBonusR'] + $scoreMiDSumMud + $scoreBonusR1991['scorePair1991AddR'];

        $this->apiTabian = array(
            'carid' => $carid,
            'percentSiang' => $this->percentSiang,
            'scoreBeforeAddBonus' => array('scoreD' => $this->scoreTotalD, 'scoreR' => $this->scoreTotalR),

            'scoreBonusR1991' => $scoreBonusR1991,
            'scoreAngsolBonusex' => $scorePairAngsolX,
            'scoreTotalD' => $scoreTotalOfTotalD,
            'scoreTotalR' => $scoreTotalOfTotalR,

            'scoreBonusPosition' => $scorePairBonus,

            'scorePrefixNumber' => $scorePrefixNumMud,

            'scoreMiDSumNumSecond' => $scoreMiDSumNumSecond,
            'scoreMiDSumMud' => $scoreMiDSumMud,

            'miracleSummary' => $this->getMiracleSummary($scoreTotalOfTotalD, $scoreTotalOfTotalR),
            'sumTotalPercent' => $this->sumOfTotalPercent(),
            'percentTabian' => $this->percentTabian,
            'case' => $this->case,
            'pairPercentAngsol' => $this->pairPercentAngSon,
            'numSecond' => $this->numLast,
            'carPairsA' => $this->pairsCarA,
            'carPairsB' => $this->pairsCarB,
            'carPairSumAll' => $this->pairSum,
            'carPairSumSecond' => $this->pairSumNumLast,
            'carPairUnique' => $this->pairsUnique,
            'pairsMiracle' => $this->pairsMiracle,

        );


        //print_r($this->percentByCaseA);

        $response->getBody()->write(json_encode($this->apiTabian));
        return $response->withHeader('Content-Type', 'application/json');
    }


    private function getScoreBonusR1991($sumLastNum, $sumNumMud): array
    {

        $scorePair1991MiD = 0;
        $scorePair1991AddR = 0;

        if ($sumLastNum == '19' || $sumLastNum == '91' || $sumNumMud == '19' || $sumNumMud == '91') {
            $scorePair1991MiD = 400;
            $scorePair1991AddR = 100;
        }


        return array('scorePair1991MiD' => $scorePair1991MiD, 'scorePair1991AddR' => $scorePair1991AddR);
    }


    private function getScoreDAngsolMud($carId): int
    {

        $allScore = 0;

        $duoAngsol = array('กม', 'กฮ', 'กร', 'กธ');
        $scoreDuo = 100;
        $tripleAngsol = array('4กม', '5กม', '4กธ', '5กธ', '4กฮ', '5กฮ', '4กร', '5กร');
        $scoreTriple = 65;
        $singleAngsol = 'ก';
        $scoreSingle = 65;


        if (mb_substr($carId, 0, 1) == $singleAngsol || mb_substr($carId, 1, 1) == $singleAngsol) {
            $allScore += $scoreSingle;
        }

        foreach ($duoAngsol as $duPair) {
            if ($duPair == mb_substr($carId, 0, 2) || $duPair == mb_substr($carId, 1, 2)) {
                $allScore += $scoreDuo;
            }
        }

        foreach ($tripleAngsol as $triplePair) {
            if ($triplePair == mb_substr($carId, 0, 3)) {
                $allScore += $scoreTriple;
            }
        }

        return $allScore;
    }

    private function getScoreMiDSumNumSecond($sumNumSecond, $pairMiracle): int
    {
        $point = 0;

        foreach ($pairMiracle as $pair) {
            if ($sumNumSecond == $pair['pairnumber']) {
                switch ($pair['pairtype']) {
                    case 'R10':
                        $point = 200;
                        break;
                    case 'R7':
                        $point = 100;
                        break;
                    case 'R5':
                        $point = 50;
                        break;

                }
            }
        }

        return $point;
    }

    private function getScoreMiDSumMud($sumNumMud, $pairMiracle): int
    {
        $point = 0;

        foreach ($pairMiracle as $pair) {
            if ($sumNumMud == $pair['pairnumber']) {
                switch ($pair['pairtype']) {
                    case 'R10':
                        $point = 200;
                        break;
                    case 'R7':
                        $point = 150;
                        break;
                    case 'R5':
                        $point = 50;
                        break;
                }
            }
        }


        return $point;
    }

    private function getScoreMiDAngsolMud($case, $pairsA, $pairMiracle): array
    {

        $pointMiD = 0;
        $pointAddR = 0;

        if ($case == 'tripleChaCase') {
            /*$pair0 = $pairsA[0];
            $pair1 = $pairsA[0] . mb_substr($pairsB[0], 0, 1);
            $pair0Type = '';
            $pair1Type = '';

            foreach ($pairMiracle as $pair) {
                if ($pair0 == $pair['pairnumber']) {
                    if (mb_substr($pair['pairtype'], 0, 1) == 'R') {
                        $pair0Type = 'R';
                    }
                }

                if ($pair1 == $pair['pairnumber']) {
                    if (mb_substr($pair['pairtype'], 0, 1) == 'R') {
                        $pair1Type = 'R';
                    }
                }
            }


            if ($pair0Type == 'R' || $pair1Type == 'R') {
                return 0;
            }
            return 0;*/
        }

        if ($case == 'singleNumCase') {

            foreach ($pairMiracle as $pair) {
                if ($pairsA[1] == $pair['pairnumber']) {
                    switch ($pair['pairtype']) {

                        case 'D10':
                            $pointAddR = 100;
                            break;

                        case 'D8':
                            $pointAddR = 50;
                            break;

                        case 'D5':
                            $pointAddR = 25;
                            break;

                        case 'R10':
                            $pointMiD = 100;
                            break;
                        case 'R7':
                            $pointMiD = 50;
                            break;
                        case 'R5':
                            $pointMiD = 25;
                            break;
                    }
                }
            }


        }


        if ($case == 'noPreNumCase') {


            foreach ($pairMiracle as $pair) {
                if ($pairsA[0] == $pair['pairnumber']) {
                    switch ($pair['pairtype']) {

                        case 'D10':
                            $pointAddR = 100;
                            break;

                        case 'D8':
                            $pointAddR = 50;
                            break;

                        case 'D5':
                            $pointAddR = 25;
                            break;

                        case 'R10':
                            $pointMiD = 100;
                            break;
                        case 'R7':
                            $pointMiD = 50;
                            break;
                        case 'R5':
                            $pointMiD = 25;
                            break;
                    }
                }
            }


        }


        return array('scoreAngsolMiD' => $pointMiD, 'scoreAngsolAddR' => $pointAddR);
        ;


    }

    private function getPairPosition0B($pairsB, $pairMiracle): array
    {

        $scoreR = 0;
        foreach ($pairMiracle as $pair) {
            if ($pair['pairnumber'] == $pairsB[0]) {
                switch (mb_substr($pair['pairtype'], 0, 1)) {
                    case "R":
                        $scoreR = 100;
                }
            }
        }

        return array('score0BD' => 0, 'score0BR' => $scoreR);
    }

    private function getScorePrefixNumber($case, $pairsA, $pairMiracle): array
    {

        $scoreD = 0;
        $scoreR = 0;
        if ($case == 'singleNumCase') {
            foreach ($pairMiracle as $pair) {
                if ($pair['pairnumber'] == $pairsA[0]) {
                    switch ($pair['pairtype']) {

                        case 'D10':
                            $scoreD = 100;
                            break;
                        case 'D8':
                            $scoreD = 50;
                            break;
                        case 'D5':
                            $scoreD = 25;
                            break;

                        case 'R10':
                            $scoreR = 100;
                            break;
                        case 'R7':
                            $scoreR = 100;
                            break;
                        case 'R5':
                            $scoreR = 100;
                            break;
                    }
                }
            }
        }


        return array('scoreD' => $scoreD, 'scoreR' => $scoreR);
    }

    private function getScoreBonusByPosition($pairsA, $pairsB, $sumMud, $sumLastNum, $pairsMiracle): array
    {
        $scorePositionLastDPairA = 0;
        $scorePositionLastRPairA = 0;

        $scorePositionLastDPairB = 0;
        $scorePositionLastRPairB = 0;

        $scorePositionLastDPairSumMud = 0;
        $scorePositionLastRPairSumMud = 0;

        $scorePositionLastDPairSumLastNum = 0;
        $scorePositionLastRPairSumLastNum = 0;

        $score42A24B = 0;
        $score59A45B = 0;


        if (count($pairsA) == 4 && count($pairsB) == 3) {
            if ($pairsA[2] == '42' && $pairsB[2] == '24') {
                $score42A24B = 100;
            }
        }


        if (count($pairsA) == 4 && count($pairsB) == 3) {
            if ($pairsA[3] == '59' && $pairsB[2] == '45') {
                $score59A45B = 150;
            }
        }


        foreach ($pairsMiracle as $item) {

            if ($item['pairnumber'] == $pairsA[count($pairsA) - 1] || $item['pairnumber'] == $pairsA[count($pairsA) - 2]) {
                switch ($item['pairtype']) {
                    case "D10":
                        $scorePositionLastDPairA += 100;
                        break;
                    case "D8":
                        $scorePositionLastDPairA += 50;
                        break;
                    case "D5":
                        $scorePositionLastDPairA += 25;
                        break;

                    case "R10":
                        $scorePositionLastRPairA += 200;
                        break;
                    case "R7":
                        $scorePositionLastRPairA += 100;
                        break;
                    case "R5":
                        $scorePositionLastRPairA += 50;
                        break;
                }
            }

            if ($item['pairnumber'] == $pairsB[count($pairsB) - 1]) {
                switch ($item['pairtype']) {
                    case "D10":
                        $scorePositionLastDPairB += 100;
                        break;
                    case "D8":
                        $scorePositionLastDPairB += 50;
                        break;
                    case "D5":
                        $scorePositionLastDPairB += 25;
                        break;

                    case "R10":
                        $scorePositionLastRPairB += 200;
                        break;
                    case "R7":
                        $scorePositionLastRPairB += 100;
                        break;
                    case "R5":
                        $scorePositionLastRPairB += 50;
                        break;
                }
            }


            if ($item['pairnumber'] == $sumMud) {
                switch ($item['pairtype']) {
                    case "D10":
                        $scorePositionLastDPairSumMud += 100;
                        break;
                    case "D8":
                        $scorePositionLastDPairSumMud += 50;
                        break;
                    case "D5":
                        $scorePositionLastDPairSumMud += 50;
                        break;

                    case "R10":
                        $scorePositionLastRPairSumMud += 200;
                        break;
                    case "R7":
                        $scorePositionLastRPairSumMud += 100;
                        break;
                    case "R5":
                        $scorePositionLastRPairSumMud += 100;
                        break;
                }
            }

            if ($item['pairnumber'] == $sumLastNum) {
                switch ($item['pairtype']) {
                    case "D10":
                        $scorePositionLastDPairSumLastNum += 160;
                        break;
                    case "D8":
                        $scorePositionLastDPairSumLastNum += 50;
                        break;
                    case "D5":
                        $scorePositionLastDPairSumLastNum += 50;
                        break;

                    case "R10":
                        $scorePositionLastRPairSumLastNum += 200;
                        break;
                    case "R7":
                        $scorePositionLastRPairSumLastNum += 100;
                        break;
                    case "R5":
                        $scorePositionLastRPairSumLastNum += 100;
                        break;
                }
            }
        }

        $sumD = $scorePositionLastDPairA + $scorePositionLastDPairB + $scorePositionLastDPairSumMud + $scorePositionLastDPairSumLastNum + $score42A24B + $score59A45B;
        $sumR = $scorePositionLastRPairA + $scorePositionLastRPairB + $scorePositionLastRPairSumMud + $scorePositionLastRPairSumLastNum;

        return array('scoreBonusD' => $sumD, 'scoreBonusR' => $sumR);

    }


    private function setScoreTotalDR()
    {
        foreach ($this->pairsMiracle as $pairx) {
            if (mb_substr($pairx['pairtype'], 0, 1) == "D") {
                $this->scoreTotalD = $this->scoreTotalD + $pairx['pairpoint'];
            }

            if (mb_substr($pairx['pairtype'], 0, 1) == "R") {
                $this->scoreTotalR = $this->scoreTotalR + $pairx['pairpoint'];
            }
        }


        $pointSpecialR = 0;
        $pointSpecialMiD = 0;
        if ($this->pairSum == '35' || $this->pairSum == '53') {

            $pointSpecialR = 40;
            $pointSpecialMiD = 100;
        }


        $poitSpecialLastNumD = 0;
        $poitSpecialLastNumR = 0;

        if ($this->pairSumNumLast == '35' || $this->pairSumNumLast == '53' || $this->pairSumNumLast == '23' || $this->pairSumNumLast == '32') {
            $poitSpecialLastNumD = 100;
            $poitSpecialLastNumR = 50;

        }

        $this->scoreTotalD = $this->scoreTotalD + ($this->sumOfTotalPercent()['sumPercentD'] * 10) - $pointSpecialMiD - $poitSpecialLastNumD;
        $this->scoreTotalR = $this->scoreTotalR + ($this->sumOfTotalPercent()['sumPercentR'] * 10) + $pointSpecialR + $poitSpecialLastNumR;
    }


    private function getMiracleSummary($scoreTotalD, $scoreTotalR): array
    {

        $scoreD = $scoreTotalD;

        if ($scoreD >= 1500) {
            $miracleD = "ทะเบียนนี้ดี = " . number_format($scoreD) . " คะแนน ทะเบียนดีมาก ทะเบียนมหามงคลหายากเสริมดวงดีแท้ ***แต่ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนทะเบียนทันที";
        } elseif ($scoreD >= 924) {
            $miracleD = "ทะเบียนนี้ดี = " . number_format($scoreD) . " คะแนน ทะเบียนดีมาก ทะเบียนมหามงคลหายากเสริมดวงดีแท้ ***แต่ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนทะเบียนทันที";
        } elseif ($scoreD >= 654) {
            $miracleD = "ทะเบียนนี้ดี = " . number_format($scoreD) . " คะแนน ดีเป็นทะเบียน มงคล แต่ควรดูความหมายเลขและเลขผลร้ายประกอบให้ดี ***ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนทะเบียนทันที";
        } elseif ($scoreD >= 540) {
            $miracleD = "ทะเบียนนี้ดี = " . number_format($scoreD) . " คะแนน ดี แต่เสี่ยง ควรดูความหมายตัวเลขและคะแนนร้ายประกอบให้ดี ***ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนทะเบียนทันที";
        } elseif ($scoreD >= 450) {
            $miracleD = "ทะเบียนนี้ดี = " . number_format($scoreD) . " คะแนน ปานกลาง เสี่ยงมาก ***ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนทะเบียนทันที";
        } elseif ($scoreD >= 350) {
            $miracleD = "ทะเบียนนี้ดี = " . number_format($scoreD) . " คะแนน พอใช้ เสี่ยงมาก ***ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนทะเบียนทันที";
        } elseif ($scoreD >= 250) {
            $miracleD = "ทะเบียนนี้ดี = " . number_format($scoreD) . " คะแนน พอใช้ เสี่ยงมาก ***ถ้าคะแนนร้ายได้เกิน 350 คะแนน ควรเปลี่ยนทะเบียนทันที";
        } else {
            $miracleD = "อันตรายมากๆ ควรสวดมนต์ทำบุญบ่อยๆและควรรีบเปลี่ยนทะเบียน";
        }


        $scoreR = $scoreTotalR;
        if ($scoreR >= 500) {
            $miracleR = "ทะเบียนนี้ร้าย = " . number_format($scoreR) . " คะแนน อันตรายมาก! หายนะรออยู่เบื้องหน้า ควรเปลี่ยนทะเบียนทันที";
        } elseif ($scoreR >= 400) {
            $miracleR = "ทะเบียนนี้ร้าย " . number_format($scoreR) . " คะแนน อันตรายมาก! ควรเปลี่ยนทะเบียนทันที";
        } elseif ($scoreR >= 300) {
            $miracleR = "ทะเบียนนี้ร้าย = " . number_format($scoreR) . " คะแนน อันตราย! เสี่ยงมาก! จงระวังอย่าประมาท ต้องรอบคอบหมั่นทำบุญ";
        } elseif ($scoreR >= 200) {
            $miracleR = "ทะเบียนนี้ร้าย = " . number_format($scoreR) . " คะแนน อันตราย! เสี่ยง! อย่าประมาท ทำบุญเสริมจะดีไม่ตก";
        } elseif ($scoreR >= 100) {
            $miracleR = "ทะเบียนนี้ร้าย = " . number_format($scoreR) . " คะแนน สมดุลชีวิตทำบุญเสริมจะได้ดีราบรื่นตลอด";
        } elseif ($scoreR >= 20) {
            $miracleR = "ทะเบียนนี้ร้าย = " . number_format($scoreR) . " คะแนน ไม่มีอิทธิพล ทำบุญเสริมราบรื่นตลอด";
        } else {
            $miracleR = "ทะเบียนนี้ร้าย = " . number_format($scoreR) . " คะแนน ไม่มีอิทธิพล ทำบุญเสริมราบรื่นตลอด";
        }

        return array('miracleD' => $miracleD, 'miracleR' => $miracleR);

    }


    private function sumOfTotalPercent(): array
    {
        $pointD = 0;
        $pointR = 0;

        // Ensure keys exist to avoid PHP Notices/Warnings
        if (!isset($this->percentTabian['pairPercentAngSon'])) {
            $this->percentTabian['pairPercentAngSon'] = array('percentD' => 0, 'percentR' => 0);
        }
        if (!isset($this->percentTabian['percentByCaseA'])) {
            $this->percentTabian['percentByCaseA'] = array();
        }
        if (!isset($this->percentTabian['percentByCaseB'])) {
            $this->percentTabian['percentByCaseB'] = array();
        }
        if (!isset($this->percentTabian['sumPercentNum'])) {
            $this->percentTabian['sumPercentNum'] = array('percentD' => 0, 'percentR' => 0);
        }
        if (!isset($this->percentTabian['sumPercentMud'])) {
            $this->percentTabian['sumPercentMud'] = array('percentD' => 0, 'percentR' => 0);
        }
        if (!isset($this->percentTabian['pairPercentNumMud'])) {
            $this->percentTabian['pairPercentNumMud'] = array('percentD' => 0, 'percentR' => 0);
        }

        $pointD = $pointD + $this->percentTabian['pairPercentAngSon']['percentD'];
        $pointR = $pointR + $this->percentTabian['pairPercentAngSon']['percentR'];

        foreach ($this->percentTabian['percentByCaseA'] as $value) {
            $pointD = $pointD + $value['percentD'];
            $pointR = $pointR + $value['percentR'];
        }

        foreach ($this->percentTabian['percentByCaseB'] as $value) {
            $pointD = $pointD + $value['percentD'];
            $pointR = $pointR + $value['percentR'];
        }

        $pointD = $pointD + $this->percentTabian['sumPercentNum']['percentD'];
        $pointR = $pointR + $this->percentTabian['sumPercentNum']['percentR'];

        $pointD = $pointD + $this->percentTabian['sumPercentMud']['percentD'];
        $pointR = $pointR + $this->percentTabian['sumPercentMud']['percentR'];

        if ($this->case == 'singleNumCase') {

            $pointD = $pointD + $this->percentTabian['pairPercentNumMud']['percentD'];
            $pointR = $pointR + $this->percentTabian['pairPercentNumMud']['percentR'];

        }


        $percentCheckPair = $this->getPercentNumSingleSecond($this->numLast);

        $pointPercentD = 0;
        $pointPercentR = 0;


        if ($this->pairSum == '35' || $this->pairSum == '53') {
            $pointPercentD += 25;
            $pointPercentR += 25;

        }

        if ($this->pairSumNumLast == '35' || $this->pairSumNumLast == '53' || $this->pairSumNumLast == '23' || $this->pairSumNumLast == '32') {
            $pointPercentD -= 5;
            $pointPercentR -= 5;

        }


        return array(

            'sumPercentD' => $pointD - $this->percentPairMi - $percentCheckPair + $pointPercentD,
            'sumPercentR' => $pointR + $this->percentPairMi + $percentCheckPair - $pointPercentR
        );

    }


    private function getPercentNumSingleSecond($pairSecoudNum): int
    {

        $pointTotal = 0;

        $pointSet1 = 10;
        $pointSet2 = 15;
        $pairsSet1 = array('2' => $pointSet1, '4' => $pointSet1, '5' => $pointSet1, '6' => $pointSet1, '9' => $pointSet1, '20' => $pointSet1, '02' => $pointSet1, '04' => $pointSet1, '40' => $pointSet1, '50' => $pointSet1, '05' => $pointSet1, '06' => $pointSet1, '60' => $pointSet1, '09' => $pointSet1, '90' => $pointSet1);

        $pairsSet2 = array('1' => $pointSet2, '3' => $pointSet2, '7' => $pointSet2, '8' => $pointSet2, '0' => $pointSet2, '10' => $pointSet2, '01' => $pointSet2, '03' => $pointSet2, '30' => $pointSet2, '07' => $pointSet2, '70' => $pointSet2, '08' => $pointSet2, '80' => $pointSet2, '00' => $pointSet2);


        if (mb_strlen($pairSecoudNum) <= 2) {
            foreach ($pairsSet1 as $k => $v) {
                if ($pairSecoudNum == $k) {
                    $pointTotal = $pointTotal + $v;
                }
            }


            foreach ($pairsSet2 as $k => $v) {
                if ($pairSecoudNum == $k) {
                    $pointTotal = $pointTotal + $v;
                }
            }
        }


        return $pointTotal;
    }


    private function isNumber(string $tabianRaw): bool
    {

        foreach ($this->realNumber as $rnumb) {
            if ($rnumb == mb_substr($tabianRaw, 0, 1)) {
                return true;
            }
        }

        return false;
    }

    private function setCasePercentTabian(string $tabainRAW)
    {

        if ($this->isNumber($tabainRAW)) {
            $this->case = 'singleNumCase';
        } elseif ($this->countPayan($tabainRAW) == 3) {
            $this->case = 'tripleChaCase';

        } elseif ($this->countPayan($tabainRAW) == 2) {
            $this->case = 'noPreNumCase';

        }


    }

    private function setPercentByCase(string $case, string $carPairSumAll, string $pairSumNumLast, string $numLast, array $pairA, array $pairB)
    {

        $sql = "SELECT * FROM tabian_number ORDER BY pairnumberid ASC";
        $result = $this->db->prepare($sql);
        $result->execute();
        $pairMiracle = $result->fetchAll(\PDO::FETCH_OBJ);

        //เอาไปคิดทั้งเลขรวมหมวดและรวมเลข
        $miSumMudPercent = array('23' => 12, '32' => 12, '35' => 12, '53' => 12, '25' => 9, '52' => 9, '26' => 3, '62' => 5);
        $miRSumMudPercent = 15;

        if ($case == 'noPreNumCase') {

            $this->percentTabian['pairPercentNumMud'] = array('percentD' => 0, 'percentR' => 0);
            $this->pairAngsol = $pairA[0];


            foreach ($pairMiracle as $pairM) {

                if ($pairM->pairnumber == $this->pairAngsol) {

                    if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                        $this->pairPercentAngSon = array('percentD' => 5, 'percentR' => 0);
                    } else {
                        $this->pairPercentAngSon = array('percentD' => 0, 'percentR' => 5);
                    }

                    $this->percentTabian['pairPercentAngSon'] = $this->pairPercentAngSon;
                }

                if ($pairM->pairnumber == $carPairSumAll) {


                    foreach ($miSumMudPercent as $num => $point) {
                        if ($carPairSumAll == $num) {
                            $this->percentPairMi = $this->percentPairMi + $point;
                            $this->percentSiang += $point;
                        }
                    }

                    if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                        $this->sumPercentMud = array('percentD' => 20, 'percentR' => 0);
                    } else {
                        $this->percentPairMi = $this->percentPairMi + $miRSumMudPercent;
                        $this->sumPercentMud = array('percentD' => 0, 'percentR' => 20);
                    }

                    $this->percentTabian['sumPercentMud'] = $this->sumPercentMud;
                }

                if ($pairM->pairnumber == $pairSumNumLast) {


                    if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                        $this->sumPercentNum = array('percentD' => 15, 'percentR' => 0);
                    } else {
                        $this->sumPercentNum = array('percentD' => 0, 'percentR' => 15);
                    }



                    $this->percentTabian['sumPercentNum'] = $this->sumPercentNum;
                }


                if (mb_strlen($numLast) == 3 || mb_strlen($numLast) == 4) {

                    for ($i = 0; $i < count($pairA); $i++) {
                        if ($pairM->pairnumber == $pairA[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                                if ($i == 0) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 10, 'percentR' => 0);
                                }
                                if ($i == 1) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 10, 'percentR' => 0);
                                }

                                if ($i == 2) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 20, 'percentR' => 0);
                                }

                            } else {
                                if ($i == 0) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 10);
                                }

                                if ($i == 1) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 10);
                                }

                                if ($i == 2) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 20);
                                }


                            }

                        }


                    }

                    for ($i = 0; $i < count($pairB); $i++) {
                        if ($pairM->pairnumber == $pairB[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                                if ($i == 0) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 10, 'percentR' => 0);

                                }
                                if ($i == 1) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 10, 'percentR' => 0);

                                }


                            } else {
                                if ($i == 0) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 0, 'percentR' => 10);
                                }
                                if ($i == 1) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 0, 'percentR' => 10);
                                }


                            }

                        }


                    }

                    $this->percentTabian['percentByCaseA'] = $this->percentByCaseA;
                    $this->percentTabian['percentByCaseB'] = $this->percentByCaseB;
                }

                if (mb_strlen($numLast) == 1 || mb_strlen($numLast) == 2) {

                    for ($i = 0; $i < count($pairA); $i++) {
                        if ($pairM->pairnumber == $pairA[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                                if ($i == 0) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 15, 'percentR' => 0);

                                }
                                if ($i == 1) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 30, 'percentR' => 0);

                                }


                            } else {
                                if ($i == 0) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 15);
                                }
                                if ($i == 1) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 30);
                                }


                            }

                        }


                    }

                    for ($i = 0; $i < count($pairB); $i++) {
                        if ($pairM->pairnumber == $pairB[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                                if ($i == 0) {

                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 15, 'percentR' => 0);

                                }


                            } else {
                                if ($i == 0) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 0, 'percentR' => 15);
                                }


                            }

                        }


                    }

                    $this->percentTabian['percentByCaseA'] = $this->percentByCaseA;
                    $this->percentTabian['percentByCaseB'] = $this->percentByCaseB;
                }


            }


        }

        if ($case == 'singleNumCase') {
            $this->pairNumMud = $pairA[0];
            $this->pairAngsol = $pairA[1];

            foreach ($pairMiracle as $pairM) {

                if ($pairM->pairnumber == $this->pairNumMud) {

                    if (mb_substr($pairM->pairtype, 0, 1) == 'D') {

                        $this->pairPercentNumMud = array('percentD' => 5, 'percentR' => 0);
                    } else {
                        $this->pairPercentNumMud = array('percentD' => 0, 'percentR' => 5);
                    }

                    $this->percentTabian['pairPercentNumMud'] = $this->pairPercentNumMud;
                }

                if ($pairM->pairnumber == $this->pairAngsol) {

                    if (mb_substr($pairM->pairtype, 0, 1) == 'D') {

                        $this->pairPercentAngSon = array('percentD' => 5, 'percentR' => 0);
                    } else {
                        $this->pairPercentAngSon = array('percentD' => 0, 'percentR' => 5);
                    }

                    $this->percentTabian['pairPercentAngSon'] = $this->pairPercentAngSon;
                }

                if ($pairM->pairnumber == $carPairSumAll) {

                    foreach ($miSumMudPercent as $num => $point) {
                        if ($carPairSumAll == $num) {
                            $this->percentPairMi = $this->percentPairMi + $point;
                            $this->percentSiang += $point;
                        }
                    }


                    //ถ้าผลรวมหมวดเป็นเลขร้ายอื่นๆ ค่าความเสี่ยงจะเพิ่มขึ้น 15 แต้ม
                    if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                        $this->sumPercentMud = array('percentD' => 15, 'percentR' => 0);
                    } else {
                        $this->percentPairMi = $this->percentPairMi + $miRSumMudPercent;
                        $this->sumPercentMud = array('percentD' => 0, 'percentR' => 15);
                    }
                    $this->percentTabian['sumPercentMud'] = $this->sumPercentMud;
                }

                if ($pairM->pairnumber == $pairSumNumLast) {
                    if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                        $this->sumPercentNum = array('percentD' => 15, 'percentR' => 0);
                    } else {
                        $this->sumPercentNum = array('percentD' => 0, 'percentR' => 15);
                    }
                    $this->percentTabian['sumPercentNum'] = $this->sumPercentNum;
                }


                if (mb_strlen($numLast) == 3 || mb_strlen($numLast) == 4) {

                    for ($i = 0; $i < count($pairA); $i++) {
                        if ($pairM->pairnumber == $pairA[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {

                                if ($i == 0) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 5, 'percentR' => 0);
                                }

                                if ($i == 1) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 10, 'percentR' => 0);
                                }

                                if ($i == 2) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 15, 'percentR' => 0);
                                }

                                if ($i == 3) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 15, 'percentR' => 0);

                                }

                            } else {

                                if ($i == 0) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 5);
                                }
                                if ($i == 1) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 10);
                                }

                                if ($i == 2) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 15);
                                }

                                if ($i == 3) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 15);
                                }

                            }

                        }


                    }

                    for ($i = 0; $i < count($pairB); $i++) {
                        if ($pairM->pairnumber == $pairB[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                                if ($i == 0) {

                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 3, 'percentR' => 0);

                                }
                                if ($i == 1) {

                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 4, 'percentR' => 0);

                                }

                                if ($i == 2) {

                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 8, 'percentR' => 0);

                                }


                            } else {
                                if ($i == 0) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 0, 'percentR' => 3);
                                }
                                if ($i == 1) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 0, 'percentR' => 4);
                                }

                                if ($i == 2) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 0, 'percentR' => 8);
                                }


                            }

                        }


                    }

                    $this->percentTabian['percentByCaseA'] = $this->percentByCaseA;
                    $this->percentTabian['percentByCaseB'] = $this->percentByCaseB;
                }

                if (mb_strlen($numLast) == 1 || mb_strlen($numLast) == 2) {

                    for ($i = 0; $i < count($pairA); $i++) {
                        if ($pairM->pairnumber == $pairA[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                                if ($i == 0) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 5, 'percentR' => 0);

                                }
                                if ($i == 1) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 10, 'percentR' => 0);

                                }
                                if ($i == 2) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 30, 'percentR' => 0);

                                }


                            } else {
                                if ($i == 0) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 5);
                                }
                                if ($i == 1) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 10);
                                }
                                if ($i == 2) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 30);
                                }


                            }

                        }


                    }

                    for ($i = 0; $i < count($pairB); $i++) {
                        if ($pairM->pairnumber == $pairB[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                                if ($i == 0) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 6, 'percentR' => 0);

                                }

                                if ($i == 1) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 9, 'percentR' => 0);

                                }


                            } else {
                                if ($i == 0) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 0, 'percentR' => 6);
                                }

                                if ($i == 1) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 0, 'percentR' => 9);
                                }


                            }

                        }


                    }

                    $this->percentTabian['percentByCaseA'] = $this->percentByCaseA;
                    $this->percentTabian['percentByCaseB'] = $this->percentByCaseB;
                }


            }

        }

        if ($case == 'tripleChaCase') {

            $this->pairAngsolA0 = $pairA[0];
            $this->pairAngsolA1 = $pairA[1];


            foreach ($pairMiracle as $pairM) {


                if ($pairM->pairnumber == $this->pairAngsolA0) {

                    if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                        $this->a0Status = true;
                    } else {
                        $this->a0Status = false;
                    }


                }

                if ($pairM->pairnumber == $this->pairAngsolA1) {

                    if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                        $this->a1Status = true;
                    } else {
                        $this->a1Status = false;
                    }
                }


                if ($this->a0Status && $this->a1Status) {
                    $this->pairAngsol = array('percentD' => 5, 'percentR' => 0);
                }

                if (!$this->a0Status || !$this->a1Status) {
                    $this->pairAngsol = array('percentD' => 2.5, 'percentR' => 2.5);
                }

                if (!$this->a0Status && !$this->a1Status) {
                    $this->pairAngsol = array('percentD' => 0, 'percentR' => 5);
                }

                $this->percentTabian['pairPercentAngSon'] = $this->pairAngsol;


                if ($pairM->pairnumber == $carPairSumAll) {

                    foreach ($miSumMudPercent as $num => $point) {
                        if ($carPairSumAll == $num) {
                            $this->percentPairMi = $point;
                            $this->percentSiang += $point;
                        }
                    }

                    if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                        $this->sumPercentMud = array('percentD' => 15, 'percentR' => 0);
                    } else {
                        $this->percentPairMi = $this->percentPairMi + $miRSumMudPercent;
                        $this->sumPercentMud = array('percentD' => 0, 'percentR' => 15);
                    }
                    $this->percentTabian['sumPercentMud'] = $this->sumPercentMud;
                }

                if ($pairM->pairnumber == $pairSumNumLast) {
                    if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                        $this->sumPercentNum = array('percentD' => 18, 'percentR' => 0);
                    } else {
                        $this->sumPercentNum = array('percentD' => 0, 'percentR' => 18);
                    }
                    $this->percentTabian['sumPercentNum'] = $this->sumPercentNum;
                }


                if (mb_strlen($numLast) == 3 || mb_strlen($numLast) == 4) {

                    for ($i = 0; $i < count($pairA); $i++) {
                        if ($pairM->pairnumber == $pairA[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                                if ($i == 0) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 5, 'percentR' => 0);

                                }
                                if ($i == 1) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 10, 'percentR' => 0);

                                }

                                if ($i == 2) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 14, 'percentR' => 0);


                                }

                                if ($i == 3) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 18, 'percentR' => 0);


                                }

                            } else {
                                if ($i == 0) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 5);
                                }
                                if ($i == 1) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 10);
                                }

                                if ($i == 2) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 14);
                                }

                                if ($i == 3) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 18);
                                }


                            }

                        }


                    }

                    for ($i = 0; $i < count($pairB); $i++) {
                        if ($pairM->pairnumber == $pairB[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                                if ($i == 0) {

                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 6, 'percentR' => 0);

                                }
                                if ($i == 1) {

                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 9, 'percentR' => 0);

                                }


                            } else {
                                if ($i == 0) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 0, 'percentR' => 6);
                                }
                                if ($i == 1) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 0, 'percentR' => 9);
                                }


                            }

                        }


                    }

                    $this->percentTabian['percentByCaseA'] = $this->percentByCaseA;
                    $this->percentTabian['percentByCaseB'] = $this->percentByCaseB;
                }

                if (mb_strlen($numLast) == 1 || mb_strlen($numLast) == 2) {

                    for ($i = 0; $i < count($pairA); $i++) {
                        if ($pairM->pairnumber == $pairA[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                                if ($i == 0) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 10, 'percentR' => 0);

                                }
                                if ($i == 1) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 10, 'percentR' => 0);

                                }
                                if ($i == 2) {

                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 30, 'percentR' => 0);

                                }


                            } else {
                                if ($i == 0) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 10);
                                }
                                if ($i == 1) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 10);
                                }
                                if ($i == 2) {
                                    $this->percentByCaseA[$i . '->' . $pairA[$i]] = array('percentD' => 0, 'percentR' => 30);
                                }


                            }

                        }


                    }

                    for ($i = 0; $i < count($pairB); $i++) {
                        if ($pairM->pairnumber == $pairB[$i]) {
                            if (mb_substr($pairM->pairtype, 0, 1) == 'D') {
                                if ($i == 0) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 12, 'percentR' => 0);

                                }


                            } else {
                                if ($i == 0) {
                                    $this->percentByCaseB[$i . '->' . $pairB[$i]] = array('percentD' => 0, 'percentR' => 12);
                                }


                            }

                        }


                    }

                    $this->percentTabian['percentByCaseA'] = $this->percentByCaseA;
                    $this->percentTabian['percentByCaseB'] = $this->percentByCaseB;
                }


            }

        }

    }

    private function initSetPairTabianV2($tabianNumber) //ถอดเลขหน้า ตรวจสอบ true หรือ false
    {
        $this->firstCharTabian = mb_substr($tabianNumber, 0, 1);
        $this->firsCharStatus = $this->getStatusFirstCharTabian($this->firstCharTabian, $this->realNumber);
    }

    private function initTabianNonFirstNum($firsCharStatus, $tabian)
    {
        if ($firsCharStatus) { //ถ้าอักษรตัวแรกเป็นตัวเลข ให้ถอดตัวเลขหน้าออก
            $this->tabianNoNFirstNum = $this->splitPairByAfterNum($tabian);

        } else { //ถ้าอักษรตัวแรกไม่ใช่ตัวเลข ไม่ต้องถอดเลขหน้าออก
            $this->tabianNoNFirstNum = $tabian;
        }

        $this->initTabianPealNum($this->tabianNoNFirstNum);

    } //ถ้าอักษรตัวแรกเป็นตัวเลข ให้ถอดตัวเลขหน้าออก

    private function initTabianPealNum($tabianNoNFirstNum)
    {
        $this->tabianPealNum = $this->changeTextToNumberV2($tabianNoNFirstNum); //ไม่มีเลขหน้า
    } //อักษรเป็นตัวเลขไม่มีเลขหมวด

    //จับคู่หลัก
    private function getPairsA($firsCharStatus, $tabianPealNum, $firstCharTabian): array
    {
        $pairsA = array();


        if ($this->countPayan($this->carRAW) == 3) {

            for ($i = 0; $i < mb_strlen($tabianPealNum); $i++) {
                if ($i < 2) {
                    $pair = mb_substr($tabianPealNum, $i, 2);
                    array_push($pairsA, $pair);
                }
            }

            $cNumber = $this->countNumber($this->carRAW);

            if ($cNumber == 4) {

                $pair = mb_substr($this->carRAW, 3, 2);
                array_push($pairsA, $pair);

                $pair = mb_substr($this->carRAW, 5, 2);
                array_push($pairsA, $pair);

            }

            if ($cNumber == 3) {

                $pair = mb_substr($this->carRAW, 3, 2);
                array_push($pairsA, $pair);

                $pair = mb_substr($this->carRAW, 4, 2);
                array_push($pairsA, $pair);

            }

            if ($cNumber == 2) {

                $pair = mb_substr($this->carRAW, 3, 2);
                array_push($pairsA, $pair);


            }

            if ($cNumber == 1) {

                $pair = mb_substr($this->carRAW, 3, 1);
                array_push($pairsA, $pair);


            }


        } else {
            if ($firsCharStatus) { //ถ้ามีเลขหน้าหมวด
                array_push($pairsA, $firstCharTabian); //รวมตัวเลขเข้าตัวเลขอักษร
            }

            if (!$this->getSingleNum($this->carRAW)) {
                for ($i = 0; $i < mb_strlen($tabianPealNum); $i += 2) {

                    $pair = (mb_strlen(mb_substr($tabianPealNum, $i, 2)) % 2 == 1) ? mb_substr($tabianPealNum, $i - 1, 2) : mb_substr($tabianPealNum, $i, 2); //ถ้าตัวหลังสุดเป็นคี่ให้เอาตัวหน้ามาเป็นคู่
                    array_push($pairsA, $pair);


                }
            } else {
                for ($i = 0; $i < mb_strlen($tabianPealNum); $i += 2) {
                    $pair = mb_substr($tabianPealNum, $i, 2); //ถ้าตัวหลังสุดเป็นคี่ให้เอาตัวหน้ามาเป็นคู่
                    array_push($pairsA, $pair);
                } //ถ้าตัวหลังสุดเป็นคี่ให้เอาตัวหน้ามาเป็นคู่
            }
        }

        return $pairsA;
    }

    //จับคู่แฝง
    private function getPairsB($firsCharStatus, $tabianPealNum, $firstCharTabian): array
    {
        $pairsB = array();

        if ($this->countPayan($this->carRAW) == 3) {

            $cNumber = $this->countNumber($this->carRAW);

            if ($cNumber == 4) {

                $pair = mb_substr($tabianPealNum, 2, 2);
                array_push($pairsB, $pair);

                $pair = mb_substr($tabianPealNum, 4, 2);
                array_push($pairsB, $pair);

            }

            if ($cNumber == 3) {

                $pair = mb_substr($tabianPealNum, 2, 2);
                array_push($pairsB, $pair);

                $pair = mb_substr($tabianPealNum, 4, 2);
                array_push($pairsB, $pair);

            }

            if ($cNumber == 2) {

                $pair = mb_substr($tabianPealNum, 2, 2);
                array_push($pairsB, $pair);


            }

            if ($cNumber == 1) {

                $pair = mb_substr($tabianPealNum, 2, 2);
                array_push($pairsB, $pair);


            }


        } else {

            if ($firsCharStatus) { //ถ้ามีเลขหน้าหมวด ให้จับคู่นับจากตำแหน่ง 0

                $arrNumFag = $firstCharTabian . $tabianPealNum;


                for ($i = 0; $i < mb_strlen($arrNumFag); $i += 2) {

                    if (mb_strlen(mb_substr($arrNumFag, $i, 2)) % 2 == 0) {
                        $pair = mb_substr($arrNumFag, $i, 2);
                        array_push($pairsB, $pair);
                    }

                }

            } else {//ถ้าไม่มีเลขหน้าหมวด ให้จับคู่นับจากตำแหน่ง 1
                for ($i = 1; $i < mb_strlen($tabianPealNum); $i += 2) {

                    if (mb_strlen(mb_substr($tabianPealNum, $i, 2)) % 2 == 0) {
                        $pair = mb_substr($tabianPealNum, $i, 2);


                        array_push($pairsB, $pair);

                    }


                }
            }
        }

        return $pairsB;
    }

    private function splitPairByAfterNum($tabian): string
    {
        return mb_substr($tabian, 1, mb_strlen($tabian));
    }

    private function changeTextToNumberV2($fullCarId): string
    {
        $txt = array(
            'ก' => '1',
            'ด' => '1',
            'ถ' => '1',
            'ท' => '1',
            'ภ' => '1',
            'ฤ' => '1',
            'ฦ' => '1',
            'า' => '1',
            'ำ' => '1',
            'ุ' => '1',
            '่' => '1',
            'ข' => '2',
            'ง' => '2',
            'ช' => '2',
            'บ' => '2',
            'ป' => '2',
            'เ' => '2',
            'แ' => '2',
            'ู' => '2',
            '้' => '2',
            'ฆ' => '3',
            'ต' => '3',
            'ฑ' => '3',
            'ฒ' => '3',
            '๋' => '3',
            'ค' => '4',
            'ธ' => '4',
            'ญ' => '4',
            'ร' => '4',
            'ษ' => '4',
            'ะ' => '4',
            'โ' => '4',
            'ั' => '4',
            'ิ' => '4',
            'ฉ' => '5',
            'ฌ' => '5',
            'ณ' => '5',
            'น' => '5',
            'ม' => '5',
            'ห' => '5',
            'ฎ' => '5',
            'ฮ' => '5',
            'ฬ' => '5',
            'ึ' => '5',
            'จ' => '6',
            'ล' => '6',
            'ว' => '6',
            'อ' => '6',
            'ใ' => '6',
            'ซ' => '7',
            'ศ' => '7',
            'ส' => '7',
            '๊' => '7',
            'ี' => '7',
            'ื' => '7',
            'ผ' => '8',
            'ฝ' => '8',
            'พ' => '8',
            'ฟ' => '8',
            'ย' => '8',
            '็' => '8',
            'ฏ' => '9',
            'ฐ' => '9',
            'ไ' => '9',
            '์' => '9',
            'a' => '1',
            'i' => '1',
            'j' => '1',
            'q' => '1',
            'y' => '1',
            'A' => '1',
            'I' => '1',
            'J' => '1',
            'Q' => '1',
            'Y' => '1',
            'b' => '2',
            'k' => '2',
            'r' => '2',
            'B' => '2',
            'K' => '2',
            'R' => '2',
            'c' => '3',
            'g' => '3',
            'l' => '3',
            's' => '3',
            'C' => '3',
            'G' => '3',
            'L' => '3',
            'S' => '3',
            'd' => '4',
            'm' => '4',
            't' => '4',
            'D' => '4',
            'M' => '4',
            'T' => '4',
            'e' => '5',
            'h' => '5',
            'n' => '5',
            'x' => '5',
            'E' => '5',
            'H' => '5',
            'N' => '5',
            'X' => '5',
            'u' => '6',
            'v' => '6',
            'w' => '6',
            'U' => '6',
            'V' => '6',
            'W' => '6',
            'o' => '7',
            'z' => '7',
            'O' => '7',
            'Z' => '7',
            'f' => '8',
            'p' => '8',
            'F' => '8',
            'P' => '8'
        );

        $caridConvert = null;

        for ($i = 0; $i < mb_strlen($fullCarId); $i++) {
            $xString = mb_substr($fullCarId, $i, 1);

            if (is_numeric($xString)) {
                $caridConvert = $caridConvert . $xString;
            } else {
                foreach ($txt as $txtKey => $txtValue) {
                    if ($xString == $txtKey) {
                        $caridConvert = $caridConvert . $txtValue;
                        break;
                    }
                }
            }


        }


        return $caridConvert;

    }

    private function getStatusFirstCharTabian(string $firstChar, array $realNumber): bool
    {

        foreach ($realNumber as $number) {
            if ($number == $firstChar) {
                return true;
            }
        }

        return false;
    }


    //นับหาจำนวนตัวเลขหลังตัวอักษร 1 ตัว
    private function getSingleNum($tabianRAW): int
    {
        $positionX = 0;
        $singleNum = false;

        for ($i = 0; $i < mb_strlen($tabianRAW); $i++) {

            $countT = 0;
            foreach ($this->txt as $k => $t) {
                for ($i = 0; $i < mb_strlen($tabianRAW); $i++) {

                    if ($k == mb_substr($tabianRAW, $i, 1)) {
                        $countT++;
                    }

                    if ($countT == 2) {
                        $positionX = $i;
                        $countT = 3; //เกินสามไม่ต้องเอาตำแหน่ง i มาใส่

                    }

                }

            }

        }

        $countz = mb_strlen(mb_substr($tabianRAW, $positionX + 1, mb_strlen($tabianRAW)));

        //echo '$countz ' . $countz;


        if ($countz == 1) {
            $singleNum = true;
        }

        return $singleNum;
    }

    private function countPayan($tabianRAW): int
    {

        $count = 0;

        for ($i = 0; $i < mb_strlen($tabianRAW); $i++) {

            $countT = 0;
            foreach ($this->txt as $k => $t) {
                for ($i = 0; $i < mb_strlen($tabianRAW); $i++) {

                    if ($k == mb_substr($tabianRAW, $i, 1)) {
                        $countT++;
                    }
                }
                $count = $countT;

            }

        }

        return $count;


    }

    private function countNumber($tabianRAW): int
    {

        $count = 0;

        for ($i = 0; $i < mb_strlen($tabianRAW); $i++) {

            $countT = 0;
            foreach ($this->realNumber as $numb) {
                for ($i = 0; $i < mb_strlen($tabianRAW); $i++) {

                    if ($numb == mb_substr($tabianRAW, $i, 1)) {
                        $countT++;
                    }
                }
                $count = $countT;

            }

        }

        return $count;


    }


    private function setPairsMiracle($pairsUnique)
    {


        $sql = "SELECT * FROM tabian_number ORDER BY pairnumberid ASC";
        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);

        $tem = array();

        foreach ($data as $value) {
            foreach ($pairsUnique as $pair) {

                if ($value->pairnumber === $pair) {


                    $percentNumberDR = 0;


                    foreach ($this->percentByCaseA as $keyPair => $NumberPercentRD) {

                        $keyNumber = (mb_strlen($keyPair) == 5) ? mb_substr($keyPair, 3, 2) : mb_substr($keyPair, 3, 1);


                        if ($keyNumber == $pair) {
                            if (mb_substr($value->pairtype, 0, 1) == "D") {
                                $percentNumberDR += $NumberPercentRD['percentD'];

                            }

                            if (mb_substr($value->pairtype, 0, 1) == "R") {
                                $percentNumberDR += $NumberPercentRD['percentR'];

                            }
                        }
                    }

                    foreach ($this->percentByCaseB as $keyPair => $NumberPercentRD) {

                        $keyNumber = (mb_strlen($keyPair) == 5) ? mb_substr($keyPair, 3, 2) : mb_substr($keyPair, 3, 1);


                        if ($keyNumber == $pair) {
                            if (mb_substr($value->pairtype, 0, 1) == "D") {
                                $percentNumberDR += $NumberPercentRD['percentD'];

                            }

                            if (mb_substr($value->pairtype, 0, 1) == "R") {
                                $percentNumberDR += $NumberPercentRD['percentR'];

                            }
                        }
                    }

                    if ($this->pairSum == $pair) {
                        if (mb_substr($value->pairtype, 0, 1) == "D") {
                            $percentNumberDR += $this->sumPercentMud['percentD'];

                        }

                        if (mb_substr($value->pairtype, 0, 1) == "R") {
                            $percentNumberDR += $this->sumPercentMud['percentR'];

                        }
                    }


                    if ($this->pairSumNumLast == $pair) {
                        if (mb_substr($value->pairtype, 0, 1) == "D") {
                            $percentNumberDR += $this->sumPercentNum['percentD'];

                        }

                        if (mb_substr($value->pairtype, 0, 1) == "R") {
                            $percentNumberDR += $this->sumPercentNum['percentR'];

                        }
                    }


                    switch ($this->case) {
                        case 'singleNumCase':
                            $prefixNumber = $this->pairsCarA[0];
                            $pairAngsol = $this->pairsCarA[1];



                            if ($prefixNumber == $pair) {

                                if (mb_substr($value->pairtype, 0, 1) == "D") {
                                    $percentNumberDR += $this->pairPercentNumMud['percentD'];

                                }

                                if (mb_substr($value->pairtype, 0, 1) == "R") {
                                    $percentNumberDR += $this->pairPercentNumMud['percentR'];

                                }
                            }

                            if ($pairAngsol == $pair) {

                                if (mb_substr($value->pairtype, 0, 1) == "D") {
                                    $percentNumberDR += $this->pairPercentAngSon['percentD'];

                                }

                                if (mb_substr($value->pairtype, 0, 1) == "R") {
                                    $percentNumberDR += $this->pairPercentAngSon['percentR'];

                                }
                            }

                            break;


                        case 'noPreNumCase':

                            $pairAngsol = $this->pairsCarA[0];

                            if ($pairAngsol == $pair) {

                                if (mb_substr($value->pairtype, 0, 1) == "D") {
                                    $percentNumberDR += $this->pairPercentAngSon['percentD'];

                                }

                                if (mb_substr($value->pairtype, 0, 1) == "R") {
                                    $percentNumberDR += $this->pairPercentAngSon['percentR'];

                                }
                            }

                            break;


                        case 'tripleChaCase':

                            $pairAngsol01 = $this->pairsCarA[0];
                            $pairAngsol02 = $this->pairsCarA[1];

                            if ($pairAngsol01 == $pair) {

                                if (mb_substr($value->pairtype, 0, 1) == "D") {
                                    $percentNumberDR += $this->percentTabian['pairPercentAngSon']['percentD'];

                                }

                                if (mb_substr($value->pairtype, 0, 1) == "R") {
                                    $percentNumberDR += $this->percentTabian['pairPercentAngSon']['percentR'];

                                }
                            }

                            if ($pairAngsol02 == $pair) {

                                if (mb_substr($value->pairtype, 0, 1) == "D") {
                                    $percentNumberDR += $this->percentTabian['pairPercentAngSon']['percentD'];

                                }

                                if (mb_substr($value->pairtype, 0, 1) == "R") {
                                    $percentNumberDR += $this->percentTabian['pairPercentAngSon']['percentR'];

                                }
                            }




                            break;
                    }


                    array_push(
                        $tem,
                        array(
                            "pairnumber" => $pair,
                            'percent' => $percentNumberDR,
                            "pairtype" => $value->pairtype,
                            "pairpoint" => (int) $value->pairpoint,
                            "miracledesc" => $value->miracledesc,
                            "miracledetail" => $value->vip_detail
                        )
                    );

                    break;
                }


            }

            $this->pairsMiracle = $tem;

            usort($this->pairsMiracle, function ($item1, $item2) {
                return $item2['pairpoint'] <=> $item1['pairpoint'];
            });


        }
    }


    private function setPairsUnique($pairsCarA, $pairsCarB, $pairSum, $pairSumLastNum)
    {
        $unique = array();

        foreach ($pairsCarA as $value) {
            array_push($unique, $value);
        }
        foreach ($pairsCarB as $value) {
            array_push($unique, $value);
        }
        array_push($unique, $pairSum);
        array_push($unique, $pairSumLastNum);

        //$this->pairsUnique = array_unique($unique);


        foreach (array_unique($unique) as $value) {
            array_push($this->pairsUnique, $value);
        }
    }


    private function setPairSum($carIdNumber)
    {

        $n = 0;
        foreach (str_split($carIdNumber, 1) as $value) {
            $n += (int) $value;
        }

        $this->pairSum = (string) $n;
    }


    private function setCarPairsB($carIdNumber)
    {
        $tem = array();
        $n = 3;
        $xMot = (strlen($carIdNumber) - 1) % 2;
        $countPair = floor((strlen($carIdNumber) - 1) / 2);

        $fPair = substr($carIdNumber, strlen($carIdNumber) - 1, 1);

        //echo $fPair;

        //array_push($tem, $fPair);


        for ($i = 0; $i < $countPair; $i++) {

            $pairJ = substr($carIdNumber, strlen($carIdNumber) - $n, 2);
            array_push($tem, $pairJ);

            $n += 2;

        }

        if ($xMot == 1) {
            $pair = substr($carIdNumber, 0, 1);
            array_push($tem, $pair);

        }

        for ($i = count($tem) - 1; $i >= 0; $i--) {
            array_push($this->pairsCarB, $tem[$i]);
        }

    }

    private function setCarPairsA($carIdNumber)
    {

        $tem = array();

        $n = 2;

        $countPair = floor(strlen($carIdNumber) / 2);
        $xMot = strlen($carIdNumber) % 2;

        //cut char right to left by 2 step คู่ท้ายทะเบียนจะอยู่ตำแหน่งแรก
        for ($i = 0; $i < $countPair; $i++) {

            $pairJ = substr($carIdNumber, strlen($carIdNumber) - $n, 2);
            array_push($tem, $pairJ);

            $n += 2;

        }


        //ถ้าจำนวนทะเบียนเป็นเลขคี่ ให้เอาตัวเศษไปต่อตูด
        if ($xMot == 1) {
            $pair = substr($carIdNumber, 0, 1);
            array_push($tem, $pair);

        }


        for ($i = count($tem) - 1; $i >= 0; $i--) {
            array_push($this->pairsCarA, $tem[$i]);
        }


    }

    private function changeTextToNumber($fullCarId)
    {

        $caridConvert = null;

        for ($i = 0; $i < mb_strlen($fullCarId); $i++) {
            $xString = mb_substr($fullCarId, $i, 1);

            if (is_numeric($xString)) {
                $caridConvert = $caridConvert . $xString;
            } else {
                foreach ($this->txt as $txtKey => $txtValue) {
                    if ($xString == $txtKey) {
                        $caridConvert = $caridConvert . $txtValue;
                        break;
                    }
                }
            }


        }


        $this->fullCarId = $caridConvert;

    }

    private function setPairsNum($carid)
    {
        $xString = '';
        for ($i = mb_strlen($carid) - 1; $i >= 0; $i--) {

            if (!is_numeric(mb_substr($carid, $i, 1))) {
                break;

            }
            $xString = $xString . mb_substr($carid, $i, 1);


        }

        $yString = '';
        for ($i = strlen($xString) + 1; $i >= 0; $i--) {
            $yString = $yString . substr($xString, $i, 1);
        }

        $this->numLast = $yString;


    }

    private function setCharKeyV()
    {
        $this->txt = array(
            'ก' => '1',
            'ด' => '1',
            'ถ' => '1',
            'ท' => '1',
            'ภ' => '1',
            'ฤ' => '1',
            'ฦ' => '1',
            'า' => '1',
            'ำ' => '1',
            'ุ' => '1',
            '่' => '1',
            'ข' => '2',
            'ง' => '2',
            'ช' => '2',
            'บ' => '2',
            'ป' => '2',
            'เ' => '2',
            'แ' => '2',
            'ู' => '2',
            '้' => '2',
            'ฆ' => '3',
            'ต' => '3',
            'ฑ' => '3',
            'ฒ' => '3',
            '๋' => '3',
            'ค' => '4',
            'ธ' => '4',
            'ญ' => '4',
            'ร' => '4',
            'ษ' => '4',
            'ะ' => '4',
            'โ' => '4',
            'ั' => '4',
            'ิ' => '4',
            'ฉ' => '5',
            'ฌ' => '5',
            'ณ' => '5',
            'น' => '5',
            'ม' => '5',
            'ห' => '5',
            'ฎ' => '5',
            'ฮ' => '5',
            'ฬ' => '5',
            'ึ' => '5',
            'จ' => '6',
            'ล' => '6',
            'ว' => '6',
            'อ' => '6',
            'ใ' => '6',
            'ซ' => '7',
            'ศ' => '7',
            'ส' => '7',
            '๊' => '7',
            'ี' => '7',
            'ื' => '7',
            'ผ' => '8',
            'ฝ' => '8',
            'พ' => '8',
            'ฟ' => '8',
            'ย' => '8',
            '็' => '8',
            'ฏ' => '9',
            'ฐ' => '9',
            'ไ' => '9',
            '์' => '9',
            'a' => '1',
            'i' => '1',
            'j' => '1',
            'q' => '1',
            'y' => '1',
            'A' => '1',
            'I' => '1',
            'J' => '1',
            'Q' => '1',
            'Y' => '1',
            'b' => '2',
            'k' => '2',
            'r' => '2',
            'B' => '2',
            'K' => '2',
            'R' => '2',
            'c' => '3',
            'g' => '3',
            'l' => '3',
            's' => '3',
            'C' => '3',
            'G' => '3',
            'L' => '3',
            'S' => '3',
            'd' => '4',
            'm' => '4',
            't' => '4',
            'D' => '4',
            'M' => '4',
            'T' => '4',
            'e' => '5',
            'h' => '5',
            'n' => '5',
            'x' => '5',
            'E' => '5',
            'H' => '5',
            'N' => '5',
            'X' => '5',
            'u' => '6',
            'v' => '6',
            'w' => '6',
            'U' => '6',
            'V' => '6',
            'W' => '6',
            'o' => '7',
            'z' => '7',
            'O' => '7',
            'Z' => '7',
            'f' => '8',
            'p' => '8',
            'F' => '8',
            'P' => '8'
        );
    }

    private function setPairSumLastNum($numLast)
    {
        $n = 0;

        for ($i = 0; $i < strlen($numLast); $i++) {
            $n += (int) substr($numLast, $i, 1);
        }

        $this->pairSumNumLast = (string) $n;

    }



    public function listTabianSell($request, $response)
    {
        $sql = "SELECT * FROM tabian_sell WHERE tabian_status = 'available' ORDER BY order_no ASC, tabian_id DESC";
        $stmt = $this->db->prepare($sql);
        $stmt->execute();
        $data = $stmt->fetchAll(\PDO::FETCH_OBJ);

        $response->getBody()->write(json_encode($data));
        return $response->withHeader('Content-Type', 'application/json');
    }

}