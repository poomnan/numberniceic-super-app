package com.numberniceic.ui.namenick


import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.gson.Gson

import com.numberniceic.R

import com.numberniceic.adapters.NickNameVipAdapter
import com.numberniceic.data.admin.Userx
import com.numberniceic.data.member.MemberVipCollectionDao
import com.numberniceic.data.nickname.NameNickVipCollection
import com.numberniceic.data.nickname.NameNickVipItem
import com.numberniceic.ui.auth.VipBlockNameF
import com.numberniceic.ui.auth.VipBlockNickF
import com.numberniceic.utils.SnackContextManager
import com.numberniceic.utils.UserContextManager


class NameNickVipF : Fragment(), NameNickListAct.SelectBuddle {

    private var progress_vip_namelist: ProgressBar? = null
    private var li_nick_vip: View? = null
    private var recycler_nickname_vip: RecyclerView? = null
    private var coor_vip_nick: CoordinatorLayout? = null

    private var sharedUser: SharedPreferences? = null
    private lateinit var day: String
    private lateinit var charx: String
    private lateinit var prefix: String

    private var statusHideNick: Boolean = false
    private var statusHideSur: Boolean = false

    private lateinit var manager: FragmentManager

    private lateinit var f: FragmentTransaction

    private var usetable: String? = null

    private lateinit var dao: NameNickVipCollection

    private var layoutManager: LinearLayoutManager? = null
    private var adapter: NickNameVipAdapter? = null
    private val xList = arrayListOf<Any>()

    companion object {
        fun newInstance(usetable: String): NameNickVipF {
            val args = Bundle()
            args.putString("usetable", usetable)

            val fragment = NameNickVipF()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {




        val nameNickAct = activity as NameNickListAct

        nameNickAct.setOnBundleSelected(this)

        this.manager = childFragmentManager
        this.f = manager.beginTransaction()


        this.layoutManager = LinearLayoutManager(context)
        this.adapter = NickNameVipAdapter(arrayListOf())


        return inflater.inflate(R.layout.fragment_name_nick_vip, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedUser = view.context.getSharedPreferences("userdata", Context.MODE_PRIVATE)

        progress_vip_namelist = view.findViewById(R.id.progress_vip_namelist)
        li_nick_vip = view.findViewById(R.id.li_nick_vip)
        recycler_nickname_vip = view.findViewById(R.id.recycler_nickname_vip)
        coor_vip_nick = view.findViewById(R.id.coor_vip_nick)

        if (progress_vip_namelist != null) progress_vip_namelist?.isVisible = false

        this.usetable = arguments!!.getString("usetable") as String


        if (this.usetable == "realname") {
            li_nick_vip?.setBackgroundColor(ContextCompat.getColor(view.context, R.color.color_head_realname_table))
        }


        chckShared()


    }


    private fun setDao(dao: NameNickVipCollection?, type: String) {
        xList.clear()

        if (dao != null) {

            if (type == "NEWDATA") xList.add("header")

            for (item in dao.NickNameListVip!!) {

                xList.add(item)
                adapter!!.notifyDataSetChanged()

            }


            if (recycler_nickname_vip != null) {


                recycler_nickname_vip?.adapter = NickNameVipAdapter(xList)
                recycler_nickname_vip?.layoutManager = this.layoutManager


                /*  recycler_nickname_vip.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                      override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                          super.onScrollStateChanged(recyclerView, newState)
                          if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                              isScroling = true
                          }
                      }

                      override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                          super.onScrolled(recyclerView, dx, dy)
                          currentItems = layoutManager!!.childCount
                          totalItems = layoutManager!!.itemCount
                          scrollOutItems = layoutManager!!.findFirstVisibleItemPosition()


                          if (isScroling && (currentItems!! + scrollOutItems!! == totalItems)) {
                              isScroling = false


                              val prefixx = when (prefix) {
                                  "เพศ" -> "x"
                                  "ชาย" -> "ช"
                                  "หญิง" -> "ญ"
                                  "ชายหญิง" -> "ชญ"
                                  else -> "x"

                              }

                              callAPIScholling(usetable!!, day, charx, prefixx, getMaxId(xList).toString())


                          }
                      }
                  })*/


            }
        }
    }


    override fun onBundleSeclect(day: String, charx: String, prefix: String) {

        this.day = day
        this.charx = charx
        this.prefix = prefix


        val prefixx = when (prefix) {
            "เพศ" -> "x"
            "ชาย" -> "ช"
            "หญิง" -> "ญ"
            "ชายหญิง" -> "ชญ"
            else -> "x"

        }


        this.callAPI(this.usetable!!, day, charx, prefixx)


    }

    private fun callAPI(usetable: String, day: String, charx: String, txtPrefix: String) {

        Log.d("CALAPI", usetable)
        Log.d("CALAPI", day)
        Log.d("CALAPI", charx)
        Log.d("CALAPI", txtPrefix)

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getNicknameList(usetable, day, charx, txtPrefix).enqueue(object : Callback<NameNickVipCollection> {
            override fun onResponse(call: Call<NameNickVipCollection>, response: Response<NameNickVipCollection>) {
                if (response.isSuccessful && response.body() != null) {
                    val dao = response.body()!!
                    if (progress_vip_namelist != null) progress_vip_namelist?.isVisible = false
                    setDao(dao, "NEWDATA")
                    this@NameNickVipF.dao = dao
                } else {
                    if (progress_vip_namelist != null) progress_vip_namelist?.isVisible = false
                    coor_vip_nick?.let { SnackContextManager.setSnack("เกิดข้อผิดพลาดในการโหลดข้อมูล", it) }
                }
            }

            override fun onFailure(call: Call<NameNickVipCollection>, t: Throwable) {
                if (progress_vip_namelist != null) progress_vip_namelist?.isVisible = false
                coor_vip_nick?.let { SnackContextManager.setSnack("อาจมีปัญหาเกี่ยวกับเครื่อข่าย โปรดตรวจสอบ!!", it) }
                Log.d("ERRORANANYA", "error" + t.message)
            }
        })
    }


    private fun hideDataNickname() {
        if(!this.statusHideNick){
            if (this.usetable == "nickname") {
                this.statusHideNick = true

                f.replace(R.id.fragment_block_vip_container, VipBlockNickF(), "VipBlockNickF")
                f.commit()
                manager.executePendingTransactions()
            }
        }

    }

    private fun hideDataRealName() {

        if (!this.statusHideSur){
            if (this.usetable == "realname") {
                this.statusHideSur = true
                f.replace(R.id.fragment_block_vip_container, VipBlockNameF(), "VipBlockNameF")
                f.commit()
                manager.executePendingTransactions()
            }
        }

    }

    override fun onResume() {
        super.onResume()

        chckShared()


    }

    private fun chckShared() {
        val userJson = this.sharedUser!!.getString("json", null)
        if (userJson != null) {
            val userX = Gson().fromJson(userJson, Userx::class.java)

            when (userX.vipcode) {
                "normal" -> if (this.usetable == "realname") hideDataRealName() else hideDataNickname()
                "silver" -> if (this.usetable == "realname") hideDataRealName() else hideDataNickname()
                "gold" -> {
                    removeBlockNickName()
                    removeBlockRealName()
                }
                "diamond" -> {
                    removeBlockNickName()
                    removeBlockRealName()
                }
                "admin" -> {
                    removeBlockNickName()
                    removeBlockRealName()
                }
                else -> if (this.usetable == "realname") hideDataRealName() else hideDataNickname()
            }

        }else{
            if (this.usetable == "realname") hideDataRealName() else hideDataNickname()
        }


    }




    private fun removeBlockRealName() {
        if(statusHideSur){
            val f = manager.findFragmentByTag("VipBlockNameF") as VipBlockNameF?
            if (f != null) {
                manager.beginTransaction().remove(f).commit()

            }

        }

    }

    private fun removeBlockNickName() {
        if(statusHideNick){
            val f = manager.findFragmentByTag("VipBlockNickF") as VipBlockNickF?
            if (f != null) {
                manager.beginTransaction().remove(f).commit()
            }
        }

    }

    private fun callAPIScholling(usetable: String, day: String, charx: String, txtPrefix: String, lastid: String) {

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getNicknameVipList(usetable, day, charx, txtPrefix, lastid).enqueue(object : Callback<NameNickVipCollection> {
            override fun onResponse(call: Call<NameNickVipCollection>, response: Response<NameNickVipCollection>) {
                if (response.isSuccessful && response.body() != null) {
                    val dao = response.body()!!
                    if (progress_vip_namelist != null) progress_vip_namelist?.isVisible = false
                    setDao(dao, "ADDDATA")
                }
            }

            override fun onFailure(call: Call<NameNickVipCollection>, t: Throwable) {
                if (progress_vip_namelist != null) progress_vip_namelist?.isVisible = false
                coor_vip_nick?.let { SnackContextManager.setSnack("อาจมีปัญหาเกี่ยวกับเครื่อข่าย โปรดตรวจสอบ!!", it) }
                Log.d("ERRORANANYA", "error" + t.message)
            }
        })
    }

    private fun getMaxId(xList: ArrayList<Any>): Int {

        if (xList[0] is NameNickVipItem) {

            val item = xList[0] as NameNickVipItem

            var maxId = item.nicnameid!!.toInt()



            for ((index) in xList.withIndex()) {
                val x = xList[index] as NameNickVipItem
                maxId = Math.max(maxId, x.nicnameid!!.toInt())
            }

            return maxId
        }

        return 0

    }


}

