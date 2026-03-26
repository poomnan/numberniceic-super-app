package com.numberniceic.utils

import org.joda.time.DateTime
import org.joda.time.LocalDate

data class TaksaPoint(val r: Int, val c: Int)

data class TaksaResult(
    val board: Array<IntArray>,
    val path: List<TaksaPoint>,
    val targetPos: TaksaPoint,
    val badPos: TaksaPoint,
    val birthBadPos: TaksaPoint,
    val birthPos: TaksaPoint,
    val ageNext: Int,
    val sumAge: Int
)

object ThaiAstrologyLib {

    // ตารางผังดาว (3x3 Grid)
    val BOARD = arrayOf(
        intArrayOf(1, 2, 3),
        intArrayOf(6, 0, 4),
        intArrayOf(8, 5, 7)
    )

    // ลำดับการนับ (Clockwise Path)
    val MASTER_PATH = listOf(
        TaksaPoint(0, 0), TaksaPoint(0, 1), TaksaPoint(0, 2),
        TaksaPoint(1, 2), TaksaPoint(2, 2), TaksaPoint(2, 1),
        TaksaPoint(2, 0), TaksaPoint(1, 0), TaksaPoint(1, 1)
    )

    fun calculateTaksa(birthDate: LocalDate, birthDayNum: Int): TaksaResult {
        val now = LocalDate.now()
        
        // 1. คำนวณอายุย่าง (ไทย)
        var ageFull = now.year - birthDate.year
        if (now.monthOfYear < birthDate.monthOfYear || (now.monthOfYear == birthDate.monthOfYear && now.dayOfMonth < birthDate.dayOfMonth)) {
            ageFull--
        }
        val ageNext = ageFull + 1

        // 2. ผลรวมอายุย่าง (Digital Root)
        var sumAge = sumDigits(ageNext)
        
        // 3. หาจุดเริ่มต้น (พิกัดวันเกิด)
        val startIdx = MASTER_PATH.indexOfFirst { BOARD[it.r][it.c] == birthDayNum }
        val birthPos = MASTER_PATH[startIdx]

        // 4. นับก้าว (เริ่มที่วันเกิดเป็นก้าวที่ 1)
        val pathLen = MASTER_PATH.size
        val targetIdx = (startIdx + (sumAge - 1)) % pathLen
        val targetPos = MASTER_PATH[targetIdx]

        // 5. จุดอัปมงคลอายุย่าง (ถอยหลัง 1 ก้าว)
        val badIdx = (targetIdx - 1 + pathLen) % pathLen
        val badPos = MASTER_PATH[badIdx]

        // 6. จุดอัปมงคลวันเกิด (ถอยหลัง 1 ก้าวจากวันเกิด)
        val birthBadIdx = (startIdx - 1 + pathLen) % pathLen
        val birthBadPos = MASTER_PATH[birthBadIdx]

        return TaksaResult(BOARD, MASTER_PATH, targetPos, badPos, birthBadPos, birthPos, ageNext, sumAge)
    }

    private fun sumDigits(n: Int): Int {
        var sum = 0
        var num = n
        while (num > 0) {
            sum += num % 10
            num /= 10
        }
        // Repeat summing if > 9 to get single digit (1-9) for astrology
        if (sum > 9) return sumDigits(sum)
        return sum
    }

    /**
     * 3. วันกาลโยค (Kala-Yoga Days)
     * คำนวณตามปี CS 1387 - 1388 (2569)
     */
    fun getKalayok(date: LocalDate): List<String> {
        val tags = mutableListOf<String>()
        val w = date.dayOfWeek // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
        val cutoff = LocalDate(2026, 4, 16)

        if (date.isBefore(cutoff)) {
            // จ.ศ. 1387: ธงชัย=ศุกร์, อธิบดี=ศุกร์, อุบาทว์=พฤหัส, โลกาวินาศ=อาทิตย์, กระทิงวัน=จันทร์
            if (w == 5) tags.addAll(listOf("ธงชัย", "อธิบดี"))
            if (w == 4) tags.add("อุบาทว์")
            if (w == 7) tags.add("โลกาวินาศ")
            if (w == 1) tags.add("กระทิงวัน")
        } else {
            // จ.ศ. 1388: ธงชัย=จันทร์, อธิบดี=เสาร์, อุบาทว์=อาทิตย์, โลกาวินาศ=จันทร์, กระทิงวัน=เสาร์
            if (w == 1) tags.addAll(listOf("ธงชัย", "โลกาวินาศ"))
            if (w == 6) tags.addAll(listOf("อธิบดี", "กระทิงวัน"))
            if (w == 7) tags.add("อุบาทว์")
        }
        return tags
    }

    /**
     * 7.1 วันจม-วันฟู (Month-based Success/Failure)
     * w: 1=Mon, 2=Tue... 7=Sun
     */
    fun getLoyFuJom(w: Int, mThai: Int): List<String> {
        val res = mutableListOf<String>()
        val month = if (mThai == 13 || mThai == 18) 8 else if (mThai > 12) mThai - 12 else mThai

        // วันลอย/ฟู/จม ตามคัมภีร์กาลโยค
        val loyDays = mapOf(1 to 4, 2 to 5, 3 to 6, 8 to 6, 4 to 7, 9 to 7, 5 to 1, 10 to 1, 6 to 2, 11 to 2, 7 to 3, 12 to 3)
        val fuDays = mapOf(1 to 1, 2 to 2, 3 to 3, 8 to 3, 4 to 4, 9 to 4, 5 to 5, 10 to 5, 6 to 6, 11 to 6, 7 to 7, 12 to 7)
        val jomDays = mapOf(1 to 5, 2 to 6, 3 to 7, 8 to 7, 4 to 1, 9 to 1, 5 to 2, 10 to 2, 6 to 3, 11 to 3, 7 to 4, 12 to 4)

        if (loyDays[month] == w) res.add("วันลอย")
        if (fuDays[month] == w) res.add("วันฟู")
        if (jomDays[month] == w) res.add("วันจม")
        
        return res
    }
    
    /**
     * 5.1 ดิถีเรียงหมอน (Dithi Riang Mon)
     * ข้างขึ้น: 7, 10, 13 ค่ำ | ข้างแรม: 4, 8, 10, 14 ค่ำ
     */
    fun isRiangMon(dithi: Int): Boolean {
        // dithi: 1-15 (Up), 16-30 (Down)
        val targets = setOf(7, 10, 13, 19, 23, 25, 29)
        return targets.contains(dithi)
    }

    /**
     * 5. ดิถีมหาโชค (Maha Chok)
     * พิจารณาประกอบกับวันในสัปดาห์
     */
    fun queryMahaChok(w: Int, dithiRaw: Int): List<String> {
        val res = mutableListOf<String>()
        var d = dithiRaw
        if (d > 15) d -= 15 // Check same for Up/Down

        when (w) {
            7 -> { // Sun
                if (d == 8) res.add("อำฤตโชค")
                if (d == 14) res.add("มหาสิทธิโชค")
                if (d == 11) res.add("สิทธิโชค")
                if (d == 8) res.add("ชัยโชค")
                if (d == 6) res.add("ราชาโชค")
            }
            1 -> { // Mon
                if (d == 3) res.add("อำฤตโชค")
                if (d == 12) res.add("มหาสิทธิโชค")
                if (d == 5) res.add("สิทธิโชค")
                if (d == 3) res.add("ชัยโชค")
                if (d == 3) res.add("ราชาโชค")
            }
            2 -> { // Tue
                if (d == 9) res.add("อำฤตโชค")
                if (d == 13) res.add("มหาสิทธิโชค")
                if (d == 14) res.add("สิทธิโชค")
                if (d == 11) res.add("ชัยโชค")
                if (d == 9) res.add("ราชาโชค")
            }
            3 -> { // Wed
                if (d == 2) res.add("อำฤตโชค")
                if (d == 4) res.add("มหาสิทธิโชค")
                if (d == 10) res.add("สิทธิโชค")
                if (d == 10) res.add("ชัยโชค")
                if (d == 6) res.add("ราชาโชค")
            }
            4 -> { // Thu
                if (d == 4) res.add("อำฤตโชค")
                if (d == 3 || d == 7) res.add("มหาสิทธิโชค")
                if (d == 9) res.add("สิทธิโชค")
                if (d == 4) res.add("ชัยโชค")
                if (d == 10) res.add("ราชาโชค")
            }
            5 -> { // Fri
                if (d == 1) res.add("อำฤตโชค")
                if (d == 10) res.add("มหาสิทธิโชค")
                if (d == 11) res.add("สิทธิโชค")
                if (d == 1) res.add("ชัยโชค")
                if (d == 1) res.add("ราชาโชค")
            }
            6 -> { // Sat
                if (d == 5) res.add("อำฤตโชค")
                if (d == 15) res.add("มหาสิทธิโชค")
                if (d == 4) res.add("สิทธิโชค")
                if (d == 11) res.add("ชัยโชค")
                if (d == 5) res.add("ราชาโชค")
            }
        }
        return res
    }

    /**
     * 6. ดิถีไม่ดีที่ควรหลีกเลี่ยง (Inauspicious Dithi)
     * พิจารณาประกอบกับวันในสัปดาห์
     */
    fun queryInauspicious(w: Int, dithiRaw: Int): List<String> {
        val res = mutableListOf<String>()
        var d = dithiRaw
        if (d > 15) d -= 15

        // Mapping from User's Table (Sun=7, Mon=1... Sat=6)
        val badMap = mapOf(
            "ทึกทึน" to mapOf(7 to 1, 1 to 4, 2 to 6, 3 to 9, 4 to 5, 5 to 3, 6 to 7),
            "ทรธึก" to mapOf(7 to 4, 1 to 6, 2 to 1, 3 to 3, 4 to 8, 5 to 7, 6 to 1),
            "ยมขันธ์" to mapOf(7 to 12, 1 to 11, 2 to 7, 3 to 13, 4 to 6, 5 to 8, 6 to 9),
            "อัตนิโรจน์" to mapOf(7 to 4, 1 to 6, 2 to 1, 3 to 3, 4 to 3, 5 to 9, 6 to 1),
            "ทินกาล" to mapOf(7 to 1, 1 to 2, 2 to 10, 3 to 7, 4 to 1, 5 to 6, 6 to 6),
            "ทินสูญ" to mapOf(7 to 12, 1 to 10, 2 to 15, 3 to 8, 4 to 5, 5 to 7, 6 to 8),
            "กาฬโชค" to mapOf(7 to 4, 1 to 6, 2 to 1, 3 to 3, 4 to 8, 5 to 9, 6 to 10),
            "กาลสูญ" to mapOf(7 to 4, 1 to 2, 2 to 7, 3 to 5, 4 to 8, 5 to 3, 6 to 6),
            "กาลทัณฑ์" to mapOf(7 to 12, 1 to 11, 2 to 10, 3 to 9, 4 to 8, 5 to 7, 6 to 1),
            "โลกวินาส" to mapOf(7 to 4, 1 to 6, 2 to 10, 3 to 9, 4 to 8, 5 to 9, 6 to 1),
            "วินาสส์" to mapOf(7 to 4, 1 to 8, 2 to 6, 3 to 4, 4 to 8, 5 to 8, 6 to 9),
            "พิลา" to mapOf(7 to 6, 1 to 10, 2 to 8, 3 to 7, 4 to 2, 5 to 9, 6 to 12),
            "มฤตยู" to mapOf(7 to 9, 1 to 1, 2 to 10, 3 to 9, 4 to 8, 5 to 7, 6 to 6),
            "วันบอด" to mapOf(7 to 7, 1 to 8, 2 to 4, 3 to 7, 4 to 1, 5 to 14, 6 to 11),
            "กาลทีน" to mapOf(7 to 5, 1 to 6, 2 to 10, 3 to 8, 4 to 11, 5 to 5, 6 to 7)
        )

        badMap.forEach { (name, rule) ->
            if (rule[w] == d) res.add(name)
        }


        return res
    }

    /**
     * 7.2 ดิถีมงคลทั้ง 5 (Day-based Dithi)
     */
    fun getDithiMongkol5(w: Int, dithiRaw: Int): List<String> {
        val res = mutableListOf<String>()
        var d = dithiRaw
        if (d > 15) d -= 15

        when (w) {
            2 -> if (d == 3 || d == 8 || d == 13) res.add("ไชยดิถี")
            3 -> if (d == 2 || d == 7 || d == 12) res.add("ภัทรดีถี")
            4 -> if (d == 5 || d == 10 || d == 15) res.add("ปุณณดีถี")
            5 -> if (d == 1 || d == 6 || d == 11) res.add("นันทดีถี")
            6 -> if (d == 4 || d == 9 || d == 14) res.add("มิตตะดีถี")
        }
        return res
    }

    /**
     * 7.3 ดิถีมหาสูญ (Mahasun)
     * Prohibited dithi based on Thai Lunar Month
     */
    fun queryMahasun(mThai: Int, dithiRaw: Int): Boolean {
        var d = dithiRaw
        if (d > 15) d -= 15
        
        // Normalize month (handle leap months like 18 or 13)
        val m = if (mThai == 18 || mThai == 13) 8 else if (mThai > 12) mThai - 12 else mThai

        return when (m) {
            6, 3 -> d == 4
            7, 10 -> d == 8
            8, 5 -> d == 6
            11, 2 -> d == 12
            9, 12 -> d == 10
            4, 1 -> d == 2
            else -> false
        }
    }

    /**
     * 7.4 ข้อห้ามดิถีต่างๆ (Common Dithi Prohibitions)
     * dithiRaw: 1-15 (Waxing), 16-30 (Waning)
     */
    fun queryDithiCommonProhibitions(dithiRaw: Int): String? {
        var d = dithiRaw
        if (d > 15) d -= 15
        
        return when (d) {
            1 -> "ห้ามซื้อวัวควาย"
            2 -> "ห้ามเที่ยวป่า"
            3 -> "ห้ามเดินทางน้ำ"
            4 -> "ห้ามเที่ยวภูเขา"
            5 -> "ห้ามแบ่งที่ดิน"
            6 -> "ห้ามปลูกบ้าน"
            7 -> "อัคนิโรธน์วัง"
            8 -> "ห้ามซื้อรถ"
            9 -> "ห้ามฝังเสาบ้าน"
            10 -> "ห้ามลงเรือ"
            11 -> "ห้ามเพาะปลูก"
            12 -> "ห้ามบุรุษเสพเมถุน"
            13 -> "ห้ามสตรีเสพเมถุน"
            14 -> "ห้ามบวช"
            15 -> "ห้ามบวงสรวง"
            else -> null
        }
    }

    // คำนวณดิถีแบบ Anchor สำหรับปี 2569
    fun getThaiLunar(date: LocalDate): Pair<Int, Int> {
        val anchors = mapOf(
            1 to Pair(19, 3), 2 to Pair(17, 4), 3 to Pair(19, 5),
            4 to Pair(17, 6), 5 to Pair(17, 7), 6 to Pair(15, 8),
            7 to Pair(15, 18), 8 to Pair(14, 9), 9 to Pair(12, 10),
            10 to Pair(12, 11), 11 to Pair(10, 12), 12 to Pair(10, 1)
        )
        
        val anchor = anchors[date.monthOfYear] ?: return Pair(1, 1)
        
        return if (date.dayOfMonth >= anchor.first) {
            Pair((date.dayOfMonth - anchor.first) + 1, anchor.second)
        } else {
            val prevMonthDate = date.minusMonths(1)
            val prevAnchor = anchors[prevMonthDate.monthOfYear] ?: return Pair(1, 1)
            val daysInPrev = date.minusDays(date.dayOfMonth).dayOfMonth
            Pair((daysInPrev - prevAnchor.first) + 1 + date.dayOfMonth, prevAnchor.second)
        }
    }
}
