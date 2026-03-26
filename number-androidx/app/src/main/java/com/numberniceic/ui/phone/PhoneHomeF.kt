package com.numberniceic.ui.phone


import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.widget.EditText
import android.widget.ProgressBar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.PhoneSellNumberCollectionDao
import com.google.android.material.snackbar.Snackbar
import com.numberniceic.utils.AppContextManager
import com.numberniceic.utils.PhoneContextManager
import com.numberniceic.utils.SnackContextManager
import java.text.NumberFormat


class PhoneHomeF : Fragment(), View.OnClickListener {

    private var shared: SharedPreferences? = null
    private var sharedUser: SharedPreferences? = null
    private var load = false
    private var imm: InputMethodManager? = null
    private var phoneNumSell: PhoneSellNumberCollectionDao? = null

    private var edt_phonex_num: EditText? = null
    private var chip_phone_all: View? = null
    private var chip_add_line: View? = null
    private var card_top1: View? = null
    private var tv_phone_top1: TextView? = null
    private var tv_sum_top1: TextView? = null
    private var tv_price_top1: TextView? = null

    private var card_top2: View? = null
    private var tv_phone_top2: TextView? = null
    private var tv_sum_top2: TextView? = null
    private var tv_price_top2: TextView? = null

    private var card_top3: View? = null
    private var tv_phone_top3: TextView? = null
    private var tv_sum_top3: TextView? = null
    private var tv_price_top3: TextView? = null

    private var card_top4: View? = null
    private var tv_phone_top4: TextView? = null
    private var tv_sum_top4: TextView? = null
    private var tv_price_top4: TextView? = null
    private var btn_homemiracle: View? = null
    private var btn_homebuy: View? = null
    private var progressbar_phone_home: ProgressBar? = null
    private var linear_vip_list: View? = null
    private var coordinator_phonehome: CoordinatorLayout? = null
    private var swipe_refresh_phone_home: androidx.swiperefreshlayout.widget.SwipeRefreshLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Log.d("LIFECYCLE", "TAB PHONE CREATE")
        return inflater.inflate(R.layout.fragment_phone_home, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edt_phonex_num = view.findViewById(R.id.edt_phonex_num)
        chip_phone_all = view.findViewById(R.id.chip_phone_all)
        chip_add_line = view.findViewById(R.id.chip_add_line)
        card_top1 = view.findViewById(R.id.card_top1)
        tv_phone_top1 = view.findViewById(R.id.tv_phone_top1)
        tv_sum_top1 = view.findViewById(R.id.tv_sum_top1)
        tv_price_top1 = view.findViewById(R.id.tv_price_top1)

        card_top2 = view.findViewById(R.id.card_top2)
        tv_phone_top2 = view.findViewById(R.id.tv_phone_top2)
        tv_sum_top2 = view.findViewById(R.id.tv_sum_top2)
        tv_price_top2 = view.findViewById(R.id.tv_price_top2)

        card_top3 = view.findViewById(R.id.card_top3)
        tv_phone_top3 = view.findViewById(R.id.tv_phone_top3)
        tv_sum_top3 = view.findViewById(R.id.tv_sum_top3)
        tv_price_top3 = view.findViewById(R.id.tv_price_top3)

        card_top4 = view.findViewById(R.id.card_top4)
        tv_phone_top4 = view.findViewById(R.id.tv_phone_top4)
        tv_sum_top4 = view.findViewById(R.id.tv_sum_top4)
        tv_price_top4 = view.findViewById(R.id.tv_price_top4)
        btn_homemiracle = view.findViewById(R.id.btn_homemiracle)
        btn_homebuy = view.findViewById(R.id.btn_homebuy)
        progressbar_phone_home = view.findViewById(R.id.progressbar_phone_home)
        linear_vip_list = view.findViewById(R.id.linear_vip_list)
        coordinator_phonehome = view.findViewById(R.id.coordinator_phonehome)
        swipe_refresh_phone_home = view.findViewById(R.id.swipe_refresh_phone_home)
        
        // Setup Pull-to-Refresh
        swipe_refresh_phone_home?.setOnRefreshListener {
            refreshPhoneSellData()
        }
        swipe_refresh_phone_home?.setColorSchemeResources(
            android.R.color.holo_green_dark,
            android.R.color.holo_orange_dark,
            android.R.color.holo_blue_dark
        )

        edt_phonex_num?.requestFocus()

        this.shared = view.context.getSharedPreferences("phonedata", Context.MODE_PRIVATE)

        this.sharedUser = view.context.getSharedPreferences("userdata", Context.MODE_PRIVATE)

        this.imm = context!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?

        chip_phone_all?.setOnClickListener(this)
        chip_add_line?.setOnClickListener(this)

        card_top1?.setOnClickListener(this)
        card_top2?.setOnClickListener(this)
        card_top3?.setOnClickListener(this)
        card_top4?.setOnClickListener(this)

        btn_homemiracle?.setOnClickListener(this)
        btn_homebuy?.setOnClickListener(this)

        progressbar_phone_home?.isVisible = true
        linear_vip_list?.isVisible = false

        val ctx = context ?: return
        
        // 1. CACHE FIRST: Load from cache immediately
        val cached = com.numberniceic.utils.PersonNewsCacheManager.loadPhoneSell(ctx)
        if (cached != null && cached.phonenumTop4 != null) {
            Log.d("PhoneHomeF", "Cache Hit! Displaying cached VIP phones.")
            this@PhoneHomeF.load = true
            this@PhoneHomeF.phoneNumSell = cached
            displayPhoneSellData(cached)
            progressbar_phone_home?.isVisible = false
            linear_vip_list?.isVisible = true
        }

        // 2. NETWORK: Fetch fresh data in background
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getPhoneSellList().enqueue(object : Callback<PhoneSellNumberCollectionDao> {
            override fun onResponse(call: Call<PhoneSellNumberCollectionDao>, response: Response<PhoneSellNumberCollectionDao>) {
                progressbar_phone_home?.isVisible = false
                Log.d("DEBUG_VIP", "Network Success: ${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    linear_vip_list?.isVisible = true
                    this@PhoneHomeF.load = true
                    this@PhoneHomeF.phoneNumSell = data
                    
                    displayPhoneSellData(data)
                    
                    // Save to Cache
                    com.numberniceic.utils.PersonNewsCacheManager.savePhoneSell(ctx, data)
                } else {
                    coordinator_phonehome?.let { SnackContextManager.setSnack("โปรดตรวจสอบอินเตอร์เน็ต และลองใหม่อีกครั้ง!!", it) }
                }
            }

            override fun onFailure(call: Call<PhoneSellNumberCollectionDao>, t: Throwable) {
                progressbar_phone_home?.isVisible = false
                // If cache was displayed, user still sees data
                if (cached == null) {
                    coordinator_phonehome?.let { SnackContextManager.setSnack("โปรดตรวจสอบอินเตอร์เน็ต และลองใหม่อีกครั้ง!!", it) }
                }
                Log.d("FROMSERVER", "error" + t.message)
            }
        })

        edt_phonex_num?.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {

                val regexStr = "^[0-9]*$".toRegex()
                val numPosition0 = if (s.toString().isNotEmpty()) s!![0].toString() else "0"

                if (numPosition0 == "0" && s.toString().trim().matches(regexStr) && s.toString().length == 10) {

                    AppContextManager.countPhoneCal(shared)

                    if (AppContextManager.checkPermisPhoneVip(shared, sharedUser)) {

                        val intent = Intent(context, PhoneCalAct::class.java)
                        intent.putExtra("PHONENUMBER", edt_phonex_num?.text.toString())
                        startActivity(intent)

                    } else {
                        Toast.makeText(context, getString(R.string.request_vip), Toast.LENGTH_SHORT).show()
                    }

                    imm!!.hideSoftInputFromWindow(view.windowToken, 0)

                }

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

    }


    override fun onClick(v: View?) {

        if (v == btn_homemiracle) {
            coordinator_phonehome?.let {
                val snack = Snackbar.make(it, "โปรดกรอกหมายเลขโทรศัพท์ก่อน", Snackbar.LENGTH_LONG)
                snack.setAction("OK") { snack.dismiss() }
                val tv = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                tv.setTextColor(Color.YELLOW)
                //tv.maxLines = 5
                snack.show()
            }
        }

        if (v == btn_homebuy) {
            val intent = Intent(context, PhoneBuyAllAct::class.java)
            startActivity(intent)
        }

        if (v == chip_phone_all) {
            val intent = Intent(context, PhoneBuyAllAct::class.java)
            startActivity(intent)
        }



        if (this.load) {

            if (v == card_top1) {
                val intent = Intent(context, PhoneCalAct::class.java)
                intent.putExtra("PHONENUMBER", this.phoneNumSell!!.phonenumTop4!![0].phoneNumber)
                startActivity(intent)
            }
            if (v == card_top2) {
                val intent = Intent(context, PhoneCalAct::class.java)
                intent.putExtra("PHONENUMBER", this.phoneNumSell!!.phonenumTop4!![1].phoneNumber)
                startActivity(intent)
            }
            if (v == card_top3) {
                val intent = Intent(context, PhoneCalAct::class.java)
                intent.putExtra("PHONENUMBER", this.phoneNumSell!!.phonenumTop4!![2].phoneNumber)
                startActivity(intent)
            }
            if (v == card_top4) {
                val intent = Intent(context, PhoneCalAct::class.java)
                intent.putExtra("PHONENUMBER", this.phoneNumSell!!.phonenumTop4!![3].phoneNumber)
                startActivity(intent)

            }



        }




        if (v == chip_add_line) {
            val userId = getString(R.string.line_id)
            // For personal account, use line:// deep link
            val lineUrl = "line://ti/p/~$userId"
            try {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(lineUrl))
                startActivity(intent)
                Toast.makeText(context, "กรุณาค้นหาและกด Add Friend ครับ", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Fallback: Open LINE app
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://line.me/ti/p/~$userId"))
                    startActivity(browserIntent)
                } catch (e2: Exception) {
                    Toast.makeText(context, "โปรดลงแอพพลิเคชั่น LINE เพื่อติดต่อกับเรา", Toast.LENGTH_LONG).show()
                }
            }


        }
    }

    override fun onPause() {
        super.onPause()
        edt_phonex_num?.text!!.clear()
    }

    private fun displayPhoneSellData(data: PhoneSellNumberCollectionDao) {
        // Fallback: if phonenumTop4 is empty, use first 4 from phonenumberSell
        val listToShow = if (!data.phonenumTop4.isNullOrEmpty()) {
            data.phonenumTop4
        } else if (!data.phonenumberSell.isNullOrEmpty()) {
            Log.d("PhoneHomeF", "Using phonenumberSell as fallback for VIP")
            data.phonenumberSell.take(4)
        } else {
            null
        }

        if (listToShow.isNullOrEmpty()) {
            Log.d("PhoneHomeF", "No phone data to display in VIP section")
            linear_vip_list?.isVisible = false
            return
        }

        linear_vip_list?.isVisible = true
        
        // Reset all cards to GONE first
        card_top1?.isVisible = false
        card_top2?.isVisible = false
        card_top3?.isVisible = false
        card_top4?.isVisible = false

        for ((i, item) in listToShow.withIndex()) {
            if (item.phoneNumber.isNullOrEmpty()) continue
            
            val phoneNum = PhoneContextManager.getFormatPhoneNumber(item.phoneNumber)
            val phoneSum = "(${item.phoneSum ?: "0"})"
            val rawPrice = item.phonePrice ?: "0"
            val phonePrice = try {
                 "ราคา ${NumberFormat.getInstance().format(rawPrice.replace(",","").toDouble().toInt())} บาท"
            } catch (e: Exception) {
                 "ราคา $rawPrice บาท"
            }

            when (i) {
                0 -> {
                    card_top1?.isVisible = true
                    tv_phone_top1?.text = phoneNum
                    tv_sum_top1?.text = phoneSum
                    tv_price_top1?.text = phonePrice
                }
                1 -> {
                    card_top2?.isVisible = true
                    tv_phone_top2?.text = phoneNum
                    tv_sum_top2?.text = phoneSum
                    tv_price_top2?.text = phonePrice
                }
                2 -> {
                    card_top3?.isVisible = true
                    tv_phone_top3?.text = phoneNum
                    tv_sum_top3?.text = phoneSum
                    tv_price_top3?.text = phonePrice
                }
                3 -> {
                    card_top4?.isVisible = true
                    tv_phone_top4?.text = phoneNum
                    tv_sum_top4?.text = phoneSum
                    tv_price_top4?.text = phonePrice
                }
            }
        }
    }

    private fun refreshPhoneSellData() {
        val ctx = context ?: return
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getPhoneSellList().enqueue(object : Callback<PhoneSellNumberCollectionDao> {
            override fun onResponse(call: Call<PhoneSellNumberCollectionDao>, response: Response<PhoneSellNumberCollectionDao>) {
                swipe_refresh_phone_home?.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    load = true
                    phoneNumSell = data
                    displayPhoneSellData(data)
                    com.numberniceic.utils.PersonNewsCacheManager.savePhoneSell(ctx, data)
                    Toast.makeText(context, "รีเฟรชข้อมูลสำเร็จ", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PhoneSellNumberCollectionDao>, t: Throwable) {
                swipe_refresh_phone_home?.isRefreshing = false
                Toast.makeText(context, "ไม่สามารถรีเฟรชข้อมูลได้", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
