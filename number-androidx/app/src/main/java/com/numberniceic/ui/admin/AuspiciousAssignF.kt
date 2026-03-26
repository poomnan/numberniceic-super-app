package com.numberniceic.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.numberniceic.R
import com.numberniceic.adapters.FindUserAdapter
import com.numberniceic.data.admin.UserZ
import com.numberniceic.data.admin.Users
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuspiciousAssignF : Fragment() {

    private lateinit var edtFindUser: EditText
    private lateinit var btnFindUser: Button
    private lateinit var txtSearchStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recycleUser: RecyclerView

    companion object {
        fun newInstance(): AuspiciousAssignF {
            val args = Bundle()
            val fragment = AuspiciousAssignF()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_buddha_assign, container, false) // Reuse Layout
        
        edtFindUser = root.findViewById(R.id.edt_find_user)
        btnFindUser = root.findViewById(R.id.btn_find_user)
        txtSearchStatus = root.findViewById(R.id.txt_search_status)
        val txtSearchTitle = root.findViewById<TextView>(R.id.txt_search_title)
        txtSearchTitle.text = "ค้นหาลูกค้าเพื่อจัดการวันมงคล"
        
        progressBar = root.findViewById(R.id.progress_bar)
        recycleUser = root.findViewById(R.id.recycle_user)
        
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        loadLatestUsers()
    }

    private fun initUI() {
        btnFindUser.setOnClickListener {
            val username = edtFindUser.text.toString()
            if (username.isNotEmpty() && username.isNotBlank()){
                searchUsers(username)
            } else {
                loadLatestUsers()
            }
        }
    }

    private fun loadLatestUsers() {
        txtSearchStatus.text = "รายชื่อลูกค้าล่าสุด"
        progressBar.visibility = View.VISIBLE
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.findUsersByUsername("").enqueue(object : Callback<Users> {
            override fun onResponse(call: Call<Users>, response: Response<Users>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val users = response.body()!!
                    if (users.resultUserz != null && users.resultUserz!!.isNotEmpty()) {
                        displayUsers(users.resultUserz!!)
                    } else {
                        txtSearchStatus.text = "ไม่มีข้อมูลผู้ใช้งาน"
                        recycleUser.adapter = null
                    }
                } else {
                    Toast.makeText(context, "เกิดข้อผิดพลาดในการโหลดข้อมูล", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Users>, t: Throwable) {
                progressBar.visibility = View.GONE
                if (isAdded) {
                    Toast.makeText(context, "เกิดข้อผิดพลาด: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun searchUsers(query: String) {
        txtSearchStatus.text = "ผลการค้นหา: $query"
        progressBar.visibility = View.VISIBLE
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.findUsersByUsername(query).enqueue(object : Callback<Users> {
            override fun onResponse(call: Call<Users>, response: Response<Users>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val users = response.body()!!
                    if (users.resultUserz != null && users.resultUserz!!.isNotEmpty()) {
                        displayUsers(users.resultUserz!!)
                    } else {
                        txtSearchStatus.text = "ไม่พบผู้ใช้งาน: $query"
                        recycleUser.adapter = null
                    }
                } else {
                    Toast.makeText(context, "เกิดข้อผิดพลาดในการค้นหา", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Users>, t: Throwable) {
                progressBar.visibility = View.GONE
                if (isAdded) {
                    Toast.makeText(context, "เกิดข้อผิดพลาด: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun displayUsers(users: List<UserZ>) {
        val adapter = FindUserAdapter(users, object : FindUserAdapter.OnItemClickListener{
            override fun itemClick(userZ: UserZ) {
                // Open Native Form
                val intent = Intent(context, AuspiciousFormAct::class.java)
                intent.putExtra("user_data", userZ)
                startActivity(intent)
            }
        })

        recycleUser.layoutManager = LinearLayoutManager(context)
        recycleUser.adapter = adapter
    }
}
