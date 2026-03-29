package com.numberniceic.ui.renkyam

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
    private var hasRengyamAccess: Boolean = false
    private var isCalendarScrollLocked: Boolean = false
    private var wanpraLayoutManager: androidx.recyclerview.widget.GridLayoutManager? = null
    // Show the full calendar even for guests to encourage login later.
    private fun shouldShowFullCalendar(): Boolean = true

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

    private fun hasPositiveAuspiciousTag(wp: Wanpra): Boolean {
        val positiveTags = setOf(
            "วันธงชัย", "วันอธิบดี", "ดิถีเรียงหมอน",
            "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค",
            "วันลอย", "วันฟู", "กระทิงวัน",
            "ไชยดิถี", "ภัทรดีถี", "ปุณณดีถี", "นันทดีถี", "มิตตะดีถี"
        )
        return hasAnyTag(wp, positiveTags)
    }

    private fun hasHardNegativeTag(wp: Wanpra): Boolean {
        val hardNegativeTags = setOf(
            "วันอุบาทว์/อุบาสน", "วันโลกาวินาศ", "วันจม",
            "พิฆาต", "ดิถีพิฆาต", "กาลกรรณี", "กาลสูร", "กาลโชค",
            "กาลทิน", "กาลทัณฑ์", "โลกาวินาศ", "วินาศ", "มฤตยู",
            "บอด", "ทินสูรย์", "ทินศูร", "ทินกาล", "ทัคธทิน",
            "ยมขันธ์", "ทักทิน", "พิลา", "ทรทึก",
            "ทึกทึน", "อัตนิโรจน์", "ทินสูญ", "กาฬโชค", "กาลสูญ", "โลกวินาส", "วินาสส์", "วันบอด", "กาลทีน"
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

        return when (category) {
            "marriage", "engagement" ->
                (wp.isRiangMon || hasAnyTag(wp, setOf("ดิถีเรียงหมอน", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))) &&
                    !hasWarningKeyword(wp, "สตรี") &&
                    !hasWarningKeyword(wp, "บุรุษ")
            "surgery" ->
                hasAnyTag(wp, setOf("อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค")) &&
                    !hasWarningKeyword(wp, "สตรี") &&
                    !hasWarningKeyword(wp, "บุรุษ")
            "house", "build_house", "move_house" ->
                hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค")) &&
                    !hasWarningKeyword(wp, "บ้าน") &&
                    !hasWarningKeyword(wp, "ที่ดิน") &&
                    !hasWarningKeyword(wp, "ดิน") &&
                    !hasWarningKeyword(wp, "วัง") &&
                    !hasWarningKeyword(wp, "ภูเขา")
            "ordination" ->
                (isFlagEnabled(wp.isWanpra) || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค"))) &&
                    !hasWarningKeyword(wp, "พัทธสีมา") &&
                    !hasWarningKeyword(wp, "บวช")
            "deordination" ->
                hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค")) &&
                    !hasWarningKeyword(wp, "พัทธสีมา") &&
                    !hasWarningKeyword(wp, "บวช")
            "car", "buy_car" ->
                hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค")) &&
                    !hasWarningKeyword(wp, "รถ")
            "childbirth" ->
                hasAnyTag(wp, setOf("อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค")) &&
                    !hasWarningKeyword(wp, "สตรี")
            "shop", "business" ->
                hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "วันลอย", "วันฟู", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))
            "merit" ->
                (isFlagEnabled(wp.isWanpra) || hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค"))) &&
                    !hasWarningKeyword(wp, "เทพ")
            "debt" ->
                hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))
            "plant" ->
                hasAnyTag(wp, setOf("วันลอย", "วันฟู", "วันธงชัย", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค")) &&
                    !hasWarningKeyword(wp, "พืช") &&
                    !hasWarningKeyword(wp, "ดิน") &&
                    !hasWarningKeyword(wp, "ที่ดิน")
            "spirit_house" ->
                hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค")) &&
                    !hasWarningKeyword(wp, "เทพ") &&
                    !hasWarningKeyword(wp, "บ้าน") &&
                    !hasWarningKeyword(wp, "ที่ดิน") &&
                    !hasWarningKeyword(wp, "ดิน")
            "travel" ->
                hasAnyTag(wp, setOf("วันลอย", "วันฟู", "วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค")) &&
                    !hasWarningKeyword(wp, "น้ำ") &&
                    !hasWarningKeyword(wp, "ป่า") &&
                    !hasWarningKeyword(wp, "ภูเขา") &&
                    !hasWarningKeyword(wp, "เรือ")
            "promotion", "job" ->
                hasAnyTag(wp, setOf("วันธงชัย", "วันอธิบดี", "อำฤตโชค", "อมุตโชค", "มหาสิทธิโชค", "สิทธิโชค", "ราชาโชค", "ชัยโชค"))
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
        val user = UserContextManager.userX(requireContext())
        isAdminMode = UserContextManager.isAdmin(user)
        isLoggedInUser = !user?.userId.isNullOrEmpty()
        hasRengyamAccess = computeRengyamAccess()
        updateCalendarScrollLock()
        this.initHeaderData()
        this.setupAdapterList()
        this.fetchDataForHeader()
        this.fetchMemberAndShowKalagni()
        if (shouldShowFullCalendar()) {
            this.initRecyclerView()
            this.fetchAuspiciousForSelectedMonth()
        } else {
            renderLockedCalendarPlaceholder()
        }
    }

    private fun initHeaderData() {
        val dt = currentViewDate
        val day = if (PersonContextManager.toThaiDay(dt.dayOfWeek().asText) == "") dt.dayOfWeek().asText else PersonContextManager.toThaiDay(dt.dayOfWeek().asText)
        val monthArr = PersonContextManager.toThaiMonth(dt.monthOfYear().asText)
        val month = if (monthArr[0] == "") dt.monthOfYear().asText else monthArr[0]
        
        headerData = headerData.copy(
            dayStr = "วันนี้$day",
            dayNum = "ที่ ${dt.dayOfMonth().asText}",
            monthStr = month,
            yearStr = (dt.year().asText.toInt() + 543).toString()
        )
    }
    
    private fun setupAdapterList() {
        if (rengYamBinding.recyclerviewWanpra.adapter == null) {
            val showInauspiciousInfo = true
            
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
                        showAuspiciousBottomSheet(selectedCategory)
                    }
                },
                onLockedCategoryClick = { category ->
                    showRengyamUnlockDialog(category)
                },
                onCalendarDayClick = { day ->
                    showDayTagDetailBottomSheet(day)
                },
                onMonthNav = { direction ->
                    val targetDate = currentViewDate.plusMonths(direction)
                    currentViewDate = targetDate
                    headerData = headerData.copy(selectedCategory = null)
                    initHeaderData()
                    cachedLengYamDao?.let { updateRecyclerWithData(it) }
                    fetchAuspiciousForSelectedMonth()
                },
                onYearSelected = { selectedYearCe ->
                    currentViewDate = currentViewDate.withYear(selectedYearCe)
                    headerData = headerData.copy(selectedCategory = null)
                    initHeaderData()
                    cachedLengYamDao?.let { updateRecyclerWithData(it) }
                    fetchAuspiciousForSelectedMonth()
                    if (!shouldShowFullCalendar()) {
                        renderLockedCalendarPlaceholder()
                    }
                },
                _showInauspiciousInfo = showInauspiciousInfo
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
            canonical == "พิฆาต" || canonical == "ดิถีพิฆาต" || canonical == "ทรทึก" || canonical == "มฤตยู" || canonical == "กาลกรรณี" || canonical == "บอด" || canonical == "วินาศ" || canonical == "โลกาวินาศ" ||
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
                    { levelPriority(classifyTagBadgeLevel(it.value)) },
                    { tagPriority(it.value.tag) },
                    { it.index }
                )
            )
            .map { it.value }
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
        if (groupedTags.isNotEmpty()) return groupedTags.distinct()

        val prioritized = wp.displayTagsPrioritized.orEmpty().filter { it.isNotBlank() }
        if (prioritized.isNotEmpty()) return prioritized.distinct()

        val display = wp.displayTags.orEmpty().filter { it.isNotBlank() }
        if (display.isNotEmpty()) return display.distinct()

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
        return fallback.distinct()
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

        if (wp.kalagniBirth && wp.kalagniAge) {
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
        backToListLabel: String? = null,
        backToListIconRes: Int? = null,
        onBackToList: (() -> Unit)? = null
    ) {
        if (!isAdded) return
        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_rengyam_day_detail, null)
        val dialog = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        dialog.setContentView(dialogView)
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        val (dateText, dayOfWeek) = try {
            val dt = formatter.parseDateTime(wp.wanpraDate)
            val thaiMonth = PersonContextManager.toThaiMonth(dt.monthOfYear().asText).firstOrNull().orEmpty()
            val thaiDayName = PersonContextManager.toThaiDay(dt.dayOfWeek().asText)
            val dayLabelRaw = if (thaiDayName.isNullOrBlank()) dt.dayOfWeek().asText else thaiDayName
            val dayLabel = dayLabelRaw.removePrefix("วัน")
            "วัน$dayLabel ที่ ${toThaiNum(dt.dayOfMonth.toString())} $thaiMonth ${toThaiNum((dt.year + 543).toString())}" to dt.dayOfWeek
        } catch (e: Exception) {
            (wp.wanpraDate ?: "-") to null
        }

        dialogView.findViewById<TextView>(R.id.txt_day_detail_subtitle)?.text = dateText
        dialogView.findViewById<TextView>(R.id.txt_back_to_auspicious)?.apply {
            if (!backToListLabel.isNullOrBlank() && onBackToList != null) {
                visibility = View.VISIBLE
                text = backToListLabel
                if (backToListIconRes != null) {
                    val icon = androidx.core.content.ContextCompat.getDrawable(context, backToListIconRes)
                    val sizePx = (resources.displayMetrics.density * 18).toInt()
                    icon?.setBounds(0, 0, sizePx, sizePx)
                    setCompoundDrawables(icon, null, null, null)
                } else {
                    setCompoundDrawables(null, null, null, null)
                }
                setOnClickListener {
                    dialog.dismiss()
                    onBackToList.invoke()
                }
            } else {
                visibility = View.GONE
            }
        }
        val container = dialogView.findViewById<LinearLayout>(R.id.layout_tag_explanations)
        val tagDetails = collectDayTagDetails(wp)
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
            val item = layoutInflater.inflate(R.layout.item_rengyam_tag_explanation, container, false)
            val titleView = item.findViewById<TextView>(R.id.txt_tag_name)
            val sourceView = item.findViewById<TextView>(R.id.txt_tag_source)
            val schoolView = item.findViewById<TextView>(R.id.txt_tag_school)
            val statusText = when (classifyTagBadgeLevel(detail)) {
                TagBadgeLevel.GOOD -> {
                    "ฤกษ์ดี"
                }
                TagBadgeLevel.WARNING -> {
                    "คำเตือน"
                }
                TagBadgeLevel.FORBIDDEN -> {
                    "ข้อห้าม"
                }
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

        val goodTimes = when (dayOfWeek) {
            7 -> listOf(
                "ยามศุกร์ 07.30-09.00 (เหนือ)",
                "ยามพุธ 09.00-10.30 (ทุกทิศ)",
                "ยามจันทร์ 10.30-12.00 (ตะวันตก)"
            )
            1 -> listOf(
                "ยามเสาร์ 07.30-09.00 (เหนือ)",
                "ยามพฤหัส 13.30-15.00 (ทุกทิศ)",
                "ยามศุกร์ 15.00-16.30 (ทุกทิศ)",
                "ยามพุธ 16.30-18.00 (ทุกทิศ)"
            )
            2 -> listOf(
                "ยามศุกร์ 09.00-10.30 (เหนือ)",
                "ยามพุธ 10.30-12.00 (ตะวันตก)",
                "ยามจันทร์ 12.00-13.30 (ตะวันออก)",
                "ยามเสาร์ 13.30-15.00 (เหนือ)",
                "ยามพฤหัส 15.00-16.30 (ทุกทิศ)"
            )
            3 -> listOf(
                "ยามพุธ 06.00-07.30 (ทุกทิศ)",
                "ยามจันทร์ 07.30-09.00 (ทุกทิศ)",
                "ยามพฤหัส 10.30-12.00 (ทิศเหนือ)",
                "ยามศุกร์ 15.00-16.30 (ทิศตะวันออก)",
                "ยามพุธ 16.30-18.00 (ทุกทิศ)"
            )
            4 -> listOf(
                "ยามพฤหัส 06.00-07.30 (ทุกทิศ)",
                "ยามอาทิตย์ 09.00-10.30 (ทุกทิศ)",
                "ยามศุกร์ 10.30-12.00 (ทุกทิศ)",
                "ยามพุธ 12.00-13.30 (ทุกทิศ)",
                "ยามจันทร์ 13.30-15.00 (ทุกทิศ)",
                "ยามพฤหัส 16.30-18.00 (ทิศอุดร)"
            )
            5 -> listOf(
                "ยามศุกร์ 06.00-07.30 (ทุกทิศ)",
                "ยามพุธ 07.30-09.00 (ทุกทิศ)",
                "ยามจันทร์ 09.00-10.30 (ทิศตะวันออก)",
                "ยามเสาร์ 10.30-12.00 (ทุกทิศ)",
                "ยามพฤหัส 12.00-13.30 (ทุกทิศ)",
                "ยามอังคาร 13.30-15.00 (ทิศเหนือ)",
                "ยามศุกร์ 16.30-18.00 (ทุกทิศ)"
            )
            6 -> listOf(
                "ยามพฤหัส 07.30-09.00 (ทุกทิศ)",
                "ยามศุกร์ 12.00-13.30 (ทุกทิศ)",
                "ยามพุธ 13.30-15.00 (ทุกทิศ)"
            )
            else -> emptyList()
        }

        val travelDirectionInfo = travelDirectionInfoForDay(dayOfWeek)

        if (goodTimes.isNotEmpty()) {
            val divider = View(requireContext()).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#C8E6C9"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (resources.displayMetrics.density * 1).toInt()
                ).apply {
                    topMargin = (resources.displayMetrics.density * 12).toInt()
                    bottomMargin = (resources.displayMetrics.density * 12).toInt()
                }
            }
            container?.addView(divider)

            val header = TextView(requireContext()).apply {
                text = "ทิศและยามดีประจำวัน"
                setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                textSize = 16f
                typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sarabun_semibold)
                setPadding(0, 0, 0, 8)
            }
            container?.addView(header)

            val tableWrap = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 10, 12, 10)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#C8E6C9"))
                    setStroke(2, android.graphics.Color.parseColor("#A5D6A7"))
                    cornerRadius = 16f
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
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
                    setPadding(6, 6, 6, 6)
                }
                val leftView = TextView(requireContext()).apply {
                    text = left
                    textSize = if (isHeader) 14f else 13.5f
                    setTextColor(android.graphics.Color.parseColor(if (isHeader) "#1B5E20" else "#1B5E20"))
                    typeface = if (isHeader) {
                        androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sarabun_semibold)
                    } else {
                        androidx.core.content.res.ResourcesCompat.getFont(context, R.font.sarabun_regular)
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f)
                }
                val rightView = TextView(requireContext()).apply {
                    text = right
                    textSize = if (isHeader) 14f else 13.5f
                    setTextColor(android.graphics.Color.parseColor(if (isHeader) "#1B5E20" else "#1B5E20"))
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
                    val divider = View(requireContext()).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("#D7EED6"))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (resources.displayMetrics.density * 1).toInt()
                        )
                    }
                    parent.addView(divider)
                }
            }

            addRow(tableWrap, "ยาม / เวลา", "ทิศ", isHeader = true, addDivider = true)

            val rowsContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 6, 8, 6)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#E8F5E9"))
                    setStroke(1, android.graphics.Color.parseColor("#B2DFDB"))
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
                setBackgroundColor(android.graphics.Color.parseColor("#C8E6C9"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (resources.displayMetrics.density * 1).toInt()
                ).apply {
                    topMargin = (resources.displayMetrics.density * 14).toInt()
                    bottomMargin = (resources.displayMetrics.density * 12).toInt()
                }
            }
            container?.addView(divider)
            container?.addView(createTravelDirectionSection(travelDirectionInfo))
        }
        dialog.show()
    }

    private fun travelDirectionInfoForDay(dayOfWeek: Int?): TravelDirectionInfo? {
        return when (dayOfWeek) {
            7 -> TravelDirectionInfo(
                deityDirection = "ตะวันออกเฉียงใต้",
                spearDirection = "ตะวันตก",
                ghostDirection = "ตะวันออกเฉียงเหนือ"
            )
            1 -> TravelDirectionInfo(
                deityDirection = "ตะวันตก",
                spearDirection = "ตะวันออก",
                ghostDirection = "ตะวันออก"
            )
            2 -> TravelDirectionInfo(
                deityDirection = "ตะวันตกเฉียงใต้",
                spearDirection = "เหนือ",
                ghostDirection = "ตะวันออกเฉียงเหนือ"
            )
            3 -> TravelDirectionInfo(
                deityDirection = "ใต้",
                spearDirection = "เหนือ",
                ghostDirection = "เหนือ"
            )
            4 -> TravelDirectionInfo(
                deityDirection = "เหนือ",
                spearDirection = "ใต้",
                ghostDirection = "ใต้"
            )
            5 -> TravelDirectionInfo(
                deityDirection = "ตะวันออก",
                spearDirection = "ตะวันตก",
                ghostDirection = "ตะวันตก"
            )
            6 -> TravelDirectionInfo(
                deityDirection = "ตะวันตกเฉียงเหนือ",
                spearDirection = "ตะวันออก",
                ghostDirection = "ตะวันออกเฉียงใต้"
            )
            else -> null
        }
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

        fun addInfoRow(parent: LinearLayout, title: String, value: String, bgColor: String, valueColor: String) {
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
        }

        val section = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        section.addView(TextView(requireContext()).apply {
            text = "ฤกษ์เดินทาง"
            setTextColor(android.graphics.Color.parseColor("#1B5E20"))
            textSize = 16f
            typeface = fontSemiBold
        })

        section.addView(TextView(requireContext()).apply {
            text = "เริ่มออกเดินทางไปทางทิศเทพเจ้าก่อน หากจำเป็นค่อยโค้งกลับเข้าทิศหมายที่ต้องการ"
            setTextColor(android.graphics.Color.parseColor("#5D6B5F"))
            textSize = 12.5f
            typeface = fontRegular
            setPadding(0, dp(2), 0, dp(8))
        })

        section.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    android.graphics.Color.parseColor("#F2FFF7"),
                    android.graphics.Color.parseColor("#E7F7F0")
                )
            ).apply {
                setStroke(dp(1), android.graphics.Color.parseColor("#BFE5CC"))
                cornerRadius = dp(20).toFloat()
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
                        setColor(android.graphics.Color.parseColor("#D7F3E3"))
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
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })

        section.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#F8FBF8"))
                setStroke(dp(1), android.graphics.Color.parseColor("#D9EBD9"))
                cornerRadius = dp(16).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }

            addInfoRow(this, "หลาวเหล็ก", info.spearDirection, "#FFE5E5", "#C62828")
            addInfoRow(this, "ผีหลวง", info.ghostDirection, "#FFF0D6", "#AD6A00")
        })

        return section
    }

    private fun updateCalendarScrollLock() {
        // Requirement: member ที่ยังไม่ปลดล็อก ห้ามเลื่อนลงดูปฏิทิน
        isCalendarScrollLocked = isLoggedInUser && !hasRengyamAccess
        wanpraLayoutManager?.requestLayout()
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
        if (access == null || !access.granted) return

        val expireAtMillis = try {
            val expireAt = access.expireAt ?: return
            DateTime.parse(expireAt).plusDays(1).minusMillis(1).millis
        } catch (e: Exception) {
            return
        }

        persistRengyamAccessForUser(userId, expireAtMillis, granted = true)
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
        if (normalizedMessage != "success" || (vipType != "rengyam_vip" && vipType != "rengyam_yearly")) {
            return false
        }

        if (vipType == "rengyam_yearly") {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, 1)
            persistRengyamAccessForUser(userId, cal.timeInMillis)
        } else if (vipType == "rengyam_vip") {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, 50)
            persistRengyamAccessForUser(userId, cal.timeInMillis)
        }

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
            showAuspiciousBottomSheet(selectedCategory)
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
        apiService.getLengYam(requestYear, requestMonth).enqueue(object : Callback<LengYamDao> {
            override fun onResponse(call: Call<LengYamDao>, response: Response<LengYamDao>) {
                if (response.isSuccessful && response.body() != null) {
                    if (requestYear != currentViewDate.year || requestMonth != currentViewDate.monthOfYear) {
                        return
                    }
                    val lengyamDao = response.body()!!
                    Log.d("RengYamDebug", "Response Code: ${response.code()}")
                    updateRecyclerWithData(lengyamDao)
                    if (lengyamDao.lengYam != null) {
                        updateHeaderSpecial(lengyamDao.lengYam)
                    }
                }
            }
            override fun onFailure(call: Call<LengYamDao>, t: Throwable) {
                Log.d("RetrofitERROR", t.message ?: "Unknown error")
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

            val dt = formatter.parseDateTime(dateKey).toLocalDate()
            val wDay = dt.dayOfWeek // Joda: 1=Mon, 7=Sun
            // Thai mapping: Mon=1, Sun=7.
            val dayNamesMapped = mapOf(1 to "จันทร์", 2 to "อังคาร", 3 to "พุธ", 4 to "พฤหัสบดี", 5 to "ศุกร์", 6 to "เสาร์", 7 to "อาทิตย์")
            val currentDayName = dayNamesMapped[wDay] ?: "อาทิตย์"

            wp.kalagniBirth = headerData.kalagniBirthDays.orEmpty().any { isSameKalagniDay(currentDayName, it) }
            wp.kalagniAge = headerData.kalagniAgeDays.orEmpty().any { isSameKalagniDay(currentDayName, it) }
            wp.isKalagni = wp.kalagniBirth || wp.kalagniAge
            sanitizeAgniknirodTags(wp, dt)
            RengYamTagCanonicalizer.normalize(wp)

            wp.isBestDay = false
        }

        val monthWanpras = dateToWanpra.values.toList()

        val safeDaysWithScore = monthWanpras.map { wp ->
            val isSafe = !wp.isKalagni && !wp.isUbath && !wp.isLokawinat && !wp.isJom && !isFlagEnabled(wp.isKating)
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

        fullDataList.add(headerData)
        
        // Removed Legend and Special text items per user request to keep UI clean

        // Add Weekday Headers
        val weekdays = listOf("จันทร์", "อังคาร", "พุธ", "พฤหัส", "ศุกร์", "เสาร์", "อาทิตย์")
        weekdays.forEach { fullDataList.add(com.numberniceic.adapters.WeekdayHeader(it)) }

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
        val loyFuJom = ThaiAstrologyLib.getLoyFuJom(dithi, mThai)
        val dithiProhibition = ThaiAstrologyLib.queryDithiCommonProhibitions(dithi)

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
        wp.isLoy = loyFuJom.contains("วันลอย")
        wp.isFu = loyFuJom.contains("วันฟู")
        wp.isJom = loyFuJom.contains("วันจม")
        if (!dithiProhibition.isNullOrBlank()) {
            wp.warningTags = (wp.warningTags.orEmpty() + dithiProhibition).distinct()
            wp.calendarDisplayTags = (wp.calendarDisplayTags.orEmpty() + dithiProhibition).distinct()
        }
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
            val lfj = ThaiAstrologyLib.getLoyFuJom(dithi, mThai)
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

        val thaiDay = PersonContextManager.toThaiDay(DateTime.now().dayOfWeek().asText)
        val thaiMonth = PersonContextManager.toThaiMonth(DateTime.now().monthOfYear().asText).let { if (it.isNotEmpty()) it[0] else "" }
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
        if (fullDataList.isNotEmpty() && fullDataList[0] is TopHeaderData) {
            fullDataList[0] = headerData
            rengYamBinding.recyclerviewWanpra.adapter?.notifyDataSetChanged()
        }
    }

    private fun fetchMemberAndShowKalagni() {
        val user = UserContextManager.userX(requireContext())
        val memberId = user?.userId ?: return
        val apiService = RetrofitClient.instance.create(ApiService::class.java)

        apiService.getMemberInfo(memberId).enqueue(object : Callback<Serverx> {
            override fun onResponse(call: Call<Serverx>, response: Response<Serverx>) {
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
                        apiService.getOutfitMiracleColorSets(reqBody).enqueue(object : Callback<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse> {
                            override fun onResponse(
                                call: Call<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse>,
                                response: Response<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse>
                            ) {
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
                                updateRecyclerHeader()
                                cachedLengYamDao?.let { updateRecyclerWithData(it) }
                            }
                            override fun onFailure(call: Call<com.numberniceic.data.persons.OutfitMiracleColorSetsResponse>, t: Throwable) {}
                        })
                    } catch(e: Exception) {}
                }
            }
            override fun onFailure(call: Call<Serverx>, t: Throwable) {}
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
        val c = normalizeKalagniDayName(candidate)
        val d = normalizeKalagniDayName(currentDay)
        // strip "กลางคืน" and parentheses to compare base day name
        val cBasic = c.replace("กลางคืน", "").replace("(", "").replace(")", "").trim()
        val dBasic = d.replace("กลางคืน", "").replace("(", "").replace(")", "").trim()
        return cBasic == dBasic
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

    private fun showAuspiciousBottomSheet(category: String) {
        val context = context ?: return
        val dialog = BottomSheetDialog(context, R.style.CustomBottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_auspicious_days, null)
        
        val title = view.findViewById<TextView>(R.id.txt_bottom_sheet_title)
        val icon = view.findViewById<android.widget.ImageView>(R.id.img_bottom_sheet_icon)
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_auspicious_list)
        
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
            "plant" -> Triple("ฤกษ์ปลูกพืช", R.drawable.ic_plant, headerData.plantDays)
            "spirit_house" -> Triple("ฤกษ์ตั้งศาล", R.drawable.ic_spirit_house, headerData.spiritHouseDays)
            "travel" -> Triple("ฤกษ์เดินทาง", R.drawable.ic_travel, headerData.travelDays)
            "promotion" -> Triple("ฤกษ์รับตำแหน่ง", R.drawable.ic_promotion, headerData.promotionDays)
            "job" -> Triple("ฤกษ์สมัครงาน", R.drawable.ic_job, headerData.jobDays)
            "business" -> Triple("ฤกษ์เปิดกิจการ", R.drawable.ic_business, headerData.businessDays)
            else -> Triple("วันมงคล", R.drawable.flag, emptyList())
        }
        
        title.text = categoryTitle
        icon.setImageResource(iconRes)
        
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

            Log.d("RengYamBS", "Category: $category, items to show: ${displayData.size}")
            
            // Hide the default recycler and clear container
            recycler.visibility = View.GONE
            contentLayout?.removeAllViews()
            
            val verticalList = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
            }
            
            displayData.forEach { wp ->
                val itemView = inflater.inflate(R.layout.item_wanpra_value, verticalList, false)
                
                // Bind basic data
                val txtDate = itemView.findViewById<TextView>(R.id.txt_date_wanpra)
                val dt = try { formatter.parseDateTime(wp.wanpraDate) } catch(e: Exception) { null }
                if (dt != null) {
                    val thaiDay = PersonContextManager.toThaiDay(dt.dayOfWeek().asText)
                    val dayRaw = if (thaiDay.isEmpty()) dt.dayOfWeek().asText else thaiDay
                    val day = dayRaw.replace("วัน", "")
                    val dayNum = dt.dayOfMonth().asString
                    val month = PersonContextManager.toThaiMonth(dt.monthOfYear().asText).let { if (it.isNotEmpty()) it[0] else dt.monthOfYear().asText }
                    val year = (dt.year + 543).toString()
                    txtDate.text = "$day $dayNum $month $year"
                } else {
                    txtDate.text = wp.wanpraDate
                }

                itemView.findViewById<TextView>(R.id.txt_extra_note)?.visibility = View.GONE
                
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
                itemView.findViewById<View>(R.id.badge_lokawinat)?.isVisible = wp.isLokawinat
                
                itemView.findViewById<View>(R.id.badge_kalagni)?.visibility = if (wp.isKalagni) View.VISIBLE else View.GONE
                if (wp.isKalagni) {
                    itemView.findViewById<TextView>(R.id.badge_kalagni)?.text = "วันอัปมงคล"
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
                    wp.wanpraDate?.let { dateStr ->
                        scrollToDate(dateStr)
                    }
                    showDayTagDetailBottomSheet(
                        wp,
                        backToListLabel = "กลับไป$categoryTitle",
                        backToListIconRes = iconRes,
                        onBackToList = { showAuspiciousBottomSheet(category) }
                    )
                }
                
                verticalList.addView(itemView)
            }
            
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
        
        dialog.setContentView(view)
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
}
