package com.numberniceic.ui.home


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.android.material.snackbar.Snackbar
import com.numberniceic.BR

import com.numberniceic.R
import com.numberniceic.adapters.HomePairAdapter
import com.numberniceic.data.apicollectiondao.HomeCollectionDao
import com.numberniceic.data.apicollectiondao.TabianCollectionDao
import com.numberniceic.databinding.FragmentHomeBinding
import com.numberniceic.ui.tabian.TabianF
import com.numberniceic.ui.tabian.TabianViewModel
import com.numberniceic.utils.AppContextManager
import com.numberniceic.utils.HomeContextManager
import com.numberniceic.utils.SnackContextManager
import android.widget.ProgressBar
import android.widget.EditText
import androidx.coordinatorlayout.widget.CoordinatorLayout
import org.w3c.dom.Text


class HomeF : Fragment(), View.OnClickListener {

    private var sharedRefHome: SharedPreferences? = null
    private var sharedRefUser: SharedPreferences? = null
    private var dao: HomeCollectionDao? = null

    private lateinit var binding: FragmentHomeBinding

    private var fragmentView: View? = null

    private lateinit var homeVieModel: HomeViewModel


    companion object {
        fun newInstance(homeNumber: String): HomeF {
            val args = Bundle()
            args.putString("homeNumber", homeNumber)
            val fragment = HomeF()
            fragment.arguments = args
            return fragment
        }
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        Log.d("LIFECYCLE", "TAB HOME CREATE")

        homeVieModel = ViewModelProvider(this)[HomeViewModel::class.java]
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        binding.apply {
            setLifecycleOwner(this@HomeF)
            binding.homeObs = homeVieModel.homeObs

        }

        val manager = childFragmentManager
        // ใช้ commitNow() แทน commit() + executePendingTransactions() เพื่อความเร็ว
        val ft = manager.beginTransaction()
        ft.replace(R.id.fragment_ad_container, HomeAdF(), "HomeAdF")
        ft.commitNow()

        fragmentView = binding.root

        return fragmentView
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        this.sharedRefHome = view.context.getSharedPreferences("homedata", Context.MODE_PRIVATE)
        this.sharedRefUser = view.context.getSharedPreferences("userdata", Context.MODE_PRIVATE)

        val progressbar_home = view.findViewById<ProgressBar>(R.id.progressbar_home)
        val edtHomeNum = view.findViewById<EditText>(R.id.edtHomeNum)

        if (progressbar_home != null) progressbar_home.isVisible = false

        if (edtHomeNum.text!!.isNotEmpty()) {
            callHomeAPi(edtHomeNum.text.toString())
        }


        if (savedInstanceState != null) {
            if (savedInstanceState.getParcelable<HomeCollectionDao>("DAO") != null) {
                this.dao = savedInstanceState.getParcelable("DAO")

            }

        }

        if (this.dao != null) {
            if (this.dao!!.pairsA!!.isNotEmpty()) {
                setRecyclerView()
            }
        }

        initInstances()

    }

    private val refreshReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("HomeF", "Refresh Broadcast Received - Updating VIP state")
            // Re-read shared prefs just in case
            context?.let {
                sharedRefUser = it.getSharedPreferences("userdata", Context.MODE_PRIVATE)
                // If there's an error message about VIP, clear it or re-run calculation
                if (binding.edtHomeNum.text!!.isNotEmpty()) {
                    callHomeAPi(binding.edtHomeNum.text.toString())
                }
            }
        }
    }

    private fun initInstances() {
        binding.btnHomeCal.setOnClickListener(this)
        binding.btnHomeMiracle.setOnClickListener(this)
        binding.btnHomeClear.setOnClickListener(this)

        // Register Refresh Listener
        val filter = android.content.IntentFilter("com.numberniceic.REFRESH_DASHBOARD")
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            context?.registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context?.registerReceiver(refreshReceiver, filter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            context?.unregisterReceiver(refreshReceiver)
        } catch (e: Exception) {}
    }

    override fun onClick(v: View?) {

        if (v == binding.btnHomeClear) {
            clearEDT()
        }

        if (v == binding.btnHomeCal) {
            getHomeApi(binding.edtHomeNum.text.toString(), v)
        }

        val coordinator_home = binding.coordinatorHome
        if (v == binding.btnHomeMiracle) {
            if (this.dao != null) {
                val intent = Intent(context, HomeMiraActivity::class.java)
                intent.putExtra("DAO", this.dao)
                startActivity(intent)
                Snackbar.make(coordinator_home, "Home Miracle!!", Snackbar.LENGTH_SHORT).show()
            } else {
                val coordinator_home = binding.coordinatorHome
                val snack = Snackbar.make(coordinator_home, "โปรดถอดรหัสก่อน!!", Snackbar.LENGTH_SHORT)
                val tv = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                tv.setTextColor(Color.YELLOW)
                snack.show()

                Snackbar.make(coordinator_home, "โปรดถอดรหัสก่อน!!", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearEDT() {
        val f = childFragmentManager.findFragmentByTag("HomeAdF") as HomeAdF?
        if (f != null) {
            childFragmentManager.beginTransaction().remove(f).commitNow()
        }

        val manager = childFragmentManager
        val ft = manager.beginTransaction()
        ft.replace(R.id.fragment_ad_container, HomeAdF(), "HomeAdF")
        ft.commitNow()

        binding.edtHomeNum.text!!.clear()
        binding.edtHomeNum.requestFocus()

        Snackbar.make(binding.coordinatorHome, "ล้างเพื่อพิมพ์ใหม่!!", Snackbar.LENGTH_SHORT).show()
    }

    private fun getHomeApi(homeNumber: String, v: View) {
        if (v == binding.btnHomeCal) {
            callHomeAPi(homeNumber)
        }

        val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun callHomeAPi(homeNumber: String) {
        val coordinator_home = binding.coordinatorHome
        if (HomeContextManager.checkString(homeNumber)) {
            val snack = Snackbar.make(coordinator_home, "ข้อมูลไม่ถูกต้อง!!", Snackbar.LENGTH_LONG)
            val tv = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            tv.setTextColor(Color.YELLOW)
            snack.show()
        } else {

            AppContextManager.countHomeCal(this.sharedRefHome)

            if (AppContextManager.checkPermisHomeVip(this.sharedRefHome, this.sharedRefUser!!)){
                if (view?.findViewById<ProgressBar>(R.id.progressbar_home) != null) view?.findViewById<ProgressBar>(R.id.progressbar_home)!!.isVisible = true
                val pealNum = if (HomeContextManager.meeSlat(homeNumber).isNullOrBlank()) homeNumber else HomeContextManager.meeSlat(homeNumber)
                val apiService = RetrofitClient.instance.create(ApiService::class.java)
                apiService.getHomeDetail(pealNum!!).enqueue(object : Callback<HomeCollectionDao> {
                    override fun onResponse(call: Call<HomeCollectionDao>, response: Response<HomeCollectionDao>) {
                        if (view?.findViewById<ProgressBar>(R.id.progressbar_home) != null) view?.findViewById<ProgressBar>(R.id.progressbar_home)!!.isVisible = false
                        
                        if (response.isSuccessful && response.body() != null) {
                            val dao = response.body()!!
                            
                            // ลบ fragment เก่าถ้ามี
                            val f = childFragmentManager.findFragmentByTag("HomeAdF") as HomeAdF?
                            if (f != null) {
                                childFragmentManager.beginTransaction().remove(f).commitNow()
                            }

                            // เพิ่ม fragment ใหม่
                            val manager = childFragmentManager
                            val ft = manager.beginTransaction()
                            ft.replace(R.id.fragment_ad_container_re, HomeAdF(), "HomeAdF")
                            ft.commitNow()

                            this@HomeF.dao = dao
                            this@HomeF.homeVieModel.addHomeDao(dao)
                            setRecyclerView()
                        } else {
                            SnackContextManager.setSnack("อาจมีปัญหาเกี่ยวกับเครื่อข่าย โปรดตรวจสอบ!!", coordinator_home)
                        }
                    }

                    override fun onFailure(call: Call<HomeCollectionDao>, t: Throwable) {
                        if (view?.findViewById<ProgressBar>(R.id.progressbar_home) != null) view?.findViewById<ProgressBar>(R.id.progressbar_home)!!.isVisible = false
                        SnackContextManager.setSnack("อาจมีปัญหาเกี่ยวกับเครื่อข่าย โปรดตรวจสอบ!!", coordinator_home)
                        Log.d("ERRORANANYA", "error" + t.message)
                    }
                })
            }else{
                Toast.makeText(context, getString(R.string.request_vip), Toast.LENGTH_SHORT).show()
            }




        }
    }

    private fun setRecyclerView() {

        binding.recyclePairsHome.adapter = HomePairAdapter(this.dao!!.pairsA!!, this.dao!!.pairMiracle!!)
        binding.recyclePairsHome.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        binding.recyclePairsHomeB.adapter = HomePairAdapter(this.dao!!.pairsB!!, this.dao!!.pairMiracle!!)
        binding.recyclePairsHomeB.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {


        if (this.dao != null) {
            outState.run {
                outState.putParcelable("DAO", dao)
            }

        }

        super.onSaveInstanceState(outState)
    }


}
