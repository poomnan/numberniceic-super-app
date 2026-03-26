package com.numberniceic.ui.admin

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GuestAddressManagementActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            GuestAddressManagementScreen()
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GuestAddressManagementScreen() {
        var addresses by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var searchText by remember { mutableStateOf("") }
        var searchResults by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
        var showSearch by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        
        // Load all addresses on startup
        LaunchedEffect(Unit) {
            loadAllAddresses(
                onSuccess = { result ->
                    addresses = result
                    searchResults = result
                },
                onError = { error ->
                    Toast.makeText(this@GuestAddressManagementActivity, error, Toast.LENGTH_LONG).show()
                }
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ที่จัดส่ง Guest",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                Row {
                    Button(
                        onClick = { showSearch = !showSearch },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF457E1E))
                    ) {
                        Text("ค้นหา", color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            loadAllAddresses(
                                onSuccess = { result ->
                                    addresses = result
                                    searchResults = result
                                    Toast.makeText(this@GuestAddressManagementActivity, "รีเฟรชข้อมูลสำเร็จ", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    Toast.makeText(this@GuestAddressManagementActivity, error, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("รีเฟรช", color = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar
            if (showSearch) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { 
                        searchText = it
                        if (it.isBlank()) {
                            searchResults = addresses
                        } else {
                            searchResults = addresses.filter { address ->
                                val guestId = address.get("guest_id")?.asString ?: ""
                                val addressText = address.get("address")?.asString ?: ""
                                val androidId = address.get("android_id")?.asString ?: ""
                                
                                guestId.contains(it, ignoreCase = true) ||
                                addressText.contains(it, ignoreCase = true) ||
                                androidId.contains(it, ignoreCase = true)
                            }
                        }
                    },
                    label = { Text("ค้นหาตาม Guest ID, ที่อยู่, หรือ Android ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "สถิติ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ที่อยู่ทั้งหมด: ${searchResults.size} รายการ",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "Guest ที่ไม่ซ้ำ: ${searchResults.map { it.get("guest_id")?.asString }.distinct().size} คน",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Loading Indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Address List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { address ->
                    AddressCard(address = address)
                }
            }
        }
    }
    
    @Composable
    fun AddressCard(address: JsonObject) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Guest ID
                Text(
                    text = "Guest ID: ${address.get("guest_id")?.asString ?: "N/A"}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Address
                Text(
                    text = "ที่อยู่: ${address.get("address")?.asString ?: "N/A"}",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Android ID
                Text(
                    text = "Android ID: ${address.get("android_id")?.asString ?: "N/A"}",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
                
                // Device Info
                val deviceInfo = address.get("device_info")?.asString
                if (!deviceInfo.isNullOrBlank()) {
                    Text(
                        text = "อุปกรณ์: $deviceInfo",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Created At
                val createdAt = address.get("server_created_at")?.asString ?: 
                               address.get("address_created_at_formatted")?.asString
                if (!createdAt.isNullOrBlank()) {
                    Text(
                        text = "วันที่บันทึก: $createdAt",
                        fontSize = 12.sp,
                        color = Color(0xFF457E1E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    
    private fun loadAllAddresses(
        onSuccess: (List<JsonObject>) -> Unit,
        onError: (String) -> Unit
    ) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val call = apiService.getGuestAddresses()
        
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    Log.d("GuestAddress", "Response: $responseBody")
                    if (responseBody.get("success")?.asBoolean == true) {
                        val dataArray = responseBody.get("data")?.asJsonArray
                        if (dataArray != null) {
                            val addressList = mutableListOf<JsonObject>()
                            for (item in dataArray) {
                                addressList.add(item.asJsonObject)
                            }
                            onSuccess(addressList)
                        } else {
                            onError("ไม่พบข้อมูลที่อยู่")
                        }
                    } else {
                        val message = responseBody.get("message")?.asString ?: "ข้อผิดพลาดที่ไม่ทราบสาเหตุ"
                        onError(message)
                    }
                } else {
                    onError("HTTP Error: ${response.code()}")
                }
            }
            
            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                onError("Network Error: ${t.message}")
                Log.e("GuestAddressActivity", "API call failed", t)
            }
        })
    }
}
