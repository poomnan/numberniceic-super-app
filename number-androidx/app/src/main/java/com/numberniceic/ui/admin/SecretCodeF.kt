package com.numberniceic.ui.admin


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
import com.google.gson.JsonObject
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import com.numberniceic.R
import com.numberniceic.data.admin.ServerMessage


class SecretCodeF : Fragment(), View.OnClickListener, SecretCodeCf.OnConfListener {


    private var codeType: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_secret_code, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rad_special_p = view.findViewById<RadioButton>(R.id.rad_special_p)
        val rad_silver = view.findViewById<RadioButton>(R.id.rad_silver)
        val rad_gold = view.findViewById<RadioButton>(R.id.rad_gold)
        val rad_diamond = view.findViewById<RadioButton>(R.id.rad_diamond)
        val rad_dream_monthly = view.findViewById<RadioButton>(R.id.rad_dream_monthly)
        val rad_dream_yearly = view.findViewById<RadioButton>(R.id.rad_dream_yearly)
        val rad_dream_lifetime = view.findViewById<RadioButton>(R.id.rad_dream_lifetime)
        val rad_rengyam_vip = view.findViewById<RadioButton>(R.id.rad_rengyam_vip)
        val btn_add_secret_code = view.findViewById<Button>(R.id.btn_add_secret_code)
        val edt_secret_code = view.findViewById<EditText>(R.id.edt_secret_code)

        rad_special_p.setOnClickListener(this)
        rad_silver.setOnClickListener(this)
        rad_gold.setOnClickListener(this)
        rad_diamond.setOnClickListener(this)
        rad_dream_monthly.setOnClickListener(this)
        rad_dream_yearly.setOnClickListener(this)
        rad_dream_lifetime.setOnClickListener(this)
        rad_rengyam_vip.setOnClickListener(this)


        btn_add_secret_code.setOnClickListener {

            val codeName = edt_secret_code.text.toString()

            if (codeType != null && codeName.isNotEmpty()) {
                val cfDialog = SecretCodeCf.newInstance(this.codeType!!, codeName)
                cfDialog.show(childFragmentManager, "SecretCodeCf")

            } else {
                Toast.makeText(context, "โปรดใส่ข้อมูลให้ครบถ้วน!", Toast.LENGTH_LONG).show()
            }
        }
    }


    override fun onClick(v: View?) {
        codeType = when (v?.id) {
            R.id.rad_special_p -> "SpecialP"
            R.id.rad_silver -> "silver"
            R.id.rad_gold -> "gold"
            R.id.rad_diamond -> "diamond"
            R.id.rad_dream_monthly -> "dream_monthly"
            R.id.rad_dream_yearly -> "dream_yearly"
            R.id.rad_dream_lifetime -> "dream_lifetime"
            R.id.rad_rengyam_vip -> "rengyam_yearly"
            else -> null
        }
    }


    override fun onConf(cf: String) {
        Log.d("CF", cf)

        if(cf.isNotEmpty()){
            val codedata = JsonObject()

            codedata.addProperty("codetype", this.codeType)
            codedata.addProperty("codename", cf)

            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            apiService.addSecretCode(codedata).enqueue(object : Callback<ServerMessage> {
                override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                    Log.d("SecretCodeF", "onResponse code: ${response.code()}")
                    if (response.isSuccessful && response.body() != null) {
                        val serverx = response.body()!!
                        Log.d("SecretCodeF", "Server message: ${serverx.message}")
                        if (serverx.message != null) {
                            if (serverx.message == "dup") {
                                Toast.makeText(context, "ไม่มีการบันทึก code name ซ้ำ!!", Toast.LENGTH_LONG).show()
                            }
                            if (serverx.message == "success") {
                                showSuccess(cf)
                            }
                        }
                    } else {
                        // Workaround for potential server response bug (same as registration)
                        val errorBody = response.errorBody()?.string()
                        Log.d("SecretCodeF", "Error body: $errorBody")
                        if (errorBody != null && errorBody.contains("\"message\":\"success\"")) {
                            Log.d("SecretCodeF", "Detected success in error body, treating as SUCCESS")
                            showSuccess(cf)
                        } else {
                            Toast.makeText(context, "เกิดข้อผิดพลาดในการบันทึก (Code: ${response.code()})", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                private fun showSuccess(code: String) {
                    val copyDialog = SecretCopf.newInstance(code)
                    copyDialog.show(childFragmentManager, "SecretCopf")
                }

                override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                    Log.e("SecretCodeF", "onFailure: ${t.message}", t)
                    Toast.makeText(context, t.message, Toast.LENGTH_SHORT).show()
                }
            })


        }
    }

}
