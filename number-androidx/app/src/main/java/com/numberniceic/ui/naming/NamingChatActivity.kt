package com.numberniceic.ui.naming

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.data.naming.NamingChatRequest
import com.numberniceic.data.naming.NamingChatResponse
import com.numberniceic.https.RetrofitClient
import com.numberniceic.https.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NamingChatActivity : AppCompatActivity() {

    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var rvChat: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var txtHeader: TextView
    private val adapter = NamingChatAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_naming_assistant)
        
        supportActionBar?.hide()

        initView()
        initListener()
        
        adapter.addMessage(ChatMessage("สวัสดีค่ะ คุณทญา ยินดีให้บริการตั้งชื่อมงคลค่ะ\nบอกแนวชื่อที่ชอบ (เช่น ชื่อแปลว่าความสุข) และวันเกิด (เช่น จันทร์) ได้เลยนะคะ", false))
    }

    private fun initView() {
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        rvChat = findViewById(R.id.rvChat)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        txtHeader = findViewById(R.id.txtHeader)

        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter

        // Window Insets
        val headerLayout = findViewById<View>(R.id.headerLayout)
        val bottomLayout = findViewById<View>(R.id.bottomLayout)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            headerLayout.setPadding(headerLayout.paddingLeft, systemBars.top, headerLayout.paddingRight, headerLayout.paddingBottom)
            
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val bottomInset = if (isImeVisible) ime.bottom else systemBars.bottom
            
            bottomLayout.setPadding(
                bottomLayout.paddingLeft, 
                bottomLayout.paddingTop, 
                bottomLayout.paddingRight, 
                bottomInset + (8 * resources.displayMetrics.density).toInt()
            )
            insets
        }
    }

    private fun initListener() {
        btnBack.setOnClickListener {
            finish()
        }

        findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupDays).setOnCheckedChangeListener { group, checkedId ->
            if (checkedId != -1) {
                val chip = group.findViewById<com.google.android.material.chip.Chip>(checkedId)
                if (chip != null) {
                    val day = chip.text.toString()
                    val cleanDay = day.replace("(", "").replace(")", "")
                    val currentText = etMessage.text.toString()
                    if (currentText.isNotEmpty() && !currentText.endsWith(" ")) {
                        etMessage.append(" ")
                    }
                    etMessage.append("เกิดวัน$cleanDay") // Append "Born on [Day]"
                }
                group.clearCheck() // Clear to allow re-selection
            }
        }

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg)
                etMessage.text.clear()
                hideKeyboard()
            }
        }
    }

    private fun sendMessage(message: String) {
        adapter.addMessage(ChatMessage(message, true))
        scrollToBottom()

        progressBar.visibility = View.VISIBLE
        btnSend.isEnabled = false

        val api = RetrofitClient.instance.create(ApiService::class.java)
        
        val userx = com.numberniceic.utils.UserContextManager.userX(this)
        val memberId = userx?.userId
        val guestId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        
        val request = NamingChatRequest(
            message = message,
            guestId = guestId,
            memberId = memberId
        )
        
        api.sendNamingChat(request).enqueue(object : Callback<NamingChatResponse> {
            override fun onResponse(call: Call<NamingChatResponse>, response: Response<NamingChatResponse>) {
                progressBar.visibility = View.GONE
                btnSend.isEnabled = true
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    
                    // Check if this is a limit message
                    if (body.reply.contains("โปรดสมัครสมาชิก")) {
                        // Don't increment, just show the message
                        val replyMsg = ChatMessage(
                            text = body.reply,
                            isUser = false,
                            nameData = body.data
                        )
                        adapter.addMessage(replyMsg)
                        scrollToBottom()
                    } else {
                        // Normal response - increment local counter
                        val usageManager = com.numberniceic.utils.GuestUsageManager(this@NamingChatActivity)
                        usageManager.increment()
                        
                        val replyMsg = ChatMessage(
                            text = body.reply,
                            isUser = false,
                            nameData = body.data
                        )
                        adapter.addMessage(replyMsg)
                        scrollToBottom()
                    }
                } else {
                    adapter.addMessage(ChatMessage("ระบบขัดข้อง (Error: ${response.code()})", false))
                    scrollToBottom()
                }
            }

            override fun onFailure(call: Call<NamingChatResponse>, t: Throwable) {
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
}
