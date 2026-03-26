<?php

namespace App\Managers;

class PersonController extends Manager
{

    public function haircut($request, $response)
    {


        date_default_timezone_set('UTC');

        $body = $request->getParsedBody();
        $dayx = filter_var($body['dayx'], FILTER_SANITIZE_STRING);
        $activity = filter_var($body['activity'], FILTER_SANITIZE_STRING);

        $datex = date("Y-m-d");
        $unixTimestamp = strtotime($datex);
        $dayy = strtolower(date('l', $unixTimestamp));


        $sqlx = "SELECT * FROM miracledo LEFT JOIN miracledo_desc ON miracledo.mira_id = miracledo_desc.mira_id WHERE miracledo.activity LIKE '{$activity}' && dayx LIKE '{$dayx}' && dayy LIKE '{$dayy}'";

        $resultx = $this->db->prepare($sqlx);
        if ($resultx->execute()) {
            $data = $resultx->fetchAll(\PDO::FETCH_OBJ);

            if (count($data) > 0) {
                echo json_encode(array(

                    'server' => array('massage'=>'success'),
                    'data' => $data


                ));

            }
        }


    }



    public function ageCal(){

        $seconds = strtotime( '1978-02-27' );
        $time = time();

            if ( ! is_numeric($seconds))
            {
                $seconds = 1;
            }

            if ( ! is_numeric($time))
            {
                $time = time();
            }

            if ($time <= $seconds)
            {
                $seconds = 1;
            }
            else
            {
                $seconds = $time - $seconds;
            }

            $str = '';
            $years = floor($seconds / 31536000);

            if ($years > 0)
            {
                $str .= $years.' ปี, ';
            }

            $seconds -= $years * 31536000;
            $months = floor($seconds / 2628000);

            if ($years > 0 OR $months > 0)
            {
                if ($months > 0)
                {
                    $str .= $months.' เดือน, ';
                }

                $seconds -= $months * 2628000;
            }

            $weeks = floor($seconds / 604800);

            if ($years > 0 OR $months > 0 OR $weeks > 0)
            {
                if ($weeks > 0)
                {
                    $str .= $weeks.' สัปดาห์, ';
                }

                $seconds -= $weeks * 604800;
            }

            $days = floor($seconds / 86400);

            if ($months > 0 OR $weeks > 0 OR $days > 0)
            {
                if ($days > 0)
                {
                    $str .= $days.' วัน, ';
                }

                $seconds -= $days * 86400;
            }

            $hours = floor($seconds / 3600);

            if ($days > 0 OR $hours > 0)
            {
                if ($hours > 0)
                {
                    $str .= $hours.' ชั่วโมง, ';
                }

                $seconds -= $hours * 3600;
            }

            $minutes = floor($seconds / 60);

            if ($days > 0 OR $hours > 0 OR $minutes > 0)
            {
                if ($minutes > 0)
                {
                    $str .= $minutes.' นาที, ';
                }

                $seconds -= $minutes * 60;
            }

            if ($str == '')
            {
                $str .= $seconds.' วินาที';
            }

            echo date("Y-m-d H:i:s");

        echo '<br>';

            echo substr(trim($str), 0, -1);
        }


/*// ตัวอย่างการใช้งาน
        $birthdate = strtotime( '1973-11-13' );
        $today = time();

        echo timespan( $birthdate , $today );
//36 ปี, 2 เดือน, 3 สัปดาห์, 2 วัน, 4 ชั่วโมง, 51 นาที*/


}