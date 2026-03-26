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
import com.numberniceic.adapters.AuspiciousHistoryAdapter
import com.numberniceic.data.admin.InauspiciousData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuspiciousFormAct : AppCompatActivity() {

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
        setContentView(R.layout.activity_auspicious_form)

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
        recycleHistory = findViewById(R.id.recycle_auspicious_history)
        recycleHistory.layoutManager = LinearLayoutManager(this)

        txtTargetUser.text = "${targetUser?.realName} (ID: ${targetUser?.memberId})"

        btnCancelEdit.setOnClickListener {
            resetForm()
        }

        btnSave.setOnClickListener {
            saveData()
        }
    }

    private fun saveData() {
        val content = edtContent.text.toString().trim()
        val type = if (radioYear.isChecked) "year" else "life"
        
        // Auto-generate title based on selection
        val autoTitle = if (type == "year") "วันมงคลปีนี้" else "วันมงคลตลอดชีวิต"

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
        
        if (type == "year") {
            try {
                val expiryMinutes = calculateExpiryMinutesUntilBirthday(targetUser?.birthDat)
                body.addProperty("expiry_duration", expiryMinutes)
            } catch (e: Exception) {
                body.addProperty("expiry_duration", 525600)
            }
        }

        api.assignAuspicious(body).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                if (response.isSuccessful) {
                    Toast.makeText(this@AuspiciousFormAct, "บันทึกเรียบร้อย", Toast.LENGTH_SHORT).show()
                    loadHistory()
                    resetForm()
                } else {
                    Toast.makeText(this@AuspiciousFormAct, "เกิดข้อผิดพลาด: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                btnSave.isEnabled = true
                Toast.makeText(this@AuspiciousFormAct, "เชื่อมต่อล้มเหลว: ${t.message}", Toast.LENGTH_SHORT).show()
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
        api.getAuspiciousHistoryAll(mid).enqueue(object : Callback<List<InauspiciousData>> {
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
        api.getAssignedAuspicious(memberId).enqueue(object : Callback<List<InauspiciousData>> {
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
        val adapter = AuspiciousHistoryAdapter(items, object : AuspiciousHistoryAdapter.OnItemClickListener {
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
                findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll_view_form)?.smoothScrollTo(0, 0)
            }

            override fun onDeleteClick(item: InauspiciousData) {
                androidx.appcompat.app.AlertDialog.Builder(this@AuspiciousFormAct)
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
        api.deleteAuspiciousAssignment(body).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@AuspiciousFormAct, "ลบเรียบร้อย", Toast.LENGTH_SHORT).show()
                    loadHistory()
                } else {
                    Toast.makeText(this@AuspiciousFormAct, "ลบล้มเหลว", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AuspiciousFormAct, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun calculateExpiryMinutesUntilBirthday(birthdayStr: String?): Int {
        if (birthdayStr.isNullOrEmpty()) return 525600 
        try {
            val dateOnly = birthdayStr.split(" ")[0]
            val cleanStr = dateOnly.replace("/", "-").replace(".", "-")
            val parts = cleanStr.split("-").filter { it.isNotEmpty() }
            if (parts.size < 3) return 525600
            val month: Int
            val day: Int
            if (parts[0].length == 4) {
                month = parts[1].toIntOrNull() ?: 1
                day = parts[2].toIntOrNull() ?: 1
            } else {
                day = parts[0].toIntOrNull() ?: 1
                month = parts[1].toIntOrNull() ?: 1
            }
            val now = org.joda.time.DateTime.now()
            val safeMonth = month.coerceIn(1, 12)
            val lastDay = org.joda.time.DateTime(now.year, safeMonth, 1, 0, 0).dayOfMonth().maximumValue
            val safeDay = day.coerceIn(1, lastDay)
            var nextBirthday = org.joda.time.DateTime(now.year, safeMonth, safeDay, 0, 0, 0, 1)
            if (nextBirthday.isBeforeNow) nextBirthday = nextBirthday.plusYears(1)
            val delayMillis = nextBirthday.millis - now.millis
            val delayMinutes = (delayMillis / (60 * 1000)).toInt()
            return if (delayMinutes > 0) delayMinutes else 525600
        } catch (e: Exception) {
            return 525600
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
