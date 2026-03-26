package com.numberniceic.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.JsonObject
import com.numberniceic.R
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class AdminDreamOrdersActivity : AppCompatActivity() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: View
    private lateinit var txtEmpty: TextView
    private lateinit var adapter: DreamOrderAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dream_orders)

        initViews()
        fetchOrders()
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_dream_orders)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "รายการสั่งซื้อเพทาย"
        toolbar.setNavigationOnClickListener { finish() }

        rvOrders = findViewById(R.id.rv_dream_orders)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        progressBar = findViewById(R.id.progress_bar)
        txtEmpty = findViewById(R.id.txt_empty)

        rvOrders.layoutManager = LinearLayoutManager(this)
        adapter = DreamOrderAdapter()
        rvOrders.adapter = adapter

        swipeRefresh.setOnRefreshListener { fetchOrders() }
    }

    private fun fetchOrders() {
        swipeRefresh.isRefreshing = true
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getZirconOrders().enqueue(object : Callback<List<JsonObject>> {
            override fun onResponse(call: Call<List<JsonObject>>, response: Response<List<JsonObject>>) {
                swipeRefresh.isRefreshing = false
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val orders = response.body()!!
                    adapter.submitList(orders)
                    txtEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    txtEmpty.visibility = View.VISIBLE
                    txtEmpty.text = "เกิดข้อผิดพลาด: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<JsonObject>>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                progressBar.visibility = View.GONE
                txtEmpty.visibility = View.VISIBLE
                txtEmpty.text = "เชื่อมต่อล้มเหลว: ${t.message}"
            }
        })
    }

    inner class DreamOrderAdapter : RecyclerView.Adapter<DreamOrderAdapter.ViewHolder>() {
        private var items = listOf<JsonObject>()

        fun submitList(newItems: List<JsonObject>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_dream_order, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val txtProductName: TextView = view.findViewById(R.id.txt_product_name)
            private val txtStatus: TextView = view.findViewById(R.id.txt_status)
            private val txtRefNo: TextView = view.findViewById(R.id.txt_ref_no)
            private val txtCustomer: TextView = view.findViewById(R.id.txt_customer)
            private val txtAmount: TextView = view.findViewById(R.id.txt_amount)
            private val txtDate: TextView = view.findViewById(R.id.txt_date)

            fun bind(item: JsonObject) {
                txtProductName.text = item.get("product_name")?.asString ?: "N/A"
                val status = item.get("status")?.asString?.uppercase() ?: "PENDING"
                txtStatus.text = status
                
                if (status == "PAID") {
                    txtStatus.setBackgroundResource(R.drawable.bg_status_paid)
                } else {
                    txtStatus.setBackgroundResource(R.drawable.bg_status_pending)
                }

                txtRefNo.text = "Ref: ${item.get("ref_no")?.asString}"
                
                val guestId = item.get("guest_id")?.asString
                val userId = item.get("user_id")?.asLong ?: 0
                txtCustomer.text = if (userId > 0) "ลูกค้า: User (id: $userId)" else "ลูกค้า: Guest (id: $guestId)"
                
                val amount = item.get("amount")?.asDouble ?: 0.0
                txtAmount.text = "จำนวนเงิน: %.2f บาท".format(amount)

                // Format date: 2026-02-13T22:13:27.137147
                val rawDate = item.get("created_at")?.asString ?: ""
                try {
                    val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    val date = sdfInput.parse(rawDate)
                    if (date != null) {
                        val sdfOutput = SimpleDateFormat("d MMM yyyy HH:mm", Locale("th", "TH"))
                        txtDate.text = sdfOutput.format(date)
                    } else {
                        txtDate.text = rawDate
                    }
                } catch (e: Exception) {
                    txtDate.text = rawDate
                }
            }
        }
    }
}
