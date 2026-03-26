package com.numberniceic.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GuestAddressManagementActivity2 : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var statsTextView: TextView
    private lateinit var pendingStatsText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var refreshButton: View
    private lateinit var backButton: View
    
    private lateinit var addressAdapter: AddressAdapter
    private var allAddresses = mutableListOf<JsonObject>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guest_address_management)
        
        initViews()
        setupRecyclerView()
        setupClickListeners()
        loadAddresses()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewAddresses)
        searchEditText = findViewById(R.id.searchEditText)
        statsTextView = findViewById(R.id.statsTextView)
        pendingStatsText = findViewById(R.id.pendingStatsText)
        progressBar = findViewById(R.id.progressBar)
        refreshButton = findViewById(R.id.refreshButton)
        backButton = findViewById(R.id.backButton)
    }
    
    private fun setupRecyclerView() {
        addressAdapter = AddressAdapter(allAddresses) { orderId, currentStatus ->
            val nextStatus = if (currentStatus == "shipped") "pending" else "shipped"
            toggleShippingStatus(orderId, nextStatus)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = addressAdapter
    }
    
    private fun toggleShippingStatus(orderId: Int, nextStatus: String) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val body = JsonObject().apply {
            addProperty("order_id", orderId)
            addProperty("status", nextStatus)
        }
        
        progressBar.visibility = View.VISIBLE
        apiService.toggleShippingStatus(body).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.safeGetBoolean("success") == true) {
                    val statusText = if (nextStatus == "shipped") "จัดส่งเรียบร้อย ✅" else "เปลี่ยนกลับเป็นรอจัดส่ง ⏳"
                    Toast.makeText(this@GuestAddressManagementActivity2, statusText, Toast.LENGTH_SHORT).show()
                    loadAddresses() // Reload to update list
                } else {
                    Toast.makeText(this@GuestAddressManagementActivity2, "เกิดข้อผิดพลาดในการเปลี่ยนสถานะ", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@GuestAddressManagementActivity2, "เชื่อมต่อไม่ได้: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun setupClickListeners() {
        backButton.setOnClickListener { finish() }
        refreshButton.setOnClickListener { loadAddresses() }
        
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAddresses(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
    
    private fun loadAddresses() {
        progressBar.visibility = View.VISIBLE
        refreshButton.isEnabled = false
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getGuestAddresses().enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                progressBar.visibility = View.GONE
                refreshButton.isEnabled = true
                
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    if (responseBody.safeGetBoolean("success")) {
                        val dataArray = responseBody.get("data")?.asJsonArray
                        pendingShipments = responseBody.get("stats")?.asJsonObject?.safeGetInt("pending_shipments") ?: 0
                        if (dataArray != null) {
                            allAddresses.clear()
                            for (item in dataArray) {
                                allAddresses.add(item.asJsonObject)
                            }
                            addressAdapter.notifyDataSetChanged()
                            updateStats()
                        } else {
                            Toast.makeText(this@GuestAddressManagementActivity2, "ไม่พบข้อมูลรายการสั่งซื้อ", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val message = responseBody.safeGetString("message") ?: "ไม่สามารถดึงข้อมูลได้"
                        Toast.makeText(this@GuestAddressManagementActivity2, message, Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@GuestAddressManagementActivity2, "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }
            
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                progressBar.visibility = View.GONE
                refreshButton.isEnabled = true
                Toast.makeText(this@GuestAddressManagementActivity2, "เชื่อมต่อไม่ได้: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("GuestAddressActivity", "API fail", t)
            }
        })
    }
    
    private fun filterAddresses(searchText: String) {
        val filteredList = if (searchText.isBlank()) {
            allAddresses
        } else {
            allAddresses.filter { address ->
                val guestId = address.safeGetString("guest_id") ?: ""
                val addressText = address.safeGetString("address") ?: ""
                val customerName = address.safeGetString("customer_name") ?: ""
                val orderSummary = address.safeGetString("order_summary") ?: ""
                val androidId = address.safeGetString("android_id") ?: ""
                
                guestId.contains(searchText, ignoreCase = true) ||
                addressText.contains(searchText, ignoreCase = true) ||
                customerName.contains(searchText, ignoreCase = true) ||
                orderSummary.contains(searchText, ignoreCase = true) ||
                androidId.contains(searchText, ignoreCase = true)
            }
        }
        addressAdapter.updateList(filteredList)
        updateStats(filteredList)
    }
    
    private var pendingShipments = 0
    
    private fun updateStats(addressList: List<JsonObject> = allAddresses) {
        val totalAddresses = addressList.size
        val uniqueGuests = addressList.map { it.get("guest_id")?.asString }.distinct().size
        val totalOrders = addressList.sumOf { it.safeGetInt("order_count") }
        
        statsTextView.text = "📍 ที่อยู่: $totalAddresses | 👤 Guest: $uniqueGuests | 📦 คำสั่งซื้อ: $totalOrders"
        pendingStatsText.text = "⚠️ รอจัดส่ง: $pendingShipments รายการ"
        
        if (pendingShipments > 0) {
            pendingStatsText.setTextColor(android.graphics.Color.parseColor("#E65100"))
        } else {
            pendingStatsText.text = "✅ จัดส่งครบแล้ว"
            pendingStatsText.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        }
    }
}

// Case-proof JSON extensions
private fun JsonObject.safeGetString(key: String): String? {
    return try {
        val element = get(key)
        if (element != null && !element.isJsonNull) element.asString else null
    } catch (e: Exception) { null }
}

private fun JsonObject.safeGetInt(key: String, defaultValue: Int = 0): Int {
    return try {
        val element = get(key)
        if (element != null && !element.isJsonNull) element.asInt else defaultValue
    } catch (e: Exception) { defaultValue }
}

private fun JsonObject.safeGetBoolean(key: String, defaultValue: Boolean = false): Boolean {
    return try {
        val element = get(key)
        if (element != null && !element.isJsonNull) try { element.asBoolean } catch(ex: Exception) { element.asString == "true" } else defaultValue
    } catch (e: Exception) { defaultValue }
}

class AddressAdapter(
    private var addresses: List<JsonObject>,
    private val onShippingToggle: ((orderId: Int, currentStatus: String) -> Unit)? = null
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {
    
    class AddressViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val guestIdText: TextView = view.findViewById(R.id.guestIdText)
        val addressText: TextView = view.findViewById(R.id.addressText)
        val orderInfoText: TextView = view.findViewById(R.id.orderInfoText)
        val androidIdText: TextView = view.findViewById(R.id.androidIdText)
        val deviceInfoText: TextView = view.findViewById(R.id.deviceInfoText)
        val createdAtText: TextView = view.findViewById(R.id.createdAtText)
        val btnToggleShipping: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btnToggleShipping)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): AddressViewHolder {
        val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_guest_address, parent, false)
        return AddressViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val address = addresses[position]
        
        val guestId = address.safeGetString("guest_id") ?: "N/A"
        val customerName = address.safeGetString("customer_name")
        val customerType = address.safeGetString("customer_type")
        
        if (customerType == "member" && !customerName.isNullOrEmpty()) {
            holder.guestIdText.text = "👤 Member: $customerName"
            holder.guestIdText.setTextColor(android.graphics.Color.parseColor("#1565C0"))
        } else {
            val displayId = if (guestId.length > 12) guestId.take(12) + "..." else guestId
            holder.guestIdText.text = "Guest: $displayId"
            holder.guestIdText.setTextColor(android.graphics.Color.parseColor("#333333"))
        }
        
        holder.addressText.text = address.safeGetString("address") ?: "(ไม่มีข้อมูลที่อยู่)"
        
        // Items summary
        val summary = address.safeGetString("order_summary")
        if (!summary.isNullOrEmpty()) {
            holder.orderInfoText.text = summary
            holder.orderInfoText.setTextColor(android.graphics.Color.parseColor("#333333"))
        } else {
            holder.orderInfoText.text = "ไม่มีข้อมูลสินค้า"
            holder.orderInfoText.setTextColor(android.graphics.Color.parseColor("#999999"))
        }
        
        holder.deviceInfoText.text = "อุปกรณ์: ${address.safeGetString("device_info") ?: "-"}"
        holder.androidIdText.text = "ID: ${address.safeGetString("android_id") ?: "-"}"
        holder.createdAtText.text = address.safeGetString("server_created_at") ?: "-"
        
        // Shipping status from first order
        val orders = address.get("orders")?.asJsonArray
        if (orders != null && orders.size() > 0) {
            val firstOrder = orders[0].asJsonObject
            val orderId = firstOrder.safeGetInt("id")
            val status = firstOrder.safeGetString("shipping_status") ?: "pending"
            
            holder.btnToggleShipping.visibility = View.VISIBLE
            updateButtonStatus(holder.btnToggleShipping, status)
            
            holder.btnToggleShipping.setOnClickListener {
                onShippingToggle?.invoke(orderId, status)
            }
        } else {
            holder.btnToggleShipping.visibility = View.GONE
        }
    }
    
    private fun updateButtonStatus(btn: com.google.android.material.button.MaterialButton, status: String) {
        if (status == "shipped") {
            btn.text = "จัดส่งแล้ว ✅"
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50"))
        } else {
            btn.text = "รอจัดส่ง ⏳"
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800"))
        }
    }
    
    override fun getItemCount(): Int = addresses.size
    fun updateList(newList: List<JsonObject>) { addresses = newList; notifyDataSetChanged() }
}
