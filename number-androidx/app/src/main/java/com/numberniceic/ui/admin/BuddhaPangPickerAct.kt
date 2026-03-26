package com.numberniceic.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.adapters.BuddhaPangPickerAdapter
import com.numberniceic.data.admin.BuddhaPang
import com.numberniceic.data.admin.ServerMessage
import com.numberniceic.data.admin.UserZ
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.utils.ImageUrlResolver
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BuddhaPangPickerAct : AppCompatActivity() {

    private var currentAssignment: JsonObject? = null
    private var userZ: UserZ? = null
    private lateinit var recycleBuddha: RecyclerView
    private lateinit var progressBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buddha_pang_picker)

        userZ = intent.getParcelableExtra("user_z")

        val toolbar = findViewById<Toolbar>(R.id.toolbar_buddha_picker)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "เลือกพระพุทธรูป"

        findViewById<TextView>(R.id.txt_target_user).text = "${userZ?.realName ?: ""} ${userZ?.surName ?: ""}"

        recycleBuddha = findViewById(R.id.recycle_buddha_pang)
        progressBar = findViewById(R.id.progress_bar)

        loadCurrentAssignment()
        loadBuddhaPangs()
    }

    private fun loadCurrentAssignment() {
        val memberId = userZ?.memberId ?: return
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        // We reuse the user-side API for checking status, or create a new one. 
        // User side API: getAssignedBuddhaPang(memberid) returns JSON with annual/lifetime objects.
        apiService.getAssignedBuddhaPang(memberId).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                 if (response.isSuccessful && response.body() != null) {
                     currentAssignment = response.body()!!
                 } else {
                     Log.e("BuddhaPicker", "Failed to load assignment: " + response.code())
                 }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) { }
        })
    }
    
    private fun loadBuddhaPangs() {
        progressBar.visibility = View.VISIBLE
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getBuddhaPangs().enqueue(object : Callback<List<BuddhaPang>> {
            override fun onResponse(call: Call<List<BuddhaPang>>, response: Response<List<BuddhaPang>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    displayBuddhaPangs(response.body()!!)
                } else {
                    Toast.makeText(this@BuddhaPangPickerAct, "โหลดข้อมูลล้มเหลว", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<BuddhaPang>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@BuddhaPangPickerAct, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayBuddhaPangs(items: List<BuddhaPang>) {
        val adapter = BuddhaPangPickerAdapter(items, object : BuddhaPangPickerAdapter.OnItemClickListener {
            override fun onItemClick(item: BuddhaPang) {
                showConfirmSheet(item)
            }

            fun onSendNotiClick(item: BuddhaPang) {
                android.app.AlertDialog.Builder(this@BuddhaPangPickerAct)
                    .setTitle("ยืนยันการส่ง")
                    .setMessage("ต้องการส่งพระ '${item.pangName}' ให้กับ ${userZ?.realName} ใช่หรือไม่?")
                    .setPositiveButton("ยืนยัน") { dialog, _ ->
                        sendBuddhaNoti(item)
                        dialog.dismiss()
                    }
                    .setNegativeButton("ยกเลิก") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        })
        recycleBuddha.layoutManager = LinearLayoutManager(this)
        recycleBuddha.adapter = adapter
    }

    private fun sendBuddhaNoti(item: BuddhaPang) {
        if (userZ == null) return
        progressBar.visibility = View.VISIBLE

        val json = JsonObject()
        json.addProperty("memberid", userZ!!.memberId)
        json.addProperty("title", "แนะนำพระประจำวัน") 
        json.addProperty("body", "คุณนินแนะนำ : ${item.pangName}")
        json.addProperty("url", "") 
        json.addProperty("type", "buddha_card") // Maybe handled as special type?

        // Reusing assignMerit for generic notification sending
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.assignMerit(json).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@BuddhaPangPickerAct, "ส่งการแจ้งเตือนเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@BuddhaPangPickerAct, "เกิดข้อผิดพลาด: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@BuddhaPangPickerAct, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ... (loadBuddhaPangs same) ...

    private fun showConfirmSheet(pang: BuddhaPang) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_buddha_assign_bottom_sheet, null)
        
        dialog.setContentView(view)

        // Ensure Dialog expands fully and handles keyboard
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val imgBuddha = view.findViewById<android.widget.ImageView>(R.id.bs_img_buddha)
        val txtName = view.findViewById<TextView>(R.id.bs_txt_buddha_name)
        val btnSave = view.findViewById<android.widget.Button>(R.id.bs_btn_save)
        val rgType = view.findViewById<android.widget.RadioGroup>(R.id.bs_rg_type)
        
        // Add Status Text View dynamically or if layout has it? 
        // Layout doesn't have it. I'll insert a TextView or reuse Title.
        // Let's assume I can change Button text or add a view logic.
        
        txtName.text = pang.pangName
        
        val imageUrl = ImageUrlResolver.resolve(pang.imageUrl)

        if (imageUrl.isNotEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.buddha)
                .error(R.drawable.pra_preang)
                .into(imgBuddha)
        } else {
            imgBuddha.setImageResource(R.drawable.pra_preang)
        }
        
        // Check Status Function
        fun checkStatus() {
            // 1. Reset Texts first
            val rbAnnual = view.findViewById<android.widget.RadioButton>(R.id.bs_rb_annual)
            val rbLifetime = view.findViewById<android.widget.RadioButton>(R.id.bs_rb_lifetime)
            
            rbAnnual.text = "ประจำปี"
            rbLifetime.text = "ตลอดชีพ"
            
            var userHasAnnual = false
            var userHasLifetime = false
            var annualPangId = "-1"
            var lifetimePangId = "-1"

            // 2. Check Annual Status
            if (currentAssignment != null && currentAssignment!!.has("annual") && !currentAssignment!!.get("annual").isJsonNull) {
                val element = currentAssignment!!.get("annual")
                var obj: JsonObject? = null
                
                if (element.isJsonArray) {
                    val arr = element.asJsonArray
                    if (arr.size() > 0) {
                        obj = arr.get(arr.size() - 1).asJsonObject // Get Latest
                    }
                } else if (element.isJsonObject) {
                     obj = element.asJsonObject
                }

                if (obj != null && obj.has("id")) {
                    userHasAnnual = true
                    annualPangId = obj.get("id").asString
                    
                    var dateStr = ""
                    if (obj.has("assigned_at") && !obj.get("assigned_at").isJsonNull) {
                        try {
                             // "2026-01-26 10:00:00" -> "26/01/2026"
                             val rowDate = obj.get("assigned_at").asString
                             val parts = rowDate.split(" ")[0].split("-")
                             if (parts.size == 3) dateStr = " " + parts[2] + "/" + parts[1] + "/" + parts[0]
                        } catch (e: Exception) {}
                    }
                    
                    // Update Text
                    rbAnnual.text = "ประจำปี (ส่งแล้ว ✅$dateStr)"
                    rbAnnual.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                }
            }
            
            // 3. Check Lifetime Status
            if (currentAssignment != null && currentAssignment!!.has("lifetime") && !currentAssignment!!.get("lifetime").isJsonNull) {
               val element = currentAssignment!!.get("lifetime")
                var obj: JsonObject? = null
                
                if (element.isJsonArray) {
                    val arr = element.asJsonArray
                    if (arr.size() > 0) {
                        obj = arr.get(arr.size() - 1).asJsonObject // Get Latest
                    }
                } else if (element.isJsonObject) {
                     obj = element.asJsonObject
                }

                if (obj != null && obj.has("id")) {
                    userHasLifetime = true
                    lifetimePangId = obj.get("id").asString

                    var dateStr = ""
                    if (obj.has("assigned_at") && !obj.get("assigned_at").isJsonNull) {
                        try {
                             val rowDate = obj.get("assigned_at").asString
                             val parts = rowDate.split(" ")[0].split("-")
                             if (parts.size == 3) dateStr = " " + parts[2] + "/" + parts[1] + "/" + parts[0]
                        } catch (e: Exception) {}
                    }

                    // Update Text
                    rbLifetime.text = "ตลอดชีพ (ส่งแล้ว ✅$dateStr)"
                    rbLifetime.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                }
            }

            // 4. Button Logic (Based on Selection)
            val isAnnualSelected = rgType.checkedRadioButtonId == R.id.bs_rb_annual
            val currentPangId = pang.id.toString()
            
            val isAlreadySentThisPang = if (isAnnualSelected) (annualPangId == currentPangId) 
                                        else (lifetimePangId == currentPangId)
            
            if (isAlreadySentThisPang) {
                 btnSave.text = "ส่งแล้ว (กดเพื่อส่งซ้ำ)"
                 btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")) // Green
            } else {
                 btnSave.text = "บันทึกและส่งการแจ้งเตือน"
                 btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3")) // Blue
            }
        }
        
        rgType.setOnCheckedChangeListener { _, _ -> checkStatus() }
        checkStatus() // Initial check

        btnSave.setOnClickListener {
            val type = if (rgType.checkedRadioButtonId == R.id.bs_rb_lifetime) "lifetime" else "annual"
            val typeThai = if (type == "lifetime") "ตลอดชีพ" else "ประจำปี"

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("ยืนยันการบันทึก")
                .setMessage("ต้องการบันทึกพระ '${pang.pangName}' เป็นพระ$typeThai ให้กับ ${userZ?.realName} ใช่หรือไม่?")
                .setPositiveButton("ยืนยัน") { confirmDialog, _ ->
                    dialog.dismiss()
                    assignBuddhaToUser(pang, "", type)
                    confirmDialog.dismiss()
                }
                .setNegativeButton("ยกเลิก") { confirmDialog, _ ->
                    confirmDialog.dismiss()
                }
                .show()
        }

        dialog.show()
    }

    private fun assignBuddhaToUser(pang: BuddhaPang, customDesc: String, type: String) {
        val memberId = userZ?.memberId ?: return
        val memberIdInt = memberId.toIntOrNull() ?: run {
            Toast.makeText(this, "Error: invalid member ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        val body = JsonObject()
        body.addProperty("memberid", memberIdInt)
        body.addProperty("buddha_id", pang.id)
        body.addProperty("custom_description", customDesc)
        body.addProperty("assignment_type", type)

        progressBar.visibility = View.VISIBLE
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.assignBuddhaPang(body).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.get("status")?.asString == "success") {
                    
                    // Success Feedback
                    // Success Feedback
                    val toast = Toast.makeText(this@BuddhaPangPickerAct, "✅ ส่งเรียบร้อยแล้ว!", Toast.LENGTH_LONG)
                    // toast.view access is deprecated and can cause crash on Android 11+
                    toast.show()
                    
                    // Refresh status
                    loadCurrentAssignment()
                    
                } else {
                    Toast.makeText(this@BuddhaPangPickerAct, "บันทึกไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@BuddhaPangPickerAct, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
