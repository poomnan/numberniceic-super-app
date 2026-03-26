package com.numberniceic.ui.phone


import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.widget.ProgressBar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.numberniceic.R
import com.numberniceic.adapters.PhoneSellAdapter
import com.numberniceic.data.apicollectiondao.PhoneSellNumberCollectionDao
import com.numberniceic.data.phone.PhoneNumberItem
import com.numberniceic.data.phone.PhoneSellHeader
import com.numberniceic.utils.SnackContextManager
import java.text.NumberFormat


class PhoneBuyAllF : Fragment(), View.OnClickListener {

    private var chip_open_miracle: View? = null
    private var progressbar_phone_buyall: ProgressBar? = null
    private var recycleview_phone_sell: RecyclerView? = null
    private var coor_phonebuy: CoordinatorLayout? = null


    override fun onClick(v: View?) {
        if (v == chip_open_miracle) {
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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_phone_buy_allf, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chip_open_miracle = view.findViewById(R.id.chip_open_miracle)
        progressbar_phone_buyall = view.findViewById(R.id.progressbar_phone_buyall)
        recycleview_phone_sell = view.findViewById(R.id.recycleview_phone_sell)
        coor_phonebuy = view.findViewById(R.id.coor_phonebuy)

        chip_open_miracle?.setOnClickListener(this)

        progressbar_phone_buyall?.isVisible = true




        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getPhoneSellList().enqueue(object : Callback<PhoneSellNumberCollectionDao> {
            override fun onResponse(call: Call<PhoneSellNumberCollectionDao>, response: Response<PhoneSellNumberCollectionDao>) {
                progressbar_phone_buyall?.isVisible = false
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val anyList: ArrayList<Any> = arrayListOf()
                    anyList.add(PhoneSellHeader("header"))

                    for (item in data.phonenumberSell!!) {
                        anyList.add(PhoneNumberItem(item.phoneNumber!!, item.phoneSum, NumberFormat.getInstance().format(item.phonePrice!!.toInt()).toString(), item.phone_group, item.sell_status, item.prefix_group))
                    }

                    anyList.add("footer")
                    recycleview_phone_sell?.adapter = PhoneSellAdapter(anyList)
                    recycleview_phone_sell?.layoutManager = LinearLayoutManager(context)
                } else {
                    coor_phonebuy?.let { SnackContextManager.setSnack("โปรดตรวจสอบอินเตอร์เน็ต และลองใหม่อีกครั้ง!!", it) }
                }
            }

            override fun onFailure(call: Call<PhoneSellNumberCollectionDao>, t: Throwable) {
                progressbar_phone_buyall?.isVisible = false
                coor_phonebuy?.let { SnackContextManager.setSnack("โปรดตรวจสอบอินเตอร์เน็ต และลองใหม่อีกครั้ง!!", it) }
                Log.d("FROMSERVER", "error" + t.message)
            }
        })

    }







}
