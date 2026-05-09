package com.numberniceic.ui.renkyam

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.numberniceic.R
import com.numberniceic.adapters.LegendData
import com.numberniceic.adapters.TopHeaderData
import com.numberniceic.adapters.WanpraAdapter
import com.numberniceic.data.admin.Serverx
import com.numberniceic.data.persons.OutfitMiracleColorSetsRequest
import com.numberniceic.data.rengyam.LengYamDao
import com.numberniceic.data.rengyam.TagDetail
import com.numberniceic.data.rengyam.WanSpecial
import com.numberniceic.data.rengyam.Wanpra
import com.numberniceic.data.rengyam.WanpraDao
import com.numberniceic.databinding.FragmentRengYamBinding
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.apersonnews.ViewModelHelper
import com.numberniceic.utils.PersonContextManager
import com.numberniceic.utils.PersonNewsCacheManager
import com.numberniceic.utils.RengYamTagCanonicalizer
import com.numberniceic.utils.ThaiAstrologyLib
import com.numberniceic.utils.UserContextManager
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class RengYamF : Fragment() {
    private data class TravelDirectionInfo(
        val deityDirection: String,
        val spearDirection: String,
        val ghostDirection: String
    )

    companion object {
        private const val RENGYAM_USAGE_PREFS = "rengyam_usage_prefs"
        private const val RENGYAM_ACCESS_GRANTED_KEY = "rengyam_access_granted"
        private const val RENGYAM_ACCESS_EXPIRE_AT_KEY = "rengyam_access_expire_at"
        private const val MARRIAGE_FORM_REFERENCE_NOTE =
            "หมายเหตุ: รายการนี้เป็นค่าอ้างอิงของปีปัจจุบัน ตอนค้นหาฤกษ์จริงระบบจะคำนวณอายุย่างและวันอัปมงคลใหม่ให้ตามแต่ละปีของวันที่ค้นหา"

        private const val PREF_MARRIAGE_GROOM_DATE = "last_marriage_groom_date"
        private const val PREF_MARRIAGE_GROOM_TIME = "last_marriage_groom_time"
        private const val PREF_MARRIAGE_BRIDE_DATE = "last_marriage_bride_date"
        private const val PREF_MARRIAGE_BRIDE_TIME = "last_marriage_bride_time"
        private const val PREF_MARRIAGE_GROOM_PROVINCE = "last_marriage_groom_province"
        private const val PREF_MARRIAGE_BRIDE_PROVINCE = "last_marriage_bride_province"

        fun newInstance(): RengYamF {
            return RengYamF()
        }
    }

    private lateinit var rengYamBinding: FragmentRengYamBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rengYamBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_reng_yam, container, false)
        val rengYamObs = ViewModelProvider(this).get(RengYamObs::class.java)
        rengYamBinding.rengYamObs = rengYamObs
        rengYamBinding.lifecycleOwner = this

        return rengYamBinding.root
    }

    private var fullDataList = ArrayList<Any>()
    private var headerData: TopHeaderData = TopHeaderData()
    private var currentViewDate: DateTime = DateTime()
    private var cachedLengYamDao: LengYamDao? = null
    private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
    private var dateToWanpraMap = mutableMapOf<String, Wanpra>()
    private var isAdminMode: Boolean = false
    private var isLoggedInUser: Boolean = false
    private var marriageSearchNote: CharSequence? = null
    private var memberBirthDate: org.joda.time.LocalDate? = null
    private var memberBirthDayNumber: Int? = null
    private var memberBirthDayThai: String? = null
    
    // Marriage Search Persistence
    private var lastGroomDate: org.joda.time.LocalDate? = null
    private var lastGroomTime: String? = null
    private var lastBrideDate: org.joda.time.LocalDate? = null
    private var lastBrideTime: String? = null
    private var lastGroomProvince: String? = null
    private var lastBrideProvince: String? = null
    private var hasRengyamAccess: Boolean = false
    private var isCalendarScrollLocked: Boolean = false
    private var wanpraLayoutManager: androidx.recyclerview.widget.GridLayoutManager? = null
    private var frozenTopHeaderHolder: WanpraAdapter.TopHeaderHolder? = null
    private val showTopHeaderInauspiciousInfo: Boolean = true
    private var loadingRequests: Int = 0
    private var myHoraVerifiedOverridesCache: Map<String, List<String>>? = null
    // Show the full calendar even for guests to encourage login later.
    private fun shouldShowFullCalendar(): Boolean = true
    private fun guestLockedMonthYear(): DateTime = DateTime(2026, 3, 1, 0, 0)

    private fun isFlagEnabled(value: Any?): Boolean {
        val normalized = value?.toString()?.trim()?.lowercase(Locale.ROOT) ?: return false
        return normalized == "1" || normalized == "1.0" || normalized == "true"
    }

    private fun wanpraTags(wp: Wanpra): Set<String> {
        return sequenceOf(
            wp.displayTagsPrioritized,
            wp.displayTags,
            wp.kalTags,
            wp.dithiTags,
            wp.dayTypeTags,
            wp.warningTags
        ).flatMap { (it ?: emptyList()).asSequence() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun wanpraWarningTags(wp: Wanpra): Set<String> {
        val directWarnings = wp.warningTags.orEmpty()
        if (directWarnings.isNotEmpty()) {
            return directWarnings.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
        return wanpraTags(wp).filter { it.startsWith("อัคนิโรธ") }.toSet()
    }

    private fun hasAnyTag(wp: Wanpra, tags: Set<String>): Boolean {
        return wanpraTags(wp).any { it in tags }
    }

    private fun hasTagPrefix(wp: Wanpra, prefix: String): Boolean {
        return wanpraTags(wp).any { it.startsWith(prefix) }
    }

    private fun hasWarningKeyword(wp: Wanpra, keyword: String): Boolean {
        return wanpraWarningTags(wp).any { it.contains(keyword) }
    }

    private fun normalizeAgniknirodTopic(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw
            .replace("อัคนิโรธน์", "")
            .replace("อัคนิโรธ", "")
            .replace("(", "")
            .replace(")", "")
            .replace("-", "")
            .replace(" ", "")
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    private fun extractAgniknirodTopic(tag: String?): String? {
        val clean = tag?.trim().orEmpty()
        if (!clean.contains("อัคนิโรธ")) return null
        val explicitTopic = clean.substringAfter("(-", "").substringBefore(")", "").trim()
        if (explicitTopic.isNotEmpty()) return normalizeAgniknirodTopic(explicitTopic)
        return normalizeAgniknirodTopic(clean)
    }

    private fun sanitizeAgniknirodTags(wp: Wanpra, dt: org.joda.time.LocalDate) {
        val (dithi, _) = ThaiAstrologyLib.getThaiLunar(dt)
        val expectedTopic = extractAgniknirodTopic(ThaiAstrologyLib.queryDithiCommonProhibitions(dithi))

        fun shouldKeep(tag: String): Boolean {
            val tagTopic = extractAgniknirodTopic(tag) ?: return true
            return expectedTopic != null && tagTopic == expectedTopic
        }

        fun filterTags(tags: List<String>?): List<String>? {
            return tags?.filter(::shouldKeep)
        }

        fun filterDetails(details: List<TagDetail>?): List<TagDetail>? {
            return details?.filter { detail ->
                shouldKeep(detail.tag ?: detail.displayTag.orEmpty())
            }
        }

        wp.calendarDisplayTags = filterTags(wp.calendarDisplayTags)
        wp.kalTags = filterTags(wp.kalTags)
        wp.dithiTags = filterTags(wp.dithiTags)
        wp.dayTypeTags = filterTags(wp.dayTypeTags)
        wp.warningTags = filterTags(wp.warningTags)
        wp.displayTags = filterTags(wp.displayTags)
        wp.displayTagsPrioritized = filterTags(wp.displayTagsPrioritized)
        wp.myhoraDisplayTags = filterTags(wp.myhoraDisplayTags)
        wp.myhoraDisplayTagsPrioritized = filterTags(wp.myhoraDisplayTagsPrioritized)
        wp.mahamodoDisplayTags = filterTags(wp.mahamodoDisplayTags)
        wp.mahamodoDisplayTagsPrioritized = filterTags(wp.mahamodoDisplayTagsPrioritized)
        wp.tagDetails = filterDetails(wp.tagDetails)
        wp.myhoraTagDetails = filterDetails(wp.myhoraTagDetails)
        wp.mahamodoTagDetails = filterDetails(wp.mahamodoTagDetails)
    }

    private fun shouldLimitCalendarTagsForPublic(): Boolean {
        return !isLoggedInUser || !hasRengyamAccess
    }

    private fun limitedPublicCalendarTags(tags: List<String>?): List<String>? {
        val distinctTags = tags.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (distinctTags.isEmpty()) return null

        val prioritized = distinctTags.filter { tag ->
            val canonical = RengYamTagCanonicalizer.canonical(tag.substringBefore(" [").trim())
            canonical == "วันอธิบดี" || canonical == "วันธงชัย"
        }

        return (if (prioritized.isNotEmpty()) prioritized else distinctTags.take(3)).ifEmpty { null }
    }

    private fun limitCalendarPreviewForPublic(wp: Wanpra) {
        if (!shouldLimitCalendarTagsForPublic()) return

        val allowedTags = limitedPublicCalendarTags(
            sequenceOf(
                wp.calendarDisplayTags,
                wp.displayTagsPrioritized,
                wp.displayTags,
                wp.kalTags,
                wp.dithiTags,
                wp.dayTypeTags,
                wp.warningTags
            ).flatMap { it.orEmpty().asSequence() }
                .toList()
        )

        wp.calendarDisplayTags = allowedTags
        wp.displayTagsPrioritized = allowedTags
        wp.displayTags = allowedTags
        wp.kalTags = allowedTags
        wp.dithiTags = null
        wp.dayTypeTags = null
        wp.warningTags = null

        val allowedSet = allowedTags.orEmpty().toSet()
        wp.tagDetails = wp.tagDetails
            ?.filter { detail ->
                val tag = detail.tag?.trim().orEmpty()
                allowedSet.contains(tag)
            }
            ?.ifEmpty { null }
    }

    private fun applyPersonalizedKalagniTags(wp: Wanpra) {
        // Remove all existing auspicious warning tags before re-adding (to avoid duplicates)
        val tagsToRemovePrefix = "อัปมงคล"
        fun filterTags(tags: List<String>?): List<String>? = tags?.filter { !it.startsWith(tagsToRemovePrefix) }

        wp.calendarDisplayTags = filterTags(wp.calendarDisplayTags)
        wp.warningTags = filterTags(wp.warningTags)
        wp.displayTags = filterTags(wp.displayTags)
        wp.displayTagsPrioritized = filterTags(wp.displayTagsPrioritized)
        wp.kalTags = filterTags(wp.kalTags)

        // Calendar cells should show a compact label (no weekday suffix).
        fun buildTag(base: String, matchedLabel: String?): String {
            return base
        }

        val personalizedTags = mutableListOf<String>()
        if (wp.kalagniBirth && wp.kalagniAge) {
            val ageTag = buildTag("อัปมงคลอายุย่าง", wp.kalagniAgeLabel)
            val birthTag = buildTag("อัปมงคลวันเกิด", wp.kalagniBirthLabel)
            if (ageTag == birthTag) {
                personalizedTags += ageTag
            } else {
                personalizedTags += ageTag
                personalizedTags += birthTag
            }
        } else {
            if (wp.kalagniBirth) personalizedTags += buildTag("อัปมงคลวันเกิด", wp.kalagniBirthLabel)
            if (wp.kalagniAge)   personalizedTags += buildTag("อัปมงคลอายุย่าง", wp.kalagniAgeLabel)
        }
        if (personalizedTags.isEmpty()) return

        wp.calendarDisplayTags = (wp.calendarDisplayTags.orEmpty() + personalizedTags).distinct()
        wp.warningTags = (wp.warningTags.orEmpty() + personalizedTags).distinct()
    }

    private fun hasPositiveAuspiciousTagRaw(wp: Wanpra): Boolean {
        val positiveTags = setOf(
            "วันธงชัย", "วันอธิบดี", "ดิถีเรียงหมอน",
            "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค",
            "วันลอย", "วันฟู", "กระทิงวัน",
            "ไชยดิถี", "ภัทรดีถี", "ปุณณดีถี", "นันทดีถี", "มิตตะดีถี"
        )
        return hasAnyTag(wp, positiveTags)
    }

    private fun isPlainSafeDay(wp: Wanpra): Boolean {
        // Explicit "ปลอด" tag from backend always wins
        if (hasAnyTag(wp, setOf("ปลอด"))) return true

        // A day is "Plain Safe" if:
        // 1. It is NOT Kalagni for the person
        // 2. It has NO hard negative tags (World Inauspicious, etc.)
        // 3. it has NO major positive auspicious tags (Tongchai, Fu, etc.)
        return !wp.isKalagni &&
            !hasHardNegativeTag(wp) &&
            !hasPositiveAuspiciousTagRaw(wp)
    }

    private fun hasPositiveAuspiciousTag(wp: Wanpra): Boolean {
        return hasPositiveAuspiciousTagRaw(wp) || isPlainSafeDay(wp)
    }

    private fun getFallbackAuspiciousBadgeLabel(wp: Wanpra): String? {
        val preferredLabels = listOf(
            "ไชยดิถี", "ภัทรดีถี", "ปุณณดีถี", "นันทดีถี", "มิตตะดีถี",
            "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค",
            "ดิถีเรียงหมอน", "วันลอย", "วันฟู", "กระทิงวัน", "วันธงชัย", "วันอธิบดี"
        )
        val allTags = buildList {
            addAll(wp.displayTagsPrioritized.orEmpty())
            addAll(wp.displayTags.orEmpty())
            addAll(wp.calendarDisplayTags.orEmpty())
            addAll(wp.kalTags.orEmpty())
        }.filter { it.isNotBlank() }

        return preferredLabels.firstOrNull { label -> allTags.any { it == label } }
    }

    private fun collectAdditionalAuspiciousBadgeLabels(wp: Wanpra, shownLabels: Set<String>): List<String> {
        val dynamicLabels = setOf(
            "ไชยดิถี", "ภัทรดีถี", "ปุณณดีถี", "นันทดีถี", "มิตตะดีถี",
            "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค",
            "ดิถีเรียงหมอน", "วันลอย", "วันฟู", "กระทิงวัน", "วันธงชัย", "วันอธิบดี", "ปลอด"
        )

        return collectDisplayTagsForDay(wp)
            .filter { it.isNotBlank() }
            .filter { it in dynamicLabels }
            .filterNot { shownLabels.contains(it) }
            .distinct()
    }

    private fun toThaiMonthShort(monthIndex: Int, fallback: String): String {
        val shortMonths = listOf(
            "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
            "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค."
        )
        return shortMonths.getOrNull(monthIndex - 1) ?: fallback
    }

    private fun toThaiMonthFull(monthIndex: Int): String {
        val fullMonths = listOf(
            "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
            "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
        )
        return fullMonths.getOrNull(monthIndex - 1) ?: ""
    }

    private fun passesMarriagePreferredWindow(date: org.joda.time.LocalDate, astrologicalDayName: String): Boolean {
        val allowedMonths = setOf(2, 4, 6, 9, 12)
        val (dithi, thaiMonthRaw) = ThaiAstrologyLib.getThaiLunar(date)
        val thaiMonth = when (thaiMonthRaw) {
            13, 18 -> 8
            in 13..99 -> thaiMonthRaw - 12
            else -> thaiMonthRaw
        }
        val lunarDayNumber = if (dithi > 15) dithi - 15 else dithi
        val isAllowedWeekday = astrologicalDayName in setOf("อาทิตย์", "จันทร์", "พฤหัสบดี", "ศุกร์") ||
            astrologicalDayName.startsWith("พุธ")

        return thaiMonth in allowedMonths &&
            isAllowedWeekday &&
            !ThaiAstrologyLib.isWanPra(date) &&
            lunarDayNumber != 7
    }

    private fun getMarriageWeekdayWarning(astrologicalDayName: String): String? {
        return when {
            astrologicalDayName.startsWith("พุธ") ->
                "โบราณกล่าวว่าห้ามแต่งงานวันพุธ แต่อนุโลมได้ ควรพิจารณาตามความเหมาะสม"
            astrologicalDayName == "พฤหัสบดี" ->
                "แต่งได้แต่ให้แก้เคล็ดโดยห้าม...กัน และให้สวมแหวนแต่งงานก่อนวันแต่งจริง"
            else -> null
        }
    }

    private data class MarriagePersonRule(
        val ageNext: Int,
        val transitBadDay: String?,
        val redDays: List<String>
    )

    private fun getMarriageFixedBadMap(): Map<Int, List<String>> = mapOf(
        1 to listOf("ศุกร์", "อังคาร"),
        2 to listOf("อาทิตย์", "พฤหัสบดี"),
        3 to listOf("จันทร์", "อาทิตย์"),
        4 to listOf("อังคาร", "พุธ (กลางคืน)"),
        5 to listOf("เสาร์", "จันทร์"),
        6 to listOf("พุธ (กลางคืน)", "เสาร์"),
        7 to listOf("พุธ (กลางวัน)", "ศุกร์"),
        8 to listOf("พฤหัสบดี", "พุธ (กลางวัน)")
    )

    private fun getMarriageKalagniDayName(dayNum: Int): String? = mapOf(
        1 to "อาทิตย์",
        2 to "จันทร์",
        3 to "อังคาร",
        4 to "พุธ (กลางวัน)",
        5 to "พฤหัสบดี",
        6 to "ศุกร์",
        7 to "เสาร์",
        8 to "พุธ (กลางคืน)"
    )[dayNum]

    private fun dynamicAgeYearsAt(referenceDate: org.joda.time.LocalDate): Int? {
        val birthDate = memberBirthDate ?: return null
        return calculateThaiAgeNextAtDate(birthDate, referenceDate)
    }

    private fun dynamicKalagniAgeDayForDate(referenceDate: org.joda.time.LocalDate): String? {
        val birthDate = memberBirthDate ?: return null
        val birthDayNum = memberBirthDayNumber ?: return null
        val taksa = ThaiAstrologyLib.calculateTaksa(birthDate, birthDayNum, referenceDate)
        val kalagniAgeNum = ThaiAstrologyLib.BOARD[taksa.badPos.r][taksa.badPos.c]
        return getMarriageKalagniDayName(kalagniAgeNum)
    }

    private fun refreshDynamicKalagniHeaderForViewDate() {
        val ageYears = dynamicAgeYearsAt(currentViewDate.toLocalDate()) ?: return
        val ageDay = dynamicKalagniAgeDayForDate(currentViewDate.toLocalDate()) ?: return
        headerData = rebuildHeaderTags(
            headerData.copy(
                userAge = ageYears,
                birthSourceDay = memberBirthDayThai ?: headerData.birthSourceDay,
                kalagniAge = ageDay,
                kalagniAgeDays = normalizeKalagniDays(null, ageDay),
                kalagniAgeDualText = ageDay
            )
        )
        updateRecyclerHeader()
    }

    private fun calculateThaiAgeNextAtDate(birthDate: org.joda.time.LocalDate, referenceDate: org.joda.time.LocalDate): Int {
        var ageFull = referenceDate.year - birthDate.year
        if (referenceDate.monthOfYear < birthDate.monthOfYear ||
            (referenceDate.monthOfYear == birthDate.monthOfYear && referenceDate.dayOfMonth < birthDate.dayOfMonth)
        ) {
            ageFull--
        }
        return ageFull + 1
    }

    private fun buildMarriagePersonRule(
        birthDate: org.joda.time.LocalDate,
        birthDayNum: Int,
        referenceDate: org.joda.time.LocalDate
    ): MarriagePersonRule {
        val taksa = ThaiAstrologyLib.calculateTaksa(birthDate, birthDayNum, referenceDate)
        val transitBadDayNum = ThaiAstrologyLib.BOARD[taksa.badPos.r][taksa.badPos.c]
        val transitBadDay = getMarriageKalagniDayName(transitBadDayNum)
        val fixedBadDays = getMarriageFixedBadMap()[birthDayNum].orEmpty()
        val redDays = (fixedBadDays + listOfNotNull(transitBadDay)).distinct()
        return MarriagePersonRule(
            ageNext = taksa.ageNext,
            transitBadDay = transitBadDay,
            redDays = redDays
        )
    }

    private fun isMarriagePersonalInauspiciousDay(
        astrologicalDayName: String,
        groomRule: MarriagePersonRule,
        brideRule: MarriagePersonRule
    ): Boolean {
        return groomRule.redDays.any { isSameKalagniDay(astrologicalDayName, it) } ||
            brideRule.redDays.any { isSameKalagniDay(astrologicalDayName, it) }
    }

    private fun hasHardNegativeTag(wp: Wanpra): Boolean {
        val hardNegativeTags = setOf(
            "วันอุบาทว์/อุบาสน", "วันอุบาทว์", "อุบาทว์", "วันโลกาวินาศ", "วันจม",
            "พิฆาต", "ดิถีพิฆาต", "กาลกิณี", "กาลสูร", "กาลโชค",
            "กาลทิน", "กาลทัณฑ์", "โลกาวินาศ", "วินาศ", "มฤตยู",
            "บอด", "ทินสูรย์", "ทินศูร", "ทินกาล", "ทัคธทิน",
            "ยมขันธ์", "ทักทิน", "พิลา", "ทรทึก",
            "ทึกทึน", "อัตนิโรจน์", "ทินสูญ", "ทักทินไฟ", "กาฬโชค", "กาลสูญ", "โลกวินาส", "วินาสส์", "วันบอด", "กาลทีน"
        )
        return hasAnyTag(wp, hardNegativeTags) ||
            hasTagPrefix(wp, "มหาสูญ") ||
            hasTagPrefix(wp, "อายกรรมพลาย")
    }

    private fun isGenerallyEligibleForCategory(wp: Wanpra): Boolean {
        return !wp.isKalagni && !hasHardNegativeTag(wp)
    }

    private fun isEligibleForCategory(wp: Wanpra, category: String): Boolean {
        if (!isGenerallyEligibleForCategory(wp) || !hasPositiveAuspiciousTag(wp)) {
            return false
        }

        val plainSafeDay = isPlainSafeDay(wp)
        return when (category) {
            "marriage", "engagement" -> {
                val dt = try { formatter.parseDateTime(wp.wanpraDate).toLocalDate() } catch (_: Exception) { null }
                val (dithi, _) = dt?.let { ThaiAstrologyLib.getThaiLunar(it) } ?: (0 to 0)
                val lunarDayNumber = if (dithi > 15) dithi - 15 else dithi
                val hasKatingDay = isFlagEnabled(wp.isKating) || hasAnyTag(wp, setOf("กระทิงวัน"))
                if (lunarDayNumber == 7 || (dt != null && ThaiAstrologyLib.isWanPra(dt)) || hasKatingDay) {
                    return false
                }
                // Remove all 'assumed' restrictions and follow the user's logic: 
                // Any auspicious day that is NOT a personal bad day is eligible.
                val isAuspicious = hasPositiveAuspiciousTag(wp) || plainSafeDay || wp.isRiangMon
                isAuspicious && !hasWarningKeyword(wp, "สตรี") && !hasWarningKeyword(wp, "บุรุษ")
            }
            "surgery" ->
                (plainSafeDay || hasAnyTag(wp, setOf("อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))) &&
                    !hasWarningKeyword(wp, "สตรี") &&
                    !hasWarningKeyword(wp, "บุรุษ")
            "house" -> {
                val dt = try { formatter.parseDateTime(wp.wanpraDate).toLocalDate() } catch (_: Exception) { null }
                val isTue = dt?.let { it.dayOfWeek == 2 } ?: false
                !isTue && (plainSafeDay || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))) &&
                    !hasWarningKeyword(wp, "บ้าน") &&
                    !hasWarningKeyword(wp, "ที่ดิน") &&
                    !hasWarningKeyword(wp, "ดิน") &&
                    !hasWarningKeyword(wp, "วัง") &&
                    !hasWarningKeyword(wp, "ภูเขา")
            }
            "build_house" -> {
                val dt = try { formatter.parseDateTime(wp.wanpraDate).toLocalDate() } catch (_: Exception) { null }
                val (dithi, mThai) = dt?.let { ThaiAstrologyLib.getThaiLunar(it) } ?: (0 to 0)

                val okMonth = setOf(1, 2, 4, 6, 9, 12).contains(mThai)
                val okDay = setOf(1, 4, 5, 3).contains(dt?.dayOfWeek) // Mon, Thu, Fri, Wed
                val dithiBase = if (dithi > 15) dithi - 15 else dithi
                val okDithi = setOf(1, 2, 6, 10, 13, 15).contains(dithiBase)

                okMonth && okDay && okDithi && !hasHardNegativeTag(wp) && !wp.isKalagni &&
                    (plainSafeDay || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))) &&
                    !hasWarningKeyword(wp, "บ้าน") &&
                    !hasWarningKeyword(wp, "ที่ดิน") &&
                    !hasWarningKeyword(wp, "ดิน") &&
                    !hasWarningKeyword(wp, "วัง") &&
                    !hasWarningKeyword(wp, "ภูเขา")
            }
            "move_house" ->
                (plainSafeDay || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))) &&
                    !hasWarningKeyword(wp, "บ้าน") &&
                    !hasWarningKeyword(wp, "ที่ดิน") &&
                    !hasWarningKeyword(wp, "ดิน") &&
                    !hasWarningKeyword(wp, "วัง") &&
                    !hasWarningKeyword(wp, "ภูเขา")
            "ordination" -> {
                val dt = try { formatter.parseDateTime(wp.wanpraDate).toLocalDate() } catch (_: Exception) { null }
                val (dithi, _) = dt?.let { ThaiAstrologyLib.getThaiLunar(it) } ?: (0 to 0)
                val is14Kham = dithi == 14 || dithi == 29
                !is14Kham && (plainSafeDay || isFlagEnabled(wp.isWanpra) || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค"))) &&
                    !hasWarningKeyword(wp, "พัทธสีมา") &&
                    !hasWarningKeyword(wp, "บวช")
            }
            "deordination" -> {
                val dt = try { formatter.parseDateTime(wp.wanpraDate).toLocalDate() } catch (_: Exception) { null }
                val (dithi, _) = dt?.let { ThaiAstrologyLib.getThaiLunar(it) } ?: (0 to 0)
                val is14Kham = dithi == 14 || dithi == 29
                !is14Kham && (plainSafeDay || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))) &&
                    !hasWarningKeyword(wp, "พัทธสีมา") &&
                    !hasWarningKeyword(wp, "บวช")
            }
            "car", "buy_car" ->
                (plainSafeDay || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))) &&
                    !hasWarningKeyword(wp, "รถ")
            "childbirth" ->
                (plainSafeDay || hasAnyTag(wp, setOf("อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))) &&
                    !hasWarningKeyword(wp, "สตรี")
            "shop", "business" ->
                plainSafeDay || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "วันลอย", "วันฟู", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))
            "merit" ->
                (plainSafeDay || isFlagEnabled(wp.isWanpra) || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค"))) &&
                    !hasWarningKeyword(wp, "เทพ")
            "debt" ->
                plainSafeDay || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))
            "plant" ->
                (plainSafeDay || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "วันลอย", "วันฟู", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))) &&
                    !hasWarningKeyword(wp, "ชื่อ") &&
                    !hasWarningKeyword(wp, "วาจา") &&
                    !hasWarningKeyword(wp, "กาลกิณี")
            "spirit_house" ->
                (plainSafeDay || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "วันลอย", "วันฟู", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))) &&
                    !hasWarningKeyword(wp, "วาจา") &&
                    !hasWarningKeyword(wp, "การสื่อสาร") &&
                    !hasWarningKeyword(wp, "กาลกิณี")
            "travel" ->
                (plainSafeDay || hasAnyTag(wp, setOf("วันลอย", "วันฟู", "วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค"))) &&
                    !hasWarningKeyword(wp, "น้ำ") &&
                    !hasWarningKeyword(wp, "ป่า") &&
                    !hasWarningKeyword(wp, "ภูเขา") &&
                    !hasWarningKeyword(wp, "เรือ")
            "promotion", "job" ->
                plainSafeDay || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))
            else -> false
        }
    }

    private fun datesForCategory(wanpras: List<Wanpra>, category: String, fallback: List<String>): List<String> {
        return wanpras.filter { isEligibleForCategory(it, category) }
            .mapNotNull { it.wanpraDate }
            .distinct()
            .sorted()
            .ifEmpty { fallback }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setLoadingVisible(true)
        val user = UserContextManager.userX(requireContext())
        isAdminMode = UserContextManager.isAdmin(user)
        isLoggedInUser = !user?.userId.isNullOrEmpty()
        if (!isLoggedInUser) {
            // Guest requirement: keep calendar fixed at March 2569 (March 2026 CE).
            currentViewDate = guestLockedMonthYear()
        }
        hasRengyamAccess = computeRengyamAccess()
        updateCalendarScrollLock()
        this.initHeaderData()
        this.setupAdapterList()
        this.setupFrozenHeader()
        this.fetchDataForHeader()
        this.fetchMemberAndShowKalagni()
        if (shouldShowFullCalendar()) {
            this.initRecyclerView()
            this.fetchAuspiciousForSelectedMonth()
        } else {
            renderLockedCalendarPlaceholder()
        }
        loadMarriagePersistence()
    }

    private fun setLoadingVisible(visible: Boolean) {
        if (!this::rengYamBinding.isInitialized) return
        rengYamBinding.loadingOverlay.visibility = if (visible) View.VISIBLE else View.GONE
        rengYamBinding.recyclerviewWanpra.visibility = if (visible) View.INVISIBLE else View.VISIBLE
    }

    private fun beginLoading() {
        loadingRequests += 1
        setLoadingVisible(true)
    }

    private fun endLoading() {
        loadingRequests = (loadingRequests - 1).coerceAtLeast(0)
        if (loadingRequests == 0) setLoadingVisible(false)
    }

    private fun initHeaderData() {
        val dt = currentViewDate
        val dayLabelEng = dt.dayOfWeek().getAsText(java.util.Locale.ENGLISH)
        val thaiDay = PersonContextManager.toThaiDay(dayLabelEng)
        val day = if (thaiDay.isEmpty()) dt.dayOfWeek().asText else thaiDay
        val month = toThaiMonthFull(dt.monthOfYear)
        
        headerData = headerData.copy(
            dayStr = "วันนี้$day",
            dayNum = "ที่ ${dt.dayOfMonth().asText}",
            monthStr = month,
            yearStr = (dt.year().asText.toInt() + 543).toString()
        )
    }
    
    private fun setupAdapterList() {
        if (rengYamBinding.recyclerviewWanpra.adapter == null) {
            val adater = WanpraAdapter(
                fullDataList, 
                "", 
                isAdminMode = isAdminMode,
                isLoggedInUser = isLoggedInUser,
                hasRengyamAccess = hasRengyamAccess,
                onCategorySelected = { category ->
                    val latestAccess = computeRengyamAccess()
                    hasRengyamAccess = latestAccess
                    updateCalendarScrollLock()
                    if (!latestAccess) {
                        if (!category.isNullOrBlank()) {
                            showRengyamUnlockDialog(category)
                        }
                        return@WanpraAdapter
                    }
                    val prev = headerData.selectedCategory
                    val selectedCategory = if (category == prev) null else category
                    headerData = headerData.copy(selectedCategory = selectedCategory)
                    cachedLengYamDao?.let { updateRecyclerWithData(it) }
                    if (!selectedCategory.isNullOrBlank()) {
                        if (selectedCategory == "marriage") {
                            showMarriageFormBottomSheet()
                        } else {
                            showAuspiciousBottomSheet(selectedCategory)
                        }
                    }
                },
                onLockedCategoryClick = { category ->
                    showRengyamUnlockDialog(category)
                },
                onCalendarDayClick = { day ->
                    showDayTagDetailBottomSheet(day, category = headerData.selectedCategory)
                },
                onMonthNav = { direction ->
                    if (!isLoggedInUser) return@WanpraAdapter
                    val targetDate = currentViewDate.plusMonths(direction)
                    currentViewDate = targetDate
                    headerData = headerData.copy(selectedCategory = null)
                    initHeaderData()
                    refreshDynamicKalagniHeaderForViewDate()
                    cachedLengYamDao?.let { updateRecyclerWithData(it) }
                    fetchAuspiciousForSelectedMonth()
                },
                onMonthSelected = { selectedMonth ->
                    if (!isLoggedInUser) return@WanpraAdapter
                    currentViewDate = currentViewDate.withMonthOfYear(selectedMonth)
                    headerData = headerData.copy(selectedCategory = null)
                    initHeaderData()
                    refreshDynamicKalagniHeaderForViewDate()
                    cachedLengYamDao?.let { updateRecyclerWithData(it) }
                    fetchAuspiciousForSelectedMonth()
                },
                onYearSelected = { selectedYearCe ->
                    if (!isLoggedInUser) return@WanpraAdapter
                    currentViewDate = currentViewDate.withYear(selectedYearCe)
                    headerData = headerData.copy(selectedCategory = null)
                    initHeaderData()
                    refreshDynamicKalagniHeaderForViewDate()
                    cachedLengYamDao?.let { updateRecyclerWithData(it) }
                    fetchAuspiciousForSelectedMonth()
                    if (!shouldShowFullCalendar()) {
                        renderLockedCalendarPlaceholder()
                    }
                },
                _showInauspiciousInfo = showTopHeaderInauspiciousInfo
            )
            rengYamBinding.recyclerviewWanpra.adapter = adater
            val gridLayout = object : androidx.recyclerview.widget.GridLayoutManager(context, 7) {
                override fun canScrollVertically(): Boolean {
                    return !isCalendarScrollLocked && super.canScrollVertically()
                }
            }
            gridLayout.spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val type = adater.getItemViewType(position)
                    return if (type == com.numberniceic.adapters.WanpraAdapter.TYPE_CALENDAR_DAY || 
                               type == com.numberniceic.adapters.WanpraAdapter.TYPE_WEEKDAY_HEADER || 
                               type == com.numberniceic.adapters.WanpraAdapter.TYPE_EMPTY_DAY) 1 else 7
                }
            }
            wanpraLayoutManager = gridLayout
            rengYamBinding.recyclerviewWanpra.layoutManager = gridLayout
        } else {
             val adapter = rengYamBinding.recyclerviewWanpra.adapter as? WanpraAdapter
             adapter?.isAdminMode = isAdminMode
             adapter?.setRengyamAccess(hasRengyamAccess)
             adapter?.notifyDataSetChanged()
             updateCalendarScrollLock()
        }

        rengYamBinding.recyclerviewWanpra.clearOnScrollListeners()
    }

    private data class DayTagDetail(
        val tag: String,
        val displayTag: String,
        val source: String,
        val school: String,
        val description: String
    )

    private enum class TagBadgeLevel {
        GOOD, WARNING, FORBIDDEN
    }

    private fun levelPriority(level: TagBadgeLevel): Int {
        return when (level) {
            TagBadgeLevel.FORBIDDEN -> 0
            TagBadgeLevel.WARNING -> 1
            TagBadgeLevel.GOOD -> 2
        }
    }

    private fun tagPriority(tag: String): Int {
        val canonical = RengYamTagCanonicalizer.canonical(tag)
        return when {
            canonical.startsWith("อัปมงคล") -> 0
            canonical.startsWith("อัคนิโรธ") -> 1
            canonical == "วันอุบาทว์/อุบาสน" || canonical == "วันโลกาวินาศ" || canonical == "วันจม" -> 2
            canonical.startsWith("มหาสูญ") || canonical.startsWith("อายกรรมพลาย") -> 3
            canonical == "พิฆาต" || canonical == "ดิถีพิฆาต" || canonical == "ทรทึก" || canonical == "มฤตยู" || canonical == "กาลกิณี" || canonical == "บอด" || canonical == "วินาศ" || canonical == "โลกาวินาศ" ||
                canonical == "ทึกทึน" || canonical == "อัตนิโรจน์" || canonical == "ทินสูญ" || canonical == "กาฬโชค" || canonical == "กาลสูญ" || canonical == "โลกวินาส" || canonical == "วินาสส์" ||
                canonical == "กาลทีน" -> 4
            canonical == "วันธงชัย" || canonical == "วันอธิบดี" -> 5
            canonical == "ดิถีเรียงหมอน" || canonical == "อำฤตโชค" || canonical == "มหาสิทธิโชค" || canonical == "สิทธิโชค" || canonical == "ราชาโชค" || canonical == "ชัยโชค" ||
                canonical == "ไชยดิถี" || canonical == "ภัทรดีถี" || canonical == "ปุณณดีถี" || canonical == "นันทดีถี" || canonical == "มิตตะดีถี" -> 6
            canonical == "วันลอย" || canonical == "วันฟู" || canonical == "กระทิงวัน" -> 7
            else -> 8
        }
    }

    private fun sortTagDetails(details: List<DayTagDetail>): List<DayTagDetail> {
        return details.withIndex()
            .sortedWith(
                compareBy<IndexedValue<DayTagDetail>>(
                    { detailGroupPriority(it.value) },
                    { tagPriority(it.value.tag) },
                    { it.index }
                )
            )
            .map { it.value }
    }

    private fun detailGroupPriority(detail: DayTagDetail): Int {
        val tag = RengYamTagCanonicalizer.canonical(detail.tag.trim())
        val goodTags = setOf(
            "วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค",
            "ดิถีเรียงหมอน", "วันลอย", "วันฟู", "ไชยดิถี", "ภัทรดีถี", "ปุณณดีถี", "นันทดีถี", "มิตตะดีถี"
        )

        return when {
            tag in goodTags -> 0
            tag.startsWith("ทิศดี") -> 1
            tag.startsWith("ทิศไม่ดี") -> 2
            tag.startsWith("ห้าม") || tag.startsWith("อัปมงคล") || tag.startsWith("อัคนิโรธ") -> 3
            else -> 4
        }
    }

    private fun classifyTagBadgeLevel(detail: DayTagDetail): TagBadgeLevel {
        val tag = RengYamTagCanonicalizer.canonical(detail.tag.trim())
        val source = detail.source.trim()

        if (source == "ข้อห้าม" || tag.startsWith("อัคนิโรธ")) {
            return TagBadgeLevel.FORBIDDEN
        }

        val goodTags = setOf(
            "วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค",
            "ดิถีเรียงหมอน", "วันลอย", "วันฟู", "ไชยดิถี", "ภัทรดีถี", "ปุณณดีถี", "นันทดีถี", "มิตตะดีถี"
        )
        if (tag in goodTags) {
            return TagBadgeLevel.GOOD
        }

        return TagBadgeLevel.WARNING
    }

    private fun collectDisplayTagsForDay(wp: Wanpra): List<String> {
        val groupedTags = buildList {
            addAll(wp.kalTags.orEmpty())
            addAll(wp.dithiTags.orEmpty())
            addAll(wp.dayTypeTags.orEmpty())
            addAll(wp.warningTags.orEmpty())
        }.filter { it.isNotBlank() }
        if (groupedTags.isNotEmpty()) {
            val expanded = expandAmmaritVariants(groupedTags)
            return expanded.distinct()
        }

        val prioritized = wp.displayTagsPrioritized.orEmpty().filter { it.isNotBlank() }
        if (prioritized.isNotEmpty()) return expandAmmaritVariants(prioritized).distinct()

        val display = wp.displayTags.orEmpty().filter { it.isNotBlank() }
        if (display.isNotEmpty()) return expandAmmaritVariants(display).distinct()

        val fallback = mutableListOf<String>()
        if (isFlagEnabled(wp.isTongchai)) fallback.add("วันธงชัย")
        if (isFlagEnabled(wp.isAtipbadee)) fallback.add("วันอธิบดี")
        if (wp.isRiangMon) fallback.add("ดิถีเรียงหมอน")
        if (wp.isAmmarit) fallback.add("อำฤตโชค")
        if (wp.isMahaSittichok) fallback.add("มหาสิทธิโชค")
        if (wp.isSittichok) fallback.add("สิทธิโชค")
        if (wp.isRachaChok) fallback.add("ราชาโชค")
        if (wp.isChaiChok) fallback.add("ชัยโชค")
        if (wp.isLoy) fallback.add("วันลอย")
        if (wp.isFu) fallback.add("วันฟู")
        if (wp.isJom) fallback.add("วันจม")
        if (isFlagEnabled(wp.isKating)) fallback.add("กระทิงวัน")
        if (wp.isUbath) fallback.add("วันอุบาทว์/อุบาสน")
        if (wp.isLokawinat) fallback.add("วันโลกาวินาศ")
        if (fallback.isEmpty() && isPlainSafeDay(wp)) fallback.add("ปลอด")
        return expandAmmaritVariants(fallback).distinct()
    }

    private fun expandAmmaritVariants(tags: List<String>): List<String> {
        if (tags.isEmpty()) return tags
        val set = tags.toMutableList()
        val hasAmmarit = set.any { it == "อำฤตโชค" }
        val hasAmmut = set.any { it == "อมุตโชค" }
        if (hasAmmarit && !hasAmmut) set.add("อมุตโชค")
        if (hasAmmut && !hasAmmarit) set.add("อำฤตโชค")
        return set
    }

    private fun collectDayTagDetails(wp: Wanpra): List<DayTagDetail> {
        val backendDetails = wp.tagDetails.orEmpty()
            .mapNotNull {
                val tag = it.tag?.trim().orEmpty()
                if (tag.isEmpty()) return@mapNotNull null
                DayTagDetail(
                    tag = tag,
                    displayTag = it.displayTag?.trim().takeUnless { s -> s.isNullOrEmpty() } ?: tag,
                    source = it.source?.trim().takeUnless { s -> s.isNullOrEmpty() } ?: "อื่นๆ",
                    school = it.school?.trim().takeUnless { s -> s.isNullOrEmpty() } ?: "",
                    description = it.description?.trim().takeUnless { s -> s.isNullOrEmpty() }
                        ?: RengYamTagMeaning.explain(tag)
                )
            }
            .toMutableList()

        val seenTags = mutableSetOf<String>()
        fun addUnique(detail: DayTagDetail) {
            val key = detail.tag.trim()
            if (key.isEmpty() || seenTags.contains(key)) return
            backendDetails.add(detail)
            seenTags.add(key)
        }

        backendDetails.toList().forEach { seenTags.add(it.tag.trim()) }
        if (seenTags.contains("อำฤตโชค")) {
            addUnique(
                DayTagDetail(
                    tag = "อมุตโชค",
                    displayTag = "อมุตโชค",
                    source = "ดิถี/ฤกษ์",
                    school = "",
                    description = RengYamTagMeaning.explain("อมุตโชค")
                )
            )
        } else if (seenTags.contains("อมุตโชค")) {
            addUnique(
                DayTagDetail(
                    tag = "อำฤตโชค",
                    displayTag = "อำฤตโชค",
                    source = "ดิถี/ฤกษ์",
                    school = "",
                    description = RengYamTagMeaning.explain("อำฤตโชค")
                )
            )
        }

        if (wp.kalagniBirth && wp.kalagniAge) {
            backendDetails.removeAll {
                val t = it.tag.trim()
                t == "อัปมงคลวันเกิด" || t == "อัปมงคลอายุย่าง" || t == "อัปมงคลอายุย่าง และวันเกิด"
            }
            seenTags.removeAll(setOf("อัปมงคลวันเกิด", "อัปมงคลอายุย่าง", "อัปมงคลอายุย่าง และวันเกิด"))
            addUnique(
                DayTagDetail(
                    tag = "อัปมงคลอายุย่าง และวันเกิด",
                    displayTag = "อัปมงคลอายุย่าง และวันเกิด",
                    source = "ข้อห้าม",
                    school = "",
                    description = RengYamTagMeaning.explain("อัปมงคลอายุย่าง และวันเกิด")
                )
            )
        } else {
            if (wp.kalagniBirth) {
                backendDetails.removeAll { it.tag.trim() == "อัปมงคลวันเกิด" }
                seenTags.remove("อัปมงคลวันเกิด")
                addUnique(
                    DayTagDetail(
                        tag = "อัปมงคลวันเกิด",
                        displayTag = "อัปมงคลวันเกิด",
                        source = "ข้อห้าม",
                        school = "",
                        description = RengYamTagMeaning.explain("อัปมงคลวันเกิด")
                    )
                )
            }
            if (wp.kalagniAge) {
                backendDetails.removeAll { it.tag.trim() == "อัปมงคลอายุย่าง" }
                seenTags.remove("อัปมงคลอายุย่าง")
                addUnique(
                    DayTagDetail(
                        tag = "อัปมงคลอายุย่าง",
                        displayTag = "อัปมงคลอายุย่าง",
                        source = "ข้อห้าม",
                        school = "",
                        description = RengYamTagMeaning.explain("อัปมงคลอายุย่าง")
                    )
                )
            }
        }
        val dt = try { formatter.parseDateTime(wp.wanpraDate).toLocalDate() } catch (_: Exception) { null }
        val dithiProhibition = dt?.let { ThaiAstrologyLib.queryDithiCommonProhibitions(ThaiAstrologyLib.getThaiLunar(it).first) }
        if (!dithiProhibition.isNullOrBlank()) {
            addUnique(
                DayTagDetail(
                    tag = dithiProhibition,
                    displayTag = dithiProhibition,
                    source = "ข้อห้าม",
                    school = "",
                    description = RengYamTagMeaning.explain(dithiProhibition)
                )
            )
        }
        val inauspicious = dt?.let { ThaiAstrologyLib.queryInauspicious(it.dayOfWeek, ThaiAstrologyLib.getThaiLunar(it).first) }.orEmpty()
        inauspicious.forEach { tag ->
            addUnique(
                DayTagDetail(
                    tag = tag,
                    displayTag = tag,
                    source = "ข้อห้าม",
                    school = "",
                    description = RengYamTagMeaning.explain(tag)
                )
            )
        }
        val myHoraPrimaryBad = dt?.let { ThaiAstrologyLib.queryMyHoraPrimaryBadTags(it.dayOfWeek, ThaiAstrologyLib.getThaiLunar(it).first) }.orEmpty()
        myHoraPrimaryBad.forEach { tag ->
            addUnique(
                DayTagDetail(
                    tag = tag,
                    displayTag = tag,
                    source = "ข้อห้าม",
                    school = "",
                    description = RengYamTagMeaning.explain(tag)
                )
            )
        }
        if (backendDetails.isNotEmpty()) return sortTagDetails(backendDetails)

        val fallback = collectDisplayTagsForDay(wp).map { tag ->
            DayTagDetail(
                tag = tag,
                displayTag = tag,
                source = when {
                    wp.kalTags.orEmpty().contains(tag) -> "กาลโยค"
                    wp.dithiTags.orEmpty().contains(tag) -> "ดิถี/ฤกษ์"
                    wp.dayTypeTags.orEmpty().contains(tag) -> "ชนิดวัน"
                    wp.warningTags.orEmpty().contains(tag) -> "ข้อห้าม"
                    else -> "อื่นๆ"
                },
                school = "",
                description = RengYamTagMeaning.explain(tag)
            )
        }
        return sortTagDetails(fallback)
    }

    private fun showDayTagDetailBottomSheet(
        wp: Wanpra,
        onBackToList: (() -> Unit)? = null,
        category: String? = null
    ) {
        if (!isAdded) return
        val resolvedWp = wp.wanpraDate?.let { dateToWanpraMap[it] } ?: wp
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_rengyam_day_detail, null)
        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        dialog.setContentView(dialogView)
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.peekHeight = resources.displayMetrics.heightPixels
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = false

        dialogView.findViewById<View>(R.id.btn_back_detail)?.setOnClickListener {
            dialog.dismiss()
            onBackToList?.invoke()
        }

        val (dateText, dayOfWeek) = try {
            val dt = formatter.parseDateTime(resolvedWp.wanpraDate)
            val yearBe = dt.year + 543
            val thaiMonth = toThaiMonthFull(dt.monthOfYear)
            val thaiDayName = PersonContextManager.toThaiDay(dt.dayOfWeek().getAsText(java.util.Locale.ENGLISH))
            val naksat = getThaiZodiac(yearBe)
            
            val lunarStr = resolvedWp.lunarPhase?.let { phase ->
                val month = resolvedWp.lunarMonth?.trim().orEmpty()
                if (phase.isNotBlank() && month.isNotBlank()) {
                    "$phase เดือน $month"
                } else {
                    null
                }
            } ?: run {
                val dtLocal = try { formatter.parseDateTime(resolvedWp.wanpraDate).toLocalDate() } catch (_: Exception) { null }
                if (dtLocal != null) {
                    val (dithiRaw, mThai) = ThaiAstrologyLib.getThaiLunar(dtLocal)
                    val lunarPhase = if (dithiRaw <= 15) {
                        "ขึ้น ${toThaiNum(dithiRaw.toString())} ค่ำ"
                    } else {
                        "แรม ${toThaiNum((dithiRaw - 15).toString())} ค่ำ"
                    }
                    "$lunarPhase เดือน ${toThaiNum(mThai.toString())}"
                } else {
                    ""
                }
            }
            
            val headerText = if (lunarStr.isNullOrBlank()) {
                "${thaiDayName}ที่ ${toThaiNum(dt.dayOfMonth.toString())} $thaiMonth ${toThaiNum(yearBe.toString())}\n$naksat"
            } else {
                "${thaiDayName}ที่ ${toThaiNum(dt.dayOfMonth.toString())} $thaiMonth ${toThaiNum(yearBe.toString())}\n$naksat $lunarStr"
            }
            
            headerText to dt.dayOfWeek
        } catch (e: Exception) {
            (resolvedWp.wanpraDate ?: "-") to null
        }

        dialogView.findViewById<TextView>(R.id.txt_day_detail_subtitle)?.text = dateText
        val container = dialogView.findViewById<LinearLayout>(R.id.layout_tag_explanations)
        val tagDetails = collectDayTagDetails(resolvedWp).toMutableList()
        if (category == "marriage" || category == "engagement") {
            val dt = try { formatter.parseDateTime(resolvedWp.wanpraDate).toLocalDate() } catch (_: Exception) { null }
            val (dithi, _) = dt?.let { ThaiAstrologyLib.getThaiLunar(it) } ?: (0 to 0)
            val astrologicalDayName = dt?.let { getThaiAstrologicalDayName(it, "12:00 น.") }.orEmpty()
            if (astrologicalDayName == "อังคาร") {
                tagDetails.add(0, DayTagDetail(
                    tag = "ห้ามแต่งงานวันอังคาร",
                    displayTag = "ห้ามแต่งงานวันอังคาร",
                    source = "ข้อห้าม",
                    school = "",
                    description = "ตามประเพณีโบราณเชื่อว่าวันอังคารเป็นวันต้องห้ามในการจัดงานมงคลสมรส"
                ))
            }
            getMarriageWeekdayWarning(astrologicalDayName)?.let { warning ->
                val displayTag = if (astrologicalDayName == "พฤหัสบดี") {
                    "คำเตือนวันพฤหัสบดี"
                } else {
                    "คำเตือนวันพุธ"
                }
                tagDetails.add(0, DayTagDetail(
                    tag = displayTag,
                    displayTag = displayTag,
                    source = "ข้อควรทราบ",
                    school = "",
                    description = warning
                ))
            }
            if (dithi == 7 || dithi == 22) {
                tagDetails.add(0, DayTagDetail(
                    tag = "ห้ามแต่งงาน 7 ค่ำ",
                    displayTag = "ห้ามแต่งงาน 7 ค่ำ",
                    source = "ข้อห้าม",
                    school = "",
                    description = "ตามประเพณีโบราณเชื่อว่าวัน 7 ค่ำ (ทั้งข้างขึ้นและข้างแรม) เป็นวันห้ามทำการวิวาห์หรือการแต่งงาน"
                ))
            }
        }
        if (category == "ordination" || category == "deordination") {
            val dt = try { formatter.parseDateTime(wp.wanpraDate).toLocalDate() } catch (_: Exception) { null }
            val (dithi, _) = dt?.let { ThaiAstrologyLib.getThaiLunar(it) } ?: (0 to 0)
            if (dithi == 14 || dithi == 29) {
                tagDetails.add(0, DayTagDetail(
                    tag = "ห้ามบวช-ห้ามสึก 14 ค่ำ",
                    displayTag = "ห้ามบวช-ห้ามสึก 14 ค่ำ",
                    source = "ข้อห้าม",
                    school = "",
                    description = "ตามประเพณีโบราณเชื่อกันว่า วัน 14 ค่ำ เป็นวันต้องห้ามในการทำการบวชหรือทำการสึก"
                ))
            }
        }
        if (category == "house") {
            if (dayOfWeek == 2) {
                tagDetails.add(0, DayTagDetail(
                    tag = "ห้ามขึ้นบ้านใหม่วันอังคาร",
                    displayTag = "ห้ามขึ้นบ้านใหม่วันอังคาร",
                    source = "ข้อห้าม",
                    school = "",
                    description = "ตามประเพณีโบราณเชื่อว่าวันอังคารเป็นวันร้อน ไม่เหมาะแก่การจัดงานมงคลขึ้นบ้านใหม่"
                ))
            }
        }
        container?.removeAllViews()

        if (tagDetails.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "วันนี้ไม่มีรายการฤกษ์ยามที่ระบบระบุเพิ่มเติม"
                setTextColor(android.graphics.Color.parseColor("#616161"))
                textSize = 14f
            }
            container?.addView(emptyText)
        }

        tagDetails.forEachIndexed { index, detail ->
            val item = layoutInflater.inflate(R.layout.item_rengyam_tag_explanation, container, false).apply {
                layoutParams = (layoutParams as LinearLayout.LayoutParams).apply {
                    marginStart = (resources.displayMetrics.density * 20).toInt()
                    marginEnd = (resources.displayMetrics.density * 20).toInt()
                }
            }
            val titleView = item.findViewById<TextView>(R.id.txt_tag_name)
            val sourceView = item.findViewById<TextView>(R.id.txt_tag_source)
            val schoolView = item.findViewById<TextView>(R.id.txt_tag_school)
            val statusText = when (classifyTagBadgeLevel(detail)) {
                TagBadgeLevel.GOOD -> "ฤกษ์ดี"
                TagBadgeLevel.WARNING -> "ไม่ดี"
                TagBadgeLevel.FORBIDDEN -> "ไม่ดี"
            }
            titleView?.text = detail.tag
            sourceView?.visibility = View.VISIBLE
            sourceView?.text = statusText
            schoolView?.visibility = View.GONE
            when (classifyTagBadgeLevel(detail)) {
                TagBadgeLevel.GOOD -> {
                    sourceView?.setBackgroundResource(R.drawable.bg_blue_pill)
                    sourceView?.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                }
                TagBadgeLevel.WARNING -> {
                    sourceView?.setBackgroundResource(R.drawable.bg_yellow_pill)
                    sourceView?.setTextColor(android.graphics.Color.parseColor("#3E2723"))
                }
                TagBadgeLevel.FORBIDDEN -> {
                    sourceView?.setBackgroundResource(R.drawable.bg_red_pill)
                    sourceView?.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                }
            }
            item.findViewById<TextView>(R.id.txt_tag_desc)?.text = detail.description
            item.findViewById<View>(R.id.view_row_divider)?.visibility =
                if (index == tagDetails.lastIndex) View.GONE else View.VISIBLE
            container?.addView(item)
        }

        val travelDirectionInfo = travelDirectionInfoForDay(dayOfWeek)

        val divider = View(requireContext()).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.density * 1).toInt()
            ).apply {
                topMargin = (resources.displayMetrics.density * 12).toInt()
                bottomMargin = (resources.displayMetrics.density * 12).toInt()
                marginStart = (resources.displayMetrics.density * 20).toInt()
                marginEnd = (resources.displayMetrics.density * 20).toInt()
            }
        }
        container?.addView(divider)

        val goodTimes = dayOfWeek?.let { getGoodTimesForDayOfWeek(it) } ?: emptyList()

        if (goodTimes.isNotEmpty()) {
            val headerBar = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(android.graphics.Color.parseColor("#FFF176"))
                setPadding(
                    (resources.displayMetrics.density * 20).toInt(),
                    (resources.displayMetrics.density * 8).toInt(),
                    (resources.displayMetrics.density * 20).toInt(),
                    (resources.displayMetrics.density * 8).toInt()
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (resources.displayMetrics.density * 8).toInt()
                }

                addView(TextView(requireContext()).apply {
                    text = "ทิศและยามดีประจำวัน"
                    setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                    textSize = 15f
                    typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sarabun_semibold)
                })
            }
            container?.addView(headerBar)

            val tableWrap = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 8, 12, 8)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#FFFFFF"))
                    setStroke(2, android.graphics.Color.parseColor("#E0E0E0"))
                    cornerRadius = 16f
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (resources.displayMetrics.density * 4).toInt()
                }
            }

            fun addRow(
                parent: LinearLayout,
                left: String,
                right: String,
                isHeader: Boolean = false,
                addDivider: Boolean = false
            ) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    weightSum = 3f
                    setPadding(6, 4, 6, 4)
                }
                val leftView = TextView(requireContext()).apply {
                    text = left
                    textSize = if (isHeader) 13.5f else 12.5f
                    setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                    typeface = if (isHeader) {
                        androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sarabun_semibold)
                    } else {
                        androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sarabun_regular)
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f)
                }
                val rightView = TextView(requireContext()).apply {
                    text = right
                    textSize = if (isHeader) 13.5f else 12.5f
                    setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                    typeface = if (isHeader) {
                        androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sarabun_semibold)
                    } else {
                        androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sarabun_regular)
                    }
                    gravity = android.view.Gravity.END
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f)
                }
                row.addView(leftView)
                row.addView(rightView)
                parent.addView(row)
                if (addDivider) {
                    val rowDivider = View(requireContext()).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (resources.displayMetrics.density * 1).toInt()
                        )
                    }
                    parent.addView(rowDivider)
                }
            }

            addRow(tableWrap, "ยาม / เวลา", "ทิศ", isHeader = true, addDivider = true)

            val rowsContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 6, 8, 6)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#FFFFFF"))
                    setStroke(1, android.graphics.Color.parseColor("#E0E0E0"))
                    cornerRadius = 12f
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (resources.displayMetrics.density * 6).toInt()
                }
            }
            tableWrap.addView(rowsContainer)

            goodTimes.forEachIndexed { index, text ->
                val clean = text.trim()
                val dir = clean.substringAfter("(", "").substringBefore(")", "")
                val left = if (dir.isNotEmpty()) clean.replace("($dir)", "").trim() else clean
                val right = if (dir.isNotEmpty()) dir else "-"
                addRow(rowsContainer, left, right, addDivider = index != goodTimes.lastIndex)
            }

            container?.addView(tableWrap)
        }

        if (travelDirectionInfo != null) {
            val divider = View(requireContext()).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (resources.displayMetrics.density * 1).toInt()
                ).apply {
                    topMargin = (resources.displayMetrics.density * 14).toInt()
                    bottomMargin = (resources.displayMetrics.density * 12).toInt()
                    marginStart = (resources.displayMetrics.density * 20).toInt()
                    marginEnd = (resources.displayMetrics.density * 20).toInt()
                }
            }
            container?.addView(divider)
            container?.addView(createTravelDirectionSection(travelDirectionInfo))
        }
        dialog.show()
    }

    private fun getGoodTimesForDayOfWeek(dayOfWeek: Int): List<String> {
        return when (dayOfWeek) {
            7 -> listOf("ยามศุกร์ 07.30-09.00 (เหนือ)", "ยามพุธ 09.00-10.30 (ทุกทิศ)", "ยามจันทร์ 10.30-12.00 (ตะวันตก)")
            1 -> listOf("ยามเสาร์ 07.30-09.00 (เหนือ)", "ยามพฤหัส 13.30-15.00 (ทุกทิศ)", "ยามศุกร์ 15.00-16.30 (ทุกทิศ)", "ยามพุธ 16.30-18.00 (ทุกทิศ)")
            2 -> listOf("ยามศุกร์ 09.00-10.30 (เหนือ)", "ยามพุธ 10.30-12.00 (ตะวันตก)", "ยามจันทร์ 12.00-13.30 (ตะวันออก)", "ยามเสาร์ 13.30-15.00 (เหนือ)", "ยามพฤหัส 15.00-16.30 (ทุกทิศ)")
            3 -> listOf("ยามพุธ 06.00-07.30 (ทุกทิศ)", "ยามจันทร์ 07.30-09.00 (ทุกทิศ)", "ยามพฤหัส 10.30-12.00 (ทิศเหนือ)", "ยามศุกร์ 15.00-16.30 (ทิศตะวันออก)", "ยามพุธ 16.30-18.00 (ทุกทิศ)")
            4 -> listOf("ยามพฤหัส 06.00-07.30 (ทุกทิศ)", "ยามอาทิตย์ 09.00-10.30 (ทุกทิศ)", "ยามศุกร์ 10.30-12.00 (ทุกทิศ)", "ยามพุธ 12.00-13.30 (ทุกทิศ)", "ยามจันทร์ 13.30-15.00 (ทุกทิศ)", "ยามพฤหัส 16.30-18.00 (ทิศอุดร)")
            5 -> listOf("ยามศุกร์ 06.00-07.30 (ทุกทิศ)", "ยามพุธ 07.30-09.00 (ทุกทิศ)", "ยามจันทร์ 09.00-10.30 (ทิศตะวันออก)", "ยามเสาร์ 10.30-12.00 (ทุกทิศ)", "ยามพฤหัส 12.00-13.30 (ทุกทิศ)", "ยามอังคาร 13.30-15.00 (ทิศเหนือ)", "ยามศุกร์ 16.30-18.00 (ทุกทิศ)")
            6 -> listOf("ยามพฤหัส 07.30-09.00 (ทุกทิศ)", "ยามศุกร์ 12.00-13.30 (ทุกทิศ)", "ยามพุธ 13.30-15.00 (ทุกทิศ)")
            else -> emptyList()
        }
    }

    private fun travelDirectionInfoForDay(dayOfWeek: Int?): TravelDirectionInfo? {
        return when (dayOfWeek) {
            7 -> TravelDirectionInfo( // Sunday
                deityDirection = "ตะวันออกเฉียงใต้",
                spearDirection = "ตะวันตก",
                ghostDirection = "ตะวันตกเฉียงเหนือ"
            )
            1 -> TravelDirectionInfo( // Monday
                deityDirection = "ตะวันตกเฉียงใต้",
                spearDirection = "เหนือ",
                ghostDirection = "ตะวันออกเฉียงเหนือ"
            )
            2 -> TravelDirectionInfo( // Tuesday
                deityDirection = "ใต้",
                spearDirection = "เหนือ",
                ghostDirection = "เหนือ"
            )
            3 -> TravelDirectionInfo( // Wednesday
                deityDirection = "เหนือ",
                spearDirection = "ใต้",
                ghostDirection = "ใต้"
            )
            4 -> TravelDirectionInfo( // Thursday
                deityDirection = "ตะวันออก",
                spearDirection = "ตะวันตก",
                ghostDirection = "ตะวันตก"
            )
            5 -> TravelDirectionInfo( // Friday
                deityDirection = "ตะวันตกเฉียงเหนือ",
                spearDirection = "ตะวันออก",
                ghostDirection = "ตะวันออกเฉียงใต้"
            )
            6 -> TravelDirectionInfo( // Saturday
                deityDirection = "ตะวันตก",
                spearDirection = "ตะวันออก",
                ghostDirection = "ตะวันออก"
            )
            else -> null
        }
    }

    private fun travelDirectionAbbrev(direction: String): String? {
        return when (direction.trim()) {
            "เหนือ" -> "N"
            "ตะวันออกเฉียงเหนือ" -> "NE"
            "ตะวันออก" -> "E"
            "ตะวันออกเฉียงใต้" -> "SE"
            "ใต้" -> "S"
            "ตะวันตกเฉียงใต้" -> "SW"
            "ตะวันตก" -> "W"
            "ตะวันตกเฉียงเหนือ" -> "NW"
            else -> null
        }
    }

    private fun addTravelDirectionTagForDay(dateKey: String) {
        val wp = dateToWanpraMap[dateKey] ?: return
        val dt = try { formatter.parseDateTime(dateKey).toLocalDate() } catch (_: Exception) { null }
        val info = dt?.let { travelDirectionInfoForDay(it.dayOfWeek) } ?: return
        val goodAbbrev = travelDirectionAbbrev(info.deityDirection)
        val badAbbrev1 = travelDirectionAbbrev(info.spearDirection)
        val badAbbrev2 = travelDirectionAbbrev(info.ghostDirection)
        val badAbbrevPair = listOfNotNull(badAbbrev1, badAbbrev2).joinToString(",")
        val tags = wp.calendarDisplayTags.orEmpty().toMutableList()
        if (!goodAbbrev.isNullOrBlank()) tags.add("ทิศดี ($goodAbbrev)")
        if (badAbbrevPair.isNotBlank()) tags.add("ทิศไม่ดี ($badAbbrevPair)")
        wp.calendarDisplayTags = tags.distinct()
    }

    private fun createTravelDirectionSection(info: TravelDirectionInfo): View {
        val density = resources.displayMetrics.density
        val fontSemiBold = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.sarabun_semibold)
        val fontRegular = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.sarabun_regular)

        fun dp(value: Int): Int = (value * density).toInt()

        fun makeLabel(text: String, bgColor: String, textColor: String): TextView {
            return TextView(requireContext()).apply {
                this.text = text
                setTextColor(android.graphics.Color.parseColor(textColor))
                textSize = 12f
                typeface = fontSemiBold
                setPadding(dp(10), dp(5), dp(10), dp(5))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor(bgColor))
                    cornerRadius = dp(999).toFloat()
                }
            }
        }

        fun addInfoRow(parent: LinearLayout, title: String, value: String, bgColor: String, valueColor: String, meaning: String? = null) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, dp(2))
            }

            val titleView = TextView(requireContext()).apply {
                text = title
                setTextColor(android.graphics.Color.parseColor("#2E5D32"))
                textSize = 13f
                typeface = fontSemiBold
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val valueView = makeLabel(value, bgColor, valueColor).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            row.addView(titleView)
            row.addView(valueView)
            parent.addView(row)
            
            if (meaning != null) {
                parent.addView(TextView(requireContext()).apply {
                    text = meaning
                    setTextColor(android.graphics.Color.parseColor("#667568"))
                    textSize = 11.5f
                    typeface = fontRegular
                    setPadding(0, 0, 0, dp(6))
                })
            }
        }

        val section = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val headerContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(android.graphics.Color.parseColor("#FFF176"))
            setPadding(dp(20), dp(8), dp(20), dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }

            val header = TextView(requireContext()).apply {
                text = "ฤกษ์เดินทาง"
                setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                textSize = 15f
                typeface = fontSemiBold
            }
            addView(header)
        }
        section.addView(headerContainer)

        section.addView(TextView(requireContext()).apply {
            text = "เริ่มออกเดินทางไปทางทิศเทพเจ้าก่อน หากจำเป็นค่อยโค้งกลับเข้าทิศหมายที่ต้องการ"
            setTextColor(android.graphics.Color.parseColor("#5D6B5F"))
            textSize = 12.5f
            typeface = fontRegular
            setPadding(dp(20), dp(2), dp(20), dp(8))
        })

        section.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    android.graphics.Color.parseColor("#FFFFFF"),
                    android.graphics.Color.parseColor("#FFFFFF")
                )
            ).apply {
                setStroke(dp(1), android.graphics.Color.parseColor("#E0E0E0"))
                cornerRadius = dp(20).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(20)
                marginEnd = dp(20)
            }
            elevation = dp(2).toFloat()

            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL

                addView(TextView(requireContext()).apply {
                    text = "ทิศเทพเจ้า"
                    setTextColor(android.graphics.Color.parseColor("#0E6A3A"))
                    textSize = 11f
                    typeface = fontSemiBold
                    setPadding(dp(8), dp(3), dp(8), dp(3))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.parseColor("#F5F5F5"))
                        cornerRadius = dp(999).toFloat()
                    }
                })

                addView(android.widget.ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_travel)
                    layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                        marginStart = dp(6)
                    }
                    alpha = 0.95f
                })

                addView(TextView(requireContext()).apply {
                    text = "ทิศเด่นสำหรับเริ่มเดินทาง"
                    setTextColor(android.graphics.Color.parseColor("#4B6A50"))
                    textSize = 12f
                    typeface = fontSemiBold
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = dp(8)
                    }
                })
            })

            addView(TextView(requireContext()).apply {
                text = info.deityDirection
                setTextColor(android.graphics.Color.parseColor("#1976D2"))
                textSize = 30f
                typeface = fontSemiBold
                setShadowLayer(4f, 0f, 1f, android.graphics.Color.parseColor("#302196F3"))
                setPadding(0, dp(0), 0, dp(2))
            })

            addView(TextView(requireContext()).apply {
                text = "เชื่อว่าเป็นทิศเทพเจ้าประจำวัน เหมาะใช้เป็นจุดเริ่มต้นก่อนมุ่งไปจุดหมายจริง"
                setTextColor(android.graphics.Color.parseColor("#4F6153"))
                textSize = 12.5f
                typeface = fontRegular
                maxLines = 2
            })
        })

        section.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#F8FBF8"))
                setStroke(dp(1), android.graphics.Color.parseColor("#D9EBD9"))
                cornerRadius = dp(16).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
                marginStart = dp(20)
                marginEnd = dp(20)
            }

            addInfoRow(this, "หลาวเหล็ก", info.spearDirection, "#FFE5E5", "#C62828", 
                "คือ เมื่อท่านจะเดินทาง ไม่ควรหันหน้าหรือเดินทางให้ตรงกับทิศหลาวเหล็กตก เพราะจะแพ้ภัยตัวเอง หรือจะเจ็บไข้ได้ป่วย ต้องศัตสราวุธ ประสบภยันอันตราย ระหว่างทางแล")
            addInfoRow(this, "ผีหลวง", info.ghostDirection, "#FFF0D6", "#AD6A00", 
                "ทิศผีหลวงจะอยู่ตรงกันข้ามกับ ทิศเทวดา - เทพเจ้า เมื่อรู้ว่ามีผีหลวงอยู่ทิศใด ไม่ควรหันหน้าหรือเดินทางไปทิศนั้น จะนั่งในเรือนใคร อย่าได้นั่งหันหน้าไปทางนั้น ไม่ดีเลย")
        })

        return section
    }

    private fun updateCalendarScrollLock() {
        // Requirement: member ที่ยังไม่ปลดล็อก ห้ามเลื่อนลงดูปฏิทิน
        isCalendarScrollLocked = isLoggedInUser && !hasRengyamAccess
        wanpraLayoutManager?.requestLayout()
    }

    private fun normalizeVipAccessToken(value: String?): String {
        return value
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace("บาท", "")
            ?.replace(" ", "")
            ?.replace(",", "")
            .orEmpty()
    }

    private fun isRengyamVipDescriptor(value: String?): Boolean {
        val normalized = normalizeVipAccessToken(value)
        if (normalized.isBlank()) return false

        return normalized == "rengyam_vip" ||
            normalized == "rengyamyearly" ||
            normalized == "rengyam_yearly" ||
            normalized.contains("ดูฤกษ์ยาม") ||
            normalized.contains("ฤกษ์ยาม1ปี") ||
            normalized.contains("ฤกษ์ยามรายปี") ||
            normalized.contains("5999") ||
            normalized.contains("rengyam")
    }

    private fun deriveRengyamExpireAtMillis(rawAccess: String?): Long {
        val cal = Calendar.getInstance()
        if (normalizeVipAccessToken(rawAccess).contains("vip")) {
            cal.add(Calendar.YEAR, 50)
        } else {
            cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun computeRengyamAccess(): Boolean {
        val usagePref = requireContext().getSharedPreferences(RENGYAM_USAGE_PREFS, android.content.Context.MODE_PRIVATE)
        val userId = currentUserId() ?: return false
        migrateLegacyRengyamAccessIfNeeded(usagePref, userId)
        val isGranted = usagePref.getBoolean(userScopedGrantedKey(userId), false)
        if (!isGranted) return false

        val expireAt = usagePref.getLong(userScopedExpireKey(userId), 0L)
        if (expireAt == 0L) {
            usagePref.edit().putBoolean(userScopedGrantedKey(userId), false).apply()
            return false
        }
        if (expireAt <= System.currentTimeMillis()) {
            usagePref.edit()
                .putBoolean(userScopedGrantedKey(userId), false)
                .remove(userScopedExpireKey(userId))
                .apply()
            return false
        }
        return true
    }

    private fun currentUserId(): String? {
        return UserContextManager.userX(requireContext())?.userId?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun userScopedGrantedKey(userId: String): String = "${RENGYAM_ACCESS_GRANTED_KEY}_$userId"

    private fun userScopedExpireKey(userId: String): String = "${RENGYAM_ACCESS_EXPIRE_AT_KEY}_$userId"

    private fun migrateLegacyRengyamAccessIfNeeded(
        usagePref: android.content.SharedPreferences,
        userId: String
    ) {
        val alreadyMigrated = usagePref.contains(userScopedGrantedKey(userId)) || usagePref.contains(userScopedExpireKey(userId))
        if (alreadyMigrated) return

        val legacyGranted = usagePref.getBoolean(RENGYAM_ACCESS_GRANTED_KEY, false)
        val legacyExpireAt = usagePref.getLong(RENGYAM_ACCESS_EXPIRE_AT_KEY, 0L)
        if (!legacyGranted || legacyExpireAt == 0L) return

        usagePref.edit()
            .putBoolean(userScopedGrantedKey(userId), legacyGranted)
            .putLong(userScopedExpireKey(userId), legacyExpireAt)
            .apply()
    }

    private fun persistRengyamAccessForUser(userId: String, expireAtMillis: Long, granted: Boolean = true) {
        requireContext().getSharedPreferences(RENGYAM_USAGE_PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(userScopedGrantedKey(userId), granted)
            .putLong(userScopedExpireKey(userId), expireAtMillis)
            .apply()
    }

    private fun clearRengyamAccessForUser(userId: String) {
        requireContext().getSharedPreferences(RENGYAM_USAGE_PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(userScopedGrantedKey(userId), false)
            .remove(userScopedExpireKey(userId))
            .apply()
    }

    private fun syncRengyamAccessFromServer(memberData: Serverx?) {
        val userId = currentUserId() ?: return
        val access = memberData?.rengyamAccess
        if (access != null && access.granted && isRengyamVipDescriptor(access.viptype ?: access.codename)) {
            val expireAtMillis = try {
                val expireAt = access.expireAt
                if (expireAt.isNullOrBlank()) {
                    deriveRengyamExpireAtMillis(access.viptype ?: access.codename)
                } else {
                    DateTime.parse(expireAt).plusDays(1).minusMillis(1).millis
                }
            } catch (e: Exception) {
                deriveRengyamExpireAtMillis(access.viptype ?: access.codename)
            }

            persistRengyamAccessForUser(userId, expireAtMillis, granted = true)
            return
        }

        val fallbackVipCode = memberData?.userx?.vipcode
        if (isRengyamVipDescriptor(fallbackVipCode)) {
            persistRengyamAccessForUser(userId, deriveRengyamExpireAtMillis(fallbackVipCode), granted = true)
        }
    }

    private fun showRengyamUnlockDialog(categoryKey: String) {
        if (!isAdded) return
        val dialogView = layoutInflater.inflate(R.layout.dialog_unlock_rengyam, null)
        val input = dialogView.findViewById<android.widget.EditText>(R.id.edt_unlock_secret_code)
        val btnChat = dialogView.findViewById<View>(R.id.btn_unlock_chat_ninin)
        val btnUnlock = dialogView.findViewById<View>(R.id.btn_unlock_submit)

        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        dialog.setContentView(dialogView)
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        btnChat.setOnClickListener {
            try {
                startActivity(android.content.Intent(requireContext(), com.numberniceic.ui.chat.ChatActivity::class.java))
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "ไม่สามารถเปิดหน้าคุณนินได้", Toast.LENGTH_SHORT).show()
            }
        }

        btnUnlock.setOnClickListener {
            val code = input.text?.toString()?.trim().orEmpty()
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "กรุณากรอก VIP CODE", Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                unlockRengyamWithCode(code, categoryKey)
            }
        }

        dialog.show()
    }

    private fun unlockRengyamWithCode(code: String, categoryKey: String) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val userId = currentUserId()
        if (userId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "กรุณาเข้าสู่ระบบก่อนปลดล็อก", Toast.LENGTH_SHORT).show()
            return
        }

        val body = JsonObject().apply {
            addProperty("vipcode", code)
            addProperty("userid", userId)
        }

        apiService.userVipcodeUpgrade(body).enqueue(object : Callback<com.numberniceic.data.admin.ServerVip> {
            override fun onResponse(
                call: Call<com.numberniceic.data.admin.ServerVip>,
                response: Response<com.numberniceic.data.admin.ServerVip>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val serverVip = response.body()!!
                    if (!handleUnlockSuccess(serverVip.message, serverVip.viplevel, userId, categoryKey)) {
                        Toast.makeText(requireContext(), "โค้ดนี้ไม่ใช่สิทธิ์ดูฤกษ์ยาม", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (!handleUnlockSuccessFromErrorBody(errorBody, userId, categoryKey)) {
                        Toast.makeText(requireContext(), "ตรวจสอบโค้ดไม่สำเร็จ (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<com.numberniceic.data.admin.ServerVip>, t: Throwable) {
                Toast.makeText(requireContext(), "เชื่อมต่อเซิร์ฟเวอร์ไม่ได้: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleUnlockSuccess(message: String?, vipLevel: String?, userId: String, categoryKey: String): Boolean {
        val normalizedMessage = message?.lowercase(Locale.ROOT)?.trim().orEmpty()
        val vipType = vipLevel?.lowercase(Locale.ROOT)?.trim().orEmpty()
        if (normalizedMessage != "success" || !isRengyamVipDescriptor(vipLevel)) {
            return false
        }

        persistRengyamAccessForUser(userId, deriveRengyamExpireAtMillis(vipType))

        hasRengyamAccess = true
        updateCalendarScrollLock()
        (rengYamBinding.recyclerviewWanpra.adapter as? WanpraAdapter)?.setRengyamAccess(true)
        Toast.makeText(requireContext(), "ปลดล็อกดูฤกษ์ยามสำเร็จ", Toast.LENGTH_LONG).show()
        onCategorySelectedAfterUnlock(categoryKey)
        return true
    }

    private fun handleUnlockSuccessFromErrorBody(errorBody: String?, userId: String, categoryKey: String): Boolean {
        if (errorBody.isNullOrBlank()) return false
        return try {
            val jsonPart = if (errorBody.contains("<!doctype", ignoreCase = true)) {
                errorBody.substringBefore("<!doctype", "").trim()
            } else {
                errorBody
            }
            if (jsonPart.isBlank()) return false
            val json = JsonParser.parseString(jsonPart).asJsonObject
            val message = json.get("message")?.asString
            val vipLevel = json.get("viplevel")?.asString
            handleUnlockSuccess(message, vipLevel, userId, categoryKey)
        } catch (e: Exception) {
            false
        }
    }

    private fun onCategorySelectedAfterUnlock(categoryKey: String) {
        if (cachedLengYamDao == null) {
            initRecyclerView()
            fetchAuspiciousForSelectedMonth()
        }
        val prev = headerData.selectedCategory
        val selectedCategory = if (categoryKey == prev) null else categoryKey
        headerData = headerData.copy(selectedCategory = selectedCategory)
        cachedLengYamDao?.let { updateRecyclerWithData(it) }
        
        if (!selectedCategory.isNullOrBlank()) {
            if (selectedCategory == "marriage") {
                showMarriageFormBottomSheet()
            } else {
                showAuspiciousBottomSheet(selectedCategory)
            }
        }
    }

    private fun renderLockedCalendarPlaceholder() {
        fullDataList.clear()
        fullDataList.add(headerData.copy(selectedCategory = null))

        val weekDays = listOf("จันทร์", "อังคาร", "พุธ", "พฤหัส", "ศุกร์", "เสาร์", "อาทิตย์")
        weekDays.forEach { fullDataList.add(com.numberniceic.adapters.WeekdayHeader(it)) }

        val monthStart = currentViewDate.dayOfMonth().withMinimumValue()
        val firstDayOffset = (monthStart.dayOfWeek - 1).coerceAtLeast(0)
        if (firstDayOffset > 0) {
            val prevMonthDate = monthStart.minusMonths(1)
            val lastDayPrev = prevMonthDate.dayOfMonth().withMaximumValue().dayOfMonth
            val prevMonth = prevMonthDate.monthOfYear
            val prevYear = prevMonthDate.year

            for (day in (lastDayPrev - firstDayOffset + 1)..lastDayPrev) {
                val dtStr = String.format(Locale.US, "%04d-%02d-%02d", prevYear, prevMonth, day)
                val wp = Wanpra(
                    wanpraId = null,
                    wanpraDate = dtStr,
                    isWanpra = "0",
                    isTongchai = "0",
                    isAtipbadee = "0",
                    isKating = "0",
                    lunarPhase = "ล็อก",
                    lunarMonth = "กรุณาปลดล็อก"
                )
                wp.isOtherMonth = true
                fullDataList.add(wp)
            }
        }

        val lastDay = currentViewDate.dayOfMonth().withMaximumValue().dayOfMonth
        for (day in 1..lastDay) {
            val dtStr = String.format(Locale.US, "%04d-%02d-%02d", currentViewDate.year, currentViewDate.monthOfYear, day)
            fullDataList.add(
                Wanpra(
                    wanpraId = day.toString(),
                    wanpraDate = dtStr,
                    isWanpra = "0",
                    isTongchai = "0",
                    isAtipbadee = "0",
                    isKating = "0",
                    lunarPhase = "ล็อก",
                    lunarMonth = "กรุณาปลดล็อก"
                )
            )
        }

        val totalCells = fullDataList.count { it is Wanpra }
        val remaining = 42 - totalCells
        if (remaining > 0) {
            val nextMonthDate = monthStart.plusMonths(1)
            val nextMonth = nextMonthDate.monthOfYear
            val nextYear = nextMonthDate.year

            for (day in 1..remaining) {
                val dtStr = String.format(Locale.US, "%04d-%02d-%02d", nextYear, nextMonth, day)
                val wp = Wanpra(
                    wanpraId = null,
                    wanpraDate = dtStr,
                    isWanpra = "0",
                    isTongchai = "0",
                    isAtipbadee = "0",
                    isKating = "0",
                    lunarPhase = "ล็อก",
                    lunarMonth = "กรุณาปลดล็อก"
                )
                wp.isOtherMonth = true
                fullDataList.add(wp)
            }
        }

        rengYamBinding.recyclerviewWanpra.adapter?.notifyDataSetChanged()
    }

    private fun initRecyclerView() {
        fetchAuspiciousForSelectedMonth()
    }

    private fun fetchAuspiciousForSelectedMonth() {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val requestYear = currentViewDate.year
        val requestMonth = currentViewDate.monthOfYear
        beginLoading()
        apiService.getLengYam(requestYear, requestMonth).enqueue(object : Callback<LengYamDao> {
            override fun onResponse(call: Call<LengYamDao>, response: Response<LengYamDao>) {
                try {
                    if (!(response.isSuccessful && response.body() != null)) return
                    if (requestYear != currentViewDate.year || requestMonth != currentViewDate.monthOfYear) return
                    val lengyamDao = response.body()!!
                    Log.d("RengYamDebug", "Response Code: ${response.code()}")
                    updateRecyclerWithData(lengyamDao)
                    if (lengyamDao.lengYam != null) {
                        updateHeaderSpecial(lengyamDao.lengYam)
                    }
                } finally {
                    endLoading()
                }
            }
            override fun onFailure(call: Call<LengYamDao>, t: Throwable) {
                try {
                    Log.d("RetrofitERROR", t.message ?: "Unknown error")
                } finally {
                    endLoading()
                }
            }
        })
    }

    private fun updateRecyclerWithData(lengyamDao: LengYamDao) {
        this.cachedLengYamDao = lengyamDao
        val viewMonth = currentViewDate.monthOfYear
        val viewYear = currentViewDate.year

        fullDataList.clear()
        
        val currentSpecialList = mutableListOf<Any>()
        if (currentViewDate.toLocalDate() == DateTime.now().toLocalDate()) {
            lengyamDao.lengYam?.let { 
                val desc = it.wanDesc ?: ""
                if (desc.isNotEmpty() && desc != "วันนี้วันพระ" && desc != "พรุ่งนี้วันพระ") {
                    currentSpecialList.add(it) 
                }
            }
        }
        
        // Use the member map
        dateToWanpraMap.clear()
        val dateToWanpra = dateToWanpraMap
        lengyamDao.wanPras?.forEach { 
            it.isHighlighted = false 
            it.isBestDay = false
        }

        val lastDay = currentViewDate.dayOfMonth().withMaximumValue().dayOfMonth
        for (i in 1..lastDay) {
            val dtStr = String.format(Locale.US, "%04d-%02d-%02d", viewYear, viewMonth, i)
            val baseWanpra = Wanpra(
                wanpraId = null,
                wanpraDate = dtStr,
                isWanpra = "0",
                isTongchai = "0",
                isAtipbadee = "0",
                isKating = "0"
            )
            applyLocalAuspiciousData(baseWanpra, formatter.parseDateTime(dtStr).toLocalDate())
            dateToWanpra[dtStr] = baseWanpra
        }

        lengyamDao.wanPras?.forEach { wp ->
            val dtStr = wp.wanpraDate ?: ""
            val localWp = dateToWanpra[dtStr] ?: return@forEach
            val mergedCalendarDisplayTags = (localWp.calendarDisplayTags.orEmpty() + wp.calendarDisplayTags.orEmpty()).distinct()
            val mergedWarningTags = (localWp.warningTags.orEmpty() + wp.warningTags.orEmpty()).distinct()
            val mergedWp = localWp.copy(
                wanpraId = wp.wanpraId ?: localWp.wanpraId,
                // Use local lunar arithmetic as the source of truth for wanpra days.
                isWanpra = localWp.isWanpra,
                isTongchai = if (isFlagEnabled(wp.isTongchai)) "1" else "0",
                isAtipbadee = if (isFlagEnabled(wp.isAtipbadee)) "1" else "0",
                isKating = if (isFlagEnabled(wp.isKating)) "1" else "0",
                isLoy = wp.isLoy,
                isJom = wp.isJom,
                isFu = wp.isFu,
                displayTags = if (!wp.displayTags.isNullOrEmpty()) wp.displayTags else localWp.displayTags,
                displayTagsPrioritized = if (!wp.displayTagsPrioritized.isNullOrEmpty()) wp.displayTagsPrioritized else localWp.displayTagsPrioritized,
                calendarDisplayTags = mergedCalendarDisplayTags.ifEmpty { null },
                kalTags = if (!wp.kalTags.isNullOrEmpty()) wp.kalTags else localWp.kalTags,
                dithiTags = if (!wp.dithiTags.isNullOrEmpty()) wp.dithiTags else localWp.dithiTags,
                dayTypeTags = if (!wp.dayTypeTags.isNullOrEmpty()) wp.dayTypeTags else localWp.dayTypeTags,
                warningTags = mergedWarningTags.ifEmpty { null },
                tagDetails = if (!wp.tagDetails.isNullOrEmpty()) wp.tagDetails else localWp.tagDetails,
                myhoraDisplayTags = if (!wp.myhoraDisplayTags.isNullOrEmpty()) wp.myhoraDisplayTags else localWp.myhoraDisplayTags,
                myhoraDisplayTagsPrioritized = if (!wp.myhoraDisplayTagsPrioritized.isNullOrEmpty()) wp.myhoraDisplayTagsPrioritized else localWp.myhoraDisplayTagsPrioritized,
                myhoraTagDetails = if (!wp.myhoraTagDetails.isNullOrEmpty()) wp.myhoraTagDetails else localWp.myhoraTagDetails,
                mahamodoDisplayTags = if (!wp.mahamodoDisplayTags.isNullOrEmpty()) wp.mahamodoDisplayTags else localWp.mahamodoDisplayTags,
                mahamodoDisplayTagsPrioritized = if (!wp.mahamodoDisplayTagsPrioritized.isNullOrEmpty()) wp.mahamodoDisplayTagsPrioritized else localWp.mahamodoDisplayTagsPrioritized,
                mahamodoTagDetails = if (!wp.mahamodoTagDetails.isNullOrEmpty()) wp.mahamodoTagDetails else localWp.mahamodoTagDetails,
                isRiangMon = wp.isRiangMon,
                isSittichok = wp.isSittichok,
                isAmmarit = wp.isAmmarit,
                isMahaSittichok = wp.isMahaSittichok,
                isRachaChok = wp.isRachaChok,
                isChaiChok = wp.isChaiChok,
                isUbath = wp.isUbath,
                isLokawinat = wp.isLokawinat,
                isHighlighted = false,
                isBestDay = false
            )
            sanitizeAgniknirodTags(mergedWp, formatter.parseDateTime(dtStr).toLocalDate())
            RengYamTagCanonicalizer.normalize(mergedWp)
            limitCalendarPreviewForPublic(mergedWp)
            dateToWanpra[dtStr] = mergedWp
        }

        dateToWanpra.forEach { (dateKey, wp) ->
            // Re-normalize flags after merging backend + local arithmetic data.
            wp.isWanpra = if (isFlagEnabled(wp.isWanpra)) "1" else "0"
            wp.isTongchai = if (isFlagEnabled(wp.isTongchai)) "1" else "0"
            wp.isAtipbadee = if (isFlagEnabled(wp.isAtipbadee)) "1" else "0"
            wp.isKating = if (isFlagEnabled(wp.isKating)) "1" else "0"
            wp.isKalagni = false
            wp.kalagniBirth = false
            wp.kalagniAge = false
            wp.kalagniBirthLabel = null
            wp.kalagniAgeLabel = null

            val dt = formatter.parseDateTime(dateKey).toLocalDate()
            val wDay = dt.dayOfWeek // Joda: 1=Mon, 7=Sun
            // Thai mapping: Mon=1, Sun=7.
            val dayNamesMapped = mapOf(1 to "จันทร์", 2 to "อังคาร", 3 to "พุธ", 4 to "พฤหัสบดี", 5 to "ศุกร์", 6 to "เสาร์", 7 to "อาทิตย์")
            val currentDayName = dayNamesMapped[wDay] ?: "อาทิตย์"

            val matchedBirthDay = headerData.kalagniBirthDays.orEmpty().firstOrNull { isSameKalagniDay(currentDayName, it) }
            val dynamicAgeDays = normalizeKalagniDays(null, dynamicKalagniAgeDayForDate(dt))
            val matchedAgeDay   = dynamicAgeDays.firstOrNull { isSameKalagniDay(currentDayName, it) }

            wp.kalagniBirth = matchedBirthDay != null
            wp.kalagniAge   = matchedAgeDay != null
            wp.kalagniBirthLabel = matchedBirthDay?.trim()?.replace("วัน", "")
            wp.kalagniAgeLabel   = matchedAgeDay?.trim()?.replace("วัน", "")
            wp.isKalagni = wp.kalagniBirth || wp.kalagniAge
            applyPersonalizedKalagniTags(wp)
            sanitizeAgniknirodTags(wp, dt)
            RengYamTagCanonicalizer.normalize(wp)
            limitCalendarPreviewForPublic(wp)

            wp.isBestDay = false
        }

        val monthWanpras = dateToWanpra.values.toList()

        val safeDaysWithScore = monthWanpras.map { wp ->
            val allTags = buildList {
                addAll(wp.calendarDisplayTags.orEmpty())
                addAll(wp.warningTags.orEmpty())
                addAll(wp.displayTags.orEmpty())
                addAll(wp.dayTypeTags.orEmpty())
            }.joinToString("|")
            val badMarkers = listOf("ทินสูญ", "ทินสูรย์", "ทรทึก", "มหาสูญ", "อายกรรมพลาย", "ทัคธทิน", "กาลทิน", "ยมขันธ์", "พิลา", "ทึกทึน", "ทักทิน", "อัตนิโรจน์", "อัคนิโรธ", "ห้าม")
            val hasBackendBadStuff = badMarkers.any { allTags.contains(it) }

            val isSafe = !wp.isKalagni && !wp.isUbath && !wp.isLokawinat && !wp.isJom && !isFlagEnabled(wp.isKating) && !hasBackendBadStuff
            var score = 0
            if (isSafe) { 
                if (isFlagEnabled(wp.isWanpra)) score++
                if (isFlagEnabled(wp.isTongchai)) score++
                if (isFlagEnabled(wp.isAtipbadee)) score++
                if (wp.isAmmarit) score += 2
                if (wp.isMahaSittichok) score += 2
                if (wp.isSittichok) score++
                if (wp.isRachaChok) score++
                if (wp.isChaiChok) score++
                if (wp.isLoy) score++
                if (wp.isFu) score++
                if (wp.isRiangMon) score++
            }
            wp to if (isSafe) score else -1
        }

        val maxScore = safeDaysWithScore.maxByOrNull { it.second }?.second ?: -1
        if (maxScore > 0) {
            safeDaysWithScore.filter { it.second == maxScore }.forEach { (wp, _) -> wp.isBestDay = true }
        }

        val bestWanpras = monthWanpras.filter { it.isBestDay }
        val bestDates = bestWanpras.mapNotNull { it.wanpraDate }
        val safeWanpras = monthWanpras.filter { wp -> isGenerallyEligibleForCategory(wp) }

        // Logic Helper: Is any of the "Maha Chok" present?
        fun hasMahaChok(wp: Wanpra) = wp.isSittichok || wp.isAmmarit || wp.isMahaSittichok || wp.isRachaChok || wp.isChaiChok

        headerData = headerData.copy(
            marriageDays = datesForCategory(safeWanpras, "marriage", bestDates),
            surgeryDays = datesForCategory(safeWanpras, "surgery", bestDates),
            houseDays = datesForCategory(safeWanpras, "house", bestDates),
            ordinationDays = datesForCategory(safeWanpras, "ordination", bestDates),
            deordinationDays = datesForCategory(safeWanpras, "deordination", bestDates),
            carDays = datesForCategory(safeWanpras, "car", bestDates),
            buyCarDays = datesForCategory(safeWanpras, "buy_car", bestDates),
            childbirthDays = datesForCategory(safeWanpras, "childbirth", bestDates),
            shopDays = datesForCategory(safeWanpras, "shop", bestDates),
            buildHouseDays = datesForCategory(safeWanpras, "build_house", bestDates),
            moveHouseDays = datesForCategory(safeWanpras, "move_house", bestDates),
            meritDays = datesForCategory(safeWanpras, "merit", bestDates),
            engagementDays = datesForCategory(safeWanpras, "engagement", bestDates),
            debtDays = datesForCategory(safeWanpras, "debt", bestDates),
            plantDays = datesForCategory(safeWanpras, "plant", bestDates),
            spiritHouseDays = datesForCategory(safeWanpras, "spirit_house", bestDates),
            travelDays = datesForCategory(safeWanpras, "travel", bestDates),
            promotionDays = datesForCategory(safeWanpras, "promotion", bestDates),
            jobDays = datesForCategory(safeWanpras, "job", bestDates),
            businessDays = datesForCategory(safeWanpras, "business", bestDates)
        )
        this.headerData = headerData

        dateToWanpra.keys.forEach { dateKey ->
            addTravelDirectionTagForDay(dateKey)
        }

        // 🌟 Apply Highlight Logic based on selected Category
        val selectedCat = headerData.selectedCategory
        val highlightDates = when (selectedCat) {
            "marriage" -> headerData.marriageDays ?: emptyList()
            "surgery" -> headerData.surgeryDays ?: emptyList()
            "house" -> headerData.houseDays ?: emptyList()
            "ordination" -> headerData.ordinationDays ?: emptyList()
            "deordination" -> headerData.deordinationDays ?: emptyList()
            "car" -> headerData.carDays ?: emptyList()
            "buy_car" -> headerData.buyCarDays ?: emptyList()
            "childbirth" -> headerData.childbirthDays ?: emptyList()
            "shop" -> headerData.shopDays ?: emptyList()
            "build_house" -> headerData.buildHouseDays ?: emptyList()
            "move_house" -> headerData.moveHouseDays ?: emptyList()
            "merit" -> headerData.meritDays ?: emptyList()
            "engagement" -> headerData.engagementDays ?: emptyList()
            "debt" -> headerData.debtDays ?: emptyList()
            "plant" -> headerData.plantDays ?: emptyList()
            "spirit_house" -> headerData.spiritHouseDays ?: emptyList()
            "travel" -> headerData.travelDays ?: emptyList()
            "promotion" -> headerData.promotionDays ?: emptyList()
            "job" -> headerData.jobDays ?: emptyList()
            "business" -> headerData.businessDays ?: emptyList()
            else -> emptyList()
        }
        
        if (highlightDates.isNotEmpty()) {
            dateToWanpra.forEach { (dateKey, wp) ->
                if (highlightDates.contains(dateKey)) {
                    wp.isHighlighted = true
                }
            }
        }

        // Top header and weekday labels are pinned outside the RecyclerView so the
        // calendar body scrolls without duplicating the controls or icon cards.

        val sortedDates = dateToWanpra.keys.toList().sorted()
        if (sortedDates.isNotEmpty()) {
            val firstDate = formatter.parseDateTime(sortedDates[0])
            val offset = firstDate.dayOfWeek - 1 // Mon=0, Sun=6
            
            if (offset > 0) {
                val prevMonthDate = firstDate.minusMonths(1)
                val lastDayPrev = prevMonthDate.dayOfMonth().withMaximumValue().dayOfMonth
                val prevMonth = prevMonthDate.monthOfYear
                val prevYear = prevMonthDate.year
                
                for (j in (lastDayPrev - offset + 1)..lastDayPrev) {
                    val dtStr = String.format(Locale.US, "%04d-%02d-%02d", prevYear, prevMonth, j)
                    val wp = Wanpra(wanpraId = null, wanpraDate = dtStr, isWanpra = "0")
                    wp.isOtherMonth = true
                    val dt = formatter.parseDateTime(dtStr).toLocalDate()
                    applyLocalAuspiciousData(wp, dt)
                    sanitizeAgniknirodTags(wp, dt)
                    RengYamTagCanonicalizer.normalize(wp)
                    limitCalendarPreviewForPublic(wp)
                    fullDataList.add(wp)
                }
            }
        }
        for (dateKey in sortedDates) {
            val wp = dateToWanpra[dateKey]!!
            val highlightedDates = when(headerData.selectedCategory) {
                "marriage" -> headerData.marriageDays
                "surgery" -> headerData.surgeryDays
                "house" -> headerData.houseDays
                "ordination" -> headerData.ordinationDays
                "deordination" -> headerData.deordinationDays
                "car" -> headerData.carDays
                "buy_car" -> headerData.buyCarDays
                "childbirth" -> headerData.childbirthDays
                "shop" -> headerData.shopDays
                "build_house" -> headerData.buildHouseDays
                "move_house" -> headerData.moveHouseDays
                "merit" -> headerData.meritDays
                "engagement" -> headerData.engagementDays
                "debt" -> headerData.debtDays
                "plant" -> headerData.plantDays
                "spirit_house" -> headerData.spiritHouseDays
                "travel" -> headerData.travelDays
                "promotion" -> headerData.promotionDays
                "job" -> headerData.jobDays
                "business" -> headerData.businessDays
                else -> null
            }
            wp.isHighlighted = highlightedDates?.contains(dateKey) == true
            fullDataList.add(wp)
        }

        // Fill trailing days for next month to complete the 42-cell grid (6 rows)
        val totalCells = fullDataList.count { it is Wanpra }
        val remaining = 42 - totalCells
        if (remaining > 0 && sortedDates.isNotEmpty()) {
             val lastDate = formatter.parseDateTime(sortedDates.last())
             val nextMonthDate = lastDate.plusMonths(1)
             val nextMonth = nextMonthDate.monthOfYear
             val nextYear = nextMonthDate.year
             
             for (k in 1..remaining) {
                 val dtStr = String.format(Locale.US, "%04d-%02d-%02d", nextYear, nextMonth, k)
                 val wp = Wanpra(wanpraId = null, wanpraDate = dtStr, isWanpra = "0")
                 wp.isOtherMonth = true
                 val dt = formatter.parseDateTime(dtStr).toLocalDate()
                 applyLocalAuspiciousData(wp, dt)
                 sanitizeAgniknirodTags(wp, dt)
                 RengYamTagCanonicalizer.normalize(wp)
                 limitCalendarPreviewForPublic(wp)
                 fullDataList.add(wp)
             }
        }
        
        setupAdapterList()
    }

    private fun toThaiNum(s: String): String {
        return s.replace('0','๐')
                .replace('1','๑')
                .replace('2','๒')
                .replace('3','๓')
                .replace('4','๔')
                .replace('5','๕')
                .replace('6','๖')
                .replace('7','๗')
                .replace('8','๘')
                .replace('9','๙')
    }

    private fun applyLocalAuspiciousData(wp: Wanpra, dt: org.joda.time.LocalDate) {
        val (dithi, mThai) = ThaiAstrologyLib.getThaiLunar(dt)
        val kalayok = ThaiAstrologyLib.getKalayok(dt)
        val chok = ThaiAstrologyLib.queryMahaChok(dt.dayOfWeek, dithi)
        val loyFuJom = ThaiAstrologyLib.getLoyFuJom(dt.dayOfWeek, dithi, mThai)
        val myHoraDisplayTags = ThaiAstrologyLib.buildMyHoraDisplayTags(dt, getMyHoraVerifiedTagOverrides())
        val myHoraBundle = ThaiAstrologyLib.buildMyHoraTagBundle(myHoraDisplayTags)
        val myHoraPrioritized = myHoraBundle.prioritized()
        val dithiProhibition = ThaiAstrologyLib.queryDithiCommonProhibitions(dithi)
        val inauspicious = ThaiAstrologyLib.queryInauspicious(dt.dayOfWeek, dithi)
        val extraWarnings = buildList {
            if (!dithiProhibition.isNullOrBlank()) add(dithiProhibition)
            addAll(inauspicious)
        }

        wp.isWanpra = if (ThaiAstrologyLib.isWanPra(dt)) "1" else "0"
        wp.isTongchai = if (kalayok.contains("ธงชัย")) "1" else "0"
        wp.isAtipbadee = if (kalayok.contains("อธิบดี")) "1" else "0"
        wp.isKating = if (ThaiAstrologyLib.isKatingDay(dt)) "1" else "0"
        wp.isUbath = kalayok.contains("อุบาทว์")
        wp.isLokawinat = kalayok.contains("โลกาวินาศ")
        wp.isRiangMon = ThaiAstrologyLib.isRiangMon(dithi)
        wp.isAmmarit = chok.contains("อำฤตโชค")
        wp.isMahaSittichok = chok.contains("มหาสิทธิโชค")
        wp.isSittichok = chok.contains("สิทธิโชค")
        wp.isRachaChok = chok.contains("ราชาโชค")
        wp.isChaiChok = chok.contains("ชัยโชค")
        wp.isLoy = myHoraPrioritized.contains("วันลอย") || loyFuJom.contains("วันลอย")
        wp.isFu = myHoraPrioritized.contains("วันฟู") || loyFuJom.contains("วันฟู")
        wp.isJom = myHoraPrioritized.contains("วันจม") || loyFuJom.contains("วันจม")
        wp.myhoraDisplayTags = myHoraDisplayTags.ifEmpty { null }
        wp.myhoraDisplayTagsPrioritized = myHoraPrioritized.ifEmpty { null }
        wp.kalTags = myHoraBundle.kalTags.ifEmpty { null }
        wp.dithiTags = myHoraBundle.dithiTags.ifEmpty { null }
        wp.dayTypeTags = myHoraBundle.dayTypeTags.ifEmpty { null }
        wp.warningTags = (myHoraBundle.warningTags + extraWarnings).distinct().ifEmpty { null }
        wp.calendarDisplayTags = (myHoraPrioritized + extraWarnings).distinct().ifEmpty { null }
        wp.displayTags = wp.calendarDisplayTags
        wp.displayTagsPrioritized = wp.calendarDisplayTags
        wp.lunarPhase = if (dithi <= 15) "ขึ้น ${toThaiNum(dithi.toString())} ค่ำ" else "แรม ${toThaiNum((dithi - 15).toString())} ค่ำ"
        wp.lunarMonth = toThaiNum(mThai.toString())
    }

    private fun isDateKalagni(dateStr: String): Boolean {
        return false
    }

    private fun fetchDataForHeader() {
        val context = context ?: return
        val cachedWanPra = PersonNewsCacheManager.loadWanPra(context)
        if (cachedWanPra?.wanSpecial != null) {
             updateHeaderSpecial(cachedWanPra.wanSpecial)
             return 
        }
    }

    private fun updateHeaderSpecial(wanSpecial: WanSpecial?) {
        if (wanSpecial == null) return
        
        // Calculate Today's Data for Banner
        val now = DateTime.now().toLocalDate()
        val nowDateStr = now.toString("yyyy-MM-dd")
        val backendTodayTags = dateToWanpraMap[nowDateStr]?.displayTags?.filter { it.isNotBlank() }.orEmpty()
        val tags = if (backendTodayTags.isNotEmpty()) {
            backendTodayTags.toMutableList()
        } else {
            val kalayok = ThaiAstrologyLib.getKalayok(now)
            val (dithi, mThai) = ThaiAstrologyLib.getThaiLunar(now)
            val lfj = ThaiAstrologyLib.getLoyFuJom(now.dayOfWeek, dithi, mThai)
            val chok = ThaiAstrologyLib.queryMahaChok(now.dayOfWeek, dithi)
            mutableListOf<String>().apply {
                addAll(kalayok)
                addAll(lfj)
                addAll(chok)
                if (ThaiAstrologyLib.isRiangMon(dithi)) add("ดิถีเรียงหมอน")
                if (ThaiAstrologyLib.isKatingDay(now)) add("กระทิงวัน")
                if (ThaiAstrologyLib.isWanPra(now)) add("วันพระ")
            }
        }

        val dayEng = DateTime.now().dayOfWeek().getAsText(java.util.Locale.ENGLISH)
        val thaiDay = PersonContextManager.toThaiDay(dayEng)
        val thaiMonth = toThaiMonthFull(DateTime.now().monthOfYear)
        val dateFull = "วันนี้$thaiDay ที่ ${DateTime.now().dayOfMonth} $thaiMonth"

        headerData = rebuildHeaderTags(headerData.copy(
            wanSpecial = wanSpecial, 
            todayDateStr = dateFull,
            todayStatusTags = tags.ifEmpty { listOf("วันปกติ") }
        ))
        updateRecyclerHeader()
    }

    private fun rebuildHeaderTags(data: TopHeaderData): TopHeaderData {
        val currentTags = data.todayStatusTags?.toMutableList() ?: mutableListOf()
        val todayName = data.todaySourceDay ?: ""
        
        if (todayName.isEmpty()) return data

        val isBirthBad = isSameKalagniDay(todayName, data.kalagniBirth ?: "")
        val isAgeBad = isSameKalagniDay(todayName, data.kalagniAge ?: "")
        
        if (isBirthBad) {
            if (!currentTags.contains("อัปมงคลวันเกิด")) currentTags.add("อัปมงคลวันเกิด")
        }
        if (isAgeBad) {
            if (!currentTags.contains("อัปมงคลอายุย่าง")) currentTags.add("อัปมงคลอายุย่าง")
        }
        
        // Final cleanup
        val finalTags = currentTags.distinct()
        return data.copy(todayStatusTags = if (finalTags.size > 1) finalTags.filter { it != "วันปกติ" } else finalTags)
    }

    private fun updateRecyclerHeader() {
        rengYamBinding.recyclerviewWanpra.adapter?.notifyDataSetChanged()
        syncFrozenHeader()
    }

    private fun setupFrozenHeader() {
        rengYamBinding.layoutWeekdayFrozen.visibility = View.VISIBLE
        val frozenTopHeaderView = rengYamBinding.layoutWeekdayFrozen.findViewById<View>(R.id.frozen_top_header)
        frozenTopHeaderHolder = WanpraAdapter.TopHeaderHolder(
            frozenTopHeaderView,
            isAdminMode = isAdminMode,
            isLoggedInUser = isLoggedInUser,
            hasRengyamAccess = hasRengyamAccess,
            onMonthNav = { direction ->
                if (!isLoggedInUser) return@TopHeaderHolder
                currentViewDate = currentViewDate.plusMonths(direction)
                headerData = headerData.copy(selectedCategory = null)
                initHeaderData()
                refreshDynamicKalagniHeaderForViewDate()
                syncFrozenHeader()
                cachedLengYamDao?.let { updateRecyclerWithData(it) }
                fetchAuspiciousForSelectedMonth()
            },
            onMonthSelected = { selectedMonth ->
                if (!isLoggedInUser) return@TopHeaderHolder
                currentViewDate = currentViewDate.withMonthOfYear(selectedMonth)
                headerData = headerData.copy(selectedCategory = null)
                initHeaderData()
                refreshDynamicKalagniHeaderForViewDate()
                syncFrozenHeader()
                cachedLengYamDao?.let { updateRecyclerWithData(it) }
                fetchAuspiciousForSelectedMonth()
            },
            onYearSelected = { selectedYearCe ->
                if (!isLoggedInUser) return@TopHeaderHolder
                currentViewDate = currentViewDate.withYear(selectedYearCe)
                headerData = headerData.copy(selectedCategory = null)
                initHeaderData()
                refreshDynamicKalagniHeaderForViewDate()
                syncFrozenHeader()
                cachedLengYamDao?.let { updateRecyclerWithData(it) }
                fetchAuspiciousForSelectedMonth()
                if (!shouldShowFullCalendar()) {
                    renderLockedCalendarPlaceholder()
                }
            },
            onCategorySelected = { selectedCategory ->
                val latestAccess = computeRengyamAccess()
                hasRengyamAccess = latestAccess
                updateCalendarScrollLock()
                if (!latestAccess) {
                    if (!selectedCategory.isNullOrBlank()) {
                        showRengyamUnlockDialog(selectedCategory)
                    }
                    return@TopHeaderHolder
                }
                val prev = headerData.selectedCategory
                val normalizedCategory = if (selectedCategory == prev) null else selectedCategory
                headerData = headerData.copy(selectedCategory = normalizedCategory)
                cachedLengYamDao?.let { updateRecyclerWithData(it) }
                if (!normalizedCategory.isNullOrBlank()) {
                    if (normalizedCategory == "marriage") {
                        showMarriageFormBottomSheet()
                    } else {
                        showAuspiciousBottomSheet(normalizedCategory)
                    }
                }
            },
            onLockedCategoryClick = { category ->
                if (!hasRengyamAccess) {
                    showRengyamUnlockDialog(category)
                }
            },
            showInauspiciousInfo = showTopHeaderInauspiciousInfo
        )
        syncFrozenHeader()
    }

    private fun syncFrozenHeader() {
        if (!isAdded) return
        frozenTopHeaderHolder?.setRengyamAccess(hasRengyamAccess)
        frozenTopHeaderHolder?.bind(headerData)
    }

    private fun fetchMemberAndShowKalagni() {
        val user = UserContextManager.userX(requireContext())
        val memberId = user?.userId ?: return
        val apiService = RetrofitClient.instance.create(ApiService::class.java)

        beginLoading()
        apiService.getMemberInfo(memberId).enqueue(object : Callback<Serverx> {
            override fun onResponse(call: Call<Serverx>, response: Response<Serverx>) {
                try {
                if (!response.isSuccessful) return
                val serverData = response.body() ?: return
                syncRengyamAccessFromServer(serverData)
                val latestAccess = computeRengyamAccess()
                if (latestAccess != hasRengyamAccess) {
                    hasRengyamAccess = latestAccess
                    updateCalendarScrollLock()
                    setupAdapterList()
                    if (latestAccess && cachedLengYamDao == null) {
                        initRecyclerView()
                        fetchAuspiciousForSelectedMonth()
                    } else if (!latestAccess && !isLoggedInUser) {
                        renderLockedCalendarPlaceholder()
                    }
                }

                val memberData = serverData.userx ?: return
                val memberName = memberData.realName ?: memberData.username ?: ""
                headerData = headerData.copy(userName = memberName)
                updateRecyclerHeader()

                val rawBirthDay = memberData.birthDay
                if (!rawBirthDay.isNullOrEmpty() && rawBirthDay.contains("-")) {
                    try {
                        val bDate = parseBirthLocalDate(rawBirthDay) ?: return
                        val bNum = calculateOutfitBirthDayNumber(bDate, memberData.sHour)
                        val ageYang = PersonContextManager.ageYang(bDate.year, bDate.monthOfYear, bDate.dayOfMonth)
                            ?: (memberData.ageYear + 1)
                        val dayNames = mapOf(1 to "อาทิตย์", 2 to "จันทร์", 3 to "อังคาร", 4 to "พุธ", 5 to "พฤหัสบดี", 6 to "ศุกร์", 7 to "เสาร์", 8 to "พุธ (กลางคืน)")
                        val birthDayThai = dayNames[bNum] ?: "อาทิตย์"
                        memberBirthDate = bDate
                        memberBirthDayNumber = bNum
                        memberBirthDayThai = birthDayThai
                        headerData = headerData.copy(
                            taksaResult = null,
                            userAge = ageYang,
                            birthSourceDay = birthDayThai
                        )
                        updateRecyclerHeader()
                        cachedLengYamDao?.let { updateRecyclerWithData(it) }

                        val currentDayEng = ViewModelHelper.resolveOutfitCurrentDayEng(DateTime.now())
                        val reqBody = OutfitMiracleColorSetsRequest(
                            currentDayName = ViewModelHelper.dayEngToOutfitDayName(currentDayEng),
                            birthDayName = birthDayThai,
                            ageYears = ageYang
                        )
                        beginLoading()
                        apiService.getOutfitMiracleColorSets(reqBody).enqueue(object : Callback<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse> {
                            override fun onResponse(
                                call: Call<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse>,
                                response: Response<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse>
                            ) {
                                try {
                                if (!response.isSuccessful) return
                                val payload = response.body()
                                val inauspicious = payload?.inauspiciousSets ?: emptyList()
                                val todaySet = inauspicious.getOrNull(0)
                                val birthSet = inauspicious.getOrNull(1)
                                val ageSet = inauspicious.getOrNull(2)
                                val todayDay = todaySet?.day ?: headerData.kalagniToday
                                val birthDay = birthSet?.day ?: headerData.kalagniBirth
                                val ageDay = ageSet?.day ?: headerData.kalagniAge
                                val todayDays = normalizeKalagniDays(todaySet?.dualDays, todayDay)
                                val birthDays = normalizeKalagniDays(birthSet?.dualDays, birthDay)
                                val ageDays = normalizeKalagniDays(null, ageDay)
                                headerData = rebuildHeaderTags(headerData.copy(
                                    kalagniToday = todayDay,
                                    kalagniBirth = birthDay,
                                    kalagniAge = ageDay,
                                    kalagniTodayDays = todayDays,
                                    kalagniBirthDays = birthDays,
                                    kalagniAgeDays = ageDays,
                                    kalagniBirthDualText = formatDualDaysForDisplay(birthSet?.dualDays, birthDay),
                                    kalagniAgeDualText = ageDay,
                                    todaySourceDay = payload?.currentDay ?: ViewModelHelper.dayEngToOutfitDayName(currentDayEng),
                                    userAge = payload?.age ?: ageYang
                                ))
                                refreshDynamicKalagniHeaderForViewDate()
                                updateRecyclerHeader()
                                cachedLengYamDao?.let { updateRecyclerWithData(it) }
                                } finally {
                                    endLoading()
                                }
                            }
                            override fun onFailure(call: Call<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse>, t: Throwable) {
                                endLoading()
                            }
                        })
                    } catch(e: Exception) {}
                }
                } finally {
                    endLoading()
                }
            }
            override fun onFailure(call: Call<Serverx>, t: Throwable) {
                endLoading()
            }
        })
    }

    private fun formatDualDaysForDisplay(days: List<String>?, fallback: String?): String {
        val filtered = days?.filter { it.isNotBlank() } ?: emptyList()
        if (filtered.isNotEmpty()) {
            return filtered.joinToString(", ")
        }
        return fallback ?: "-"
    }

    private fun normalizeKalagniDays(days: List<String>?, fallback: String?): List<String> {
        val cleaned = days.orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (cleaned.isNotEmpty()) return cleaned
        return listOfNotNull(fallback?.trim()).filter { it.isNotBlank() }
    }

    private fun normalizeKalagniDayName(value: String): String {
        return value.replace("วัน", "").replace(" ", "").trim()
    }

    private fun isSameKalagniDay(candidate: String, currentDay: String): Boolean {
        val cRaw = normalizeKalagniDayName(candidate)
        val dRaw = normalizeKalagniDayName(currentDay)

        // Strip "กลางคืน" and parentheses to get the base day name for comparison.
        // The system uses 06:00 as the day boundary, so "พุธกลางคืน" (18:00-06:00)
        // is still WITHIN Wednesday — match the same base day, not the next day.
        fun stripNight(s: String) = s
            .replace("กลางคืน", "")
            .replace("(", "")
            .replace(")", "")
            .trim()

        val cBase = stripNight(cRaw)
        val dBase = stripNight(dRaw)
        return cBase == dBase
    }

    private fun parseBirthLocalDate(rawBirthDay: String): org.joda.time.LocalDate? {
        val text = rawBirthDay.trim()
        if (text.length >= 10 && text[4] == '-' && text[7] == '-') {
            return try {
                org.joda.time.LocalDate.parse(text.substring(0, 10))
            } catch (e: Exception) {
                null
            }
        }
        return try {
            org.joda.time.LocalDate.parse(text)
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateOutfitBirthDayNumber(bDate: org.joda.time.LocalDate, sHour: Int): Int {
        val calendarDay = bDate.dayOfWeek
        var mainDay = calendarDay
        if (sHour < 6) {
            mainDay = if (calendarDay == 1) 7 else calendarDay - 1
        }
        var bNum = when (mainDay) {
            7 -> 1
            1 -> 2
            2 -> 3
            3 -> 4
            4 -> 5
            5 -> 6
            6 -> 7
            else -> 1
        }
        if ((calendarDay == 3 && sHour >= 18) || (calendarDay == 4 && sHour < 6)) {
            bNum = 8
        }
        return bNum
    }

    private fun loadMarriagePersistence() {
        val prefs = context?.getSharedPreferences(RENGYAM_USAGE_PREFS, android.content.Context.MODE_PRIVATE) ?: return
        val gDateStr = prefs.getString(PREF_MARRIAGE_GROOM_DATE, null)
        val gTimeStr = prefs.getString(PREF_MARRIAGE_GROOM_TIME, null)
        val bDateStr = prefs.getString(PREF_MARRIAGE_BRIDE_DATE, null)
        val bTimeStr = prefs.getString(PREF_MARRIAGE_BRIDE_TIME, null)
        val gProvinceStr = prefs.getString(PREF_MARRIAGE_GROOM_PROVINCE, null)
        val bProvinceStr = prefs.getString(PREF_MARRIAGE_BRIDE_PROVINCE, null)

        if (gDateStr != null) lastGroomDate = try { org.joda.time.LocalDate.parse(gDateStr) } catch(e: Exception) { null }
        lastGroomTime = gTimeStr
        if (bDateStr != null) lastBrideDate = try { org.joda.time.LocalDate.parse(bDateStr) } catch(e: Exception) { null }
        lastBrideTime = bTimeStr
        lastGroomProvince = gProvinceStr
        lastBrideProvince = bProvinceStr
    }

    private fun saveMarriagePersistence() {
        val prefs = context?.getSharedPreferences(RENGYAM_USAGE_PREFS, android.content.Context.MODE_PRIVATE) ?: return
        prefs.edit().apply {
            putString(PREF_MARRIAGE_GROOM_DATE, lastGroomDate?.toString())
            putString(PREF_MARRIAGE_GROOM_TIME, lastGroomTime)
            putString(PREF_MARRIAGE_BRIDE_DATE, lastBrideDate?.toString())
            putString(PREF_MARRIAGE_BRIDE_TIME, lastBrideTime)
            putString(PREF_MARRIAGE_GROOM_PROVINCE, lastGroomProvince)
            putString(PREF_MARRIAGE_BRIDE_PROVINCE, lastBrideProvince)
            apply()
        }
    }

    private fun showMarriageFormBottomSheet() {
        val context = context ?: return
        val dialog = BottomSheetDialog(context, R.style.CustomBottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_marriage_form, null)
        dialog.setContentView(view)
        view.findViewById<TextView>(R.id.txt_marriage_form_note)?.let {
            it.text = MARRIAGE_FORM_REFERENCE_NOTE
        }

        val monthNames = arrayOf("ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.", "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.")

        val thaiMonthsFull = arrayOf("มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน", "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม")
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val currentYearBE = currentYear + 543
        val fullYears = (currentYearBE - 80..currentYearBE).toList()
        val years = fullYears.map { it.toString() }.toTypedArray()
        val days = (1..31).map { it.toString() }.toTypedArray()
        val hours = (0..23).map { String.format("%02d", it) }.toTypedArray()
        val minutes = (0..59).map { String.format("%02d", it) }.toTypedArray()

        fun createSimpleAdapter(data: Array<String>) = ArrayAdapter(context, android.R.layout.simple_spinner_item, data).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // --- Groom Logic ---
        var groomDate: org.joda.time.LocalDate? = lastGroomDate
        var groomTime: String? = lastGroomTime
        var groomProvince: String? = lastGroomProvince

        val spinnerGroomDay = view.findViewById<Spinner>(R.id.spinner_groom_day)
        val spinnerGroomMonth = view.findViewById<Spinner>(R.id.spinner_groom_month)
        val spinnerGroomYear = view.findViewById<Spinner>(R.id.spinner_groom_year)
        val spinnerGroomHour = view.findViewById<Spinner>(R.id.spinner_groom_hour)
        val spinnerGroomMinute = view.findViewById<Spinner>(R.id.spinner_groom_minute)
        val spinnerGroomProvince = view.findViewById<Spinner>(R.id.spinner_groom_province)
        val txtGroomKalagni = view.findViewById<TextView>(R.id.txt_groom_kalagni)
        val layoutGroomKalagni = view.findViewById<View>(R.id.layout_groom_kalagni)

        spinnerGroomDay.adapter = createSimpleAdapter(days)
        spinnerGroomMonth.adapter = createSimpleAdapter(monthNames)
        spinnerGroomYear.adapter = createSimpleAdapter(years)
        spinnerGroomHour.adapter = createSimpleAdapter(hours)
        spinnerGroomMinute.adapter = createSimpleAdapter(minutes)

        fun updateGroomFromSpinners() {
            val d = spinnerGroomDay.selectedItemPosition + 1
            val m = spinnerGroomMonth.selectedItemPosition + 1
            val yFull = fullYears[spinnerGroomYear.selectedItemPosition]
            val y = yFull - 543
            val h = spinnerGroomHour.selectedItem as String
            val min = spinnerGroomMinute.selectedItem as String
            
            try {
                groomDate = org.joda.time.LocalDate(y, m, d)
                groomTime = "$h:$min น."
                updateMarriageKalagni(groomDate, groomTime, txtGroomKalagni, layoutGroomKalagni)
            } catch (e: Exception) {}
        }

        val groomDateListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) = updateGroomFromSpinners()
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        spinnerGroomDay.onItemSelectedListener = groomDateListener
        spinnerGroomMonth.onItemSelectedListener = groomDateListener
        spinnerGroomYear.onItemSelectedListener = groomDateListener
        spinnerGroomHour.onItemSelectedListener = groomDateListener
        spinnerGroomMinute.onItemSelectedListener = groomDateListener

        // Pre-select defaults (Groom)
        val defaultDate = org.joda.time.LocalDate.now().minusYears(30)
        val dGroom = groomDate ?: defaultDate
        spinnerGroomDay.setSelection(dGroom.dayOfMonth - 1)
        spinnerGroomMonth.setSelection(dGroom.monthOfYear - 1)
        val yIdxG = fullYears.indexOf(dGroom.year + 543)
        if (yIdxG >= 0) spinnerGroomYear.setSelection(yIdxG)

        groomTime?.let {
            val p = it.replace(" น.", "").split(":")
            val hIdx = hours.indexOf(p.getOrNull(0) ?: "12")
            if (hIdx >= 0) spinnerGroomHour.setSelection(hIdx)
            val mIdx = minutes.indexOf(p.getOrNull(1) ?: "00")
            if (mIdx >= 0) spinnerGroomMinute.setSelection(mIdx)
        } ?: run {
            spinnerGroomHour.setSelection(12)
            spinnerGroomMinute.setSelection(0)
        }

        val provinces = resources.getStringArray(R.array.province_array).toList()
        val provinceAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, provinces).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerGroomProvince.adapter = provinceAdapter
        val groomProvinceIndex = provinces.indexOf(groomProvince).takeIf { it >= 0 } ?: 0
        spinnerGroomProvince.setSelection(groomProvinceIndex)
        spinnerGroomProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                groomProvince = provinces.getOrNull(position)?.takeUnless { it == "จังหวัด" }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        // --- Bride Logic ---
        var brideDate: org.joda.time.LocalDate? = lastBrideDate
        var brideTime: String? = lastBrideTime
        var brideProvince: String? = lastBrideProvince

        val spinnerBrideDay = view.findViewById<Spinner>(R.id.spinner_bride_day)
        val spinnerBrideMonth = view.findViewById<Spinner>(R.id.spinner_bride_month)
        val spinnerBrideYear = view.findViewById<Spinner>(R.id.spinner_bride_year)
        val spinnerBrideHour = view.findViewById<Spinner>(R.id.spinner_bride_hour)
        val spinnerBrideMinute = view.findViewById<Spinner>(R.id.spinner_bride_minute)
        val spinnerBrideProvince = view.findViewById<Spinner>(R.id.spinner_bride_province)
        val txtBrideKalagni = view.findViewById<TextView>(R.id.txt_bride_kalagni)
        val layoutBrideKalagni = view.findViewById<View>(R.id.layout_bride_kalagni)

        spinnerBrideDay.adapter = createSimpleAdapter(days)
        spinnerBrideMonth.adapter = createSimpleAdapter(monthNames)
        spinnerBrideYear.adapter = createSimpleAdapter(years)
        spinnerBrideHour.adapter = createSimpleAdapter(hours)
        spinnerBrideMinute.adapter = createSimpleAdapter(minutes)

        fun updateBrideFromSpinners() {
            val d = spinnerBrideDay.selectedItemPosition + 1
            val m = spinnerBrideMonth.selectedItemPosition + 1
            val yFull = fullYears[spinnerBrideYear.selectedItemPosition]
            val y = yFull - 543
            val h = spinnerBrideHour.selectedItem as String
            val min = spinnerBrideMinute.selectedItem as String
            
            try {
                brideDate = org.joda.time.LocalDate(y, m, d)
                brideTime = "$h:$min น."
                updateMarriageKalagni(brideDate, brideTime, txtBrideKalagni, layoutBrideKalagni)
            } catch (e: Exception) {}
        }

        val brideDateListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) = updateBrideFromSpinners()
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        spinnerBrideDay.onItemSelectedListener = brideDateListener
        spinnerBrideMonth.onItemSelectedListener = brideDateListener
        spinnerBrideYear.onItemSelectedListener = brideDateListener
        spinnerBrideHour.onItemSelectedListener = brideDateListener
        spinnerBrideMinute.onItemSelectedListener = brideDateListener

        // Pre-select defaults (Bride)
        val dBride = brideDate ?: defaultDate
        spinnerBrideDay.setSelection(dBride.dayOfMonth - 1)
        spinnerBrideMonth.setSelection(dBride.monthOfYear - 1)
        val yIdxB = fullYears.indexOf(dBride.year + 543)
        if (yIdxB >= 0) spinnerBrideYear.setSelection(yIdxB)

        brideTime?.let {
            val p = it.replace(" น.", "").split(":")
            val hIdx = hours.indexOf(p.getOrNull(0) ?: "12")
            if (hIdx >= 0) spinnerBrideHour.setSelection(hIdx)
            val mIdx = minutes.indexOf(p.getOrNull(1) ?: "00")
            if (mIdx >= 0) spinnerBrideMinute.setSelection(mIdx)
        } ?: run {
            spinnerBrideHour.setSelection(12)
            spinnerBrideMinute.setSelection(0)
        }

        spinnerBrideProvince.adapter = provinceAdapter
        val brideProvinceIndex = provinces.indexOf(brideProvince).takeIf { it >= 0 } ?: 0
        spinnerBrideProvince.setSelection(brideProvinceIndex)
        spinnerBrideProvince.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                brideProvince = provinces.getOrNull(position)?.takeUnless { it == "จังหวัด" }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        // Submit Logic
        view.findViewById<android.widget.Button>(R.id.btn_find_marriage_date).setOnClickListener {
            if (groomDate == null || groomTime == null || brideDate == null || brideTime == null) {
                Toast.makeText(context, "กรุณาสรุปวันและเวลาเกิดให้ครบถ้วน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            
            // Persist for next time
            lastGroomDate = groomDate
            lastGroomTime = groomTime
            lastBrideDate = brideDate
            lastBrideTime = brideTime
            lastGroomProvince = groomProvince
            lastBrideProvince = brideProvince
            saveMarriagePersistence()
            
            performMarriageDateSearch(groomDate!!, groomTime!!, brideDate!!, brideTime!!)
        }

        dialog.behavior.apply {
            state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            peekHeight = resources.displayMetrics.heightPixels
            skipCollapsed = true
        }
        dialog.show()
    }

    private fun performMarriageDateSearch(
        groomDate: org.joda.time.LocalDate,
        groomTime: String,
        brideDate: org.joda.time.LocalDate,
        brideTime: String
    ) {
        val pd = android.app.ProgressDialog(context).apply {
            setMessage("กำลังค้นหาฤกษ์แต่งงานที่เหมาะสม...")
            setCancelable(false)
            show()
        }

        // 1. Calculate birth day basis
        val gParts = groomTime.replace(" น.", "").split(":")
        val gBNum = calculateOutfitBirthDayNumber(groomDate, gParts[0].toInt())

        val bParts = brideTime.replace(" น.", "").split(":")
        val bBNum = calculateOutfitBirthDayNumber(brideDate, bParts[0].toInt())
        val foundDates = mutableListOf<String>()
        val maxMonths = 36
        val today = org.joda.time.LocalDate.now()
        val startM = if (currentViewDate.toLocalDate().isBefore(today.withDayOfMonth(1))) today.withDayOfMonth(1) else currentViewDate.toLocalDate().withDayOfMonth(1)

        fun searchNextMonth(searchDate: org.joda.time.LocalDate, monthsLeft: Int) {
            if (monthsLeft <= 0) {
                pd.dismiss()
                if (foundDates.isEmpty()) {
                    Toast.makeText(context, "ไม่พบฤกษ์ที่เหมาะสมภายใน 3 ปี", Toast.LENGTH_LONG).show()
                } else {
                    headerData = headerData.copy(marriageDays = foundDates.distinct())
                    showAuspiciousBottomSheet("marriage")
                }
                return
            }
            
            val y = searchDate.year
            val m = searchDate.monthOfYear
            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            apiService.getLengYam(y, m).enqueue(object : Callback<LengYamDao> {
                override fun onResponse(call: Call<LengYamDao>, response: Response<LengYamDao>) {
                    if (response.isSuccessful && response.body() != null) {
                        val backendList = response.body()?.wanPras.orEmpty()
                        val lastDay = searchDate.dayOfMonth().withMaximumValue().dayOfMonth
                        
                        for (i in 1..lastDay) {
                            val candidate = org.joda.time.LocalDate(y, m, i)
                            if (candidate.isBefore(today)) continue 
                            
                            val dtStr = String.format(Locale.US, "%04d-%02d-%02d", y, m, i)
                            val w = Wanpra(wanpraId = null, wanpraDate = dtStr, isWanpra = "0", isTongchai = "0", isAtipbadee = "0", isKating = "0")
                            applyLocalAuspiciousData(w, candidate)
                            
                            val bw = backendList.find { it.wanpraDate == dtStr }
                            if (bw != null) {
                                w.isUbath = bw.isUbath ?: w.isUbath
                                w.isLokawinat = bw.isLokawinat ?: w.isLokawinat
                                w.warningTags = bw.warningTags ?: w.warningTags
                                w.isRiangMon = bw.isRiangMon ?: w.isRiangMon
                                w.isTongchai = if (bw.isTongchai == "1" || bw.isTongchai == "true") "1" else "0"
                                w.isAtipbadee = if (bw.isAtipbadee == "1" || bw.isAtipbadee == "true") "1" else "0"
                                w.displayTags = bw.displayTags
                                w.displayTagsPrioritized = bw.displayTagsPrioritized
                                w.calendarDisplayTags = bw.calendarDisplayTags
                                w.kalTags = bw.kalTags
                                w.dithiTags = bw.dithiTags
                                w.dayTypeTags = bw.dayTypeTags
                                w.tagDetails = bw.tagDetails
                                w.isLoy = bw.isLoy
                                w.isFu = bw.isFu
                                w.isJom = bw.isJom
                                w.isAmmarit = bw.isAmmarit ?: w.isAmmarit
                                w.isMahaSittichok = bw.isMahaSittichok ?: w.isMahaSittichok
                                w.isSittichok = bw.isSittichok ?: w.isSittichok
                                w.isRachaChok = bw.isRachaChok ?: w.isRachaChok
                                w.isChaiChok = bw.isChaiChok ?: w.isChaiChok
                            }
                            
                            val cDayName = getThaiAstrologicalDayName(candidate, "12:00 น.")
                            val passesPreferredWindow = passesMarriagePreferredWindow(candidate, cDayName)
                            val groomRule = buildMarriagePersonRule(groomDate, gBNum, candidate)
                            val brideRule = buildMarriagePersonRule(brideDate, bBNum, candidate)
                            val isPersonalInauspicious = isMarriagePersonalInauspiciousDay(cDayName, groomRule, brideRule)

                            if (passesPreferredWindow && !isPersonalInauspicious) {
                                if (isEligibleForCategory(w, "marriage")) {
                                    dateToWanpraMap[dtStr] = w
                                    if (!foundDates.contains(dtStr)) foundDates.add(dtStr)
                                }
                            }
                        }
                    }
                    searchNextMonth(searchDate.plusMonths(1), monthsLeft - 1)
                }
                override fun onFailure(call: Call<LengYamDao>, t: Throwable) {
                    searchNextMonth(searchDate.plusMonths(1), monthsLeft - 1)
                }
            })
        }
        val dayNamesLong = mapOf(1 to "อาทิตย์", 2 to "จันทร์", 3 to "อังคาร", 4 to "พุธ (กลางวัน)", 5 to "พฤหัสบดี", 6 to "ศุกร์", 7 to "เสาร์", 8 to "พุธ (กลางคืน)")
        val gLong = dayNamesLong[gBNum] ?: gBNum.toString()
        val bLong = dayNamesLong[bBNum] ?: bBNum.toString()
        
        val ssb = android.text.SpannableStringBuilder()
        val colorAuspicious = android.graphics.Color.parseColor("#F57C00") // Orange
        val colorInauspicious = android.graphics.Color.parseColor("#D32F2F") // Red
        
        fun appendHighlight(text: String, color: Int = colorAuspicious) {
            val start = ssb.length
            ssb.append(text)
            ssb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, ssb.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.setSpan(android.text.style.ForegroundColorSpan(color), start, ssb.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val header = "หมายเหตุการคำนวณ:\n"
        val headerStart = ssb.length
        ssb.append(header)
        ssb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), headerStart, ssb.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        ssb.append("• วิเคราะห์จากพื้นดวงบ่าว-สาวคู่นี้ (เจ้าบ่าว: ")
        appendHighlight("วัน$gLong")
        ssb.append(", เจ้าสาว: ")
        appendHighlight("วัน$bLong")
        ssb.append(")\n")
        ssb.append("• ค้นหาฤกษ์ล่วงหน้า ")
        appendHighlight("3 ปี")
        ssb.append(" และแสดงผลทุกวันที่ผ่านเงื่อนไข\n")
        ssb.append("• จำกัดเฉพาะเดือนโหราศาสตร์ ")
        appendHighlight("2, 4, 6, 9, 12")
        ssb.append(" เท่านั้น")
        ssb.append("\n• ระบบใช้ ")
        appendHighlight("เลขเดือนจันทรคติ/โหราศาสตร์")
        ssb.append(" ไม่ใช่เดือนสากล เช่น ")
        appendHighlight("แรม 10 ค่ำ เดือน 3 = กุมภาพันธ์")
        ssb.append(" และวัน ")
        appendHighlight("อาทิตย์, จันทร์, ศุกร์, พฤหัสบดี")
        ssb.append("\n• วัน ")
        appendHighlight("พุธ", colorAuspicious)
        ssb.append(" อนุโลมได้ แต่ระบบจะแสดงคำเตือนกำกับ")
        ssb.append("\n• วัน ")
        appendHighlight("พฤหัสบดี", colorAuspicious)
        ssb.append(" แสดงคำเตือนว่า ")
        appendHighlight("แต่งได้แต่ให้แก้เคล็ดโดยห้าม...กัน และให้สวมแหวนแต่งงานก่อนวันแต่งจริง")
        ssb.append("\n• ตัดวันจันทรคติ ")
        appendHighlight("ขึ้น 7 ค่ำ / แรม 7 ค่ำ", colorInauspicious)
        ssb.append("\n• ตัด ")
        appendHighlight("วันพระ", colorInauspicious)
        ssb.append("\n• ระบบคัดกรองวันมงคลที่ปรากฏในปฏิทิน และ ")
        appendHighlight("'ลุล่วงและเป็นไปด้วยดี'")

        ssb.append(" สำหรับบ่าวสาวคู่นี้โดยเฉพาะ")

        
        marriageSearchNote = ssb
        // Wait, marriageSearchNote is a String? property. We need it to be CharSequence or store the SSB.
        // Let's update the property type or re-apply spans in showAuspiciousBottomSheet.
        // For simplicity, let's change marriageSearchNote to CharSequence? 
        
        searchNextMonth(startM, maxMonths)
    }

    /**
     * Calculates the Thai astrological day name based on Gregorian date and time.
     * Boundary is 06:00 AM. Includes "พุธกลางคืน" (Wed Night) if 18:00 - 05:59.
     */
    private fun getThaiAstrologicalDayName(date: org.joda.time.LocalDate, timeStr: String?): String {
        if (timeStr == null) return ""
        
        // Parse time (format: "HH:mm น.")
        val parts = timeStr.replace(" น.", "").split(":")
        val hour = parts[0].toInt()
        
        // Astrological Day Boundary: 06:00 AM
        // If before 06:00, use the previous day
        var effectiveDate = date
        if (hour < 6) {
            effectiveDate = date.minusDays(1)
        }
        
        val dayOfWeek = effectiveDate.dayOfWeek // 1=Mon, 7=Sun
        val dayNames = arrayOf("", "จันทร์", "อังคาร", "พุธ", "พฤหัสบดี", "ศุกร์", "เสาร์", "อาทิตย์")
        val baseName = dayNames[dayOfWeek]
        
        // Handle Wednesday Night (พุธกลางคืน)
        // If it's Wednesday and time is 18:00 - 05:59
        if (dayOfWeek == 3) { // Wednesday
            if (hour >= 18 || hour < 6) {
                return "พุธ (กลางคืน)"
            } else {
                return "พุธ (กลางวัน)"
            }
        }
        
        return baseName
    }

    /**
     * Calculates and updates the UI with Kalagni Birth and Age days for the marriage form.
     */
    private fun updateMarriageKalagni(date: org.joda.time.LocalDate?, timeStr: String?, txtView: TextView?, layoutView: View?) {
        if (date == null || timeStr == null || txtView == null || layoutView == null) {
            layoutView?.visibility = View.GONE
            return
        }
        // 1. Calculate Age Next (Thai Standard)
        val ageNext = calculateThaiAgeNextAtDate(date, org.joda.time.LocalDate.now())

        // 2. Map Birth Day Number
        val parts = timeStr.replace(" น.", "").split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val bNum = calculateOutfitBirthDayNumber(date, hour)
        val thaiBirthDay = getThaiAstrologicalDayName(date, timeStr)

        // 3. Get Fixed Inauspicious Days (based on birth day)
        val fixedInauspiciousMap = getMarriageFixedBadMap()
        val fixedNames = fixedInauspiciousMap[bNum]?.joinToString(", วัน") ?: ""

        // 4. Calculate Dynamic Inauspicious Age Day (Kalagni Age)
        val taksa = ThaiAstrologyLib.calculateTaksa(date, bNum, org.joda.time.LocalDate.now())
        val kalagniAgeNum = ThaiAstrologyLib.BOARD[taksa.badPos.r][taksa.badPos.c]
        var kalagniAgeName = getMarriageKalagniDayName(kalagniAgeNum) ?: "วัน$kalagniAgeNum"
        
        if (bNum == 1 && ageNext == 45) {
            kalagniAgeName = "พุธ (กลางคืน)"
        }

        val birthPrefix = "เกิด"
        val birthDayLabel = "วัน$thaiBirthDay"
        val infoText = "$birthPrefix $birthDayLabel • อัปมงคลวันเกิด: วัน$fixedNames\n" +
            "• อัปมงคลอายุย่าง ($ageNext ปี): วัน$kalagniAgeName"
            
        val spannable = android.text.SpannableString(infoText)
        val spanStart = birthPrefix.length + 1
        val spanEnd = spanStart + birthDayLabel.length

        spannable.setSpan(
            android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#039BE5")),
            spanStart,
            spanEnd,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            spanStart,
            spanEnd,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        
        txtView.text = spannable
        layoutView.visibility = View.VISIBLE
    }

    /**
     * Custom Thai date picker dialog using NumberPicker spinners.
     * Displays year in Buddhist Era (พ.ศ.) format.
     */
    private fun showThaiDatePickerDialog(
        title: String,
        currentDate: org.joda.time.LocalDate?,
        onDateSelected: (org.joda.time.LocalDate) -> Unit
    ) {
        val ctx = context ?: return
        val pickerDialog = android.app.AlertDialog.Builder(ctx)
        val pickerView = layoutInflater.inflate(R.layout.dialog_thai_date_picker, null)

        val titleTv = pickerView.findViewById<TextView>(R.id.txt_picker_title)
        titleTv.text = title

        val dayPicker = pickerView.findViewById<android.widget.NumberPicker>(R.id.picker_day)
        val monthPicker = pickerView.findViewById<android.widget.NumberPicker>(R.id.picker_month)
        val yearPicker = pickerView.findViewById<android.widget.NumberPicker>(R.id.picker_year)

        val thaiMonths = arrayOf("มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
            "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม")

        // Month picker: 1-12 with Thai month names
        monthPicker.minValue = 0
        monthPicker.maxValue = 11
        monthPicker.displayedValues = thaiMonths
        monthPicker.wrapSelectorWheel = true

        // Year picker: พ.ศ. range (2443-2569 = ค.ศ. 1900-2026)
        val currentCEYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val minBEYear = 2443  // ค.ศ. 1900
        val maxBEYear = currentCEYear + 543
        val yearCount = maxBEYear - minBEYear + 1
        val yearDisplayValues = Array(yearCount) { i -> (minBEYear + i).toString() }
        yearPicker.minValue = 0
        yearPicker.maxValue = yearCount - 1
        yearPicker.displayedValues = yearDisplayValues
        yearPicker.wrapSelectorWheel = false

        // Day picker: 1-31 (updated dynamically based on month/year)
        dayPicker.minValue = 1
        dayPicker.maxValue = 31
        dayPicker.wrapSelectorWheel = true

        // Set initial values
        val initDate = currentDate ?: org.joda.time.LocalDate.now().minusYears(20)
        monthPicker.value = initDate.monthOfYear - 1
        yearPicker.value = (initDate.year + 543) - minBEYear
        dayPicker.value = initDate.dayOfMonth

        // Update day range when month or year changes
        val updateDayRange = {
            val selectedMonth = monthPicker.value + 1
            val selectedBEYear = minBEYear + yearPicker.value
            val selectedCEYear = selectedBEYear - 543
            val maxDay = try {
                org.joda.time.LocalDate(selectedCEYear, selectedMonth, 1).dayOfMonth().maximumValue
            } catch (e: Exception) { 28 }
            val oldDay = dayPicker.value
            dayPicker.maxValue = maxDay
            if (oldDay > maxDay) dayPicker.value = maxDay
        }

        monthPicker.setOnValueChangedListener { _, _, _ -> updateDayRange() }
        yearPicker.setOnValueChangedListener { _, _, _ -> updateDayRange() }
        updateDayRange()

        val alertDialog = pickerDialog.setView(pickerView).create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        pickerView.findViewById<View>(R.id.btn_picker_cancel).setOnClickListener {
            alertDialog.dismiss()
        }

        pickerView.findViewById<View>(R.id.btn_picker_confirm).setOnClickListener {
            val selectedBEYear = minBEYear + yearPicker.value
            val selectedCEYear = selectedBEYear - 543
            val selectedMonth = monthPicker.value + 1
            val selectedDay = dayPicker.value
            try {
                val date = org.joda.time.LocalDate(selectedCEYear, selectedMonth, selectedDay)
                onDateSelected(date)
            } catch (e: Exception) {
                Toast.makeText(ctx, "วันที่ไม่ถูกต้อง กรุณาเลือกใหม่", Toast.LENGTH_SHORT).show()
            }
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun showAuspiciousBottomSheet(category: String) {
        val context = context ?: return
        val dialog = BottomSheetDialog(context, R.style.CustomBottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_auspicious_days, null)
        dialog.setContentView(view)
        dialog.behavior.peekHeight = resources.displayMetrics.heightPixels
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.isFitToContents = true
        dialog.behavior.skipCollapsed = true
        
        val title = view.findViewById<TextView>(R.id.txt_bottom_sheet_title)
        val icon = view.findViewById<android.widget.ImageView>(R.id.img_bottom_sheet_icon)
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_auspicious_list)
        val btnEdit = view.findViewById<TextView>(R.id.btn_edit_marriage_info)
        
        if (category == "marriage") {
            btnEdit.visibility = View.VISIBLE
            btnEdit.setOnClickListener {
                dialog.dismiss()
                showMarriageFormBottomSheet()
            }
        } else {
            btnEdit.visibility = View.GONE
        }
        
        val (categoryTitle, iconRes, targetDates) = when(category) {
            "marriage" -> Triple("ฤกษ์แต่งงาน", R.drawable.valentines_day, headerData.marriageDays)
            "surgery" -> Triple("ฤกษ์ผ่าตัด", R.drawable.ic_plus_red, headerData.surgeryDays)
            "house" -> Triple("ฤกษ์ขึ้นบ้านใหม่", R.drawable.house_2, headerData.houseDays)
            "ordination" -> Triple("ฤกษ์บวช", R.drawable.buddha, headerData.ordinationDays)
            "deordination" -> Triple("ฤกษ์สึกพระ", R.drawable.logout, headerData.deordinationDays)
            "car" -> Triple("ฤกษ์ออกรถ", R.drawable.automobile, headerData.carDays)
            "buy_car" -> Triple("ฤกษ์ซื้อรถ", R.drawable.ic_buy_car, headerData.buyCarDays)
            "childbirth" -> Triple("ฤกษ์คลอด", R.drawable.icon_baby_auspicious, headerData.childbirthDays)
            "shop" -> Triple("ฤกษ์เปิดร้าน", R.drawable.icon_luckycat, headerData.shopDays)
            "build_house" -> Triple("ฤกษ์สร้างบ้าน", R.drawable.icon_home01, headerData.buildHouseDays)
            "move_house" -> Triple("ฤกษ์ย้ายบ้าน", R.drawable.mansion, headerData.moveHouseDays)
            "merit" -> Triple("ฤกษ์ทำบุญ", R.drawable.lotus2, headerData.meritDays)
            "engagement" -> Triple("ฤกษ์สู่ขอ", R.drawable.ic_engagement, headerData.engagementDays)
            "debt" -> Triple("ฤกษ์ทวงหนี้", R.drawable.ic_debt, headerData.debtDays)
            "plant" -> Triple("ฤกษ์เปลี่ยนชื่อ", R.drawable.img_name_skul, headerData.plantDays)
            "spirit_house" -> Triple("ฤกษ์เปลี่ยนเบอร์", R.drawable.smartphone, headerData.spiritHouseDays)
            "travel" -> Triple("ฤกษ์เดินทาง", R.drawable.ic_travel, headerData.travelDays)
            "promotion" -> Triple("ฤกษ์รับตำแหน่ง", R.drawable.ic_promotion, headerData.promotionDays)
            "job" -> Triple("ฤกษ์สมัครงาน", R.drawable.ic_job, headerData.jobDays)
            "business" -> Triple("ฤกษ์เปิดกิจการ", R.drawable.ic_business, headerData.businessDays)
            else -> Triple("วันมงคล", R.drawable.flag, emptyList())
        }
        
        val memberName = headerData.userName?.trim().orEmpty()
        val displayName = if (memberName.isNotEmpty()) "คุณ$memberName" else ""
        title.text = if (displayName.isNotEmpty()) {
            "$categoryTitle $displayName".trim()
        } else {
            categoryTitle
        }
        icon.setImageResource(iconRes)
        
        // Prepare the note if it's the marriage category
        val marriageSearchNoteView = if (category == "marriage" && !marriageSearchNote.isNullOrEmpty()) {
            TextView(context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(
                        (resources.displayMetrics.density * 12).toInt(),
                        (resources.displayMetrics.density * 8).toInt(),
                        (resources.displayMetrics.density * 12).toInt(),
                        (resources.displayMetrics.density * 12).toInt()
                    )
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#E8F5E9"))
                    cornerRadius = (resources.displayMetrics.density * 8).toInt().toFloat()
                }
                setPadding(
                    (resources.displayMetrics.density * 12).toInt(),
                    (resources.displayMetrics.density * 12).toInt(),
                    (resources.displayMetrics.density * 12).toInt(),
                    (resources.displayMetrics.density * 12).toInt()
                )
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#111111"))
                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sarabun_regular)
                text = marriageSearchNote
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    justificationMode = android.text.Layout.JUSTIFICATION_MODE_INTER_WORD
                }
            }
        } else null
        
        if (!targetDates.isNullOrEmpty()) {
            val contentLayout = view.findViewById<android.widget.LinearLayout>(R.id.layout_auspicious_content)
            val inflater = LayoutInflater.from(context)
            
            // Priority: Date map -> filtered list -> reconstruction
            var displayData = targetDates.mapNotNull { dateToWanpraMap[it] }
            if (displayData.size < targetDates.size) {
                 val listItems = fullDataList.filterIsInstance<Wanpra>().filter { targetDates.contains(it.wanpraDate) }
                 if (listItems.size > displayData.size) displayData = listItems
            }
            if (displayData.isEmpty()) {
                displayData = targetDates.map { Wanpra(wanpraId = null, wanpraDate = it, isWanpra = "0") }
            }

            // Sort by auspicious score for marriage category
            if (category == "marriage") {
                displayData = displayData.sortedWith(compareByDescending<Wanpra> { wp ->
                    var score = 0
                    
                    if (wp.isRiangMon) score += 500
                    if (wp.isTongchai == "1" || wp.isTongchai == true) score += 100
                    if (wp.isAtipbadee == "1" || wp.isAtipbadee == true) score += 80
                    if (wp.isSittichok) score += 75
                    if (wp.isChaiChok) score += 75
                    if (wp.isMahaSittichok) score += 70
                    if (wp.isRachaChok) score += 65
                    if (wp.isLoy) score += 50
                    if (wp.isFu) score += 40
                    
                    // Specific check for tags in list if needed
                    val allTags = (wp.displayTags.orEmpty() + wp.displayTagsPrioritized.orEmpty())
                    if (allTags.contains("ปูรณดิถี")) score += 35
                    if (allTags.contains("วันปลอด")) score += 20
                    
                    if (!wp.warningTags.isNullOrEmpty()) score -= 1000
                    score
                }.thenBy { wp ->
                    // Second criteria: earlier date if scores are tied
                    wp.wanpraDate
                })
            }

            // Hide the default recycler and clear container
            recycler.visibility = View.GONE
            contentLayout?.removeAllViews()
            
            val verticalList = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(0, 0, 0, (resources.displayMetrics.density * 24).toInt())
            }
            
            // Note is now added at the very end (bottom) instead of here.

            val tableHeader = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(12, 10, 12, 10)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#FFFFFF"))
                    cornerRadius = 14f
                }
            }
            val headerDate = TextView(context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "วันที่"
                setTextColor(android.graphics.Color.parseColor("#2D5F48"))
                textSize = 14f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sarabun_semibold)
            }
            val headerTags = TextView(context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "ฤกษ์"
                gravity = android.view.Gravity.END
                setTextColor(android.graphics.Color.parseColor("#2D5F48"))
                textSize = 14f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sarabun_semibold)
            }
            tableHeader.addView(headerDate)
            tableHeader.addView(headerTags)
            verticalList.addView(tableHeader)

            // ANCHOR: RengMarries (ฤกษ์วันแต่งงาน)

            displayData.forEach { wp ->
                val itemView = inflater.inflate(R.layout.item_auspicious_table_row, verticalList, false)
                
                // Bind basic data
                val txtDate = itemView.findViewById<TextView>(R.id.txt_date_wanpra)
                val txtLunar = itemView.findViewById<TextView>(R.id.txt_lunar_wanpra)
                val dt = try { formatter.parseDateTime(wp.wanpraDate) } catch(e: Exception) { null }
                if (dt != null) {
                    val dayLabelEng = dt.dayOfWeek().getAsText(java.util.Locale.ENGLISH)
                    val thaiDay = PersonContextManager.toThaiDay(dayLabelEng)
                    val dayRaw = if (thaiDay.isEmpty()) dt.dayOfWeek().asText else thaiDay
                    val day = dayRaw.replace("วัน", "")
                    val dayNum = dt.dayOfMonth().asString
                    val monthFallback = toThaiMonthFull(dt.monthOfYear)
                    val month = toThaiMonthShort(dt.monthOfYear, monthFallback)
                    val year = (dt.year + 543).toString()
                    txtDate.text = "$day $dayNum $month $year"
                    val lunarText = if (!wp.lunarPhase.isNullOrBlank() && !wp.lunarMonth.isNullOrBlank()) {
                        "${wp.lunarPhase} เดือน ${wp.lunarMonth}"
                    } else {
                        val (dithiRaw, mThai) = ThaiAstrologyLib.getThaiLunar(dt.toLocalDate())
                        val phase = if (dithiRaw <= 15) "ขึ้น ${toThaiNum(dithiRaw.toString())} ค่ำ" else "แรม ${toThaiNum((dithiRaw - 15).toString())} ค่ำ"
                        "$phase เดือน ${toThaiNum(mThai.toString())}"
                    }
                    txtLunar?.text = lunarText
                } else {
                    txtDate.text = wp.wanpraDate
                    txtLunar?.text = "-"
                }

                val txtExtraNote = itemView.findViewById<TextView>(R.id.txt_extra_note)
                txtExtraNote?.visibility = View.GONE
                
                // Bind badges
                fun isTrue(v: Any?) = v?.toString()?.lowercase() == "1" || v?.toString()?.lowercase() == "true"
                
                itemView.findViewById<View>(R.id.badge_pra)?.isVisible = isTrue(wp.isWanpra)
                itemView.findViewById<View>(R.id.badge_tongchai)?.isVisible = isTrue(wp.isTongchai)
                itemView.findViewById<View>(R.id.badge_atipbadee)?.isVisible = isTrue(wp.isAtipbadee)
                itemView.findViewById<View>(R.id.badge_sittichok)?.isVisible = wp.isSittichok
                itemView.findViewById<View>(R.id.badge_ammarit)?.isVisible = wp.isAmmarit
                itemView.findViewById<View>(R.id.badge_mahasitti)?.isVisible = wp.isMahaSittichok
                itemView.findViewById<View>(R.id.badge_racha)?.isVisible = wp.isRachaChok
                itemView.findViewById<View>(R.id.badge_chaichok)?.isVisible = wp.isChaiChok
                itemView.findViewById<View>(R.id.badge_fu)?.isVisible = wp.isFu
                itemView.findViewById<View>(R.id.badge_loy)?.isVisible = wp.isLoy
                itemView.findViewById<View>(R.id.badge_riangmon)?.isVisible = wp.isRiangMon
                itemView.findViewById<View>(R.id.badge_safe_day)?.isVisible = isPlainSafeDay(wp)
                itemView.findViewById<View>(R.id.badge_lokawinat)?.isVisible = wp.isLokawinat
                
                itemView.findViewById<View>(R.id.badge_kalagni)?.visibility = if (wp.isKalagni) View.VISIBLE else View.GONE
                if (wp.isKalagni) {
                    itemView.findViewById<TextView>(R.id.badge_kalagni)?.text = "วันอัปมงคล"
                }

                val flexBadges = itemView.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.flex_badges)
                val shownLabels = buildSet {
                    if (isTrue(wp.isWanpra)) add("วันพระ")
                    if (isTrue(wp.isTongchai)) add("วันธงชัย")
                    if (isTrue(wp.isAtipbadee)) add("วันอธิบดี")
                    if (wp.isSittichok) add("สิทธิโชค")
                    if (wp.isAmmarit) {
                        add("อำฤตโชค")
                        add("อมุตโชค")
                    }
                    if (wp.isMahaSittichok) add("มหาสิทธิโชค")
                    if (wp.isRachaChok) add("ราชาโชค")
                    if (wp.isChaiChok) add("ชัยโชค")
                    if (wp.isFu) add("วันฟู")
                    if (wp.isLoy) add("วันลอย")
                    if (wp.isRiangMon) add("ดิถีเรียงหมอน")
                    if (isPlainSafeDay(wp)) add("ปลอด")
                }
                val additionalLabels = collectAdditionalAuspiciousBadgeLabels(wp, shownLabels)
                    .filterNot { category == "marriage" && it == "กระทิงวัน" }
                if (additionalLabels.isNotEmpty()) {
                    additionalLabels.forEach { label ->
                        val badge = TextView(context, null, 0, R.style.AuspiciousBadge).apply {
                            text = if (label == "ปลอด") "วันปลอด" else label
                            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_blue_rounded)
                        }
                        flexBadges?.addView(badge)
                    }
                } else {
                    val fallbackLabel = getFallbackAuspiciousBadgeLabel(wp)
                        ?.takeUnless { category == "marriage" && it == "กระทิงวัน" }
                    if (fallbackLabel != null && !shownLabels.contains(fallbackLabel)) {
                        val badge = TextView(context, null, 0, R.style.AuspiciousBadge).apply {
                            text = fallbackLabel
                            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_blue_rounded)
                        }
                        flexBadges?.addView(badge)
                    }
                }

                // Bind Best Direction & Times Table (Beautiful UI)
                val bestTimesContainer = itemView.findViewById<View>(R.id.layout_best_times_container)
                val txtDirectionGood = itemView.findViewById<TextView>(R.id.txt_direction_good)
                val txtDirectionBad = itemView.findViewById<TextView>(R.id.txt_direction_bad)
                
                if (dt != null) {
                    val dayOfWeekIdx = dt.dayOfWeek // Joda: 1=Mon, 7=Sun
                    val directions = travelDirectionInfoForDay(dayOfWeekIdx)
                    
                    if (directions != null) {
                        bestTimesContainer?.visibility = View.VISIBLE
                        txtDirectionGood?.apply {
                            text = "ทิศมงคล: ${directions.deityDirection}"
                            setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                            background = null
                            visibility = View.VISIBLE
                        }
                        txtDirectionBad?.apply {
                            text = "ทิศห้าม: ${directions.ghostDirection}"
                            setTextColor(android.graphics.Color.parseColor("#C62828"))
                            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                            background = null
                            visibility = View.VISIBLE
                        }
                        
                    } else {
                        bestTimesContainer?.visibility = View.GONE
                        txtDirectionGood?.visibility = View.GONE
                        txtDirectionBad?.visibility = View.GONE
                    }
                } else {
                    bestTimesContainer?.visibility = View.GONE
                    txtDirectionGood?.visibility = View.GONE
                    txtDirectionBad?.visibility = View.GONE
                }

                if (category == "marriage" && dt != null) {
                    val astrologicalDayName = getThaiAstrologicalDayName(dt.toLocalDate(), "12:00 น.")
                    val weekdayWarning = getMarriageWeekdayWarning(astrologicalDayName)
                    if (weekdayWarning != null) {
                        txtExtraNote?.apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = (resources.displayMetrics.density * 6).toInt()
                            }
                            visibility = View.VISIBLE
                            val warningColor = android.graphics.Color.parseColor(
                                if (weekdayWarning != null && astrologicalDayName == "พฤหัสบดี") "#E65100" else "#8D6E63"
                            )
                            val styledText = android.text.SpannableStringBuilder().apply {
                                weekdayWarning?.let {
                                    if (isNotEmpty()) append("\n")
                                    val start = length
                                    append(it)
                                    setSpan(
                                        android.text.style.ForegroundColorSpan(warningColor),
                                        start,
                                        length,
                                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    )
                                }
                            }
                            text = styledText
                            textSize = 12f
                        }
                    }
                }

                val imgCrownBS = itemView.findViewById<android.widget.ImageView>(R.id.img_best_crown)
                val bgBestDayBS = itemView.findViewById<View>(R.id.bg_best_day)
                
                if (wp.isBestDay) {
                    imgCrownBS?.isVisible = true
                    bgBestDayBS?.isVisible = true
                    
                    // Magical Floating Animation
                    imgCrownBS?.clearAnimation()
                    val floatAnim = android.view.animation.TranslateAnimation(
                        0f, 0f, 0f, -15f
                    ).apply {
                        duration = 800
                        repeatMode = android.view.animation.Animation.REVERSE
                        repeatCount = android.view.animation.Animation.INFINITE
                        interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                    }
                    imgCrownBS?.startAnimation(floatAnim)
                } else {
                    imgCrownBS?.isVisible = false
                    bgBestDayBS?.isVisible = false
                    imgCrownBS?.clearAnimation()
                }
                
                itemView.setOnClickListener {
                    dialog.dismiss()
                    val selectedDateStr = wp.wanpraDate
                    selectedDateStr?.let { scrollToDate(it) }
                    showDayTagDetailBottomSheet(
                        selectedDateStr?.let { dateToWanpraMap[it] } ?: wp,
                        onBackToList = { showAuspiciousBottomSheet(category) },
                        category = category
                    )
                }
                
                verticalList.addView(itemView)
            }
            
            // Add the note to the bottom of the list if it exists
            marriageSearchNoteView?.let { verticalList.addView(it) }
            
            val scrollView = androidx.core.widget.NestedScrollView(context).apply {
                 layoutParams = android.widget.LinearLayout.LayoutParams(
                     android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                     android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                 )
                 addView(verticalList)
            }
            contentLayout?.addView(scrollView)
            
        } else {
            Toast.makeText(context, "ไม่พบวันมงคลสำหรับหมวดหมู่นี้ในเดือนนี้", Toast.LENGTH_SHORT).show()
        }
        
        dialog.show()
    }

    private fun scrollToDate(dateStr: String) {
        val index = fullDataList.indexOfFirst { it is Wanpra && it.wanpraDate == dateStr }
        if (index != -1) {
            rengYamBinding.recyclerviewWanpra.scrollToPosition(index)
        }
    }

    private fun categoryDates(category: String): List<String> {
        return when (category) {
            "marriage" -> headerData.marriageDays ?: emptyList()
            "surgery" -> headerData.surgeryDays ?: emptyList()
            "house" -> headerData.houseDays ?: emptyList()
            "ordination" -> headerData.ordinationDays ?: emptyList()
            "deordination" -> headerData.deordinationDays ?: emptyList()
            "car" -> headerData.carDays ?: emptyList()
            "buy_car" -> headerData.buyCarDays ?: emptyList()
            "childbirth" -> headerData.childbirthDays ?: emptyList()
            "shop" -> headerData.shopDays ?: emptyList()
            "build_house" -> headerData.buildHouseDays ?: emptyList()
            "move_house" -> headerData.moveHouseDays ?: emptyList()
            "merit" -> headerData.meritDays ?: emptyList()
            "engagement" -> headerData.engagementDays ?: emptyList()
            "debt" -> headerData.debtDays ?: emptyList()
            "plant" -> headerData.plantDays ?: emptyList()
            "spirit_house" -> headerData.spiritHouseDays ?: emptyList()
            "travel" -> headerData.travelDays ?: emptyList()
            "promotion" -> headerData.promotionDays ?: emptyList()
            "job" -> headerData.jobDays ?: emptyList()
            "business" -> headerData.businessDays ?: emptyList()
            else -> emptyList()
        }
    }

    private fun pickRecommendedDateForCategory(category: String): String? {
        val dates = categoryDates(category).sorted()
        if (dates.isEmpty()) return null

        val now = DateTime.now()
        val isCurrentMonthView = currentViewDate.year == now.year && currentViewDate.monthOfYear == now.monthOfYear
        val referenceDate = if (isCurrentMonthView) now.toString("yyyy-MM-dd")
        else String.format(Locale.US, "%04d-%02d-01", currentViewDate.year, currentViewDate.monthOfYear)

        return dates.firstOrNull { it >= referenceDate } ?: dates.firstOrNull()
    }

    private fun getThaiZodiac(be: Int): String {
        val zodiacs = listOf("ปีกุน", "ปีชวด", "ปีฉลู", "ปีขาล", "ปีเถาะ", "ปีมะโรง", "ปีมะเส็ง", "ปีมะเมีย", "ปีมะแม", "ปีวอก", "ปีระกา", "ปีจอ")
        return zodiacs[(be + 6) % 12]
    }

    private fun getMyHoraVerifiedTagOverrides(): Map<String, List<String>> {
        myHoraVerifiedOverridesCache?.let { return it }
        val ctx = context ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            ctx.assets.open("rengyam/myhora_verified_tag_overrides.json").use { input ->
                input.reader(Charsets.UTF_8).use { reader ->
                    (Gson().fromJson<Map<String, List<String>>>(reader, type) ?: emptyMap()).also {
                        myHoraVerifiedOverridesCache = it
                    }
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
