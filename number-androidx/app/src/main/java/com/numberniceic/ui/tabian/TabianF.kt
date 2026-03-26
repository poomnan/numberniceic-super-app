package com.numberniceic.ui.tabian


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.material.snackbar.Snackbar

import com.numberniceic.R
import com.numberniceic.adapters.TabianPairAdapter
import com.numberniceic.data.apicollectiondao.TabianCollectionDao
import com.numberniceic.databinding.FragmentTabianBinding

import com.numberniceic.ui.phone.PhoneF
import com.numberniceic.ui.phone.PhoneMiraActivity
import com.numberniceic.utils.SnackContextManager
import com.numberniceic.utils.TabianContextManager
import android.widget.ProgressBar
import android.widget.EditText


class TabianF : Fragment(), View.OnClickListener {

    private var imm: InputMethodManager? = null
    private lateinit var binding: FragmentTabianBinding

    private var fragmentView: View? = null

    private lateinit var tabainViewModel: TabianViewModel
    private var daoTabian: TabianCollectionDao? = null



    companion object {
        fun newInstance(tabainNumber: String): TabianF {
            val args = Bundle()
            args.putString("tabainNumber", tabainNumber)
            val fragment = TabianF()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {


        this.imm = context!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?

        tabainViewModel = ViewModelProvider(this)[TabianViewModel::class.java]
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tabian, container, false)
        binding.apply {
            setLifecycleOwner(this@TabianF)
            binding.tabianObs = tabainViewModel.tabianObs
        }

        fragmentView = binding.root

        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        val edtTabainNum = view.findViewById<EditText>(R.id.edtTabainNum)
        val txtTabainNum = view.findViewById<TextView>(R.id.txtTabainNum)
        val progressbar_tabian = view.findViewById<ProgressBar>(R.id.progressbar_tabian)

        edtTabainNum.text = Editable.Factory.getInstance().newEditable(arguments!!.getString("tabainNumber"))
        txtTabainNum.text = arguments!!.getString("tabainNumber")
        setTabianApi()


        progressbar_tabian.isVisible = false

        if (savedInstanceState != null){
            if (savedInstanceState.getParcelable<TabianCollectionDao>("DAO") != null){
                this.daoTabian = savedInstanceState.getParcelable("DAO")


            }

        }

        if (this.daoTabian != null) {
            if (this.daoTabian!!.carPairsA!!.isNotEmpty())
                setRecyclePairCircleAB()
        }

        binding.btnTabianCal.setOnClickListener(this)
        binding.btnTabianMiracle.setOnClickListener(this)
        binding.btnTabianClear.setOnClickListener(this)

        view.findViewById<View>(R.id.btnTabianDuuTabianAll).setOnClickListener {
            val intentTabian = Intent(context, TabianBuyAllAct::class.java)
            startActivity(intentTabian)
        }
    }


    override fun onClick(v: View?) {

        if (v == binding.btnTabianClear){
            clearEDT()
        }

        if (v == binding.btnTabianCal) {
            setTabianApi()



        }

        if(v == binding.btnTabianMiracle){
            if(this.daoTabian != null){
                val intent = Intent(context, TabianMiraActivity::class.java)
                intent.putExtra("DAO", this.daoTabian)
                val coordinator_tabian = binding.coordinatorTabian
                startActivity(intent)
                Snackbar.make(coordinator_tabian, "ทะเบียน Miracle!!", Snackbar.LENGTH_SHORT).show()
            }else{

                SnackContextManager.setSnack("โปรดคลิกปุ่มถอดรหัสก่อน!!", binding.coordinatorTabian)
            }

        }


        imm!!.hideSoftInputFromWindow(v!!.windowToken, 0)
    }

    private fun clearEDT() {
        binding.edtTabainNum.text.clear()
        binding.edtTabainNum.requestFocus()

        Snackbar.make(binding.coordinatorTabian, "ล้างเพื่อพิมพ์ใหม่!!", Snackbar.LENGTH_SHORT).show()
    }

    private fun setTabianApi() {
        if (TabianContextManager.checkTabianNumber(binding.edtTabainNum.text.toString())) {
            val progressbar_tabian = view?.findViewById<ProgressBar>(R.id.progressbar_tabian)
            if (progressbar_tabian != null) progressbar_tabian.isVisible = true
            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            apiService.getTabianDetail(binding.edtTabainNum.text.toString()).enqueue(object : Callback<TabianCollectionDao> {
                override fun onResponse(call: Call<TabianCollectionDao>, response: Response<TabianCollectionDao>) {
                    if (progressbar_tabian != null) progressbar_tabian.isVisible = false
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        this@TabianF.daoTabian = data
                        tabainViewModel.addTabianRepo(data)
                        setRecyclePairCircleAB()
                    } else {
                        SnackContextManager.setSnack("โปรดตรวจสอบ และลองใหม่อีกครั้ง!!", binding.coordinatorTabian)
                    }
                }

                override fun onFailure(call: Call<TabianCollectionDao>, t: Throwable) {
                    if (progressbar_tabian != null) progressbar_tabian.isVisible = false
                    SnackContextManager.setSnack("โปรดตรวจสอบ และลองใหม่อีกครั้ง!!", binding.coordinatorTabian)
                    Log.d("FROMSERVER", "error" + t.message)
                }
            })

        } else {
            val snack = Snackbar.make(binding.coordinatorTabian, "กรอกข้อมูลไม่ถูกต้อง!!", Snackbar.LENGTH_SHORT)
            val tv = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            tv.setTextColor(Color.YELLOW)
            snack.show()
        }



    }

    private fun setRecyclePairCircleAB() {
        binding.recyclePairsTabian.adapter = TabianPairAdapter(tabainViewModel.getCarPairA(), tabainViewModel.getPairMiracle()!!)
        binding.recyclePairsTabian.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        binding.recyclePairsTabianFang.adapter = TabianPairAdapter(tabainViewModel.getCarPairB(), tabainViewModel.getPairMiracle()!!)
        binding.recyclePairsTabianFang.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

    }

    override fun onSaveInstanceState(outState: Bundle) {

        if (daoTabian != null){
            outState.run {
                outState.putParcelable("DAO", daoTabian)
            }
        }

        super.onSaveInstanceState(outState)

    }


}
