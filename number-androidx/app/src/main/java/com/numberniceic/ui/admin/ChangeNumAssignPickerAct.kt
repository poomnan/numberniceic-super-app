package com.numberniceic.ui.admin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.adapters.MeritAdapter
import com.numberniceic.data.admin.MeritItem
import com.numberniceic.data.admin.UserZ
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangeNumAssignPickerAct : AppCompatActivity() {

    private lateinit var txtTargetUser: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var targetUser: UserZ? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merit_assign_picker) // Reuse layout

        val toolbar = findViewById<Toolbar>(R.id.toolbar_merit_picker)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "ขั้นตอนการเปลี่ยนแปลง"

        txtTargetUser = findViewById(R.id.txt_target_user_merit)
        recyclerView = findViewById(R.id.recycle_merit)
        progressBar = findViewById(R.id.progress_bar_merit)

        targetUser = intent.getParcelableExtra("user_z")

        if (targetUser != null) {
            txtTargetUser.text = "ส่งขั้นตอนการเปลี่ยนแปลงให้: ${targetUser!!.realName}"
            loadChangeNumItems()
        } else {
            Toast.makeText(this, "ไม่พบข้อมูลผู้ใช้", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadChangeNumItems() {
        val items = listOf(
            MeritItem("phone", "ขั้นตอนการเปลี่ยนแปลงเบอร์โทรศัพท์", "${com.numberniceic.https.NetworkConfig.BASE_URL}/changenum/view?id=phone", "${com.numberniceic.https.NetworkConfig.BASE_URL}/uploads/merit/change_default.png"),
            MeritItem("namenick", "ขั้นตอนการเปลี่ยนแปลงชื่อเล่น", "${com.numberniceic.https.NetworkConfig.BASE_URL}/changenum/view?id=namenick", "${com.numberniceic.https.NetworkConfig.BASE_URL}/uploads/merit/change_default.png"),
            MeritItem("namesur", "ขั้นตอนการเปลี่ยนแปลงชื่อจริง นามสกุล", "${com.numberniceic.https.NetworkConfig.BASE_URL}/changenum/view?id=namesur", "${com.numberniceic.https.NetworkConfig.BASE_URL}/uploads/merit/change_default.png"),
            MeritItem("tabian", "ขั้นตอนการเปลี่ยนแปลงทะเบียนรถ", "${com.numberniceic.https.NetworkConfig.BASE_URL}/changenum/view?id=tabian", "${com.numberniceic.https.NetworkConfig.BASE_URL}/uploads/merit/change_default.png"),
            MeritItem("home", "ขั้นตอนการเปลี่ยนแปลงบ้านเลขที่", "${com.numberniceic.https.NetworkConfig.BASE_URL}/changenum/view?id=home", "${com.numberniceic.https.NetworkConfig.BASE_URL}/uploads/merit/change_default.png")
        )

        val adapter = MeritAdapter(items, object : MeritAdapter.ActionListener {
            override fun onViewClick(item: MeritItem) {
                // Open URL in Bottom Sheet
                val bottomSheet = com.numberniceic.ui.merit.MeritWebViewBottomSheet.newInstance(item.url)
                bottomSheet.show(supportFragmentManager, "MeritWebViewBottomSheet")
            }

            override fun onSendClick(item: MeritItem) {
                // Show Confirmation Dialog
                android.app.AlertDialog.Builder(this@ChangeNumAssignPickerAct)
                    .setTitle("ยืนยันการส่ง")
                    .setMessage("ต้องการส่ง '${item.title}' ให้กับ ${targetUser?.realName} ใช่หรือไม่?")
                    .setPositiveButton("ยืนยัน") { dialog, _ ->
                        sendChangeNumToUser(item)
                        dialog.dismiss()
                    }
                    .setNegativeButton("ยกเลิก") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun sendChangeNumToUser(item: MeritItem) {
        if (targetUser == null) return

        progressBar.visibility = View.VISIBLE

        val json = JsonObject()
        json.addProperty("memberid", targetUser!!.memberId)
        json.addProperty("title", "ขั้นตอนการเปลี่ยนแปลง") // Title for notification
        json.addProperty("body", "คุณนินแนะนำ : ${item.title}") // Body
        json.addProperty("url", item.url)
        json.addProperty("type", "webview_changenum") // Custom type

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        
        apiService.assignMerit(json).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@ChangeNumAssignPickerAct, "ส่งข้อมูลเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ChangeNumAssignPickerAct, "เกิดข้อผิดพลาด: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ChangeNumAssignPickerAct, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
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
