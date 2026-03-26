<?php

namespace App\Managers;


class NickNameVipController extends Manager
{

    private $limit = 100;

    public function listNickNameByParamBoth($request, $response)
    {

        $day = $request->getAttribute('day');
        $charx = $request->getAttribute('charx');
        $usetable = $request->getAttribute('usetable');
        $prefix = $request->getAttribute('prefix');


        if ($prefix == '') {
            $sql = "SELECT * FROM {$usetable} where thainame LIKE '{$charx}%' && day LIKE '{$day}' order by thainame";
        } else {
            $sql = "SELECT * FROM {$usetable} where thainame LIKE '{$charx}%' && day LIKE '{$day}' && sex LIKE '{$prefix}' order by thainame";
        }


        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);


        $vArr = array();

        foreach ($data as $k => $v) {

            $cKalikini = $this->getKarakini($day, $v->thainame);

            if (count($cKalikini) == null) {
                array_push($vArr, $v);

            }

        }


        $api = array(
            'day' => $day,
            'nSingle' => $charx,
            'nick_name_list_vip' => $vArr
        );


        $response->getBody()->write(json_encode($api));
        return $response->withHeader('Content-Type', 'application/json');
    }


    public function listNickNameByParamCharxOnly($request, $response)
    {

        $charx = $request->getAttribute('charx');
        $usetable = $request->getAttribute('usetable');
        $prefix = $request->getAttribute('prefix');

        if ($prefix == '') {
            $sql = "SELECT * FROM {$usetable} where thainame LIKE '{$charx}%' order by thainame";
        } else {
            $sql = "SELECT * FROM {$usetable} where thainame LIKE '{$charx}%' && sex LIKE '{$prefix}' order by thainame";
        }


        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);


        $api = array(
            'day' => null,
            'nSingle' => $charx,
            'nick_name_list_vip' => $data
        );

        $response->getBody()->write(json_encode($api));
        return $response->withHeader('Content-Type', 'application/json');
    }

    public function listNickNameByParamDayOnly($request, $response)
    {

        $day = $request->getAttribute('day');
        $usetable = $request->getAttribute('usetable');
        $prefix = $request->getAttribute('prefix');

        if ($prefix == '') {
            $sql = "SELECT * FROM {$usetable} WHERE day LIKE '{$day}' order by thainame";
        } else {
            $sql = "SELECT * FROM {$usetable} WHERE sex LIKE '{$prefix}' && day LIKE '{$day}' order by thainame";
        }


        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);


        $vArr = array();

        foreach ($data as $k => $v) {

            $cKalikini = $this->getKarakini($day, $v->thainame);

            if (count($cKalikini) == null) {
                array_push($vArr, $v);

            }

        }


        $api = array(
            'day' => $day,
            'nSingle' => null,
            'nick_name_list_vip' => $vArr
        );

        $response->getBody()->write(json_encode($api));
        return $response->withHeader('Content-Type', 'application/json');
    }


    public function listNickNameByAll($request, $response)
    {
        $usetable = $request->getAttribute('usetable');
        $day = $request->getAttribute('day');
        $charx = $request->getAttribute('charx');
        $prefix = $request->getAttribute('prefix');


        if ($charx == 'x' && $prefix == 'x') {
            $sql = "SELECT * FROM {$usetable} order by thainame";

        }

        if ($charx == 'x' && $prefix != 'x') {
            $sql = "SELECT * FROM {$usetable} WHERE sex LIKE '{$prefix}' order by thainame";

        }

        if ($charx != 'x' && $prefix == 'x') {
            $sql = "SELECT * FROM {$usetable} WHERE thainame LIKE '{$charx}%' order by thainame";

        }

        if ($charx != 'x' && $prefix != 'x') {
            $sql = "SELECT * FROM {$usetable} WHERE thainame LIKE '{$charx}%' && sex LIKE '{$prefix}' order by thainame";

        }


        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);


        $vArr = array();


        if ($day == 'x') {
            $vArr = $data;
        } else {
            foreach ($data as $k => $v) {

                $cKalikini = $this->getKarakini($day, $v->thainame);

                if (count($cKalikini) == null) {
                    array_push($vArr, $v);

                }

            }
        }


        $api = array(
            'day' => $day,
            'nSingle' => $charx,
            'nick_name_list_vip' => $vArr
        );


        $response->getBody()->write(json_encode($api));
        return $response->withHeader('Content-Type', 'application/json');
    }


    public function listNameVip($request, $response)
    {
        $usetable = $request->getAttribute('usetable');
        $day = $request->getAttribute('day');
        $charx = $request->getAttribute('charx');
        $prefix = $request->getAttribute('prefix');

        $lastid = $request->getAttribute('lastid');


        if ($charx == 'x' && $prefix == 'x') {
            $sql = "SELECT * FROM {$usetable} WHERE nameid >= $lastid order by thainame LIMIT $this->limit";

        }

        if ($charx == 'x' && $prefix != 'x') {
            $sql = "SELECT * FROM {$usetable} WHERE nameid >= $lastid && sex LIKE '{$prefix}' order by thainame LIMIT $this->limit";

        }

        if ($charx != 'x' && $prefix == 'x') {
            $sql = "SELECT * FROM {$usetable} WHERE nameid >= $lastid && thainame LIKE '{$charx}%' order by thainame LIMIT $this->limit";

        }

        if ($charx != 'x' && $prefix != 'x') {
            $sql = "SELECT * FROM {$usetable} WHERE nameid >= $lastid && thainame LIKE '{$charx}%' && sex LIKE '{$prefix}' order by thainame LIMIT $this->limit";

        }


        $result = $this->db->prepare($sql);
        $result->execute();
        $data = $result->fetchAll(\PDO::FETCH_OBJ);


        $vArr = array();


        if ($day == 'x') {
            $vArr = $data;
        } else {
            foreach ($data as $k => $v) {

                $cKalikini = $this->getKarakini($day, $v->thainame);

                if (count($cKalikini) == null) {
                    array_push($vArr, $v);

                }

            }
        }


        $api = array(
            'day' => $day,
            'nSingle' => $charx,
            'nick_name_list_vip' => $vArr
        );


        $response->getBody()->write(json_encode($api));
        return $response->withHeader('Content-Type', 'application/json');
    }


    private function getKarakini(string $aBirthDay, string $aNickName): array
    {

        $kalikiniName = array();

        $txt_kday = array(
            '1' => 'ศษสหฬฮ',
            '2' => 'ะ้ืแ๊็าิึี่ำุูเใไโอ',
            '3' => 'กขคฆง',
            '4' => 'จฉชซฌญ',
            '8' => 'บปผฝพฟภม',
            '5' => 'ดตถทธน',
            '6' => 'ยรลว',
            '7' => 'ฎฏฐฑฒณ',
        );


        foreach ($txt_kday as $kD => $kV) {

            if ($aBirthDay == $kD) {

                for ($i = 0; $i < strlen($kV); $i++) {
                    $strK = mb_substr($kV, $i, 1); //loop หาตัวกาลินีจากวันเกิด

                    for ($n = 0; $n < strlen($aNickName); $n++) {
                        $xString = mb_substr($aNickName, $n, 1); //loop หาอักษรจากชื่อ

                        if ($strK == $xString && $strK != null) {
                            array_push($kalikiniName, $strK);
                            break;
                        }

                    }

                }// loop for birthday kalakini


                break;
            }
        }


        return $kalikiniName;


    }


}