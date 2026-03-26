package com.numberniceic.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.data.admin.ServerMessage
import java.util.*
import java.text.SimpleDateFormat

class LuckyNumberF : Fragment() {

    private lateinit var edtLuckyDate: TextInputEditText
    private lateinit var num1: TextInputEditText
    private lateinit var num2: TextInputEditText
    private lateinit var num3: TextInputEditText
    private lateinit var num4: TextInputEditText
    private lateinit var num5: TextInputEditText
    private lateinit var num6: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var txtCurrentDate: android.widget.TextView
    private lateinit var txtCurrentNumbers: android.widget.TextView

    private val calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        fun newInstance(): LuckyNumberF {
            return LuckyNumberF()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_lucky_number, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupDatePicker()
        setupSaveButton()
        
        // Default to today
        edtLuckyDate.setText(sdf.format(calendar.time))

        // Fetch current numbers
        fetchCurrentLuckyNumbers()
    }

    private fun initViews(view: View) {
        edtLuckyDate = view.findViewById(R.id.edt_lucky_date)
        num1 = view.findViewById(R.id.lucky_num1)
        num2 = view.findViewById(R.id.lucky_num2)
        num3 = view.findViewById(R.id.lucky_num3)
        num4 = view.findViewById(R.id.lucky_num4)
        num5 = view.findViewById(R.id.lucky_num5)
        num6 = view.findViewById(R.id.lucky_num6)
        btnSave = view.findViewById(R.id.btn_save_lucky)
        txtCurrentDate = view.findViewById(R.id.txt_current_date)
        txtCurrentNumbers = view.findViewById(R.id.txt_current_numbers)
    }

    private fun fetchCurrentLuckyNumbers() {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getLuckyNumber().enqueue(object : Callback<com.numberniceic.data.rengyam.LuckyNumber> {
            override fun onResponse(call: Call<com.numberniceic.data.rengyam.LuckyNumber>, response: Response<com.numberniceic.data.rengyam.LuckyNumber>) {
                if (response.isSuccessful && response.body() != null) {
                    val lucky = response.body()!!
                    if (!lucky.luckyDate.isNullOrEmpty()) {
                        txtCurrentDate.text = "วันที่ล่าสุด: ${lucky.luckyDate}"
                        txtCurrentNumbers.text = "เลขชุดปัจจุบัน: ${lucky.number}"
                        
                        // Split numbers and populate fields as suggestion
                        lucky.number?.split(" ")?.let { nums ->
                            if (nums.size >= 1) num1.setText(nums[0])
                            if (nums.size >= 2) num2.setText(nums[1])
                            if (nums.size >= 3) num3.setText(nums[2])
                            if (nums.size >= 4) num4.setText(nums[3])
                            if (nums.size >= 5) num5.setText(nums[4])
                            if (nums.size >= 6) num6.setText(nums[5])
                        }
                    } else {
                        txtCurrentDate.text = "วันที่ล่าสุด: ยังไม่มีข้อมูล"
                        txtCurrentNumbers.text = "เลขชุดปัจจุบัน: ยังไม่มีข้อมูล"
                    }
                }
            }

            override fun onFailure(call: Call<com.numberniceic.data.rengyam.LuckyNumber>, t: Throwable) {
                Log.e("LuckyNumberF", "Fetch Error: ${t.message}")
            }
        })
    }

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            edtLuckyDate.setText(sdf.format(calendar.time))
        }

        edtLuckyDate.setOnClickListener {
            DatePickerDialog(requireContext(), dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener {
            val date = edtLuckyDate.text.toString()
            if (date.isEmpty()) {
                Toast.makeText(context, "กรุณาเลือกวันที่", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val numStr = "${num1.text}${num2.text}${num3.text}${num4.text}${num5.text}${num6.text}"

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("ยืนยันการบันทึกเลขนำโชค")
                .setMessage("ต้องการบันทึกเลข '$numStr' สำหรับวันที่ $date ใช่หรือไม่?")
                .setPositiveButton("ยืนยัน") { dialog, _ ->
                    saveLuckyNumber(date)
                    dialog.dismiss()
                }
                .setNegativeButton("ยกเลิก") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun saveLuckyNumber(date: String) {
        val json = JsonObject()
        json.addProperty("date", date)
        json.addProperty("num1", num1.text.toString())
        json.addProperty("num2", num2.text.toString())
        json.addProperty("num3", num3.text.toString())
        json.addProperty("num4", num4.text.toString())
        json.addProperty("num5", num5.text.toString())
        json.addProperty("num6", num6.text.toString())

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        btnSave.isEnabled = false
        apiService.addLuckyNumberV2(json).enqueue(object : Callback<ServerMessage> {
            override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                btnSave.isEnabled = true
                if (response.isSuccessful && response.body() != null) {
                    val serverx = response.body()!!
                    if (serverx.message == "success") {
                        Toast.makeText(context, "บันทึกเลขนำโชคเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                        activity?.finish()
                    } else {
                        Toast.makeText(context, "บันทึกผิดพลาด: ${serverx.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "เกิดข้อผิดพลาดในการบันทึก", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                btnSave.isEnabled = true
                Toast.makeText(context, "เกิดข้อผิดพลาด: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("LuckyNumberF", "Error: ${t.message}")
            }
        })
    }
}
