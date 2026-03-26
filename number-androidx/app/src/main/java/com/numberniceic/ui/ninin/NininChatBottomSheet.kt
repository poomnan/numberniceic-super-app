package com.numberniceic.ui.ninin

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.numberniceic.R
import android.widget.EditText
import android.widget.ImageButton
import android.util.Log
import android.content.Context
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.data.ninin.NininChatRequest
import com.numberniceic.data.ninin.NininChatResponse
import com.numberniceic.https.RetrofitClient
import com.numberniceic.https.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NininChatBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val MAX_DREAM_CHAT_COUNT = 30
        private const val DREAM_USAGE_PREFS = "ninin_usage_prefs"
        private const val DREAM_USAGE_KEY = "dream_chat_count"
        private const val DREAM_ACCESS_EXPIRE_AT_KEY = "dream_access_expire_at"
    }

    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnDebugExpire: View
    private lateinit var rvChat: RecyclerView
    private lateinit var progressBar: ProgressBar
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_ninin_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnDebugExpire = view.findViewById(R.id.btnDebugExpire)
        rvChat = view.findViewById(R.id.rvChat)
        progressBar = view.findViewById(R.id.progressBar)
        inputContainer = view.findViewById(R.id.inputContainer)

        btnDebugExpire.visibility = View.VISIBLE

        rvChat.layoutManager = LinearLayoutManager(context)
        rvChat.adapter = adapter

        // Add initial greeting
        adapter.addMessage(ChatMessage("สวัสดีจ้า พิมพ์สิ่งที่คุณฝันเห็นเพื่อรับคำทำนายฝัน โชคลาง และเลขนำโชค", false))

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                if (forceShowPackagesOnNextSend) {
                    adapter.addMessage(ChatMessage(msg, true))
                    etMessage.text.clear()
                    forceShowPackagesOnNextSend = false
                    showDreamPackageMessage()
                    return@setOnClickListener
                }
                if (!canSendDreamChat()) {
                    showDreamPackageMessage()
                    return@setOnClickListener
                }
                sendMessage(msg)
                etMessage.text.clear()
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

        // Show Loading (Optional: Add a "Typing..." message)
        progressBar.visibility = View.VISIBLE
        btnSend.isEnabled = false

        // Call API
        val api = RetrofitClient.instance.create(ApiService::class.java)
        
        val userx = com.numberniceic.utils.UserContextManager.userX(requireContext())
        val memberId = userx?.userId
        val guestId = android.provider.Settings.Secure.getString(requireContext().contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        
        val request = NininChatRequest(
            message = message,
            guestId = guestId,
            memberId = memberId
        )
        
        api.sendNininChat(request).enqueue(object : Callback<NininChatResponse> {
            override fun onResponse(call: Call<NininChatResponse>, response: Response<NininChatResponse>) {
                progressBar.visibility = View.GONE
                btnSend.isEnabled = true
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val isMember = com.numberniceic.utils.UserContextManager.userX(requireContext()) != null
                    // Add Ninin Response
                    val replyMsg = ChatMessage(
                        body.reply, 
                        false, 
                        body.dreamData, 
                        body.nameFound,
                        body.nameData,
                        body.showConsultButton,
                        body.showPackages
                    )
                    adapter.addMessage(replyMsg)
                    
                    if (body.usageCount != null) {
                        saveServerUsageCount(isMember, body.usageCount)
                    } else {
                        incrementDreamChatUsage()
                    }
                    if (!body.showPackages && body.freeRemaining != null && body.freeLimit != null) {
                        adapter.addMessage(
                            ChatMessage(
                                "สิทธิ์ใช้ฟรีคงเหลือ ${body.freeRemaining}/${body.freeLimit} ครั้ง",
                                false
                            )
                        )
                    }
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

    private fun scrollToBottom() {
        if (adapter.itemCount > 0) {
            rvChat.smoothScrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun canSendDreamChat(): Boolean {
        // Backend is authoritative for package entitlement and expiry.
        return true
    }

    private fun getDreamChatUsageCount(): Int {
        val prefs = requireContext().getSharedPreferences(DREAM_USAGE_PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(DREAM_USAGE_KEY, 0)
    }

    private fun incrementDreamChatUsage() {
        val prefs = requireContext().getSharedPreferences(DREAM_USAGE_PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(DREAM_USAGE_KEY, 0)
        prefs.edit().putInt(DREAM_USAGE_KEY, current + 1).apply()
    }

    private fun saveServerUsageCount(isMember: Boolean, count: Int) {
        val prefs = requireContext().getSharedPreferences(DREAM_USAGE_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(DREAM_USAGE_KEY, count).apply()
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

    private fun hideKeyboard() {
        val ctx = context ?: return
        val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        view?.windowToken?.let { token ->
            imm?.hideSoftInputFromWindow(token, 0)
        }
    }

    private fun grantDreamAccess(packageItem: DreamPackageItem) {
        val ctx = context ?: return
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
        ctx.getSharedPreferences(DREAM_USAGE_PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putLong(DREAM_ACCESS_EXPIRE_AT_KEY, expireAt)
            .apply()
    }

    private fun hasDreamAccess(): Boolean {
        val ctx = context ?: return false
        val prefs = ctx.getSharedPreferences(DREAM_USAGE_PREFS, android.content.Context.MODE_PRIVATE)
        val expireAt = prefs.getLong(DREAM_ACCESS_EXPIRE_AT_KEY, 0L)
        if (expireAt == 0L) {
            return false
        }
        if (expireAt != Long.MAX_VALUE && expireAt <= System.currentTimeMillis()) {
            prefs.edit().remove(DREAM_ACCESS_EXPIRE_AT_KEY).apply()
            return false
        }
        return true
    }

    private fun startDreamPackagePayment(packageItem: DreamPackageItem) {
        val ctx = context ?: return
        val msg = "สนใจสมัครแพ็กเกจทำนายฝันแบบ${packageItem.title} ราคา ${packageItem.amount.toInt()} บาทค่ะ"
        val intent = android.content.Intent(ctx, com.numberniceic.ui.ChatComposeActivity::class.java).apply {
            putExtra("initial_message", msg)
        }
        ctx.startActivity(intent)
        dismiss()
    }

    private fun getDreamAccessSuccessText(packageItem: DreamPackageItem): String {
        return if (packageItem.code == "dream_lifetime") {
            "ชำระเงินสำเร็จแล้ว แพ็กเกจตลอดชีวิตเปิดใช้งานเรียบร้อย สามารถทำนายฝันต่อได้ทันทีค่ะ"
        } else {
            val ctx = context ?: return "ชำระเงินสำเร็จแล้ว สามารถทำนายฝันต่อได้ทันทีค่ะ"
            val prefs = ctx.getSharedPreferences(DREAM_USAGE_PREFS, android.content.Context.MODE_PRIVATE)
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
        val ctx = context ?: return
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
                                Toast.makeText(ctx, "Secret Code นี้ไม่ใช่แพ็กเกจทำนายฝัน", Toast.LENGTH_SHORT).show()
                                return
                            }
                        }
                        
                        val userx = com.numberniceic.utils.UserContextManager.userX(ctx)
                        val memberId = userx?.userId
                        val guestId = android.provider.Settings.Secure.getString(ctx.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
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

                                    Toast.makeText(ctx, "ปลดล็อกทำนายฝันด้วย Secret Code สำเร็จ!", Toast.LENGTH_LONG).show()
                                    adapter.clearDreamPackageMessages()
                                    adapter.addMessage(ChatMessage("รหัสลับถูกต้อง! คุณนินพร้อมทำนายฝันให้คุณต่อแล้วค่ะ พิมพ์บอกความฝันได้เลย", false))
                                    setMainInputVisible(true)
                                    scrollToBottom()

                                    try {
                                        val pref = ctx.getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
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
                                    Toast.makeText(ctx, "ยืนยันสิทธิ์กับเซิร์ฟเวอร์ไม่สำเร็จ (${redeemResponse.code()})", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<com.numberniceic.data.ninin.NininRedeemAccessResponse>, t: Throwable) {
                                Toast.makeText(ctx, "เปิดสิทธิ์ไม่สำเร็จ: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                        
                    } else {
                        Toast.makeText(ctx, "VIP Code ไม่ถูกต้อง หรือถูกใช้ไปแล้ว", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(ctx, "เกิดข้อผิดพลาดในการตรวจสอบ (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.numberniceic.data.member.MemberVipCollectionDao>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(ctx, "เชื่อมต่อเซิร์ฟเวอร์ไม่ได้ โปรดตรวจสอบอินเทอร์เน็ต", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun forceExpireDreamAccessDebug() {
        val ctx = context ?: return
        ctx.getSharedPreferences(DREAM_USAGE_PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putLong(DREAM_ACCESS_EXPIRE_AT_KEY, System.currentTimeMillis() - 1000L)
            .apply()
        forceShowPackagesOnNextSend = true
        adapter.clearDreamPackageMessages()
        Toast.makeText(ctx, "ทำให้หมดสิทธิ์แล้ว: ส่งข้อความครั้งถัดไปจะแสดงแพ็กเกจทันที", Toast.LENGTH_SHORT).show()
    }
    
    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialogTheme
    }
}
