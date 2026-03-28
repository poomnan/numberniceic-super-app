package com.numberniceic.adapters

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.rengyam.WanSpecial
import com.numberniceic.data.rengyam.Wanpra
import com.numberniceic.utils.PersonContextManager
import com.numberniceic.utils.ThaiAstrologyLib
import com.numberniceic.utils.TaksaResult
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.Locale

data class TopHeaderData(
    val dayStr: String = "",
    val dayNum: String = "",
    val monthStr: String = "",
    val yearStr: String = "",
    val wanSpecial: WanSpecial? = null,
    val kalagniToday: String? = null,
    val kalagniBirth: String? = null,
    val kalagniAge: String? = null,
    val kalagniTodayDays: List<String>? = null,
    val kalagniBirthDays: List<String>? = null,
    val kalagniAgeDays: List<String>? = null,
    val kalagniBirthDualText: String? = null,
    val kalagniAgeDualText: String? = null,
    val userAge: Int? = null,
    val todaySourceDay: String? = null,
    val birthSourceDay: String? = null,
    val fooDays: List<String>? = null,
    val sittiChokDays: List<String>? = null,
    val ubathDays: List<String>? = null,
    val lokawinatDays: List<String>? = null,
    val marriageDays: List<String>? = null,
    val surgeryDays: List<String>? = null,
    val houseDays: List<String>? = null,
    val ordinationDays: List<String>? = null,
    val deordinationDays: List<String>? = null,
    val carDays: List<String>? = null,
    val buyCarDays: List<String>? = null,
    val childbirthDays: List<String>? = null,
    val shopDays: List<String>? = null,
    val buildHouseDays: List<String>? = null,
    val moveHouseDays: List<String>? = null,
    val meritDays: List<String>? = null,
    val engagementDays: List<String>? = null,
    val debtDays: List<String>? = null,
    val plantDays: List<String>? = null,
    val spiritHouseDays: List<String>? = null,
    val travelDays: List<String>? = null,
    val promotionDays: List<String>? = null,
    val jobDays: List<String>? = null,
    val businessDays: List<String>? = null,
    val userName: String? = null,
    val selectedCategory: String? = null,
    val taksaResult: TaksaResult? = null,
    val todayDateStr: String? = null,
    val todayStatusTags: List<String>? = null
)

object LegendData
class EmptyDay
data class WeekdayHeader(val name: String)

private fun toThaiNum(s: String?): String {
    if (s == null) return ""
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

private fun formatAuspiciousDates(dates: List<String>, monthStr: String): String {
    val dayNums = dates.mapNotNull { 
        try { it.split("-")[2].toInt().toString() } catch (e: java.lang.Exception) { null }
    }.joinToString(", ")
    return "$dayNums\n$monthStr"
}

class WanpraAdapter(
    val lengYam: List<Any>?, 
    val nextWanpra: String, 
    isAdminMode: Boolean = false,
    private val isLoggedInUser: Boolean = false,
    private var hasRengyamAccess: Boolean = false,
    private val onMonthNav: ((Int) -> Unit)? = null,
    private val onYearSelected: ((Int) -> Unit)? = null,
    private val onCategorySelected: ((String?) -> Unit)? = null,
    private val onLockedCategoryClick: ((String) -> Unit)? = null,
    var isGridLayout: Boolean = true,
    private val _showInauspiciousInfo: Boolean = true
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isAdminMode: Boolean = isAdminMode
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun setRengyamAccess(enabled: Boolean) {
        hasRengyamAccess = enabled
        notifyDataSetChanged()
    }
    
    private val showInauspiciousInfo: Boolean = _showInauspiciousInfo

    private val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
    private var nextWanpraDT: DateTime? = null
    
    companion object {
        const val TYPE_TOP_HEADER = 4    
        const val TYPE_LEGEND = 5        
        const val TYPE_ITEM = 1
        const val TYPE_HEADER_DETAIL = 2
        const val TYPE_DETAIL = 3
        const val TYPE_HEADER_LIST = 6
        const val TYPE_CALENDAR_DAY = 10
        const val TYPE_WEEKDAY_HEADER = 11
        const val TYPE_EMPTY_DAY = 12
    }

    init {
        nextWanpraDT = try { if (nextWanpra.isNotEmpty()) formatter.parseDateTime(nextWanpra) else null } catch (e: Exception) { null }
    }

    override fun getItemViewType(position: Int): Int {
        val item = this.lengYam!![position]
        return when {
            item is TopHeaderData -> TYPE_TOP_HEADER
            item is LegendData -> TYPE_LEGEND
            item is String && item == "header_detail" -> TYPE_HEADER_DETAIL
            item is WanSpecial -> TYPE_DETAIL
            item is String && item == "header_list" -> TYPE_HEADER_LIST
            item is EmptyDay -> TYPE_EMPTY_DAY
            item is WeekdayHeader -> TYPE_WEEKDAY_HEADER
            item is Wanpra && isGridLayout -> TYPE_CALENDAR_DAY
            else -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TOP_HEADER -> TopHeaderHolder(
                inflater.inflate(R.layout.item_rengyam_top_header, parent, false),
                isAdminMode,
                isLoggedInUser,
                hasRengyamAccess,
                onMonthNav,
                onYearSelected,
                onCategorySelected,
                onLockedCategoryClick,
                showInauspiciousInfo
            )
            TYPE_LEGEND -> LegendHolder(inflater.inflate(R.layout.item_rengyam_legend, parent, false))
            TYPE_HEADER_DETAIL, TYPE_HEADER_LIST -> HeaderHolder(inflater.inflate(R.layout.item_wanpra_header, parent, false))
            TYPE_DETAIL -> DetailHolder(inflater.inflate(R.layout.item_lengyam_detail, parent, false))
            TYPE_CALENDAR_DAY -> CalendarDayHolder(inflater.inflate(R.layout.item_calendar_day, parent, false))
            TYPE_WEEKDAY_HEADER -> WeekdayHeaderHolder(inflater.inflate(R.layout.item_calendar_weekday_header, parent, false))
            TYPE_EMPTY_DAY -> EmptyDayHolder(inflater.inflate(R.layout.item_calendar_day, parent, false))
            else -> WanparHolder(inflater.inflate(R.layout.item_wanpra_value, parent, false))
        }
    }

    override fun getItemCount(): Int {
        return this.lengYam?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = this.lengYam!![position]
        when(holder) {
            is TopHeaderHolder -> {
                holder.setRengyamAccess(hasRengyamAccess)
                holder.bind(item as TopHeaderData)
            }
            is LegendHolder -> holder.bind(item as LegendData)
            is WanparHolder -> holder.bind(item, position, nextWanpra, formatter, nextWanpraDT)
            is DetailHolder -> holder.bind(item as WanSpecial)
            is CalendarDayHolder -> holder.bind(item as Wanpra)
            is WeekdayHeaderHolder -> holder.bind(item as WeekdayHeader)
        }
    }

    class TopHeaderHolder(
        itemView: View, 
        private val isAdminMode: Boolean = false,
        private val isLoggedInUser: Boolean = false,
        private var hasRengyamAccess: Boolean = false,
        private val onMonthNav: ((Int) -> Unit)? = null,
        private val onYearSelected: ((Int) -> Unit)? = null,
        private val onCategorySelected: ((String?) -> Unit)? = null,
        private val onLockedCategoryClick: ((String) -> Unit)? = null,
        private val showInauspiciousInfo: Boolean = true
    ) : RecyclerView.ViewHolder(itemView) {
        fun setRengyamAccess(enabled: Boolean) {
            hasRengyamAccess = enabled
        }

        fun bind(data: TopHeaderData) {
            val txtCurrentMonth = itemView.findViewById<TextView>(R.id.txt_current_mount)
            val txtCurrentYear = itemView.findViewById<TextView>(R.id.txt_current_year)
            val btnPrev = itemView.findViewById<View>(R.id.btn_prev_month)
            val btnNext = itemView.findViewById<View>(R.id.btn_next_month)
            txtCurrentMonth?.text = data.monthStr
            txtCurrentYear?.text = toThaiNum(data.yearStr)
            btnPrev?.setOnClickListener { onMonthNav?.invoke(-1) }
            btnNext?.setOnClickListener { onMonthNav?.invoke(1) }
            txtCurrentYear?.setOnClickListener { anchor ->
                val yearCe = data.yearStr.toIntOrNull() ?: return@setOnClickListener
                showYearDropdown(anchor, yearCe)
            }
            val btnChat = itemView.findViewById<View>(R.id.btn_chat_ninin)
            btnChat?.visibility = if (isLoggedInUser) View.GONE else View.VISIBLE
            
            // Add scale animation for feedback
            btnChat?.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            v.performClick()
                        }
                    }
                }
                true
            }

            btnChat?.setOnClickListener {
                (itemView.context as? android.app.Activity)?.let { activity ->
                    if (!isLoggedInUser) {
                        showGuestVipBottomSheet(activity)
                        return@let
                    }
                }
            }
            val flexTaksa = itemView.findViewById<View>(R.id.flex_taksa_summary)
            val badgeBirth = itemView.findViewById<TextView>(R.id.badge_bad_birth)
            val badgeAge = itemView.findViewById<TextView>(R.id.badge_bad_age)
            if (showInauspiciousInfo && (!data.kalagniBirth.isNullOrBlank() || !data.kalagniAge.isNullOrBlank())) {
                flexTaksa?.visibility = View.VISIBLE
                val birthDay = data.birthSourceDay ?: ""
                val badBirthDay = data.kalagniBirthDualText ?: data.kalagniBirth ?: "-"
                val badAgeDay = data.kalagniAgeDualText ?: data.kalagniAge ?: "-"
                val ageLabel = data.userAge ?: 0
                val userDisplayName = data.userName?.trim().orEmpty()
                val greetingText = if (userDisplayName.isNotEmpty()) "สวัสดีคุณ$userDisplayName" else "สวัสดีคุณ..."
                val userPrefix = "●$greetingText "
                
                fun colorize(text: String, parts: Map<String, Int>): CharSequence {
                    val builder = SpannableStringBuilder(text)
                    parts.forEach { (part, color) ->
                        if (!part.isNullOrBlank()) {
                            var start = text.indexOf(part)
                            while (start >= 0) {
                                builder.setSpan(ForegroundColorSpan(color), start, start + part.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                builder.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, start + part.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                start = text.indexOf(part, start + part.length)
                            }
                        }
                    }
                    return builder
                }

                fun styleLeadingMagicDot(text: CharSequence): CharSequence {
                    val builder = SpannableStringBuilder(text)
                    if (builder.isNotEmpty() && builder[0] == '●') {
                        builder.setSpan(
                            MagicDotSpan(itemView.resources.displayMetrics.density),
                            0,
                            1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    return builder
                }

                val colorBlue = Color.parseColor("#03A9F4")
                val colorGreen = Color.parseColor("#2E7D32")
                val colorOrange = Color.parseColor("#E65100")

                if (birthDay.isNotEmpty()) {
                    val fullText = "${userPrefix}อายุย่าง $ageLabel เกิดวัน$birthDay\nห้ามใช้ฤกษ์วัน$badAgeDay $badBirthDay"
                    badgeBirth?.text = styleLeadingMagicDot(colorize(fullText, mapOf(
                        greetingText to colorBlue,
                        "อายุย่าง $ageLabel" to colorGreen,
                        "วัน$birthDay" to colorBlue,
                        "วัน$badAgeDay" to colorOrange,
                        badBirthDay to colorOrange
                    )))
                    badgeBirth?.let { applyMagicDotBadge(it) }
                } else {
                    val fullText = "${userPrefix}อายุย่าง $ageLabel\nห้ามใช้ฤกษ์วัน$badAgeDay $badBirthDay"
                    badgeBirth?.text = styleLeadingMagicDot(colorize(fullText, mapOf(
                        greetingText to colorBlue,
                        "อายุย่าง $ageLabel" to colorGreen,
                        "วัน$badAgeDay" to colorOrange,
                        badBirthDay to colorOrange
                    )))
                    badgeBirth?.let { applyMagicDotBadge(it) }
                }

                badgeAge?.isVisible = false
            } else {
                flexTaksa?.visibility = View.GONE
            }
            val layoutAuspiciousCards = itemView.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.layout_auspicious_cards_grid)
            val txtMarriage = itemView.findViewById<TextView>(R.id.txt_dates_marriage)
            val txtSurgery = itemView.findViewById<TextView>(R.id.txt_dates_surgery)
            val txtHouse = itemView.findViewById<TextView>(R.id.txt_dates_house)
            val txtOrdination = itemView.findViewById<TextView>(R.id.txt_dates_ordination)
            val txtDeordination = itemView.findViewById<TextView>(R.id.txt_dates_deordination)
            val txtCar = itemView.findViewById<TextView>(R.id.txt_dates_car)
            val txtBuyCar = itemView.findViewById<TextView>(R.id.txt_dates_buy_car)
            val txtChildbirth = itemView.findViewById<TextView>(R.id.txt_dates_childbirth)
            val txtShop = itemView.findViewById<TextView>(R.id.txt_dates_shop)
            val txtBuildHouse = itemView.findViewById<TextView>(R.id.txt_dates_build_house)
            val txtMoveHouse = itemView.findViewById<TextView>(R.id.txt_dates_move_house)
            val txtMerit = itemView.findViewById<TextView>(R.id.txt_dates_merit)
            val txtEngagement = itemView.findViewById<TextView>(R.id.txt_dates_engagement)
            val txtDebt = itemView.findViewById<TextView>(R.id.txt_dates_debt)
            val txtPlant = itemView.findViewById<TextView>(R.id.txt_dates_plant)
            val txtSpiritHouse = itemView.findViewById<TextView>(R.id.txt_dates_spirit_house)
            val txtTravel = itemView.findViewById<TextView>(R.id.txt_dates_travel)
            val txtPromotion = itemView.findViewById<TextView>(R.id.txt_dates_promotion)
            val txtJob = itemView.findViewById<TextView>(R.id.txt_dates_job)
            val txtBusiness = itemView.findViewById<TextView>(R.id.txt_dates_business)
            fun setDates(tv: TextView?, days: List<String>?) {
                if (days == null) tv?.text = "กำลังโหลด..."
                else if (days.isEmpty()) tv?.text = "ไม่มีในเดือนนี้"
                else tv?.text = formatAuspiciousDates(days, data.monthStr)
            }
            setDates(txtMarriage, data.marriageDays)
            setDates(txtSurgery, data.surgeryDays)
            setDates(txtHouse, data.houseDays)
            setDates(txtOrdination, data.ordinationDays)
            setDates(txtDeordination, data.deordinationDays)
            setDates(txtCar, data.carDays)
            setDates(txtBuyCar, data.buyCarDays)
            setDates(txtChildbirth, data.childbirthDays)
            setDates(txtShop, data.shopDays)
            setDates(txtBuildHouse, data.buildHouseDays)
            setDates(txtMoveHouse, data.moveHouseDays)
            setDates(txtMerit, data.meritDays)
            setDates(txtEngagement, data.engagementDays)
            setDates(txtDebt, data.debtDays)
            setDates(txtPlant, data.plantDays)
            setDates(txtSpiritHouse, data.spiritHouseDays)
            setDates(txtTravel, data.travelDays)
            setDates(txtPromotion, data.promotionDays)
            setDates(txtJob, data.jobDays)
            setDates(txtBusiness, data.businessDays)
            val cards = mapOf(
                "marriage" to itemView.findViewById<View>(R.id.card_marriage),
                "surgery" to itemView.findViewById<View>(R.id.card_surgery),
                "house" to itemView.findViewById<View>(R.id.card_house),
                "ordination" to itemView.findViewById<View>(R.id.card_ordination),
                "deordination" to itemView.findViewById<View>(R.id.card_deordination),
                "car" to itemView.findViewById<View>(R.id.card_car),
                "buy_car" to itemView.findViewById<View>(R.id.card_buy_car),
                "childbirth" to itemView.findViewById<View>(R.id.card_childbirth),
                "shop" to itemView.findViewById<View>(R.id.card_shop),
                "build_house" to itemView.findViewById<View>(R.id.card_build_house),
                "move_house" to itemView.findViewById<View>(R.id.card_move_house),
                "merit" to itemView.findViewById<View>(R.id.card_merit),
                "engagement" to itemView.findViewById<View>(R.id.card_engagement),
                "debt" to itemView.findViewById<View>(R.id.card_debt),
                "plant" to itemView.findViewById<View>(R.id.card_plant),
                "spirit_house" to itemView.findViewById<View>(R.id.card_spirit_house),
                "travel" to itemView.findViewById<View>(R.id.card_travel),
                "promotion" to itemView.findViewById<View>(R.id.card_promotion),
                "job" to itemView.findViewById<View>(R.id.card_job),
                "business" to itemView.findViewById<View>(R.id.card_business)
            )
            if (isAdminMode || isLoggedInUser) {
                layoutAuspiciousCards?.visibility = View.VISIBLE
                // Force a strict 5-column keypad: each card gets exactly 1/5 of container width.
                layoutAuspiciousCards?.post {
                    val containerWidth = layoutAuspiciousCards.width
                    if (containerWidth > 0) {
                        val cellWidth = containerWidth / 5
                        cards.values.forEach { card ->
                            val lp = card?.layoutParams as? com.google.android.flexbox.FlexboxLayout.LayoutParams
                            if (lp != null) {
                                lp.width = cellWidth
                                lp.flexGrow = 0f
                                card.layoutParams = lp
                            }
                        }
                    }
                }
                cards.forEach { (key, card) ->
                    val isSelected = data.selectedCategory == key
                    card?.setBackgroundResource(if (isSelected) R.drawable.bg_yellow_rounded_with_stroke else R.drawable.bg_white_rounded_with_stroke)
                    card?.setOnTouchListener { v, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start()
                            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                                if (event.action == android.view.MotionEvent.ACTION_UP) {
                                    if (hasRengyamAccess) {
                                        onCategorySelected?.invoke(if (data.selectedCategory == key) null else key)
                                    } else {
                                        onLockedCategoryClick?.invoke(key)
                                    }
                                }
                            }
                        }
                        true
                    }
                }
            } else {
                layoutAuspiciousCards?.visibility = View.GONE
            }
            val txtGreetingMsg = itemView.findViewById<TextView>(R.id.txt_greeting_message)
            val txtGreetingSubtitle = itemView.findViewById<TextView>(R.id.txt_greeting_subtitle)
            val flexTodayBadges = itemView.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.flex_today_badges)
            val chatButton = itemView.findViewById<View>(R.id.container_chat_button)
            
            chatButton?.visibility = View.GONE // Remove "ทักคุณนิน" per request
            flexTodayBadges?.visibility = View.GONE 
            txtGreetingSubtitle?.visibility = View.GONE 
            itemView.findViewById<View>(R.id.container_date_subtitle)?.visibility = View.GONE

            if (!data.todayDateStr.isNullOrEmpty()) {
                val userName = data.userName ?: ""
                val guestOrUser = if (isLoggedInUser && userName.isNotEmpty()) "$userName " else ""
                val fullText = "● ดูฤกษ์ดีฤกษ์เศรษฐีปี 2569 สำหรับคุณ ${guestOrUser}คลิ๊ก"
                val spannable = android.text.SpannableString(fullText)
                
                txtGreetingMsg?.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                spannable.setSpan(
                    MagicDotSpan(itemView.resources.displayMetrics.density),
                    0,
                    1,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                // Color for Username
                if (userName.isNotEmpty()) {
                    val startIndex = fullText.indexOf(userName)
                    if (startIndex != -1) {
                        val endIndex = startIndex + userName.length
                        spannable.setSpan(
                            android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#FF9800")), // Orange color for username
                            startIndex,
                            endIndex,
                            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                
                // Color for "คลิ๊ก"
                val arrowIndex = fullText.indexOf("คลิ๊ก")
                if (arrowIndex != -1) {
                    spannable.setSpan(
                        android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#FFD700")), // Gold color for clickable part
                        arrowIndex,
                        fullText.length,
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                
                txtGreetingMsg?.text = spannable
                txtGreetingMsg?.let { applyMagicDotBadge(it) }
            }
        }

        private fun showYearDropdown(anchor: View, currentYearCe: Int) {
            val popup = PopupMenu(anchor.context, anchor)
            val currentYearBe = currentYearCe
            val startYearBe = currentYearBe
            val endYearBe = currentYearBe + 10
            for (yearBe in startYearBe..endYearBe) {
                val yearCe = yearBe - 543
                popup.menu.add(0, yearCe, 0, "พ.ศ. ${toThaiNum(yearBe.toString())}")
            }
            popup.setOnMenuItemClickListener { item ->
                onYearSelected?.invoke(item.itemId)
                true
            }
            popup.show()
        }

        private fun showGuestVipBottomSheet(activity: android.app.Activity) {
            val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(activity, R.style.CustomBottomSheetDialogTheme)
            val view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_guest_vip_required, null, false)
            view.findViewById<View>(R.id.btn_guest_login)?.setOnClickListener {
                dialog.dismiss()
                activity.startActivity(android.content.Intent(activity, com.numberniceic.ui.auth.UserLoginAct::class.java))
            }
            view.findViewById<View>(R.id.btn_guest_register)?.setOnClickListener {
                dialog.dismiss()
                activity.startActivity(android.content.Intent(activity, com.numberniceic.ui.auth.UserRegisAct::class.java))
            }
            dialog.setContentView(view)
            val behavior = dialog.behavior
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            dialog.show()
        }

        private fun applyMagicDotBadge(target: TextView) {
            val oldPulse = target.getTag(R.id.tag_magic_dot_pulse_runnable) as? Runnable
            if (oldPulse != null) {
                target.removeCallbacks(oldPulse)
            }
            val pulse = object : Runnable {
                override fun run() {
                    if (!target.isAttachedToWindow) return
                    target.invalidate()
                    target.postDelayed(this, 16L)
                }
            }
            target.setTag(R.id.tag_magic_dot_pulse_runnable, pulse)
            target.post(pulse)
        }
    }

    private class MagicDotSpan(private val density: Float) : ReplacementSpan() {
        private val baseDotPx = 14f * density
        private val spacingPx = 4f * density

        override fun getSize(
            paint: Paint,
            text: CharSequence,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?
        ): Int {
            return (baseDotPx + spacingPx).toInt()
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
        ) {
            val t = ((SystemClock.uptimeMillis() % 1400L).toFloat() / 1400f)
            val wave = kotlin.math.sin((t * Math.PI * 2.0)).toFloat()
            val pulse = 0.94f + (0.06f * wave)
            val radius = (baseDotPx / 2f) * pulse
            val cx = x + radius
            val cy = (top + bottom) / 2f

            val glowRadius = radius * 1.65f
            val oldShader = paint.shader
            val oldStyle = paint.style
            val oldAlpha = paint.alpha

            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                cx,
                cy,
                glowRadius,
                intArrayOf(
                    Color.argb((48f * pulse).toInt().coerceAtLeast(24), 255, 224, 130),
                    Color.argb(0, 255, 224, 130)
                ),
                floatArrayOf(0.2f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, glowRadius, paint)

            paint.shader = RadialGradient(
                cx - radius * 0.25f,
                cy - radius * 0.25f,
                radius,
                intArrayOf(Color.parseColor("#FFFDE7"), Color.parseColor("#FFD54F")),
                floatArrayOf(0.25f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius, paint)

            paint.shader = oldShader
            paint.style = oldStyle
            paint.alpha = oldAlpha
        }
    }

    class LegendHolder(itemView: View) : RecyclerView.ViewHolder(itemView) { fun bind(data: LegendData) {} }
    class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class DetailHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: WanSpecial) {
             val txtLengYamDesc = itemView.findViewById<TextView>(R.id.txt_lengyam_desc)
             val txtLengYamDetail = itemView.findViewById<TextView>(R.id.txt_lengyam_detail)
             val linearParent = itemView.findViewById<LinearLayout>(R.id.linear_parent_lengyam)
             if (!item.wanDesc.isNullOrEmpty() && item.wanDesc != "วันนี้วันพระ" && item.wanDesc != "พรุ่งนี้วันพระ") {
                 linearParent.visibility = View.VISIBLE
                 txtLengYamDesc.text = item.wanDesc
                 if (!item.wanDetail.isNullOrEmpty()) {
                     txtLengYamDetail.text = item.wanDetail
                     txtLengYamDetail.visibility = View.VISIBLE
                 } else { txtLengYamDetail.visibility = View.GONE }
             } else { linearParent.visibility = View.GONE }
        }
    }

    inner class WanparHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: Any, position: Int, nextWanpra: String, formatter: org.joda.time.format.DateTimeFormatter, nextWanpraDT: DateTime?) {
            if (item is Wanpra) {
                val wp = item
                val dt = try { formatter.parseDateTime(wp.wanpraDate) } catch(e: Exception) { null }
                if (dt != null) {
                    val day = PersonContextManager.toThaiDay(dt.dayOfWeek().asText).replace("วัน", "")
                    val dayNum = dt.dayOfMonth().asString
                    val month = (PersonContextManager.toThaiMonth(dt.monthOfYear().asText).firstOrNull() ?: dt.monthOfYear().asText)
                    itemView.findViewById<TextView>(R.id.txt_date_wanpra).text = "$day $dayNum $month ${dt.year + 543}"
                }
                val badgeKalagniRow = itemView.findViewById<TextView>(R.id.badge_kalagni)
                if (isLoggedInUser && (wp.kalagniBirth || wp.kalagniAge)) {
                    badgeKalagniRow?.visibility = View.VISIBLE
                    badgeKalagniRow?.text = when {
                        wp.kalagniBirth && wp.kalagniAge -> "วันอัปมงคลอายุย่าง และวันเกิด"
                        wp.kalagniBirth -> "วันอัปมงคลวันเกิด"
                        else -> "วันอัปมงคลอายุย่าง"
                    }
                } else { badgeKalagniRow?.visibility = View.GONE }
                itemView.setBackgroundColor(android.graphics.Color.parseColor(if (wp.isHighlighted) "#FFF9C4" else if (wp.isBestDay) "#FFF176" else "#FFFDE7"))
                
                // Add Admin Info for List View
                val flexBadges = itemView.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.flex_badges)
                if (isAdminMode && flexBadges != null) {
                    flexBadges.visibility = View.VISIBLE
                    flexBadges.removeAllViews()
                    val sarabunTypeface = androidx.core.content.res.ResourcesCompat.getFont(itemView.context, R.font.sarabun_semibold)
                    
                    fun addBadge(text: String, color: String) {
                        val badgeColor = if (wp.isOtherMonth) "#9E9E9E" else color
                        val tv = TextView(itemView.context).apply {
                            this.text = text
                            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
                            setTextColor(android.graphics.Color.parseColor(badgeColor))
                            typeface = sarabunTypeface
                            val lp = com.google.android.flexbox.FlexboxLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            lp.setMargins(0, 0, 8, 4)
                            layoutParams = lp
                        }
                        flexBadges.addView(tv)
                    }
                    
                    try {
                        val wpDt = formatter.parseDateTime(wp.wanpraDate).toLocalDate()
                        val wDay = wpDt.dayOfWeek
                        val (lunarDithi, mThai) = com.numberniceic.utils.ThaiAstrologyLib.getThaiLunar(wpDt)
                        val choks = com.numberniceic.utils.ThaiAstrologyLib.queryMahaChok(wDay, lunarDithi)
                        val kalayok = com.numberniceic.utils.ThaiAstrologyLib.getKalayok(wpDt)
                        
                        if (kalayok.contains("ธงชัย")) addBadge("ธงชัย", "#2E7D32")
                        if (kalayok.contains("อธิบดี")) addBadge("อธิบดี", "#2E7D32")
                        if (choks.contains("อำฤตโชค")) addBadge("อำฤตโชค", "#2E7D32")
                        if (choks.contains("มหาสิทธิโชค")) addBadge("มหาสิทธิโชค", "#2E7D32")
                        if (choks.contains("สิทธิโชค")) addBadge("สิทธิโชค", "#2E7D32")
                        if (choks.contains("ราชาโชค")) addBadge("ราชาโชค", "#2E7D32")
                        if (choks.contains("ชัยโชค")) addBadge("ชัยโชค", "#2E7D32")
                        if (kalayok.contains("อุบาทว์")) addBadge("อุบาทว์", "#D32F2F")
                        if (kalayok.contains("โลกาวินาศ")) addBadge("โลกาวินาศ", "#D32F2F")
                        
                        if (wp.kalagniBirth) addBadge("อัปมงคลวันเกิด", "#C62828")
                        if (wp.kalagniAge) addBadge("อัปมงคลอายุย่าง", "#C62828")
                        
                    } catch (e: Exception) {}
                }
            }
        }
    }

    inner class WeekdayHeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: WeekdayHeader) {
            val txt = itemView.findViewById<TextView>(R.id.txt_weekday)
            val container = itemView.findViewById<View>(R.id.layout_weekday_container)
            txt?.text = item.name
            val bgColor = when(item.name) {
                "อาทิตย์" -> "#FF9800"
                "เสาร์" -> "#E0D8B0"
                else -> "#E0D8B0"
            }
            container?.setBackgroundColor(android.graphics.Color.parseColor(bgColor))
        }
    }

    inner class EmptyDayHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
             itemView.findViewById<View>(R.id.view_marquee_mask)?.visibility = View.VISIBLE
             itemView.findViewById<View>(R.id.view_marquee_mask)?.setBackgroundResource(R.drawable.bg_calendar_cell)
             itemView.findViewById<View>(R.id.txt_day_num)?.visibility = View.GONE
             itemView.findViewById<View>(R.id.txt_lunar_info)?.visibility = View.GONE
             itemView.findViewById<View>(R.id.layout_stars)?.visibility = View.GONE
        }
    }

    inner class CalendarDayHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewBorderAura: View? = itemView.findViewById(R.id.view_border_aura)
        val layoutBorderMarquee: View? = itemView.findViewById(R.id.layout_border_marquee)
        val viewBorderMarquee: View? = itemView.findViewById(R.id.view_border_marquee)
        val txtDayNum: TextView? = itemView.findViewById(R.id.txt_day_num)
        val txtLunar: TextView? = itemView.findViewById(R.id.txt_lunar_info)
        val flexBadges: LinearLayout? = itemView.findViewById(R.id.flex_badges)
        val viewMarqueeMask: View? = itemView.findViewById(R.id.view_marquee_mask)
        val iconPra: View? = itemView.findViewById(R.id.icon_pra)

        fun bind(wp: Wanpra) {
            val dt = try { formatter.parseDateTime(wp.wanpraDate).toLocalDate() } catch(e: Exception) { null }
            val isToday = wp.wanpraDate == org.joda.time.DateTime.now().toString("yyyy-MM-dd")
            val wpDate = wp.wanpraDate

            if (dt != null) {
                txtDayNum?.text = toThaiNum(dt.dayOfMonth.toString())
                txtDayNum?.visibility = View.VISIBLE
                val isLockedCell = wp.lunarPhase == "ล็อก" || wp.lunarMonth == "กรุณาปลดล็อก"
                txtLunar?.text = if (isLockedCell) {
                    "เฉพาะสมาชิก VIP"
                } else {
                    "${wp.lunarPhase ?: ""}\nเดือน ${wp.lunarMonth ?: ""}"
                }
                txtLunar?.visibility = if (txtLunar?.text.isNullOrBlank()) View.GONE else View.VISIBLE
            } else {
                txtDayNum?.visibility = View.GONE
                txtLunar?.visibility = View.GONE
            }

            // 🧚 Centralized Color & Text Hierarchy
            val isSun = dt?.dayOfWeek == 7
            val isSat = dt?.dayOfWeek == 6
            
            // Fixed Palette
            val grayColor = "#BDBDBD"
            val todayColor = "#000000"
            val sunColor = "#C62828"
            val satColor = "#4A148C"
            val normalColor = "#000000"
            val lunarNormal = "#212121"

            val textColorHex = when {
                wp.isOtherMonth -> grayColor
                isToday -> todayColor
                isSun -> sunColor
                isSat -> satColor
                else -> normalColor
            }
            
            txtDayNum?.setTextColor(android.graphics.Color.parseColor(textColorHex))
            txtLunar?.setTextColor(android.graphics.Color.parseColor(if (wp.isOtherMonth) grayColor else lunarNormal))

            // Highlight Today - Circle background logic if current month
            if (isToday && !wp.isOtherMonth) {
                txtDayNum?.setBackgroundResource(R.drawable.bg_calendar_cell_highlight)
                txtDayNum?.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                txtDayNum?.background = null
                txtDayNum?.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            // 🧚 Icon Pra Logic
            val isPra = wp.isWanpra?.toString() == "1" || (wp.lunarPhase?.let { it.contains("๘ ค่ำ") || it.contains("๑๕ ค่ำ") } ?: false)
            iconPra?.visibility = if (isPra && !wp.isOtherMonth) View.VISIBLE else View.GONE
            if (isPra && iconPra is android.widget.ImageView) iconPra.setColorFilter(android.graphics.Color.parseColor("#D4AF37"))

            // 🧚 Flex Badges Initialization
            flexBadges?.visibility = View.VISIBLE
            flexBadges?.removeAllViews()
            val sarabunTypeface = androidx.core.content.res.ResourcesCompat.getFont(itemView.context, R.font.sarabun_semibold)
            
            fun norm(v: Any?): Boolean {
                val s = v?.toString()?.lowercase() ?: ""
                return s == "1" || s == "1.0" || s == "true"
            }

            fun addBadge(text: String, color: String) {
                val badgeColor = if (wp.isOtherMonth) "#9E9E9E" else color
                    val tv = TextView(itemView.context).apply {
                        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 7f)
                        setTextColor(android.graphics.Color.parseColor(badgeColor))
                        typeface = sarabunTypeface
                        gravity = android.view.Gravity.START
                        textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                        setPadding(0, 0, 0, 0)
                        background = null
                        layoutParams = LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    this.text = text
                }
                flexBadges?.addView(tv)
            }

            // 🧚 Dynamic Badge Calculation - Compute EVERYTHING for every cell
            if (dt != null && !wpDate.isNullOrEmpty()) {
                try {
                    val wDay = dt.dayOfWeek
                    val (lunarDithi, mThai) = com.numberniceic.utils.ThaiAstrologyLib.getThaiLunar(dt)
                    val lfjTags = com.numberniceic.utils.ThaiAstrologyLib.getLoyFuJom(wDay, mThai)
                    val choks = com.numberniceic.utils.ThaiAstrologyLib.queryMahaChok(wDay, lunarDithi)
                    val dithi5 = com.numberniceic.utils.ThaiAstrologyLib.getDithiMongkol5(wDay, lunarDithi)
                    val badDithis = com.numberniceic.utils.ThaiAstrologyLib.queryInauspicious(wDay, lunarDithi)
                    val isMahasun = com.numberniceic.utils.ThaiAstrologyLib.queryMahasun(mThai, lunarDithi)
                    val dithiCommonProhibition = com.numberniceic.utils.ThaiAstrologyLib.queryDithiCommonProhibitions(lunarDithi)
                    val kalayok = com.numberniceic.utils.ThaiAstrologyLib.getKalayok(dt)
                    
                    val goodColor = "#2196F3" // Blue for auspicious days
                    
                    // Rendering sequence
                    if (kalayok.contains("ธงชัย")) addBadge("ธงชัย", goodColor)
                    if (kalayok.contains("อธิบดี")) addBadge("อธิบดี", goodColor)
                    if (com.numberniceic.utils.ThaiAstrologyLib.isRiangMon(lunarDithi)) addBadge("วันเคียงหมอน", goodColor)
                    if (lfjTags.any { it.contains("ลอย") }) addBadge("วันลอย", goodColor)
                    if (lfjTags.any { it.contains("ฟู") }) addBadge("วันฟู", goodColor)
                    if (lfjTags.any { it.contains("จม") }) addBadge("วันจม", "#D32F2F")
                    
                    // แสดงรายละเอียดกฤษ์เต็มสำหรับ:
                    // - Admin
                    // - Member ที่ปลดล็อกแล้ว
                    // - Member ที่ login อยู่ (ตาม requirement ล่าสุดให้โชว์เต็มเพื่อเชิญชวนใช้งาน)
                    if (this@WanpraAdapter.isAdminMode || this@WanpraAdapter.hasRengyamAccess || this@WanpraAdapter.isLoggedInUser) {
                        if (choks.contains("อำฤตโชค")) addBadge("อำฤตโชค", goodColor)
                        if (choks.contains("มหาสิทธิโชค")) addBadge("มหาสิทธิโชค", goodColor)
                        if (choks.contains("สิทธิโชค")) addBadge("สิทธิโชค", goodColor)
                        if (choks.contains("ราชาโชค")) addBadge("ราชาโชค", goodColor)
                        if (choks.contains("ชัยโชค")) addBadge("ชัยโชค", goodColor)
                        
                        dithi5.forEach { addBadge(it, goodColor) }
                        badDithis.forEach { addBadge(it, "#D32F2F") }
                        if (isMahasun) addBadge("มหาสูญ", "#D32F2F")
                        dithiCommonProhibition?.let { addBadge(it, "#8E24AA") }
                        
                        if (kalayok.contains("อุบาทว์")) addBadge("อุบาทว์", "#D32F2F")
                        if (kalayok.contains("โลกาวินาศ")) addBadge("โลกาวินาศ", "#D32F2F")
                    }
                } catch (e: Exception) {}
            }

            // Personalized Inauspicious (Only for main month and Admin mode)
            if (!wp.isOtherMonth && this@WanpraAdapter.isAdminMode) {
                if (wp.kalagniBirth && wp.kalagniAge) addBadge("อัปมงคลอายุย่าง และวันเกิด", "#C62828")
                else if (wp.kalagniBirth) addBadge("อัปมงคลวันเกิด", "#C62828")
                else if (wp.kalagniAge || wp.isKalagni) addBadge("อัปมงคลอายุย่าง", "#C62828")
            }

            viewBorderAura?.visibility = if (wp.isHighlighted && !wp.isOtherMonth) View.VISIBLE else View.GONE
            viewMarqueeMask?.visibility = View.VISIBLE
            
            if (wp.isOtherMonth) {
                viewMarqueeMask?.setBackgroundResource(R.drawable.bg_calendar_cell)
                layoutBorderMarquee?.visibility = View.GONE
                viewBorderMarquee?.clearAnimation()
            } else {
                // Determine base background
                if (wp.isHighlighted) {
                    // Set to Light Green for Auspicious selection as requested
                    viewMarqueeMask?.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
                } else {
                    viewMarqueeMask?.setBackgroundResource(when {
                        wp.isBestDay -> R.drawable.bg_calendar_cell_best
                        else -> R.drawable.bg_calendar_cell
                    })
                }
                
                if (wp.isBestDay) {
                    layoutBorderMarquee?.visibility = View.VISIBLE
                    val rotate = android.view.animation.RotateAnimation(0f, 360f, android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f, android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f).apply {
                        duration = 800; repeatCount = -1; interpolator = android.view.animation.LinearInterpolator()
                    }
                    viewBorderMarquee?.startAnimation(rotate)
                } else {
                    layoutBorderMarquee?.visibility = View.GONE
                    viewBorderMarquee?.clearAnimation()
                }
            }
        }
    }
}
