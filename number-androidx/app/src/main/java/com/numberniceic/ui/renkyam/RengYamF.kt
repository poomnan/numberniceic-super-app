package com.numberniceic.ui.renkyam

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.numberniceic.R
import com.numberniceic.adapters.LegendData
import com.numberniceic.adapters.TopHeaderData
import com.numberniceic.adapters.WanpraAdapter
import com.numberniceic.data.admin.Serverx
import com.numberniceic.data.persons.OutfitMiracleColorSetsRequest
import com.numberniceic.data.rengyam.LengYamDao
import com.numberniceic.data.rengyam.WanSpecial
import com.numberniceic.data.rengyam.Wanpra
import com.numberniceic.data.rengyam.WanpraDao
import com.numberniceic.databinding.FragmentRengYamBinding
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.apersonnews.ViewModelHelper
import com.numberniceic.utils.PersonContextManager
import com.numberniceic.utils.PersonNewsCacheManager
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
    private fun shouldShowFullCalendar(): Boolean = isLoggedInUser || hasRengyamAccess

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = UserContextManager.userX(requireContext())
        isAdminMode = UserContextManager.isAdmin(user)
        isLoggedInUser = !user?.userId.isNullOrEmpty()
        hasRengyamAccess = computeRengyamAccess()
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
                        val recommendedDate = pickRecommendedDateForCategory(selectedCategory)
                        if (!recommendedDate.isNullOrBlank()) {
                            rengYamBinding.recyclerviewWanpra.post {
                                scrollToDate(recommendedDate)
                            }
                        }
                    }
                },
                onLockedCategoryClick = { category ->
                    showRengyamUnlockDialog(category)
                },
                onMonthNav = { direction ->
                    currentViewDate = currentViewDate.plusMonths(direction)
                    headerData = headerData.copy(selectedCategory = null)
                    initHeaderData()
                    cachedLengYamDao?.let { updateRecyclerWithData(it) }
                    fetchAuspiciousForSelectedMonth()
                },
                _showInauspiciousInfo = showInauspiciousInfo
            )
            rengYamBinding.recyclerviewWanpra.adapter = adater
            val gridLayout = androidx.recyclerview.widget.GridLayoutManager(context, 7)
            gridLayout.spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val type = adater.getItemViewType(position)
                    return if (type == com.numberniceic.adapters.WanpraAdapter.TYPE_CALENDAR_DAY || 
                               type == com.numberniceic.adapters.WanpraAdapter.TYPE_WEEKDAY_HEADER || 
                               type == com.numberniceic.adapters.WanpraAdapter.TYPE_EMPTY_DAY) 1 else 7
                }
            }
            rengYamBinding.recyclerviewWanpra.layoutManager = gridLayout
        } else {
             val adapter = rengYamBinding.recyclerviewWanpra.adapter as? WanpraAdapter
             adapter?.isAdminMode = isAdminMode
             adapter?.setRengyamAccess(hasRengyamAccess)
             adapter?.notifyDataSetChanged()
        }
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

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

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
                Toast.makeText(requireContext(), "กรุณากรอก Secret Code", Toast.LENGTH_SHORT).show()
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
                    val vipType = serverVip.viplevel?.lowercase(Locale.ROOT)?.trim().orEmpty()
                    if (serverVip.message == "success" && (vipType == "rengyam_vip" || vipType == "rengyam_yearly")) {
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
                        (rengYamBinding.recyclerviewWanpra.adapter as? WanpraAdapter)?.setRengyamAccess(true)
                        Toast.makeText(requireContext(), "ปลดล็อกดูฤกษ์ยามสำเร็จ", Toast.LENGTH_LONG).show()
                        onCategorySelectedAfterUnlock(categoryKey)
                    } else {
                        Toast.makeText(requireContext(), "โค้ดนี้ไม่ใช่สิทธิ์ดูฤกษ์ยาม", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "ตรวจสอบโค้ดไม่สำเร็จ (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.numberniceic.data.admin.ServerVip>, t: Throwable) {
                Toast.makeText(requireContext(), "เชื่อมต่อเซิร์ฟเวอร์ไม่ได้: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
            val recommendedDate = pickRecommendedDateForCategory(selectedCategory)
            if (!recommendedDate.isNullOrBlank()) {
                rengYamBinding.recyclerviewWanpra.post { scrollToDate(recommendedDate) }
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
        repeat(firstDayOffset) { fullDataList.add(com.numberniceic.adapters.EmptyDay()) }

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

        rengYamBinding.recyclerviewWanpra.adapter?.notifyDataSetChanged()
    }

    private fun initRecyclerView() {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getLengYam().enqueue(object : Callback<LengYamDao> {
            override fun onResponse(call: Call<LengYamDao>, response: Response<LengYamDao>) {
                if (response.isSuccessful && response.body() != null) {
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
            dateToWanpra[dtStr] = Wanpra(
                wanpraId = null,
                wanpraDate = dtStr,
                isWanpra = "0",
                isTongchai = "0",
                isAtipbadee = "0",
                isKating = "0"
            )
        }

        lengyamDao.wanPras?.forEach { wp ->
             val dtStr = wp.wanpraDate ?: ""
             if (dtStr.isNotEmpty() && dateToWanpra.containsKey(dtStr)) {
                 dateToWanpra[dtStr] = wp.copy(isHighlighted = false, isBestDay = false)
             }
        }

        dateToWanpra.forEach { (dateKey, wp) ->
            // ล้างไพ่ (Reset all flags)
            wp.isTongchai = "0"
            wp.isAtipbadee = "0"
            wp.isUbath = false
            wp.isLokawinat = false
            wp.isSittichok = false
            wp.isMahaSittichok = false
            wp.isAmmarit = false
            wp.isRachaChok = false
            wp.isChaiChok = false
            wp.isLoy = false
            wp.isFu = false
            wp.isJom = false
            wp.isRiangMon = false
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
            val kalayok = ThaiAstrologyLib.getKalayok(dt)
            wp.isTongchai = if (kalayok.contains("ธงชัย")) "1" else "0"
            wp.isAtipbadee = if (kalayok.contains("อธิบดี")) "1" else "0"
            wp.isKating = if (kalayok.contains("กระทิงวัน")) "1" else "0"
            wp.isUbath = kalayok.contains("อุบาทว์")
            wp.isLokawinat = kalayok.contains("โลกาวินาศ")

            val (dithi, mThai) = ThaiAstrologyLib.getThaiLunar(dt)
            wp.lunarPhase = if (dithi <= 15) "ขึ้น ${toThaiNum(dithi.toString())} ค่ำ" else "แรม ${toThaiNum((dithi - 15).toString())} ค่ำ"
            wp.lunarMonth = toThaiNum(mThai.toString())

            val choks = ThaiAstrologyLib.queryMahaChok(wDay, dithi)
            wp.isAmmarit = choks.contains("อำฤตโชค")
            wp.isMahaSittichok = choks.contains("มหาสิทธิโชค")
            wp.isSittichok = choks.contains("สิทธิโชค")
            wp.isRachaChok = choks.contains("ราชาโชค")
            wp.isChaiChok = choks.contains("ชัยโชค")
            wp.isRiangMon = ThaiAstrologyLib.isRiangMon(dithi)

            val lfjTags = ThaiAstrologyLib.getLoyFuJom(wDay, mThai)
            wp.isLoy = lfjTags.any { it.contains("ลอย") }
            wp.isFu = lfjTags.any { it.contains("ฟู") }
            wp.isJom = lfjTags.any { it.contains("จม") }

            wp.isBestDay = false
        }

        val monthWanpras = dateToWanpra.values.toList()

        val safeDaysWithScore = monthWanpras.map { wp ->
            val isSafe = !wp.isKalagni && !wp.isUbath && !wp.isLokawinat && !wp.isJom && wp.isKating != "1"
            var score = 0
            if (isSafe) { 
                if (wp.isWanpra == "1") score++
                if (wp.isTongchai == "1") score++
                if (wp.isAtipbadee == "1") score++
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
        val safeWanpras = monthWanpras.filter { wp -> !wp.isKalagni && !wp.isUbath && !wp.isLokawinat && !wp.isJom && wp.isKating != "1" }

        // Logic Helper: Is any of the "Maha Chok" present?
        fun hasMahaChok(wp: Wanpra) = wp.isSittichok || wp.isAmmarit || wp.isMahaSittichok || wp.isRachaChok || wp.isChaiChok

        headerData = headerData.copy(
            marriageDays = safeWanpras.filter { it.isRiangMon }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            surgeryDays = safeWanpras.filter { hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            houseDays = safeWanpras.filter { it.isTongchai == "1" || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            ordinationDays = safeWanpras.filter { it.isWanpra == "1" || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            deordinationDays = safeWanpras.filter { hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            carDays = safeWanpras.filter { it.isTongchai == "1" || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            buyCarDays = safeWanpras.filter { (it.isTongchai == "1" || hasMahaChok(it)) && (formatter.parseDateTime(it.wanpraDate).dayOfMonth != 8) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            childbirthDays = safeWanpras.filter { hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            shopDays = safeWanpras.filter { it.isLoy || it.isFu || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            buildHouseDays = safeWanpras.filter { it.isTongchai == "1" || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            moveHouseDays = safeWanpras.filter { it.isTongchai == "1" || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            meritDays = safeWanpras.filter { it.isWanpra == "1" || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            engagementDays = safeWanpras.filter { it.isRiangMon || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            debtDays = safeWanpras.filter { hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            plantDays = safeWanpras.filter { (it.isLoy || it.isFu || hasMahaChok(it)) && (formatter.parseDateTime(it.wanpraDate).dayOfMonth != 11) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            spiritHouseDays = safeWanpras.filter { it.isTongchai == "1" || it.isAtipbadee == "1" || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            travelDays = safeWanpras.filter { it.isLoy || it.isFu || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            promotionDays = safeWanpras.filter { it.isTongchai == "1" || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            jobDays = safeWanpras.filter { hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted(),
            businessDays = safeWanpras.filter { it.isLoy || it.isFu || hasMahaChok(it) }.mapNotNull { it.wanpraDate }.ifEmpty { bestDates }.distinct().sorted()
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
                    val (dithi, mThai) = com.numberniceic.utils.ThaiAstrologyLib.getThaiLunar(dt)
                    wp.lunarPhase = if (dithi <= 15) "ขึ้น ${toThaiNum(dithi.toString())} ค่ำ" else "แรม ${toThaiNum((dithi - 15).toString())} ค่ำ"
                    wp.lunarMonth = toThaiNum(mThai.toString())
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
                 val (dithi, mThai) = com.numberniceic.utils.ThaiAstrologyLib.getThaiLunar(dt)
                 wp.lunarPhase = if (dithi <= 15) "ขึ้น ${toThaiNum(dithi.toString())} ค่ำ" else "แรม ${toThaiNum((dithi - 15).toString())} ค่ำ"
                 wp.lunarMonth = toThaiNum(mThai.toString())
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

    private fun isDateKalagni(dateStr: String): Boolean {
        return false
    }

    private fun fetchAuspiciousForSelectedMonth() {
        Log.d("RengYamF", "fetchAuspiciousForSelectedMonth")
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
        val kalayok = ThaiAstrologyLib.getKalayok(now)
        val (dithi, mThai) = ThaiAstrologyLib.getThaiLunar(now)
        val lfj = ThaiAstrologyLib.getLoyFuJom(now.dayOfWeek, mThai)
        val chok = ThaiAstrologyLib.queryMahaChok(now.dayOfWeek, dithi)
        
        val tags = mutableListOf<String>()
        tags.addAll(kalayok)
        tags.addAll(lfj)
        tags.addAll(chok)
        if (ThaiAstrologyLib.isRiangMon(dithi)) tags.add("ดิถีเรียงหมอน")
        // Check if today is Wanpra
        val dateToWanpra = this.dateToWanpraMap // Corrected variable name
        val todayStr = DateTime.now().toString("yyyy-MM-dd")
        if (dateToWanpraMap.get(todayStr)?.isWanpra == "1") tags.add("วันพระ")

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
                    wp.wanpraDate?.let { dateStr: String -> scrollToDate(dateStr) }
                    dialog.dismiss()
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
