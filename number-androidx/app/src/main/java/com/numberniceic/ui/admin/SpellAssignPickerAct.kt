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
import com.numberniceic.adapters.SpellAdapter
import com.numberniceic.data.admin.SpellItem
import com.numberniceic.data.admin.SpellListResponse
import com.numberniceic.data.admin.UserZ
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SpellAssignPickerAct : AppCompatActivity() {

    private lateinit var txtTargetUser: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var targetUser: UserZ? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spell_assign_picker)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_spell_picker)
        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "เลือกคาถา/คำเตือน"

        txtTargetUser = findViewById(R.id.txt_target_user_spell)
        recyclerView = findViewById(R.id.recycle_spell)
        progressBar = findViewById(R.id.progress_bar_spell)

        targetUser = intent.getParcelableExtra("user_z")

        if (targetUser != null) {
            txtTargetUser.text = "ส่งคาถา/คำเตือนให้: ${targetUser!!.realName}"
            loadSpells()
        } else {
            Toast.makeText(this, "ไม่พบข้อมูลผู้ใช้", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadSpells() {
        progressBar.visibility = View.VISIBLE
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        
        apiService.getAllSpells(targetUser?.memberId).enqueue(object : Callback<SpellListResponse> {
            override fun onResponse(call: Call<SpellListResponse>, response: Response<SpellListResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.status == "success" && result.data.isNotEmpty()) {
                         displaySpells(result.data)
                    } else {
                         Toast.makeText(this@SpellAssignPickerAct, "ไม่พบข้อมูลรายการ", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SpellAssignPickerAct, "เกิดข้อผิดพลาด: ${response.code()} ${response.message()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<SpellListResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@SpellAssignPickerAct, "Failed: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
    
    private fun displaySpells(items: List<SpellItem>) {
        val adapter = SpellAdapter(items, object : SpellAdapter.ActionListener {
            override fun onSendClick(item: SpellItem) {
                 val editText = android.widget.EditText(this@SpellAssignPickerAct)
                 editText.setText(item.note ?: "")
                 editText.hint = "ใส่ข้อความบันทึก/คำเตือนพิเศษ..."
                 
                 val container = android.widget.FrameLayout(this@SpellAssignPickerAct)
                 val params = android.widget.FrameLayout.LayoutParams(
                     android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                     android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                 )
                 val margin = (20 * resources.displayMetrics.density).toInt()
                 params.leftMargin = margin
                 params.rightMargin = margin
                 params.topMargin = margin / 2
                 editText.layoutParams = params
                 container.addView(editText)

                 android.app.AlertDialog.Builder(this@SpellAssignPickerAct)
                    .setTitle("ยืนยันการส่ง")
                    .setMessage("ต้องการส่ง '${item.title}' ให้กับ ${targetUser?.realName} ใช่หรือไม่?")
                    .setView(container)
                    .setPositiveButton("ยืนยัน") { dialog, _ ->
                        val note = editText.text.toString()
                        sendSpellToUser(item, note)
                        dialog.dismiss()
                    }
                    .setNegativeButton("ยกเลิก") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            
            override fun onViewClick(item: SpellItem) {
                 val content = item.content ?: ""
                 val title = item.title ?: ""
                 val note = item.note ?: ""
                 val miraDiaryF = com.numberniceic.ui.apersonnews.MiraDiaryF.newInstance(content, title, note)
                 miraDiaryF.show(supportFragmentManager, "MiraDiaryF")
            }
        })
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun sendSpellToUser(item: SpellItem, note: String = "") {
        if (targetUser == null) return

        progressBar.visibility = View.VISIBLE

        val json = JsonObject()
        json.addProperty("memberid", targetUser!!.memberId)
        json.addProperty("spell_id", item.id)
        json.addProperty("note", note)

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        
        apiService.assignSpell(json).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@SpellAssignPickerAct, "ส่งข้อมูลเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@SpellAssignPickerAct, "เกิดข้อผิดพลาด: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@SpellAssignPickerAct, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
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
