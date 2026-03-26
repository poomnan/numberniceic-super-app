<?php

namespace App\Managers;

class NameController extends Manager
{

    private $apiName;
    private $birthDay;
    private $kName = array();
    private $aName = array(), $aSurName = array();
    private $dealSatNameFormat = array(), $dealSatSurNameFormat = array();

    private $shaName = array(), $shaSurName = array();
    private $shaNameDealFormat = array(), $shaSurNameDealFormat = array();

    private $sumShaName = 0, $sumShaSurName = 0, $sumShaNameSurName = 0;
    private $sumSatName = 0, $sumSatSurName = 0, $sumSatNameSurName = 0;

    private $ayadName = '', $ayadSurName = '', $ayadNameSurName = '';

    private $pairShaName = '', $pairShaSurName = '', $pairShaNameSurName = '';
    private $pairSatName = '', $pairSatSurName = '', $pairSatNameSurName = '';

    private $pairsUnique = array();

    private $pairsMiracle = array();
    private $name;
    private $surName;



    public function main($request, $response, $args)
    {
        $this->setNameRequest($request, $args);


        $this->setKarakini($this->birthDay, $this->aName, $this->aSurName);
        $this->setShaName($this->aName, $this->aSurName);
        $this->setSumSha($this->shaName, $this->shaSurName);
        $this->setSumSat($this->aName, $this->aSurName);

        $this->ayadName = $this->getAyad($this->sumShaName);
        $this->ayadSurName = $this->getAyad($this->sumShaSurName);

        $this->ayadNameSurName = $this->getAyad(($this->sumShaName + $this->sumShaSurName));

        $this->pairShaName = $this->getPairNum($this->sumShaName);
        $this->pairShaSurName = $this->getPairNum($this->sumShaSurName);

        $this->pairShaNameSurName = $this->getPairNum(($this->sumShaName + $this->sumShaSurName));

        $this->pairSatName = $this->getPairNum($this->sumSatName);
        $this->pairSatSurName = $this->getPairNum($this->sumSatSurName);

        $this->pairSatNameSurName = $this->getPairNum(($this->sumSatName + $this->sumSatSurName));

        $this->pairsUnique = $this->getPairsUnique($this->pairSatName, $this->pairSatSurName, $this->pairSatNameSurName, $this->pairShaName, $this->pairShaSurName, $this->pairShaNameSurName);

        $this->pairsMiracle = $this->getPairsMiracle($this->pairsUnique);


        $this->apiName = array(
            'birthDay' => $this->birthDay,
            'name' => $this->name,
            'surname' => $this->surName,

            'sumSatName' => $this->sumSatName,
            'sumSatSurName' => $this->sumSatSurName,
            'sumSatNameSurName' => $this->sumSatNameSurName,
            'pairSatName' => $this->pairSatName,
            'pairSatSurName' => $this->pairSatSurName,
            'pairSatNameSurName' => $this->pairSatNameSurName,

            'kName' => $this->kName,


            'sumShaName' => $this->sumShaName,
            'sumShaSurName' => $this->sumShaSurName,
            'sumShaNameSurName' => $this->sumShaNameSurName,

            'pairShaName' => $this->pairShaName,
            'pairShaSurName' => $this->pairShaSurName,

            'pairShaNameSurName' => $this->pairShaNameSurName,

            'ayadName' => $this->ayadName,
            'ayadSurName' => $this->ayadSurName,
            'ayadNameSurName' => $this->ayadNameSurName,


            'pairsUnique' => $this->pairsUnique,


            'satName' => $this->dealSatNameFormat,
            'satSurName' => $this->dealSatSurNameFormat,
            'shaName' => $this->shaNameDealFormat,
            'shaSurName' => $this->shaSurNameDealFormat,

            'pairsMiracle' => $this->pairsMiracle,
        );

        $response->getBody()->write(json_encode($this->apiName));
        return $response->withHeader('Content-Type', 'application/json');
    }

    private function setNameRequest($request, $args)
    {
        $strBirthDay = preg_replace('/\s+/', '', $args['birthday']);
        $strName = preg_replace('/\s+/', '', $args['name']);
        $strSurName = preg_replace('/\s+/', '', $args['surname']);
        // $strBirthDay = preg_replace('/\s+/', '', $request->getAttribute('birthday'));
        // $strName = preg_replace('/\s+/', '', $request->getAttribute('name'));
        // $strSurName = preg_replace('/\s+/', '', $request->getAttribute('surname'));


        $this->birthDay = $strBirthDay;
        $this->name = $strName;
        $this->surName = $strSurName;


        $this->setNameToNum($strName, $strSurName);


    }

    private function setNameToNum($name, $surName)
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


        for ($i = 0; $i < strlen($name); $i++) {
            $xString = mb_substr($name, $i, 1);

            foreach ($txt as $kText => $vText) {
                if ($kText == $xString) {

                    array_push($this->aName, array($kText => $vText));
                    array_push($this->dealSatNameFormat, array('xChar' => $kText, 'xNum' => $vText));

                }
            }

        }


        for ($i = 0; $i < strlen($surName); $i++) {
            $xString = mb_substr($surName, $i, 1);

            foreach ($txt as $kText => $vText) {
                if ($kText == $xString) {

                    array_push($this->aSurName, array($kText => $vText));
                    array_push($this->dealSatSurNameFormat, array('xChar' => $kText, 'xNum' => $vText));

                }
            }

        }




    }

    private function setKarakini(string $aBirthDay, array $aName, array $aSurName)
    {

        $txt_kday = array(
            'sunday' => 'ศษสหฬฮ',
            'monday' => 'ะ้ื์แ๊็าิึี่ำุูเใไโอ',
            'tuesday' => 'กขคฆง',
            'wednesday1' => 'จฉชซฌญ',
            'wednesday2' => 'บปผฝพฟภม',
            'thursday' => 'ดตถทธน',
            'friday' => 'ยรลว',
            'saturday' => 'ฎฏฐฑฒณ',
        );

        foreach ($txt_kday as $kD => $kV) {
            if ($aBirthDay == $kD) {
                for ($i = 0; $i < strlen($kV); $i++) {
                    $strK = mb_substr($kV, $i, 1);

                    foreach ($aName as $aK => $aV) {
                        foreach ($aV as $k => $v) {
                            if ($strK == $k) {
                                array_push($this->kName, $strK);
                                break;
                            }
                        }

                    }

                    foreach ($aSurName as $aK => $aV) {
                        foreach ($aV as $k => $v) {
                            if ($strK == $k) {
                                array_push($this->kName, $strK);
                                break;
                            }
                        }
                    }



                }// loop for birthday

                break;
            }
        }


    }

    private function setShaName(array $aName, array $aSurName)
    {
        $txt_star = array(
            'อ' => '6',
            'ะ' => '6',
            'า' => '6',
            'ิ' => '6',
            'ี' => '6',
            'ุ' => '6',
            'ู' => '6',
            'เ' => '6',
            'โ' => '6',
            'ก' => '15',
            'ข' => '15',
            'ค' => '15',
            'ฆ' => '15',
            'ง' => '15',
            'จ' => '8',
            'ฉ' => '8',
            'ช' => '8',
            'ซ' => '8',
            'ฌ' => '8',
            'ญ' => '8',
            'ฎ' => '17',
            'ฏ' => '17',
            'ฐ' => '17',
            'ฑ' => '17',
            'ฒ' => '17',
            'ณ' => '17',
            'บ' => '19',
            'ป' => '19',
            'ผ' => '19',
            'ฝ' => '19',
            'พ' => '19',
            'ฟ' => '19',
            'ภ' => '19',
            'ม' => '19',
            'ศ' => '21',
            'ษ' => '21',
            'ส' => '21',
            'ห' => '21',
            'ฬ' => '21',
            'ฮ' => '21',
            'ด' => '10',
            'ต' => '10',
            'ถ' => '10',
            'ท' => '10',
            'ธ' => '10',
            'น' => '10',
            'ย' => '12',
            'ร' => '12',
            'ล' => '12',
            'ว' => '12'
        );


        foreach ($aName as $kN => $vN) {
            foreach ($vN as $kk => $vv) {
                foreach ($txt_star as $k => $v) {
                    if ($kk == $k) {
                        array_push($this->shaName, array($kk => $v));
                        array_push($this->shaNameDealFormat, array('xChar' => $kk, 'xNum' => $v));

                    }
                }

            }
        }

        foreach ($aSurName as $kN => $vN) {
            foreach ($vN as $kk => $vv) {
                foreach ($txt_star as $k => $v) {
                    if ($kk == $k) {
                        array_push($this->shaSurName, array($kk => $v));
                        array_push($this->shaSurNameDealFormat, array('xChar' => $kk, 'xNum' => $v));
                    }
                }

            }
        }



    }

    private function setSumSha($shaName, $shaSurName)
    {

        foreach ($shaName as $k => $v) {

            foreach ($v as $num) {
                $this->sumShaName += (int) $num;

            }

        }
        foreach ($shaSurName as $k => $v) {

            foreach ($v as $num) {
                $this->sumShaSurName += (int) $num;

            }

        }


        $this->sumShaNameSurName = $this->sumShaName + $this->sumShaSurName;


    }


    private function setSumSat($aName, $aSurName)
    {
        foreach ($aName as $k => $v) {

            foreach ($v as $num) {
                $this->sumSatName += (int) $num;

            }

        }
        foreach ($aSurName as $k => $v) {

            foreach ($v as $num) {
                $this->sumSatSurName += (int) $num;

            }

        }


        $this->sumSatNameSurName = $this->sumSatName + $this->sumSatSurName;
    }

    private function getAyad($sumShaString): string
    {

        $sumx = 0;
        $sumy = 0;

        for ($i = 0; $i < strlen($sumShaString); $i++) {
            $sumx += (int) substr($sumShaString, $i, 1);
        }

        for ($i = 0; $i < strlen($sumx); $i++) {
            $sumy += (int) substr($sumx, $i, 1);
        }

        if ($sumy >= 10) {
            $sumz = $sumy;
            $sumy = 0;
            for ($i = 0; $i < strlen($sumz); $i++) {
                $sumy += (int) substr($sumz, $i, 1);
            }
        }

        return (string) $sumy;

    }

    private function getPairNum($sumShaString): array
    {

        $pair = null;


        if ($sumShaString >= 100) {

            $pair = array('pair' => substr((string) $sumShaString, 1, 2), 'fang' => substr((string) $sumShaString, 0, 2));

        } else {
            $pair = array('pair' => (string) $sumShaString, 'fang' => null);
        }


        return $pair;
    }

    private function getPairsUnique($pairSatName, $pairSatSurName, $pairSatNameSurName, $pairShaName, $pairShaSurName, $pairShaNameSurName): array
    {
        $unique = array();


        foreach ($pairSatName as $key => $value) {
            if ($value != null)
                array_push($unique, $value);
        }
        foreach ($pairSatSurName as $key => $value) {
            if ($value != null)
                array_push($unique, $value);
        }

        foreach ($pairSatNameSurName as $key => $value) {
            if ($value != null)
                array_push($unique, $value);
        }
        foreach ($pairShaName as $key => $value) {
            if ($value != null)
                array_push($unique, $value);
        }
        foreach ($pairShaSurName as $key => $value) {
            if ($value != null)
                array_push($unique, $value);
        }

        foreach ($pairShaNameSurName as $key => $value) {
            if ($value != null)
                array_push($unique, $value);
        }


        $uni = array_unique($unique);


        $dealFormat = array();
        foreach ($uni as $key => $val) {
            array_push($dealFormat, $val);
        }

        return $dealFormat;
    }

    private function getPairsMiracle($pairsUnique): array
    {

        $pairMiracle = array();

        $sql = "SELECT * FROM numbers ORDER BY pairnumberid ASC";
        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);

        foreach ($data as $value) {
            foreach ($pairsUnique as $pair) {

                if ($value->pairnumber === $pair) {
                    array_push(
                        $pairMiracle,
                        array(
                            "pairnumber" => $pair,
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

        return $pairMiracle;
    }


}












































