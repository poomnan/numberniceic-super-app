package com.numberniceic.ui.namesur


import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
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
import com.numberniceic.data.apicollectiondao.NameSurnameCollectionDao


import com.numberniceic.ui.namenick.NameNickListAct
import com.numberniceic.utils.AppContextManager


class NamesurAdF : Fragment() {

    private var process_admin_realname: ProgressBar? = null
    private var btn_select_namereallist: Button? = null
    private var btn_surname_add_line: View? = null
    private var btn_admin_add_realname: Button? = null


    companion object {
        fun newInstance(dao: NameSurnameCollectionDao): NamesurAdF {
            val args = Bundle()
            args.putParcelable("NameSurnameCollectionDao", dao)
            val fragment = NamesurAdF()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_namesur_ad, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        process_admin_realname = view.findViewById(R.id.process_admin_realname)
        btn_select_namereallist = view.findViewById(R.id.btn_select_namereallist)
        btn_surname_add_line = view.findViewById(R.id.btn_surname_add_line)
        btn_admin_add_realname = view.findViewById(R.id.btn_admin_add_realname)

        if (process_admin_realname != null) process_admin_realname?.isVisible = false

        setTitleButtonListName()



        btn_select_namereallist?.setOnClickListener {
            val intent = Intent(context, NameNickListAct::class.java)
            intent.putExtra("call_fragmentx", "realname")
            startActivity(intent)
        }



        btn_surname_add_line?.setOnClickListener {
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





        btn_admin_add_realname?.isVisible = AppContextManager.checkPermisAdminAddRealname(requireContext())

        btn_admin_add_realname?.setOnClickListener {
            if (arguments == null) {
                Toast.makeText(context, "ต้องมีการคำนวณชื่อเสียก่อนบันทึก!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val dao = arguments!!.getParcelable<NameSurnameCollectionDao>("NameSurnameCollectionDao")
            if (dao == null) return@setOnClickListener

            // Validation Logic: Check if it is a "Good Name" (Real Name)
            // Both Sat and Shadow must be Good(D5), Very Good(D8), or Excellent(D10)
            // And must NOT be risky numbers
            var isSatGood = false
            var isShaGood = (dao.sumShaName == 0)
            
            val riskyNumbers = listOf("26", "62", "23", "32", "40", "04")
            
            if (dao.pairsMiracle != null) {
                for (pair in dao.pairsMiracle!!) {
                    if (pair.pairnumber == dao.sumSatName.toString()) {
                        if (!riskyNumbers.contains(pair.pairnumber) && 
                            (pair.pairtype == "D5" || pair.pairtype == "D8" || pair.pairtype == "D10")) {
                            isSatGood = true
                        }
                    }
                    if (pair.pairnumber == dao.sumShaName.toString()) {
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

            // Toast.makeText(context ?: return@setOnClickListener, "กำลังบันทึกข้อมูลชื่อจริง...", Toast.LENGTH_SHORT).show()
            if (process_admin_realname != null) process_admin_realname?.isVisible = true

            val realname = JsonObject()
            val sat = StringBuilder()

            if (dao.satName != null){
                for (cn in dao.satName!!) {
                    sat.append(cn.xNum)
                }
            }

            realname.addProperty("thainame", dao.name)
            realname.addProperty("reangthai", sat.toString())
            realname.addProperty("leksat_thai", dao.sumSatName)
            realname.addProperty("shadow", dao.sumShaName)

            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            apiService.addRealname(realname).enqueue(object : Callback<ServerMessage> {
                override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                    if (process_admin_realname != null) process_admin_realname?.isVisible = false
                    if (response.isSuccessful && response.body() != null) {
                        val servermessage = response.body()!!
                        if (servermessage.activity == "insert" && servermessage.message == "success") {
                            Toast.makeText(context, "บันทึกข้อมูลสมบูรณ์แล้ว!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "ไม่สำเร็จ ${servermessage.message} ${servermessage.debug ?: ""}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "ไม่สามารถบันทึกข้อมูลได้ (Error: ${response.code()})", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                    Log.d("ERRORANANYA", "error" + t.message)
                    if (process_admin_realname != null) process_admin_realname?.isVisible = false
                    Toast.makeText(context, "เกิดข้อผิดพลาดในการเชื่อมต่อ (Network Error) : ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }


    }





    override fun onResume() {
        super.onResume()
        setTitleButtonListName()
        if (btn_admin_add_realname != null) btn_admin_add_realname?.isVisible = AppContextManager.checkPermisAdminAddRealname(requireContext())


    }

    private fun setTitleButtonListName() {
        if (btn_select_namereallist != null) btn_select_namereallist?.text = if (AppContextManager.checkPermisListRealname(this.context!!)) "VIP ชื่อไทย - ENG ตามวันเกิดและอักษรนำ" else "เลือกชื่อไทย - ENG ตามวันเกิด UPGRADE !"
    }

}
