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
import com.numberniceic.adapters.SacredTempleAdapter
import com.numberniceic.data.admin.SacredTemple
import com.numberniceic.data.admin.UserZ
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SacredTemplePickerAct : AppCompatActivity() {

    private lateinit var txtTargetUser: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var targetUser: UserZ? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sacred_temple_picker)

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar_temple_picker))
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "เลือกวัดศักดิ์สิทธิ์"

        txtTargetUser = findViewById(R.id.txt_target_user)
        recyclerView = findViewById(R.id.recycle_temple)
        progressBar = findViewById(R.id.progress_bar)

        targetUser = intent.getParcelableExtra("user_z")
        
        if (targetUser != null) {
            txtTargetUser.text = "แนะนำวัดให้คุณ: ${targetUser!!.realName}"
            loadTemples()
        } else {
            Toast.makeText(this, "ไม่พบข้อมูลผู้ใช้", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadTemples() {
        progressBar.visibility = View.VISIBLE
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getSacredTemples().enqueue(object : Callback<List<SacredTemple>> {
            override fun onResponse(call: Call<List<SacredTemple>>, response: Response<List<SacredTemple>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val temples = response.body()!!
                    if (temples.isNotEmpty()) {
                        displayTemples(temples)
                    } else {
                        Toast.makeText(this@SacredTemplePickerAct, "ยังไม่มีวัดในระบบ", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SacredTemplePickerAct, "เกิดข้อผิดพลาดในการโหลดข้อมูล", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<SacredTemple>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@SacredTemplePickerAct, "เกิดข้อผิดพลาด: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayTemples(temples: List<SacredTemple>) {
        val adapter = SacredTempleAdapter(temples, object : SacredTempleAdapter.OnItemClickListener {
            override fun onItemClick(item: SacredTemple) {
                androidx.appcompat.app.AlertDialog.Builder(this@SacredTemplePickerAct)
                    .setTitle("ยืนยันการแนะนำ")
                    .setMessage("ต้องการแนะนำวัด '${item.templeName}' ให้กับ ${targetUser?.realName} ใช่หรือไม่?")
                    .setPositiveButton("ยืนยัน") { dialog, _ ->
                        assignTempleToUser(item)
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



    private fun assignTempleToUser(temple: SacredTemple) {
        if (targetUser == null) return

        progressBar.visibility = View.VISIBLE

        val json = JsonObject()
        json.addProperty("memberid", targetUser!!.memberId)
        json.addProperty("temple_id", temple.id)
        json.addProperty("custom_description", "") // Admin can add custom desc later if needed

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.assignSacredTemple(json).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.has("activity") && result.get("activity").asString == "success") {
                        Toast.makeText(
                            this@SacredTemplePickerAct,
                            "แนะนำวัด '${temple.templeName}' ให้ ${targetUser!!.realName} เรียบร้อยแล้ว",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(this@SacredTemplePickerAct, "ไม่สามารถบันทึกข้อมูลได้", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SacredTemplePickerAct, "เกิดข้อผิดพลาด", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@SacredTemplePickerAct, "เกิดข้อผิดพลาด: ${t.message}", Toast.LENGTH_SHORT).show()
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
