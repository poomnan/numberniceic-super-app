package com.numberniceic.ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.numberniceic.data.admin.ServerMessage
import com.numberniceic.data.tabian.TabianSellItem
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminTabianActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AdminTabianScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTabianScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tabianList by remember { mutableStateOf<List<TabianSellItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<TabianSellItem?>(null) } // null = new item

    fun refreshData() {
        isLoading = true
        val api = RetrofitClient.instance.create(ApiService::class.java)
        api.getTabianSellList().enqueue(object : Callback<List<TabianSellItem>> {
            override fun onResponse(call: Call<List<TabianSellItem>>, response: Response<List<TabianSellItem>>) {
                if (response.isSuccessful) {
                    tabianList = response.body() ?: emptyList()
                    // Sort by ID desc
                    tabianList = tabianList.sortedByDescending { it.tabianId }
                } else {
                    Toast.makeText(context, "Cloud Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
                isLoading = false
            }

            override fun onFailure(call: Call<List<TabianSellItem>>, t: Throwable) {
                Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        })
    }
    
    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("จัดการทะเบียนรถ", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF283593)),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedItem = null
                    showDialog = true
                },
                containerColor = Color(0xFF283593),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F5F5))) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabianList) { item ->
                        TabianAdminItem(item) {
                            selectedItem = item
                            showDialog = true
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        TabianEditDialog(
            item = selectedItem,
            onDismiss = { showDialog = false },
            onSave = { 
                showDialog = false
                refreshData() 
            },
            onDelete = {
                showDialog = false
                refreshData()
            }
        )
    }
}

@Composable
fun TabianAdminItem(item: TabianSellItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.tabianNumber ?: "-",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(text = "${item.tabianProvince} | ${item.tabianPrice ?: 0} บาท", color = Color.Gray)
                
                if (!item.tabianStatus.isNullOrEmpty() && item.tabianStatus != "available") {
                     Text(text = "สถานะ: ${item.tabianStatus}", color = Color.Red, fontSize = 12.sp)
                }
            }
            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun TabianEditDialog(
    item: TabianSellItem?,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var number by remember { mutableStateOf(item?.tabianNumber ?: "") }
    var province by remember { mutableStateOf(item?.tabianProvince ?: "กรุงเทพมหานคร") }
    var price by remember { mutableStateOf(item?.tabianPrice?.toString() ?: "") }
    var status by remember { mutableStateOf(item?.tabianStatus ?: "available") }
    var category by remember { mutableStateOf(item?.tabianCategory ?: "") }
    
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "เพิ่มทะเบียนรถ" else "แก้ไขทะเบียนรถ") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("เลขทะเบียน (เช่น 1กท 9999)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = province,
                    onValueChange = { province = it },
                    label = { Text("จังหวัด") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("ราคา") },
                    singleLine = true
                )
                 OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("สถานะ (available/sold)") },
                     singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (number.isBlank()) return@Button
                    isSaving = true
                    val json = JsonObject()
                    if (item?.tabianId != null) {
                        json.addProperty("id", item.tabianId)
                    }
                    json.addProperty("tabian_number", number)
                    json.addProperty("tabian_province", province)
                    json.addProperty("tabian_price", price.toIntOrNull() ?: 0)
                    json.addProperty("tabian_status", status)
                    json.addProperty("tabian_category", category)

                    val api = RetrofitClient.instance.create(ApiService::class.java)
                    api.saveTabian(json).enqueue(object : Callback<ServerMessage> {
                        override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                            isSaving = false
                            if (response.isSuccessful) {
                                Toast.makeText(context, "บันทึกข้อมูลเรียบร้อย", Toast.LENGTH_SHORT).show()
                                onSave()
                            } else {
                                Toast.makeText(context, "Save Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onFailure(call: Call<ServerMessage>, t: Throwable) {
                            isSaving = false
                            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Booking..." else "บันทึก")
            }
        },
        dismissButton = {
            if (item != null) {
                TextButton(
                    onClick = {
                        val json = JsonObject()
                        json.addProperty("id", item.tabianId)
                         val api = RetrofitClient.instance.create(ApiService::class.java)
                         api.deleteTabian(json).enqueue(object : Callback<ServerMessage> {
                             override fun onResponse(call: Call<ServerMessage>, response: Response<ServerMessage>) {
                                 if (response.isSuccessful) {
                                     Toast.makeText(context, "ลบข้อมูลเรียบร้อย", Toast.LENGTH_SHORT).show()
                                     onDelete()
                                 }
                             }
                             override fun onFailure(call: Call<ServerMessage>, t: Throwable) {}
                         })
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("ลบ")
                }
            }
            TextButton(onClick = onDismiss) {
                Text("ยกเลิก")
            }
        }
    )
}
