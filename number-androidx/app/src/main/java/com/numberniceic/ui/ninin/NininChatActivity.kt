package com.numberniceic.ui.ninin

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.util.Log
import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.ninin.NininChatRequest
import com.numberniceic.data.ninin.NininChatResponse
import com.numberniceic.https.RetrofitClient
import com.numberniceic.https.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NininChatActivity : AppCompatActivity() {

    companion object {
        private const val DREAM_USAGE_PREFS = "ninin_usage_prefs"
        private const val DREAM_ACCESS_EXPIRE_AT_KEY = "dream_access_expire_at"
    }

    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var rvChat: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnDebugExpire: View
    private lateinit var txtHeader: TextView
    private lateinit var inputContainer: View
    private var forceShowPackagesOnNextSend = false
    private val adapter = NininChatAdapter(
        onDreamPackageClick = { selectedPackage ->
            startDreamPackagePayment(selectedPackage)
        },
        onSecretCodeSubmit = { code ->
            submitSecretCode(code)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ninin_chat)
        
        // Hide default action bar if present
        supportActionBar?.hide()

        initView()
        initListener()
        
        // Greeting
        adapter.addMessage(ChatMessage("สวัสดีจ้า พิมพ์สิ่งที่คุณฝันเห็นเพื่อรับคำทำนายฝัน โชคลาง และเลขนำโชค", false))
    }

    private fun initView() {
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        rvChat = findViewById(R.id.rvChat)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnDebugExpire = findViewById(R.id.btnDebugExpire)
        txtHeader = findViewById(R.id.txtHeader)

        btnDebugExpire.visibility = View.GONE

        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        // 🛠️ Handle System Navigation Bar & Keyboard Insets (Fix for S22 and similar devices)
        val headerLayout = findViewById<android.view.View>(R.id.headerLayout)
        inputContainer = findViewById(R.id.inputContainer)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            // Adjust header for status bar
            headerLayout.setPadding(headerLayout.paddingLeft, systemBars.top, headerLayout.paddingRight, headerLayout.paddingBottom)
            
            // Adjust input for keyboard (IME) or navigation bar
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val bottomInset = if (isImeVisible) ime.bottom else systemBars.bottom
            
            inputContainer.setPadding(
                inputContainer.paddingLeft, 
                inputContainer.paddingTop, 
                inputContainer.paddingRight, 
                bottomInset + (8 * resources.displayMetrics.density).toInt()
            )
            
            insets
        }
    }

    private fun initListener() {
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                if (forceShowPackagesOnNextSend) {
                    adapter.addMessage(ChatMessage(msg, true))
                    etMessage.text.clear()
                    forceShowPackagesOnNextSend = false
                    showDreamPackageMessage()
                    hideKeyboard()
                    return@setOnClickListener
                }
                if (!canSendDreamChat()) {
                    showDreamPackageMessage()
                    return@setOnClickListener
                }
                sendMessage(msg)
                etMessage.text.clear()
                
                // Hide keyboard after sending
                hideKeyboard()
            }
        }

        btnDebugExpire.setOnClickListener {
            forceExpireDreamAccessDebug()
        }
    }

    private fun sendMessage(message: String) {
        // Add User Message
        adapter.addMessage(ChatMessage(message, true))
        scrollToBottom()

        // Show Loading
        progressBar.visibility = View.VISIBLE
        btnSend.isEnabled = false

        // Call API
        val api = RetrofitClient.instance.create(ApiService::class.java)
        
        val userx = com.numberniceic.utils.UserContextManager.userX(this)
        val memberId = userx?.userId
        val guestId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        
        val request = NininChatRequest(
            message = message,
            guestId = guestId,
            memberId = memberId
        )
        
        android.util.Log.d("NininChat", "Sending: guestId=$guestId, memberId=$memberId, message=$message")
        
        api.sendNininChat(request).enqueue(object : Callback<NininChatResponse> {
            override fun onResponse(call: Call<NininChatResponse>, response: Response<NininChatResponse>) {
                progressBar.visibility = View.GONE
                btnSend.isEnabled = true
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val formattedReply = formatDreamReply(body.reply)
                    val replyMsg = ChatMessage(
                        text = formattedReply,
                        isUser = false,
                        dreamData = body.dreamData,
                        showConsultButton = body.showConsultButton,
                        showDreamPackages = body.showPackages
                    )
                    adapter.addMessage(replyMsg)

                    if (!canSendDreamChat()) {
                        showDreamPackageMessage()
                    }
                    scrollToBottom()
                } else {
                    adapter.addMessage(ChatMessage("ขออภัย ระบบขัดข้องชั่วคราว (Error: ${response.code()})", false))
                    scrollToBottom()
                }
            }

            override fun onFailure(call: Call<NininChatResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                btnSend.isEnabled = true
                adapter.addMessage(ChatMessage("เชื่อมต่อล้มเหลว: ${t.message}", false))
                scrollToBottom()
            }
        })
    }

    private fun hideKeyboard() {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        currentFocus?.let { view ->
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun scrollToBottom() {
        if (adapter.itemCount > 0) {
            rvChat.smoothScrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun canSendDreamChat(): Boolean {
        // Backend is the source of truth for access/expiry.
        // Always allow sending so server can decide free-limit vs paid entitlement.
        return true
    }

    private fun showDreamPackageMessage() {
        etMessage.clearFocus()
        hideKeyboard()
        setMainInputVisible(false)
        if (adapter.hasDreamPackageMessage()) {
            return
        }
        adapter.addMessage(
            ChatMessage(
                text = "คุณใช้สิทธิ์ทำนายฝัน ความเชื่อโชคลางครบ 30 ครั้งแล้ว ถ้าต้องการใช้งานต่อเนื่อง โปรดเลือกแพ็คเกจตามรายละเอียดด้านล่าง ขอบคุณจ้า",
                isUser = false,
                showDreamPackages = true
            )
        )
        scrollToBottom()
    }

    private fun setMainInputVisible(visible: Boolean) {
        if (!visible) {
            etMessage.clearFocus()
            etMessage.isFocusable = false
            etMessage.isFocusableInTouchMode = false
        } else {
            etMessage.isFocusable = true
            etMessage.isFocusableInTouchMode = true
        }
        inputContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun hasDreamAccess(): Boolean {
        val prefs = getSharedPreferences(DREAM_USAGE_PREFS, MODE_PRIVATE)
        val expireAt = prefs.getLong(DREAM_ACCESS_EXPIRE_AT_KEY, 0L)
        if (expireAt == 0L) {
            return false
        }
        if (expireAt != Long.MAX_VALUE && expireAt <= System.currentTimeMillis()) {
            prefs.edit().remove(DREAM_ACCESS_EXPIRE_AT_KEY).apply()
            return false
        }
        return expireAt == Long.MAX_VALUE || expireAt > System.currentTimeMillis()
    }

    private fun grantDreamAccess(packageItem: DreamPackageItem) {
        val calendar = java.util.Calendar.getInstance()
        val expireAt = when (packageItem.code) {
            "dream_monthly" -> {
                calendar.add(java.util.Calendar.MONTH, 1)
                calendar.timeInMillis
            }
            "dream_yearly" -> {
                calendar.add(java.util.Calendar.YEAR, 1)
                calendar.timeInMillis
            }
            else -> Long.MAX_VALUE
        }
        getSharedPreferences(DREAM_USAGE_PREFS, MODE_PRIVATE)
            .edit()
            .putLong(DREAM_ACCESS_EXPIRE_AT_KEY, expireAt)
            .apply()
    }

    private fun startDreamPackagePayment(packageItem: DreamPackageItem) {
        val msg = "สนใจสมัครแพ็กเกจทำนายฝันแบบ${packageItem.title} ราคา ${packageItem.amount.toInt()} บาทค่ะ"
        val intent = android.content.Intent(this, com.numberniceic.ui.ChatComposeActivity::class.java).apply {
            putExtra("initial_message", msg)
        }
        startActivity(intent)
    }

    private fun getDreamAccessSuccessText(packageItem: DreamPackageItem): String {
        return if (packageItem.code == "dream_lifetime") {
            "ชำระเงินสำเร็จแล้ว แพ็กเกจตลอดชีวิตเปิดใช้งานเรียบร้อย สามารถทำนายฝันต่อได้ทันทีค่ะ"
        } else {
            val prefs = getSharedPreferences(DREAM_USAGE_PREFS, MODE_PRIVATE)
            val expireAt = prefs.getLong(DREAM_ACCESS_EXPIRE_AT_KEY, 0L)
            val formatted = if (expireAt > 0L) {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(expireAt))
            } else {
                "-"
            }
            "ชำระเงินสำเร็จแล้ว แพ็กเกจใช้งานได้ถึง $formatted สามารถทำนายฝันต่อได้ทันทีค่ะ"
        }
    }

    private fun submitSecretCode(code: String) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        
        progressBar.visibility = View.VISIBLE
        
        apiService.checkVipCodeApi(code).enqueue(object : Callback<com.numberniceic.data.member.MemberVipCollectionDao> {
            override fun onResponse(call: Call<com.numberniceic.data.member.MemberVipCollectionDao>, response: Response<com.numberniceic.data.member.MemberVipCollectionDao>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    if (data.member != "fail" && data.vipcode != null) {
                        
                        val viptype = data.vipcode.viptype ?: ""

                        // Only dream secret codes are accepted in this flow.
                        val packageItem = when (viptype.lowercase(Locale.ROOT)) {
                            "dream_lifetime" -> DREAM_PACKAGE_LIFETIME
                            "dream_yearly" -> DREAM_PACKAGE_YEARLY
                            "dream_monthly" -> DREAM_PACKAGE_MONTHLY
                            else -> {
                                Toast.makeText(this@NininChatActivity, "Secret Code นี้ไม่ใช่แพ็กเกจทำนายฝัน", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                        
                        val userx = com.numberniceic.utils.UserContextManager.userX(this@NininChatActivity)
                        val memberId = userx?.userId
                        val guestId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
                        val redeemRequest = com.numberniceic.data.ninin.NininRedeemAccessRequest(
                            memberId = memberId,
                            guestId = guestId,
                            packageCode = packageItem.code,
                            secretCode = code
                        )

                        apiService.redeemNininAccess(redeemRequest).enqueue(object : Callback<com.numberniceic.data.ninin.NininRedeemAccessResponse> {
                            override fun onResponse(
                                call: Call<com.numberniceic.data.ninin.NininRedeemAccessResponse>,
                                redeemResponse: Response<com.numberniceic.data.ninin.NininRedeemAccessResponse>
                            ) {
                                if (redeemResponse.isSuccessful && redeemResponse.body()?.ok == true) {
                                    grantDreamAccess(packageItem)

                                    Toast.makeText(this@NininChatActivity, "ปลดล็อกทำนายฝันด้วย Secret Code สำเร็จ!", Toast.LENGTH_LONG).show()
                                    adapter.clearDreamPackageMessages()
                                    adapter.addMessage(ChatMessage("รหัสลับถูกต้อง! คุณนินพร้อมทำนายฝันให้คุณต่อแล้วค่ะ พิมพ์บอกความฝันได้เลย", false))
                                    setMainInputVisible(true)
                                    scrollToBottom()

                                    try {
                                        val pref = getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                                        pref.edit().apply {
                                            putString("member", data.member)
                                            putString("vipcode", data.vipcode.vipcode)
                                            putString("viptype", data.vipcode.viptype)
                                            putString("vipstatus", data.vipcode.vipstatus)
                                        }.apply()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    Toast.makeText(this@NininChatActivity, "ยืนยันสิทธิ์กับเซิร์ฟเวอร์ไม่สำเร็จ (${redeemResponse.code()})", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<com.numberniceic.data.ninin.NininRedeemAccessResponse>, t: Throwable) {
                                Toast.makeText(this@NininChatActivity, "เปิดสิทธิ์ไม่สำเร็จ: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                        
                    } else {
                        Toast.makeText(this@NininChatActivity, "VIP Code ไม่ถูกต้อง หรือถูกใช้ไปแล้ว", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@NininChatActivity, "เกิดข้อผิดพลาดในการตรวจสอบ (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.numberniceic.data.member.MemberVipCollectionDao>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@NininChatActivity, "เชื่อมต่อเซิร์ฟเวอร์ไม่ได้ โปรดตรวจสอบอินเทอร์เน็ต", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun forceExpireDreamAccessDebug() {
        getSharedPreferences(DREAM_USAGE_PREFS, MODE_PRIVATE)
            .edit()
            .putLong(DREAM_ACCESS_EXPIRE_AT_KEY, System.currentTimeMillis() - 1000L)
            .apply()
        forceShowPackagesOnNextSend = true
        adapter.clearDreamPackageMessages()
        Toast.makeText(this, "ทำให้หมดสิทธิ์แล้ว: ส่งข้อความครั้งถัดไปจะแสดงแพ็กเกจทันที", Toast.LENGTH_SHORT).show()
    }

    private fun formatDreamReply(text: String?): String {
        if (text.isNullOrBlank()) {
            return "คำนี้ยังไม่มีในระบบโปรดใช้คำใกล้เคียง"
        }
        var cleaned = text.trim()
        
        // In case the API returns its own not found message, also override it
        if (cleaned.contains("ไม่พบข้อมูล") || cleaned == "ไม่พบคำทำนาย") {
            return "คำนี้ยังไม่มีในระบบโปรดใช้คำใกล้เคียง"
        }
        
        cleaned = cleaned.replace("ทำนายว่า ฝันว่า", "ทำนายว่า\n\n🔹 ฝันว่า")
        cleaned = cleaned.replace(" ถ้าฝันว่า", "\n🔹 ถ้าฝันว่า")
        cleaned = cleaned.replace(" ทายว่า จะ", " หมายความว่าจะ")
        cleaned = cleaned.replace(" ทายว่าจะ", " หมายความว่าจะ")
        cleaned = cleaned.replace(" ทายว่า ", " หมายความว่า ")
        cleaned = cleaned.replace(Regex("(?m)^\\s*จำนวนครั้งที่ใช้งานสะสม\\s*:\\s*\\d+\\s*ครั้ง\\s*$"), "")
        cleaned = cleaned.replace(Regex("(?m)^\\s*สิทธิ์ใช้ฟรีคงเหลือ\\s*\\d+\\s*/\\s*\\d+\\s*ครั้ง\\s*$"), "")
        cleaned = cleaned.replace(Regex("\\n{3,}"), "\n\n").trim()
        return cleaned
    }
}
