package com.numberniceic.ui.admin


import android.content.Intent
import android.os.Bundle
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.numberniceic.R
import com.numberniceic.adapters.FindUserAdapter
import com.numberniceic.data.admin.UserZ
import com.numberniceic.data.admin.Users
import com.numberniceic.databinding.FragmentBagColorBinding


class BagColorF : Fragment() {

    private lateinit var binding:FragmentBagColorBinding

    companion object {
        fun newInstance(): BagColorF {
            val args = Bundle()

            val fragment = BagColorF()
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {


        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_bag_color, container, false)
        binding.apply {
            lifecycleOwner = this@BagColorF

        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.txtSearchTitle.text = "ค้นหาลูกค้าเพื่อเพิ่มสีกระเป๋า"
        initUI()
        // Load latest users on start
        loadLatestUsers()
    }

    private fun initUI() {
        binding.btnFindUser.setOnClickListener {
            val username = binding.edtFindUser.text.toString()
            if (username.isNotEmpty() && username.isNotBlank()){
                searchUsers(username)
            }else{
                // If empty, load latest users
                loadLatestUsers()
            }
        }
    }

    private fun loadLatestUsers() {
        binding.txtSearchStatus.text = "รายชื่อผู้ใช้งานล่าสุด (50 รายการ)"
        binding.progressBar.visibility = View.VISIBLE
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.findUsersByUsername("").enqueue(object : Callback<Users> {
            override fun onResponse(call: Call<Users>, response: Response<Users>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val users = response.body()!!
                    if (users.resultUserz!!.isNotEmpty()) {
                        displayUsers(users.resultUserz)
                    } else {
                        binding.txtSearchStatus.text = "ไม่มีข้อมูลผู้ใช้งาน"
                        binding.recycleUser.adapter = null
                    }
                } else {
                    Toast.makeText(context, "เกิดข้อผิดพลาดในการโหลดข้อมูล", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Users>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "เกิดข้อผิดพลาด: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.d("RetrofitError", t.message ?: "Unknown error")
            }
        })
    }

    private fun searchUsers(query: String) {
        binding.txtSearchStatus.text = "ผลการค้นหา: $query"
        binding.progressBar.visibility = View.VISIBLE
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.findUsersByUsername(query).enqueue(object : Callback<Users> {
            override fun onResponse(call: Call<Users>, response: Response<Users>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val users = response.body()!!
                    if (users.resultUserz!!.isNotEmpty()) {
                        displayUsers(users.resultUserz)
                    } else {
                        binding.txtSearchStatus.text = "ไม่พบผู้ใช้งานที่ค้นหา: $query"
                        binding.recycleUser.adapter = null
                    }
                } else {
                    Toast.makeText(context, "เกิดข้อผิดพลาดในการค้นหา", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Users>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "เกิดข้อผิดพลาด: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.d("RetrofitError", t.message ?: "Unknown error")
            }
        })
    }

    private fun displayUsers(users: List<UserZ>) {
        val adapter = FindUserAdapter(users, object : FindUserAdapter.OnItemClickListener{
            override fun itemClick(userZ: UserZ) {
                val intent = Intent(context, BagPaletteAct::class.java)
                intent.putExtra("userz", userZ)
                startActivity(intent)
            }
        })

        binding.recycleUser.layoutManager = LinearLayoutManager(context)
        binding.recycleUser.adapter = adapter
    }
}

