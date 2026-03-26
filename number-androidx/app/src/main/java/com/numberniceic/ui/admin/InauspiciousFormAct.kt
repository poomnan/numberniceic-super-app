package com.numberniceic.ui.admin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.data.admin.UserZ
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.adapters.InauspiciousHistoryAdapter
import com.numberniceic.data.admin.InauspiciousData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class InauspiciousFormAct : AppCompatActivity() {

    private lateinit var txtTargetUser: TextView
    private lateinit var radioGroup: RadioGroup
    private lateinit var radioYear: RadioButton
    private lateinit var radioLife: RadioButton
    private lateinit var edtContent: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancelEdit: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var recycleHistory: RecyclerView

    private var targetUser: UserZ? = null
    private var currentEditingId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inauspicious_form)

        targetUser = intent.getParcelableExtra("user_data")
        if (targetUser == null) {
            Toast.makeText(this, "ข้อมูลผู้ใช้ไม่ถูกต้อง", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initUI()
        loadHistory()
    }

    private fun initUI() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_form)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "บันทึกและส่งแจ้งเตือน"

        txtTargetUser = findViewById(R.id.txt_target_user)
        radioGroup = findViewById(R.id.radio_group_type)
        radioYear = findViewById(R.id.radio_year)
        radioLife = findViewById(R.id.radio_life)
        edtContent = findViewById(R.id.edt_title)
        btnSave = findViewById(R.id.btn_save)
        btnCancelEdit = findViewById(R.id.btn_cancel_edit)
        progressBar = findViewById(R.id.progress_bar_form)
        recycleHistory = findViewById(R.id.recycle_inauspicious_history)
        recycleHistory.layoutManager = LinearLayoutManager(this)

        txtTargetUser.text = "${targetUser?.realName} (ID: ${targetUser?.memberId})"

        btnCancelEdit.setOnClickListener {
            resetForm()
        }

        // Pre-fill if in edit mode
        if (intent.getBooleanExtra("edit_mode", false)) {
            val prevType = intent.getStringExtra("inauspicious_data_type")
            val prevContent = intent.getStringExtra("inauspicious_data_content")
            
            edtContent.setText(prevContent)
            if (prevType == "year") {
                radioYear.isChecked = true
            } else if (prevType == "life") {
                radioLife.isChecked = true
            }
            
            supportActionBar?.title = "แก้ไขและส่งแจ้งเตือนใหม่"
        }

        btnSave.setOnClickListener {
            saveData()
        }
    }

    private fun saveData() {
        val content = edtContent.text.toString().trim()
        val type = if (radioYear.isChecked) "year" else "life"
        
        // Auto-generate title based on selection to maintain UI consistency
        val autoTitle = if (type == "year") "วันอัปมงคลปีนี้" else "วันอัปมงคลตลอดชีวิต"

        if (content.isEmpty()) {
            edtContent.error = "กรุณากรอกข้อมูล"
            return
        }

        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        val api = RetrofitClient.instance.create(ApiService::class.java)
        val body = JsonObject()
        body.addProperty("memberid", targetUser?.memberId)
        body.addProperty("type", type)
        body.addProperty("title", autoTitle)
        body.addProperty("description", content)
        if (currentEditingId != null) {
            body.addProperty("id", currentEditingId)
        }
        
        // Calculate expiry duration based on user's birthday (for "year" type only)
        if (type == "year") {
            try {
                val expiryMinutes = calculateExpiryMinutesUntilBirthday(targetUser?.birthDat)
                body.addProperty("expiry_duration", expiryMinutes)
                android.util.Log.d("InauspiciousForm", "Calculated expiry: $expiryMinutes minutes until birthday for user ${targetUser?.realName}")
            } catch (e: Exception) {
                android.util.Log.e("InauspiciousForm", "Error calculating expiry: ${e.message}", e)
                // Fallback to 1 year if calculation fails
                body.addProperty("expiry_duration", 525600) // 365 days in minutes
            }
        }

        api.assignInauspicious(body).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@InauspiciousFormAct, "บันทึกเรียบร้อย", Toast.LENGTH_SHORT).show()
                    loadHistory()
                    resetForm()
                } else {
                    Toast.makeText(this@InauspiciousFormAct, "เกิดข้อผิดพลาด: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                Toast.makeText(this@InauspiciousFormAct, "เชื่อมต่อล้มเหลว: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun resetForm() {
        edtContent.setText("")
        currentEditingId = null
        btnSave.text = "บันทึกและส่งแจ้งเตือน"
        btnCancelEdit.visibility = View.GONE
        supportActionBar?.title = "บันทึกและส่งแจ้งเตือน"
    }

    private fun loadHistory() {
        val mid = targetUser?.memberId ?: return
        val api = RetrofitClient.instance.create(ApiService::class.java)
        
        progressBar.visibility = View.VISIBLE
        api.getInauspiciousHistoryAll(mid).enqueue(object : Callback<List<InauspiciousData>> {
            override fun onResponse(call: Call<List<InauspiciousData>>, response: Response<List<InauspiciousData>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    displayHistory(response.body()!!)
                } else {
                    fallbackLoadHistoryActiveOnly(mid)
                }
            }

            override fun onFailure(call: Call<List<InauspiciousData>>, t: Throwable) {
                fallbackLoadHistoryActiveOnly(mid)
            }
        })
    }

    private fun fallbackLoadHistoryActiveOnly(memberId: String) {
        val api = RetrofitClient.instance.create(ApiService::class.java)
        api.getAssignedInauspicious(memberId).enqueue(object : Callback<List<InauspiciousData>> {
            override fun onResponse(call: Call<List<InauspiciousData>>, response: Response<List<InauspiciousData>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    displayHistory(response.body()!!)
                } else {
                    displayHistory(emptyList())
                }
            }

            override fun onFailure(call: Call<List<InauspiciousData>>, t: Throwable) {
                progressBar.visibility = View.GONE
                displayHistory(emptyList())
            }
        })
    }

    private fun displayHistory(items: List<InauspiciousData>) {
        val adapter = InauspiciousHistoryAdapter(items, object : InauspiciousHistoryAdapter.OnItemClickListener {
            override fun onEditClick(item: InauspiciousData) {
                currentEditingId = item.id
                edtContent.setText(item.description)
                if (item.type == "year") {
                    radioYear.isChecked = true
                } else {
                    radioLife.isChecked = true
                }
                
                btnSave.text = "ตกลงแก้ไขและส่งแจ้งเตือน"
                btnCancelEdit.visibility = View.VISIBLE
                supportActionBar?.title = "แก้ไขแจ้งเตือนเดิม"
                
                edtContent.requestFocus()
                edtContent.setSelection(edtContent.text?.length ?: 0)
                
                // Scroll to top to see the form
                findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll_view_form)?.smoothScrollTo(0, 0)
                
                Toast.makeText(this@InauspiciousFormAct, "เข้าสู่โหมดแก้ไข", Toast.LENGTH_SHORT).show()
            }

            override fun onDeleteClick(item: InauspiciousData) {
                androidx.appcompat.app.AlertDialog.Builder(this@InauspiciousFormAct)
                    .setTitle("ยืนยันการลบ")
                    .setMessage("ต้องการลบรายการนี้ใช่หรือไม่?")
                    .setPositiveButton("ลบ") { _, _ -> deleteHistoryItem(item.id) }
                    .setNegativeButton("ยกเลิก", null)
                    .show()
            }
        })
        recycleHistory.adapter = adapter
    }

    private fun deleteHistoryItem(id: Int) {
        val api = RetrofitClient.instance.create(ApiService::class.java)
        val body = JsonObject()
        body.addProperty("id", id)
        body.addProperty("memberid", targetUser?.memberId)

        progressBar.visibility = View.VISIBLE
        api.deleteInauspiciousAssignment(body).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@InauspiciousFormAct, "ลบเรียบร้อย", Toast.LENGTH_SHORT).show()
                    loadHistory()
                } else {
                    Toast.makeText(this@InauspiciousFormAct, "ลบล้มเหลว", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@InauspiciousFormAct, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun calculateExpiryMinutesUntilBirthday(birthdayStr: String?): Int {
        android.util.Log.d("InauspiciousForm", "=== Calculating Expiry ===")
        android.util.Log.d("InauspiciousForm", "Birthday string: '$birthdayStr'")
        
        if (birthdayStr.isNullOrEmpty()) {
            android.util.Log.e("InauspiciousForm", "Birthday is null/empty, defaulting to 1 year")
            return 525600 // 1 year in minutes
        }
        
        try {
            // Parse birthday (support multiple formats)
            val dateOnly = birthdayStr.split(" ")[0]
            val cleanStr = dateOnly.replace("/", "-").replace(".", "-")
            val parts = cleanStr.split("-").filter { it.isNotEmpty() }
            
            if (parts.size < 3) {
                android.util.Log.e("InauspiciousForm", "Invalid date format, defaulting to 1 year")
                return 525600
            }
            
            val month: Int
            val day: Int
            
            if (parts[0].length == 4) {
                // YYYY-MM-DD
                month = parts[1].toIntOrNull() ?: 1
                day = parts[2].toIntOrNull() ?: 1
            } else {
                // DD-MM-YYYY
                day = parts[0].toIntOrNull() ?: 1
                month = parts[1].toIntOrNull() ?: 1
            }
            
            val now = org.joda.time.DateTime.now()
            val safeMonth = month.coerceIn(1, 12)
            val lastDay = org.joda.time.DateTime(now.year, safeMonth, 1, 0, 0).dayOfMonth().maximumValue
            val safeDay = day.coerceIn(1, lastDay)
            
            var nextBirthday = org.joda.time.DateTime(now.year, safeMonth, safeDay, 0, 0, 0, 1)
            
            if (nextBirthday.isBeforeNow) {
                nextBirthday = nextBirthday.plusYears(1)
            }
            
            val delayMillis = nextBirthday.millis - now.millis
            val delayMinutes = (delayMillis / (60 * 1000)).toInt()
            
            android.util.Log.d("InauspiciousForm", "Next birthday: ${nextBirthday.toString("yyyy-MM-dd")}")
            android.util.Log.d("InauspiciousForm", "Delay: $delayMinutes minutes (~${delayMinutes / 1440} days)")
            android.util.Log.d("InauspiciousForm", "=== Calculation Complete ===")
            
            return if (delayMinutes > 0) delayMinutes else 525600 // Fallback to 1 year
            
        } catch (e: Exception) {
            android.util.Log.e("InauspiciousForm", "Exception: ${e.message}", e)
            return 525600 // 1 year in minutes
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
