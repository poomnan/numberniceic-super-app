package com.numberniceic.ui.vip

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.numberniceic.R
import com.numberniceic.data.member.MemberVipCollectionDao
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.AppActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VipCodeBottomSheet : BottomSheetDialogFragment() {

    private lateinit var edtVipCode: EditText
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vip_code_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edtVipCode = view.findViewById(R.id.edt_vip_code)
        btnSubmit = view.findViewById(R.id.btn_submit_vip_code)
        progressBar = view.findViewById(R.id.progress_vipcode)

        progressBar.isVisible = false

        btnSubmit.setOnClickListener {
            val code = edtVipCode.text.trim().toString()

            if (code.length >= 5) {
                progressBar.isVisible = true
                btnSubmit.isEnabled = false
                callApiCode(code)
            } else {
                Toast.makeText(context, "กรุณากรอกรหัส VIP ที่ถูกต้อง (อย่างน้อย 5 ตัวอักษร)", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun callApiCode(apicode: String) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.checkVipCodeApi(apicode).enqueue(object : Callback<MemberVipCollectionDao> {
            override fun onResponse(
                call: Call<MemberVipCollectionDao>,
                response: Response<MemberVipCollectionDao>
            ) {
                if (!isAdded) return  // Fragment detached check

                progressBar.isVisible = false
                btnSubmit.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    val pref = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
                    val editor = pref.edit()
                    editor.putString("member", data.member)

                    if (data.member != "fail") {
                        editor.putString("vipcode", data.vipcode!!.vipcode)
                        editor.putString("viptype", data.vipcode.viptype)
                        editor.putString("vipstatus", data.vipcode.vipstatus)
                        editor.apply()
                        
                        Toast.makeText(
                            context,
                            "🎉 ยินดีด้วย! ตอนนี้สถานะของท่านเป็น VIP ระดับ ${data.vipcode.viptype} เรียบร้อยแล้ว",
                            Toast.LENGTH_LONG
                        ).show()

                        // Update UI and dismiss
                        (activity as? AppActivity)?.updateUserUI()
                        dismissAllowingStateLoss()
                    } else {
                        Toast.makeText(
                            context,
                            "รหัส VIP นี้ไม่ถูกต้องหรือถูกใช้งานแล้ว กรุณาตรวจสอบและลองใหม่อีกครั้ง",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<MemberVipCollectionDao>, t: Throwable) {
                if (!isAdded) return  // Fragment detached check

                progressBar.isVisible = false
                btnSubmit.isEnabled = true

                Log.e("VipCodeBottomSheet", "API call failed: ${t.message}", t)
                Toast.makeText(
                    context,
                    "โปรดตรวจสอบอินเตอร์เน็ต และลองใหม่อีกครั้ง",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    companion object {
        const val TAG = "VipCodeBottomSheet"
    }    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { sheet ->
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
            val displayMetrics = resources.displayMetrics
            val totalHeight = displayMetrics.heightPixels
            val targetHeight = (totalHeight * 0.85).toInt()

            sheet.layoutParams.height = targetHeight
            behavior.isFitToContents = false
            behavior.expandedOffset = totalHeight - targetHeight
            behavior.peekHeight = targetHeight
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
    }
}
