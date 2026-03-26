package com.numberniceic.ui.screens

import android.widget.Toast
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.numberniceic.R
import com.numberniceic.data.product.Product
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.phone.PhoneBuyAllF
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import retrofit2.awaitResponse
import android.util.Log
import com.numberniceic.ui.theme.Kanit

// Helper Color if Orange is missing
val Orange = Color(0xFFFFA500)

data class CartUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTabIndex: Int = 0,
    val showPaymentPanel: Boolean = false,
    val isFetchingQR: Boolean = false,
    val selectedProduct: Product? = null,
    val qrCodeUrl: String? = null,
    val paymentRefNo: String? = null,
    val paymentStatus: String = "PENDING"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    onBack: () -> Unit,
    initialTabIndex: Int = 0
) {
    val context = LocalContext.current
    val safeInitialTabIndex = initialTabIndex.coerceIn(0, 3)
    var uiState by remember { mutableStateOf(CartUiState(selectedTabIndex = safeInitialTabIndex)) }

@Composable
fun PaymentProductDetailCard(
    product: Product,
    qrCodeBase64: String?,
    isFetchingQR: Boolean
) {
    val formattedPrice = java.text.DecimalFormat("#,###").format(product.price)
    val descriptionText = product.description?.takeIf { it.isNotBlank() } ?: "-"
    val qrBitmap = remember(qrCodeBase64) {
        try {
            val base64String = qrCodeBase64?.substringAfter("base64,") ?: ""
            if (base64String.isBlank()) {
                null
            } else {
                val bytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            Log.e("CartScreen", "Error decoding QR in product card: ${e.message}")
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_shop_white),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.Black
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = "รายละเอียดสินค้า",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF457E1E)
            )
            Text(
                text = descriptionText,
                fontSize = 15.sp,
                color = Color(0xFF555555),
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE6E6E6))
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isFetchingQR -> CircularProgressIndicator(color = Color(0xFF457E1E), strokeWidth = 2.dp)
                        qrBitmap != null -> Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Payment QR",
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )

                        else -> Text("QR Code", color = Color(0xFF9A9A9A), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "ราคา: $formattedPrice บาท",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun LegacyPhoneBuyAllFragmentHost() {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val containerId = remember { View.generateViewId() }
    val fragmentTag = "CartPhoneBuyAllF"

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = containerId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = {
            if (activity != null) {
                val fm = activity.supportFragmentManager
                val existing = fm.findFragmentByTag(fragmentTag)
                if (existing == null || existing.id != containerId) {
                    fm.beginTransaction().apply {
                        if (existing != null && existing.id != containerId) {
                            remove(existing)
                        }
                        replace(containerId, PhoneBuyAllF(), fragmentTag)
                    }.commitAllowingStateLoss()
                }
            }
        }
    )
}
    val scope = rememberCoroutineScope()
    val tabs = listOf("เพทาย", "เบอร์โทร", "ทะเบียนรถ", "หนังสือ")

    LaunchedEffect(Unit) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getProducts().enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                if (response.isSuccessful && response.body() != null) {
                    uiState = uiState.copy(products = response.body()!!, isLoading = false)
                } else {
                    uiState = uiState.copy(isLoading = false)
                }
            }
            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                uiState = uiState.copy(isLoading = false)
            }
        })
    }


    var showAddressPrompt by remember { mutableStateOf(false) }
    var showGuestAddressDialog by remember { mutableStateOf(false) }
    var showMemberAddressChoice by remember { mutableStateOf(false) }
    var lastPaidRefNo by remember { mutableStateOf("") }
    var lastPaidProduct by remember { mutableStateOf<Product?>(null) }
    var addressInput by remember { mutableStateOf("") }
    var isUpdatingAddress by remember { mutableStateOf(false) }

    fun showGuestAddressDialog() {
        showGuestAddressDialog = true
    }

    fun updateShippingAddress(newAddress: String) {
        val userx = com.numberniceic.utils.UserContextManager.userX(context) ?: return
        val mid = userx.userId ?: return
        
        scope.launch {
            isUpdatingAddress = true
            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            val body = com.google.gson.JsonObject().apply {
                addProperty("memberid", mid)
                addProperty("address", newAddress)
                addProperty("realname", userx.realName ?: "")
                addProperty("surname", userx.surname ?: "")
                addProperty("avatar", userx.avatar ?: "10")
                addProperty("shour", userx.sHour)
                addProperty("sminute", userx.sMinute)
                addProperty("sprovince", userx.sProvince ?: "")
                addProperty("sgender", userx.sGender ?: "")
                
                val bday = userx.birthDay ?: "1990-01-01"
                val parts = bday.split("-")
                if (parts.size == 3) {
                    addProperty("syear", parts[0])
                    addProperty("smonth", parts[1])
                    addProperty("sday", parts[2])
                }
            }

            try {
                val response = apiService.userUpdateData(body).awaitResponse()
                if (response.isSuccessful && response.body() != null) {
                    val serverx = response.body()!!
                    if (serverx.serverx?.message == "success") {
                        // Update local cache
                        val updatedUserJson = com.google.gson.Gson().toJson(serverx.userx)
                        context.getSharedPreferences("userdata", android.content.Context.MODE_PRIVATE)
                            .edit().putString("json", updatedUserJson).apply()
                        
                        // Also update shipping_address in guest_orders for the last paid order
                        if (lastPaidRefNo.isNotBlank()) {
                            val orderUpdateBody = com.google.gson.JsonObject().apply {
                                addProperty("order_id", lastPaidRefNo)
                                addProperty("shipping_address", newAddress)
                            }
                            apiService.updateOrderAddress(orderUpdateBody).enqueue(object : Callback<com.google.gson.JsonObject> {
                                override fun onResponse(call: Call<com.google.gson.JsonObject>, response: Response<com.google.gson.JsonObject>) {
                                    Log.d("CartScreen", "Order address updated for: $lastPaidRefNo")
                                }
                                override fun onFailure(call: Call<com.google.gson.JsonObject>, t: Throwable) {
                                    Log.e("CartScreen", "Failed to update order address: ${t.message}")
                                }
                            })
                        }
                        
                        Toast.makeText(context, "บันทึกที่อยู่จัดส่งเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                        showAddressPrompt = false
                    } else {
                        Toast.makeText(context, "บันทึกไม่สำเร็จ: ${serverx.serverx?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CartScreen", "Update address error: ${e.message}")
                Toast.makeText(context, "เกิดข้อผิดพลาดในการบันทึกที่อยู่", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdatingAddress = false
            }
        }
    }

    fun startPollingPaymentStatus(refNo: String, product: Product) {
        scope.launch {
            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            var isPaid = false
            var attempts = 0
            while (!isPaid && attempts < 60) { // 5 minutes max
                delay(5000)
                attempts++
                try {
                    val response = apiService.getPaymentStatus(refNo).awaitResponse()
                    if (response.isSuccessful && response.body() != null) {
                        val status = response.body()!!.get("status")?.asString
                        if (status == "paid") {
                            isPaid = true
                            uiState = uiState.copy(paymentStatus = "SUCCESS", showPaymentPanel = false)
                            Toast.makeText(context, "การสั่งซื้อสำเร็จ! เรากำลังเตรียมการจัดส่ง", Toast.LENGTH_LONG).show()
                            
                            lastPaidRefNo = refNo
                            lastPaidProduct = product
                            val userx = com.numberniceic.utils.UserContextManager.userX(context)
                            val guestManager = com.numberniceic.utils.GuestManager(context)
                            
                            // Save order for both guest and member
                            val orderData = com.google.gson.JsonObject().apply {
                                addProperty("guest_id", guestManager.getGuestId() ?: "")
                                if (userx != null) addProperty("member_id", userx.userId?.toIntOrNull() ?: 0)
                                addProperty("order_id", refNo)
                                addProperty("order_data", com.google.gson.JsonObject().apply {
                                    addProperty("product_id", product.id)
                                    addProperty("product_name", product.name)
                                    addProperty("product_price", product.price)
                                    addProperty("category", product.categoryName ?: "")
                                    addProperty("ref_no", refNo)
                                    addProperty("payment_status", "paid")
                                }.toString())
                                val memberAddr = userx?.shippingAddress?.takeIf { it.isNotBlank() } ?: userx?.address?.takeIf { it.isNotBlank() }
                                if (userx != null && !memberAddr.isNullOrEmpty()) {
                                    addProperty("shipping_address", memberAddr)
                                }
                                addProperty("created_at", System.currentTimeMillis())
                            }
                            apiService.saveGuestOrder(orderData).enqueue(object : Callback<com.google.gson.JsonObject> {
                                override fun onResponse(call: Call<com.google.gson.JsonObject>, response: Response<com.google.gson.JsonObject>) {
                                    Log.d("CartScreen", "Order saved: $refNo product: ${product.name}")
                                }
                                override fun onFailure(call: Call<com.google.gson.JsonObject>, t: Throwable) {
                                    Log.e("CartScreen", "Failed to save order: ${t.message}")
                                }
                            })
                            
                            // Address flow
                            if (userx != null) {
                                val existingAddr = userx.shippingAddress?.takeIf { it.isNotBlank() } ?: userx.address?.takeIf { it.isNotBlank() }
                                if (!existingAddr.isNullOrEmpty()) {
                                    showMemberAddressChoice = true
                                } else {
                                    // Member ไม่มีที่อยู่ - ให้กรอกใหม่
                                    showAddressPrompt = true
                                }
                            } else {
                                // Guest user - ถามที่อยู่
                                showGuestAddressDialog()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CartScreen", "Polling error: ${e.message}")
                }
            }
        }
    }

    fun startPayment(product: Product) {
        uiState = uiState.copy(selectedProduct = product, showPaymentPanel = true, isFetchingQR = true, qrCodeUrl = null, paymentRefNo = null)
        
        val userx = com.numberniceic.utils.UserContextManager.userX(context)
        val chatPrefs = context.getSharedPreferences("chat_prefs", android.content.Context.MODE_PRIVATE)
        val chatSessionId = chatPrefs.getString("session_id", "")
        val androidDeviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_guest"
        val finalGuestId = if (!chatSessionId.isNullOrEmpty()) chatSessionId else androidDeviceId
        val fcmToken = context.getSharedPreferences("fcm_prefs", android.content.Context.MODE_PRIVATE).getString("fcm_token", "")

        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val body = com.google.gson.JsonObject().apply {
            addProperty("product_id", product.id)
            addProperty("product_detail", product.name)
            addProperty("amount", product.price)
            addProperty("device_id", androidDeviceId)
            if (!fcmToken.isNullOrEmpty()) {
                addProperty("fcm_token", fcmToken)
            }
            
            val uid = userx?.userId
            if (!uid.isNullOrEmpty()) {
                addProperty("user_id", uid.toIntOrNull() ?: 0)
            } else {
                addProperty("guest_id", finalGuestId)
            }
        }
        
        apiService.createPaymentQR(body).enqueue(object : Callback<com.google.gson.JsonObject> {
            override fun onResponse(call: Call<com.google.gson.JsonObject>, response: Response<com.google.gson.JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val qr = data.get("qr_base64")?.asString
                    val ref = data.get("ref_no")?.asString
                    uiState = uiState.copy(
                        isFetchingQR = false,
                        qrCodeUrl = qr,
                        paymentRefNo = ref
                    )
                    if (ref != null) startPollingPaymentStatus(ref, product)
                } else {
                    uiState = uiState.copy(isFetchingQR = false, showPaymentPanel = false)
                    Toast.makeText(context, "ไม่สามารถสร้าง QR Code ได้", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<com.google.gson.JsonObject>, t: Throwable) {
                uiState = uiState.copy(isFetchingQR = false, showPaymentPanel = false)
                Toast.makeText(context, "การเชื่อมต่อล้มเหลว", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("สินค้าทั้งหมด", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF457E1E))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Category Tabs
            TabRow(
                selectedTabIndex = uiState.selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFF457E1E),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTabIndex]),
                        color = Color(0xFF457E1E)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTabIndex == index,
                        onClick = { uiState = uiState.copy(selectedTabIndex = index) },
                        text = { 
                            Text(
                                title, 
                                fontSize = 14.sp, 
                                fontWeight = if (uiState.selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.selectedTabIndex == index) Color(0xFF457E1E) else Color.Gray,
                                fontFamily = Kanit
                            ) 
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF457E1E))
                } else {
                    when (uiState.selectedTabIndex) {
                        1 -> LegacyPhoneBuyAllFragmentHost()
                        2 -> LicensePlateScreen(
                            navController = navController, 
                            showInputSection = false, 
                            showViewAllButton = false,
                            showContactSectionAtBottom = true
                        )
                        3 -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ยังไม่มีสินค้า", color = Color.Gray)
                        }
                        else -> {
                            val filteredProducts = uiState.products.filter { product ->
                                val pName = product.name ?: ""
                                val cName = product.categoryName ?: ""
                                when (uiState.selectedTabIndex) {
                                    0 -> pName.contains("พลอย") || cName.contains("พลอย")
                                    else -> true
                                }
                            }

                            if (filteredProducts.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("ไม่มีข้อมูลสินค้าในขณะนี้", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredProducts) { product ->
                                        ProductItemCard(product) {
                                            startPayment(product)
                                        }
                                    }
                                    
                                    item {
                                        ZirconArticleCard()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showPaymentPanel && uiState.selectedProduct != null) {
        val selected = uiState.selectedProduct!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        ModalBottomSheet(
            onDismissRequest = { uiState = uiState.copy(showPaymentPanel = false) },
            containerColor = Color.White,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ชำระเงินผ่าน QR Code (PromptPay)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(Modifier.height(16.dp))
                
                PaymentProductDetailCard(
                    product = selected,
                    qrCodeBase64 = uiState.qrCodeUrl,
                    isFetchingQR = uiState.isFetchingQR
                )

                Spacer(Modifier.height(20.dp))

                if (!uiState.isFetchingQR && uiState.qrCodeUrl.isNullOrEmpty()) {
                    Text("ไม่พบข้อมูล QR Code", color = Color.Gray)
                }

                Text("Ref: ${uiState.paymentRefNo}", fontSize = 14.sp, color = Color.Gray)
                Text("สถานะ: ${uiState.paymentStatus}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (uiState.paymentStatus == "SUCCESS") Color(0xFF2E7D32) else Orange)
                
                Spacer(Modifier.height(40.dp))
                
                Button(
                    onClick = { uiState = uiState.copy(showPaymentPanel = false) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF457E1E))
                ) {
                    Text("ปิดหน้าต่าง")
                }
            }
        }
    }

    if (showAddressPrompt) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismiss without choice or cancel */ },
            title = { Text("กรุณาระบุที่อยู่จัดส่ง", fontFamily = Kanit, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("คุณได้สั่งซื้อสินค้าเพทาย กรุณาระบุที่อยู่สำหรับจัดส่งสินค้าเพื่อให้เจ้าหน้าทีดำเนินการฝากและจัดส่งให้ท่าน", fontFamily = Kanit, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("ที่อยู่จัดส่ง", fontFamily = Kanit) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ระบุจังหวัด อำเภอ เขต รหัสไปรษณีย์...", fontSize = 16.sp, fontFamily = Kanit) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if (addressInput.isNotBlank()) {
                            updateShippingAddress(addressInput)
                        } else {
                            Toast.makeText(context, "กรุณากรอกที่อยู่", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF457E1E)),
                    enabled = !isUpdatingAddress
                ) {
                    if (isUpdatingAddress) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("บันทึกที่อยู่", fontFamily = Kanit)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddressPrompt = false }) {
                    Text("ภายหลัง", color = Color.Gray, fontFamily = Kanit)
                }
            }
        )
    }
    
    // Guest Address Dialog
    if (showGuestAddressDialog) {
        val guestManager = com.numberniceic.utils.GuestManager(context)
        
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showGuestAddressDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "กรุณาระบุที่อยู่จัดส่ง",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Kanit,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "คุณได้สั่งซื้อสินค้าเพทาย กรุณาระบุที่อยู่สำหรับจัดส่งสินค้าเพื่อให้เจ้าหน้าทีดำเนินการฝากและจัดส่งให้ท่าน",
                        fontSize = 16.sp,
                        fontFamily = Kanit,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("ที่อยู่จัดส่ง", fontFamily = Kanit) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ระบุจังหวัด อำเภอ เขต รหัสไปรษณีย์...", fontSize = 14.sp, fontFamily = Kanit) }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                showGuestAddressDialog = false
                                Toast.makeText(context, "กรุณาลงทะเบียนเพื่อระบุที่อยู่จัดส่ง มิฉะนั้นสินค้าจะจัดส่งไม่ได้", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ลงทะเบียน", fontFamily = Kanit, color = Color(0xFFE65100))
                        }
                        
                        Button(
                            onClick = { 
                                if (addressInput.isNotBlank()) {
                                    // บันทึกที่อยู่สำหรับ guest
                                    guestManager.saveTemporaryAddress(addressInput)
                                    
                                    // ส่งไปยัง server
                                    val addressData = com.google.gson.JsonObject().apply {
                                        addProperty("guest_id", guestManager.getGuestId())
                                        addProperty("address", addressInput)
                                        addProperty("created_at", System.currentTimeMillis())
                                    }
                                    
                                    scope.launch {
                                        try {
                                            val apiService = RetrofitClient.instance.create(ApiService::class.java)
                                            val response = apiService.saveGuestAddress(addressData)
                                            
                                            response.enqueue(object : retrofit2.Callback<com.google.gson.JsonObject> {
                                                override fun onResponse(
                                                    call: retrofit2.Call<com.google.gson.JsonObject>,
                                                    response: retrofit2.Response<com.google.gson.JsonObject>
                                                ) {
                                                    if (response.isSuccessful && response.body() != null) {
                                                        val serverResponse = response.body()!!
                                                        if (serverResponse.get("success")?.asBoolean == true) {
                                                            Toast.makeText(context, "บันทึกที่อยู่สำเร็จ!", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(context, "บันทึกไม่สำเร็จ: ${serverResponse.get("message")}", Toast.LENGTH_LONG).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "เกิดข้อผิดพลาดในการบันทึก", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                                
                                                override fun onFailure(
                                                    call: retrofit2.Call<com.google.gson.JsonObject>,
                                                    t: Throwable
                                                ) {
                                                    Toast.makeText(context, "เกิดข้อผิดพลาด: ${t.message}", Toast.LENGTH_LONG).show()
                                                }
                                            })
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    
                                    showGuestAddressDialog = false
                                } else {
                                    Toast.makeText(context, "กรุณากรอกที่อยู่", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF457E1E))
                        ) {
                            Text("บันทึกที่อยู่", fontFamily = Kanit, color = Color.White)
                        }
                    }
                }
            }
        }
    }
    
    // Member Address Choice Dialog - ถามว่าจะใช้ที่อยู่จากระบบหรือกรอกใหม่
    if (showMemberAddressChoice) {
        val userx = com.numberniceic.utils.UserContextManager.userX(context)
        val existingAddress = userx?.shippingAddress?.takeIf { it.isNotBlank() } ?: userx?.address?.takeIf { it.isNotBlank() } ?: ""
        
        AlertDialog(
            onDismissRequest = { showMemberAddressChoice = false },
            title = { Text("เลือกที่อยู่จัดส่ง", fontFamily = Kanit, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("ที่อยู่ปัจจุบันในระบบ:", fontFamily = Kanit, fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = existingAddress,
                            modifier = Modifier.padding(12.dp),
                            fontFamily = Kanit,
                            fontSize = 14.sp,
                            color = Color(0xFF1B5E20)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("ต้องการใช้ที่อยู่นี้จัดส่งสินค้าหรือกรอกที่อยู่ใหม่?", fontFamily = Kanit, fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showMemberAddressChoice = false
                        // Update guest_orders with existing address
                        if (lastPaidRefNo.isNotBlank() && existingAddress.isNotBlank()) {
                            val apiService = RetrofitClient.instance.create(ApiService::class.java)
                            val body = com.google.gson.JsonObject().apply {
                                addProperty("order_id", lastPaidRefNo)
                                addProperty("shipping_address", existingAddress)
                            }
                            apiService.updateOrderAddress(body).enqueue(object : Callback<com.google.gson.JsonObject> {
                                override fun onResponse(call: Call<com.google.gson.JsonObject>, response: Response<com.google.gson.JsonObject>) {
                                    Log.d("CartScreen", "Order address confirmed for: $lastPaidRefNo")
                                }
                                override fun onFailure(call: Call<com.google.gson.JsonObject>, t: Throwable) {}
                            })
                        }
                        Toast.makeText(context, "ใช้ที่อยู่ในระบบสำหรับจัดส่ง", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF457E1E))
                ) {
                    Text("ใช้ที่อยู่นี้", fontFamily = Kanit)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { 
                    showMemberAddressChoice = false
                    showAddressPrompt = true
                }) {
                    Text("กรอกที่อยู่ใหม่", fontFamily = Kanit, color = Color(0xFFE65100))
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = product.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_shop_white),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                product.description?.let {
                    Text(it, fontSize = 12.sp, color = Color.Gray, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                val formattedPrice = java.text.DecimalFormat("#,###").format(product.price)
                Text("ราคา: $formattedPrice บาท", fontWeight = FontWeight.Medium, color = Color.Black, fontSize = 14.sp)
            }
            
            IconButton(onClick = { onClick() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_cart_white),
                    contentDescription = "Buy",
                    tint = Color(0xFF457E1E)
                )
            }
        }
    }
}

@Composable
fun ZirconArticleCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF7)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFFF9C4), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_vip02),
                        contentDescription = null,
                        tint = Color(0xFFFBC02D),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "พลอยเพทายแท้",
                    fontFamily = Kanit,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4527A0)
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFFFF), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "ความเชื่อ :",
                    fontFamily = Kanit,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE64A19)
                )
                Text(
                    text = "คนโบราณเชื่อว่าจะช่วยนำความมั่งคั่งด้านการเงินการงาน และนำ เกียรติยศ ความเจริญรุ่งเรืองมาสู่ผู้สวมใส่พกพา จึงนิยมใส่ไว้ในกระเป๋าเงิน หรือทำเป็นแหวนเป็นกำไลสวมใส่",
                    fontFamily = Kanit,
                    fontSize = 14.sp,
                    color = Color(0xFF424242),
                    lineHeight = 22.sp
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "อีกทั้งยังเชื่อว่า จะช่วยเสริมดวงความรักให้มั่นคงได้ด้วย และคนโบราณยังเชื่อว่าเพทายเป็นเครื่องรางคุ้มครองป้องกันอันตราย โดยเฉพาะคุ้มครองแบ่งเบาจากคดีความ และภัยจากการเดินทางได้ และยังช่วยเสริมสร้างสติปัญญาให้ฉลาด มีสมาธิ และเชื่อว่าช่วยบรรเทาอาการเจ็บป่วยหรืออาการนอนไม่หลับให้ดีขึ้นได้",
                    fontFamily = Kanit,
                    fontSize = 14.sp,
                    color = Color(0xFF424242),
                    lineHeight = 22.sp
                )
            }
        }
    }
}

