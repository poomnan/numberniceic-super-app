package com.numberniceic.ui.namesur


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View

import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Toast

import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject

import com.numberniceic.R
import com.numberniceic.adapters.DayAdapter
import com.numberniceic.data.admin.ServerMessage
import com.numberniceic.data.apicollectiondao.NameSurnameCollectionDao


import com.numberniceic.data.nickname.Day

import com.numberniceic.databinding.FragmentNameSurBinding
import com.numberniceic.utils.AppContextManager

import com.numberniceic.utils.SnackContextManager
import com.numberniceic.utils.UserContextManager

import android.widget.ProgressBar
import androidx.core.content.ContextCompat



class NameSurF : Fragment(), AdapterView.OnItemSelectedListener, View.OnClickListener {
    private var day = 0
    private var shareRef: SharedPreferences? = null
    private var shareRefUser: SharedPreferences? = null
    private lateinit var spinnterAdapter: DayAdapter
    private var fragmentView: View? = null
    private var dao: NameSurnameCollectionDao? = null
    private lateinit var imm: InputMethodManager
    private lateinit var binding: FragmentNameSurBinding
    private val dayList: ArrayList<Day> = arrayListOf()
    private lateinit var nameSurViewModel: NameSurViewModel

    companion object {

        fun newInstance(): NameSurF {
            val args = Bundle()

            val fragment = NameSurF()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: android.view.ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        Log.d("LIFECYCLE", "TAB NAMESUR CREATE")

        nameSurViewModel = ViewModelProvider(this)[NameSurViewModel::class.java]

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_name_sur, container, false)
        binding.apply {
            setLifecycleOwner(this@NameSurF)

            binding.nameSurObs = nameSurViewModel.nameSurObs

        }
        initInstances()

        fragmentView = binding.root

        return fragmentView
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        this.shareRef = view.context.getSharedPreferences("nameserdata", Context.MODE_PRIVATE)
        this.shareRefUser = view.context.getSharedPreferences("userdata", Context.MODE_PRIVATE)


        val progressbar_namesur = view.findViewById<ProgressBar>(R.id.progressbar_namesur)
        if(progressbar_namesur != null) progressbar_namesur.isVisible = false

        val manager = childFragmentManager

        val ft = manager.beginTransaction()
        ft.replace(R.id.fragment_ad_container_namesur, NamesurAdF(), "NamesurAdF")
        ft.commit()

        manager.executePendingTransactions()




        if (!binding.edtName.text.isNullOrEmpty() && !binding.edtSurName.text.isNullOrEmpty() && this.day != 0){

            this.callApi()

        }


    }





    private fun initInstances() {

        dayList.clear()

        dayList.add(Day("--วันเกิด--", "BirthDay", R.drawable.ic_clover))
        dayList.add(Day("วันอาทิตย์", "sunday", R.drawable.d_sun))
        dayList.add(Day("วันจันทร์", "monday", R.drawable.d_moon))
        dayList.add(Day("วันอังคาร", "tuesday", R.drawable.d_mars))
        dayList.add(Day("วันพุธ (กลางวัน)", "wednesday1", R.drawable.d_mercury))
        dayList.add(Day("วันพุธ (กลางคืน)", "wednesday2", R.drawable.d_mercury))
        dayList.add(Day("วันพฤหัสบดี", "thursday", R.drawable.d_jupiter))
        dayList.add(Day("วันศุกร์", "friday", R.drawable.d_asteroid))
        dayList.add(Day("วันเสาร์", "saturday", R.drawable.d_saturn))


        spinnterAdapter = DayAdapter(context!!, dayList)

        binding.daysSpinner.adapter = spinnterAdapter
        binding.daysSpinner.onItemSelectedListener = this
        binding.btnNameSurCal.setOnClickListener(this)
        binding.btnNameSurMiracle.setOnClickListener(this)
        binding.btnNameSurClear.setOnClickListener(this)
    }

    override fun onClick(v: View?) {

        if (v == binding.btnNameSurClear) {
            clearUI()
        }

        if (v == binding.btnNameSurMiracle) {
            if (this.dao != null) {
                val intent = Intent(context, NameSurMiraAct::class.java)
                intent.putExtra("DAO", this.dao)
                startActivity(intent)
            }
        }


        if (v == binding.btnNameSurCal) {
            setNameSurAPI(v!!)
        }
    }

    private fun setNameSurAPI(v: View) {
        if (this.day == 0 && (binding.edtName.text?.isNotEmpty() == true || binding.edtSurName.text?.isNotEmpty() == true)) {
            binding.daysSpinner.performClick()
            Toast.makeText(context, "กรุณาเลือกวันเกิดเพื่อวิเคราะห์ชื่อ", Toast.LENGTH_SHORT).show()
            return
        }

        if (checkEDT()) {
            checkSuccessful()
        }

        hideKeyboard()
    }

    private fun checkSuccessful() {
        AppContextManager.countSurnameCal(this.shareRef)

        if (AppContextManager.checkPermisSurname(this.shareRef, this.shareRefUser)){
            callApi()

        }else{
            Toast.makeText(context, getString(R.string.request_vip), Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkEDT(): Boolean {
        if (this.day == 0) {
            SnackContextManager.setSnack("โปรดเลือกวันเกิด", binding.coordinatorNamesur)
            return false
        }

        if (binding.edtName.text!!.isEmpty() || binding.edtSurName.text!!.isEmpty()) {
            //SnackContextManager.setSnack("โปรดใส่ชื่อสกุล!!", binding.coordinatorNamesur)

            return false
        }

        return true
    }

    private fun clearUI() {
        this.dao = null
        binding.daysSpinner.setSelection(0)
        binding.edtName.text!!.clear()
        binding.edtSurName.text!!.clear()
        binding.edtName.requestFocus()
        
        val manager = childFragmentManager

        try {
            val f = manager.findFragmentByTag("NamesurAdF") as NamesurAdF?
            if (f != null) {
                manager.beginTransaction().remove(f).commit()
            }
        } catch (e: Exception) {
            Log.e("NameSurF", "Error removing fragment", e)
        }

        val ft = manager.beginTransaction()
        ft.replace(R.id.fragment_ad_container_namesur, NamesurAdF(), "NamesurAdF")
        ft.commit()

        manager.executePendingTransactions()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        this.day = position
        if (position > 0 && (binding.edtName.text?.isNotEmpty() == true || binding.edtSurName.text?.isNotEmpty() == true)) {
            if (checkEDT()) {
                checkSuccessful()
                hideKeyboard()
            }
        }
    }

    private fun callApi() {
        val progressbar_namesur = view?.findViewById<ProgressBar>(R.id.progressbar_namesur)
        if(progressbar_namesur != null) progressbar_namesur.isVisible = true

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getNameSurnameDetail(binding.edtName.text.toString(), binding.edtSurName.text.toString(), dayList[this.day].nameEng).enqueue(object : Callback<NameSurnameCollectionDao> {
            override fun onResponse(call: Call<NameSurnameCollectionDao>, response: Response<NameSurnameCollectionDao>) {
                if (response.isSuccessful && response.body() != null) {
                    val dao = response.body()!!
                    val f = childFragmentManager.findFragmentByTag("NamesurAdF") as NamesurAdF?
                    if (f != null) childFragmentManager.beginTransaction().remove(f).commit()

                    val manager = childFragmentManager
                    val ft = manager.beginTransaction()
                    ft.replace(R.id.fragment_ad_container_namesur_re, NamesurAdF.newInstance(dao), "NamesurAdF")
                    ft.commit()
                    manager.executePendingTransactions()

                    if (view?.findViewById<ProgressBar>(R.id.progressbar_namesur) != null) view?.findViewById<ProgressBar>(R.id.progressbar_namesur)!!.isVisible = false
                    this@NameSurF.dao = dao
                    this@NameSurF.nameSurViewModel.addNameSurObs(dao)

                    val userx = UserContextManager.userX(this@NameSurF.context!!)
                    if (userx != null) {
                        if (userx.vipcode != "admin" && userx.vipcode != "administrator" && userx.vipcode != "admin7") {
                            this@NameSurF.contributioRealname(dao)
                        }
                    } else {
                        this@NameSurF.contributioRealname(dao)
                    }
                } else {
                    if (view?.findViewById<ProgressBar>(R.id.progressbar_namesur) != null) view?.findViewById<ProgressBar>(R.id.progressbar_namesur)!!.isVisible = false
                    SnackContextManager.setSnack("อาจมีปัญหาเกี่ยวกับเครื่อข่าย โปรดตรวจสอบ!!", binding.coordinatorNamesur)
                }
            }

            override fun onFailure(call: Call<NameSurnameCollectionDao>, t: Throwable) {
                if (view?.findViewById<ProgressBar>(R.id.progressbar_namesur) != null) view?.findViewById<ProgressBar>(R.id.progressbar_namesur)!!.isVisible = false
                SnackContextManager.setSnack("อาจมีปัญหาเกี่ยวกับเครื่อข่าย โปรดตรวจสอบ!!", binding.coordinatorNamesur)
                Log.d("ERRORANANYA", "error" + t.message)
            }
        })
    }


    private fun contributioRealname(dao: NameSurnameCollectionDao) {

        var pairTypeSumSat = false
        var pairTypeSumSha = false
        if (dao.pairsMiracle != null){
            for (pair in dao.pairsMiracle){
                if (pair.pairnumber == dao.sumSatName.toString()){
                    when(pair.pairtype!![0].toString()){
                        "D" -> pairTypeSumSat = true
                        "R" -> pairTypeSumSat = false
                    }
                }

                if (pair.pairnumber == dao.sumShaName.toString()){
                    when(pair.pairtype!![0].toString()){
                        "D" -> pairTypeSumSha = true
                        "R" -> pairTypeSumSha = false
                    }
                }
            }
        }


        if (pairTypeSumSat && pairTypeSumSha){
            val nickname = JsonObject()
            val sat = StringBuilder()

            for (cn in dao.satName!!) {
                sat.append(cn.xNum)
            }

            nickname.addProperty("thainame", dao.name)
            nickname.addProperty("reangthai", sat.toString())

            nickname.addProperty("leksat_thai", dao.sumSatName)
            nickname.addProperty("shadow", dao.sumShaName)

            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            apiService.addRealname(nickname).enqueue(object : Callback<ServerMessage> {
                override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                    if (response.isSuccessful && response.body() != null) {
                        val servermessage = response.body()!!
                        if (servermessage.activity == "insert" && servermessage.message == "success") {
                            Log.d("MESSAGE_CONTRIBUTION", "บันทึกข้อมูลสมบูรณ์แล้ว!")
                        } else {
                            Log.d("MESSAGE_CONTRIBUTION", "ไม่สำเร็จ ${servermessage.message}")
                        }
                    }
                }

                override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                    Log.d("ERRORANANYA", "error" + t.message)
                }
            })
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.edtName.windowToken, 0)
        } catch (e: Exception) {
            Log.e("NameSurF", "Error hiding keyboard", e)
        }
    }
}
