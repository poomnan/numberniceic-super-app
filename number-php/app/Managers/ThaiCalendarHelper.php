<?php

namespace App\Managers;

/**
 * Helper for Thai Astrological Calculations.
 * Aligned with number-androidx/app/src/main/java/com/numberniceic/utils/ThaiAstrologyLib.kt
 */
class ThaiCalendarHelper
{
    /**
     * Determines if a date is a Buddhist Holy Day.
     * Uses simplified anchor-based lunar calculation.
     */
    public static function isWanPra($date)
    {
        $lunar = self::getThaiLunarDate($date);
        $day = $lunar['day'];
        $dithi = $lunar['dithi'];

        if ($day == 8 || $day == 15) {
            return true;
        }

        // Special case for waning month 29-day month (Waning 14 is the last day)
        if ($dithi == 29) {
            return true;
        }

        return false;
    }

    /**
     * Aligned with ThaiAstrologyLib.kt simplified anchors for 2569.
     * Returns dithi (1-30), day (1-15), and Thai month (1-12/13).
     */
    public static function getThaiLunarDate($dateStr)
    {
        $dt = new \DateTime($dateStr);
        $month = (int) $dt->format('n');
        $dayNum = (int) $dt->format('j');

        // Anchors for 2569 (2026) - Mapping solar day to lunar start
        $anchors = [
            1 => [19, 3], 2 => [17, 4], 3 => [19, 5],
            4 => [17, 6], 5 => [17, 7], 6 => [15, 8],
            7 => [15, 18], 8 => [14, 9], 9 => [12, 10],
            10 => [12, 11], 11 => [10, 12], 12 => [10, 1]
        ];

        $anchor = $anchors[$month] ?? [1, 1];

        if ($dayNum >= $anchor[0]) {
            $dithi = ($dayNum - $anchor[0]) + 1;
            $mThai = $anchor[1];
        } else {
            $prevMonthDT = (clone $dt)->modify('last day of previous month');
            $prevMonth = (int) $prevMonthDT->format('n');
            $prevAnchor = $anchors[$prevMonth] ?? [1, 1];
            $daysInPrev = (int) $prevMonthDT->format('j');
            
            $dithi = ($daysInPrev - $prevAnchor[0]) + 1 + $dayNum;
            $mThai = $prevAnchor[1];
        }

        return [
            'dithi' => $dithi,
            'day' => ($dithi > 15 ? $dithi - 15 : $dithi),
            'mThai' => $mThai
        ];
    }

    /**
     * Aligned with getKalayok in ThaiAstrologyLib.kt
     */
    public static function getKalayok($dateStr)
    {
        $date = new \DateTime($dateStr);
        $w = (int) $date->format('w'); // 0 (Sun) - 6 (Sat)
        $wDay = ($w == 0) ? 7 : $w;
        $cutoff = new \DateTime('2026-04-16');

        $res = [
            'is_tongchai' => false, 
            'is_atipbadee' => false, 
            'is_ubath' => false, 
            'is_lokawinat' => false
        ];

        if ($date < $cutoff) {
            // จ.ศ. 1387 (ก่อน 16 เม.ย. 2569): ธงชัย+อธิบดี=ศุกร์, อุบาทว์=พฤหัส, โลกาวินาศ=อาทิตย์
            if ($wDay == 5) {
                $res['is_tongchai'] = true;
                $res['is_atipbadee'] = true;
            }
            if ($wDay == 4) $res['is_ubath'] = true;
            if ($wDay == 7) $res['is_lokawinat'] = true;
        } else {
            // จ.ศ. 1388 (ตั้งแต่ 16 เม.ย. 2569): ธงชัย=จันทร์, โลกาวินาศ=จันทร์, อธิบดี=เสาร์, อุบาทว์=อาทิตย์
            if ($wDay == 1) {
                $res['is_tongchai'] = true;
                $res['is_lokawinat'] = true;
            }
            if ($wDay == 6) $res['is_atipbadee'] = true;
            if ($wDay == 7) $res['is_ubath'] = true;
        }
        return $res;
    }

    public static function isRiangMon($dithi)
    {
        $targets = [7, 10, 13, 19, 23, 25, 29];
        return in_array($dithi, $targets);
    }

    /**
     * Combined logic for various auspicious days.
     */
    public static function queryMahaChok($dateStr)
    {
        $date = new \DateTime($dateStr);
        $w = (int) $date->format('w');
        $wDay = ($w == 0) ? 7 : $w;
        
        $lunar = self::getThaiLunarDate($dateStr);
        $d = $lunar['day'];
        $dithi = $lunar['dithi'];

        $res = [
            'is_ammarit' => false,
            'is_mahasittichok' => false,
            'is_sittichok' => false,
            'is_rachachok' => false,
            'is_chaichok' => false,
            'is_tongchai' => false,
            'is_atipbadee' => false,
            'is_lokawinat' => false,
            'is_ubath' => false,
            'is_riangmon' => self::isRiangMon($dithi)
        ];

        // Include Kalayok results
        $kalayok = self::getKalayok($dateStr);
        $res['is_tongchai'] = $kalayok['is_tongchai'];
        $res['is_atipbadee'] = $kalayok['is_atipbadee'];
        $res['is_lokawinat'] = $kalayok['is_lokawinat'];
        $res['is_ubath'] = $kalayok['is_ubath'];

        // Maha Chok Rules (Aligned with ThaiAstrologyLib.kt)
        switch ($wDay) {
            case 7: // Sunday
                if ($d == 8) $res['is_ammarit'] = true;
                if ($d == 14) $res['is_mahasittichok'] = true;
                if ($d == 11) $res['is_sittichok'] = true;
                if ($d == 6) $res['is_rachachok'] = true;
                if ($d == 8) $res['is_chaichok'] = true;
                break;
            case 1: // Monday
                if ($d == 3) $res['is_ammarit'] = true;
                if ($d == 12) $res['is_mahasittichok'] = true;
                if ($d == 5) $res['is_sittichok'] = true;
                if ($d == 3) $res['is_rachachok'] = true;
                if ($d == 3) $res['is_chaichok'] = true;
                break;
            case 2: // Tuesday
                if ($d == 9) $res['is_ammarit'] = true;
                if ($d == 13) $res['is_mahasittichok'] = true;
                if ($d == 14) $res['is_sittichok'] = true;
                if ($d == 9) $res['is_rachachok'] = true;
                if ($d == 11) $res['is_chaichok'] = true;
                break;
            case 3: // Wednesday
                if ($d == 2) $res['is_ammarit'] = true;
                if ($d == 4) $res['is_mahasittichok'] = true;
                if ($d == 10) $res['is_sittichok'] = true;
                if ($d == 6) $res['is_rachachok'] = true;
                if ($d == 10) $res['is_chaichok'] = true;
                break;
            case 4: // Thursday
                if ($d == 4) $res['is_ammarit'] = true;
                if ($d == 7) $res['is_mahasittichok'] = true;
                if ($d == 9) $res['is_sittichok'] = true;
                if ($d == 10) $res['is_rachachok'] = true;
                if ($d == 4) $res['is_chaichok'] = true;
                break;
            case 5: // Friday
                if ($d == 1) $res['is_ammarit'] = true;
                if ($d == 10) $res['is_mahasittichok'] = true;
                if ($d == 11) $res['is_sittichok'] = true;
                if ($d == 1) $res['is_rachachok'] = true;
                if ($d == 1) $res['is_chaichok'] = true;
                break;
            case 6: // Saturday
                if ($d == 5) $res['is_ammarit'] = true;
                if ($d == 15) $res['is_mahasittichok'] = true;
                if ($d == 4) $res['is_sittichok'] = true;
                if ($d == 5) $res['is_rachachok'] = true;
                if ($d == 11) $res['is_chaichok'] = true;
                break;
        }
        return $res;
    }

    public static function getUpcomingWanPras($months = 4)
    {
        $wanpras = [];
        $start = new \DateTime();
        $end = (new \DateTime())->modify("+$months months");
        $current = clone $start;
        $current->modify("-1 day");

        while ($current <= $end) {
            $dateStr = $current->format('Y-m-d');
            if (self::isWanPra($dateStr)) {
                $wanpras[] = [
                    'wanpra_date' => $dateStr
                ];
            }
            $current->modify("+1 day");
        }
        return $wanpras;
    }
}
