package com.numberniceic.ui.auth


import android.content.Context
import com.numberniceic.R
import android.content.Intent
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.numberniceic.adapters.PhoneSellAdapter
import com.numberniceic.data.apicollectiondao.PhoneSellNumberCollectionDao
import com.numberniceic.data.member.MemberVipCollectionDao
import com.numberniceic.data.member.VipCodeDao
import com.numberniceic.data.phone.PhoneNumberItem
import com.numberniceic.data.phone.PhoneSellHeader
import com.numberniceic.ui.namenick.NameNickListAct
import com.numberniceic.utils.SnackContextManager
import java.text.NumberFormat
import android.widget.Toast


class AuthF :  Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_df, container, false)
    }

    private lateinit var progress_vipcode: ProgressBar
    private lateinit var coordinator_logind: CoordinatorLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progress_vipcode = view.findViewById(R.id.progress_vipcode)
        coordinator_logind = view.findViewById(R.id.coordinator_logind)
        val btn_vip_code = view.findViewById<Button>(R.id.btn_vip_code)
        val edt_vip_code = view.findViewById<EditText>(R.id.edt_vip_code)

        if(progress_vipcode != null) progress_vipcode.isVisible = false

        btn_vip_code.setOnClickListener {

            val apicode = edt_vip_code.text.trim().toString()

            if (apicode.length >= 5){

                if(progress_vipcode != null) progress_vipcode.isVisible = true

            callApiCode(apicode)

            }else{
                Toast.makeText(context, "ข้อมูล VIP CODE ไม่ถูกต้อง!!", Toast.LENGTH_LONG).show()
            }


        }
    }

    private fun callApiCode(apicode: String) {

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.checkVipCodeApi(apicode).enqueue(object : Callback<MemberVipCollectionDao> {
            override fun onResponse(call: Call<MemberVipCollectionDao>, response: Response<MemberVipCollectionDao>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    if (progress_vipcode != null) progress_vipcode.isVisible = false

                    val pref = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
                    val editor = pref.edit()
                    editor.putString("member", data.member)

                    if (data.member != "fail") {
                        editor.putString("vipcode", data.vipcode!!.vipcode)
                        editor.putString("viptype", data.vipcode.viptype)
                        editor.putString("vipstatus", data.vipcode.vipstatus)
                        editor.apply()
                        Toast.makeText(context, "ตอนนี้สถานะของท่านเป็น VIP ระดับ ${data.vipcode.viptype} เรียบร้อยแล้ว", Toast.LENGTH_LONG).show()

                        if (parentFragment is com.google.android.material.bottomsheet.BottomSheetDialogFragment) {
                            (parentFragment as com.google.android.material.bottomsheet.BottomSheetDialogFragment).dismiss()
                            (activity as? com.numberniceic.ui.AppActivity)?.updateUserUI()
                        } else {
                            activity?.finish()
                        }
                    } else {
                        Toast.makeText(context, "Code นี้ไม่สามารถใช้งานได้ กรุณาตรวจสอบและลองดูใหม่ !!!", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<MemberVipCollectionDao>, t: Throwable) {
                SnackContextManager.setSnack("โปรดตรวจสอบอินเตอร์เน็ต และลองใหม่อีกครั้ง!!", coordinator_logind)
                Log.d("FROMSERVER", "error" + t.message)
            }
        })
    }


}
