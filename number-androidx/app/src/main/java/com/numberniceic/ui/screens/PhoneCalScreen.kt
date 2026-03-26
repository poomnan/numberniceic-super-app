package com.numberniceic.ui.screens

import android.content.Intent
import android.net.Uri
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import com.google.gson.JsonElement
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.PhoneCollectionDao
import com.numberniceic.data.repos.PhoneRepository
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.phone.PhoneBuyAllAct
import com.numberniceic.ui.phone.PhoneMiraActivity
import com.numberniceic.utils.PhoneContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import android.util.Log
import retrofit2.awaitResponse

private val numberFormat = NumberFormat.getInstance()
private val phoneCache = mutableMapOf<String, PhoneCollectionDao>()

// Data class to hold UI state (Replacement for PhoneObs)
data class PhoneCalUiState(
    val isLoading: Boolean = false,
    val phoneNumber: String = "",
    val scoreD: String = "0",
    val scoreR: String = "0",
    val percentD: String = "0",
    val percentR: String = "0",
    val pairSum: String = "",
    val txtPairSum: String = "",
    val txtPairLast: String = "",
    val txtPairCon: String = "",
    val gradePairsA: String = "?",
    val gradePairsB: String = "?",
    val gradePairSum: String = "?",
    
    // Pairs A
    val pairAp1: String = "", val pairAp1Bg: Color = Color.Transparent,
    val pairAp2: String = "", val pairAp2Bg: Color = Color.Transparent,
    val pairAp3: String = "", val pairAp3Bg: Color = Color.Transparent,
    val pairAp4: String = "", val pairAp4Bg: Color = Color.Transparent,
    val pairAp5: String = "", val pairAp5Bg: Color = Color.Transparent,
    
    // Pairs B
    val pairBp1: String = "", val pairBp1Bg: Color = Color.Transparent,
    val pairBp2: String = "", val pairBp2Bg: Color = Color.Transparent,
    val pairBp3: String = "", val pairBp3Bg: Color = Color.Transparent,
    val pairBp4: String = "", val pairBp4Bg: Color = Color.Transparent,
    
    // Pair Sum
    val pairSumBg: Color = Color.Transparent,
    
    val imgReportD: Int = 0,
    val imgReportR: Int = 0,
    val txtReportD: String = "",
    val txtReportR: String = "",
    
    // Payment State
    val showPaymentPanel: Boolean = false,
    val qrCodeBase64: String? = null,
    val paymentRefNo: String? = null,
    val isFetchingQR: Boolean = false,
    val paymentStatus: String = "pending", // pending, paid
    
    val dao: PhoneCollectionDao? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneCalScreen(
    initialPhoneNumber: String,
    initialPrice: String = "0",
    onBackClick: () -> Unit
) {
    Log.d("PhoneCalPerf", "Screen Composing Start")
    val context = LocalContext.current
    var uiState by remember { mutableStateOf(PhoneCalUiState(phoneNumber = initialPhoneNumber, isLoading = true)) }
    
    val scope = rememberCoroutineScope()

    // Optimized Data Processing
    fun updateUiState(dao: PhoneCollectionDao) {
        Log.d("PhoneCalPerf", "Processing Start")
        scope.launch(Dispatchers.Default) {
             try {
                PhoneRepository.addPhoneData(dao) // Update Repo
                
                // 1. Pre-calculate Color Cache (O(N))
                val colorCache = HashMap<String, Color>()
                dao.dataSortByType?.forEach { pairMiracle ->
                    val pairNum = pairMiracle.number
                    if (pairNum != null) {
                        val color = if (pairMiracle.type?.startsWith("D") == true) Color(0xFF3A9900)
                        else when (pairMiracle.type) {
                            "R10" -> Color(0xFFD13124)
                            "R7" -> Color(0xFFFF5722)
                            "R5" -> Color(0xFFFF9800)
                            else -> Color.Transparent
                        }
                        colorCache[pairNum] = color
                    }
                }
                
                Log.d("PhoneCalPerf", "Cache Built")

                // Helper to get color from cache (O(1))
                fun getColor(pair: String): Color = colorCache[pair] ?: Color.Transparent

                // 2. Helper to get grade/text logic
                val scoreD = numberFormat.format(PhoneRepository.getScoreD()).toString()
                val scoreR = numberFormat.format(PhoneRepository.getScoreR()).toString()
                
                // Pair Sum Grade
                var gradePairSum = "?"
                val pairSumVal = PhoneRepository.getPairSum()
                
                dao.dataSortByType?.forEach { pair ->
                    val typeSpecial = if(pair.number=="60") "R4" else "x"
                    if (typeSpecial == "R4") gradePairSum = "พอใช้"
                    else if (pair.number == pairSumVal) {
                        gradePairSum = when (pair.type) {
                            "D10" -> "ดีเยี่ยม"
                            "D8" -> "ดีมาก"
                            "D5" -> "ดี"
                            "R10" -> "อันตรายมาก"
                            "R7" -> "อันตราย"
                            "R5" -> "อันตราย"
                            else -> "?"
                        }
                    }
                }

                // Grade A & B
                val gradePairsA = when(dao.scoreByContinueCountPairsA?.pairContinueD){
                    1 -> "เสี่ยงมาก"
                    2 -> "เสี่ยง"
                    3 -> "พอใช้"
                    4 -> "ดี"
                    5 -> "ดีเยี่ยม"
                    else -> when(dao.scoreByContinueCountPairsA?.pairContinueR){
                        1 -> "เสี่ยงมาก" 2 -> "อันตราย" 3 -> "อันตราย" 4 -> "อันตรายมาก" 5 -> "อันตรายมาก" else -> "?"
                    }
                }

                val gradePairsB = when(dao.scoreByContinueCountPairsB?.pairContinueD){
                    1 -> "เสี่ยง" 2 -> "พอใช้" 3 -> "ดี" 4 -> "ดีเยี่ยม"
                    else -> when(dao.scoreByContinueCountPairsB?.pairContinueR){
                        1 -> "พอใช้" 2 -> "อันตราย" 3 -> "อันตรายมาก" 4 -> "อันตรายมาก" else -> "?"
                    }
                }
                
                // Text Reports
                val txtPairLast = when (dao.specialGoal?.specialPairLast) {
                    "A" -> "ดีเยี่ยม" "B" -> "ดีมาก" "C" -> "ดี" "D" -> "พอใช้" else -> "อันตราย"
                }
                
                val txtPairSum = when (dao.specialGoal?.specialPairSum) {
                    "A" -> "ดีเยี่ยม" "B" -> "ดีมาก" "C" -> "ดี" "D" -> "พอใช้" else -> "อันตราย"
                }
                
                val txtPairCon = when {
                    (dao.scoreByContinueCountPairsA?.pairContinueD ?: 0) >= 3 && (dao.scoreByContinueCountPairsB?.pairContinueD ?: 0) >= 2 -> "ดีเยี่ยม"
                    (dao.scoreByContinueCountPairsA?.pairContinueD ?: 0) >= 2  && (dao.scoreByContinueCountPairsB?.pairContinueD ?: 0) >= 1 -> "ปานกลาง"
                    (dao.scoreByContinueCountPairsA?.pairContinueD ?: 0) == 0  && (dao.scoreByContinueCountPairsB?.pairContinueD ?: 0) == 0  -> "อันตราย"
                    else -> "พอใช้"
                }

                // Img Reports
                val imgReportD = when {
                    (dao.scoreTotalOfTotal?.scoreTotalD ?: 0) >= 1700 -> 9
                    (dao.scoreTotalOfTotal?.scoreTotalD ?: 0) > 1000 -> 1
                    else -> 0
                }
                val imgReportR = when {
                    (dao.scoreTotalOfTotal?.scoreTotalR ?: 0) < 0 -> 2
                    (dao.scoreTotalOfTotal?.scoreTotalR ?: 0) == 0 -> 1
                    else -> 0
                }

                // 3. Update UI on Main Thread
                withContext(Dispatchers.Main) {
                    Log.d("PhoneCalPerf", "Updating UI State")
                    uiState = uiState.copy(
                        isLoading = false,
                        scoreD = scoreD,
                        scoreR = scoreR,
                        percentD = PhoneRepository.getPercentD().toString(),
                        percentR = PhoneRepository.getPercentR().toString(),
                        pairSum = pairSumVal,
                        txtPairSum = txtPairSum,
                        txtPairLast = txtPairLast,
                        txtPairCon = txtPairCon,
                        gradePairsA = gradePairsA,
                        gradePairsB = gradePairsB,
                        gradePairSum = gradePairSum,
                        
                        pairAp1 = PhoneRepository.getPairAp1(), pairAp1Bg = getColor(PhoneRepository.getPairAp1()),
                        pairAp2 = PhoneRepository.getPairAp2(), pairAp2Bg = getColor(PhoneRepository.getPairAp2()),
                        pairAp3 = PhoneRepository.getPairAp3(), pairAp3Bg = getColor(PhoneRepository.getPairAp3()),
                        pairAp4 = PhoneRepository.getPairAp4(), pairAp4Bg = getColor(PhoneRepository.getPairAp4()),
                        pairAp5 = PhoneRepository.getPairAp5(), pairAp5Bg = getColor(PhoneRepository.getPairAp5()),
                        
                        pairBp1 = PhoneRepository.getPairBp1(), pairBp1Bg = getColor(PhoneRepository.getPairBp1()),
                        pairBp2 = PhoneRepository.getPairBp2(), pairBp2Bg = getColor(PhoneRepository.getPairBp2()),
                        pairBp3 = PhoneRepository.getPairBp3(), pairBp3Bg = getColor(PhoneRepository.getPairBp3()),
                        pairBp4 = PhoneRepository.getPairBp4(), pairBp4Bg = getColor(PhoneRepository.getPairBp4()),
                        
                        pairSumBg = getColor(pairSumVal),
                        imgReportD = imgReportD,
                        imgReportR = imgReportR,
                        txtReportD = dao.miracleSummary?.miracleD ?: "",
                        txtReportR = dao.miracleSummary?.miracleR ?: "",
                        dao = dao
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Log.e("PhoneCalPerf", "Error: ${e.message}")
                    uiState = uiState.copy(isLoading = false)
                    Toast.makeText(context, "Error processing data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    fun startPollingPaymentStatus(refNo: String) {
        scope.launch {
            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            var attempts = 0
            while (attempts < 60 && uiState.paymentStatus != "paid" && uiState.showPaymentPanel) {
                try {
                    val response = apiService.getPaymentStatus(refNo).awaitResponse()
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val statusStr = if (body.has("status") && !body.get("status").isJsonNull) body.get("status").asString else "pending"
                        if (statusStr == "paid") {
                            uiState = uiState.copy(paymentStatus = "paid")
                            
                            // Save order for both guest and member
                            val guestManager = com.numberniceic.utils.GuestManager(context)
                            val userx = com.numberniceic.utils.UserContextManager.userX(context)
                            val cleanedPrice = initialPrice.replace(",", "").toDoubleOrNull() ?: 0.0
                            val orderData = com.google.gson.JsonObject().apply {
                                addProperty("guest_id", guestManager.getGuestId() ?: "")
                                if (userx != null) addProperty("member_id", userx.userId?.toIntOrNull() ?: 0)
                                addProperty("order_id", refNo)
                                addProperty("order_data", com.google.gson.JsonObject().apply {
                                    addProperty("product_name", "เบอร์มงคล $initialPhoneNumber")
                                    addProperty("product_price", cleanedPrice)
                                    addProperty("category", "เบอร์โทร")
                                    addProperty("ref_no", refNo)
                                    addProperty("payment_status", "paid")
                                }.toString())
                                if (userx != null && !userx.shippingAddress.isNullOrEmpty()) {
                                    addProperty("shipping_address", userx.shippingAddress)
                                }
                                addProperty("created_at", System.currentTimeMillis())
                            }
                            apiService.saveGuestOrder(orderData).enqueue(object : Callback<com.google.gson.JsonObject> {
                                override fun onResponse(call: Call<com.google.gson.JsonObject>, response: Response<com.google.gson.JsonObject>) {
                                    Log.d("PhoneCalScreen", "Order saved: $refNo phone: $initialPhoneNumber")
                                }
                                override fun onFailure(call: Call<com.google.gson.JsonObject>, t: Throwable) {
                                    Log.e("PhoneCalScreen", "Failed to save order: ${t.message}")
                                }
                            })
                            
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PaymentPoll", "Error: ${e.message}")
                }
                delay(3000) // Poll every 3 seconds
                attempts++
            }
        }
    }

    // --- Payment Logic ---
    fun startPayment() {
        if (uiState.isFetchingQR) return
        
        val user = com.numberniceic.utils.UserContextManager.userX(context)
        val userId = user?.userId?.toIntOrNull() ?: 0
        
        uiState = uiState.copy(isFetchingQR = true, showPaymentPanel = true, paymentStatus = "pending")
        
        val cleanedPrice = initialPrice.replace(",", "").toDoubleOrNull() ?: 0.0
        
        val body = com.google.gson.JsonObject().apply {
            addProperty("user_id", userId)
            addProperty("product_id", 0)
            addProperty("amount", cleanedPrice)
            addProperty("product_detail", "ซื้อเบอร์มงคล $initialPhoneNumber")
        }
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.createPaymentQR(body).enqueue(object : Callback<com.google.gson.JsonObject> {
            override fun onResponse(call: Call<com.google.gson.JsonObject>, response: Response<com.google.gson.JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val qr = if (data.has("qr_base64") && !data.get("qr_base64").isJsonNull) data.get("qr_base64").asString else null
                    val ref = if (data.has("ref_no") && !data.get("ref_no").isJsonNull) data.get("ref_no").asString else null
                    
                    uiState = uiState.copy(
                        isFetchingQR = false,
                        qrCodeBase64 = qr,
                        paymentRefNo = ref
                    )
                    // Start Polling
                    if (ref != null) startPollingPaymentStatus(ref)
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

    LaunchedEffect(initialPhoneNumber) {
        // Check cache first
        val cachedDao = phoneCache[initialPhoneNumber]
        if (cachedDao != null) {
            Log.d("PhoneCalPerf", "Data found in cache")
            updateUiState(cachedDao)
        } else {
            // Not in cache, fetch from API
            Log.d("PhoneCalPerf", "Data not in cache, fetching from API")
            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            apiService.getPhoneDetail(initialPhoneNumber).enqueue(object : Callback<PhoneCollectionDao> {
                override fun onResponse(call: Call<PhoneCollectionDao>, response: Response<PhoneCollectionDao>) {
                    Log.d("PhoneCalPerf", "API Response Received")
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        // Store in cache
                        phoneCache[initialPhoneNumber] = body
                        updateUiState(body)
                    } else {
                         Log.e("PhoneCalPerf", "API Error")
                         uiState = uiState.copy(isLoading = false)
                         Toast.makeText(context, "Error fetching data", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<PhoneCollectionDao>, t: Throwable) {
                    Log.e("PhoneCalPerf", "API Failure: ${t.message}")
                    uiState = uiState.copy(isLoading = false)
                    Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "ถอดรหัสเบอร์ ${PhoneContextManager.getFormatPhoneNumber(initialPhoneNumber)}",
                        color = Color.White,
                        fontSize = 18.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF05581E))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFEAEBEA))
        ) {
            // 1. Header Number Display - Show Immediately
            Box(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFF8C307)).padding(9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = PhoneContextManager.getFormatPhoneNumber(initialPhoneNumber),
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF488309),
                    modifier = Modifier.background(Color.White).fillMaxWidth().padding(8.dp),
                    textAlign = TextAlign.Center
                )
            }

            // 2. Action Buttons - Show Immediately
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF61AC12)),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { 
                        if (uiState.dao != null) {
                            val intent = Intent(context, PhoneMiraActivity::class.java)
                            intent.putExtra("DAO", uiState.dao)
                            intent.putExtra("PHONENUMBER", initialPhoneNumber)
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "กรุณารอข้อมูลสักครู่...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF61AC12)),
                    shape = RoundedCornerShape(9.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Icon(painterResource(R.drawable.ic_eye_48), null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ดูคำทำนาย")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { context.startActivity(Intent(context, PhoneBuyAllAct::class.java)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF61AC12)),
                    shape = RoundedCornerShape(9.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Icon(painterResource(R.drawable.ic_clover), null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("เลือกเบอร์ดี")
                }
            }
            
            // 3. Content or Loading
            if (uiState.isLoading) {
                 Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF61AC12))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "กำลังประมวลผลข้อมูล...",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "(อาจใช้เวลาสักครู่ ขึ้นอยู่กับเครือข่าย)",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                // 3. Detail Section
                // Header Details
                Box(Modifier.fillMaxWidth().background(Color.Black).padding(6.dp)) {
                    Text("รายละเอียดการถอดคู่", color = Color(0xFFFAF19F), fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
                }
                
                // Pairs Grid
                Column(Modifier.fillMaxWidth().background(Color(0xFFECEBEB)).padding(bottom = 6.dp)) {
                    // Pair A
                    PairRow(label = "คู่หลัก :", grade = uiState.gradePairsA) {
                         PairCircle(uiState.pairAp1, uiState.pairAp1Bg)
                         PairCircle(uiState.pairAp2, uiState.pairAp2Bg)
                         PairCircle(uiState.pairAp3, uiState.pairAp3Bg)
                         PairCircle(uiState.pairAp4, uiState.pairAp4Bg)
                         PairCircle(uiState.pairAp5, uiState.pairAp5Bg)
                    }
                    
                    // Pair B
                    PairRow(label = "คู่แฝง :", grade = uiState.gradePairsB, bgColor = Color(0xFFE2E2E2)) {
                        PairCircle(uiState.pairBp1, uiState.pairBp1Bg)
                        PairCircle(uiState.pairBp2, uiState.pairBp2Bg)
                        PairCircle(uiState.pairBp3, uiState.pairBp3Bg)
                        PairCircle(uiState.pairBp4, uiState.pairBp4Bg)
                    }
                    
                    // Sum Piar
                    PairRow(label = "ผลรวม :", grade = uiState.gradePairSum, bgColor = Color(0xFFFAFAF7)) {
                        PairCircle(uiState.pairSum, uiState.pairSumBg)
                    }
                }
                
                // Summary Boxes (3 green boxes)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    val mod = Modifier.weight(1f).padding(1.dp).background(Color(0xFFFAFAF7))
                    SummaryBox(mod, "ผลรวม", uiState.txtPairSum)
                    SummaryBox(mod, "คู่ท้าย", uiState.txtPairLast)
                    SummaryBox(mod, "ดีต่อเนื่อง", uiState.txtPairCon)
                }

                 // 4. Good/Bad Summary
                 Box(Modifier.padding(top = 10.dp).fillMaxWidth().background(Color.Black).padding(8.dp)) {
                    val formattedNum = PhoneContextManager.getFormatPhoneNumber(initialPhoneNumber)
                    Text("สรุปผลดีร้ายของเบอร์นี้ $formattedNum", color = Color(0xFFFAF19F), fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
                }
                
                // Score Table
                Row(Modifier.fillMaxWidth()) {
                    // Left Side (Scores)
                    Column(Modifier.weight(1f)) {
                        Row {
                            ScoreBox(Modifier.weight(1f), "คะแนนดี", uiState.scoreD, Color(0xFF3A9900), Color(0xFF1CA90E))
                            ScoreBox(Modifier.weight(1f), "คะแนนร้าย", uiState.scoreR, Color(0xFFB43C0B), Color(0xFFFF0004))
                        }
                    }
                     // Right Side (Percents)
                    Column(Modifier.weight(1f)) {
                         Row {
                            ScoreBox(Modifier.weight(1f), "% ดี", uiState.percentD, Color(0xFF3A9900), Color(0xFF1CA90E))
                            ScoreBox(Modifier.weight(1f), "% ร้าย", uiState.percentR, Color(0xFFB43C0B), Color(0xFFFF0004))
                         }
                    }
                }

                // Prediction Summary Section matching item_header.xml
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .background(Color(0xFFF1EFEF))
                ) {
                    // Good Prediction (Clover)
                    if (uiState.imgReportD == 9) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp)
                                .background(Color.White)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_clover),
                                contentDescription = null,
                                modifier = Modifier.size(45.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = uiState.txtReportD,
                                color = Color(0xFF3FA33F),
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Bad Prediction (Devil)
                    if (uiState.imgReportR > 0) {
                        val iconRes = if (uiState.imgReportR == 2) R.drawable.ic_evil03 else R.drawable.ic_evil02
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(2.dp)
                                .background(Color.White)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(45.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = uiState.txtReportR,
                                color = Color(0xFFE70202),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                // Disclaimer Text (Single Line)
                Text(
                    "เพื่อความแม่นยำสูงสุดควรดูร่วมกับคำทำนายและเปิดดวงชะตาประกอบ",
                    color = Color(0xFFE87A13),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )

                // Contact (Add Line)
                ContactBox(
                    onClickLine = { 
                        val userId = context.getString(R.string.line_id)
                        val lineUrl = "line://ti/p/~$userId"
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lineUrl)))
                        } catch (e: Exception) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://line.me/ti/p/~$userId")))
                            } catch (e2: Exception) {
                                Toast.makeText(context, "โปรดลงแอพพลิเคชั่น LINE เพื่อติดต่อกับเรา", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onClickPay = { /* startPayment() - Disabled for now */ }
                )
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // --- Payment Bottom Sheet (Disabled for now) ---
    /*
    if (uiState.showPaymentPanel) {
        ModalBottomSheet(
            onDismissRequest = { uiState = uiState.copy(showPaymentPanel = false) },
            containerColor = Color.White
        ) {
            ...
        }
    }
    */
}

@Composable
fun PairRow(label: String, grade: String, bgColor: Color = Color.Transparent, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(bgColor).padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, width = 60.dp, fontSize = 16.sp, color = Color.Black)
        Row(Modifier.weight(1f)) {
            content()
        }
        Text(grade, color = Color(0xFFF59B25), fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PairCircle(text: String, bgColor: Color) {
    if (text.isNotEmpty()) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(36.dp)
                .background(bgColor, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun Text(text: String, width: androidx.compose.ui.unit.Dp, fontSize: androidx.compose.ui.unit.TextUnit, color: Color) {
    androidx.compose.material3.Text(
        text = text,
        modifier = Modifier.width(width),
        fontSize = fontSize,
        color = color
    )
}

@Composable
fun SummaryBox(modifier: Modifier, title: String, value: String) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().background(Color(0xFF3A9900)).padding(vertical = 4.dp)) {
            Text(title, color = Color.White, modifier = Modifier.align(Alignment.Center), fontSize = 14.sp)
        }
        Text(value, color = Color(0xFFFF5722), fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp), fontSize = 16.sp)
    }
}

@Composable
fun ScoreBox(modifier: Modifier, title: String, value: String, headerColor: Color, textColor: Color) {
     Column(modifier = modifier.padding(0.5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().background(headerColor).padding(vertical = 4.dp)) {
            Text(title, color = Color.White, modifier = Modifier.align(Alignment.Center), fontSize = 14.sp)
        }
        Box(Modifier.fillMaxWidth().background(Color.White).padding(vertical = 8.dp)) {
             Text(value, color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center), fontSize = 20.sp)
        }
    }
}

@Composable
fun ContactBox(onClickLine: () -> Unit, onClickPay: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.ic_clover),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            androidx.compose.material3.Text(
                "ติดต่อซื้อเบอร์มงคล หรือ เปิดดวงชะตา",
                color = Color(0xFF04B40B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Button(
                onClick = onClickLine,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B106)),
                contentPadding = PaddingValues(horizontal = 26.dp, vertical = 10.dp),
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
            ) {
                androidx.compose.material3.Text("CLICK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
