package com.numberniceic.ui.apersonnews

import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.view.Gravity
import android.graphics.Typeface
import android.animation.ArgbEvaluator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.cardview.widget.CardView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.numberniceic.R
import com.numberniceic.data.member.BagColor
import com.numberniceic.data.member.BagColorDaoCollection
import com.numberniceic.data.persons.DressColorCollection
import com.numberniceic.databinding.FragmentPersonNewsBinding
import com.numberniceic.data.admin.BuddhaPang
import com.numberniceic.ui.renkyam.RengYam
import com.numberniceic.ui.tambon.TambonDiaf
import com.bumptech.glide.Glide
import com.numberniceic.utils.PersonContextManager
import com.numberniceic.utils.UserContextManager
import org.joda.time.DateTime
import java.util.Date
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.work.ExistingWorkPolicy
import java.util.concurrent.TimeUnit
import android.os.Handler
import android.os.Looper
import java.util.Locale
import com.numberniceic.utils.ThaiAstrologyLib
import org.joda.time.LocalDate

import java.text.SimpleDateFormat
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.numberniceic.data.news.News24
import com.numberniceic.ui.news.NewsAct
import com.numberniceic.ui.news.NewsAllAct
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.numberniceic.data.notification.NotificationResponse
import com.numberniceic.data.notification.MarkReadRequest
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.numberniceic.data.admin.SpellItem
import android.view.animation.LinearInterpolator


class PersonNewsF : Fragment() {
    private lateinit var clothColorModel: Clothcolor3dModel
    private lateinit var viewModel: PersonNewsViewModel
    private lateinit var binding: FragmentPersonNewsBinding
    private var bagColorCollDao: BagColorDaoCollection? = null
    private var ageNextYang: Int? = null
    private var ageCurrent: Int? = null
    private var stateBagColor = false
    private var wanpraStatus = false
    private var meritAdapter: DynamicNotificationAdapter? = null
    private var changeAdapter: DynamicNotificationAdapter? = null
    private var templeAdapter: TempleAdapter? = null
    private var spellAdapter: SpellAdapter? = null
    private var buddhaAnnualAdapter: BuddhaAdapter? = null
    private var buddhaLifetimeAdapter: BuddhaAdapter? = null
    private var rengyamMagicAnimator: AnimatorSet? = null
    private var rengyamSparkleDrawable: GradientDrawable? = null

    private val refreshReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            try {
                Log.d("PersonNewsF", "Refresh Broadcast Received - Reloading Data")
                if (isAdded) {
                    val ctx = context ?: return
                    // Force network refresh when notification arrives to show latest data immediately
                    viewModel.loadAllData(ctx, forceRefresh = true)
                    initDataUserCalculationsOnly()
                    updateDynamicNotifications()
                    loadAssignedSpells()
                }
            } catch (e: Exception) {
                Log.e("PersonNewsF", "refreshReceiver failed", e)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d("PersonNewsF", "onCreateView args=$arguments")
        clothColorModel = ViewModelProvider(this)[Clothcolor3dModel::class.java]
        viewModel = ViewModelProvider(this)[PersonNewsViewModel::class.java]
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_person_news, container, false)
        binding.apply {
            lifecycleOwner = this@PersonNewsF
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        Log.d("PersonNewsF", "onViewCreated savedInstanceState=${savedInstanceState != null}")
        
        setupAdapters()

        this.setCurrentDate()

        binding.linearRengyam.isVisible = false
        // Hide WanPra default to avoid hardcoded text showing before data loads
        binding.linearWanpraTomorro.isVisible = false  // Default placeholders: show something immediately even if APIs fail.
        binding.defaultBagView.isVisible = true
        binding.linearColorBag.isVisible = false
        // Prevent showing static white placeholder dots before real color data arrives.
        setChipColorDNew(null)
        setChipColorRNew(null)

        setupClickListeners()
        initRengYam(view.context)
        startRengyamMagicAnimation()
        initDressColor3Day()
        
        // News Section Removed
        // initNewsFromTable()
        // initBtnReadmoreNews()
        
        // Calculate user data (Age) first
        initDataUserCalculationsOnly()

        // Observe and Load Data
        observeViewModel()

        // Avoid immediate duplicate refresh from onResume(): set baseline user id here.
        try {
            val user = UserContextManager.userX(requireContext())
            lastUserId = user?.userId ?: "guest"
        } catch (_: Exception) {
        }
        
        val force = arguments?.getBoolean("force_refresh", false) ?: false
        Log.d("PersonNewsF", "Calling viewModel.loadAllData forceRefresh=$force")
        viewModel.loadAllData(requireContext(), forceRefresh = force)
        
        // Register Refresh Listener
        val filter = android.content.IntentFilter()
        filter.addAction("com.numberniceic.REFRESH_DASHBOARD")
        filter.addAction("com.numberniceic.NEW_NOTIFICATION")

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            view.context.registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            view.context.registerReceiver(refreshReceiver, filter)
        }
        
        // Initial Local Notifications Load
        updateDynamicNotifications()
        
        // Load Spells
        loadAssignedSpells()

        // 🌟 Initialize Shimmering Gold Chat Icons for all contact buttons

        setupComposeChatIcon(binding.composeChatIconBuddhaCombined, 24)
        setupComposeChatIcon(binding.composeChatIconInauspiciousCombined, 24)



        // Set visibility and click listener for the contact button


        // Setup All Section Contact Buttons
        val openChat = { startActivity(Intent(context, com.numberniceic.ui.chat.ChatActivity::class.java)) }
        val openLine = {
            val lineId = getString(R.string.line_id)
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://line.me/ti/p/~$lineId"))
            startActivity(intent)
        }


        binding.defaultBuddhaAnnualView.setOnClickListener { openChat() }
        binding.defaultBuddhaLifetimeView.setOnClickListener { openChat() }
        binding.btnContactNinBuddhaCombined.setOnClickListener { openChat() }
        binding.defaultMeritView.setOnClickListener { openChat() }
        binding.defaultChangeView.setOnClickListener { openChat() }
        binding.defaultInauspiciousYearView.setOnClickListener { openChat() }
        binding.defaultInauspiciousLifeView.setOnClickListener { openChat() }
        binding.btnContactNinInauspiciousCombined.setOnClickListener { openChat() }
        binding.defaultAuspiciousYearView.setOnClickListener { openChat() }
        binding.defaultAuspiciousLifeView.setOnClickListener { openChat() }
        binding.defaultTempleView.setOnClickListener { openChat() }
        binding.btnContactNinSpell.setOnClickListener { openChat() }
        binding.btnContactNinInauspiciousFooter.setOnClickListener { openLine() }

        updateWednesdayWarningUI()
    }

    private fun updateWednesdayWarningUI() {
        if (clothColorModel.getStatusWednesday()) {
            binding.layoutWarningWed.visibility = View.VISIBLE
            // Colors for Wednesday Night: Yellow (2), Orange (5), Green (4)
            val colors = listOf("#FFEB3B", "#FF9800", "#4CAF50")
            val views = listOf(
                binding.chipColorWedAnti1,
                binding.chipColorWedAnti2,
                binding.chipColorWedAnti3,
                binding.chipColorWedAnti4,
                binding.chipColorWedAnti5,
                binding.chipColorWedAnti6
            )
            
            views.forEachIndexed { index, view ->
                if (index < colors.size) {
                    view.setCardBackgroundColor(Color.parseColor(colors[index]))
                    view.visibility = View.VISIBLE
                } else {
                    view.visibility = View.GONE
                }
            }
        } else {
            binding.layoutWarningWed.visibility = View.GONE
        }
    }


    private var lastUserId: String? = null

    override fun onResume() {
        super.onResume()
        try {
            val user = UserContextManager.userX(requireContext())
            val currentId = user?.userId ?: "guest"
            
            // 🚀 SMART REFRESH: If user changed while sheet was open, reload data INSTANTLY
            if (currentId != lastUserId) {
                Log.d("PersonNewsF", "onResume: User ID changed from $lastUserId to $currentId - Refreshing Dashboard")
                lastUserId = currentId
                viewModel.loadAllData(requireContext())
                initDataUserCalculationsOnly() 
                updateDynamicNotifications()
                loadAssignedSpells()
            }
        } catch (e: Exception) { 
            Log.e("PersonNewsF", "Error in onResume refresh", e)
        }
    }

    override fun onDestroyView() {
        stopRengyamMagicAnimation()
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(refreshReceiver)
        } catch (e: Exception) { }
    }

    private fun setupClickListeners() {

        binding.cardBagColor.setOnClickListener {
            val userx = UserContextManager.userX(requireContext()) ?: return@setOnClickListener
            
            val vipRole = userx.vipcode?.lowercase() ?: ""
            if (UserContextManager.isAdmin(userx) || listOf("normal", "specialp", "silver", "gold", "diamond", "member").contains(vipRole) || (bagColorCollDao?.bagColor != null && bagColorCollDao!!.bagColor!!.any { !it.bagColor1.isNullOrEmpty() })) {
                if (bagColorCollDao != null && bagColorCollDao!!.bagColor != null) {
                    if (!this.stateBagColor) {
                        this.stateBagColor = true
                        val agenex = (this.ageCurrent ?: 0) + 1
                        val ageYx = (this.ageNextYang ?: 0) + 1
                        this.setBagColor(this.bagColorCollDao!!.bagColor, this.ageNextYang.toString())
                        binding.txtAgeYang.text = "อายุ $agenex ปี ย่าง $ageYx ปี"
                        binding.txtBagDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_bag_desc))
                    } else {
                        stateBagColor = false
                        this.setBagColor(this.bagColorCollDao!!.bagColor, this.ageCurrent.toString())
                        binding.txtAgeYang.text = "อายุ ${this.ageCurrent} ปี ย่าง ${this.ageNextYang} ปี"
                        binding.txtBagDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_bag_desc_current))
                    }
                }
            } else {
                binding.txtBagDesc.text = "กรุณาติดต่อเปิดดวงประจำปี เพื่อทราบสีกระเป๋ามงคลของท่านในปีนี้"
                binding.txtBagDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_txt))

            }
        }

        binding.cardNameUser.setOnClickListener {
            val tambonDiaf = TambonDiaf.newInstance()
            tambonDiaf.show(childFragmentManager, "TambonDiaf")
        }

        binding.linearRengyam.setOnClickListener {
            startActivity(Intent(context, RengYam::class.java))
        }
        
        binding.chipDayBirth.setOnClickListener {
             startActivity(Intent(context, BirthDayAct::class.java))
        }

        // New Footer Contact (Internal Chat)
        binding.cardContactFooter.setOnClickListener {
            val mainNavController = (activity as? com.numberniceic.ui.AppActivity)?.let { act ->
                // This is a hack because PersonNewsF is inside a BottomSheet inside AppActivity 
                // which might have a Compose MainScreen if they switched to it.
                // But traditionally AppActivity uses ViewPager.
                // If it's ViewPager, we can't 'navigate' to Chat Screen easily if it's Compose-only.
                // So I will make Chat Screen an Activity or a Fragment too.
            }
            
            // For now, let's open ChatActivity (to be created)
            startActivity(Intent(context, com.numberniceic.ui.chat.ChatActivity::class.java))
        }
    }

    private fun observeViewModel() {

        // Lucky Number
        viewModel.luckyNumber.observe(viewLifecycleOwner) { luckynumbers ->
            if (luckynumbers != null && luckynumbers.active == "1" && !luckynumbers.number.isNullOrEmpty()) {
                binding.txtLuckyNumber.text = luckynumbers.number!!
                binding.txtTitleLuckynumber.text = "เลขนำโชค"
                binding.txtTitleLuckynumber.visibility = View.VISIBLE
                binding.txtLuckyNumber.visibility = View.VISIBLE
                setBagColor(bagColorCollDao?.bagColor, ageCurrent?.toString())
            } else {
                binding.txtLuckyNumber.text = ""
                binding.txtTitleLuckynumber.text = ""
                binding.txtTitleLuckynumber.visibility = View.GONE
                binding.txtLuckyNumber.visibility = View.GONE
                setBagColor(bagColorCollDao?.bagColor, ageCurrent?.toString())
            }
        }

        // Wan Pra
        viewModel.wanPra.observe(viewLifecycleOwner) { wanPra ->
            if (wanPra != null) {
                if (wanPra.activity == "wanpra") {
                    if (wanPra.wanpra != null) {
                        wanpraStatus = true
                        // Show in Dashboard HEADER date bar
                        binding.linearWanpraTomorro.isVisible = true
                        binding.txtWanpraTomorro.text = "วันนี้วันพระ"
                    } else if (wanPra.tomorro) {
                         // Show in Dashboard HEADER date bar
                         binding.linearWanpraTomorro.isVisible = true
                         binding.txtWanpraTomorro.text = "พรุ่งนี้วันพระ"
                    } else {
                         // Not Today or Tomorrow
                        // Hide from HEADER if not today
                        binding.linearWanpraTomorro.isVisible = false                    }
                    
                    // Auspicious Days Logic (Icons in Header) - NOW ONLINE!
                    val special = wanPra.wanSpecial
                    
                    fun norm(v: String?): Boolean {
                         return v == "1" || v?.lowercase() == "true"
                    }

                    // Populate the new symbolic area beside the calendar
                    binding.flexAuspiciousSymbols.removeAllViews()
                    
                    val addSymbol = { iconRes: Int ->
                        val iconView = android.widget.ImageView(requireContext()).apply {
                            setImageResource(iconRes)
                            val sizePx = (22 * resources.displayMetrics.density).toInt()
                            layoutParams = android.view.ViewGroup.LayoutParams(sizePx, sizePx)
                            setPadding(4, 0, 4, 0)
                        }
                        binding.flexAuspiciousSymbols.addView(iconView)
                    }

                    if (norm(special?.wanTongchai)) addSymbol(R.drawable.flag)    // วันธงชัย
                    if (norm(special?.wanAtipbadee)) addSymbol(R.drawable.boss)  // วันอธิบดี
                } else {
                    binding.linearWanpraTomorro.isVisible = false
                }
            }
        }

        // Bag Color
        viewModel.bagColorCollection.observe(viewLifecycleOwner) { result ->
            Log.d("PersonNewsF", "Bag Color Observer: result=${result?.activity}, count=${result?.bagColor?.size}, currentAge=$ageCurrent")
            if (result != null && result.activity == "success") {
                bagColorCollDao = result
                activity?.runOnUiThread {
                    binding.defaultBagView.isVisible = false
                    binding.linearColorBag.isVisible = true
                    setBagColor(result.bagColor, ageCurrent.toString())
                }
            } else {
                 Log.d("PersonNewsF", "Bag Color not found or error: activity=${result?.activity}")
                 bagColorCollDao = null
                 activity?.runOnUiThread {
                    binding.defaultBagView.isVisible = true
                    binding.linearColorBag.isVisible = false
                    setBagColor(null, ageCurrent.toString())
                 }
            }
        }

        // Dress Colors
        viewModel.dressColorD.observe(viewLifecycleOwner) { res ->
            if (res != null) {
                lifecycleScope.launch(Dispatchers.Default) {
                    val colors = clothColorModel.getColorSortxD(res, requireContext())
                    withContext(Dispatchers.Main) {
                        setChipColorDNew(colors)
                    }
                }
            } else {
                setChipColorDNew(null)
            }
        }
        viewModel.dressColorR.observe(viewLifecycleOwner) { res ->
            if (res != null) {
                lifecycleScope.launch(Dispatchers.Default) {
                    val colors = clothColorModel.getColorSortxR(res, requireContext())
                    withContext(Dispatchers.Main) {
                        setChipColorRNew(colors)
                    }
                }
            } else {
                setChipColorRNew(null)
            }
        }
        viewModel.outfitApiAuspiciousThai.observe(viewLifecycleOwner) { text ->
            binding.txtOutfitApiMongkol.text = text ?: "API มงคล: ไม่มีข้อมูล"
        }
        viewModel.outfitApiInauspiciousThai.observe(viewLifecycleOwner) { text ->
            binding.txtOutfitApiApmongkol.text = text ?: "API อัปมงคล: ไม่มีข้อมูล"
        }
        viewModel.outfitApiReferenceThai.observe(viewLifecycleOwner) { text ->
            binding.txtOutfitApiSource.text = text ?: "อ้างอิงการคำนวณชุดสีจาก: /Users/tayap/project-naming/outfit-miracle/SKILL.md"
        }
        
        // Miracle Dos
        viewModel.miracleSapom.observe(viewLifecycleOwner) { res -> updateMiracleUI(res, "สระผม") }
        viewModel.miracleCutNail.observe(viewLifecycleOwner) { res -> updateMiracleUI(res, "ตัดเล็บ") }
        viewModel.miracleCutHair.observe(viewLifecycleOwner) { res -> updateMiracleUI(res, "ตัดผม") }
        
        viewModel.miracleParmai.observe(viewLifecycleOwner) { res -> 
             if (res == null) return@observe
             val strActivity = StringBuilder()
             if (res.activity == "karee") {
                  strActivity.append("คุณ \"ห้ามใส่ผ้าใหม่\" ในวันนี้ ไม่ดี")
                  binding.txtParmai.text = strActivity.toString()
                  binding.txtParmai.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_txt))
                  binding.imgParmai.setImageResource(R.drawable.delete)
                  binding.imgDoParmai.isVisible = false
             } else if (res.activity == "ผ้าใหม่") {
                  if (res.action == "0") {
                       strActivity.append("คุณ \"ห้ามใส่ผ้าใหม่\" ในวันนี้ ไม่ดี")
                       binding.txtParmai.text = strActivity.toString()
                       binding.txtParmai.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_txt))
                       binding.imgParmai.setImageResource(R.drawable.delete)
                       binding.imgDoParmai.isVisible = false
                  } else if (res.action == "1") {
                       strActivity.append("คุณควร \"ใส่ผ้าใหม่\" ในวันนี้")
                       binding.txtParmai.text = strActivity.toString()
                       binding.txtParmai.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_txt))
                       binding.imgParmai.setImageResource(R.drawable.approval)
                  }
             }
             binding.chipReadmoreParmai.setOnClickListener {
                 val userx = UserContextManager.userX(requireContext())
                 if (userx == null) {
                     (parentFragment as? com.google.android.material.bottomsheet.BottomSheetDialogFragment)?.dismiss()
                     (activity as? com.numberniceic.ui.AppActivity)?.showLoginBottomSheet()
                     return@setOnClickListener
                 }
                 val desc = res.miraDesc ?: ""
                 val miraDiaryF = MiraDiaryF.newInstance(desc)
                 miraDiaryF.show(childFragmentManager, "MiraDiaryF")
             }
        }
        
        binding.chipReadmoreNight.setOnClickListener {
             val miraDiaryF = MiraDiaryF.newInstance(getString(R.string.wan_pra))
             miraDiaryF.show(childFragmentManager, "MiraDiaryF")
        }

        // Assigned Buddha Pang (Annual + Lifetime Sync)
        viewModel.assignedBuddhaPang.observe(viewLifecycleOwner) { json ->
            android.util.Log.d("PersonNewsF", "=== Full Buddha JSON Response ===")
            
            val userx = UserContextManager.userX(requireContext())
            val gson = com.google.gson.Gson()
            val userIdStr = userx?.userId ?: "guest"
            
            val annualListResponse = if (json != null && json.has("annual") && !json.get("annual").isJsonNull) {
                val element = json.get("annual")
                if (element.isJsonArray) {
                    val listType = object : com.google.gson.reflect.TypeToken<List<com.numberniceic.data.admin.BuddhaPang>>() {}.type
                    gson.fromJson<List<com.numberniceic.data.admin.BuddhaPang>>(element, listType).filter { it.id != null && it.id != 0 }
                } else {
                    val pang = gson.fromJson(element, com.numberniceic.data.admin.BuddhaPang::class.java)
                    if (pang != null && pang.id != 0) listOf(pang) else emptyList()
                }
            } else emptyList()
            
            val finalAnnualList = if (json != null) {
                saveLocalBuddhaHistory(userIdStr, "annual", annualListResponse)
                annualListResponse.filter { 
                    val idToSafe = it.id?.toLong() ?: 0L
                    !com.numberniceic.data.local.NotificationStorage.isBlacklistedId(requireContext(), idToSafe)
                }
            } else {
                loadLocalBuddhaHistory(userIdStr, "annual").filter {
                    val idToSafe = it.id?.toLong() ?: 0L
                    !com.numberniceic.data.local.NotificationStorage.isBlacklistedId(requireContext(), idToSafe)
                }
            }
            
            // 2. Lifetime Pang Logic
            val lifetimeListResponse = if (json != null && json.has("lifetime") && !json.get("lifetime").isJsonNull) {
                val element = json.get("lifetime")
                if (element.isJsonArray) {
                    val listType = object : com.google.gson.reflect.TypeToken<List<com.numberniceic.data.admin.BuddhaPang>>() {}.type
                    gson.fromJson<List<com.numberniceic.data.admin.BuddhaPang>>(element, listType).filter { it.id != null && it.id != 0 }
                } else {
                    val pang = gson.fromJson(element, com.numberniceic.data.admin.BuddhaPang::class.java)
                    if (pang != null && pang.id != 0) listOf(pang) else emptyList()
                }
            } else emptyList()

            val finalLifetimeList = if (json != null) {
                saveLocalBuddhaHistory(userIdStr, "lifetime", lifetimeListResponse)
                lifetimeListResponse.filter {
                    val idToSafe = it.id?.toLong() ?: 0L
                    !com.numberniceic.data.local.NotificationStorage.isBlacklistedId(requireContext(), idToSafe)
                }
            } else {
                loadLocalBuddhaHistory(userIdStr, "lifetime").filter {
                    val idToSafe = it.id?.toLong() ?: 0L
                    !com.numberniceic.data.local.NotificationStorage.isBlacklistedId(requireContext(), idToSafe)
                }
            }

            // Consolidation Logic
            if (finalAnnualList.isEmpty() && finalLifetimeList.isEmpty()) {
                // Both missing -> Show Grouped Placeholder
                binding.headerBuddhaAnnual.visibility = View.VISIBLE
                binding.txtHeaderBuddhaAnnual.text = "พระพุทธรูปที่ต้องถวายประจำปีนี้${getAgeHeaderSuffix()}"
                binding.headerBuddhaLifetime.visibility = View.VISIBLE
                binding.txtHeaderBuddhaLife.text = "พระพุทธรูปที่ต้องถวายตลอดชีวิต"
                
                binding.combinedBuddhaPlaceholder.visibility = View.GONE
                binding.defaultBuddhaAnnualView.visibility = View.VISIBLE
                binding.defaultBuddhaLifetimeView.visibility = View.VISIBLE
                binding.containerAnnualList.visibility = View.GONE
                binding.containerLifetimeList.visibility = View.GONE
            } else {
                // Separate Data logic
                binding.combinedBuddhaPlaceholder.visibility = View.GONE
                binding.headerBuddhaAnnual.visibility = View.VISIBLE
                binding.txtHeaderBuddhaAnnual.text = "พระพุทธรูปที่ต้องถวายประจำปีนี้${getAgeHeaderSuffix()}"
                
                if (finalAnnualList.isNotEmpty()) {
                    binding.defaultBuddhaAnnualView.visibility = View.GONE
                    binding.containerAnnualList.visibility = View.VISIBLE
                    updateBuddhaAnnualListUI(finalAnnualList)
                } else {
                    binding.defaultBuddhaAnnualView.visibility = View.VISIBLE
                    binding.containerAnnualList.visibility = View.GONE
                }

                binding.headerBuddhaLifetime.visibility = View.VISIBLE
                binding.txtHeaderBuddhaLife.text = "พระพุทธรูปที่ต้องถวายตลอดชีวิต"
                
                if (finalLifetimeList.isNotEmpty()) {
                    binding.defaultBuddhaLifetimeView.visibility = View.GONE
                    binding.containerLifetimeList.visibility = View.VISIBLE
                    updateBuddhaLifetimeListUI(finalLifetimeList)
                } else {
                    binding.defaultBuddhaLifetimeView.visibility = View.VISIBLE
                    binding.containerLifetimeList.visibility = View.GONE
                }
            }
        }
        
        // Assigned Sacred Temple (List Accumulation - Fixed)
        viewModel.assignedSacredTemple.observe(viewLifecycleOwner) { temples ->
            val userId = com.numberniceic.utils.UserContextManager.userX(requireContext())?.userId ?: "guest"
            
            if (!temples.isNullOrEmpty()) {
                // Valid list from API
                val displayItems = temples.filter { t ->
                    !com.numberniceic.data.local.NotificationStorage.isBlacklistedId(requireContext(), (t.id ?: 0).toLong())
                }.map { t ->
                     var photoUrl = t.imageUrl ?: ""
                     if (photoUrl.isNotEmpty() && !photoUrl.startsWith("http")) {
                         // Fix path: Move from /uploads/temple (broken) to /uploads/buddha/temple (working)
                         if (photoUrl.contains("/uploads/temple/")) {
                             photoUrl = photoUrl.replace("/uploads/temple/", "/uploads/buddha/temple/")
                         }
                         // Fix path if it contains /public (API returns it, but webroot is public)
                         if (photoUrl.startsWith("/public")) {
                             photoUrl = photoUrl.replace("/public", "")
                         }
                         
                         photoUrl = com.numberniceic.https.NetworkConfig.BASE_URL + (if (photoUrl.startsWith("/")) photoUrl else "/$photoUrl")
                     }
                     // Use server time or fallback to now if missing
                     val timeStr = if (!t.assignedAt.isNullOrEmpty()) t.assignedAt else java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                     
                     TempleItem(
                        id = t.id ?: 0,
                        name = t.templeName ?: "",
                        description = t.description ?: "",
                        photoUrl = photoUrl,
                        assignedAt = timeStr
                     )
                }
                
                // Sync Local Storage (Server is source of truth now)
                saveLocalTempleHistory(userId, displayItems)
                updateTempleListUI(displayItems)
            } else {
                // List is null or empty
                if (temples != null) {
                    // API returned empty list explicitly -> Clear UI
                    updateTempleListUI(emptyList())
                    // Optionally clear local history too if you want full sync
                    saveLocalTempleHistory(userId, emptyList())
                } else {
                    // Null Response (Network Error) -> Fallback to Local
                    val localList = loadLocalTempleHistory(userId)
                    if (localList.isNotEmpty()) {
                        updateTempleListUI(localList)
                    } else {
                        updateTempleListUI(emptyList())
                    }
                }
            }
        }

        viewModel.meritNotifications.observe(viewLifecycleOwner) {
            updateDynamicNotifications()
        }

        viewModel.changeNotifications.observe(viewLifecycleOwner) {
            updateDynamicNotifications()
        }

        viewModel.spellNotifications.observe(viewLifecycleOwner) {
            loadAssignedSpells()
        }

        // Assigned Inauspicious Data
        viewModel.assignedInauspicious.observe(viewLifecycleOwner) { list ->
            // Filter Data
            val yearList = list?.filter { it.type == "year" }?.reversed() ?: emptyList()
            val lifeList = list?.filter { it.type == "life" }?.reversed() ?: emptyList()

            // Separate Headers Logic
            binding.txtHeaderInauspiciousYear.text = "วันอัปมงคล เดือนอัปมงคล และทิศอัปมงคลในปีนี้${getAgeHeaderSuffix()}"
            
            binding.headerInauspiciousLife.isVisible = true
            binding.txtHeaderInauspiciousLife.text = "วันอัปมงคล เดือนอัปมงคล และทิศอัปมงคลตลอดชีวิต"

            // Hide Combined Placeholder ALWAYS
            binding.combinedInauspiciousPlaceholder.isVisible = false

            if (yearList.isEmpty() && lifeList.isEmpty()) {
                // Both missing or Age threshold reached -> Show Individual Placeholders
                // Note: If threshold reached, we specifically hide Year data. 
                // Life data might still be allowed but usually annual sections are the ones needing renewal.
                binding.defaultInauspiciousYearView.isVisible = true
                binding.containerInauspiciousYearList.isVisible = false
                
                if (lifeList.isEmpty()) {
                    binding.defaultInauspiciousLifeView.isVisible = true
                    binding.containerInauspiciousLifeList.isVisible = false
                } else {
                    binding.defaultInauspiciousLifeView.isVisible = false
                    binding.containerInauspiciousLifeList.isVisible = true
                    binding.containerInauspiciousLifeList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                    binding.containerInauspiciousLifeList.adapter = InauspiciousAdapter(lifeList)
                }
            } else {
                // Year Part
                if (yearList.isNotEmpty()) {
                    binding.containerInauspiciousYearList.isVisible = true
                    binding.defaultInauspiciousYearView.isVisible = false
                    binding.containerInauspiciousYearList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                    binding.containerInauspiciousYearList.adapter = InauspiciousAdapter(yearList)
                    
                    try {
                         val loggedInUser = com.numberniceic.utils.UserContextManager.userX(requireContext())
                         if (loggedInUser != null) {
                             val userIdStr = loggedInUser.userId ?: "guest"
                             androidx.work.WorkManager.getInstance(requireContext())
                                 .cancelUniqueWork("InauspiciousExpiry_$userIdStr")
                         }
                    } catch (e: Exception) {}
                } else {
                    binding.containerInauspiciousYearList.isVisible = false
                    binding.defaultInauspiciousYearView.isVisible = true
                }

                // Life Part
                if (lifeList.isNotEmpty()) {
                    binding.containerInauspiciousLifeList.isVisible = true
                    binding.defaultInauspiciousLifeView.isVisible = false
                    binding.containerInauspiciousLifeList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                    binding.containerInauspiciousLifeList.adapter = InauspiciousAdapter(lifeList)
                } else {
                    binding.containerInauspiciousLifeList.isVisible = false
                    binding.defaultInauspiciousLifeView.isVisible = true
                }
            }
        }

        // Assigned Auspicious Data
        viewModel.assignedAuspicious.observe(viewLifecycleOwner) { list ->
            val yearList = list?.filter { it.type == "year" }?.reversed() ?: emptyList()
            val lifeList = list?.filter { it.type == "life" }?.reversed() ?: emptyList()

            binding.txtHeaderAuspiciousYear.text = "วันมงคล เดือนมงคล และทิศมงคลในปีนี้${getAgeHeaderSuffix()}"
            binding.txtHeaderAuspiciousLife.text = "วันมงคล เดือนมงคล และทิศมงคลตลอดชีวิต"

            // Year Part
            if (yearList.isNotEmpty()) {
                binding.containerAuspiciousYearList.isVisible = true
                binding.defaultAuspiciousYearView.isVisible = false
                binding.containerAuspiciousYearList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                binding.containerAuspiciousYearList.adapter = AuspiciousAdapter(yearList)
            } else {
                binding.containerAuspiciousYearList.isVisible = false
                binding.defaultAuspiciousYearView.isVisible = true
            }

            // Life Part
            if (lifeList.isNotEmpty()) {
                binding.containerAuspiciousLifeList.isVisible = true
                binding.defaultAuspiciousLifeView.isVisible = false
                binding.containerAuspiciousLifeList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                binding.containerAuspiciousLifeList.adapter = AuspiciousAdapter(lifeList)
            } else {
                binding.containerAuspiciousLifeList.isVisible = false
                binding.defaultAuspiciousLifeView.isVisible = true
            }
        }
    }
    
    // Generic UI Update for MiraDoV2
    private fun updateMiracleUI(miracledo: com.numberniceic.data.persons.MiracleDoV2?, type: String) {
        if (miracledo == null) return
        
        val strActivity = StringBuilder()
        val txtView = when(type) { "สระผม" -> binding.txtSapom; "ตัดเล็บ" -> binding.txtCutNail; "ตัดผม" -> binding.txtCuthair; else -> return }
        val imgIcon = when(type) { "สระผม" -> binding.imgSapom; "ตัดเล็บ" -> binding.imgCutNail; "ตัดผม" -> binding.imgCuthair; else -> return }
        val imgDo = when(type) { "สระผม" -> binding.imgDoSapom; "ตัดเล็บ" -> binding.imgDoNail; "ตัดผม" -> binding.imgDoHaircut; else -> return }
        val chipRead = when(type) { "สระผม" -> binding.chipReadmoreSapom; "ตัดเล็บ" -> binding.chipReadmoreCutnail; "ตัดผม" -> binding.chipReadmoreCuthair; else -> return }
        
        if (miracledo.domira?.activity == "karee") {
            strActivity.append("คุณ \"ห้าม$type\" ในวันนี้ ไม่ดี")
            txtView.text = strActivity.toString()
            txtView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_txt))
            imgIcon.setImageResource(R.drawable.delete)
            imgDo.isVisible = false
        } else {
             if (miracledo.wanpra) {
                 strActivity.append("คุณ \"ห้าม$type\" ในวันพระ ไม่ดี")
                 txtView.text = strActivity.toString()
                 txtView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_txt))
                 imgIcon.setImageResource(R.drawable.delete)
                 imgDo.isVisible = false
             } else if (miracledo.domira?.activity == type) {
                 if (miracledo.domira?.action == "0") {
                     strActivity.append("คุณ \"ห้าม$type\" ในวันนี้ ไม่ดี")
                     txtView.text = strActivity.toString()
                     txtView.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_txt))
                     imgIcon.setImageResource(R.drawable.delete)
                     imgDo.isVisible = false
                 } else if (miracledo.domira?.action == "1") {
                     strActivity.append("คุณควร \"$type\" ในวันนี้")
                     txtView.text = strActivity.toString()
                     txtView.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_txt))
                     imgIcon.setImageResource(R.drawable.approval)
                 }
             }
        }
        
        chipRead.setOnClickListener {
             val userx = UserContextManager.userX(requireContext())
             if (userx == null) {
                 (parentFragment as? com.google.android.material.bottomsheet.BottomSheetDialogFragment)?.dismiss()
                 (activity as? com.numberniceic.ui.AppActivity)?.showLoginBottomSheet()
                 return@setOnClickListener
             }

            val strDoDetail = if (miracledo.wanpra) {
                "วันนี้วันพระไม่ควร$type คนที่$type วันนี้ มักจะอายุสั้น ล้มป่วยไม่สบายง่าย และยังทำให้มีเรื่องเดือดร้อนเข้ามา อีกทั้งยังเกิดอุบัติเหตุได้ง่าย"
            } else {
                miracledo.domira?.miraDesc
            }
            val miraDiaryF = MiraDiaryF.newInstance(strDoDetail ?: "")
            miraDiaryF.show(childFragmentManager, "MiraDiaryF")
        }
    }




    private fun initDressColor3Day() {
        val openPreview = openPreview@{
            val userx = UserContextManager.userX(requireContext())
            if (userx == null) {
                (activity as? com.numberniceic.ui.AppActivity)?.showLoginBottomSheet()
                return@openPreview
            }

            val birthDay = userx.birthDay
            if (birthDay.isNullOrBlank()) {
                Toast.makeText(requireContext(), "กรุณาระบุวันเกิดก่อนดูสีเสื้อผ้าล่วงหน้า", Toast.LENGTH_SHORT).show()
                startActivity(Intent(context, BirthDayAct::class.java))
                return@openPreview
            }

            try {
                DateTime.parse(birthDay)
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "รูปแบบวันเกิดไม่ถูกต้อง กรุณาตั้งค่าใหม่", Toast.LENGTH_SHORT).show()
                startActivity(Intent(context, BirthDayAct::class.java))
                return@openPreview
            }

            if (childFragmentManager.findFragmentByTag("Clothcolor3df") == null) {
                Clothcolor3df.newInstance().show(childFragmentManager, "Clothcolor3df")
            }
        }

        binding.layoutColorSeemongkolD.setOnClickListener {
            openPreview()
        }
        binding.layoutColorSeemongkolR.setOnClickListener {
            openPreview()
        }
    }

    private fun initRengYam(context: Context) {
        // Date bar visible for everyone
        binding.linearRengyam.isVisible = true
    }

    private fun startRengyamMagicAnimation() {
        val target = binding.linearRengyam
        stopRengyamMagicAnimation()

        val density = resources.displayMetrics.density
        
        // Use the original background (color #F2DF5E) and no elevation as requested
        target.setBackgroundColor(Color.parseColor("#F2DF5E"))
        target.elevation = 0f 
        
        // 2. Create the Shimmer/Light Beam Layer (Diagonal Sheen)
        val shimmerHighlight = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            gradientType = GradientDrawable.LINEAR_GRADIENT
            orientation = GradientDrawable.Orientation.TL_BR // Diagonal Sheen
            colors = intArrayOf(
                Color.parseColor("#00FFFFFF"), // Transparent
                Color.parseColor("#90FFFFFF"), // Bright Semi-Transparent White
                Color.parseColor("#00FFFFFF")  // Transparent
            )
        }
        
        rengyamSparkleDrawable = shimmerHighlight
        target.overlay.add(shimmerHighlight)

        // 3. Animate the light beam sweep
        target.post {
            val sweepAnim = ValueAnimator.ofFloat(-1.5f, 1.5f).apply {
                duration = 3000
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    val t = animator.animatedValue as Float
                    val w = target.width.toFloat()
                    val h = target.height.toFloat()
                    if (w <= 0f) return@addUpdateListener

                    val highlightWidth = w * 0.4f // Width of the beam
                    val x = w * t // Current center position
                    
                    // Set bounds for a wide vertical streak passing through
                    shimmerHighlight.setBounds(
                        (x - highlightWidth / 2).toInt(),
                        0,
                        (x + highlightWidth / 2).toInt(),
                        h.toInt()
                    )
                }
            }

            rengyamMagicAnimator = AnimatorSet().apply {
                playTogether(sweepAnim)
                start()
            }
        }
    }

    private fun stopRengyamMagicAnimation() {
        rengyamMagicAnimator?.cancel()
        rengyamMagicAnimator = null
        rengyamSparkleDrawable?.let { binding.linearRengyam.overlay.remove(it) }
        rengyamSparkleDrawable = null
    }

    private fun setCurrentDate() {
        val dt = DateTime()
        val dayLabelEng = dt.dayOfWeek().getAsText(java.util.Locale.ENGLISH)
        val thaiDay = PersonContextManager.toThaiDay(dayLabelEng)
        val day = if (thaiDay == "") dt.dayOfWeek().asText else thaiDay
        val monthLabelEng = dt.monthOfYear().getAsText(java.util.Locale.ENGLISH)
        val month = if (PersonContextManager.toThaiMonth(monthLabelEng)[1] == "") PersonContextManager.monthTH2FullTH(dt.monthOfYear().asText)[1] else PersonContextManager.toThaiMonth(monthLabelEng)[1]

        binding.txtCurrentDay2.text = day
        binding.txtCurrentDaynum2.text = dt.dayOfMonth().asText
        binding.txtCurrentMount2.text = month
        binding.txtCurrentYear2.text = (dt.year().asText.toInt() + 543).toString()
    }

    private fun setBagColor(bagColor: List<BagColor>?, age: String?) {
        val hasLucky = binding.txtLuckyNumber.visibility == View.VISIBLE && !binding.txtLuckyNumber.text.isNullOrEmpty()
        
        if (bagColor == null) {
            if (hasLucky) {
                // If we have lucky numbers, we show linearColorBag (which contains lucky numbers)
                // but show placeholder text for the bag part.
                binding.defaultBagView.visibility = View.GONE
                binding.linearColorBag.visibility = View.VISIBLE
                
                binding.txtBagDesc.text = "กรุณาติดต่อเปิดดวงประจำปี เพื่อทราบสีกระเป๋ามงคลของท่านในปีนี้"
                try {
                    binding.txtBagDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_txt))
                } catch (e: Exception) {}
                
                // Set default/gray for bag dots if no data
                binding.bagcolor01.setCardBackgroundColor(Color.LTGRAY)
                binding.bagcolor02.setCardBackgroundColor(Color.LTGRAY)
                binding.bagcolor03.setCardBackgroundColor(Color.LTGRAY)
                binding.bagcolor04.setCardBackgroundColor(Color.LTGRAY)
                binding.bagcolor05.setCardBackgroundColor(Color.LTGRAY)
                binding.bagcolor06.setCardBackgroundColor(Color.LTGRAY)
            } else {
                // Completely missing data: show the new modernized placeholder
                binding.defaultBagView.visibility = View.VISIBLE
                binding.linearColorBag.visibility = View.GONE
            }
            return
        }

        // Show data layout, hide placeholder
        binding.defaultBagView.visibility = View.GONE
        binding.linearColorBag.visibility = View.VISIBLE

        val userx = UserContextManager.userX(requireContext())
        val vipRole = userx?.vipcode?.lowercase() ?: ""

        // No age limit for bag colors if we have data

        if (UserContextManager.isAdmin(userx) || listOf("normal", "specialp", "silver", "gold", "diamond", "member").contains(vipRole) || (bagColor != null && bagColor.any { it.age.toString() == age && !it.bagColor1.isNullOrEmpty() })) {
            var foundCurrent = false
            for ((n, bag) in bagColor.withIndex()) {
                if (bag.age.toString() == age) {
                    foundCurrent = true
                    // Robust Parsing with Fallback
                    fun parse(hex: String?): Int {
                        if (hex.isNullOrEmpty()) return Color.LTGRAY
                        return try {
                            Color.parseColor(hex.trim())
                        } catch (e: Exception) {
                            android.util.Log.e("ColorDebug", "Parse Error: $hex", e)
                            Color.RED // Error indicator
                        }
                    }

                    val c1 = parse(bagColor[n].bagColor1)
                    val c2 = parse(bagColor[n].bagColor2)
                    val c3 = parse(bagColor[n].bagColor3)
                    val c4 = parse(bagColor[n].bagColor4)
                    val c5 = parse(bagColor[n].bagColor5)
                    val c6 = parse(bagColor[n].bagColor6)

                    binding.bagcolor01.setCardBackgroundColor(c1)
                    binding.bagcolor02.setCardBackgroundColor(c2)
                    binding.bagcolor03.setCardBackgroundColor(c3)
                    binding.bagcolor04.setCardBackgroundColor(c4)
                    binding.bagcolor05.setCardBackgroundColor(c5)
                    binding.bagcolor06.setCardBackgroundColor(c6)
                    
                    binding.txtBagDesc.text = "สีกระเป๋าเงินและกระเป๋าสะพายเสริมดวงชะตาของคุณประจำปีนี้ เรียงตามลำดับ คือ"
                    binding.txtBagDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_bag_desc_current))
                }
            }
            if (!foundCurrent) {
                 // Admin fallback: show the first available bag color set as a sample
                 val isAdmin = UserContextManager.isAdmin(userx)
                 if (isAdmin && !bagColor.isNullOrEmpty()) {
                     val first = bagColor.first()
                     fun parse(hex: String?): Int {
                         if (hex.isNullOrEmpty()) return Color.LTGRAY
                         return try { Color.parseColor(hex.trim()) } catch (e: Exception) { Color.LTGRAY }
                     }
                     binding.bagcolor01.setCardBackgroundColor(parse(first.bagColor1))
                     binding.bagcolor02.setCardBackgroundColor(parse(first.bagColor2))
                     binding.bagcolor03.setCardBackgroundColor(parse(first.bagColor3))
                     binding.bagcolor04.setCardBackgroundColor(parse(first.bagColor4))
                     binding.bagcolor05.setCardBackgroundColor(parse(first.bagColor5))
                     binding.bagcolor06.setCardBackgroundColor(parse(first.bagColor6))
                     binding.txtBagDesc.text = "ตัวอย่างสีกระเป๋าประจำปี (ตัวอย่างอายุ ${first.age})"
                     binding.txtBagDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.txt_bag_desc_current))
                 } else {
                     binding.txtBagDesc.text = "กรุณาติดต่อเปิดดวงประจำปี เพื่อทราบสีกระเป๋ามงคลของท่านในปีนี้"
                     binding.txtBagDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_txt))
                 }
            }
        } else {
            binding.txtBagDesc.text = "ติดต่อคุณนินเพื่อเปิดดวงประจำปี เพื่อทราบสีกระเป๋ามงคลของท่านในปีนี้"
            binding.txtBagDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_txt))
        }
    }

    private fun initDataUserCalculationsOnly() {
        val userx = UserContextManager.userX(requireContext())
        if (userx != null) {
            binding.linearDoView.isVisible = true
            binding.linearColorBag.isVisible = true
            binding.txtNameUser.text = "คุณ${userx.realName}"
            binding.txtUserBirthPrefix.text = "คุณเกิด"


            val birthday = PersonContextManager.parseBirthdayOrNull(userx.birthDay)
            if (birthday != null) {
                this.ageNextYang = PersonContextManager.ageYang(birthday.year, birthday.monthOfYear, birthday.dayOfMonth)
                this.ageCurrent = PersonContextManager.ageCurrent(birthday.year, birthday.monthOfYear, birthday.dayOfMonth)

                if (this.ageNextYang != null) {
                    binding.txtAgeYang.text = "อายุ $ageCurrent ปี ย่าง $ageNextYang ปี"
                    buddhaAnnualAdapter?.updateAge(ageCurrent, ageNextYang)
                }

                val d = birthday.dayOfWeek()
                val dayBirth = d.getAsText(Locale.ENGLISH)
                val dayNumBirth = PersonContextManager.convertDayEngToNum(dayBirth)
                val dayBirthNumber = if (userx.sHour <= 4) {
                    if (dayNumBirth == 1) 7 else dayNumBirth - 1
                } else {
                    dayNumBirth
                }
                
                val dayBirthEng = PersonContextManager.convertDayNumToEng(dayBirthNumber)
                if (dayBirthEng != null) {
                    val birthDayString = if (PersonContextManager.toThaiDay(dayBirthEng) == "") dayBirth else PersonContextManager.toThaiDay(dayBirthEng)
                    if (userx.sHour in 6..17) binding.imgBirthLight.setImageResource(R.drawable.sunsun) else binding.imgBirthLight.setImageResource(R.drawable.moon)
                    binding.txtDayBirth.text = birthDayString
                }

                // Components handled by setBagColor and other logic
                setBagColor(bagColorCollDao?.bagColor, ageCurrent.toString())
                binding.linearDoView.isVisible = true
                binding.frameNameUser.isVisible = true
                
                setupBuddhaSection(userx)
                setupInauspiciousSection(userx)

            } else {
                binding.txtDayBirth.isVisible = false
                binding.chipDayBirth.isVisible = true
                binding.frameNameUser.isVisible = true // Show name even if no birthday
            }
            // visibility is managed by setBagColor and setup sections
            // binding.btnContactNinGuest.isVisible = false 

        } else {
            // Guest Mode: Show components with placeholders
            setBagColor(null, null)
            binding.linearDoView.isVisible = true
            binding.frameNameUser.isVisible = true
            binding.linearRengyam.isVisible = true
            
            binding.txtNameUser.text = "ยินดีต้อนรับ"
            binding.txtUserBirthPrefix.text = "คุณ"
            binding.txtDayBirth.text = "บุคคลทั่วไป"
            
            binding.txtBagDesc.text = "ติดต่อเปิดดวงเพื่อดูสีกระเป๋าของคุณในปีนี้"
            try {
                binding.txtBagDesc.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_txt))
            } catch (e: Exception) {}

            // Open Chat when clicking the text
            binding.txtBagDesc.setOnClickListener {
                startActivity(Intent(context, com.numberniceic.ui.chat.ChatActivity::class.java))
            }
            

            
            // Initialize Buddha Section for Guest
            setupBuddhaSection(null)
            setupInauspiciousSection(null)

            // Guest Listeners for Miracle Do (Because... buttons)
            val guestListener = View.OnClickListener {
                (parentFragment as? com.google.android.material.bottomsheet.BottomSheetDialogFragment)?.dismiss()
                (activity as? com.numberniceic.ui.AppActivity)?.showLoginBottomSheet()
            }
            binding.chipReadmoreSapom.setOnClickListener(guestListener)
            binding.chipReadmoreCutnail.setOnClickListener(guestListener)
            binding.chipReadmoreCuthair.setOnClickListener(guestListener)
            binding.chipReadmoreParmai.setOnClickListener(guestListener)

            // 🛡️ Safety: Cancel any existing expiration workers that might be lingering (the "1 minute" bug)
            try {
               androidx.work.WorkManager.getInstance(requireContext()).cancelUniqueWork("InauspiciousExpiry_guest")
               // Also try cancelling for generic/potential previous IDs if possible, but specific ID required.
            } catch (e: Exception) {}
        }
    }

    private fun setChipColorRNew(colorSortxR: ArrayList<ColorStateList>?) {
        renderDynamicColorChips(binding.chipContainerBadToday, colorSortxR)
    }

    private fun setChipColorDNew(colorSortxD: ArrayList<ColorStateList>?) {
        renderDynamicColorChips(binding.chipContainerGoodToday, colorSortxD)
    }

    private fun renderDynamicColorChips(container: LinearLayout, colors: ArrayList<ColorStateList>?) {
        container.removeAllViews()
        if (colors.isNullOrEmpty()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        colors.forEachIndexed { index, colorStateList ->
            val dot = CardView(requireContext())
            val size = dpToPx(15f)
            val params = LinearLayout.LayoutParams(size, size)
            if (index > 0) {
                params.marginStart = dpToPx(3f)
            }
            dot.layoutParams = params
            dot.radius = dpToPx(7.5f).toFloat()
            dot.cardElevation = 0f
            dot.setCardBackgroundColor(colorStateList.defaultColor)
            container.addView(dot)
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }


    private fun setupBuddhaSection(userx: com.numberniceic.data.admin.Userx?) {
        // Guests or Users without data -> Show Grouped Placeholder by default
        binding.headerBuddhaAnnual.visibility = View.VISIBLE
        binding.txtHeaderBuddhaAnnual.text = "พระพุทธรูปที่ต้องถวายประจำปีนี้${getAgeHeaderSuffix()}"
        binding.headerBuddhaLifetime.visibility = View.VISIBLE
        binding.txtHeaderBuddhaLife.text = "พระพุทธรูปที่ต้องถวายตลอดชีวิต"
        
        binding.combinedBuddhaPlaceholder.visibility = View.GONE
        binding.defaultBuddhaAnnualView.visibility = View.VISIBLE
        binding.defaultBuddhaLifetimeView.visibility = View.VISIBLE
        binding.containerAnnualList.visibility = View.GONE
        binding.containerLifetimeList.visibility = View.GONE
    }

    private fun setupInauspiciousSection(userx: com.numberniceic.data.admin.Userx?) {
        // Separate Headers Logic (Guest/Default)
        binding.headerInauspiciousYear.isVisible = true
        binding.txtHeaderInauspiciousYear.text = "วันอัปมงคล เดือนอัปมงคล และทิศอัปมงคลในปีนี้${getAgeHeaderSuffix()}"
        
        binding.headerInauspiciousLife.isVisible = true
        binding.txtHeaderInauspiciousLife.text = "วันอัปมงคล เดือนอัปมงคล และทิศอัปมงคลตลอดชีวิต"
        
        binding.combinedInauspiciousPlaceholder.isVisible = false
        binding.defaultInauspiciousYearView.isVisible = true
        binding.defaultInauspiciousLifeView.isVisible = true
        
        binding.containerInauspiciousYearList.isVisible = false
        binding.containerInauspiciousLifeList.isVisible = false
    }

    private fun updateDynamicNotifications() {
        val userx = UserContextManager.userX(requireContext())

        // --- Zodiac Calculation Logic Start ---
        if (userx != null) {
            try {
                // 1. Get Birthday Month (and Day) safely from parsed birthday first.
                val parsedBirthday = PersonContextManager.parseBirthdayOrNull(userx.birthDay)

                if (parsedBirthday != null) {
                     var day = 0
                     var month = 0

                    val dobParts = userx.birthDay
                        ?.split(" ")
                        ?.firstOrNull()
                        ?.split("-")
                        ?.filter { it.isNotEmpty() }

                    if (dobParts != null && dobParts.size >= 3) {
                        // Simple heuristic: if parts[0] > 31 it's likely Year. YYYY-MM-DD
                        if ((dobParts[0].toIntOrNull() ?: 0) > 31) {
                            month = dobParts[1].toIntOrNull() ?: parsedBirthday.monthOfYear
                            day = dobParts[2].toIntOrNull() ?: parsedBirthday.dayOfMonth
                        } else {
                            // Format DD-MM-YYYY
                            day = dobParts[0].toIntOrNull() ?: parsedBirthday.dayOfMonth
                            month = dobParts[1].toIntOrNull() ?: parsedBirthday.monthOfYear
                        }
                    } else {
                        month = parsedBirthday.monthOfYear
                        day = parsedBirthday.dayOfMonth
                    }

                    // 2. Get Birth Time (Hour)
                    // The Userx class does not have 'birthTime' string field.
                    // It has 'sHour' (Int).
                    // We can use sHour directly.
                    
                    val birthHour = userx.sHour // Userx.sHour is an Int.
                    
                    // Note: If sHour is -1 or invalid, we might default to 6?
                    // But assume server sends valid hour 0-23
                    
                    // 3. Zodiac Table Logic (Thai Suriyayart - Fixed Table Approximation)
                    var baseZodiacIndex = 0 // Aries
                    
                    if ((month == 4 && day >= 13) || (month == 5 && day <= 13)) baseZodiacIndex = 0 // Aries
                    else if ((month == 5 && day >= 14) || (month == 6 && day <= 13)) baseZodiacIndex = 1 // Taurus
                    else if ((month == 6 && day >= 14) || (month == 7 && day <= 14)) baseZodiacIndex = 2 // Gemini
                    else if ((month == 7 && day >= 15) || (month == 8 && day <= 16)) baseZodiacIndex = 3 // Cancer
                    else if ((month == 8 && day >= 17) || (month == 9 && day <= 16)) baseZodiacIndex = 4 // Leo
                    else if ((month == 9 && day >= 17) || (month == 10 && day <= 16)) baseZodiacIndex = 5 // Virgo
                    else if ((month == 10 && day >= 17) || (month == 11 && day <= 15)) baseZodiacIndex = 6 // Libra
                    else if ((month == 11 && day >= 16) || (month == 12 && day <= 15)) baseZodiacIndex = 7 // Scorpio
                    else if ((month == 12 && day >= 16) || (month == 1 && day <= 15)) baseZodiacIndex = 8 // Sagittarius
                    else if ((month == 1 && day >= 16) || (month == 2 && day <= 12)) baseZodiacIndex = 9 // Capricorn
                    else if ((month == 2 && day >= 13) || (month == 3 && day <= 13)) baseZodiacIndex = 10 // Aquarius
                    else if ((month == 3 && day >= 14) || (month == 4 && day <= 12)) baseZodiacIndex = 11 // Pisces

                    
                    var timeOffset = 0
                    // Count slots from 05:00.
                    // 05-07 (0), 07-09 (1), 09-11 (2), 11-13 (3), 13-15 (4), 15-17 (5)
                    // 17-19 (6), 19-21 (7), 21-23 (8), 23-01 (9), 01-03 (10), 03-05 (11)
                    
                    if (birthHour in 5..23) {
                         if (birthHour >= 23) timeOffset = 9
                         else timeOffset = (birthHour - 5) / 2
                    } else {
                        // 00 to 04
                         if (birthHour < 1) timeOffset = 9 // 00:xx -> 23-01 slot
                         else if (birthHour < 3) timeOffset = 10 // 01-03
                         else timeOffset = 11 // 03-05
                    }

                    // Step 3.3: Calculate Final Zodiac Index
                    var finalZodiacIndex = (baseZodiacIndex + timeOffset) % 12
                    
                    val zodiacNamesRaw = arrayOf(
                        "เมษ", "พฤษภ", "เมถุน", "กรกฎ",
                        "สิงห์", "กันย์", "ตุลย์", "พิจิก",
                        "ธนู", "มังกร", "กุมภ์", "มีน"
                    )
                    
                    val zodiacEmojis = arrayOf(
                        "♈", "♉", "♊", "♋",
                        "♌", "♍", "♎", "♏",
                        "♐", "♑", "♒", "♓"
                    )
                    
                    val myLagna = zodiacNamesRaw[finalZodiacIndex]
                    val myLagnaEmoji = zodiacEmojis[finalZodiacIndex]
                    
                    val myBirthZodiac = zodiacNamesRaw[baseZodiacIndex]
                    // Optional: Emoji for birth zodiac too? Maybe just show text for clarity or both.
                    // Request: "เกิดราศี: เมถุน ลัคนาราศี: เมษ" (with emoji perhaps)
                    // The user said: "ตัดส่วนที่เป็นภาษาอังกฤกษออก"
                    
                     binding.cardZodiacDisplay.visibility = View.VISIBLE
                     // Format: ♊ ราศีเกิด: เมถุน  |  ♈ ลัคนา: เมษ
                     binding.cardZodiacDisplay.visibility = View.VISIBLE
                     // Format: ราศีเกิด: เมถุน  |  ลัคนา: เมษ
                     binding.txtZodiacName.text = "เกิดลัคนา:$myLagna ราศี:$myBirthZodiac"


                     
                } else {
                     binding.cardZodiacDisplay.visibility = View.GONE
                }
            } catch (e: Exception) {
                // Log.e("ZodiacCalc", "Error: ${e.message}")
                binding.cardZodiacDisplay.visibility = View.GONE
            }
        }
        // --- Zodiac Calculation Logic End ---

        val context = context ?: return
        
        val merits = viewModel.meritNotifications.value ?: emptyList()
    val changes = viewModel.changeNotifications.value ?: emptyList()

    binding.headerMeritMethod.visibility = View.VISIBLE

    updateMeritListUI(mergeAndMapMerit(com.numberniceic.data.local.NotificationStorage.getAllByType(context, "webview_merit"), merits))

    binding.headerChangeSteps.visibility = View.VISIBLE

    updateChangeListUI(mergeAndMapChange(com.numberniceic.data.local.NotificationStorage.getAllByType(context, "webview_changenum"), changes))

    // 3. Spells (Also refresh)
    loadAssignedSpells()
}

    private fun mergeAndMapMerit(local: List<com.numberniceic.data.local.NotiModel>, server: List<NotificationResponse>): List<MeritItem> {
        // Map server items first (they are the source of truth when available)
        val serverItems = server
            .filter { !com.numberniceic.data.local.NotificationStorage.isBlacklistedId(requireContext(), it.id.toLong()) }
            .map { res ->
                MeritItem(
                    id = res.id,
                    title = res.title,
                    content = res.body ?: "",
                    url = res.url ?: "",
                    photoUrl = "",
                    createdAt = res.createdAt
                )
            }
        
        // If server has data, use only server data (no merge needed - avoids duplicates)
        if (serverItems.isNotEmpty()) {
            return serverItems
        }
        
        // Fallback: use local FCM notifications only if server returned nothing
        return local.map { noti ->
            MeritItem(
                id = null,
                title = noti.title,
                content = noti.body,
                url = noti.url ?: "",
                photoUrl = "",
                createdAt = noti.dateDisplay
            )
        }
    }

    private fun mergeAndMapChange(local: List<com.numberniceic.data.local.NotiModel>, server: List<NotificationResponse>): List<ChangeItem> {
        // Map server items first (they are the source of truth when available)
        val serverItems = server
            .filter { !com.numberniceic.data.local.NotificationStorage.isBlacklistedId(requireContext(), it.id.toLong()) }
            .map { res ->
                ChangeItem(
                    id = res.id,
                    title = res.title,
                    content = res.body ?: "",
                    url = res.url ?: "",
                    photoUrl = "",
                    createdAt = res.createdAt
                )
            }
        
        // If server has data, use only server data (no merge needed - avoids duplicates)
        if (serverItems.isNotEmpty()) {
            return serverItems
        }
        
        // Fallback: use local FCM notifications only if server returned nothing
        return local.map { noti ->
            ChangeItem(
                id = null,
                title = noti.title,
                content = noti.body,
                url = noti.url ?: "",
                photoUrl = "",
                createdAt = noti.dateDisplay
            )
        }
    }





    fun showWidgetOnVip(show: Boolean) {
        binding.linearRengyam.isVisible = show
        binding.linearDoView.isVisible = show
        binding.frameNameUser.isVisible = show
        // Bag, Buddha and others now handle their own default/dynamic visibility in their update methods
    }

    private fun setupComposeChatIcon(composeView: androidx.compose.ui.platform.ComposeView, size: Int) {
        composeView.apply {
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            post {
                try {
                    setContent {
                        com.numberniceic.ui.components.GoldShimmerChatIcon(size = size.dp)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PersonNewsF", "Failed to set Compose content", e)
                }
            }
        }
    }

    private fun loadAssignedSpells() {
        val context = context ?: return
        val memberId = com.numberniceic.utils.UserContextManager.userX(context)?.userId
        if (memberId == null) {
            processSpellResponse(null)
            return
        }
        
        android.util.Log.d("PersonNewsF", "loadAssignedSpells for memberId=$memberId")
 
        val call = RetrofitClient.getInstance().api.getAssignedSpells(memberId)
        call.enqueue(object : retrofit2.Callback<com.google.gson.JsonObject> {
            override fun onResponse(call: Call<com.google.gson.JsonObject>, response: Response<com.google.gson.JsonObject>) {
                val json = response.body()
                if (response.isSuccessful && json != null) {
                    processSpellResponse(json)
                } else {
                    processSpellResponse(null)
                }
            }
            override fun onFailure(call: Call<com.google.gson.JsonObject>, t: Throwable) {
                processSpellResponse(null)
            }
        })
    }
    
    // Fallback method
    private fun loadFallbackLatest() {
        val memberId = com.numberniceic.utils.UserContextManager.userX(context ?: return)?.userId
        val call = RetrofitClient.getInstance().api.getLatestSpell(memberId)
        call.enqueue(object : retrofit2.Callback<com.google.gson.JsonObject> {
             override fun onResponse(call: Call<com.google.gson.JsonObject>, response: Response<com.google.gson.JsonObject>) {
                 val json = response.body()
                 processSpellResponse(json)
             }
             override fun onFailure(call: Call<com.google.gson.JsonObject>, t: Throwable) {
                 processSpellResponse(null)
             }
        })
    }

    private fun showDefaultSpell() {
        binding.headerSpells.visibility = View.VISIBLE
        binding.defaultSpellsView.visibility = View.VISIBLE
        binding.containerSpellsList.visibility = View.GONE

    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView, adapter: DynamicNotificationAdapter) {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val context = context ?: return
                val position = viewHolder.adapterPosition
                val removedItem = adapter.removeItem(position)
                com.numberniceic.data.local.NotificationStorage.deleteNotification(context, removedItem)
                
                // Server Delete
                if (removedItem.id != 0L) {
                    val type = if (removedItem.type?.contains("change") == true) "change" else "merit"
                    deleteAssignmentFromServer(type, removedItem.id.toString())
                }

                if (adapter.itemCount == 0) {
                    updateDynamicNotifications()
                }
                Toast.makeText(context, "ลบรายการแล้ว", Toast.LENGTH_SHORT).show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun setupAdapters() {
        val context = context ?: return
        
        // 1. Merit
        meritAdapter = DynamicNotificationAdapter(mutableListOf()) { noti ->
            handleNotiClick(noti)
        }
        binding.containerMeritList.layoutManager = LinearLayoutManager(context)
        binding.containerMeritList.adapter = meritAdapter
        setupSwipeToDelete(binding.containerMeritList, meritAdapter!!)

        // 2. Change
        changeAdapter = DynamicNotificationAdapter(mutableListOf()) { noti ->
            handleNotiClick(noti)
        }
        binding.containerChangeList.layoutManager = LinearLayoutManager(context)
        binding.containerChangeList.adapter = changeAdapter
        setupSwipeToDelete(binding.containerChangeList, changeAdapter!!)

        // 3. Temple
        templeAdapter = TempleAdapter(mutableListOf()) { temple ->
            // Show Temple Details using Dialog
            val miraDiaryF = com.numberniceic.ui.apersonnews.MiraDiaryF.newInstance(temple.description, temple.name)
            miraDiaryF.show(childFragmentManager, "MiraDiaryF")
        }
        binding.containerTempleList.layoutManager = LinearLayoutManager(context)
        binding.containerTempleList.adapter = templeAdapter
        setupSwipeToDeleteTemple(binding.containerTempleList, templeAdapter!!)

        // 4. Spells
        spellAdapter = SpellAdapter(mutableListOf()) { spell ->
            val miraDiaryF = com.numberniceic.ui.apersonnews.MiraDiaryF.newInstance(spell.content ?: "", spell.title, spell.note)
            miraDiaryF.show(childFragmentManager, "MiraDiaryF")
        }
        binding.containerSpellsList.layoutManager = LinearLayoutManager(context)
        binding.containerSpellsList.adapter = spellAdapter
        setupSwipeToDeleteSpell(binding.containerSpellsList, spellAdapter!!)

        // 5. Buddha Annual
        buddhaAnnualAdapter = BuddhaAdapter(mutableListOf(), ageCurrent, ageNextYang) { pang ->
             val miraDiaryF = com.numberniceic.ui.apersonnews.MiraDiaryF.newInstance(pang.description ?: "", pang.pangName ?: "พระพุทธรูป")
             miraDiaryF.show(childFragmentManager, "MiraDiaryF")
        }
        binding.containerAnnualList.layoutManager = LinearLayoutManager(context)
        binding.containerAnnualList.adapter = buddhaAnnualAdapter
        setupSwipeToDeleteBuddha(binding.containerAnnualList, buddhaAnnualAdapter!!, "annual")

        // 6. Buddha Lifetime
        buddhaLifetimeAdapter = BuddhaAdapter(mutableListOf(), ageCurrent, ageNextYang) { pang ->
             val miraDiaryF = com.numberniceic.ui.apersonnews.MiraDiaryF.newInstance(pang.description ?: "", pang.pangName ?: "พระพุทธรูป")
             miraDiaryF.show(childFragmentManager, "MiraDiaryF")
        }
        binding.containerLifetimeList.layoutManager = LinearLayoutManager(context)
        binding.containerLifetimeList.adapter = buddhaLifetimeAdapter
        setupSwipeToDeleteBuddha(binding.containerLifetimeList, buddhaLifetimeAdapter!!, "lifetime")
    }

    private fun handleNotiClick(noti: com.numberniceic.data.local.NotiModel) {
        if (noti.type == "webview_merit" || noti.type == "webview_changenum") {
            var url = noti.url
            if (url.isNullOrBlank()) {
                url = if (noti.type == "webview_merit") {
                    "${com.numberniceic.https.NetworkConfig.BASE_URL}/merit/view?id=${noti.id}"
                } else {
                    "${com.numberniceic.https.NetworkConfig.BASE_URL}/changenum/view?id=${noti.id}"
                }
            }
            
            val sheet = com.numberniceic.ui.merit.MeritWebViewBottomSheet.newInstance(url)
            sheet.show(childFragmentManager, "MeritWebViewBottomSheet")
        } else if (!noti.url.isNullOrEmpty()) {
            val intent = android.content.Intent(requireContext(), com.numberniceic.ui.news.NewsAct::class.java)
            intent.putExtra("url", noti.url)
            startActivity(intent)
        }
        
        // Mark Read
        try {
            com.numberniceic.data.local.NotificationStorage.markReadByContent(requireContext(), noti.title, noti.body)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun setupSwipeToDeleteTemple(recyclerView: RecyclerView, adapter: TempleAdapter) {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val context = context ?: return
                val position = viewHolder.adapterPosition
                val removed = adapter.removeItem(position)
                
                // Server Delete
                if (removed.id != 0) {
                    deleteAssignmentFromServer("temple", removed.id.toString())
                    val proxyNoti = com.numberniceic.data.local.NotiModel(id = removed.id.toLong(), title = "", body = "", date = 0L)
                    com.numberniceic.data.local.NotificationStorage.deleteNotification(context, proxyNoti)
                }
                
                Toast.makeText(context, "ลบวัดที่แนะนำแล้ว", Toast.LENGTH_SHORT).show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun setupSwipeToDeleteSpell(recyclerView: RecyclerView, adapter: SpellAdapter) {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val context = context ?: return
                val userId = com.numberniceic.utils.UserContextManager.userX(context)?.userId ?: "guest"
                val position = viewHolder.adapterPosition
                val removed = adapter.removeItem(position)
                
                // Delete from local history
                val history = loadLocalSpellHistory(userId).toMutableList()
                history.removeAll { it.title == removed.title && it.content == removed.content }
                saveLocalSpellHistory(userId, history)
                
                // Server Delete
                removed.id?.let {
                    deleteAssignmentFromServer("spell", it.toString())
                    val proxyNoti = com.numberniceic.data.local.NotiModel(id = it.toLongOrNull() ?: 0L, title = "", body = "", date = 0L)
                    com.numberniceic.data.local.NotificationStorage.deleteNotification(context, proxyNoti)
                }
                
                Toast.makeText(context, "ลบคาถาแล้ว", Toast.LENGTH_SHORT).show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun setupSwipeToDeleteBuddha(recyclerView: RecyclerView, adapter: BuddhaAdapter, type: String) {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val context = context ?: return
                val userId = com.numberniceic.utils.UserContextManager.userX(context)?.userId ?: "guest"
                val position = viewHolder.adapterPosition
                val removed = adapter.removeItem(position)
                
                // Delete from local history if it exists
                val history = loadLocalBuddhaHistory(userId, type).toMutableList()
                history.removeAll { it.id == removed.id }
                saveLocalBuddhaHistory(userId, type, history)
                
                // Server Delete
                if (removed.id != null && removed.id != 0) {
                    deleteAssignmentFromServer("buddha", removed.id.toString(), removed.id.toString(), type)
                    val proxyNoti = com.numberniceic.data.local.NotiModel(id = removed.id!!.toLong(), title = "", body = "", date = 0L)
                    com.numberniceic.data.local.NotificationStorage.deleteNotification(context, proxyNoti)
                }
                
                Toast.makeText(context, "ลบรายการแล้ว", Toast.LENGTH_SHORT).show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    private fun deleteAssignmentFromServer(type: String, id: String, buddhaId: String? = null, buddhaType: String? = null) {
        val context = context ?: return
        val memberId = com.numberniceic.utils.UserContextManager.userX(context)?.userId ?: return
        
        val body = com.google.gson.JsonObject().apply {
            addProperty("memberid", memberId)
            when (type) {
                "merit", "change" -> addProperty("id", id)
                "buddha" -> {
                    addProperty("buddha_id", buddhaId)
                    addProperty("assignment_type", buddhaType)
                }
                "temple" -> addProperty("temple_id", id)
                "spell" -> addProperty("spell_id", id)
            }
        }

        val call = when (type) {
            "merit", "change" -> com.numberniceic.https.RetrofitClient.api.deleteMeritAssignment(body)
            "buddha" -> com.numberniceic.https.RetrofitClient.api.deleteBuddhaAssignment(body)
            "temple" -> com.numberniceic.https.RetrofitClient.api.deleteTempleAssignment(body)
            "spell" -> com.numberniceic.https.RetrofitClient.api.deleteSpellAssignment(body)
            else -> null
        }

        call?.enqueue(object : retrofit2.Callback<com.google.gson.JsonObject> {
            override fun onResponse(call: retrofit2.Call<com.google.gson.JsonObject>, response: retrofit2.Response<com.google.gson.JsonObject>) {
                // Done
            }
            override fun onFailure(call: retrofit2.Call<com.google.gson.JsonObject>, t: Throwable) {
                // Done
            }
        })
    }

    // === Merit, Change, Temple Helper Functions ===
    
    data class MeritItem(val id: Int?, val title: String, val content: String, val url: String, val photoUrl: String, val createdAt: String)
    data class ChangeItem(val id: Int?, val title: String, val content: String, val url: String, val photoUrl: String, val createdAt: String)
    data class TempleItem(val id: Int, val name: String, val description: String, val photoUrl: String, val assignedAt: String)



    // Merit Functions
    private fun updateMeritListUI(items: List<MeritItem>) {
        binding.headerMeritMethod.visibility = View.VISIBLE
        binding.txtHeaderMeritMethod.text = "วิธีทำบุญเพื่อแก้ไขคุ้มครองดวงประจำปีนี้${getAgeHeaderSuffix()}"
        if (items.isEmpty()) {
            binding.containerMeritList.visibility = View.GONE
            binding.defaultMeritView.visibility = View.VISIBLE
        } else {
            binding.containerMeritList.visibility = View.VISIBLE
            binding.defaultMeritView.visibility = View.GONE
            
            val notis = items.map { 
                com.numberniceic.data.local.NotiModel(
                    id = it.id?.toLong() ?: 0,
                    title = it.title,
                    body = it.content,
                    url = it.url,
                    type = "webview_merit",
                    dateDisplay = it.createdAt
                )
            }
            meritAdapter?.updateItems(notis)
            binding.headerMeritMethod.visibility = View.VISIBLE
        }
    }

    // Change Functions
    private fun updateChangeListUI(items: List<ChangeItem>) {
        binding.headerChangeSteps.visibility = View.VISIBLE
        if (items.isEmpty()) {
            binding.containerChangeList.visibility = View.GONE
            binding.defaultChangeView.visibility = View.VISIBLE
        } else {
            binding.containerChangeList.visibility = View.VISIBLE
            binding.defaultChangeView.visibility = View.GONE
            
            val notis = items.map { 
                com.numberniceic.data.local.NotiModel(
                    id = it.id?.toLong() ?: 0,
                    title = it.title,
                    body = it.content,
                    url = it.url,
                    type = "webview_changenum",
                    dateDisplay = it.createdAt
                )
            }
            changeAdapter?.updateItems(notis)
            binding.headerChangeSteps.visibility = View.VISIBLE
        }
    }

    // Temple Functions
    private fun updateTempleListUI(items: List<TempleItem>) {
        binding.headerSacredTemple.visibility = View.VISIBLE
        if (items.isEmpty()) {
            binding.containerTempleList.visibility = View.GONE
            binding.defaultTempleView.visibility = View.VISIBLE
        } else {
            binding.containerTempleList.visibility = View.VISIBLE
            binding.defaultTempleView.visibility = View.GONE
            templeAdapter?.updateItems(items)
            binding.headerSacredTemple.visibility = View.VISIBLE
        }
    }

    private fun updateSpellsListUI(list: List<SpellItem>) {
        if (list.isEmpty()) {
            showDefaultSpell()
        } else {
            binding.headerSpells.visibility = View.VISIBLE

            binding.defaultSpellsView.visibility = View.GONE
            binding.containerSpellsList.visibility = View.VISIBLE
            spellAdapter?.updateItems(list)
        }
    }

    private fun processSpellResponse(json: com.google.gson.JsonObject?) {
        val context = context ?: return
        if (!isAdded) return
        val userId = com.numberniceic.utils.UserContextManager.userX(context)?.userId ?: "guest"
        
        if (json != null && json.has("status") && json.get("status").asString == "success") {
            try {
                val dataElement = json.get("data")
                val spellList = mutableListOf<SpellItem>()
                if (dataElement.isJsonArray) {
                    val arr = dataElement.asJsonArray
                    for (i in 0 until arr.size()) {
                        spellList.add(parseSpellObject(arr.get(i).asJsonObject))
                    }
                } else if (dataElement.isJsonObject) {
                    spellList.add(parseSpellObject(dataElement.asJsonObject))
                }

                if (spellList.isNotEmpty()) {
                    val history = loadLocalSpellHistory(userId).toMutableList()
                    var changed = false
                    for (newItem in spellList) {
                        val idLong = newItem.id?.toLongOrNull() ?: 0L
                        if (!com.numberniceic.data.local.NotificationStorage.isBlacklistedId(requireContext(), idLong) &&
                            !history.any { it.title == newItem.title && it.content == newItem.content }) {
                            history.add(0, newItem)
                            changed = true
                        }
                    }
                    if (changed) {
                        saveLocalSpellHistory(userId, if (history.size > 10) history.take(10) else history)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        updateSpellsListUI(loadLocalSpellHistory(userId))
    }

    private fun parseSpellObject(data: com.google.gson.JsonObject): SpellItem {
        val id = if (data.has("id")) data.get("id").asString else ""
        val title = if (data.has("title")) data.get("title").asString else ""
        val content = if (data.has("content")) data.get("content").asString else ""
        val note = if (data.has("note")) data.get("note").asString else ""
        val type = if (data.has("type")) data.get("type").asString else "spell"
        
        // Strategy: Always try to resolve relative 'photo' field first for local consistency
        var photoUrl = ""
        if (data.has("photo")) {
            val p = data.get("photo").asString
            if (p.isNotEmpty()) {
                photoUrl = com.numberniceic.utils.ImageUrlResolver.resolve(p)
            }
        }

        // Fallback to 'photo_url' if 'photo' was missing/empty
        if (photoUrl.isEmpty() && data.has("photo_url")) {
            val p = data.get("photo_url").asString
            if (p.isNotEmpty()) {
                photoUrl = com.numberniceic.utils.ImageUrlResolver.resolve(p)
            }
        }
        
        android.util.Log.d("PersonNewsF", "Parsed Spell: Title=$title, PhotoUrl=$photoUrl")
        
        return SpellItem(id, title, content, photoUrl, type, note)
    }


    private fun saveLocalSpellHistory(userId: String, list: List<SpellItem>) {
        try {
            val prefs = requireContext().getSharedPreferences("spells_history_$userId", Context.MODE_PRIVATE)
            prefs.edit().putString("data", com.google.gson.Gson().toJson(list)).apply()
        } catch (e: Exception) {}
    }

    private fun loadLocalSpellHistory(userId: String): List<SpellItem> {
        try {
            val prefs = requireContext().getSharedPreferences("spells_history_$userId", Context.MODE_PRIVATE)
            val json = prefs.getString("data", null) ?: return emptyList()
            val type = object : com.google.gson.reflect.TypeToken<List<SpellItem>>() {}.type
            return com.google.gson.Gson().fromJson(json, type)
        } catch (e: Exception) { return emptyList() }
    }

    private fun saveLocalTempleHistory(userId: String, list: List<TempleItem>) {
        try {
            val prefs = requireContext().getSharedPreferences("temple_history_$userId", Context.MODE_PRIVATE)
            prefs.edit().putString("history_data", com.google.gson.Gson().toJson(list)).apply()
        } catch (e: Exception) {}
    }

    private fun loadLocalTempleHistory(userId: String): List<TempleItem> {
        try {
            val prefs = requireContext().getSharedPreferences("temple_history_$userId", Context.MODE_PRIVATE)
            val json = prefs.getString("history_data", null) ?: return emptyList()
            val typeToken = object : com.google.gson.reflect.TypeToken<List<TempleItem>>() {}.type
            return com.google.gson.Gson().fromJson(json, typeToken)
        } catch (e: Exception) { return emptyList() }
    }

    private fun updateBuddhaAnnualListUI(pangList: List<com.numberniceic.data.admin.BuddhaPang>) {
        val limitAge = 44
        if (pangList.isEmpty()) {
            binding.containerAnnualList.visibility = View.GONE
            binding.defaultBuddhaAnnualView.visibility = View.VISIBLE
        } else {
            binding.defaultBuddhaAnnualView.visibility = View.GONE
            binding.containerAnnualList.visibility = View.VISIBLE
            buddhaAnnualAdapter?.updateItems(pangList)
            buddhaAnnualAdapter?.updateAge(ageCurrent, ageNextYang)
        }
    }

    private fun updateBuddhaLifetimeListUI(pangList: List<com.numberniceic.data.admin.BuddhaPang>) {
        binding.headerBuddhaLifetime.visibility = View.VISIBLE
        binding.txtHeaderBuddhaLife.text = "พระพุทธรูปที่ต้องถวายตลอดชีวิต"
        
        if (pangList.isEmpty()) {
            binding.containerLifetimeList.visibility = View.GONE
            binding.defaultBuddhaLifetimeView.visibility = View.VISIBLE
        } else {
            binding.defaultBuddhaLifetimeView.visibility = View.GONE
            binding.containerLifetimeList.visibility = View.VISIBLE
            buddhaLifetimeAdapter?.updateItems(pangList)
        }
    }

    private fun getAgeHeaderSuffix(): String {
        return if (ageCurrent != null && ageNextYang != null) "\n(อายุ $ageCurrent ย่าง $ageNextYang)" else ""
    }

    private fun saveLocalBuddhaHistory(userId: String, type: String, list: List<com.numberniceic.data.admin.BuddhaPang>) {
        try {
            val key = if (type == "annual") "buddha_history_$userId" else "buddha_history_${type}_$userId"
            requireContext().getSharedPreferences(key, Context.MODE_PRIVATE).edit().putString("history_data", com.google.gson.Gson().toJson(list)).apply()
        } catch (e: Exception) {}
    }

    private fun loadLocalBuddhaHistory(userId: String, type: String): List<com.numberniceic.data.admin.BuddhaPang> {
        try {
            val key = if (type == "annual") "buddha_history_$userId" else "buddha_history_${type}_$userId"
            val json = requireContext().getSharedPreferences(key, Context.MODE_PRIVATE).getString("history_data", null) ?: return emptyList()
            val typeToken = object : com.google.gson.reflect.TypeToken<List<com.numberniceic.data.admin.BuddhaPang>>() {}.type
            return com.google.gson.Gson().fromJson(json, typeToken)
        } catch (e: Exception) { return emptyList() }
    }

    private fun calculateMillisUntilNextBirthday(birthdayStr: String?): Long {
        android.util.Log.d("BirthdayCalc", "=== START CALCULATION ===")
        android.util.Log.d("BirthdayCalc", "Input birthday string: '$birthdayStr'")
        
        if (birthdayStr.isNullOrEmpty()) {
            android.util.Log.e("BirthdayCalc", "Birthday string is null or empty! Returning 0")
            return 0
        }
        
        try {
            // Support multiple separators and remove time part if present
            val dateOnly = birthdayStr.split(" ")[0]
            android.util.Log.d("BirthdayCalc", "Date only (after removing time): '$dateOnly'")
            
            val cleanStr = dateOnly.replace("/", "-").replace(".", "-")
            android.util.Log.d("BirthdayCalc", "Clean string (normalized separators): '$cleanStr'")
            
            val parts = cleanStr.split("-").filter { it.isNotEmpty() }
            android.util.Log.d("BirthdayCalc", "Parts after split: ${parts.joinToString(", ")}")
            
            if (parts.size < 3) {
                android.util.Log.e("BirthdayCalc", "Invalid format - less than 3 parts! Returning 24h")
                return 24L * 60 * 60 * 1000 // Default to 24h if invalid format
            }
            
            val month: Int
            val day: Int
            
            if (parts[0].length == 4) {
                // YYYY-MM-DD
                month = parts[1].toIntOrNull() ?: 1
                day = parts[2].toIntOrNull() ?: 1
                android.util.Log.d("BirthdayCalc", "Format detected: YYYY-MM-DD -> Month=$month, Day=$day")
            } else {
                // DD-MM-YYYY
                day = parts[0].toIntOrNull() ?: 1
                month = parts[1].toIntOrNull() ?: 1
                android.util.Log.d("BirthdayCalc", "Format detected: DD-MM-YYYY -> Month=$month, Day=$day")
            }

            val now = DateTime.now()
            android.util.Log.d("BirthdayCalc", "Current time: ${now.toString("yyyy-MM-dd HH:mm:ss")}")
            
            // Ensure month/day are valid ranges
            val safeMonth = month.coerceIn(1, 12)
            val lastDay = DateTime(now.year, safeMonth, 1, 0, 0).dayOfMonth().maximumValue
            val safeDay = day.coerceIn(1, lastDay)
            
            android.util.Log.d("BirthdayCalc", "Safe values: Month=$safeMonth, Day=$safeDay (max day in month=$lastDay)")
            
            var nextBirthday = DateTime(now.year, safeMonth, safeDay, 0, 0, 0, 1) // Start of day + 1ms
            android.util.Log.d("BirthdayCalc", "Next birthday (this year): ${nextBirthday.toString("yyyy-MM-dd HH:mm:ss")}")
            
            if (nextBirthday.isBeforeNow) {
                android.util.Log.d("BirthdayCalc", "Birthday already passed this year, moving to next year")
                nextBirthday = nextBirthday.plusYears(1)
                android.util.Log.d("BirthdayCalc", "Next birthday (next year): ${nextBirthday.toString("yyyy-MM-dd HH:mm:ss")}")
            }
            
            val delay = nextBirthday.millis - now.millis
            val delayDays = delay / (24L * 60 * 60 * 1000)
            val delayHours = (delay % (24L * 60 * 60 * 1000)) / (60L * 60 * 1000)
            
            android.util.Log.d("BirthdayCalc", "Calculated delay: $delay ms (~$delayDays days, $delayHours hours)")
            android.util.Log.d("BirthdayCalc", "=== END CALCULATION ===")
            
            return if (delay < 0) {
                android.util.Log.e("BirthdayCalc", "Negative delay! Returning 0")
                0
            } else delay
            
        } catch (e: Exception) {
            android.util.Log.e("BirthdayCalc", "EXCEPTION during calculation: ${e.message}", e)
            e.printStackTrace()
            return 24L * 60 * 60 * 1000 // Fallback to 24 hours instead of 1 minute
        }
    }
}
