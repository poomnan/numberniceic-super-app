<?php
namespace App\Managers;

class PersonManager
{
    public function age($seconds = 1, $time = ''):array
    {
        $age = array();

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
            //$str .= $years.' ปี, ';
            $age['year'] = $years;
        }

        $seconds -= $years * 31536000;
        $months = floor($seconds / 2628000);

        if ($years > 0 OR $months > 0)
        {
            if ($months > 0)
            {
                //str .= $months.' เดือน, ';
                $age['month'] = $months;
            }

            $seconds -= $months * 2628000;
        }

        $weeks = floor($seconds / 604800);

        if ($years > 0 OR $months > 0 OR $weeks > 0)
        {
            if ($weeks > 0)
            {
                //$str .= $weeks.' สัปดาห์, ';
                $age['week'] = $weeks;
            }

            $seconds -= $weeks * 604800;
        }

        $days = floor($seconds / 86400);

        if ($months > 0 OR $weeks > 0 OR $days > 0)
        {
            if ($days > 0)
            {
                //$str .= $days.' วัน, ';
                $age['day'] = $days;
            }

            $seconds -= $days * 86400;
        }

        $hours = floor($seconds / 3600);

        if ($days > 0 OR $hours > 0)
        {
            if ($hours > 0)
            {
                //$str .= $hours.' ชั่วโมง, ';
                $age['hour'] = $hours;
            }

            $seconds -= $hours * 3600;
        }

        $minutes = floor($seconds / 60);

        if ($days > 0 OR $hours > 0 OR $minutes > 0)
        {
            if ($minutes > 0)
            {
                //$str .= $minutes.' นาที, ';
                $age['minute'] = $minutes;
            }

            $seconds -= $minutes * 60;
        }

        if ($str == '')
        {
            //$str .= $seconds.' วินาที';
            $age['second'] = $seconds;
        }

        return $age;
    }
}