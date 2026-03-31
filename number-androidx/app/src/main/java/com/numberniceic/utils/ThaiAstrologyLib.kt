package com.numberniceic.utils

import org.joda.time.DateTime
import org.joda.time.Days
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
    private const val LUNAR_EPOCH_GREGORIAN_YEAR = 1900
    private const val YEAR_SELECTION_RADIUS = 12

    private data class LunarState(
        val lunarYear: Int,
        val thaiMonth: Int,
        val dithi: Int
    )

    private val kalayokTongchaiByCsRemainder = intArrayOf(0, 5, 1, 4, 7, 3, 6, 2)
    private val kalayokAtipbadeeByCsRemainder = intArrayOf(0, 5, 6, 7, 1, 2, 3, 4)
    private val kalayokUbathByCsRemainder = intArrayOf(0, 4, 7, 3, 6, 2, 5, 1)
    private val kalayokLokawinatByCsRemainder = intArrayOf(0, 7, 1, 2, 3, 4, 5, 6)

    private val jan1LunarStateCache = mutableMapOf(
        LUNAR_EPOCH_GREGORIAN_YEAR to LunarState(
            lunarYear = LUNAR_EPOCH_GREGORIAN_YEAR,
            thaiMonth = 2,
            dithi = 1
        )
    )
    private val lunarMonthLengthsCache = mutableMapOf<Int, Map<Int, Int>>()

    fun getYearSelectionRange(centerYearCe: Int, radius: Int = YEAR_SELECTION_RADIUS): IntRange {
        return (centerYearCe - radius)..(centerYearCe + radius)
    }

    fun getSupportedDisplayYearRange(): IntRange = getYearSelectionRange(LocalDate.now().year)

    fun isSupportedDisplayYear(yearCe: Int): Boolean = true

    fun getSupportedDisplayYearRangeBeLabel(): String {
        val range = getSupportedDisplayYearRange()
        return "${range.first + 543}-${range.last + 543}"
    }

    private fun gregorianYearDayCount(year: Int): Int {
        return if ((year % 400 == 0) || (year % 4 == 0 && year % 100 != 0)) 366 else 365
    }

    private fun buddhistEraYear(gregorianYear: Int): Int = gregorianYear + 544

    private fun aharkun(gregorianYear: Int): Int {
        val be = buddhistEraYear(gregorianYear).toLong()
        return (((be * 292207L) + 499L) / 800L).toInt() + 4
    }

    private fun avoman(gregorianYear: Int): Int {
        return ((11 * aharkun(gregorianYear)) + 25) % 692
    }

    private fun bodithey(gregorianYear: Int): Int {
        val ahk = aharkun(gregorianYear)
        return (((11 * ahk) + 25) / 692 + ahk + 29) % 30
    }

    private fun isSolarLeapYearInLunisolarSystem(gregorianYear: Int): Boolean {
        val be = buddhistEraYear(gregorianYear).toLong()
        return 800 - (((be * 292207L) + 499L) % 800L).toInt() <= 207
    }

    private fun boditheyLeapType(gregorianYear: Int): Int {
        val av = avoman(gregorianYear)
        val bod = bodithey(gregorianYear)

        var isLeapMonth = bod >= 25 || bod <= 5
        if (bod == 25 && bodithey(gregorianYear + 1) == 5) {
            isLeapMonth = false
        }
        if (bod == 24 && bodithey(gregorianYear + 1) == 6) {
            isLeapMonth = true
        }

        val isLeapDay = if (isSolarLeapYearInLunisolarSystem(gregorianYear)) {
            av <= 126
        } else {
            av <= 137 && avoman(gregorianYear + 1) != 0
        }

        return when {
            isLeapMonth && isLeapDay -> 3
            isLeapMonth -> 1
            isLeapDay -> 2
            else -> 0
        }
    }

    private fun lunarYearType(gregorianYear: Int): Int {
        val leapType = boditheyLeapType(gregorianYear)
        return when {
            leapType == 3 -> 1
            leapType == 1 || leapType == 2 -> leapType
            boditheyLeapType(gregorianYear - 1) == 3 -> 2
            else -> 0
        }
    }

    private fun lunarMonthLengths(lunarYear: Int): Map<Int, Int> {
        return lunarMonthLengthsCache.getOrPut(lunarYear) {
            when (lunarYearType(lunarYear)) {
                1 -> linkedMapOf(
                    1 to 29,
                    2 to 30,
                    3 to 29,
                    4 to 30,
                    5 to 29,
                    6 to 30,
                    7 to 29,
                    8 to 30,
                    18 to 30,
                    9 to 29,
                    10 to 30,
                    11 to 29,
                    12 to 30
                )
                2 -> linkedMapOf(
                    1 to 29,
                    2 to 30,
                    3 to 29,
                    4 to 30,
                    5 to 29,
                    6 to 30,
                    7 to 30,
                    8 to 30,
                    9 to 29,
                    10 to 30,
                    11 to 29,
                    12 to 30
                )
                else -> linkedMapOf(
                    1 to 29,
                    2 to 30,
                    3 to 29,
                    4 to 30,
                    5 to 29,
                    6 to 30,
                    7 to 29,
                    8 to 30,
                    9 to 29,
                    10 to 30,
                    11 to 29,
                    12 to 30
                )
            }
        }
    }

    private fun normalizedThaiMonth(thaiMonth: Int): Int {
        return when {
            thaiMonth == 18 || thaiMonth == 13 -> 8
            thaiMonth > 12 -> thaiMonth - 12
            else -> thaiMonth
        }
    }

    private fun lunarMonthLength(thaiMonth: Int, lunarYear: Int): Int {
        return lunarMonthLengths(lunarYear)[thaiMonth]
            ?: error("Unknown Thai lunar month $thaiMonth in lunar year $lunarYear")
    }

    private fun moveToNextLunarMonth(state: LunarState): LunarState {
        val sequence = lunarMonthLengths(state.lunarYear).keys.toList()
        val index = sequence.indexOf(state.thaiMonth)
        check(index >= 0) { "Unable to advance lunar month ${state.thaiMonth} in lunar year ${state.lunarYear}" }

        return if (index + 1 < sequence.size) {
            state.copy(thaiMonth = sequence[index + 1])
        } else {
            state.copy(lunarYear = state.lunarYear + 1, thaiMonth = 1)
        }
    }

    private fun moveToPreviousLunarMonth(state: LunarState): LunarState {
        val sequence = lunarMonthLengths(state.lunarYear).keys.toList()
        val index = sequence.indexOf(state.thaiMonth)
        check(index >= 0) { "Unable to rewind lunar month ${state.thaiMonth} in lunar year ${state.lunarYear}" }

        return if (index > 0) {
            state.copy(thaiMonth = sequence[index - 1])
        } else {
            val previousLunarYear = state.lunarYear - 1
            val previousSequence = lunarMonthLengths(previousLunarYear).keys.toList()
            state.copy(lunarYear = previousLunarYear, thaiMonth = previousSequence.last())
        }
    }

    private fun advanceLunarStateByDays(initialState: LunarState, deltaDays: Int): LunarState {
        var state = initialState
        var delta = deltaDays

        while (delta > 0) {
            val currentMonthLength = lunarMonthLength(state.thaiMonth, state.lunarYear)
            val remainingInMonth = currentMonthLength - state.dithi
            if (delta <= remainingInMonth) {
                return state.copy(dithi = state.dithi + delta)
            }

            delta -= remainingInMonth + 1
            state = moveToNextLunarMonth(state).copy(dithi = 1)
        }

        while (delta < 0) {
            val daysBeforeCurrent = state.dithi - 1
            if (-delta <= daysBeforeCurrent) {
                return state.copy(dithi = state.dithi + delta)
            }

            delta += daysBeforeCurrent + 1
            val previousState = moveToPreviousLunarMonth(state)
            state = previousState.copy(
                dithi = lunarMonthLength(previousState.thaiMonth, previousState.lunarYear)
            )
        }

        return state
    }

    private fun getLunarStateAtJan1(gregorianYear: Int): LunarState {
        jan1LunarStateCache[gregorianYear]?.let { return it }

        val state = if (gregorianYear > LUNAR_EPOCH_GREGORIAN_YEAR) {
            advanceLunarStateByDays(
                getLunarStateAtJan1(gregorianYear - 1),
                gregorianYearDayCount(gregorianYear - 1)
            )
        } else {
            advanceLunarStateByDays(
                getLunarStateAtJan1(gregorianYear + 1),
                -gregorianYearDayCount(gregorianYear)
            )
        }

        jan1LunarStateCache[gregorianYear] = state
        return state
    }

    fun canResolveThaiLunar(date: LocalDate): Boolean = true

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
     * คำนวณแบบ dynamic ตามปีจุลศักราช
     */
    fun getKalayok(date: LocalDate): List<String> {
        val tags = mutableListOf<String>()
        val w = date.dayOfWeek // 1=Mon ... 7=Sun
        val csYear = if (!date.isBefore(LocalDate(date.year, 4, 16))) date.year - 638 else date.year - 639
        val remainder = ((csYear % 7) + 7) % 7
        val rem = if (remainder == 0) 7 else remainder

        if (w == kalayokTongchaiByCsRemainder[rem]) tags.add("ธงชัย")
        if (w == kalayokAtipbadeeByCsRemainder[rem]) tags.add("อธิบดี")
        if (w == kalayokUbathByCsRemainder[rem]) tags.add("อุบาทว์")
        if (w == kalayokLokawinatByCsRemainder[rem]) tags.add("โลกาวินาศ")

        return tags
    }

    /**
     * 7.1 วันลอย-วันฟู-วันจม
     * วันฟู/วันจมยึดตามตาราง mahamodo โดยใช้เดือนจันทรคติและวันในสัปดาห์ตรง ๆ
     * ส่วนวันลอยคง logic เดิมไว้เพื่อไม่ให้กระทบพฤติกรรมเดิมนอกเหนือจากที่ร้องขอ
     */
    fun getLoyFuJom(weekday: Int, dithiRaw: Int, mThai: Int): List<String> {
        val res = mutableListOf<String>()
        val month = normalizedThaiMonth(mThai)
        val residue = ((dithiRaw % 7) + 7) % 7

        val loyResidue = when (month) {
            1, 6 -> 6
            2, 12 -> 0
            3, 4, 7, 8 -> 5
            5 -> 4
            9 -> 2
            10, 11 -> 1
            else -> 5
        }
        val jomFuRule = when (month) {
            1 -> Pair(5, 1) // ศุกร์, จันทร์
            2 -> Pair(6, 2) // เสาร์, อังคาร
            3, 8 -> Pair(7, 3) // อาทิตย์, พุธ
            4, 9 -> Pair(1, 4) // จันทร์, พฤหัสบดี
            5, 10 -> Pair(2, 5) // อังคาร, ศุกร์
            6, 11 -> Pair(3, 6) // พุธ, เสาร์
            7, 12 -> Pair(4, 7) // พฤหัสบดี, อาทิตย์
            else -> null
        }

        if (residue == loyResidue) res.add("วันลอย")
        if (jomFuRule != null) {
            if (weekday == jomFuRule.first) res.add("วันจม")
            if (weekday == jomFuRule.second) res.add("วันฟู")
        }

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
            // ตามเงื่อนไขปัจจุบัน: อาทิตย์ 5, จันทร์ 2, อังคาร 8, พุธ 3, พฤหัส 1, ศุกร์ 9, เสาร์ 8
            "ทินสูญ" to mapOf(7 to 5, 1 to 2, 2 to 8, 3 to 3, 4 to 1, 5 to 9, 6 to 8),
            "กาฬโชค" to mapOf(7 to 4, 1 to 6, 2 to 1, 3 to 3, 4 to 8, 5 to 9, 6 to 10),
            "กาลสูญ" to mapOf(7 to 4, 1 to 2, 2 to 7, 3 to 5, 4 to 8, 5 to 3, 6 to 6),
            "กาลทัณฑ์" to mapOf(7 to 12, 1 to 11, 2 to 10, 3 to 9, 4 to 8, 5 to 7, 6 to 1),
            "โลกวินาส" to mapOf(7 to 4, 1 to 6, 2 to 10, 3 to 9, 4 to 8, 5 to 9, 6 to 1),
            "วินาสส์" to mapOf(7 to 4, 1 to 8, 2 to 6, 3 to 4, 4 to 8, 5 to 8, 6 to 9),
            "พิลา" to mapOf(7 to 6, 1 to 10, 2 to 8, 3 to 7, 4 to 2, 5 to 9, 6 to 12),
            "มฤตยู" to mapOf(7 to 9, 1 to 1, 2 to 10, 3 to 9, 4 to 8, 5 to 7, 6 to 6),
            "วันบอด" to mapOf(7 to 7, 1 to 8, 2 to 4, 3 to 7, 4 to 1, 5 to 14, 6 to 11),
            "กาลทีน" to mapOf(7 to 5, 1 to 6, 2 to 10, 3 to 8, 4 to 11, 5 to 5, 6 to 7)
            ,
            // ทักทินไฟ (ห้ามแต่งงาน): อาทิตย์ 12, เสาร์ 6, จันทร์ 11, อังคาร 10, พุธ 9, พฤหัส 8, ศุกร์ 7
            "ทักทินไฟ" to mapOf(7 to 12, 6 to 6, 1 to 11, 2 to 10, 3 to 9, 4 to 8, 5 to 7)
        )

        badMap.forEach { (name, rule) ->
            if (rule[w] == d) {
                res.add(name)
                if (name == "ทักทินไฟ") res.add("ห้ามแต่งงาน")
            }
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
        val m = normalizedThaiMonth(mThai)

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
        if (dithiRaw == 15 || dithiRaw == 30) return "ห้ามเผาผี" // ขึ้น 15 ค่ำ / แรม 15 ค่ำ
        if (dithiRaw == 22) return "ห้ามแต่งงาน" // แรม 7 ค่ำ
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

    fun isWanPra(date: LocalDate): Boolean {
        val (dithi, _) = getThaiLunar(date)
        val day = if (dithi > 15) dithi - 15 else dithi
        return day == 8 || day == 15
    }

    fun isKatingDay(date: LocalDate): Boolean {
        val (dithi, thaiMonth) = getThaiLunar(date)
        val day = if (dithi > 15) dithi - 15 else dithi
        val sundayBasedWeekday = if (date.dayOfWeek == 7) 1 else date.dayOfWeek + 1
        val normalizedMonth = normalizedThaiMonth(thaiMonth)
        return sundayBasedWeekday == day ||
            normalizedMonth == day
    }

    // คำนวณดิถีแบบ dynamic lunisolar arithmetic
    fun getThaiLunar(date: LocalDate): Pair<Int, Int> {
        val jan1State = getLunarStateAtJan1(date.year)
        val deltaDays = Days.daysBetween(LocalDate(date.year, 1, 1), date).days
        val state = advanceLunarStateByDays(jan1State, deltaDays)
        return state.dithi to state.thaiMonth
    }
}
