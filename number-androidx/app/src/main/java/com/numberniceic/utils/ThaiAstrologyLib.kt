package com.numberniceic.utils

import com.numberniceic.BuildConfig
import org.joda.time.DateTime
import org.joda.time.Days
import org.joda.time.LocalDate
import org.json.JSONObject

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
    private const val DEFAULT_SONGKRAN_CUTOVER_MONTH_DAY = "04-16"

    data class CalendarTagBundle(
        val kalTags: List<String>,
        val dithiTags: List<String>,
        val dayTypeTags: List<String>,
        val warningTags: List<String>,
        val otherTags: List<String>
    ) {
        fun prioritized(): List<String> =
            kalTags + dithiTags + dayTypeTags + warningTags + otherTags
    }

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
    private val songkranCutoverDateByYear: Map<Int, String> by lazy { parseSongkranCutoverDateByYear() }

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

    private fun dayOfFortnight(dithiRaw: Int): Int {
        return if (dithiRaw > 15) dithiRaw - 15 else dithiRaw
    }

    private fun parseSongkranCutoverDateByYear(): Map<Int, String> {
        return try {
            val root = JSONObject(BuildConfig.SONGKRAN_CUTOVER_JSON)
            val years = root.optJSONObject("years") ?: return emptyMap()
            buildMap {
                years.keys().forEach { key ->
                    val year = key.toIntOrNull() ?: return@forEach
                    val value = years.optString(key).takeIf { it.isNotBlank() } ?: return@forEach
                    put(year, value)
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun getSongkranCutoverDate(year: Int): LocalDate {
        val configured = songkranCutoverDateByYear[year]
        val fallback = "$year-$DEFAULT_SONGKRAN_CUTOVER_MONTH_DAY"
        return LocalDate.parse(configured ?: fallback)
    }

    private fun getMyHoraSolarZodiacName(date: LocalDate): String {
        val month = date.monthOfYear
        val day = date.dayOfMonth
        return when {
            (month == 4 && day >= 13) || (month == 5 && day <= 13) -> "เมษ"
            (month == 5 && day >= 14) || (month == 6 && day <= 13) -> "พฤษภ"
            (month == 6 && day >= 14) || (month == 7 && day <= 14) -> "มิถุน"
            (month == 7 && day >= 15) || (month == 8 && day <= 16) -> "กรกฎ"
            (month == 8 && day >= 17) || (month == 9 && day <= 16) -> "สิงห์"
            (month == 9 && day >= 17) || (month == 10 && day <= 16) -> "กันย์"
            (month == 10 && day >= 17) || (month == 11 && day <= 15) -> "ตุล"
            (month == 11 && day >= 16) || (month == 12 && day <= 15) -> "พิจิก"
            (month == 12 && day >= 16) || (month == 1 && day <= 15) -> "ธนู"
            (month == 1 && day >= 16) || (month == 2 && day <= 12) -> "มกร"
            (month == 2 && day >= 13) || (month == 3 && day <= 13) -> "กุมภ์"
            else -> "มีน"
        }
    }

    private fun getMyHoraMahasunTags(date: LocalDate, mThai: Int, dithiRaw: Int): List<String> {
        val day = dayOfFortnight(dithiRaw)
        val normalizedMonth = normalizedThaiMonth(mThai)
        val solarZodiac = getMyHoraSolarZodiacName(date)
        val solarMap = mapOf(
            "เมษ" to 6, "พฤษภ" to 4, "มิถุน" to 8, "กรกฎ" to 6,
            "สิงห์" to 10, "กันย์" to 8, "ตุล" to 12, "พิจิก" to 10,
            "ธนู" to 2, "มกร" to 12, "กุมภ์" to 4, "มีน" to 2
        )
        val lunarMap = mapOf(
            6 to 4, 3 to 4, 7 to 8, 10 to 8, 8 to 6, 5 to 6,
            11 to 12, 2 to 12, 9 to 10, 12 to 10, 1 to 2, 4 to 2
        )

        return buildList {
            if (solarMap[solarZodiac] == day) add("มหาสูญ [ก]")
            if (lunarMap[normalizedMonth] == day) add("มหาสูญ [ข]")
        }
    }

    private fun getMyHoraAyakarnTag(mThai: Int, dithiRaw: Int): String? {
        val day = dayOfFortnight(dithiRaw)
        val normalizedMonth = normalizedThaiMonth(mThai)
        val rules = mapOf(
            3 to mapOf(4 to "ปฐม", 5 to "ทุติยะ", 6 to "ตติยะ"),
            7 to mapOf(4 to "ปฐม", 5 to "ทุติยะ", 6 to "ตติยะ"),
            4 to mapOf(1 to "ปฐม", 2 to "ทุติยะ", 3 to "ตติยะ"),
            10 to mapOf(1 to "ปฐม", 2 to "ทุติยะ", 3 to "ตติยะ"),
            5 to mapOf(13 to "ปฐม", 14 to "ทุติยะ", 15 to "ตติยะ"),
            11 to mapOf(13 to "ปฐม", 14 to "ทุติยะ", 15 to "ตติยะ"),
            6 to mapOf(10 to "ปฐม", 11 to "ทุติยะ", 12 to "ตติยะ"),
            8 to mapOf(6 to "ปฐม", 7 to "ทุติยะ", 8 to "ตติยะ"),
            9 to mapOf(3 to "ปฐม", 4 to "ทุติยะ", 5 to "ตติยะ"),
            12 to mapOf(2 to "ปฐม", 3 to "ทุติยะ", 4 to "ตติยะ"),
            1 to mapOf(9 to "ปฐม", 10 to "ทุติยะ", 11 to "ตติยะ"),
            2 to mapOf(7 to "ปฐม", 8 to "ทุติยะ", 9 to "ตติยะ")
        )
        return rules[normalizedMonth]?.get(day)?.let { "อายกรรมพลาย$it" }
    }

    private fun getMyHoraTrathuekTag(mThai: Int, dithiRaw: Int): String? {
        val day = dayOfFortnight(dithiRaw)
        val normalizedMonth = normalizedThaiMonth(mThai)
        val rules = mapOf(
            5 to 7, 6 to 7, 7 to 7, 8 to 8, 9 to 8, 10 to 8,
            11 to 9, 12 to 9, 1 to 9, 2 to 4, 3 to 4, 4 to 4
        )
        return if (rules[normalizedMonth] == day) "ทรทึก" else null
    }

    fun getMyHoraAgniTag(dithiRaw: Int): String? {
        val day = dayOfFortnight(dithiRaw)
        val targets = mapOf(
            1 to "อัคนิโรธ (-สัตว์)",
            2 to "อัคนิโรธ (-ป่า)",
            3 to "อัคนิโรธ (-น้ำ)",
            4 to "อัคนิโรธ (-ภูเขา)",
            5 to "อัคนิโรธ (-ที่ดิน)",
            6 to "อัคนิโรธ (-บ้าน)",
            7 to "อัคนิโรธ (-วัง)",
            8 to "อัคนิโรธ (-รถ)",
            9 to "อัคนิโรธ (-ดิน)",
            10 to "อัคนิโรธ (-เรือ)",
            11 to "อัคนิโรธ (-พืช)",
            12 to "อัคนิโรธ (-สตรี)",
            13 to "อัคนิโรธ (-บุรุษ)",
            14 to "อัคนิโรธ (-พัทธสีมา)",
            15 to "อัคนิโรธ (-เทพ)"
        )
        return targets[day]
    }

    fun getMyHoraLoyFuJomTags(dithiRaw: Int, mThai: Int): List<String> {
        val month = normalizedThaiMonth(mThai)
        val rules = mapOf(
            1 to Triple(6, 1, 3),
            2 to Triple(0, 2, 4),
            3 to Triple(5, 0, 2),
            4 to Triple(5, 0, 2),
            5 to Triple(4, 6, 1),
            6 to Triple(6, 1, 3),
            7 to Triple(5, 0, 2),
            8 to Triple(5, 0, 2),
            9 to Triple(2, 4, 6),
            10 to Triple(1, 3, 5),
            11 to Triple(1, 3, 5),
            12 to Triple(0, 2, 4)
        )
        val (loy, fu, jom) = rules[month] ?: rules.getValue(4)
        val residue = ((dithiRaw % 7) + 7) % 7
        return buildList {
            if (residue == loy) add("วันลอย")
            if (residue == fu) add("วันฟู")
            if (residue == jom) add("วันจม")
        }
    }

    private fun isGoodDithiTag(tag: String): Boolean {
        return tag in setOf(
            "มหาสิทธิโชค", "สิทธิโชค", "อำฤตโชค", "อมุตโชค", "ราชาโชค", "ชัยโชค",
            "ไชยดิถี", "ภัทรดีถี", "ปุณณดีถี", "นันทดีถี", "มิตตะดีถี"
        )
    }

    private fun isPrimaryDithiTag(tag: String): Boolean {
        if (isGoodDithiTag(tag)) return true
        if (tag in setOf(
                "พิฆาต", "ดิถีพิฆาต", "ดิถีเรียงหมอน", "กระทิงวัน", "ทรทึก",
                "ทึกทึน", "อัตนิโรจน์", "ทินสูญ", "กาฬโชค", "กาลสูญ",
                "โลกวินาส", "วินาสส์", "วันบอด", "กาลทีน", "กาลกรรณี",
                "กาลทิน", "ทินสูรย์", "กาลสูร", "กาลโชค", "ยมขันธ์", "ทักทิน", "ทัคธทิน",
                "วินาศ", "โลกาวินาศ", "มฤตยู"
            )) return true
        return tag.startsWith("มหาสูญ") || tag.startsWith("อายกรรมพลาย")
    }

    private fun classifyCalendarTag(tag: String): String {
        return when {
            tag in setOf("วันธงชัย", "วันอธิบดี", "วันอุบาทว์/อุบาสน", "วันโลกาวินาศ") -> "kal"
            tag in setOf("วันลอย", "วันฟู", "วันจม") -> "day_type"
            tag.startsWith("อัคนิโรธ") -> "warning"
            isPrimaryDithiTag(tag) -> "dithi"
            else -> "other"
        }
    }

    fun normalizeMyHoraDisplayTags(tags: List<String>): List<String> {
        val normalized = mutableListOf<String>()
        val seen = linkedSetOf<String>()
        var chosenDayType: String? = null

        tags.forEach { raw ->
            val tag = raw.trim()
            if (tag.isEmpty() || !seen.add(tag)) return@forEach
            if (tag in setOf("วันลอย", "วันฟู", "วันจม")) {
                if (chosenDayType != null) return@forEach
                chosenDayType = tag
            }
            normalized += tag
        }

        return normalized
    }

    fun buildMyHoraTagBundle(displayTags: List<String>): CalendarTagBundle {
        val kal = mutableListOf<String>()
        val dithi = mutableListOf<String>()
        val dayType = mutableListOf<String>()
        val warning = mutableListOf<String>()
        val other = mutableListOf<String>()

        displayTags.forEach { tag ->
            when (classifyCalendarTag(tag)) {
                "kal" -> kal += tag
                "dithi" -> dithi += tag
                "day_type" -> dayType += tag
                "warning" -> warning += tag
                else -> other += tag
            }
        }

        return CalendarTagBundle(kal, dithi, dayType, warning, other)
    }

    fun buildMyHoraDisplayTags(date: LocalDate, verifiedOverrides: Map<String, List<String>>? = null): List<String> {
        verifiedOverrides?.get(date.toString("yyyy-MM-dd"))?.let { overrideTags ->
            return normalizeMyHoraDisplayTags(overrideTags)
        }

        val (dithi, mThai) = getThaiLunar(date)
        val kalayok = getKalayok(date)
        val chok = queryMahaChok(date.dayOfWeek, dithi)
        val tags = mutableListOf<String>()

        if ("มหาสิทธิโชค" in chok) tags += "มหาสิทธิโชค"
        if ("สิทธิโชค" in chok) tags += "สิทธิโชค"
        if ("อำฤตโชค" in chok) tags += "อำฤตโชค"
        if ("ราชาโชค" in chok) tags += "ราชาโชค"
        if ("ชัยโชค" in chok) tags += "ชัยโชค"

        tags += queryMyHoraPrimaryBadTags(date.dayOfWeek, dithi)
        tags += getMyHoraMahasunTags(date, mThai, dithi)
        getMyHoraAyakarnTag(mThai, dithi)?.let { tags += it }
        if (isKatingDay(date)) tags += "กระทิงวัน"
        getMyHoraTrathuekTag(mThai, dithi)?.let { tags += it }

        if ("ธงชัย" in kalayok) tags += "วันธงชัย"
        if ("อธิบดี" in kalayok) tags += "วันอธิบดี"
        if ("อุบาทว์" in kalayok) tags += "วันอุบาทว์/อุบาสน"
        if ("โลกาวินาศ" in kalayok) tags += "วันโลกาวินาศ"

        tags += getMyHoraLoyFuJomTags(dithi, mThai)
        if (isRiangMon(dithi)) tags += "ดิถีเรียงหมอน"
        getMyHoraAgniTag(dithi)?.let { tags += it }

        return normalizeMyHoraDisplayTags(tags)
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

    // ตารางผังดาว (3x3 Grid) - ทิศมาตรฐาน
    // 6 1 2
    // 5 0 3
    // 4 8 7
    val BOARD = arrayOf(
        intArrayOf(6, 1, 2),
        intArrayOf(5, 0, 3),
        intArrayOf(4, 8, 7)
    )

    // ลำดับการนับ (Clockwise Path) - อาทิตย์(1) -> จันทร์(2) -> อังคาร(3) -> พุธ(4) -> เสาร์(7) -> พฤหัส(5) -> ราหู(8) -> ศุกร์(6)
    // จุดพัก (0) จะถูกข้ามในการนับปกติ
    val MASTER_PATH = listOf(
        TaksaPoint(0, 1), // 1 อาทิตย์
        TaksaPoint(0, 2), // 2 จันทร์
        TaksaPoint(1, 2), // 3 อังคาร
        intArrayOf(1, 0).let { TaksaPoint(2, 2) }, // 4 พุธ(กลางวัน) - Wait, let's just list points directly
        TaksaPoint(2, 2), // 4 พุธ(กลางวัน) - No, standard sequence: 1,2,3,4,7,5,8,6
        // Fixed coordinates for sequence 1,2,3,4,7,5,8,6
        TaksaPoint(0, 1), // 1 (N)
        TaksaPoint(0, 2), // 2 (NE)
        TaksaPoint(1, 2), // 3 (E)
        TaksaPoint(2, 2), // 4 (SE)
        TaksaPoint(2, 1), // 8 (S) - Rahu
        TaksaPoint(2, 0), // 7 (SW) - Saturday
        TaksaPoint(1, 0), // 5 (W) - Thursday
        TaksaPoint(0, 0)  // 6 (NW) - Friday
    )
    // Actually the standard 8-direction sequence in Taksa is: 1, 2, 3, 4, 7, 5, 8, 6.
    // Let's use a explicit list of day numbers in order to find indices.
    private val TAKSA_SEQUENCE = listOf(1, 2, 3, 4, 7, 5, 8, 6)
    private val TAKSA_COORDS = listOf(
        TaksaPoint(0, 1), // 1 อาทิตย์
        TaksaPoint(0, 2), // 2 จันทร์
        TaksaPoint(1, 2), // 3 อังคาร
        TaksaPoint(2, 2), // 4 พุธ(กลางวัน)
        TaksaPoint(2, 1), // 8 ราหู
        TaksaPoint(2, 0), // 7 เสาร์
        TaksaPoint(1, 0), // 5 พฤหัส
        TaksaPoint(0, 0)  // 6 ศุกร์
    )

    fun calculateTaksa(birthDate: LocalDate, birthDayNum: Int): TaksaResult {
        return calculateTaksa(birthDate, birthDayNum, LocalDate.now())
    }

    fun calculateTaksa(birthDate: LocalDate, birthDayNum: Int, referenceDate: LocalDate): TaksaResult {
        // 1. คำนวณอายุย่าง ณ วันอ้างอิง
        var ageFull = referenceDate.year - birthDate.year
        if (referenceDate.monthOfYear < birthDate.monthOfYear || (referenceDate.monthOfYear == birthDate.monthOfYear && referenceDate.dayOfMonth < birthDate.dayOfMonth)) {
            ageFull--
        }
        val ageNext = ageFull + 1

        // 2. หาจุดเริ่มต้น (ตำแหน่งวันเกิดในวงโคจร 8 ทิศ)
        val startIdx = TAKSA_SEQUENCE.indexOf(birthDayNum)
        val birthPos = TAKSA_COORDS[startIdx]

        // 3. คำนวณจุดปัจจุบัน (นับตามอายุย่าง 1 ก้าวต่อปี)
        // เริ่มก้าวที่ 1 ที่วันเกิดตัวเอง
        val currentIdx = (startIdx + (ageNext - 1)) % 8
        val targetPos = TAKSA_COORDS[currentIdx]

        // 4. คำนวณกาลกิณี (จุดเสีย)
        // ในทักษา กาลกิณีคือจุดที่ 8 จากจุดตั้งต้น (หรือถอยหลัง 1 ก้าว)
        
        // กาลกิณีจร (ตามอายุย่าง)
        val badIdx = (currentIdx + 7) % 8
        val badPos = TAKSA_COORDS[badIdx]

        // กาลกิณีวันเกิด (ถาวร)
        val birthBadIdx = (startIdx + 7) % 8
        val birthBadPos = TAKSA_COORDS[birthBadIdx]

        return TaksaResult(BOARD, TAKSA_COORDS, targetPos, badPos, birthBadPos, birthPos, ageNext, ageNext)
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
        val weekday = date.dayOfWeek // 1=Mon ... 7=Sun
        val cutoverDate = getSongkranCutoverDate(date.year)
        val csYear = if (!date.isBefore(cutoverDate)) date.year - 638 else date.year - 639
        val remainder = ((csYear % 7) + 7) % 7
        val rem = if (remainder == 0) 7 else remainder

        if (weekday == kalayokTongchaiByCsRemainder[rem]) tags.add("ธงชัย")
        if (weekday == kalayokAtipbadeeByCsRemainder[rem]) tags.add("อธิบดี")
        if (weekday == kalayokUbathByCsRemainder[rem]) tags.add("อุบาทว์")
        if (weekday == kalayokLokawinatByCsRemainder[rem]) tags.add("โลกาวินาศ")

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

        if (w == 5) {
            res.add("ห้ามเผาผี")
        }

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
     * MyHora primary bad dithi rules.
     * Weekday uses Joda/ISO numbering: Mon=1 ... Sun=7.
     */
    fun queryMyHoraPrimaryBadTags(w: Int, dithiRaw: Int): List<String> {
        var d = dithiRaw
        if (d > 15) d -= 15

        val badMap = linkedMapOf(
            "ทักทิน" to mapOf(7 to 1, 1 to 4, 2 to 5, 3 to 9, 4 to 5, 5 to 3, 6 to 7),
            "ยมขันธ์" to mapOf(7 to 12, 1 to 11, 2 to 7, 3 to 3, 4 to 6, 5 to 8, 6 to 9),
            "ทัคธทิน" to mapOf(7 to 4, 1 to 6, 2 to 1, 3 to 3, 4 to 3, 5 to 9, 6 to 1),
            "ทินกาล" to mapOf(7 to 12, 1 to 10, 2 to 15, 3 to 8, 4 to 5, 5 to listOf(3, 7), 6 to 8),
            "ทินสูรย์" to mapOf(7 to 4, 1 to 6, 2 to 1, 3 to 3, 4 to listOf(3, 7), 5 to 9, 6 to 1),
            "กาลโชค" to mapOf(7 to 4, 1 to 2, 2 to 7, 3 to 5, 4 to 8, 5 to 3, 6 to 6),
            "กาลสูร" to mapOf(7 to 12, 1 to 11, 2 to 10, 3 to 9, 4 to 8, 5 to 7, 6 to 6),
            "กาลทัณฑ์" to mapOf(7 to 4, 1 to 6, 2 to 10, 3 to 9, 4 to 8, 5 to 9, 6 to 1),
            "โลกาวินาศ" to mapOf(7 to 4, 1 to 5, 2 to 6, 3 to 6, 4 to 8, 5 to 8, 6 to 9),
            "วินาศ" to mapOf(7 to 6, 1 to 10, 2 to 8, 3 to 7, 4 to 12, 5 to 9, 6 to 12),
            "พิลา" to mapOf(7 to 9, 1 to 1, 2 to 10, 3 to 9, 4 to 8, 5 to 7, 6 to 6),
            "มฤตยู" to mapOf(7 to 7, 1 to 8, 2 to 4, 3 to 7, 4 to 1, 5 to 14, 6 to 11),
            "บอด" to mapOf(7 to 5, 1 to 6, 2 to 10, 3 to 8, 4 to 11, 5 to 5, 6 to 7),
            "กาลทิน" to mapOf(7 to 12, 1 to 11, 2 to 7, 3 to 3, 4 to 6, 5 to 8, 6 to 9),
            "พิฆาต" to mapOf(7 to 12, 1 to 11, 2 to 7, 3 to 3, 4 to 6, 5 to 9, 6 to 8),
            "กาลกรรณี" to mapOf(7 to 5, 1 to 6, 2 to 15, 3 to 8, 4 to 1, 5 to 2, 6 to 10)
        )

        fun matches(ruleValue: Any?): Boolean {
            return when (ruleValue) {
                is Int -> ruleValue == d
                is List<*> -> ruleValue.filterIsInstance<Int>().contains(d)
                else -> false
            }
        }

        val matches = badMap
            .filterValues { rule -> matches(rule[w]) }
            .keys
            .toList()

        if ("พิฆาต" in matches) {
            return listOf("พิฆาต", "ดิถีพิฆาต")
        }

        val priority = listOf(
            "กาลกรรณี",
            "พิฆาต",
            "ทินสูรย์",
            "กาลทิน",
            "มฤตยู",
            "บอด",
            "วินาศ",
            "โลกาวินาศ",
            "กาลสูร",
            "กาลโชค",
            "ทินกาล",
            "ทัคธทิน",
            "ยมขันธ์",
            "ทักทิน"
        )

        return priority.firstOrNull { it in matches }?.let(::listOf).orEmpty()
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
        if (dithiRaw == 7 || dithiRaw == 22) return "ห้ามแต่งงาน" // ขึ้น 7 ค่ำ / แรม 7 ค่ำ
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
