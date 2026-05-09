package com.numberniceic.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.numberniceic.R
import com.numberniceic.adapters.TabAdapter
import com.numberniceic.databinding.AppMainBinding
import android.view.View
import androidx.compose.runtime.getValue
import com.numberniceic.ui.auth.UserRegisAct
import com.numberniceic.ui.apersonnews.PersonNewsF
import com.numberniceic.ui.apersonnews.DashboardBottomSheet
import com.numberniceic.ui.notification.NotificationBottomSheet
import com.numberniceic.ui.auth.MemberBottomSheet
import com.numberniceic.ui.auth.LoginBottomSheet
import com.numberniceic.data.local.NotificationStorage
import com.numberniceic.utils.UserContextManager
import com.numberniceic.BuildConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.JsonObject
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.numberniceic.ui.ChatComposeActivity
import com.numberniceic.utils.ChatNotificationManager
import com.numberniceic.utils.dismissAllBottomSheets
import com.numberniceic.utils.showSingle
import com.numberniceic.ui.components.VvipBadgeComponent
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner


class AppActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    var doubleBack = false
    private var personNoti: MenuItem? = null
    private var imm: InputMethodManager? = null
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private var chatMenuItem: MenuItem? = null
    private var shouldVibrateBell = false
    private var lastUnreadNotiCount = -1
    
    private lateinit var binding: AppMainBinding
    private lateinit var dashSheetBehavior: com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View>
    
    private val notificationReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                invalidateOptionsMenu()
                
                val title = intent?.getStringExtra("title") ?: ""
                val body = intent?.getStringExtra("body") ?: ""
                val type = intent?.getStringExtra("type") ?: ""
                val url = intent?.getStringExtra("url")
                
                Log.d("AppActivity", "Notification Received Broadcast: type=$type, title=$title, url=$url")
                
                // Set flag to vibrate bell on next update
                shouldVibrateBell = true
                
                // Update Chat Badge
                updateChatFabBadge()
                
                // 🎶 Play Sound Effect for Chat
                if (type == "chat" || type == "guest_chat" || type == "admin_message" || type == "customer_message") {
                    context?.let { com.numberniceic.utils.SoundManager.playChatSound(it) }
                    // 🚀 Trigger UI Refresh via StateManager
                    com.numberniceic.data.chat.AdminChatStateManager.refreshSignal++
                }
                
                if (isFinishing || isDestroyed) {
                    Log.w("AppActivity", "Skipping notification UI update because activity is finishing/destroyed")
                    return
                }

                // 1. Show Special Notification Bottom Sheet (Premium Popup)
                if (type == "custom" || type == "webview_merit" || type == "webview_changenum" || type == "order_success") {
                    showSpecialNotification(title, body, url)
                } else {
                    val isBagColor = title.contains("สีกระเป๋า") || body.contains("สีกระเป๋า") || type == "bag_color"
                    val isLuckyNum = title.contains("เลขนำโชค") || type == "lucky_number"
                    val isSpell = type == "webview_spell"

                    if (isBagColor || isLuckyNum || isSpell) {
                        showDashboard(forceRefresh = true)
                    } else {
                        val refreshIntent = Intent("com.numberniceic.REFRESH_DASHBOARD")
                        sendBroadcast(refreshIntent)
                    }
                }
                
                // 🚀 อัปเดตสถานะ VIP ทันทีที่ได้รับการแจ้งเตือน (ไม่ต้องรอ Polling 10 วิ)
                Log.d("AppActivity", "Notification received: Triggering immediate VIP status poll")
                pollVIPStatus()
            } catch (e: Exception) {
                Log.e("AppActivity", "notificationReceiver failed", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = AppMainBinding.inflate(layoutInflater)

            setContentView(binding.root)
            

            
            initInstances()
            
            // Init Admin Chat State Manager
            com.numberniceic.data.chat.AdminChatStateManager.init(this)
            com.numberniceic.utils.SoundManager.init(this)
            
            // TODO: ปิดการทดสอบอัตโนมัติชั่วคราว
            // Log.d("AppActivity", "Testing guest system...")
            // Handler(android.os.Looper.getMainLooper()).postDelayed({
            //     if (!isFinishing && !isDestroyed) {
            //         testGuestSystem()
            //     }
            // }, 3000)
            
            // // ทดสอบ Payment Flow (หลัง guest test 5 วินาที)
            // Handler(android.os.Looper.getMainLooper()).postDelayed({
            //     if (!isFinishing && !isDestroyed) {
            //         testPaymentFlow()
            //     }
            // }, 8000)
            
            if (savedInstanceState == null) {
                try {
                    val userx = UserContextManager.userX(this)
                    // Logic Change: Don't force login on launch. 
                    if (intent.getBooleanExtra("SHOW_LOGIN_BOTTOM_SHEET", false)) {
                        showLoginBottomSheet()
                    } else {
                        // Show Dashboard for EVERYONE (Guest or Logged In)
                        // Delay 1.2s to let UI render first, so user sees it "Slide Up"
                        Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (!isFinishing && !isDestroyed) {
                                showDashboard()
                            }
                        }, 1200)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            checkAndUpdateFcmToken()
            
            checkNotificationPermission()
            
            handleIntentExtras(intent)
            
            // 🎯 Initialize Persistent Bottom Sheet
            dashSheetBehavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(binding.persistentDashSheet)
            dashSheetBehavior.isHideable = true
            dashSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
            dashSheetBehavior.peekHeight = 0
            
            // Set Max Height (Lowered to 80% as requested)
            val metrics = resources.displayMetrics
            dashSheetBehavior.maxHeight = (metrics.heightPixels * 0.80).toInt()

            dashSheetBehavior.addBottomSheetCallback(object : com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: android.view.View, newState: Int) {
                    if (newState == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN) {
                        binding.persistentDashSheet.visibility = android.view.View.GONE
                    }
                }
                override fun onSlide(bottomSheet: android.view.View, slideOffset: Float) {}
            })
        } catch (e: Exception) {
            Log.e("AppActivity", "Error in onCreate: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
            }
        }
    }

    private fun handleIntentExtras(intent: Intent?) {
        if (intent == null) return

        if (intent.getBooleanExtra("open_home_after_login", false)) {
            navigateToHomeAfterLogin()
            return
        }
        
        val type = intent.getStringExtra("type") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val body = intent.getStringExtra("body") ?: ""
        val url = intent.getStringExtra("url")
        
        Log.d("AppActivity", "Handling intent extras: type=$type, title=$title")
        
        // Save to storage if clicked from a background notification
        if (title.isNotEmpty() || body.isNotEmpty()) {
            val currentUser = UserContextManager.userX(this)
            if (currentUser?.userId.isNullOrBlank()) {
                Log.w("AppActivity", "Skip saving intent notification: no logged-in user")
            } else {
                NotificationStorage.saveNotification(this, title, body, currentUser?.userId, type, url)
                // Show badge update immediately
                invalidateOptionsMenu()
            }
        }
        
        if (type == "custom" || type == "order_success") {
            showSpecialNotification(title, body)
        } else if (type == "webview_merit" || type == "webview_changenum") {
            // Open WebView as Bottom Sheet
            val url = intent?.getStringExtra("url") ?: com.numberniceic.https.NetworkConfig.BASE_URL
            try {
                val webSheet = com.numberniceic.ui.merit.MeritWebViewBottomSheet.newInstance(url)
                val transaction = supportFragmentManager.beginTransaction()
                // Avoid Can not perform this action after onSaveInstanceState
                transaction.add(webSheet, "MeritWebViewBottomSheet")
                transaction.commitAllowingStateLoss() 
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (intent.getBooleanExtra("open_dashboard", false)) {
            showDashboard(forceRefresh = true)
        } else if (intent.getBooleanExtra("open_chat", false)) {
             val sessionId = intent.getStringExtra("session_id")
             if (sessionId != null) {
                 val roomIntent = Intent(this, com.numberniceic.ui.chat.ChatActivity::class.java)
                 roomIntent.putExtra("session_id", sessionId)
                 startActivity(roomIntent)
             } else {
                 startActivity(Intent(this, com.numberniceic.ui.ChatComposeActivity::class.java))
             }
        } else if (intent.getBooleanExtra("open_admin_chat", false)) {
             val sessionId = intent.getStringExtra("session_id")
             if (sessionId != null) {
                 val roomIntent = Intent(this, com.numberniceic.ui.admin.AdminChatRoomActivity::class.java)
                 roomIntent.putExtra("session_id", sessionId)
                 startActivity(roomIntent)
             } else {
                 startActivity(Intent(this, com.numberniceic.ui.admin.AdminChatActivity::class.java))
             }
        } else if (intent.getBooleanExtra("open_zircon_orders", false)) {
            startActivity(Intent(this, com.numberniceic.ui.admin.AdminDreamOrdersActivity::class.java))
        }
    }

    private fun navigateToHomeAfterLogin() {
        try {
            // Consume login redirect flag once, so later navigation is not affected.
            getSharedPreferences("userdata", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("just_logged_in", false)
                .apply()
            binding.pagerMain.currentItem = 0
            binding.tabLayout.getTabAt(0)?.select()
            binding.root.post {
                if (::dashSheetBehavior.isInitialized) {
                    showDashboard(forceRefresh = true)
                }
            }
        } catch (e: Exception) {
            Log.e("AppActivity", "navigateToHomeAfterLogin failed: ${e.message}")
        }
    }



    private fun showSpecialNotification(title: String, body: String, url: String? = null) {
        try {
            val specialBS = com.numberniceic.ui.notification.SpecialNotificationDialogFragment.newInstance(title, body, url)
            specialBS.showSingle(supportFragmentManager, "SpecialNotificationBottomSheet")
        } catch (e: Exception) {
            Log.e("AppActivity", "Error showing special notification: ${e.message}")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentExtras(intent)
    }

    // ... (rest of methods)

    fun showLoginBottomSheet() {
        try {
            val loginBS = LoginBottomSheet()
            loginBS.showSingle(supportFragmentManager, "LoginBottomSheet")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun showDashboard(forceRefresh: Boolean = false) {
    if (!::dashSheetBehavior.isInitialized) return
    
    // Dismiss any dialog-based bottom sheets before showing persistent dashboard
    supportFragmentManager.dismissAllBottomSheets()
    
    try {
        binding.persistentDashSheet.visibility = android.view.View.VISIBLE
        
        // Logic Change: Always replace content even if already loaded to force a "Reload" 
        // as requested (ถ้า bottom sheet เก่าอยู่ให้ทำการปิด และเปิดใหม่ เพื่อ reload ข้อมูลใหม่)
        val fm = supportFragmentManager
        val fragment = com.numberniceic.ui.apersonnews.PersonNewsF()
        if (forceRefresh) {
            val args = Bundle()
            args.putBoolean("force_refresh", true)
            fragment.arguments = args
        }
        
        fm.beginTransaction()
            .replace(R.id.fragment_container_dashboard_bs, fragment, "PersistentDashboard")
            .commitAllowingStateLoss()
        
        dashSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
    } catch (e: Exception) {
        e.printStackTrace()
    }
    }


    private fun checkAndUpdateFcmToken() {
        try {
            // Toast.makeText(applicationContext, "Start FCM Check...", Toast.LENGTH_SHORT).show()
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        val token = task.result
                        if (!token.isNullOrEmpty()) {
                            Log.d("AppActivity", "Got FCM Token: ${token.take(10)}...")
                            
                            // 🔔 ALWAYS save locally first (for guest users)
                            val prefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("fcm_token", token).apply()
                            
                            // Update server if logged in
                            val user = UserContextManager.userX(this)
                            if (user != null && user.userId != null) {
                                updateTokenOnServer(user.userId!!, token)
                            } else {
                                // Guest user: Sync token with Chat Server/Session
                                val chatPrefs = getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
                                val sessionId = chatPrefs.getString("session_id", "")
                                if (!sessionId.isNullOrEmpty()) {
                                     val json = JsonObject().apply {
                                         addProperty("session_id", sessionId)
                                         addProperty("fcm_token", token)
                                         addProperty("device_id", android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID))
                                     }
                                     RetrofitClient.api.initChat(json).enqueue(object : Callback<JsonObject> {
                                         override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {}
                                         override fun onFailure(call: Call<JsonObject>, t: Throwable) {}
                                     })
                                }
                                Log.d("AppActivity", "Guest user - FCM token sync attempt")
                            }
                        } else {
                            // Toast.makeText(applicationContext, "FCM Token เป็นค่าว่าง", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Toast.makeText(applicationContext, "ขอ FCM Token ไม่สำเร็จ: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            Log.e("AppActivity", "FCM Check Failed: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun updateTokenOnServer(userId: String, token: String) {
        val json = JsonObject()
        json.addProperty("memberid", userId)
        json.addProperty("token", token)
        
        try {
            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            apiService.updateFcmToken(json).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    try {
                        if (response.isSuccessful) {
                            Log.d("AppActivity", "FCM Token updated successfully on app launch")
                            // Toast.makeText(applicationContext, "อัปเดต Token สำเร็จ ✅", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.d("AppActivity", "Failed to update FCM token: ${response.code()}")
                            // Toast.makeText(applicationContext, "อัปเดต Token ไม่สำเร็จ: ${response.code()}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Log.d("AppActivity", "Error updating FCM token: ${t.message}")
                    // Toast.makeText(applicationContext, "Err: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            // Toast.makeText(applicationContext, "Exception: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initInstances() {
        this.imm = applicationContext!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?

        try {
            setSupportActionBar(binding.toolbarMain)
        } catch (_: Exception) {
        }

        drawerToggle = ActionBarDrawerToggle(this, binding.drawerMain, R.string.CLOSE, R.string.OPEN)
        binding.drawerMain.addDrawerListener(drawerToggle)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        configTabs()
        

        
        updateChatFabBadge()
    }

    private fun configTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("HOME").setIcon(R.drawable.ic_world_white))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("เบอร์โทร").setIcon(R.drawable.icon_phone05))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("ทะเบียน").setIcon(R.drawable.car))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("ชื่อเล่น").setIcon(R.drawable.icon_family))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("ชื่อสกุล").setIcon(R.drawable.icon_children))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("บ้านเลขที่").setIcon(R.drawable.ico_house))

        binding.pagerMain.adapter = TabAdapter(supportFragmentManager, binding.tabLayout.tabCount)
        binding.pagerMain.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(binding.tabLayout))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // 🆕 Also hide when tab is clicked again
                if (::dashSheetBehavior.isInitialized) {
                    dashSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.pagerMain.currentItem = tab!!.position
                personNoti?.icon = ContextCompat.getDrawable(applicationContext, R.drawable.notification)
                
                // 🆕 Hide dashboard when switching tabs so user can see the content
                if (::dashSheetBehavior.isInitialized) {
                    dashSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
                }
            }
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        binding.drawerMain.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()

        binding.navHeaderInclude.btnUserRegisterBeside.setOnClickListener {
            val intent = Intent(this, UserRegisAct::class.java)
            startActivity(intent)
        }

        // Admin buttons moved to admin management screen (fragment_user_logout_admin.xml)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        
        when (item.itemId) {
            R.id.action_dream -> {
                try {
                    val intent = android.content.Intent(this, com.numberniceic.ui.ninin.NininChatActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }

            R.id.action_vip_code -> {
                // Logic Change: VIP Icon opens Dashboard for everyone (Guest or Logged In)
                showDashboard()
                return true
            }

            R.id.action_sell_product -> {
                try {
                    val intent = Intent(this, com.numberniceic.ui.CartComposeActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return true
            }

            R.id.action_dashboard -> {
                val userx = UserContextManager.userX(this)
                if (userx != null) {
                    val memberSheet = MemberBottomSheet()
                    memberSheet.showSingle(supportFragmentManager, "MemberBottomSheet")
                } else {
                    showLoginBottomSheet()
                }
                return true
            }
        }
        
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        
        val chatItem = menu?.findItem(R.id.action_chat)
        val chatView = chatItem?.actionView
        chatMenuItem = chatItem
        
        chatView?.setOnClickListener {
            val intent = Intent(this, com.numberniceic.ui.ChatComposeActivity::class.java)
            startActivity(intent)
        }

        val dreamItem = menu?.findItem(R.id.action_dream)
        val dreamView = dreamItem?.actionView
        dreamView?.setOnClickListener {
            try {
                val intent = android.content.Intent(this, com.numberniceic.ui.ninin.NininChatActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // 💤 Start Floating Animation for Dream ZZZ Icon
        dreamView?.let { view ->
            val icon = view.findViewById<android.widget.ImageView>(R.id.img_dream_icon)
            if (icon != null && icon.animation == null) {
                val anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.dream_float)
                icon.startAnimation(anim)
            }
        }

        val notificationItem = menu?.findItem(R.id.action_notification)
        val notificationView = notificationItem?.actionView
        
        personNoti = notificationItem
        
        notificationView?.setOnClickListener {
            shouldVibrateBell = false
            com.numberniceic.data.local.NotificationStorage.markAllRead(this)
            updateNotificationBadge(notificationView)
            
            val notificationSheet = NotificationBottomSheet()
            notificationSheet.showSingle(supportFragmentManager, "NotificationBottomSheet")
        }
        
        updateNotificationBadge(notificationView)
        updateChatFabBadge() // Update chat badge on creation

        // NEW: Manually handle clicks for custom action items
        val sellItem = menu?.findItem(R.id.action_sell_product)
        sellItem?.actionView?.setOnClickListener { 
            onOptionsItemSelected(sellItem)
        }

        val vipItem = menu?.findItem(R.id.action_vip_code)
        val vipView = vipItem?.actionView
        vipView?.setOnClickListener { 
            onOptionsItemSelected(vipItem)
        }



        // 🏅 Update VIP icon based on actual user status
        updateVipStatusIcon(vipView)

        val dashboardItem = menu?.findItem(R.id.action_dashboard)
        dashboardItem?.actionView?.setOnClickListener { 
            onOptionsItemSelected(dashboardItem)
        }

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        val vipItem = menu?.findItem(R.id.action_vip_code)
        val vipView = vipItem?.actionView
        updateVipStatusIcon(vipView)
        return true
    }

    // ✨ Animator สำหรับขอบทองหมุนของ VVIP badge (เก็บไว้เพื่อ cancel ได้)
    private var vvipBorderAnimator: android.animation.ValueAnimator? = null

    private fun updateVipStatusIcon(vipView: View?) {
        if (vipView == null) return
        val imgVip = vipView.findViewById<android.widget.ImageView>(R.id.img_vip_icon) ?: return
        val vvipLegacy = vipView.findViewById<android.view.View>(R.id.layout_vvip_legacy) ?: return


        vvipLegacy.visibility = View.GONE

        val user = UserContextManager.userX(this)
        val vipcode = user?.vipcode?.lowercase()?.trim() ?: ""

        // หยุด animator เก่าก่อนเสมอ
        vvipBorderAnimator?.cancel()
        vvipBorderAnimator = null
        imgVip.clearAnimation()

        if (user == null) {
            // Case 1: ยังไม่ Login — แสดงไอคอนไอคอน 'VIP' (ตามรูปกรอบสีแดงที่คุณนินส่งมา)
            imgVip.visibility = View.VISIBLE
            imgVip.layoutParams = (imgVip.layoutParams as android.widget.FrameLayout.LayoutParams).apply {
                val dp36 = android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 36f,
                    resources.displayMetrics
                ).toInt()
                width = dp36
                height = dp36
                gravity = android.view.Gravity.CENTER
            }
            imgVip.setImageResource(R.drawable.ic_vip02) // ไอคอน VIP สวยๆ ล่อตาล่อใจคนยังไม่สมัคร
            return
        }

        if (UserContextManager.isAdmin(user)) {
            // Case Admin: แสดงตรา 'ADM' (Admin) วงกลมสีแดงพรีเมียมตามที่คุณนินต้องการ
            imgVip.visibility = View.VISIBLE
            imgVip.layoutParams = (imgVip.layoutParams as android.widget.FrameLayout.LayoutParams).apply {
                val dp42 = android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 42f,
                    resources.displayMetrics
                ).toInt()
                width = dp42
                height = dp42
                gravity = android.view.Gravity.CENTER
            }
            try {
                val admBmp = com.numberniceic.ui.components.VvipBadgeBitmapHelper.createAdmBadgeBitmap(this, 40)
                imgVip.setImageBitmap(admBmp)
            } catch (e: Exception) {
                imgVip.setImageResource(R.drawable.icon_admin)
            }
            return
        }

        if (vipcode == "vvip") {
            // Case 2: เป็น VVIP — แสดงขอบทองหมุนพรีเมียม
            imgVip.visibility = View.VISIBLE
            imgVip.layoutParams = (imgVip.layoutParams as android.widget.FrameLayout.LayoutParams).apply {
                val dp42 = android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 42f,
                    resources.displayMetrics
                ).toInt()
                width = dp42
                height = dp42
                gravity = android.view.Gravity.CENTER
            }

            val animator = android.animation.ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 4000
                repeatCount = android.animation.ValueAnimator.INFINITE
                repeatMode = android.animation.ValueAnimator.RESTART
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener { anim ->
                    val angle = anim.animatedValue as Float
                    try {
                        val bmp = com.numberniceic.ui.components.VvipBadgeBitmapHelper
                            .createBadgeBitmap(this@AppActivity, 40, angle)
                        imgVip.setImageBitmap(bmp)
                    } catch (_: Exception) {}
                }
            }
            animator.start()
            vvipBorderAnimator = animator

        } else if (vipcode == "normal" || vipcode == "") {
            // Case 3: Login แล้วแต่เป็นสถานะ Normal — แสดงตรา 'MB' (Member) วงกลมสีฟ้าอ่อนตามแบบที่คุณนินต้องการ
            imgVip.visibility = View.VISIBLE
            imgVip.layoutParams = (imgVip.layoutParams as android.widget.FrameLayout.LayoutParams).apply {
                val dp36 = android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 36f,
                    resources.displayMetrics
                ).toInt()
                width = dp36
                height = dp36
                gravity = android.view.Gravity.CENTER
            }
            try {
                val mbBmp = com.numberniceic.ui.components.VvipBadgeBitmapHelper.createMbBadgeBitmap(this, 36)
                imgVip.setImageBitmap(mbBmp)
            } catch (e: Exception) {
                imgVip.setImageResource(R.drawable.icon_member)
            }
        } else {
            // Case 4: เป็น VIP ประเภทอื่นๆ (VIP, MVP)
            imgVip.visibility = View.VISIBLE
            imgVip.layoutParams = (imgVip.layoutParams as android.widget.FrameLayout.LayoutParams).apply {
                val dp32 = android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 32f,
                    resources.displayMetrics
                ).toInt()
                width = dp32
                height = dp32
                gravity = android.view.Gravity.CENTER
            }
            val iconRes = when (vipcode) {
                "mvp"  -> R.drawable.icon_gold01
                "vip"  -> R.drawable.ic_vip02
                else   -> R.drawable.ic_vip02
            }
            imgVip.setImageResource(iconRes)
        }
    }


    private fun updateNotificationBadge(view: View?) {
        if (view == null) return
        val badge = view.findViewById<android.widget.TextView>(R.id.txt_notification_badge)
        val icon = view.findViewById<android.widget.ImageView>(R.id.img_notification_icon)
        val count = NotificationStorage.getUnreadCount(this)
        
        // Logic for vibration state
        if (lastUnreadNotiCount != -1) {
            if (count > lastUnreadNotiCount) {
                shouldVibrateBell = true
            } else if (count < lastUnreadNotiCount) {
                shouldVibrateBell = false
            }
        } else if (count > 0) {
            // Initial load with existing notifications
            shouldVibrateBell = true
        }
        lastUnreadNotiCount = count

        if (count > 0) {
            badge.text = if (count > 9) "9+" else count.toString()
            badge.isVisible = true
            
            // Animation for bell
            if (icon != null) {
                if (shouldVibrateBell) {
                    if (icon.animation == null) {
                        val anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.vibrate_bell)
                        icon.startAnimation(anim)
                    }
                } else {
                    icon.clearAnimation()
                }
            }
        } else {
            badge.isVisible = false
            icon?.clearAnimation()
            shouldVibrateBell = false
        }
    }

    private var lastKnownUnread = 0
    private var lastNotifiedMsgId = 0L
    
    private val unreadCountState = androidx.compose.runtime.mutableStateOf(0)
    private val isUserAdminState = androidx.compose.runtime.mutableStateOf(false)
    
    private fun updateChatFabBadge(explicitCount: Int? = null) {
        val user = UserContextManager.userX(this)
        val isAdmin = UserContextManager.isAdmin(user)
        val isLoggedIn = user != null && !user.userId.isNullOrEmpty()
        
        isUserAdminState.value = isAdmin

        val count = if (isLoggedIn) {
            explicitCount ?: ChatNotificationManager.getUnreadCount(this)
        } else {
            0
        }
        
        android.util.Log.d("AppActivity", "Updating Badge UI -> Count: $count (LoggedIn: $isLoggedIn, Admin: $isAdmin)")
        unreadCountState.value = count
        
        // Update Toolbar Badge & Label/Icon
        val chatView = chatMenuItem?.actionView
        if (chatView != null) {
            val badge = chatView.findViewById<android.widget.TextView>(R.id.txt_chat_badge)
            val icon = chatView.findViewById<android.widget.ImageView>(R.id.img_chat_icon)
            
            if (icon != null) {
                // 🔄 Apply Shimmering Gold Animation via legacy AnimationDrawable
                if (icon.drawable !is android.graphics.drawable.AnimationDrawable) {
                    icon.setImageResource(R.drawable.ic_chat_bubble_animated)
                }
                val animDrawable = icon.drawable as? android.graphics.drawable.AnimationDrawable
                animDrawable?.start()
                
                // Clear tint to show the gold gradients
                icon.clearColorFilter()
            }




            
            chatView.setOnClickListener {
                if (isAdmin) {
                    val intent = Intent(this, com.numberniceic.ui.admin.AdminChatActivity::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, com.numberniceic.ui.ChatComposeActivity::class.java)
                    startActivity(intent)
                }
            }

            if (count > 0) {
                badge.text = if (count > 9) "9+" else count.toString()
                badge.visibility = android.view.View.VISIBLE
            } else {
                badge.visibility = android.view.View.GONE
            }
        }
    }

    override fun onBackPressed() {
        if (::dashSheetBehavior.isInitialized && dashSheetBehavior.state != com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN) {
            dashSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
            return
        }
        if (doubleBack) {
            super.onBackPressed()
        } else {
            doubleBack = true
            Toast.makeText(applicationContext, "กดอีกครั้งเพื่อปิดแอพ", Toast.LENGTH_SHORT).show()
            Handler().postDelayed({ doubleBack = false }, 2000)
        }
    }

    // --- Chat Polling Logic ---
    private val pollHandler = Handler(android.os.Looper.getMainLooper())
    
    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isDestroyed && !isFinishing) {
                android.util.Log.d("ChatPoll", "--- Loop Start: Syncing Badge ---")
                
                // 1. Always Sync Badge from DB (Truth)
                refreshUnreadCountFromServer()
                
                // 2. Poll messages (For Notifications & Local Logic)
                pollChatForBadge()
                
                // 3. Poll VIP Status to sync icon UI immediately
                pollVIPStatus()

                pollHandler.postDelayed(this, 10000) // Poll every 10 seconds
            }
        }
    }

    private fun pollVIPStatus() {
        val user = UserContextManager.userX(this@AppActivity)
        val memberId = user?.userId ?: return
        
        // 🚀 Use the PHP Endpoint for 100% Truth consistency
        val apiService = com.numberniceic.https.RetrofitClient.instance.create(com.numberniceic.https.ApiService::class.java)
        apiService.getMemberInfo(memberId).enqueue(object : retrofit2.Callback<com.numberniceic.data.admin.Serverx> {
            override fun onResponse(call: retrofit2.Call<com.numberniceic.data.admin.Serverx>, response: retrofit2.Response<com.numberniceic.data.admin.Serverx>) {
                val freshUser = response.body()?.userx ?: return
                val newVip = freshUser.vipcode?.lowercase()?.trim() ?: "normal"
                val currentVip = user.vipcode?.lowercase()?.trim() ?: "normal"
                
                if (newVip != currentVip) {
                    android.util.Log.d("ChatPoll", "VIP Status changed globally: $currentVip -> $newVip (Refreshed from PHP)")
                    
                    // Update Local Storage
                    val prefs = getSharedPreferences("userdata", Context.MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.putString("json", com.google.gson.Gson().toJson(freshUser))
                    editor.putString("vipcode", newVip)
                    editor.putString("status", freshUser.status)
                    editor.apply()
                    
                    // Force refresh AppActivity Action Bar icon
                    runOnUiThread { 
                        invalidateOptionsMenu() 
                        
                        // ✅ กระจายแรงกระตุ้น (Broadcast) เพื่อให้ UserLogoutF (Profile) รีเฟรชด้วยแบบ Real-time
                        sendBroadcast(Intent("com.numberniceic.REFRESH_DASHBOARD"))
                    }
                }

                com.numberniceic.utils.RengyamAccessManager.persistFromServer(
                    this@AppActivity,
                    freshUser.userId,
                    response.body()?.rengyamAccess
                )
            }
            override fun onFailure(call: retrofit2.Call<com.numberniceic.data.admin.Serverx>, t: Throwable) { }
        })
    }

    private fun refreshUnreadCountFromServer() {
        val user = UserContextManager.userX(this)
        val memberId = user?.userId
        
        // Use SharedPreferences session_id as the primary key for history
        val prefs = getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
        var sessionId = prefs.getString("session_id", "") ?: ""

        if (sessionId.isEmpty() && !memberId.isNullOrEmpty()) {
            // Go Backend uses "u" + memberId for logged-in users. 
            // We can predict this to sync the badge even if it's a new install.
            sessionId = "u$memberId"
            android.util.Log.d("ChatBadge", "Predicting sessionId for UID $memberId: $sessionId")
        }

        if (sessionId.isEmpty()) {
            android.util.Log.d("ChatBadge", "Sync Skipped: sessionId is empty")
            runOnUiThread { updateChatFabBadge(0) }
            return
        }

        val lastReadId = ChatNotificationManager.getLastReadMessageId(this@AppActivity)
        android.util.Log.d("ChatBadge", "Syncing from History (DB Truth) for SID: $sessionId, LocalLastRead: $lastReadId")
        com.numberniceic.https.RetrofitClient.api.getChatHistory(sessionId, 50).enqueue(object : retrofit2.Callback<List<com.numberniceic.data.chat.ChatMessage>> {
            override fun onResponse(call: retrofit2.Call<List<com.numberniceic.data.chat.ChatMessage>>, response: retrofit2.Response<List<com.numberniceic.data.chat.ChatMessage>>) {
                try {
                    if (response.isSuccessful && response.body() != null) {
                        val messages = response.body()!!
                        val maxMsgId = messages.maxOfOrNull { it.messageId } ?: 0L
                        
                        // 🛡️ ANOMALY CHECK: Did Server DB Reset or ID go back?
                        var effectiveLastRead = lastReadId
                        if (maxMsgId > 0 && lastReadId > maxMsgId) {
                            android.util.Log.e("ChatBadge", "Anomaly: LocalLastRead ($lastReadId) > ServerMaxId ($maxMsgId). Resetting local marker.")
                            val prefsChat = getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
                            prefsChat.edit().putLong("last_read_msg_id", maxMsgId).apply()
                            effectiveLastRead = maxMsgId
                        }

                        // Calculate unread count based on admin messages higher than last read ID AND not marked as read on server
                        val unreadCount = messages.count { 
                            (it.senderType.equals("admin", ignoreCase = true)) && 
                            it.messageId > effectiveLastRead && !it.isRead
                        }
                        
                        android.util.Log.e("ChatBadge", "History Sync Result: $unreadCount (LastRead: $effectiveLastRead, ServerMax: $maxMsgId)")
                        ChatNotificationManager.setUnreadCount(this@AppActivity, unreadCount)
                        runOnUiThread { 
                            updateChatFabBadge(unreadCount)
                        }
                    } else {
                        android.util.Log.e("ChatBadge", "History Sync Error: ${response.code()}")
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            override fun onFailure(call: retrofit2.Call<List<com.numberniceic.data.chat.ChatMessage>>, t: Throwable) {
                android.util.Log.e("ChatBadge", "History Sync Failure: ${t.message}")
            }
        })
    }

    private fun pollChatForBadge() {
        val user = UserContextManager.userX(this)
        val isAdmin = UserContextManager.isAdmin(user)
        
        if (isAdmin) {
             pollAdminChatForBadge()
             return
        }
        
        val prefs = getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
        val sessionId = prefs.getString("session_id", "")
        
        // 1. Silent Init if needed
        if (sessionId.isNullOrEmpty()) {
            val user = UserContextManager.userX(this)
            val json = JsonObject()
            
            if (user != null && user.userId != null) {
                val guestName = if (user.realName.isNullOrEmpty()) "User ${user.userId}" else user.realName
                json.addProperty("name", guestName)
                json.addProperty("user_id", user.userId)
            } else {
                // GUEST INIT: Essential for getting FCM Token to server for non-logged users
                json.addProperty("name", "Guest")
            }
            
            // Add Device ID and FCM Token to ensure server can notify this specific device
            val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
            json.addProperty("device_id", deviceId)
            
            val fcmToken = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE).getString("fcm_token", "")
            if (!fcmToken.isNullOrEmpty()) {
                json.addProperty("fcm_token", fcmToken)
            }

            RetrofitClient.api.initChat(json).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                     try {
                        if (response.isSuccessful) {
                            val newId = response.body()?.get("session_id")?.asString ?: ""
                            if (newId.isNotEmpty()) {
                                prefs.edit().putString("session_id", newId).apply()
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
                override fun onFailure(call: Call<JsonObject>, t: Throwable) { }
            })
            return
        }

        // 2. Poll Messages - Focused on showing NEW notifications
        RetrofitClient.api.pollMessages(sessionId).enqueue(object : Callback<List<com.numberniceic.data.chat.ChatMessage>> {
            override fun onResponse(call: Call<List<com.numberniceic.data.chat.ChatMessage>>, response: Response<List<com.numberniceic.data.chat.ChatMessage>>) {
                try {
                    if (response.isSuccessful) {
                        val messages = response.body() ?: emptyList()
                        if (messages.isEmpty()) return

                        val lastReadId = ChatNotificationManager.getLastReadMessageId(this@AppActivity)
                        val maxMsgId = messages.maxByOrNull { it.messageId }?.messageId ?: 0

                        // 🛡️ ANOMALY CHECK: Did Server DB Reset?
                        var effectiveLastRead = lastReadId
                        if (maxMsgId > 0 && lastReadId > maxMsgId) {
                            val prefsChat = getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
                            prefsChat.edit().putLong("last_read_msg_id", maxMsgId).apply()
                            effectiveLastRead = maxMsgId
                        }
                        
                        // 1. Calculate Real Unread Count locally
                        // Filter by ID barrier AND server 'is_read' status
                        val newMessages = messages.filter { 
                            (it.senderType.equals("admin", ignoreCase = true)) && 
                            it.messageId > effectiveLastRead &&
                            !it.isRead
                        }
                        val realUnreadCount = newMessages.count()
                        
                        android.util.Log.e("ChatBadge", "Local Poll Count: $realUnreadCount (EffectiveLastRead: $effectiveLastRead)")

                        // Update local count immediately for fast response
                        ChatNotificationManager.setUnreadCount(this@AppActivity, realUnreadCount)
                        runOnUiThread { updateChatFabBadge(realUnreadCount) }

                        // 2. Logic: New Message Detection based on ID (Robust)
                        val latestMsg = newMessages.maxByOrNull { it.messageId }
                        
                        // Condition: New Message + ID Higher than last notified + Chat Screen NOT Open
                        if (latestMsg != null && latestMsg.messageId > lastNotifiedMsgId && !ChatNotificationManager.isChatScreenOpen) {
                             sendChatNotification("ข้อความใหม่จาก Admin", latestMsg.message, isAdmin = false, sessionId = latestMsg.sessionId)
                             lastNotifiedMsgId = latestMsg.messageId
                        } else if (ChatNotificationManager.isChatScreenOpen && latestMsg != null) {
                             lastNotifiedMsgId = latestMsg.messageId
                        }
                        
                        // 3. Trigger a DB sync to verify/correct against official number
                        refreshUnreadCountFromServer()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            override fun onFailure(call: Call<List<com.numberniceic.data.chat.ChatMessage>>, t: Throwable) { 
                android.util.Log.e("ChatBadge", "Poll Failure: ${t.message}")
            }
        })
    }

    private fun pollAdminChatForBadge() {
        // 🚀 Use "Recent Messages" (Chat Heads) to calculate accurate Unread Count
        // This ensures that when we read a message locally, the badge count decreases.
        RetrofitClient.api.getAdminRecentMessages(limit = 60).enqueue(object : Callback<List<com.numberniceic.data.chat.ChatMessage>> {
            override fun onResponse(call: Call<List<com.numberniceic.data.chat.ChatMessage>>, response: Response<List<com.numberniceic.data.chat.ChatMessage>>) {
                try {
                    if (response.isSuccessful) {
                        val sessions = response.body() ?: emptyList()
                        
                        // 1. Calculate Unread Count (The Truth)
                        val unreadCount = sessions.count { msg ->
                            val isCustomer = msg.senderType == "customer"
                            val isHidden = com.numberniceic.data.chat.AdminChatStateManager.isHidden(msg.sessionId, msg.messageId)
                            // Check both Server Status and Local Status (in case we just read it)
                            val isRead = com.numberniceic.data.chat.AdminChatStateManager.isMessageRead(msg.sessionId, msg.messageId, msg.isRead)
                            
                            isCustomer && !isHidden && !isRead
                        }
                        
                        // 2. Update Badge UI
                        ChatNotificationManager.setUnreadCount(this@AppActivity, unreadCount)
                        runOnUiThread { updateChatFabBadge(unreadCount) }
                        
                        // 3. Notification Logic for New Messages
                        val latestMsg = sessions.maxByOrNull { it.messageId }
                        if (latestMsg != null && latestMsg.messageId > lastNotifiedMsgId) {
                            android.util.Log.d("ChatBadge", "Potential Noti found: ${latestMsg.messageId} (Last: $lastNotifiedMsgId)")
                            // Only notify if it's a customer message and genuinely unread
                            if (latestMsg.senderType == "customer") {
                                 val isHidden = com.numberniceic.data.chat.AdminChatStateManager.isHidden(latestMsg.sessionId, latestMsg.messageId)
                                 val isRead = com.numberniceic.data.chat.AdminChatStateManager.isMessageRead(latestMsg.sessionId, latestMsg.messageId, latestMsg.isRead)
                                 
                                 android.util.Log.d("ChatBadge", "Details: isHidden=$isHidden, isRead=$isRead, activeSession=${ChatNotificationManager.activeSessionId}")
                                 
                                 // 🛡️ ADMIN Logic: Only silence if it's the SAME session they are currently viewing OR they are in ANY chat screen
                                 val isViewingThisSession = ChatNotificationManager.activeSessionId == latestMsg.sessionId
                                 val shouldSilence = isViewingThisSession || ChatNotificationManager.isChatScreenOpen
                                 
                                 if (!isHidden && !isRead && !shouldSilence) {
                                     val displayBody = if (latestMsg.message.isNullOrBlank() && !latestMsg.imageUrl.isNullOrBlank()) "[รูปภาพ]" else latestMsg.message
                                     val displayTitle = "ลูกค้าทักแชท: ${latestMsg.senderName ?: "ท่านใหม่"}"
                                     sendChatNotification(displayTitle, displayBody, isAdmin = true, sessionId = latestMsg.sessionId)
                                     com.numberniceic.data.local.NotificationStorage.saveNotification(this@AppActivity, displayTitle, displayBody, type = "admin_message")
                                     lastNotifiedMsgId = latestMsg.messageId
                                 } else {
                                     // Even if read/hidden/viewing, update ID so we don't notify for this message later
                                     lastNotifiedMsgId = latestMsg.messageId
                                 }
                            } else {
                                // Update ID for admin messages too
                                lastNotifiedMsgId = latestMsg.messageId
                            }
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            override fun onFailure(call: Call<List<com.numberniceic.data.chat.ChatMessage>>, t: Throwable) {}
        })
    }

    private fun sendChatNotification(title: String, body: String, isAdmin: Boolean = false, sessionId: String? = null) {
        val intent = Intent(this, com.numberniceic.ui.AppActivity::class.java).apply {
            if (isAdmin) {
                putExtra("open_admin_chat", true)
            } else {
                putExtra("open_chat", true)
            }
            if (sessionId != null) {
                putExtra("session_id", sessionId)
            }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, if (isAdmin) 101 else 0, intent, 
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val channelId = getString(R.string.default_notification_channel_id)
        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        // Ensure Channel Exists
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId, 
                "NumberNice Notifications", 
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat Notifications"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        notificationManager.notify(1001, builder.build())
    }

    override fun onResume() {
        super.onResume()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
             registerReceiver(notificationReceiver, android.content.IntentFilter("com.numberniceic.NEW_NOTIFICATION"), Context.RECEIVER_NOT_EXPORTED)
        } else {
             registerReceiver(notificationReceiver, android.content.IntentFilter("com.numberniceic.NEW_NOTIFICATION"))
        }
        invalidateOptionsMenu()
        
        // Smart Refresh based on Role
        val user = UserContextManager.userX(this)
        if (UserContextManager.isAdmin(user)) {
            pollAdminChatForBadge()
        } else {
            refreshUnreadCountFromServer()
        }
        
        updateChatFabBadge()
        updateChatFabBadge()
        
        // Start Polling
        pollHandler.removeCallbacks(pollRunnable)
        pollHandler.post(pollRunnable)
        
        val userx = UserContextManager.userX(applicationContext)

        if (userx != null) {
            binding.navHeaderInclude.btnUserRegisterBeside.isVisible = false
            binding.navHeaderInclude.txtSlogan.setPadding(90, 24, 0, 24)
            binding.navHeaderInclude.txtSlogan.textSize = 16f
            
            // Check Token on Resume (covers login return)
            checkAndUpdateFcmToken()
        } else {
            binding.navHeaderInclude.btnUserRegisterBeside.isVisible = true
            binding.navHeaderInclude.txtSlogan.setPadding(45, 24, 0, 24)
            binding.navHeaderInclude.txtSlogan.textSize = 12f
            showWidgetVip("logout")
        }
        
        // Update version string to real version
        binding.navHeaderInclude.txtSlogan.text = "v${BuildConfig.VERSION_NAME} เพราะชีวิตมีค่า จงเลือกให้ดีที่สุด"

        // Setup Admin Button (Check every time onResume)
        setupAdminButton()
    }
    
    private fun setupAdminButton() {
        // Admin buttons moved to admin management screen (fragment_user_logout_admin.xml)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(notificationReceiver)
        pollHandler.removeCallbacks(pollRunnable)
        vvipBorderAnimator?.cancel()
    }

    private fun showWidgetVip(userStatus: String) {
        val page = supportFragmentManager.findFragmentByTag("android:switcher:${R.id.pager_main}:${binding.pagerMain.currentItem}")
        if (page != null && binding.pagerMain.currentItem == 0 && page is PersonNewsF) {
            if (userStatus == "logout") page.showWidgetOnVip(false)
            if (userStatus == "vip") page.showWidgetOnVip(true)
        }
    }
    
    fun updateUserUI() {
        Log.d("AppActivity", "updateUserUI called")

        if (isFinishing || isDestroyed) return

        val user = UserContextManager.userX(this)
        if (UserContextManager.isAdmin(user)) {
            pollAdminChatForBadge()
        } else {
            refreshUnreadCountFromServer()
        }
        updateChatFabBadge()
        
        // Show Dashboard if just logged in
        val sharedref = getSharedPreferences("userdata", Context.MODE_PRIVATE)
        val userx = UserContextManager.userX(this)
        
        if (sharedref.getBoolean("just_logged_in", false) && userx != null) {
            sharedref.edit().putBoolean("just_logged_in", false).apply()
            
            // 🚀 Trigger IMMEDIATE broadcast
            sendBroadcast(Intent("com.numberniceic.REFRESH_DASHBOARD"))
            
            // Fix for S22/Slower Devices: Send again after a very short delay just in case
            Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                     sendBroadcast(Intent("com.numberniceic.REFRESH_DASHBOARD"))
                     invalidateOptionsMenu() // Refresh Notification/VIP Icons
                } catch (e: Exception) { e.printStackTrace() }
            }, 300)
            
            Handler(android.os.Looper.getMainLooper()).post {
                try {
                    if (!isFinishing && !isDestroyed) {
                        showDashboard()
                    }
                } catch (e: Exception) {
                    Log.e("AppActivity", "updateUserUI showDashboard failed", e)
                }
            }
        }
        
        // Also refresh other fragments if needed
        invalidateOptionsMenu()
    }
    
    private fun testGuestSystem() {
        Log.d("AppActivity", "=== Testing Guest System ===")
        
        try {
            val guestManager = com.numberniceic.utils.GuestManager(this)
            val guestId = guestManager.getGuestId()
            val isGuest = guestManager.isGuestUser()
            
            Log.d("AppActivity", "Guest ID: $guestId")
            Log.d("AppActivity", "Is Guest: $isGuest")
            
            // ทดสอบแสดง Guest Address Dialog
            if (guestId != null) {
                val addressDialog = com.numberniceic.ui.auth.GuestAddressDialog()
                
                addressDialog.setOnAddressSaved {
                    Log.d("AppActivity", "✅ Guest address saved successfully!")
                }
                
                addressDialog.setOnSkip {
                    Log.d("AppActivity", "⏭️ Guest skipped address")
                }
                
                // แสดง dialog
                addressDialog.show(supportFragmentManager, "DebugGuestAddressDialog")
                Log.d("AppActivity", "Guest Address Dialog shown")
            } else {
                Log.e("AppActivity", "❌ No Guest ID found!")
            }
            
        } catch (e: Exception) {
            Log.e("AppActivity", "Error testing guest system", e)
        }
    }
    
    private fun testPaymentFlow() {
        Log.d("AppActivity", "=== Testing Payment Flow ===")
        
        try {
            val paymentTestFragment = com.numberniceic.ui.payment.PaymentTestFragment()
            
            // แสดง fragment ทดสอบ
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, paymentTestFragment)
                .addToBackStack("PaymentTest")
                .commit()
                
            Log.d("AppActivity", "Payment Test Fragment shown")
            
        } catch (e: Exception) {
            Log.e("AppActivity", "Error testing payment flow", e)
        }
    }
}
