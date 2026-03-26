package com.numberniceic.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.PhoneSellNumberCollectionDao
import com.numberniceic.data.phone.PhoneNumberItem
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.phone.PhoneBuyAllAct
import com.numberniceic.ui.phone.PhoneCalAct
import com.numberniceic.utils.AppContextManager
import com.numberniceic.utils.PersonNewsCacheManager
import com.numberniceic.utils.PhoneContextManager
import com.numberniceic.utils.UserContextManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

// Custom Fonts


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneHomeScreen(navController: NavController) {
    val context = LocalContext.current
    val shared = remember { context.getSharedPreferences("phonedata", android.content.Context.MODE_PRIVATE) }
    val sharedUser = remember { context.getSharedPreferences("userdata", android.content.Context.MODE_PRIVATE) }
    
    // Correct type: PhoneNumberItem
    var vipPhones by remember { mutableStateOf<List<PhoneNumberItem>?>(null) }
    var phoneInput by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Initial Data Load
    LaunchedEffect(Unit) {
        // 1. Cache First
        val cached = PersonNewsCacheManager.loadPhoneSell(context)
        if (cached?.phonenumTop4 != null) {
            vipPhones = cached.phonenumTop4
        }
        
        // 2. Network Fetch
        fetchPhoneSellList(context) { data ->
            if (data?.phonenumTop4 != null) {
                vipPhones = data.phonenumTop4
                PersonNewsCacheManager.savePhoneSell(context, data)
            }
        }
    }

    // Input Logic (Auto-submit at 10 digits)
    LaunchedEffect(phoneInput) {
        val cleanInput = phoneInput.filter { it.isDigit() }
        if (cleanInput.length == 10 && cleanInput.startsWith("0")) {
            // Count stat
            AppContextManager.countPhoneCal(shared)
            
            // Check permission
            if (AppContextManager.checkPermisPhoneVip(shared, sharedUser)) {
                val intent = Intent(context, PhoneCalAct::class.java)
                intent.putExtra("PHONENUMBER", cleanInput)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, context.getString(R.string.request_vip), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onRefresh() {
        isRefreshing = true
        fetchPhoneSellList(context) { data ->
            isRefreshing = false
            if (data != null) {
                vipPhones = data.phonenumTop4
                PersonNewsCacheManager.savePhoneSell(context, data)
                Toast.makeText(context, "รีเฟรชข้อมูลสำเร็จ", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "ไม่สามารถรีเฟรชข้อมูลได้", Toast.LENGTH_SHORT).show()
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onRefresh() },
        modifier = Modifier.fillMaxSize().background(Color(0xFFCCCBCB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Input Section - Yellow/Orange Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFB300))
                    .padding(top = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = phoneInput,
                        onValueChange = { if (it.length <= 10) phoneInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(Color.White),
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }

            // 2. Action Buttons Section - Green Background #61AC12
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF61AC12))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                // Prediction Result Button
                Button(
                    onClick = { 
                         Toast.makeText(context, "โปรดกรอกหมายเลขโทรศัพท์ก่อน", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF61AC12)),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_eye_48),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ผลทำนาย", color = Color.White)
                }

                // Buy Home Button
                Button(
                    onClick = {
                        context.startActivity(Intent(context, PhoneBuyAllAct::class.java))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF61AC12)),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clover),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("เลือกเบอร์ดี", color = Color.White)
                }
            }
            
            // Light grey divider/spacer matching lin_summery (#f1efef)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0xFFF1EFEF))
            )

            // 3. Contact/Add Line Section - White Background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_clover),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 9.dp, top = 4.dp)
                        .size(24.dp)
                )
                
                Text(
                    text = stringResource(id = R.string.contact_miraphone),
                    color = Color(0xFF04B40B),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 9.dp),
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = {
                        // Logic to open LINE
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B106)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(end = 9.dp)
                ) {
                    Text("CLICK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // 4. VIP List Section
            // Header - Black background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(4.dp)
            ) {
                Text(
                    text = "เบอร์โทรศัพท์มงคล VIP",
                    color = Color(0xFFFAF19F),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 3.dp)
                )
            }

            // List Items Container - White background
             Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 2.dp)
            ) {
                if (vipPhones != null) {
                    // Correct type usage: PhoneNumberItem
                    vipPhones!!.take(4).forEach { item ->
                        VipPhoneCard(item) {
                            val intent = Intent(context, PhoneCalAct::class.java)
                            intent.putExtra("PHONENUMBER", item.phoneNumber)
                            context.startActivity(intent)
                        }
                    }
                } else {
                    // Loading state
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                // "Show All" Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { context.startActivity(Intent(context, PhoneBuyAllAct::class.java)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8F7F7)),
                        modifier = Modifier.height(45.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icon_luckycat),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "เลือกดูเบอร์ทั้งหมด",
                            color = Color(0xFFFA801B),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 5. Info Ribbon/Card Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                shape = RoundedCornerShape(0.dp) // XML looks square/default
            ) {
                Column {
                    // Header #3a9900 (Ribbon top)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2D2D2D))
                            .padding(start = 15.dp, top = 36.dp, end = 16.dp, bottom = 14.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.caption_phone_ad),
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }

                    // Content #5c5c5c
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF5C5C5C))
                            .padding(16.dp)
                    ) {
                        InfoRow("1. ", stringResource(R.string.detail_01))
                        InfoRow("2. ", stringResource(R.string.detail_02))
                        InfoRow("3. ", stringResource(R.string.detail03))
                        InfoRow("4. ", stringResource(R.string.detail04))
                        InfoRow("5. ", stringResource(R.string.detail05))
                        InfoRow("6. ", stringResource(R.string.detail06))
                        InfoRow("7. ", stringResource(R.string.detail07))
                    }
                }
            }

            // Footer
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun VipPhoneCard(item: PhoneNumberItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_vip02),
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .padding(start = 5.dp)
            )
            
            // Format phone number
            val phoneNum = PhoneContextManager.getFormatPhoneNumber(item.phoneNumber ?: "")
            Text(
                text = phoneNum,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A96EB),
                modifier = Modifier.padding(start = 9.dp)
            )

            // Sum
            Text(
                text = "(${item.phoneSum})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF8F00),
                modifier = Modifier.padding(start = 3.dp)
            )

            // Price - using DecimalFormat for thousands separator
            val price = try {
                item.phonePrice?.toDouble() ?: 0.0
            } catch (e: Exception) { 0.0 }
            val formattedPrice = java.text.DecimalFormat("#,###").format(price)
            
            Text(
                text = "ราคา $formattedPrice บาท",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(start = 9.dp)
            )
        }
    }
}

@Composable
fun InfoRow(prefix: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
    ) {
        Text(
            text = prefix,
            color = Color(0xFFF7E337),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = browaFont
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 20.sp,
            fontFamily = browaFont
        )
    }
}

private fun fetchPhoneSellList(context: android.content.Context, onResult: (PhoneSellNumberCollectionDao?) -> Unit) {
    val apiService = RetrofitClient.instance.create(ApiService::class.java)
    apiService.getPhoneSellList().enqueue(object : Callback<PhoneSellNumberCollectionDao> {
        override fun onResponse(call: Call<PhoneSellNumberCollectionDao>, response: Response<PhoneSellNumberCollectionDao>) {
            if (response.isSuccessful && response.body() != null) {
                onResult(response.body())
            } else {
                onResult(null)
            }
        }

        override fun onFailure(call: Call<PhoneSellNumberCollectionDao>, t: Throwable) {
            onResult(null)
        }
    })
}
