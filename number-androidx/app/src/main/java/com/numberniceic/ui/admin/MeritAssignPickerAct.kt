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
import com.numberniceic.https.NetworkConfig
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MeritAssignPickerAct : AppCompatActivity() {

    private lateinit var txtTargetUser: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var targetUser: UserZ? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merit_assign_picker)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_merit_picker)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "วิธีทำบุญ"

        txtTargetUser = findViewById(R.id.txt_target_user_merit)
        recyclerView = findViewById(R.id.recycle_merit)
        progressBar = findViewById(R.id.progress_bar_merit)

        targetUser = intent.getParcelableExtra("user_z")

        if (targetUser != null) {
            txtTargetUser.text = "ส่งวิธีทำบุญให้: ${targetUser!!.realName}"
            loadMeritItems()
        } else {
            Toast.makeText(this, "ไม่พบข้อมูลผู้ใช้", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadMeritItems() {
        // Hardcoded items as per user request image
        val items = listOf(
            MeritItem("1", "ปางวันอาทิตย์", "${NetworkConfig.BASE_URL}/merit/view?id=1", "${NetworkConfig.BASE_URL}/uploads/buddha/sun.png"),
            MeritItem("2", "ปางวันจันทร์", "${NetworkConfig.BASE_URL}/merit/view?id=2", "${NetworkConfig.BASE_URL}/uploads/buddha/mon.png"),
            MeritItem("3", "ปางวันอังคาร", "${NetworkConfig.BASE_URL}/merit/view?id=3", "${NetworkConfig.BASE_URL}/uploads/buddha/tue.png"),
            MeritItem("4", "ปางวันพุธ", "${NetworkConfig.BASE_URL}/merit/view?id=4", "${NetworkConfig.BASE_URL}/uploads/buddha/wed_day.png"),
            MeritItem("5", "ปางวันพฤหัสบดี", "${NetworkConfig.BASE_URL}/merit/view?id=5", "${NetworkConfig.BASE_URL}/uploads/buddha/thu.png"),
            MeritItem("6", "ปางวันศุกร์", "${NetworkConfig.BASE_URL}/merit/view?id=6", "${NetworkConfig.BASE_URL}/uploads/buddha/fri.png"),
            MeritItem("7", "ปางวันเสาร์", "${NetworkConfig.BASE_URL}/merit/view?id=7", "${NetworkConfig.BASE_URL}/uploads/buddha/sat.png"),
            MeritItem("8", "ปางวันพุธกลางคืน", "${NetworkConfig.BASE_URL}/merit/view?id=8", "${NetworkConfig.BASE_URL}/uploads/buddha/wed_night.png"),
            MeritItem("9", "วิธีการปรับดวงเรื่องความรักเงินงาน", "${NetworkConfig.BASE_URL}/merit/view?id=9", "${NetworkConfig.BASE_URL}/uploads/buddha/default.png"),
            MeritItem("10", "การทำบุญช่วยส่งเสริมชะตาอาภัพคู่", "${NetworkConfig.BASE_URL}/merit/view?id=10", "${NetworkConfig.BASE_URL}/uploads/buddha/default.png")
        )

        val adapter = MeritAdapter(items, object : MeritAdapter.ActionListener {
            override fun onViewClick(item: MeritItem) {
                // Open URL in Bottom Sheet
                val bottomSheet = com.numberniceic.ui.merit.MeritWebViewBottomSheet.newInstance(item.url)
                bottomSheet.show(supportFragmentManager, "MeritWebViewBottomSheet")
            }

            override fun onSendClick(item: MeritItem) {
                 android.app.AlertDialog.Builder(this@MeritAssignPickerAct)
                    .setTitle("ยืนยันการส่ง")
                    .setMessage("ต้องการส่ง '${item.title}' ให้กับ ${targetUser?.realName} ใช่หรือไม่?")
                    .setPositiveButton("ยืนยัน") { dialog, _ ->
                        sendMeritToUser(item)
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

    private fun sendMeritToUser(item: MeritItem) {
        if (targetUser == null) return

        progressBar.visibility = View.VISIBLE

        val json = JsonObject()
        json.addProperty("memberid", targetUser!!.memberId)
        json.addProperty("title", "วิธีการทำบุญ") // Title for notification
        json.addProperty("body", "คุณนินแนะนำ : ${item.title}") // Body
        json.addProperty("url", item.url)
        json.addProperty("type", "webview_merit") // Custom type

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        // Check if we have an endpoint for Generic Notify or creating a new one?
        // Let's use a new endpoint or reuse if possible.
        // Assuming we need to Create "assignMerit" endpoint in PHP.
        
        apiService.assignMerit(json).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@MeritAssignPickerAct, "ส่งข้อมูลเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@MeritAssignPickerAct, "เกิดข้อผิดพลาด: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@MeritAssignPickerAct, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
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
