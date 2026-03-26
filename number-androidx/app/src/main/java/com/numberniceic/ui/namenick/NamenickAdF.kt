package com.numberniceic.ui.namenick


import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.data.admin.ServerMessage
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao
import com.numberniceic.utils.AppContextManager
import android.widget.Button
import android.widget.ProgressBar


class NamenickAdF : Fragment() {

    companion object {
        fun newInstance(dao: NickNameCollectionDao): NamenickAdF {
            val args = Bundle()
            args.putParcelable("NickNameCollectionDao", dao)
            val fragment = NamenickAdF()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_namenick_ad, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTitleBtnListname()


        val process_admin = view.findViewById<ProgressBar>(R.id.process_admin)
        val btn_select_namelist = view.findViewById<Button>(R.id.btn_select_namelist)
        val btn_nickname_add_line = view.findViewById<Button>(R.id.btn_nickname_add_line)
        val btn_admin_add_nickname = view.findViewById<Button>(R.id.btn_admin_add_nickname)
        if (process_admin != null) process_admin.isVisible = false


        btn_select_namelist.setOnClickListener {

                val intent = Intent(context, NameNickListAct::class.java)
                intent.putExtra("call_fragmentx", "nickname")
                startActivity(intent)


        }



        btn_nickname_add_line.setOnClickListener {
            val userId = getString(R.string.line_id)
            val sentText = "line://ti/p/~$userId"
            try{
                val intentLine = Intent.parseUri(sentText, Intent.URI_INTENT_SCHEME)
                startActivity(intentLine)
                Toast.makeText(context, "กรุณารอสักครู่...", Toast.LENGTH_LONG).show()
            }catch (e : ActivityNotFoundException){
                Toast.makeText(context, "โปรดลงแอพพลิเคชั่น LINE เพื่อติดต่อกับเรา", Toast.LENGTH_LONG).show()
            }

        }

        btn_admin_add_nickname.isVisible = AppContextManager.checkPermisAdminAddNickname(view.context)

        btn_admin_add_nickname.setOnClickListener {
            if (arguments == null) {
                Toast.makeText(context, "ต้องมีการคำนวณชื่อเสียก่อนบันทึก!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val dao = arguments!!.getParcelable<NickNameCollectionDao>("NickNameCollectionDao")
            if (dao == null) return@setOnClickListener

            // Validation Logic: Check if it is a "Good Name"
            // Both Sat and Shadow must be Good(D5), Very Good(D8), or Excellent(D10)
            // And must NOT be risky numbers
            var isSatGood = false
            var isShaGood = (dao.sumShaNickName == 0)
            
            val riskyNumbers = listOf("26", "62", "23", "32", "40", "04")
            
            if (dao.pairsMiracle != null) {
                for (pair in dao.pairsMiracle!!) {
                    if (pair.pairnumber == dao.sumSatNickName.toString()) {
                        if (!riskyNumbers.contains(pair.pairnumber) && 
                            (pair.pairtype == "D5" || pair.pairtype == "D8" || pair.pairtype == "D10")) {
                            isSatGood = true
                        }
                    }
                    if (pair.pairnumber == dao.sumShaNickName.toString()) {
                         if (!riskyNumbers.contains(pair.pairnumber) && 
                            (pair.pairtype == "D5" || pair.pairtype == "D8" || pair.pairtype == "D10")) {
                            isShaGood = true
                        }
                    }
                }
            }

            if (!isSatGood || !isShaGood) {
                Toast.makeText(context, "ชื่อนี้ไม่ดีไม่มีการบันทึก", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }


            // Toast.makeText(context ?: return@setOnClickListener, "กำลังบันทึกข้อมูลชื่อเล่น...", Toast.LENGTH_SHORT).show()
            if (process_admin != null) process_admin.isVisible = true


            val nickname = JsonObject()
            val sat = StringBuilder()

            if(dao.satNickName != null){ 
                for (cn in dao.satNickName!!) {
                    sat.append(cn.xNum)
                }
            }

            nickname.addProperty("thainame", dao.nickname)
            nickname.addProperty("reangthai", sat.toString())
            nickname.addProperty("leksat_thai", dao.sumSatNickName)
            nickname.addProperty("shadow", dao.sumShaNickName)


            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            apiService.addNickname(nickname).enqueue(object : Callback<ServerMessage> {
                override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                    if (process_admin != null) process_admin.isVisible = false
                    if (response.isSuccessful && response.body() != null) {
                        val servermessage = response.body()!!
                        if (servermessage.activity == "insert" && servermessage.message == "success") {
                            Toast.makeText(context, "บันทึกข้อมูลสมบูรณ์แล้ว!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context ?: return, "ไม่สำเร็จ ${servermessage.message} ${servermessage.debug ?: ""}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context ?: return, "ไม่สามารถบันทึกข้อมูลได้ (Error: ${response.code()})", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                    Log.d("ERRORANANYA", "error " + t.message)
                    if (process_admin != null) process_admin.isVisible = false
                    Toast.makeText(context ?: return, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }






    override fun onResume() {
        super.onResume()

        val btn_admin_add_nickname = view?.findViewById<Button>(R.id.btn_admin_add_nickname)
        if(btn_admin_add_nickname != null) btn_admin_add_nickname.isVisible = AppContextManager.checkPermisAdminAddNickname(requireContext())
        setTitleBtnListname()

    }


    private fun setTitleBtnListname(){
        val btn_select_namelist = view?.findViewById<Button>(R.id.btn_select_namelist)
        if(btn_select_namelist != null) btn_select_namelist.text =  if (AppContextManager.checkPermisListNickname(this.context!!)) "VIP ชื่อเล่นไทย - ENG ตามวันเกิดและอักษรนำ" else "เลือกชื่อเล่นไทย - ENG ตามวันเกิด UPGRADE !"
    }

}
