package com.numberniceic.ui.admin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
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
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PersonalMessageComposeAct : AppCompatActivity() {

    private var userz: UserZ? = null
    private lateinit var edtTitle: TextInputEditText
    private lateinit var edtBody: TextInputEditText
    private lateinit var btnSend: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_message_compose)

        userz = intent.getParcelableExtra("userz")

        val toolbar = findViewById<Toolbar>(R.id.toolbar_personal_msg_compose_act)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = "เขียนข้อความพิเศษ"

        findViewById<TextView>(R.id.txt_target_user).text = "${userz?.realName} ${userz?.surName} (@${userz?.userName})"
        
        edtTitle = findViewById(R.id.edt_msg_title)
        edtBody = findViewById(R.id.edt_msg_body)
        btnSend = findViewById(R.id.btn_send_msg)
        progressBar = findViewById(R.id.progress_bar)

        btnSend.setOnClickListener {
            val title = edtTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกหัวข้อ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("ยืนยันการส่งข้อความ")
                .setMessage("ต้องการส่งข้อความพิเศษนี้ให้กับ ${userz?.realName} ใช่หรือไม่?")
                .setPositiveButton("ยืนยัน") { dialog, _ ->
                    sendNotification()
                    dialog.dismiss()
                }
                .setNegativeButton("ยกเลิก") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun sendNotification() {
        val title = edtTitle.text.toString().trim()
        val body = edtBody.text.toString().trim()

        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกข้อมูลให้ครบถ้วน", Toast.LENGTH_SHORT).show()
            return
        }

        if (userz == null || userz?.memberId == null) {
            Toast.makeText(this, "ไม่พบข้อมูลผู้ใช้", Toast.LENGTH_SHORT).show()
            return
        }

        btnSend.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val json = JsonObject()
        json.addProperty("memberid", userz?.memberId)
        json.addProperty("title", title)
        json.addProperty("body", body)
        json.addProperty("type", "custom")

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.sendCustomNotify(json).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                progressBar.visibility = View.GONE
                btnSend.isEnabled = true
                
                if (response.isSuccessful) {
                    val rawJson = response.body()?.string() ?: ""
                    try {
                        val jsonRes = com.google.gson.JsonParser.parseString(rawJson).asJsonObject
                        if (jsonRes.get("status").asString == "success") {
                            Toast.makeText(this@PersonalMessageComposeAct, "ส่งข้อความสำเร็จแล้ว ✅", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            val msg = jsonRes.get("message")?.asString ?: "Unknown Error"
                            Toast.makeText(this@PersonalMessageComposeAct, "ส่งล้มเหลว: $msg", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@PersonalMessageComposeAct, "ส่งข้อความแล้ว (ตรวจสอบสถานะไม่ได้)", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@PersonalMessageComposeAct, "ส่งล้มเหลว: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                progressBar.visibility = View.GONE
                btnSend.isEnabled = true
                Toast.makeText(this@PersonalMessageComposeAct, "เกิดข้อผิดพลาด: ${t.message}", Toast.LENGTH_SHORT).show()
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
