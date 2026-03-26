<?php

namespace App\Managers\Telephone;

use App\Daos\PairItemCollectionDao;
use App\Daos\PairItemDao;
use App\Managers\Manager;
use App\Managers\Telephone\Util\APIPhone;
use App\Managers\Telephone\Util\AppMessage;


class PairController extends Manager
{
    private $percentDup = 0;
    private $pointPercentAction = 0; //default 9
    private $point06 = 0;
    private $countPairZero = 0;
    private $pairScoreA0490R = 0;
    private $pairScoreA0490D = 0;
    private $pointDownD = 0;
    private $pointUpR = 0;

    private $point40 = 0;
    private $point50 = 0;

    private $rStatusLastPairA = false;//เพิ่มคะแนนร้าย -150
    private $rStatusPreLastPairA = false;
    private $rStatusLastPairB = false;

    private $rScoreLastPairA = -100;
    private $rScorePreLastPairA = -50;
    private $rScoreLastPairB = -50; //ลดคะแนนดี


    private $mainApi;
    private $xmessage;
    private $pairsA = array();
    private $pairsB = array();

    private $pairSum;

    private $pairsUnique = array();
    private $dao;
    private $dataSortByType = array();

    private $scoreByPairsCount = array();

    private $scoreByPairsPositionA = array();
    private $scoreByPairsPositionB = array();
    private $scoreByPairSum = array();

    private $scoreByContinueCountPairsA = array();
    private $scoreByContinueCountPairsB = array();

    private $scoreTotalOfTotal = array();
    private $percentTotalOfTotal = array();

    private $miracleSummary = array();

    private $specialGoal = array();

    private $scoreX = 0;  //เอาไปลบดี

    private $scorepairDup = 0;

    private $miScoreNumber69 = 0;
    private $miScoreNumber69AddR = 0;


    private $miScoreNumberDup44 = 0;
    private $miScoreNumberDup44AddR = 0;
    private $percentNumber44 = 0;
    private $percentNumber6996 = 0;


    public function mainPhone($request, $response, $args)
    {
        $this->mainApi = new APIPhone();
        $phoneNumber = $args['phoneNumber'];
        // $phoneNumber = $request->getAttribute('phoneNumber');
        if (is_numeric($phoneNumber) && strlen($phoneNumber) == 10) {
            //preparing
            $this->pairsA = $this->getPairsMain('A', $phoneNumber);
            $this->pairsB = $this->getPairsMain('B', $phoneNumber);
            $this->pairSum = $this->getPairSum($phoneNumber);
            $this->pairsUnique = $this->getPairsUnique($this->pairsA, $this->pairsB, $this->pairSum);

            $this->dao = $this->getDataMiracleDao();

            $this->dataSortByType = $this->getDataSortByType($this->dao);
            $sD = $this->getScoreByPairsCount('D', $this->dataSortByType, $this->pairsA, $this->pairsB, $this->pairSum);
            $sR = $this->getScoreByPairsCount('R', $this->dataSortByType, $this->pairsA, $this->pairsB, $this->pairSum);
            $this->scoreByPairsCount = array('scorePairCountD' => $sD, 'scorePairCountR' => $sR);

            $this->scoreByPairsPositionA = $this->getScoreByPairsPosition($this->pairsA, "A");
            $this->scoreByPairsPositionB = $this->getScoreByPairsPosition($this->pairsB, "B");
            $this->scoreByPairSum = $this->getScoreBySum($this->pairSum);

            $this->scoreByContinueCountPairsA = $this->getScoreByContinue($this->pairsA, 'A');
            $this->scoreByContinueCountPairsB = $this->getScoreByContinue($this->pairsB, 'B');

            $this->listPiarDup = $this->getPairDupScore($this->pairsA, $this->pairsB, $this->pairSum);
            $this->scorepairDup = $this->getScoreForPairDup($this->listPiarDup);

            $this->setScorePercentDRMiNumber($this->pairsA, $this->pairsB);

            $this->specialGoal = $this->getSpecailGoal($this->scoreByPairsPositionA, $this->scoreByPairSum, $this->scoreByContinueCountPairsA, $this->scoreByContinueCountPairsB);


            $this->countPairZero = $this->getPairsZero($this->pairsA, $this->pairsB);

            $this->scoreTotalOfTotal = $this->getScoreTotalOfTotal($this->scoreByPairsCount, $this->scoreByPairsPositionA, $this->scoreByPairsPositionB, $this->scoreByPairSum);


            $this->percentTotalOfTotal = $this->getPercentTotalOfTotal($this->dataSortByType);
            $this->miracleSummary = $this->getMiracleSummary($this->scoreTotalOfTotal);


            //set to main API
            $this->mainApi->pairsA = $this->pairsA;
            $this->mainApi->pairsB = $this->pairsB;
            $this->mainApi->pairSum = $this->pairSum;
            $this->mainApi->pairsUnique = $this->pairsUnique;

            $this->mainApi->dataSortByType = $this->dataSortByType;

            $this->mainApi->scoreByPairsCount = $this->scoreByPairsCount;
            $this->mainApi->scorePairByPositionA = $this->scoreByPairsPositionA;
            $this->mainApi->scorePairByPositionB = $this->scoreByPairsPositionB;
            $this->mainApi->scorePairBySum = $this->scoreByPairSum;

            $this->mainApi->scoreByContinueCountPairsA = $this->scoreByContinueCountPairsA;
            $this->mainApi->scoreByContinueCountPairsB = $this->scoreByContinueCountPairsB;




            $this->mainApi->pairDup = $this->listPiarDup;
            $this->mainApi->scoreDupMi = $this->scorepairDup;


            $this->mainApi->percentAfterDupAB = $this->percentAfterDupAB();
            $this->mainApi->percentAfterStickNum = array("percentNumber6996" => $this->percentNumber6996, "percentNumber44" => $this->percentNumber44);


            $this->mainApi->scoreTotalOfTotal = $this->scoreTotalOfTotal;

            $this->mainApi->percentTotalOfTotal = $this->percentTotalOfTotal;

            $this->mainApi->countPairZero = $this->countPairZero;
            $this->mainApi->miracleSummary = $this->miracleSummary;
            $this->mainApi->specialGoal = $this->specialGoal;

            $this->mainApi->pairSpecialX = array('countPairZero' => $this->countPairZero, 'scoreDupMi' => $this->scorepairDup, 'percentTotalOfTotal' => $this->percentTotalOfTotal, 'percentOriginD' => $this->getSumOriginPercent($this->dataSortByType, "D"), 'percentOriginR' => $this->getSumOriginPercent($this->dataSortByType, "R"));

            $this->mainApi->miPercentScoreNum = array('miScoreNumber69' => $this->miScoreNumber69, 'miScoreNumberDup44' => $this->miScoreNumberDup44, 'miScoreNumberDup44AddR' => $this->miScoreNumberDup44AddR, 'miScoreNumber69AddR' => $this->miScoreNumber69AddR);

            $this->mainApi->xmessage = 'prepare success';




            $response->getBody()->write(json_encode($this->mainApi));
            return $response->withHeader('Content-Type', 'application/json');


        } else {
            $this->xmessage = new AppMessage('prepare fail');
            $response->getBody()->write(json_encode($this->xmessage));
            return $response->withHeader('Content-Type', 'application/json');
        }

    }

    //ลด % เลขที่มี และเลขที่ซ้ำ
    private function setScorePercentDRMiNumber($pairsA, $pairsB)
    {

        $num6996 = array('69', '96');

        $num44 = '44';
        $count44 = 0;

        $count6996 = 0;


        foreach ($pairsA as $pair) {
            foreach ($num6996 as $num) {
                if ($pair == $num) {
                    $this->miScoreNumber69 = $this->miScoreNumber69 + 800;
                    $this->miScoreNumber69AddR = $this->miScoreNumber69AddR + 150;
                    $count6996++;

                }
            }

        }


        switch ($count6996) {
            case 1:
                $this->percentNumber6996 = 10;
                break;
            case 2:
                $this->percentNumber6996 = 15;
                break;
            case 3:
                $this->percentNumber6996 = 20;
                break;
            case 4:
                $this->percentNumber6996 = 25;
                break;
            default:
                break;
        }




        foreach ($pairsB as $pair) {
            if ($pair == $num44) {
                $count44++;
            }
        }

        switch ($count44) {
            case 2:
                $this->miScoreNumberDup44 = 350;
                $this->miScoreNumberDup44AddR = 100;
                $this->percentNumber44 = 10;
                break;
            case 3:
                $this->miScoreNumberDup44 = 450;
                $this->miScoreNumberDup44AddR = 150;
                $this->percentNumber44 = 15;
                break;
            case 4:
                $this->miScoreNumberDup44 = 550;
                $this->miScoreNumberDup44AddR = 200;
                $this->percentNumber44 = 18;
                break;
            case 5:
                $this->miScoreNumberDup44 = 650;
                $this->miScoreNumberDup44AddR = 250;
                $this->percentNumber44 = 21;
                break;
            case 6:
                $this->miScoreNumberDup44 = 750;
                $this->miScoreNumberDup44AddR = 300;
                $this->percentNumber44 = 23;
                break;
            default:
                break;
        }


    }

    private function getSumOriginPercent(array $datasortByType, string $percentType): int
    {

        $percentOrin = 0;

        foreach ($datasortByType as $value) {
            if (mb_substr($value->type, 0, 1) == $percentType) {
                $percentOrin = $percentOrin + $value->percentile;
            }

        }

        return $percentOrin;
    }

    private function getPairsZero($pairsA, $pairB): int
    {


        //เช็ค AB ยกเว้น ตำแหน่งที่ 0A

        $pairTaget = array('00', '01', '10', '02', '20', '04', '40', '05', '50', '06', '60', '09', '90');
        $countDupx = 0;
        foreach ($pairsA as $pair) {

            foreach ($pairTaget as $pairTarget) {
                if ($pair == $pairTarget) {
                    $countDupx = $countDupx + 1;
                }
            }
        }

        foreach ($pairB as $pair) {

            foreach ($pairTaget as $pairTarget) {
                if ($pair == $pairTarget) {
                    $countDupx = $countDupx + 1;
                }
            }
        }


        return $countDupx;
    }

    //ถ้าซ้ำเกิน 4 ตัวให้ +-15% ถ้าซ้ำเกินนั้นให้ไปลดคะแนน
    private function getScoreForPairDup($pairsDup): int
    {
        foreach ($pairsDup as $key => $value) {

            if ($value > 4) {
                $this->percentDup = 15;
            }

            if ($value > 5) {
                return 350;
            }

            if ($value > 4) {

                return 350;
            }

            if ($value > 3) {
                return 350;
            }

        }
        return 0;
    }

    private function getPairDupScore(array $pairsA, array $pairsB, string $pairSum): array
    {

        $arrxD = array(
            array('00', '00'),
            array('01', '10'),
            array('02', '20'),
            array('03', '30'),
            array('04', '40'),
            array('05', '50'),
            array('06', '60'),
            array('07', '70'),
            array('08', '80'),
            array('09', '90'),

            array('11', '11'),
            array('12', '21'),
            array('13', '31'),
            array('14', '41'),
            array('15', '51'),
            array('16', '61'),
            array('17', '71'),
            array('18', '81'),
            array('19', '91'),


            array('22', '22'),
            array('23', '32'),
            array('24', '42'),
            array('25', '52'),
            array('26', '62'),
            array('27', '72'),
            array('28', '82'),
            array('29', '92'),


            array('33', '33'),
            array('34', '43'),
            array('35', '53'),
            array('36', '63'),
            array('37', '73'),
            array('38', '83'),
            array('39', '93'),


            array('44', '44'),
            array('45', '54'),
            array('46', '64'),
            array('47', '74'),
            array('48', '84'),
            array('49', '94'),


            array('55', '55'),
            array('56', '65'),
            array('57', '75'),
            array('58', '85'),
            array('59', '95'),


            array('66', '66'),
            array('67', '76'),
            array('68', '86'),
            //array('69', '96'),


            array('77', '77'),
            array('78', '87'),
            array('79', '97'),


            array('88', '88'),
            array('89', '98'),


            array('99', '99')
        );

        $arrCountX = array();


        $pairs = array();

        foreach ($pairsA as $value) {
            array_push($pairs, $value);
        }


        foreach ($pairsB as $value) {
            array_push($pairs, $value);
        }


        $paircount = array_count_values($pairs);


        foreach ($arrxD as $number) {

            foreach ($paircount as $pair => $amount) {

                if ($number[0] == $pair | $number[1] == $pair) {

                    $arrCountX[$number[0] . $number[1]][] = $amount;
                }


            }
        }

        $arrSuccessDup = array();


        foreach ($arrCountX as $number2d => $arrPoint) {
            $xDup = 0;
            foreach ($arrPoint as $dup) {
                $xDup = $xDup + $dup;
            }

            $arrSuccessDup[$number2d] = $xDup;
        }



        return $arrSuccessDup;


    }


    private function getPairsMain(string $type, string $phoneNumber): array
    {
        $pairsX = array();
        if ($type == 'A') {
            $pairsX = str_split($phoneNumber, 2);
        }

        if ($type == 'B') {
            array_push($pairsX, substr($phoneNumber, 1, 2));
            array_push($pairsX, substr($phoneNumber, 3, 2));
            array_push($pairsX, substr($phoneNumber, 5, 2));
            array_push($pairsX, substr($phoneNumber, 7, 2));
        }

        return $pairsX;
    }


    private function getScoreByContinue(array $pairs, string $type): array
    {


        $countD = 0;
        $countR = 0;

        if ($type == "A") {

            if ($this->scoreByPairsPositionA[4]['type'][0] === "D") {
                $countD++;
                if ($this->scoreByPairsPositionA[3]['type'][0] === "D") {
                    $countD++;
                    if ($this->scoreByPairsPositionA[2]['type'][0] === "D") {
                        $countD++;
                        if ($this->scoreByPairsPositionA[1]['type'][0] === "D") {
                            $countD++;
                            if ($this->scoreByPairsPositionA[0]['type'][0] === "D") {
                                $countD++;
                            }
                        }
                    }
                }
            }

            if ($this->scoreByPairsPositionA[4]['type'][0] === "R") {

                $countR++;
                if ($this->scoreByPairsPositionA[3]['type'][0] === "R") {
                    $countR++;
                    if ($this->scoreByPairsPositionA[2]['type'][0] === "R") {
                        $countR++;
                        if ($this->scoreByPairsPositionA[1]['type'][0] === "R") {
                            $countR++;
                            if ($this->scoreByPairsPositionA[0]['type'][0] === "R") {
                                $countR++;
                            }
                        }
                    }
                }
            }


        }


        if ($type == "B") {


            if ($this->scoreByPairsPositionB[3]['type'][0] == "D") {
                $countD++;
                if ($this->scoreByPairsPositionB[2]['type'][0] == "D") {
                    $countD++;
                    if ($this->scoreByPairsPositionB[1]['type'][0] == "D") {
                        $countD++;
                        if ($this->scoreByPairsPositionB[0]['type'][0] == "D") {
                            $countD++;
                        }
                    }
                }
            }

            if ($this->scoreByPairsPositionB[3]['type'][0] == "R") {
                $countR++;
                if ($this->scoreByPairsPositionB[2]['type'][0] == "R") {
                    $countR++;
                    if ($this->scoreByPairsPositionB[1]['type'][0] == "R") {
                        $countR++;
                        if ($this->scoreByPairsPositionB[0]['type'][0] == "R") {
                            $countR++;
                        }
                    }
                }
            }


        }

        return array('pairContinueD' => $countD, 'pairContinueR' => $countR);
    }


    private function getPointRx($type)
    {
        if ($type == "R10") {
            $this->pointDownD -= 500;
            $this->pointUpR -= 50;
        }

        if ($type == "R7") {
            $this->pointDownD -= 200;
            $this->pointUpR -= 50;
        }

        if ($type == "R5") {
            $this->pointDownD -= 100;
            $this->pointUpR -= 50;
        }
    }

    private function getScoreBySum($pair): array
    {
        foreach ($this->dao->pairsItemMiracleDao as $value) {
            if ($value->number === $pair) {


                if ($pair == '40') {
                    $this->point40 = 400; //เอาไปลบออกจาก D
                }

                if ($pair == '50') {
                    $this->point50 = 200; //เอาไปลบออกจาก D
                }


                switch ($value->type) {
                    case 'D10':
                        return array('pair' => $pair, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 209);
                        break;

                    case 'D8':
                        return array('pair' => $pair, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 80);
                        break;

                    case 'D5':

                        return array('pair' => $pair, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 50);
                        break;

                    case 'R10':

                        $this->scoreX = 400; //เอาไปลบดี
                        return array('pair' => $pair, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -300);
                        break;

                    case 'R7':

                        $this->scoreX = 300;
                        return array('pair' => $pair, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -200);
                        break;

                    case 'R5':

                        $this->scoreX = 200;
                        return array('pair' => $pair, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -100);
                        break;
                }
            }
        }
    }

    private function getScoreByPairsPosition(array $pairs, $type): array
    {
        $pairScore = array();

        $a0490D = -350;
        $a0490R = -100;
        $numSpecial = array('04', '40', '05', '50', '09', '90', '60');
        $pairsPercentAction = array('05', '50', '04', '40', '06', '60', '09', '90');
        $pointPercentAction = 9;


        foreach ($pairs as $key => $v) {
            switch ($key) {
                case 4:

                    if ($type == 'A' || $type == 'B') {
                        foreach ($pairsPercentAction as $pair) {
                            if ($v == $pair) {
                                $this->pointPercentAction = $this->pointPercentAction + $pointPercentAction;
                            }
                        }
                    }


                    if ($type == 'A' || $type == 'B') {
                        foreach ($numSpecial as $number) {
                            if ($v == $number) {
                                $this->pairScoreA0490D = $this->pairScoreA0490D + $a0490D;
                                $this->pairScoreA0490R = $this->pairScoreA0490R + $a0490R;
                            }
                        }
                    }


                    foreach ($this->dao->pairsItemMiracleDao as $value) {
                        if ($value->number === $v) {

                            switch ($value->type) {
                                case 'D10':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 100));
                                    break;

                                case 'D8':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 80));
                                    break;

                                case 'D5':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 50));
                                    break;

                                case 'R10':

                                    if ($type == 'B') {
                                        $this->getPointRx('R10');

                                    }

                                    if ($type == 'A') {
                                        $this->getPointRx('R10');

                                    }

                                    $this->rStatusLastPairA = true;
                                    $this->rScoreLastPairA -= 200;

                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -100));
                                    break;

                                case 'R7':

                                    $this->getPointRx('R7');

                                    $this->rStatusLastPairA = true;
                                    $this->rScoreLastPairA -= 100;
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -70));
                                    break;

                                case 'R5':

                                    $this->getPointRx('R5');
                                    $this->rStatusLastPairA = true;
                                    $this->rScoreLastPairA -= 100;
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -50));
                                    break;
                            }
                        }
                    }
                    break;

                case 3:

                    if ($type == 'A' || $type == 'B') {
                        foreach ($pairsPercentAction as $pair) {
                            if ($v == $pair) {
                                $this->pointPercentAction = $this->pointPercentAction + $pointPercentAction;
                            }
                        }
                    }

                    if ($type == 'A' || $type == 'B') {
                        foreach ($numSpecial as $number) {
                            if ($v == $number) {
                                $this->pairScoreA0490D = $this->pairScoreA0490D + $a0490D;
                                $this->pairScoreA0490R = $this->pairScoreA0490R + $a0490R;
                            }
                        }
                    }


                    foreach ($this->dao->pairsItemMiracleDao as $value) {
                        if ($value->number === $v) {
                            switch ($value->type) {
                                case 'D10':
                                    $point = 0;
                                    if ($type == 'B') {
                                        $point = 300;
                                    }
                                    if ($type == 'A') {
                                        $point = 80;
                                    }


                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => $point));
                                    break;

                                case 'D8':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 60));
                                    break;

                                case 'D5':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 30));
                                    break;

                                case 'R10':
                                    if ($type == 'A' || 'B') {
                                        $this->getPointRx('R10');

                                    }

                                    $this->rStatusPreLastPairA = true;
                                    ($type == 'B') ? $this->rStatusLastPairB = true : $this->rStatusLastPairB = false;

                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -20));
                                    break;

                                case 'R7':

                                    $this->getPointRx('R7');

                                    $this->rStatusPreLastPairA = true;

                                    ($type == 'B') ? $this->rStatusLastPairB = true : $this->rStatusLastPairB = false;

                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -50));
                                    break;

                                case 'R5':

                                    $this->getPointRx('R5');

                                    $this->rStatusPreLastPairA = true;

                                    ($type == 'B') ? $this->rStatusLastPairB = true : $this->rStatusLastPairB = false;

                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -30));
                                    break;
                            }
                        }
                    }
                    break;

                case 2:

                    if ($type == 'A' || $type == 'B') {
                        foreach ($pairsPercentAction as $pair) {
                            if ($v == $pair) {
                                $this->pointPercentAction = $this->pointPercentAction + $pointPercentAction;
                            }
                        }
                    }

                    if ($type == 'A' || $type == 'B') {
                        foreach ($numSpecial as $number) {
                            if ($v == $number) {
                                $this->pairScoreA0490D = $this->pairScoreA0490D + $a0490D;
                                $this->pairScoreA0490R = $this->pairScoreA0490R + $a0490R;
                            }
                        }
                    }
                    foreach ($this->dao->pairsItemMiracleDao as $value) {
                        if ($value->number === $v) {
                            switch ($value->type) {
                                case 'D10':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 60));
                                    break;

                                case 'D8':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 40));
                                    break;

                                case 'D5':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 10));
                                    break;

                                case 'R10':

                                    $this->getPointRx('R10');

                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -60));
                                    break;

                                case 'R7':

                                    $this->getPointRx('R7');

                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -30));
                                    break;

                                case 'R5':

                                    $this->getPointRx('R5');

                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -0));
                                    break;
                            }
                        }
                    }
                    break;

                case 1:

                    if ($type == 'A' || $type == 'B') {
                        foreach ($pairsPercentAction as $pair) {
                            if ($v == $pair) {
                                $this->pointPercentAction = $this->pointPercentAction + $pointPercentAction;
                            }
                        }
                    }

                    if ($type == 'A' || $type == 'B') {
                        foreach ($numSpecial as $number) {
                            if ($v == $number) {
                                $this->pairScoreA0490D = $this->pairScoreA0490D + $a0490D;
                                $this->pairScoreA0490R = $this->pairScoreA0490R + $a0490R;
                            }
                        }
                    }
                    foreach ($this->dao->pairsItemMiracleDao as $value) {
                        if ($value->number === $v) {
                            switch ($value->type) {
                                case 'D10':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 40));
                                    break;

                                case 'D8':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 20));
                                    break;

                                case 'D5':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 5));
                                    break;

                                case 'R10':

                                    $this->getPointRx('R10');

                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -40));
                                    break;

                                case 'R7':

                                    $this->getPointRx('R7');

                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -10));
                                    break;

                                case 'R5':

                                    $this->getPointRx('R5');

                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -0));
                                    break;
                            }
                        }
                    }
                    break;

                case 0:

                    if ($type == 'B') {
                        foreach ($pairsPercentAction as $pair) {
                            if ($v == $pair) {
                                $this->pointPercentAction = $this->pointPercentAction + $pointPercentAction;
                            }
                        }
                    }


                    foreach ($this->dao->pairsItemMiracleDao as $value) {
                        if ($value->number === $v) {
                            if ($type == 'A') {
                                if ($value->number == '06') {
                                    $this->point06 = 55;
                                }
                            }


                            switch ($value->type) {
                                case 'D10':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 20));
                                    break;

                                case 'D8':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 10));
                                    break;

                                case 'D5':
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => 0));
                                    break;

                                case 'R10':


                                    if ($type == 'A') {
                                        $this->getPointRx('R10');

                                    }


                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -20));
                                    break;

                                case 'R7':

                                    $this->getPointRx('R7');
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -5));
                                    break;

                                case 'R5':

                                    $this->getPointRx('R5');
                                    array_push($pairScore, array('pair' => $v, 'type' => $value->type, 'point' => $value->point, 'bonusScore' => -0));
                                    break;
                            }
                        }
                    }
                    break;
            }


        }


        return $pairScore;
    }

    private function getScoreByPairsCount(string $typePair, array $pairsDataSortByType, array $pairsA, array $pairsB, string $pairSum): int
    {
        $score = 0;

        foreach ($pairsA as $item) {
            foreach ($pairsDataSortByType as $obj) {
                if ($item === $obj->number && $obj->type[0] === $typePair) {
                    $score = $score + $obj->point;
                    //echo $item . ' + ' . $obj->point . ' = ' . $score . '<BR>';
                }
            }
        }
        foreach ($pairsB as $item) {
            foreach ($pairsDataSortByType as $obj) {
                if ($item === $obj->number && $obj->type[0] === $typePair) {
                    $score = $score + $obj->point;
                    //echo $item . ' + ' . $obj->point . ' = ' . $score . '<BR>';
                }
            }
        }

        foreach ($pairsDataSortByType as $obj) {
            if ($pairSum === $obj->number && $obj->type[0] === $typePair) {
                $score = $score + $obj->point;
                //echo $pairSum . ' + ' . $obj->point . ' = ' . $score . '<BR>';
            }
        }

        return $score;
    }

    private function getPairSum($phonenum): string
    {

        $pairs = str_split($phonenum, 1);
        $sumPair = 0;
        foreach ($pairs as $value) {
            $sumPair += (int) $value;
        }

        return (string) $sumPair;


    }

    private function getPairsUnique(array $pairsA, array $pairsB, $pairSum): array
    {
        $pairs = array();
        $pairsOut = array();

        foreach ($pairsA as $pA) {
            array_push($pairs, $pA);
        }

        foreach ($pairsB as $pB) {
            array_push($pairs, $pB);
        }

        array_push($pairs, $pairSum);

        foreach (array_count_values($pairs) as $key => $value) {
            array_push($pairsOut, (string) $key);
        }

        return $pairsOut;
    }


    private function getDataSortByType(PairItemCollectionDao $dataMiracleDao): array
    {
        $dataDaoD10 = array();
        $dataDaoD8 = array();
        $dataDaoD5 = array();

        $dataDaoR10 = array();
        $dataDaoR7 = array();
        $dataDaoR5 = array();

        $dataDaoAll = array();
        foreach ($dataMiracleDao->pairsItemMiracleDao as $value) {
            if ($value->type == 'D10') {
                array_push($dataDaoD10, $value);
            }

            if ($value->type == 'D8') {
                array_push($dataDaoD8, $value);
            }

            if ($value->type == 'D5') {
                array_push($dataDaoD5, $value);
            }

            if ($value->type == 'R10') {
                array_push($dataDaoR10, $value);
            }

            if ($value->type == 'R7') {
                array_push($dataDaoR7, $value);
            }

            if ($value->type == 'R5') {
                array_push($dataDaoR5, $value);
            }
        }

        foreach ($dataDaoD10 as $value) {
            array_push($dataDaoAll, $value);
        }

        foreach ($dataDaoD8 as $value) {
            array_push($dataDaoAll, $value);
        }

        foreach ($dataDaoD5 as $value) {
            array_push($dataDaoAll, $value);
        }

        foreach ($dataDaoR10 as $value) {
            array_push($dataDaoAll, $value);
        }

        foreach ($dataDaoR7 as $value) {
            array_push($dataDaoAll, $value);
        }

        foreach ($dataDaoR5 as $value) {
            array_push($dataDaoAll, $value);
        }


        return $dataDaoAll;
    }

    private function getDataMiracleDao(): PairItemCollectionDao
    {
        $arrItems = array();

        $sql = "SELECT * FROM numbers ORDER BY pairnumberid ASC ";
        $result = $this->db->prepare($sql);
        $result->execute();
        $dataMiracle = $result->fetchAll(\PDO::FETCH_OBJ);


        if (is_array($dataMiracle) && count($dataMiracle) > 0) {
            foreach ($this->pairsUnique as $v) {
                foreach ($dataMiracle as $value) {
                    if ($v === $value->pairnumber) {
                        array_push($arrItems, new PairItemDao($value->pairnumber, $value->miracledesc, $this->getPercentByPair($value->pairnumber), $value->pairtype, (int) $value->pairpoint, $value->detail_vip));
                        break;
                    }
                }
            }


        }

        return new PairItemCollectionDao($arrItems);
    }

    private function getPercentByPair($pair): int
    {

        $percentSum = 20;

        $percentA5 = 20;
        $percentA4 = 15;
        $percentA3 = 10;
        $percentA2 = 5;
        $percentA1 = 5;

        $percentB4 = 12;
        $percentB3 = 5;
        $percentB2 = 5;
        $percentB1 = 3;


        $percentNumber = 0;

        for ($i = 0; $i < count($this->pairsA); $i++) {
            if ($pair === $this->pairsA[$i]) {
                switch ($i) {
                    case 0:
                        $percentNumber += $percentA1;
                        break;
                    case 1:
                        $percentNumber += $percentA2;
                        break;
                    case 2:
                        $percentNumber += $percentA3;
                        break;
                    case 3:
                        $percentNumber += $percentA4;
                        break;
                    case 4:
                        $percentNumber += $percentA5;
                        break;
                    default;

                }
            }
        }

        for ($i = 0; $i < count($this->pairsB); $i++) {
            if ($pair === $this->pairsB[$i]) {
                switch ($i) {
                    case 0:
                        $percentNumber += $percentB1;
                        break;
                    case 1:
                        $percentNumber += $percentB2;
                        break;
                    case 2:
                        $percentNumber += $percentB3;
                        break;
                    case 3:
                        $percentNumber += $percentB4;
                        break;

                    default;

                }
            }
        }

        if ($pair === $this->pairSum) {
            $percentNumber += $percentSum;
        }


        return $percentNumber;

    }

    private function getScoreTotalOfTotal(array $scoreByPairsCount, array $scoreByPairsPositionA, array $scoreByPairsPositionB, array $scoreByPairSum): array
    {
        $scoreTotalD = 0;
        $scoreTotalR = 0;

        $raiStatus = 0;
        $xScore = 0;

        $raiSpecial = ($this->rStatusLastPairA == true) ? $this->rScoreLastPairA : 0;

        $addRaiSpecial = ($this->rStatusLastPairA == true) ? 150 : 0;

        $doublePrelastPair = ($this->rStatusPreLastPairA == true && $this->rStatusLastPairA == true) ? $this->rScorePreLastPairA : 0;

        $raiSpecailLastPairB = ($this->rStatusLastPairB) ? $this->rScoreLastPairB : 0;


        if ($scoreByPairSum['type'][0] == 'R') {
            $scoreTotalR += $scoreByPairSum['point'] + $scoreByPairSum['bonusScore'];
            $raiStatus = 1;

        }

        if ($scoreByPairSum['type'][0] == 'D') {
            $scoreTotalD += $scoreByPairSum['point'] + $scoreByPairSum['bonusScore'];

        }


        foreach ($scoreByPairsPositionA as $k => $value) {
            if ($value['point'] > 0) {
                $scoreTotalD = $scoreTotalD + $value['point'];
            } else {
                $scoreTotalR = $scoreTotalR + $value['point'];
            }
            if ($value['bonusScore'] > 0) {
                $scoreTotalD = $scoreTotalD + $value['bonusScore'];
            } else {
                $scoreTotalR = $scoreTotalR + $value['bonusScore'];
            }
        }

        foreach ($scoreByPairsPositionB as $k => $value) {
            if ($value['point'] > 0) {
                $scoreTotalD = $scoreTotalD + $value['point'];
            } else {
                $scoreTotalR = $scoreTotalR + $value['point'];
            }
            if ($value['bonusScore'] > 0) {
                $scoreTotalD = $scoreTotalD + $value['bonusScore'];
            } else {
                $scoreTotalR = $scoreTotalR + $value['bonusScore'];
            }
        }


        if ($raiStatus == 1) {
            $xScore = $this->scoreX;  //เอาไปลบดี
        }


        $scoreTotalD = $scoreTotalD + $scoreByPairsCount['scorePairCountD'] - $xScore + $raiSpecial + $doublePrelastPair + $raiSpecailLastPairB - $this->point40 - $this->point50 + $this->pointDownD - $this->scorepairDup + $this->pairScoreA0490D;


        $scoreTotalR = $scoreTotalR + $scoreByPairsCount['scorePairCountR'] - $addRaiSpecial + $this->pointUpR + $this->pairScoreA0490R + $this->point06;




        return array('scoreTotalD' => $scoreTotalD - $this->miScoreNumberDup44 - $this->miScoreNumber69, 'scoreTotalR' => $scoreTotalR - $this->miScoreNumber69AddR - $this->miScoreNumberDup44AddR);

    }

    private function getPercentTotalOfTotal(array $dataSortByType): array
    {
        $percentD = 0;
        $percentR = 0;


        foreach ($dataSortByType as $k => $value) {
            if ($value->percentile > 0 && $value->type[0] == 'D') {
                $percentD = $percentD + $value->percentile;
            } else {
                $percentR = $percentR + $value->percentile;
            }
        }


        if ($percentR == 0 && $percentD == 100) {
            $percentR = $percentR + $this->pointPercentAction;
            $percentD = $percentD - $this->pointPercentAction;
        }

        for ($i = 1; $i < 14; $i++) {
            if ($percentR == $i) {
                $percentR = $percentR + $this->pointPercentAction;
                $percentD = $percentD - $this->pointPercentAction;
            }
        }


        $percentD = $percentD - $this->percentDup - $this->percentNumber6996 - $this->percentNumber44;
        $percentR = $percentR + $this->percentDup + $this->percentNumber6996 + $this->percentNumber44;


        return array('percentTotalD' => $percentD, 'percentTotalR' => $percentR);
    }

    private function percentAfterDupAB(): int
    {
        return $this->percentDup;
    }

    private function getMiracleSummary(array $scoreTotalOfTotal): array
    {

        $scoreD = $scoreTotalOfTotal['scoreTotalD'];

        if ($scoreTotalOfTotal['scoreTotalD'] >= 1500) {
            $miracleD = "เบอร์นี้ดี = " . number_format($scoreD) . " คะแนน เบอร์ดีมาก เบอร์มหามงคลหายากเสริมดวงดีแท้ ***แต่ถ้าคะแนนร้ายได้เกิน -350 คะแนน ควรเปลี่ยนเบอร์ทันที";
        } elseif ($scoreTotalOfTotal['scoreTotalD'] >= 924) {
            $miracleD = "เบอร์นี้ดี = " . number_format($scoreD) . " คะแนน เบอร์ดีมาก เบอร์มหามงคลหายากเสริมดวงดีแท้ ***แต่ถ้าคะแนนร้ายได้เกิน -350 คะแนน ควรเปลี่ยนเบอร์ทันที";
        } elseif ($scoreTotalOfTotal['scoreTotalD'] >= 654) {
            $miracleD = "เบอร์นี้ดี = " . number_format($scoreD) . " คะแนน ดีเป็นเบอร์ มงคล แต่ควรดูความหมายเลขและเลขผลร้ายประกอบให้ดี ***ถ้าคะแนนร้ายได้เกิน -350 คะแนน ควรเปลี่ยนเบอร์ทันที";
        } elseif ($scoreTotalOfTotal['scoreTotalD'] >= 540) {
            $miracleD = "เบอร์นี้ดี = " . number_format($scoreD) . " คะแนน ดี แต่เสี่ยง ควรดูความหมายตัวเลขและคะแนนร้ายประกอบให้ดี ***ถ้าคะแนนร้ายได้เกิน -350 คะแนน ควรเปลี่ยนเบอร์ทันที";
        } elseif ($scoreTotalOfTotal['scoreTotalD'] >= 450) {
            $miracleD = "เบอร์นี้ดี = " . number_format($scoreD) . " คะแนน ปานกลาง เสี่ยงมาก ***ถ้าคะแนนร้ายได้เกิน -350 คะแนน ควรเปลี่ยนเบอร์ทันที";
        } elseif ($scoreTotalOfTotal['scoreTotalD'] >= 350) {
            $miracleD = "เบอร์นี้ดี = " . number_format($scoreD) . " คะแนน พอใช้ เสี่ยงมาก ***ถ้าคะแนนร้ายได้เกิน -350 คะแนน ควรเปลี่ยนเบอร์ทันที";
        } elseif ($scoreTotalOfTotal['scoreTotalD'] >= 250) {
            $miracleD = "เบอร์นี้ดี = " . number_format($scoreD) . " คะแนน พอใช้ เสี่ยงมาก ***ถ้าคะแนนร้ายได้เกิน -350 คะแนน ควรเปลี่ยนเบอร์ทันที";
        } else {
            $miracleD = "อันตรายมากๆ ควรสวดมนต์ทำบุญบ่อยๆและควรรีบเปลี่ยนเบอร์";
        }


        $scoreR = $scoreTotalOfTotal['scoreTotalR'];

        if ($scoreR <= -500) {
            $miracleR = "เบอร์นี้ร้าย = " . number_format($scoreR) . " คะแนน อันตรายมาก! หายนะรออยู่เบื้องหน้า ควรเปลี่ยนเบอร์ทันที";
        } elseif ($scoreR <= -400) {
            $miracleR = "เบอร์นี้ร้าย " . number_format($scoreR) . " คะแนน อันตรายมาก! ควรเปลี่ยนเบอร์ทันที";
        } elseif ($scoreR <= -300) {
            $miracleR = "เบอร์นี้ร้าย = " . number_format($scoreR) . " คะแนน อันตราย! เสี่ยงมาก! จงระวังอย่าประมาท ต้องรอบคอบหมั่นทำบุญ";
        } elseif ($scoreR <= -200) {
            $miracleR = "เบอร์นี้ร้าย = " . number_format($scoreR) . " คะแนน อันตราย! เสี่ยง! อย่าประมาท ทำบุญเสริมจะดีไม่ตก";
        } elseif ($scoreR <= -100) {
            $miracleR = "เบอร์นี้ร้าย = " . number_format($scoreR) . " คะแนน สมดุลชีวิตทำบุญเสริมจะได้ดีราบรื่นตลอด";
        } elseif ($scoreR <= -20) {
            $miracleR = "เบอร์นี้ร้าย = " . number_format($scoreR) . " คะแนน ไม่มีอิทธิพล ทำบุญเสริมราบรื่นตลอด";
        } else {
            $miracleR = "เบอร์นี้ร้าย = " . number_format($scoreR) . " คะแนน ไม่มีอิทธิพล ทำบุญเสริมราบรื่นตลอด";
        }

        return array('miracleD' => $miracleD, 'miracleR' => $miracleR);

    }

    private function getSpecailGoal(array $scoreByPairsPositionA, array $scoreByPairSum, array $scoreByContinueCountPairsA, array $scoreByContinueCountPairsB): array
    {

        $specialPairContinue = 'NO';

        switch ($scoreByPairSum['type']) {
            case 'D10':
                $specialPairSum = 'A';
                break;
            case 'D8':
                $specialPairSum = 'B';
                break;
            case 'D5':
                $specialPairSum = 'C';
                break;
            default:
                $specialPairSum = 'F';
                break;
        }

        switch ($scoreByPairsPositionA[4]['type']) {
            case 'D10':
                $specialPairLast = 'A';
                break;
            case 'D8':
                $specialPairLast = 'B';
                break;
            case 'D5':
                $specialPairLast = 'C';
                break;
            default:
                $specialPairLast = 'F';
                break;
        }

        if ($scoreByContinueCountPairsA['pairContinueD'] > 0 && $scoreByContinueCountPairsB['pairContinueD'] > 0) {
            $specialPairContinue = 'YES';
        }

        return array('specialPairSum' => $specialPairSum, 'specialPairLast' => $specialPairLast, 'specialPairContinue' => $specialPairContinue);
    }


}























































