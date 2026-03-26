package com.numberniceic.ui.namenick

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.gson.JsonObject

import com.numberniceic.R
import com.numberniceic.adapters.DayAdapter
import com.numberniceic.data.admin.ServerMessage
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao
import com.numberniceic.data.nickname.Day
import com.numberniceic.databinding.FragmentNameNickBinding
import com.numberniceic.utils.AppContextManager

import com.numberniceic.utils.SnackContextManager
import com.numberniceic.utils.UserContextManager
import android.widget.ProgressBar

class NameNickF : Fragment(), View.OnClickListener, AdapterView.OnItemSelectedListener {
    private var sharedRef: SharedPreferences? = null
    private var sharedRefUserdata: SharedPreferences? = null

    private var dao: NickNameCollectionDao? = null
    private lateinit var imm: InputMethodManager
    private lateinit var binding: FragmentNameNickBinding

    private var fragmentView: View? = null
    private var day = 0
    private val dayList: ArrayList<Day> = arrayListOf()

    private lateinit var nickNameViewModel: NameNickViewModel


    companion object {
        fun newInstance(dayPosition: Int, nickname: String): NameNickF {
            val args = Bundle()
            args.putString("nickname", nickname)
            args.putInt("dayPosition", dayPosition)
            val fragment = NameNickF()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        Log.d("LIFECYCLE", "TAB NICKF CREATE")

        this.sharedRef = context!!.getSharedPreferences("nicknamedata", Context.MODE_PRIVATE)
        this.sharedRefUserdata = context!!.getSharedPreferences("userdata", Context.MODE_PRIVATE)


        nickNameViewModel = ViewModelProvider(this)[NameNickViewModel::class.java]

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_name_nick, container, false)
        binding.apply {
            setLifecycleOwner(this@NameNickF)
            binding.nickNameObs = nickNameViewModel.nickNameObs
        }

        initInstances()

        fragmentView = binding.root

        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressbar_nickname = view.findViewById<ProgressBar>(R.id.progressbar_nickname)
        if (progressbar_nickname != null) progressbar_nickname.isVisible = false

        val manager = childFragmentManager

        val ft = manager.beginTransaction()
        ft.replace(R.id.fragment_ad_container_nickname, NamenickAdF(), "NamenickAdF")
        ft.commit()

        manager.executePendingTransactions()

        if (!binding.edtName.text.isNullOrEmpty() && this.day != 0) {
            callAPI()

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

        val spinnterAdapter = DayAdapter(context!!, dayList)

        binding.daysSpinner.adapter = spinnterAdapter
        binding.daysSpinner.onItemSelectedListener = this
        binding.btnNickNameCal.setOnClickListener(this)
        binding.btnNickNameMiracle.setOnClickListener(this)
        binding.btnNickNameClear.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnNickNameCal -> setNickNameAPI(v)
            R.id.btnNickNameMiracle -> launchingNickNameAct()
            R.id.btnNickNameClear -> clearEDT()
        }
    }

    private fun clearEDT() {
        this.dao = null
        // Also possibly clear viewModel if it has state.
        // But the previous implementation only reset UI elements.

        binding.daysSpinner.setSelection(0)
        binding.edtName.text!!.clear()
        binding.edtName.requestFocus()
        
        val manager = childFragmentManager

        try {
            val f = manager.findFragmentByTag("NamenickAdF") as NamenickAdF?
            if (f != null) {
                manager.beginTransaction().remove(f).commit()
            }
        } catch (e: Exception) {
            Log.e("NameNickF", "Error removing fragment", e)
        }

        val ft = manager.beginTransaction()
        ft.replace(R.id.fragment_ad_container_nickname, NamenickAdF(), "NamenickAdF")
        ft.commit()

        manager.executePendingTransactions()
    }

    private fun launchingNickNameAct() {
        if (this.dao != null) {
            val intent = Intent(context, NameNickMiraAct::class.java)
            intent.putExtra("DAO", this.dao)
            startActivity(intent)
        }
    }

    private fun setNickNameAPI(v: View) {
        if (this.day == 0 && binding.edtName.text?.isNotEmpty() == true) {
            binding.daysSpinner.performClick()
            Toast.makeText(context, "กรุณาเลือกวันเกิดเพื่อวิเคราะห์ชื่อ", Toast.LENGTH_SHORT).show()
            return
        }

        if (checkEDT()) {
            checkSuccessful()
        }

        hideKeyboard()
    }


    private fun callAPI() {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getNicknameDetail(binding.edtName.text.toString(), dayList[this.day].nameEng).enqueue(object : Callback<NickNameCollectionDao> {
            override fun onResponse(call: Call<NickNameCollectionDao>, response: Response<NickNameCollectionDao>) {
                if (response.isSuccessful && response.body() != null) {
                    val dao = response.body()!!
                    val f = childFragmentManager.findFragmentByTag("NamenickAdF") as NamenickAdF?
                    if (f != null) childFragmentManager.beginTransaction().remove(f).commit()

                    val manager = childFragmentManager
                    val ft = manager.beginTransaction()
                    ft.replace(R.id.fragment_ad_container_nickname_re, NamenickAdF.newInstance(dao), "NamenickAdF")
                    ft.commit()
                    manager.executePendingTransactions()

                    if (view?.findViewById<ProgressBar>(R.id.progressbar_nickname) != null) view?.findViewById<ProgressBar>(R.id.progressbar_nickname)!!.isVisible = false
                    this@NameNickF.nickNameViewModel.addNickNameDao(dao)
                    this@NameNickF.dao = dao

                    val userx = UserContextManager.userX(this@NameNickF.context!!)
                    if (userx != null) {
                        if (userx.vipcode != "admin" && userx.vipcode != "administrator" && userx.vipcode != "admin7") {
                            this@NameNickF.contributionNickName(dao)
                        }
                    } else {
                        this@NameNickF.contributionNickName(dao)
                    }
                } else {
                    if (view?.findViewById<ProgressBar>(R.id.progressbar_nickname) != null) view?.findViewById<ProgressBar>(R.id.progressbar_nickname)!!.isVisible = false
                    SnackContextManager.setSnack("อาจมีปัญหาเกี่ยวกับเครื่อข่าย โปรดตรวจสอบ!!", binding.coordinatorNickname)
                }
            }

            override fun onFailure(call: Call<NickNameCollectionDao>, t: Throwable) {
                if (view?.findViewById<ProgressBar>(R.id.progressbar_nickname) != null) view?.findViewById<ProgressBar>(R.id.progressbar_nickname)!!.isVisible = false
                SnackContextManager.setSnack("อาจมีปัญหาเกี่ยวกับเครื่อข่าย โปรดตรวจสอบ!!", binding.coordinatorNickname)
                Log.d("ERRORANANYA", "error" + t.message)
            }
        })
    }

    private fun contributionNickName(dao: NickNameCollectionDao) {


        var pairTypeSumSat = false
        var pairTypeSumSha = false
        if (dao.pairsMiracle != null) {
            for (pair in dao.pairsMiracle) {
                if (pair.pairnumber == dao.sumSatNickName.toString()) {
                    when (pair.pairtype!![0].toString()) {
                        "D" -> pairTypeSumSat = true
                        "R" -> pairTypeSumSat = false
                    }
                }

                if (pair.pairnumber == dao.sumShaNickName.toString()) {
                    when (pair.pairtype!![0].toString()) {
                        "D" -> pairTypeSumSha = true
                        "R" -> pairTypeSumSha = false
                    }
                }
            }
        }


        if (pairTypeSumSat && pairTypeSumSha) {
            val nickname = JsonObject()
            val sat = StringBuilder()

            for (cn in dao.satNickName!!) {
                sat.append(cn.xNum)
            }

            nickname.addProperty("thainame", dao.nickname)
            nickname.addProperty("reangthai", sat.toString())

            nickname.addProperty("leksat_thai", dao.sumSatNickName)
            nickname.addProperty("shadow", dao.sumShaNickName)

            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            apiService.addNickname(nickname).enqueue(object : Callback<ServerMessage> {
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


    private fun checkEDT(): Boolean {
        if (this.day == 0) {
            SnackContextManager.setSnack("โปรดเลือกวันเกิด!!", binding.coordinatorNickname)
            if (view?.findViewById<ProgressBar>(R.id.progressbar_nickname) != null) view?.findViewById<ProgressBar>(R.id.progressbar_nickname)!!.isVisible = false
            return false
        }

        if (binding.edtName.text!!.isEmpty()) {
            SnackContextManager.setSnack("โปรดใส่ชื่อเล่น!!", binding.coordinatorNickname)
            if (view?.findViewById<ProgressBar>(R.id.progressbar_nickname) != null) view?.findViewById<ProgressBar>(R.id.progressbar_nickname)!!.isVisible = false
            return false
        }
        return true
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        this.day = position
        // If a valid day is selected (position > 0) and nickname is not empty, analyze automatically
        if (position > 0 && binding.edtName.text?.isNotEmpty() == true) {
             // Reset the flag if it was set
            if (checkEDT()) {
                checkSuccessful()
                hideKeyboard()
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    private fun checkSuccessful() {
        AppContextManager.countNicknameCal(this.sharedRef)

        if (AppContextManager.checkPermisNickname(this.sharedRef, this.sharedRefUserdata)) {

            if (view?.findViewById<ProgressBar>(R.id.progressbar_nickname) != null) view?.findViewById<ProgressBar>(R.id.progressbar_nickname)!!.isVisible = true

            callAPI()

        } else {
            Toast.makeText(context, getString(R.string.request_vip), Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.edtName.windowToken, 0)
        } catch (e: Exception) {
            Log.e("NameNickF", "Error hiding keyboard", e)
        }
    }
}
