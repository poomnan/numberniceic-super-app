package com.numberniceic.ui.auth


import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.data.admin.ServerVip
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.numberniceic.ui.admin.*
import com.numberniceic.utils.UserContextManager
import com.numberniceic.utils.RengyamAccessManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import com.numberniceic.ui.components.VvipBadgeBitmapHelper


class UserLogoutF : Fragment() {


    private var btn_color_bag: View? = null
    private var btn_secret_code: View? = null
    private var btn_lucky_number: View? = null
    private var btn_personal_msg: View? = null
    private var btn_buddha_pang_assign: View? = null
    private var btn_sacred_temple_assign: View? = null
    private var btn_merit_assign: View? = null
    private var btn_changenum_assign: View? = null
    private var btn_spell_assign: View? = null
    private var btn_inauspicious_assign: View? = null
    private var btn_auspicious_assign: View? = null
    private var btn_tabian_manage: View? = null
    private var btn_admin_chat: View? = null
    private var btn_add_dream: View? = null
    private var btn_change_vip_status: View? = null
    private var btn_product_manage: View? = null
    private var btn_category_manage: View? = null
    private var btn_guest_address_manage: View? = null
    private var btn_manual_palette_manage: View? = null
    private var btn_zircon_orders: View? = null
    
    private var btn_vip_codeuser: Button? = null
    private var edt_vipcode_user: EditText? = null
    private var btn_vip_codesilver: Button? = null
    private var edt_vipcode_silver: EditText? = null
    private var btn_vip_codegold: Button? = null
    private var edt_vipcode_gold: EditText? = null
    
    // 🔔 Receiver สำหรับอัปเดตข้อมูลแบบ Real-time เมื่อมี Notification เข้า
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.numberniceic.NEW_NOTIFICATION") {
                Log.d("UserLogoutF", "🔔 Received NEW_NOTIFICATION broadcast! Refreshing data...")
                context?.let { refreshUserFromServer(it) }
            }
        }
    }
    private var txt_member_name_diamond: TextView? = null
    private var txt_member_type_diamond: androidx.compose.ui.platform.ComposeView? = null
    private var img_member_diamond: com.google.android.material.imageview.ShapeableImageView? = null
    private var txt_member_name_gold: TextView? = null
    private var txt_member_type_gold: androidx.compose.ui.platform.ComposeView? = null
    private var img_member_gold: com.google.android.material.imageview.ShapeableImageView? = null
    private var btn_gold_upgrade_to_diamond: Button? = null
    private var txt_member_name_silver: TextView? = null
    private var txt_member_type_silver: androidx.compose.ui.platform.ComposeView? = null
    private var img_member_silver: com.google.android.material.imageview.ShapeableImageView? = null
    private var btn_upgrade_silver_gold: Button? = null
    private var txt_member01: TextView? = null
    private var txt_member_type01: androidx.compose.ui.platform.ComposeView? = null
    private var img_member01: com.google.android.material.imageview.ShapeableImageView? = null
    private var img_member_admin: com.google.android.material.imageview.ShapeableImageView? = null
    private var btn_upgrade01: Button? = null
    private var btn_user_logout_silver: Button? = null
    private var btn_user_logout_admin: Button? = null
    private var btn_user_logout_normal: Button? = null
    private var btn_user_logout_gold: Button? = null
    private var btn_user_logout_diamond: Button? = null
    private var btn_user_edit_profile: Button? = null
    private var txt_status_explanation: TextView? = null
    private var txt_status_explanation_silver: TextView? = null
    private var txt_status_explanation_gold: TextView? = null
    private var txt_status_explanation_diamond: TextView? = null




    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val userx = UserContextManager.userX(container!!.context)

        // 🔍 Debug: แสดงข้อมูล userx ที่อ่านได้จาก SharedPreferences
        Log.d("UserLogoutF", "=== onCreateView DEBUG ===")
        Log.d("UserLogoutF", "userId   = ${userx?.userId}")
        Log.d("UserLogoutF", "username = ${userx?.username}")
        Log.d("UserLogoutF", "vipcode  = '${userx?.vipcode}'")
        Log.d("UserLogoutF", "status   = '${userx?.status}'")

        val isAdmin = UserContextManager.isAdmin(userx)
        val vipcodeNormalized = userx?.vipcode?.lowercase()?.trim() ?: ""
        val vipCheck = if (isAdmin) "admin" else vipcodeNormalized

        Log.d("UserLogoutF", "isAdmin  = $isAdmin  |  vipCheck = '$vipCheck'")
        Log.d("UserLogoutF", "=========================")

        return when (vipCheck) {

            "normal" -> inflater.inflate(R.layout.fragment_user_logout, container, false)
            "silver" -> inflater.inflate(R.layout.fragment_user_logout_silver, container, false)
            "gold" -> inflater.inflate(R.layout.fragment_user_logout_gold, container, false)
            "diamond" -> inflater.inflate(R.layout.fragment_user_logout_diamond, container, false)
            "admin" -> inflater.inflate(R.layout.fragment_user_logout_admin, container, false)
            else -> inflater.inflate(R.layout.fragment_user_logout, container, false)

        }

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        btn_color_bag = view.findViewById(R.id.btn_color_bag)
        btn_secret_code = view.findViewById(R.id.btn_secret_code)
        btn_lucky_number = view.findViewById(R.id.btn_lucky_number)
        btn_personal_msg = view.findViewById(R.id.btn_personal_msg)
        btn_buddha_pang_assign = view.findViewById(R.id.btn_buddha_pang_assign)
        btn_sacred_temple_assign = view.findViewById(R.id.btn_sacred_temple_assign)
        btn_merit_assign = view.findViewById(R.id.btn_merit_assign)
        btn_changenum_assign = view.findViewById(R.id.btn_changenum_assign)
        btn_spell_assign = view.findViewById(R.id.btn_spell_assign)
        btn_inauspicious_assign = view.findViewById(R.id.btn_inauspicious_assign)
        btn_auspicious_assign = view.findViewById(R.id.btn_auspicious_assign)
        btn_tabian_manage = view.findViewById(R.id.btn_tabian_manage)
        btn_admin_chat = view.findViewById(R.id.btn_admin_chat)
        btn_add_dream = view.findViewById(R.id.btn_add_dream)
        btn_change_vip_status = view.findViewById(R.id.btn_change_vip_status)
        btn_product_manage = view.findViewById(R.id.btn_product_manage)
        btn_category_manage = view.findViewById(R.id.btn_category_manage)
        btn_guest_address_manage = view.findViewById(R.id.btn_guest_address_manage)
        btn_manual_palette_manage = view.findViewById(R.id.btn_manual_palette_manage)
        btn_zircon_orders = view.findViewById(R.id.btn_zircon_orders)

        btn_vip_codeuser = view.findViewById(R.id.btn_vip_codeuser)
        edt_vipcode_user = view.findViewById(R.id.edt_vipcode_user)
        btn_vip_codesilver = view.findViewById(R.id.btn_vip_codesilver)
        edt_vipcode_silver = view.findViewById(R.id.edt_vipcode_silver)
        btn_vip_codegold = view.findViewById(R.id.btn_vip_codegold)
        edt_vipcode_gold = view.findViewById(R.id.edt_vipcode_gold)
        txt_member_name_diamond = view.findViewById(R.id.txt_member_name_diamond)
        txt_member_type_diamond = view.findViewById(R.id.txt_member_type_diamond)
        img_member_diamond = view.findViewById(R.id.img_member_diamond)
        txt_member_name_gold = view.findViewById(R.id.txt_member_name_gold)
        txt_member_type_gold = view.findViewById(R.id.txt_member_type_gold)
        img_member_gold = view.findViewById(R.id.img_member_gold)
        btn_gold_upgrade_to_diamond = view.findViewById(R.id.btn_gold_upgrade_to_diamond)
        txt_member_name_silver = view.findViewById(R.id.txt_member_name_silver)
        txt_member_type_silver = view.findViewById(R.id.txt_member_type_silver)
        img_member_silver = view.findViewById(R.id.img_member_silver)
        btn_upgrade_silver_gold = view.findViewById(R.id.btn_upgrade_silver_gold)
        txt_member01 = view.findViewById(R.id.txt_member01)
        txt_member_type01 = view.findViewById(R.id.txt_member_type01)
        img_member01 = view.findViewById(R.id.img_member01)
        img_member_admin = view.findViewById(R.id.img_member_admin)
        btn_upgrade01 = view.findViewById(R.id.btn_upgrade01)
        btn_user_logout_silver = view.findViewById(R.id.btn_user_logout_silver)
        btn_user_logout_admin = view.findViewById(R.id.btn_user_logout_admin)
        btn_user_logout_normal = view.findViewById(R.id.btn_user_logout_normal)
        btn_user_logout_gold = view.findViewById(R.id.btn_user_logout_gold)
        btn_user_logout_diamond = view.findViewById(R.id.btn_user_logout_diamond)
        btn_user_edit_profile = view.findViewById(R.id.btn_user_edit_profile)
        txt_status_explanation = view.findViewById(R.id.txt_status_explanation)
        txt_status_explanation_silver = view.findViewById(R.id.txt_status_explanation_silver)
        txt_status_explanation_gold = view.findViewById(R.id.txt_status_explanation_gold)
        txt_status_explanation_diamond = view.findViewById(R.id.txt_status_explanation_diamond)

        btn_user_edit_profile?.setOnClickListener {
            // Dismiss current (MemberBottomSheet)
            (parentFragment as? com.google.android.material.bottomsheet.BottomSheetDialogFragment)?.dismiss()

            // Open UserEditProfileBottomSheet
            val editSheet = UserEditProfileBottomSheet()
            editSheet.show(requireActivity().supportFragmentManager, "UserEditProfileBottomSheet")
        }

        // 1. ระดับ Normal / Member
        view.findViewById<View>(R.id.layout_member_header_normal)?.setOnClickListener { openDashboard() }
        txt_member_type01?.setOnClickListener { openDashboard() }
        img_member01?.setOnClickListener { openDashboard() }
        
        // 2. ระดับ Silver
        view.findViewById<View>(R.id.layout_member_header_silver)?.setOnClickListener { openDashboard() }
        txt_member_type_silver?.setOnClickListener { openDashboard() }
        img_member_silver?.setOnClickListener { openDashboard() }
        
        // 3. ระดับ Gold
        view.findViewById<View>(R.id.layout_member_header_gold)?.setOnClickListener { openDashboard() }
        txt_member_type_gold?.setOnClickListener { openDashboard() }
        img_member_gold?.setOnClickListener { openDashboard() }
        
        // 4. ระดับ Diamond
        view.findViewById<View>(R.id.layout_member_header_diamond)?.setOnClickListener { openDashboard() }
        txt_member_type_diamond?.setOnClickListener { openDashboard() }
        img_member_diamond?.setOnClickListener { openDashboard() }
        
        // 5. ระดับ Admin
        img_member_admin?.setOnClickListener { openDashboard() }

        setBtnLogout()
        setTxtMembe01(view.context)
        setTxtMembeSilver(view.context)
        setTxtMembeGold(view.context)
        setTxtMembeDiamond(view.context)
        setImgMemberAdmin(view.context)
        setBtnAddCodeVip()
        setBtnAdmin()

        // 🔄 Refresh user data from server to fix stale vipcode in SharedPreferences
        refreshUserFromServer(view.context)
        loadApplicationPrivileges()
    }

    private fun loadApplicationPrivileges() {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getApplicationPrivileges().enqueue(object : Callback<List<com.numberniceic.data.admin.ApplicationPrivilege>> {
            override fun onResponse(call: Call<List<com.numberniceic.data.admin.ApplicationPrivilege>>, response: Response<List<com.numberniceic.data.admin.ApplicationPrivilege>>) {
                if (!isAdded) return
                if (response.isSuccessful && response.body() != null) {
                    val privileges = response.body()!!
                    Log.d("UserLogoutF", "Loaded ${privileges.size} privileges from server")
                    updatePrivilegeUI(privileges)
                }
            }
            override fun onFailure(call: Call<List<com.numberniceic.data.admin.ApplicationPrivilege>>, t: Throwable) {
                Log.e("UserLogoutF", "Failed to load privileges: ${t.message}")
            }
        })
    }

    private fun updatePrivilegeUI(privileges: List<com.numberniceic.data.admin.ApplicationPrivilege>) {
        val currentView = view ?: return
        for (priv in privileges) {
            when (priv.code.uppercase()) {
                "NORMAL" -> {
                    currentView.findViewById<TextView>(R.id.txt_normal_benefits_intro)?.text = priv.benefits
                    currentView.findViewById<TextView>(R.id.txt_normal_details)?.text = priv.detail
                }
                "SILVER" -> {
                    currentView.findViewById<TextView>(R.id.vip_silver)?.text = priv.name
                    currentView.findViewById<TextView>(R.id.txt_vip_silver_benefits_intro)?.text = priv.benefits
                    currentView.findViewById<TextView>(R.id.txt_vip_silver_details)?.text = "${priv.detail} ${priv.price}".trim()
                    currentView.findViewById<TextView>(R.id.txt_silver_h)?.text = priv.benefits
                }
                "GOLD" -> {
                    currentView.findViewById<TextView>(R.id.txt_vip_gold)?.text = priv.name
                    currentView.findViewById<TextView>(R.id.txt_vip_gold1)?.text = priv.name
                    currentView.findViewById<TextView>(R.id.txt_vip_gold_benefits_intro)?.text = priv.benefits
                    currentView.findViewById<TextView>(R.id.txt_vip_gold_details)?.text = "${priv.detail} ${priv.price}".trim()
                    currentView.findViewById<TextView>(R.id.txt_gold)?.text = priv.benefits
                }
                "DIAMOND" -> {
                    currentView.findViewById<TextView>(R.id.txt_vip_daimond)?.text = priv.name
                    currentView.findViewById<TextView>(R.id.txt_vip_diamond_benefits_intro)?.text = priv.benefits
                    currentView.findViewById<TextView>(R.id.txt_vip_diamond_details)?.text = "${priv.detail} ${priv.price}".trim()
                    currentView.findViewById<TextView>(R.id.txt_daimond)?.text = priv.benefits
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 🔔 ลงทะเบียนรับแจ้งเตือนเมื่อหน้าโปรไฟล์เปิดอยู่
        val filter = IntentFilter()
        filter.addAction("com.numberniceic.NEW_NOTIFICATION")
        filter.addAction("com.numberniceic.REFRESH_DASHBOARD")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(notificationReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            requireContext().registerReceiver(notificationReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        // 🔕 ยกเลิกการรับเมื่อปิดหน้าไปเพื่อประหยัดทรัพยากร
        try {
            requireContext().unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            Log.e("UserLogoutF", "Error unregistering receiver: ${e.message}")
        }
    }

    /**
     * ดึงข้อมูล user ล่าสุดจาก server และ update SharedPreferences
     * ถ้า vipcode เปลี่ยน → reload fragment ด้วย layout ที่ถูกต้อง
     */
    private fun refreshUserFromServer(context: android.content.Context) {
        val localUser = UserContextManager.userX(context) ?: return
        val memberId = localUser.userId ?: return

        val apiService = RetrofitClient.instance.create(com.numberniceic.https.ApiService::class.java)
        apiService.getMemberInfo(memberId).enqueue(object : retrofit2.Callback<com.numberniceic.data.admin.Serverx> {
            override fun onResponse(
                call: retrofit2.Call<com.numberniceic.data.admin.Serverx>,
                response: retrofit2.Response<com.numberniceic.data.admin.Serverx>
            ) {
                if (!isAdded) return
                if (response.isSuccessful && response.body()?.userx != null) {
                    val freshUser = response.body()!!.userx!!
                    val localVip = localUser.vipcode?.lowercase()?.trim() ?: ""
                    val freshVip = freshUser.vipcode?.lowercase()?.trim() ?: ""

                    Log.d("UserLogoutF", "Server vipcode='$freshVip'  Local vipcode='$localVip'")

                    // บันทึก JSON และค่าเดี่ยวๆ ไว้กันตาย
                    val sharedref = context.getSharedPreferences("userdata", android.content.Context.MODE_PRIVATE)
                    val editor = sharedref.edit()
                    editor.putString("json", com.google.gson.Gson().toJson(freshUser))
                    editor.putString("vipcode", freshVip)
                    editor.putString("saved_userid", freshUser.userId)
                    editor.putString("status", freshUser.status)
                    editor.apply()

                    RengyamAccessManager.persistFromServer(
                        context,
                        freshUser.userId,
                        response.body()?.rengyamAccess
                    )

                    val localIsAdmin = UserContextManager.isAdmin(localUser)
                    val freshIsAdmin = UserContextManager.isAdmin(freshUser)

                    // 🚀 เช็คว่าต้องเปลี่ยน Layout หรือไม่ (เช่น จาก MB ไปเป็น Admin)
                    val localCategory = if (localIsAdmin) "admin" else localVip
                    val freshCategory = if (freshIsAdmin) "admin" else freshVip
                    
                    if (localCategory != freshCategory && (localCategory == "admin" || freshCategory == "admin")) {
                         Log.d("UserLogoutF", "⚠️ Category changed ($localCategory -> $freshCategory). Reloading fragment layout...")
                         reloadFragment()
                         return
                    }

                    // ✅ อัปเดต UI แบบ Real-time ทันที (ไม่ต้องรอเทียบค่าเก่า เพื่อความแม่นยำสูงสุด)
                    Log.d("UserLogoutF", "✨ Refreshing UI with fresh data: $freshVip")
                    
                    // 1. อัปเดต Badge (ComposeView)
                    txt_member_type01?.let { setVipBadgeUI(it, freshVip, freshUser.status, onClick = { openDashboard() }) }
                    txt_member_type_diamond?.let { setVipBadgeUI(it, freshVip, freshUser.status, onClick = { openDashboard() }) }
                    txt_member_type_gold?.let { setVipBadgeUI(it, freshVip, freshUser.status, onClick = { openDashboard() }) }
                    txt_member_type_silver?.let { setVipBadgeUI(it, freshVip, freshUser.status, onClick = { openDashboard() }) }

                    // 2. อัปเดตรูปภาพโปรไฟล์
                    val avatarRes = UserContextManager.getAvatarResId(freshUser.avatar)
                    img_member01?.setImageResource(avatarRes)
                    img_member_diamond?.setImageResource(avatarRes)
                    img_member_gold?.setImageResource(avatarRes)
                    img_member_silver?.setImageResource(avatarRes)
                    img_member_admin?.setImageResource(avatarRes)
                    
                    // 3. อัปเดตชื่อ
                    txt_member01?.text = freshUser.realName
                    txt_member_name_diamond?.text = freshUser.realName
                    txt_member_name_gold?.text = freshUser.realName
                    txt_member_name_silver?.text = freshUser.realName
                    
                    // 4. Update status explanation
                    updateStatusExplanation(freshUser.vipcode)
                }
            }

            override fun onFailure(call: retrofit2.Call<com.numberniceic.data.admin.Serverx>, t: Throwable) {
                Log.w("UserLogoutF", "refreshUserFromServer failed (no network?): ${t.message}")
                // ไม่ทำอะไร - ใช้ข้อมูล local ต่อไป
            }
        })
    }

    private fun reloadFragment() {
        try {
            parentFragmentManager.beginTransaction()
                .detach(this)
                .attach(this)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            Log.e("UserLogoutF", "Reload failed: ${e.message}")
        }
    }

    private fun setBtnAdmin() {



        if(btn_color_bag != null){
            btn_color_bag?.setOnClickListener {
                val intent = Intent(context, BagColorAct::class.java)
                startActivity(intent)
            }
        }

        if (btn_secret_code != null) {
            btn_secret_code?.setOnClickListener {
                val intent = Intent(context, SecretCodeAct::class.java)
                startActivity(intent)

            }
        }

        if (btn_lucky_number != null) {
            btn_lucky_number?.setOnClickListener {
                val intent = Intent(context, LuckyNumberAct::class.java)
                startActivity(intent)

            }
        }

        if (btn_personal_msg != null) {
            btn_personal_msg?.setOnClickListener {
                val intent = Intent(context, PersonalMessageAct::class.java)
                startActivity(intent)
            }
        }

        if (btn_buddha_pang_assign != null) {
            btn_buddha_pang_assign?.setOnClickListener {
                val intent = Intent(context, BuddhaAssignAct::class.java)
                startActivity(intent)
            }
        }
        
        if (btn_sacred_temple_assign != null) {
            btn_sacred_temple_assign?.setOnClickListener {
                val intent = Intent(context, SacredTempleAssignAct::class.java)
                startActivity(intent)
            }
        }
        
        if (btn_merit_assign != null) {
            btn_merit_assign?.setOnClickListener {
                val intent = Intent(context, MeritAssignAct::class.java)
                startActivity(intent)
            }
        }

        if (btn_changenum_assign != null) {
            btn_changenum_assign?.setOnClickListener {
                val intent = Intent(context, ChangeNumAssignAct::class.java)
                startActivity(intent)
            }
        }

        if (btn_spell_assign != null) {
            btn_spell_assign?.setOnClickListener {
                val intent = Intent(context, SpellAssignAct::class.java)
                startActivity(intent)
            }
        }

        if (btn_inauspicious_assign != null) {
            btn_inauspicious_assign?.setOnClickListener {
                val intent = Intent(context, InauspiciousAssignAct::class.java)
                startActivity(intent)
            }
        }

        if (btn_auspicious_assign != null) {
            btn_auspicious_assign?.setOnClickListener {
                val intent = Intent(context, AuspiciousAssignAct::class.java)
                startActivity(intent)
            }
        }

        if (btn_tabian_manage != null) {
            btn_tabian_manage?.setOnClickListener {
                val intent = Intent(context, AdminTabianActivity::class.java)
                startActivity(intent)
            }
        }

        if (btn_admin_chat != null) {
            btn_admin_chat?.setOnClickListener {
                startActivity(Intent(context, com.numberniceic.ui.admin.AdminChatActivity::class.java))
            }
        }

        if (btn_add_dream != null) {
            btn_add_dream?.setOnClickListener {
                startActivity(Intent(context, com.numberniceic.ui.admin.AdminDreamActivity::class.java))
            }
        }

        if (btn_change_vip_status != null) {
            btn_change_vip_status?.setOnClickListener {
                startActivity(Intent(context, com.numberniceic.ui.admin.AdminUserActivity::class.java))
            }
        }

        if (btn_product_manage != null) {
            btn_product_manage?.setOnClickListener {
                startActivity(Intent(context, com.numberniceic.ui.admin.AdminProductActivity::class.java))
            }
        }

        if (btn_category_manage != null) {
            btn_category_manage?.setOnClickListener {
                startActivity(Intent(context, com.numberniceic.ui.admin.AdminCategoryActivity::class.java))
            }
        }

        if (btn_guest_address_manage != null) {
            btn_guest_address_manage?.setOnClickListener {
                startActivity(Intent(context, com.numberniceic.ui.admin.GuestAddressManagementActivity2::class.java))
            }
        }

        if (btn_manual_palette_manage != null) {
            btn_manual_palette_manage?.setOnClickListener {
                startActivity(Intent(context, com.numberniceic.ui.admin.ApiColorMappingAct::class.java))
            }
        }

        if (btn_zircon_orders != null) {
            btn_zircon_orders?.setOnClickListener {
                startActivity(Intent(context, com.numberniceic.ui.admin.AdminDreamOrdersActivity::class.java))
            }
        }


    }


    private fun setBtnAddCodeVip() {

        if (btn_vip_codeuser != null) {
            btn_vip_codeuser?.setOnClickListener {
                if (edt_vipcode_user != null) {

                    val vipcodeJson = JsonObject()
                    val userx = UserContextManager.userX(context!!)

                    val vipcode = edt_vipcode_user?.text.toString()

                    if (!vipcode.isEmpty()) {

                        vipcodeJson.addProperty("vipcode", vipcode)
                        vipcodeJson.addProperty("userid", userx!!.userId)
                        setVipcodeToUpgrade(vipcodeJson)

                    } else {
                        Toast.makeText(context, "กรุณากรอก VIP CODE!!", Toast.LENGTH_SHORT).show()
                    }


                }
            }
        }

        if (btn_vip_codesilver != null) {
            btn_vip_codesilver?.setOnClickListener {
                if (edt_vipcode_silver != null) {

                    val vipcodeJson = JsonObject()
                    val userx = UserContextManager.userX(context!!)


                    val vipcode = edt_vipcode_silver?.text.toString()

                    if (!vipcode.isEmpty()) {

                        vipcodeJson.addProperty("vipcode", vipcode)
                        vipcodeJson.addProperty("userid", userx!!.userId)

                        setVipcodeToUpgrade(vipcodeJson)
                    } else {
                        Toast.makeText(context, "กรุณากรอก VIP CODE!!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        if (btn_vip_codegold != null) {
            btn_vip_codegold?.setOnClickListener {
                if (edt_vipcode_gold != null) {

                    val vipcode = edt_vipcode_gold?.text.toString()

                    if (!vipcode.isEmpty()) {
                        val vipcodeJson = JsonObject()
                        val userx = UserContextManager.userX(context!!)

                        vipcodeJson.addProperty("vipcode", vipcode)
                        vipcodeJson.addProperty("userid", userx!!.userId)

                        setVipcodeToUpgrade(vipcodeJson)
                    } else {
                        Toast.makeText(context, "กรุณากรอก VIP CODE!!", Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }


    }

    private fun setVipcodeToUpgrade(vipcodeJson: JsonObject) {
         Log.d("VIPCODE", "Sending VIP upgrade request: $vipcodeJson")
         val apiService = RetrofitClient.instance.create(ApiService::class.java)
         apiService.userVipcodeUpgrade(vipcodeJson).enqueue(object : Callback<ServerVip> {
            override fun onResponse(call: Call<ServerVip>, response: Response<ServerVip>) {
                Log.d("VIPCODE", "Response code: ${response.code()}")
                if (response.isSuccessful && response.body() != null) {
                    val serverVip = response.body()!!
                    Log.d("VIPCODE", "ServerVip message: ${serverVip.message}, viplevel: ${serverVip.viplevel}")

                    if (serverVip.message == "success") {
                        handleVipSuccess(serverVip.viplevel)
                    } else {
                        // Handle specific error cases from server
                        showVipErrorMessage(serverVip)
                    }
                } else {
                    // Try to parse error body for specific messages
                    val errorBody = response.errorBody()?.string()
                    Log.d("VIPCODE", "Error body: $errorBody")
                    
                    if (errorBody != null) {
                        try {
                            // Extract JSON part only (server may append HTML error page)
                            val jsonPart = if (errorBody.contains("<!doctype") || errorBody.contains("<!DOCTYPE")) {
                                errorBody.substringBefore("<!doctype").substringBefore("<!DOCTYPE").trim()
                            } else {
                                errorBody
                            }
                            
                            val json = com.google.gson.JsonParser.parseString(jsonPart).asJsonObject
                            
                            // Check for success in error body (server bug workaround)
                            if (json.has("message") && json.get("message").asString == "success") {
                                val viplevel = json.get("viplevel")?.asString ?: "VIP"
                                handleVipSuccess(viplevel)
                                return
                            }
                            
                            // Handle specific error types
                            when {
                                json.has("wrong_not_real") -> {
                                    Toast.makeText(context, "ไม่พบ VIP Code นี้ในระบบ กรุณาตรวจสอบอีกครั้ง", Toast.LENGTH_LONG).show()
                                }
                                json.has("wrong_code_used") -> {
                                    Toast.makeText(context, "VIP Code นี้ถูกใช้งานแล้ว กรุณาติดต่อคุณนิน", Toast.LENGTH_LONG).show()
                                }
                                json.get("message")?.asString == "wrong" -> {
                                    Toast.makeText(context, "ไม่พบ VIP Code นี้ในระบบ กรุณาตรวจสอบอีกครั้ง", Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    Toast.makeText(context, "ไม่สามารถใช้ VIP Code นี้ได้", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("VIPCODE", "Failed to parse error body", e)
                            // Fallback: check for keywords in raw error body
                            when {
                                errorBody.contains("wrong_not_real") -> {
                                    Toast.makeText(context, "ไม่พบ VIP Code นี้ในระบบ กรุณาตรวจสอบอีกครั้ง", Toast.LENGTH_LONG).show()
                                }
                                errorBody.contains("wrong_code_used") -> {
                                    Toast.makeText(context, "VIP Code นี้ถูกใช้งานแล้ว กรุณาติดต่อคุณนิน", Toast.LENGTH_LONG).show()
                                }
                                errorBody.contains("\"message\":\"wrong\"") -> {
                                    Toast.makeText(context, "ไม่พบ VIP Code นี้ในระบบ กรุณาตรวจสอบอีกครั้ง", Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    Toast.makeText(context, "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            private fun showVipErrorMessage(serverVip: ServerVip) {
                // Check for specific error indicators in response
                val errorMsg = when {
                    serverVip.viplevel == "realVip" && serverVip.message == "wrong" -> 
                        "ไม่พบ VIP Code นี้ในระบบ หรือ Code ถูกใช้งานแล้ว"
                    else -> 
                        "ไม่สามารถใช้ VIP Code นี้ได้ กรุณาติดต่อคุณนิน"
                }
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }

            private fun handleVipSuccess(viplevel: String?) {
                Toast.makeText(context, "ยินดีต้อนรับท่านปรับระดับเป็น VIP $viplevel แล้ว", Toast.LENGTH_LONG).show()

                val userx = UserContextManager.userX(context!!)

                if (userx != null) {
                    userx.vipcode = viplevel
                }

                val userj = Gson().toJson(userx)

                val sharedVip = context!!.getSharedPreferences("userdata", Context.MODE_PRIVATE)
                val editor: SharedPreferences.Editor = sharedVip.edit()
                editor.putString("json", userj)
                editor.putString("codevip", userx?.vipcode)
                editor.putString("userid", userx?.userId)
                editor.apply()

                if (parentFragment is com.google.android.material.bottomsheet.BottomSheetDialogFragment) {
                    (parentFragment as com.google.android.material.bottomsheet.BottomSheetDialogFragment).dismiss()
                } else {
                    activity?.finish()
                }
            }

            override fun onFailure(call: Call<ServerVip>, t: Throwable) {
                Log.e("VIPCODE", "onFailure: ${t.message}", t)
                Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
            }
         })
    }


    private fun openDashboard() {
        // 1. ปิดหน้าสารบัญปัจจุบัน (MemberBottomSheet)
        (parentFragment as? com.google.android.material.bottomsheet.BottomSheetDialogFragment)?.dismiss()
        // 2. สั่งให้ AppActivity เปิด Dashboard (PersonNewsF)
        (requireActivity() as? com.numberniceic.ui.AppActivity)?.showDashboard(true)
    }

    private fun setTxtMembeDiamond(context: Context?) {
        val userX = UserContextManager.userX(context!!)
        if (userX != null) {
            if (txt_member_name_diamond != null) {
                txt_member_name_diamond?.text = userX.realName
            }

            if (txt_member_type_diamond != null) {
                setVipBadgeUI(txt_member_type_diamond, userX.vipcode ?: "", userX.status, onClick = { openDashboard() })
            }

            if (img_member_diamond != null) {
                img_member_diamond?.setImageResource(UserContextManager.getAvatarResId(userX.avatar))
            }

        }
    }


    private fun setTxtMembeGold(context: Context?) {
        val userX = UserContextManager.userX(context!!)
        if (userX != null) {
            if (txt_member_name_gold != null) {
                txt_member_name_gold?.text = userX.realName
            }

            if (txt_member_type_gold != null) {
                setVipBadgeUI(txt_member_type_gold, userX.vipcode ?: "", userX.status, onClick = { openDashboard() })
            }

            if (img_member_gold != null) {
                img_member_gold?.setImageResource(UserContextManager.getAvatarResId(userX.avatar))
            }

            if (btn_gold_upgrade_to_diamond != null) {
                btn_gold_upgrade_to_diamond?.setOnClickListener {
                    runLINEApp()
                }
            }
        }
    }

    private fun setTxtMembeSilver(context: Context?) {

        val userX = UserContextManager.userX(context!!)
        if (userX != null) {

            if (txt_member_name_silver != null) {
                txt_member_name_silver?.text = userX.realName
            }

            if (txt_member_type_silver != null) {
                setVipBadgeUI(txt_member_type_silver, userX.vipcode ?: "", userX.status, onClick = { openDashboard() })
            }

            if (img_member_silver != null) {
                img_member_silver?.setImageResource(UserContextManager.getAvatarResId(userX.avatar))
            }

            if (btn_upgrade_silver_gold != null) {
                btn_upgrade_silver_gold?.setOnClickListener {
                    runLINEApp()
                }
            }
        }
    }

    private fun setTxtMembe01(context: Context?) {

        val userX = UserContextManager.userX(context!!)
        if (userX != null) {
            if (txt_member01 != null) {
                txt_member01?.text = userX.realName
            }

            if (txt_member_type01 != null) {
                setVipBadgeUI(txt_member_type01, userX.vipcode ?: "", userX.status, onClick = { openDashboard() })
            }

            if (img_member01 != null) {
                img_member01?.setImageResource(UserContextManager.getAvatarResId(userX.avatar))
            }

            if (btn_upgrade01 != null) {
                btn_upgrade01?.setOnClickListener {
                    runLINEApp()
                }
            }
        }
    }

    private fun setImgMemberAdmin(context: Context?) {
        val userX = UserContextManager.userX(context!!)
        if (userX != null) {
            if (img_member_admin != null) {
                img_member_admin?.setImageResource(UserContextManager.getAvatarResId(userX.avatar))
            }
            // Update status explanation in all potential views
            updateStatusExplanation(userX.vipcode)
        }
    }

    private fun updateStatusExplanation(vipcode: String?) {
        val vip = vipcode?.lowercase()?.trim() ?: ""
        val displayStatus = when {
            vip == "vvip" -> "VVIP"
            vip == "mvp" -> "MVP"
            vip == "vip" || vip.contains("vip") || vip == "silver" || vip == "gold" || vip == "diamond" -> "VIP"
            vip == "normal" -> "Member"
            else -> "Member"
        }
        val text = "ระดับสมาชิก: $displayStatus"
        txt_status_explanation?.text = text
        txt_status_explanation_silver?.text = text
        txt_status_explanation_gold?.text = text
        txt_status_explanation_diamond?.text = text
    }

    private fun runLINEApp() {
        val userId = getString(R.string.line_id)
        val sentText = "line://ti/p/~$userId"
        try {
            val intentLine = Intent.parseUri(sentText, Intent.URI_INTENT_SCHEME)
            startActivity(intentLine)
            Toast.makeText(context, "กรุณารอสักครู่...", Toast.LENGTH_LONG).show()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "โปรดลงแอพพลิเคชั่น LINE เพื่อติดต่อกับเรา", Toast.LENGTH_LONG).show()
        }

    }


    private fun setBtnLogout() {


        if (btn_user_logout_silver != null) btn_user_logout_silver?.setOnClickListener {
            removeUserdata(it.context)
            this.userLogout()
        }


        if (btn_user_logout_admin != null) btn_user_logout_admin?.setOnClickListener {
            removeUserdata(it.context)
            this.userLogout()
        }

        if (btn_user_logout_normal != null) btn_user_logout_normal?.setOnClickListener {
            removeUserdata(it.context)
            this.userLogout()
        }

        if (btn_user_logout_gold != null) btn_user_logout_gold?.setOnClickListener {
            removeUserdata(it.context)
            this.userLogout()
        }

        if (btn_user_logout_diamond != null) btn_user_logout_diamond?.setOnClickListener {
            removeUserdata(it.context)
            this.userLogout()
        }
    }

    private fun userLogout() {
        // Navigate to AppActivity (main page) after logout and show login bottom sheet
        val intent = Intent(context, com.numberniceic.ui.AppActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        
        // Close current activity/bottom sheet
        if (parentFragment is com.google.android.material.bottomsheet.BottomSheetDialogFragment) {
            (parentFragment as com.google.android.material.bottomsheet.BottomSheetDialogFragment).dismiss()
        }
        activity?.finish()
    }


    private fun removeUserdata(context: Context) {
        val userx = UserContextManager.userX(context)
        if (userx != null) {
            // 1. Tell Server to clear this user's Token (DISABLED FOR TESTING)
            /*
            userx.userId?.let { memberId ->
                val json = JsonObject()
                json.addProperty("memberid", memberId)
                json.addProperty("token", "") // Clear token
                
                val apiService = RetrofitClient.instance.create(ApiService::class.java)
                apiService.updateFcmToken(json).enqueue(object : Callback<JsonObject> {
                    override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {}
                    override fun onFailure(call: Call<JsonObject>, t: Throwable) {}
                })
            }
            */

            // 2. Delete local Firebase instance token (DISABLED)
            /*
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { 
                // Token deleted locally
            }
            */

            // 3. Clear local storage thoroughly
            val sharedref = context.getSharedPreferences("userdata", Context.MODE_PRIVATE)
            sharedref.edit().clear().commit() 
            Log.d("LOGOUT", "UserData cleared")

            // 4. CLEAR ALL NOTIFICATIONS
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancelAll()
                Log.d("LOGOUT", "All notifications cleared")
            } catch (e: Exception) {
                Log.e("LOGOUT", "Failed to clear notifications: ${e.message}")
            }

            // 5. Clear chat session transient data but KEEP history markers (last_read_msg_id)
            val chatPrefs = context.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
            chatPrefs.edit()
                .remove("session_id")
                .putInt("unread_count", 0)
                .apply()
            Log.d("LOGOUT", "Chat session transient data cleared")
            
            // 🧹 Clear Person News Cache
            com.numberniceic.utils.PersonNewsCacheManager.clearCache(context)

            Toast.makeText(context, "ออกจากระบบแล้ว", Toast.LENGTH_LONG).show()
        }
    }


    private fun getTypeMember(type: String): String {


        return when (type) {
            "normal" -> "Member"
            "silver" -> "VIP Silver"
            "gold" -> "VIP Gold"
            "diamond" -> "VIP Diamond"
            "admin" -> "VIP Admin"
            "SpecialP" -> "Special Personal"
            else -> ""
        }


    }

    private fun setVipBadgeUI(composeView: androidx.compose.ui.platform.ComposeView?, vipcode: String, status: String?, onClick: (() -> Unit)? = null) {
        if (composeView == null) return
        composeView.setContent {
            val vip = vipcode.lowercase().trim()
            val stat = status?.lowercase()?.trim() ?: ""
            // ✅ ใช้ logic เดียวกับ UserContextManager.isAdmin()
            val isAdmin = stat == "admin" || vip.contains("admin") || vip == "administrator"
            val userBadge = when {
                vip == "vvip" -> "VVIP"
                vip == "mvp" -> "MVP"
                // ✅ เช็ค admin ก่อน VIP เพื่อไม่ให้ถูก map เป็น VIP
                isAdmin -> "ADMIN"
                vip.contains("vip") || vip == "gold" || vip == "silver" || vip == "diamond" -> "VIP"
                vip == "normal" -> "MB"
                else -> "GE"
            }
            val badgeColor = when(userBadge) {
                "GE" -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                "MB" -> androidx.compose.ui.graphics.Color(0xFF2196F3)
                "ADMIN" -> androidx.compose.ui.graphics.Color(0xFFD32F2F)
                else -> androidx.compose.ui.graphics.Color.Gray
            }

            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .size(64.dp)
                    .then(if (onClick != null) androidx.compose.ui.Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material3.ripple(bounded = false),
                        onClick = onClick
                    ) else androidx.compose.ui.Modifier),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                when (userBadge) {
                    "VVIP" -> {
                        // ✨ นำ Rotating VVIP Badge (VvipBadgeBitmapHelper) มาใช้ตามที่คุณนินต้องการ
                        val infiniteTransition = rememberInfiniteTransition()
                        val angle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(4000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )
                        val context = LocalContext.current
                        val bitmap = VvipBadgeBitmapHelper.createBadgeBitmap(context, 60, angle)
                        
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "VVIP",
                            modifier = androidx.compose.ui.Modifier.size(60.dp)
                        )
                    }
                    "ADMIN" -> androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        modifier = androidx.compose.ui.Modifier.size(48.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Color.White)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color(0xFFFFD700), // Bright Gold
                                            androidx.compose.ui.graphics.Color(0xFFD4AF37), // Metallic Gold
                                            androidx.compose.ui.graphics.Color(0xFF8A6508)  // Dark Gold
                                        )
                                    )
                                ),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "ADM",
                                color = androidx.compose.ui.graphics.Color.Black,
                                fontSize = 14.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                            )
                        }
                    }
                    "VIP" -> androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.numberniceic.R.drawable.ic_vip02),
                        contentDescription = "VIP",
                        modifier = androidx.compose.ui.Modifier.size(48.dp)
                    )
                    "MVP" -> androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.numberniceic.R.drawable.icon_gold01),
                        contentDescription = "MVP",
                        modifier = androidx.compose.ui.Modifier.size(48.dp)
                    )
                    else -> {
                        // ✨ ออกแบบตรา MB (และอื่นๆ) ให้สวยพรีเมียมเลียนแบบ VVIP ตามที่คุณนินต้องการ
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = androidx.compose.ui.graphics.Color.Black,
                            modifier = androidx.compose.ui.Modifier.size(48.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, 
                                androidx.compose.ui.graphics.Brush.sweepGradient(
                                    listOf(
                                        androidx.compose.ui.graphics.Color(0xFFB0BEC5), // Silver
                                        androidx.compose.ui.graphics.Color(0xFFFFFFFF), // White
                                        androidx.compose.ui.graphics.Color(0xFF90A4AE), // Steel
                                        androidx.compose.ui.graphics.Color(0xFFB0BEC5)  // Silver
                                    )
                                )
                            )
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                androidx.compose.material3.Text(
                                    text = userBadge,
                                    style = androidx.compose.ui.text.TextStyle(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(
                                                androidx.compose.ui.graphics.Color(0xFF64B5F6),
                                                androidx.compose.ui.graphics.Color(0xFF2196F3),
                                                androidx.compose.ui.graphics.Color(0xFF1976D2)
                                            )
                                        ),
                                        fontSize = 14.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}
