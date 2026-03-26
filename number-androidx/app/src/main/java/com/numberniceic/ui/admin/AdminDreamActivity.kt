package com.numberniceic.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.data.admin.AddDreamRequest
import com.numberniceic.data.admin.AddDreamResponse
import com.numberniceic.data.admin.DreamAdminItem
import com.numberniceic.data.admin.UpdateDreamRequest
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDreamActivity : AppCompatActivity() {

    private lateinit var rvDreams: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var btnAdd: View
    private lateinit var etSearch: EditText
    private var allDreams = mutableListOf<DreamAdminItem>()


    private lateinit var adapter: AdminDreamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dream)
        supportActionBar?.hide()

        initView()
        loadDreams()
    }

    private fun initView() {
        rvDreams = findViewById(R.id.rvDreams)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        btnAdd = findViewById(R.id.btnAdd)



        adapter = AdminDreamAdapter(
            onEdit = { showEditDialog(it) },
            onDelete = { showDeleteConfirm(it) }
        )

        rvDreams.layoutManager = LinearLayoutManager(this)
        rvDreams.adapter = adapter

        btnBack.setOnClickListener { finish() }
        btnAdd.setOnClickListener { showAddDialog() }

        etSearch = findViewById(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDreams(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterDreams(query: String) {
        if (query.isEmpty()) {
            adapter.setItems(allDreams)
        } else {
            val filtered = allDreams.filter {
                it.dreamKeyword.contains(query, ignoreCase = true) ||
                it.dreamInterpretation.contains(query, ignoreCase = true)
            }
            adapter.setItems(filtered)
        }
    }

    private fun loadDreams() {
        progressBar.visibility = View.VISIBLE
        val api = RetrofitClient.instance.create(ApiService::class.java)
        api.getAllDreams().enqueue(object : Callback<List<DreamAdminItem>> {
            override fun onResponse(call: Call<List<DreamAdminItem>>, response: Response<List<DreamAdminItem>>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    allDreams.clear()
                    allDreams.addAll(response.body()!!)
                    
                    val currentQuery = etSearch.text.toString()
                    if (currentQuery.isEmpty()) {
                        adapter.setItems(allDreams)
                    } else {
                        filterDreams(currentQuery)
                    }

                } else {
                    Toast.makeText(this@AdminDreamActivity, "โหลดข้อมูลล้มเหลว", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<DreamAdminItem>>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AdminDreamActivity, "เชื่อมต่อล้มเหลว: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddDialog() {
        showDreamDialog(null)
    }

    private fun showEditDialog(item: DreamAdminItem) {
        showDreamDialog(item)
    }

    private fun showDreamDialog(item: DreamAdminItem?) {
        val dialog = AlertDialog.Builder(this).create()
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_add_dream, null)
        
        val txtDialogTitle = dialogView.findViewById<TextView>(R.id.txtDialogTitle)
        val etKeyword = dialogView.findViewById<EditText>(R.id.etKeyword)
        val etMeaning = dialogView.findViewById<EditText>(R.id.etMeaning)
        val etLuckyNumbers = dialogView.findViewById<EditText>(R.id.etLuckyNumbers)
        val spnCategory = dialogView.findViewById<Spinner>(R.id.spnCategory)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val dialogProgress = dialogView.findViewById<ProgressBar>(R.id.progressBar)

        val categories = arrayOf("ความฝัน", "ปรึกษา")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnCategory.adapter = categoryAdapter

        if (item != null) {
            txtDialogTitle.text = "แก้ไขข้อมูลฝัน"
            etKeyword.setText(item.dreamKeyword)
            etMeaning.setText(item.dreamInterpretation)
            etLuckyNumbers.setText(item.luckyNumbers)
            btnSave.text = "อัปเดตข้อมูล"
            
            val pos = if (item.category == "ปรึกษา") 1 else 0
            spnCategory.setSelection(pos)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val keyword = etKeyword.text.toString().trim()
            val meaning = etMeaning.text.toString().trim()
            val luckyNumbers = etLuckyNumbers.text.toString().trim()
            val category = spnCategory.selectedItem.toString()

            if (keyword.isEmpty() || meaning.isEmpty()) {
                val msg = if (keyword.isEmpty()) "กรุณากรอกชื่อฝัน" else "กรุณากรอกคำทำนาย"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialogProgress.visibility = View.VISIBLE
            btnSave.isEnabled = false

            val api = RetrofitClient.instance.create(ApiService::class.java)
            
            val call = if (item == null) {

                val request = AddDreamRequest(keyword, meaning, luckyNumbers, category)
                api.addDream(request)
            } else {
                val request = UpdateDreamRequest(item.dreamId, keyword, meaning, luckyNumbers, category)
                api.updateDream(request)
            }

            call.enqueue(object : Callback<AddDreamResponse> {
                override fun onResponse(call: Call<AddDreamResponse>, response: Response<AddDreamResponse>) {
                    dialogProgress.visibility = View.GONE
                    btnSave.isEnabled = true
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@AdminDreamActivity, "สำเร็จ", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadDreams()
                    } else {
                        val error = response.errorBody()?.string() ?: "เกิดข้อผิดพลาด"
                        Toast.makeText(this@AdminDreamActivity, error, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<AddDreamResponse>, t: Throwable) {
                    dialogProgress.visibility = View.GONE

                    btnSave.isEnabled = true
                    Toast.makeText(this@AdminDreamActivity, "ล้มเหลว: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        dialog.setView(dialogView)
        dialog.show()
    }

    private fun showDeleteConfirm(item: DreamAdminItem) {
        AlertDialog.Builder(this)
            .setTitle("ลบข้อมูล")
            .setMessage("คุณต้องการลบข้อมูลฝัน '${item.dreamKeyword}' ใช่หรือไม่?")
            .setPositiveButton("ลบ") { _, _ ->
                deleteDream(item.dreamId)
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun deleteDream(id: Int) {
        progressBar.visibility = View.VISIBLE
        val api = RetrofitClient.instance.create(ApiService::class.java)
        val json = JsonObject()
        json.addProperty("dream_id", id)

        api.deleteDream(json).enqueue(object : Callback<AddDreamResponse> {
            override fun onResponse(call: Call<AddDreamResponse>, response: Response<AddDreamResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@AdminDreamActivity, "ลบสำเร็จ", Toast.LENGTH_SHORT).show()
                    loadDreams()
                } else {
                    Toast.makeText(this@AdminDreamActivity, "ลบไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AddDreamResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@AdminDreamActivity, "เชื่อมต่อล้มเหลว: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
