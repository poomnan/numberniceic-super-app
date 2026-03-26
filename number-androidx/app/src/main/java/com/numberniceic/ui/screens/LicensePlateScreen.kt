package com.numberniceic.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.numberniceic.R
import com.numberniceic.ui.tabian.TabianBuyAllAct
import com.numberniceic.ui.tabian.TabianCalActivity
import com.numberniceic.utils.AppContextManager
import com.numberniceic.utils.TabianContextManager
import com.numberniceic.https.RetrofitClient
import com.numberniceic.https.ApiService
import com.numberniceic.data.tabian.TabianSellItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Custom Fonts

private val sarunFont = FontFamily(Font(R.font.sarun, FontWeight.Normal)) // Based on XML font family

// Global Cache to prevent reloading delay
private var globalTabianList: List<TabianSellItem> = emptyList()

@Composable
fun LicensePlateScreen(
    navController: NavController,
    showInputSection: Boolean = true,
    showViewAllButton: Boolean = true,
    showContactSection: Boolean = true,
    showContactSectionAtBottom: Boolean = false,
    limitCount: Int? = null
) {
    val context = LocalContext.current
    val shared = remember { context.getSharedPreferences("tabiandata", android.content.Context.MODE_PRIVATE) }
    val sharedUser = remember { context.getSharedPreferences("userdata", android.content.Context.MODE_PRIVATE) }
    
    var plateInput by remember { mutableStateOf("") }
    
    // Use cached list if available
    var tabianList by remember { mutableStateOf(globalTabianList) }
    // Only show loading if we have nothing
    var isLoading by remember { mutableStateOf(globalTabianList.isEmpty()) }
    var refreshCount by remember { mutableStateOf(0) }

    var isError by remember { mutableStateOf(false) }

    // Auto-timeout mechanism
    LaunchedEffect(isLoading) {
        if (isLoading) {
            kotlinx.coroutines.delay(10000) // 10 seconds timeout
            if (isLoading) {
                isLoading = false
                isError = true
            }
        }
    }

    fun refreshData() {
        // Only show full loading blocking UI if we have nothing to show
        if (tabianList.isEmpty()) {
            isLoading = true
        }
        isError = false
        
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getTabianSellList().enqueue(object : Callback<List<TabianSellItem>> {
            override fun onResponse(
                call: Call<List<TabianSellItem>>,
                response: Response<List<TabianSellItem>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val newList = response.body()!!
                    val sortedList = newList.sortedWith(
                        compareBy<TabianSellItem> { 
                            val ord = it.orderNo ?: 0
                            if (ord == 0) Int.MAX_VALUE else ord 
                        }
                        .thenByDescending { it.tabianId ?: 0 }
                    )
                    
                    // Update cache and state
                    globalTabianList = sortedList
                    tabianList = sortedList
                    isLoading = false
                    isError = false

                } else {
                    isLoading = false
                    isError = true
                    // Only show toast if it's an error and we are actively waiting
                }
            }

            override fun onFailure(call: Call<List<TabianSellItem>>, t: Throwable) {
                isLoading = false
                isError = true
                Log.e("LicensePlateScreen", "Network Error: ${t.message}")
            }
        })
    }

    LaunchedEffect(refreshCount) {
        refreshData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEEEEE))
    ) {
        
        // 1. Contact Section (top)


        // 2. White Container for Input
        if (showInputSection) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    // Grey Background Container (#AAA8A8) for Input Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFAAA8A8))
                            .padding(bottom = 0.dp), // Check padding
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Black Card -> Inner Card -> Input
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(5.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White), // Back to White
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                    // Using BasicTextField to remove default Material padding and height constraints
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = plateInput,
                                        onValueChange = { if (it.length <= 7) plateInput = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .offset(y = (-14).dp), // Adjust for optical centering
                                        textStyle = TextStyle(
                                            fontSize = 64.sp, // Slightly Larger to fit the box nicely
                                            fontFamily = sarunFont,
                                            textAlign = TextAlign.Center,
                                            color = Color.Black,
                                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                                includeFontPadding = false
                                            ),
                                            lineHeight = 70.sp
                                        ),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (plateInput.isEmpty()) {
                                                    Text(
                                                        text = "4กธ4245", // Hint Updated
                                                        style = TextStyle(
                                                            fontSize = 64.sp,
                                                            fontFamily = sarunFont,
                                                            textAlign = TextAlign.Center,
                                                            color = Color.Gray.copy(alpha = 0.5f),
                                                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                                                includeFontPadding = false
                                                            ),
                                                            lineHeight = 70.sp
                                                        )
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        }
                                    )
                            }
                        }
        
                        // Blue Action Bar (#3CA7E6)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF3CA7E6))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Clear Button
                                Button(
                                    onClick = { plateInput = "" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3CA7E6)),
                                    shape = RoundedCornerShape(9.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(painterResource(R.drawable.ic_stop_48), null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ล้าง", fontSize = 14.sp)
                                }
                                
                                Spacer(Modifier.width(8.dp)) 

                                // Miracle (Predict) Button
                                Button(
                                    onClick = { 
                                        val snackbar = android.widget.Toast.makeText(context, "โปรดคลิกปุ่มถอดรหัสก่อน!!", android.widget.Toast.LENGTH_SHORT)
                                        snackbar.show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3CA7E6)),
                                    shape = RoundedCornerShape(9.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(painterResource(R.drawable.ic_eye_48), null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ผลทำนาย", fontSize = 14.sp)
                                }

                                Spacer(Modifier.width(8.dp)) 

                                // Calculate Button
                                Button(
                                    onClick = { 
                                        if (TabianContextManager.checkTabianNumber(plateInput)) {
                                            AppContextManager.countCalTatian(shared)
                                            if (AppContextManager.checkPermisTabianVip(shared, sharedUser)) {
                                                val intent = Intent(context, TabianCalActivity::class.java)
                                                intent.putExtra("TABAINNUMBER", plateInput)
                                                context.startActivity(intent)
                                            } else {
                                                Toast.makeText(context, "โปรดปรับระดับสมาชิกเป็น VIP", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                             Toast.makeText(context, "กรอกข้อมูลไม่ถูกต้อง!!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3CA7E6)),
                                    shape = RoundedCornerShape(9.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 2.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(painterResource(R.drawable.ic_clover), null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ถอดรหัส", fontSize = 14.sp)
                                }
                            }
                        }
                    } // End Grey/Input Section
        
                } 
            }
        }
        // Contact Section (Middle)
        if (showContactSection && !showContactSectionAtBottom) {
            item {
                LicenseContactSection()
            }
        }

        // 3. VIP Data Section Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Button-like (Black)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1EFEF))
                        .padding(bottom = 2.dp) // Margin bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "เลือกซื้อทะเบียนรถมงคล",
                            color = Color(0xFFFAF19F),
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 3.dp)
                        )
                    }
                }
    
                // VIP Header
                 Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                     Image(
                        painter = painterResource(id = R.drawable.ic_vip02),
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp)
                            .padding(start = 2.dp)
                    )
                    Text(
                        text = "ทะเบียนรถมงคล VIP",
                        color = Color(0xFF3fa33f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 3.dp)
                    )
                 }
                 
                 // Loading
                 if (isLoading) {
                     Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(20.dp), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator(color = Color(0xFF3fa33f))
                     }
                 } else if (isError) {
                      Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(20.dp), contentAlignment = Alignment.Center) {
                          Column(horizontalAlignment = Alignment.CenterHorizontally) {
                              Text(
                                "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color = Color.Red
                              )
                              Spacer(modifier = Modifier.height(8.dp))
                              Button(
                                  onClick = { refreshCount++ },
                                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3CA7E6))
                              ) {
                                  Text("ลองใหม่อีกครั้ง")
                              }
                          }
                      }
                 } else if (tabianList.isEmpty()) {
                     Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(20.dp), contentAlignment = Alignment.Center) {
                         Text(
                            "ไม่พบข้อมูลป้ายทะเบียนในขณะนี้",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                         )
                     }
                 }
            }
        }
        
        // Items
        if (!isLoading && tabianList.isNotEmpty()) {
            val displayList = if (limitCount != null) tabianList.take(limitCount) else tabianList
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEEEEE)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        displayList.chunked(2).forEachIndexed { index, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                row.forEach { item ->
                                    val tNum = item.tabianNumber ?: ""
                                    val tProv = item.tabianProvince ?: "กรุงเทพมหานคร"
                                    TabianCard(tNum, tProv, item.tabianPrice) {
                                        openTabian(context, tNum.replace(" ", ""))
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier
                                        .width(160.dp)
                                        .padding(5.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // View All Button
        if (showViewAllButton) {
            item {
                 Column(
                     modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                 ) {
                      Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { context.startActivity(Intent(context, TabianBuyAllAct::class.java)) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF8F7F7)),
                             modifier = Modifier.height(45.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.icon_luckycat),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(1.dp)) // marginStart 1dp
                            Text(
                                "เลือกดูทะเบียนทั้งหมด",
                                color = Color(0xFFFA801B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                 }
            }
        }

        
        // 4. Info Ribbon
        item {
            Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column {
                         // Header #3a9900 (Ribbon top)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2D2D2D))
                                .padding(start = 15.dp, top = 14.dp, end = 16.dp, bottom = 14.dp)
                        ) {
                             Text(
                                text = stringResource(id = R.string.tabain_vision),
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        
                        // Content
                         Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF5C5C5C))
                                .padding(16.dp)
                        ) {
                            LicenseInfoRow("1. ", stringResource(R.string.tabian_recument01))
                            LicenseInfoRow("2. ", stringResource(R.string.tabian_recoment02))
                            LicenseInfoRow("3. ", stringResource(R.string.tabian_recoment03))
                            LicenseInfoRow("4. ", stringResource(R.string.tabian_recomment04))
                            LicenseInfoRow("5. ", stringResource(R.string.tabian_recoment05))
                            LicenseInfoRow("6. ", stringResource(R.string.tabian_recomment06))
                            LicenseInfoRow("7. ", stringResource(R.string.tabian_recoment07))
                        }
                    }
            }
        }

        item {
            Spacer(Modifier.height(80.dp))
        }

        // Contact Section (bottom)
        if (showContactSection && showContactSectionAtBottom) {
            item {
                LicenseContactSection()
            }
        }
    }
}

@Composable
fun TabianCard(plate: String, province: String, price: Int?, onClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(5.dp), // Move padding here
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .width(160.dp) // approx
                .height(80.dp)
                //.padding(5.dp) // Removed padding from here as it is now on parent column
                .clickable(onClick = onClick)
                .border(3.dp, Color.Black, RoundedCornerShape(4.dp)), // Stroke
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                 Text(
                    text = plate,
                    fontFamily = sarunFont,
                    color = Color.Black,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = province,
                    color = Color.Black,
                    fontSize = 12.sp,
                    modifier = Modifier.offset(y = (-5).dp)
                 )
            }
        }
        
        // Price Tag
        if (price != null && price > 0) {
            val formatter = java.text.DecimalFormat("#,###")
            Text(
                text = "${formatter.format(price)} บาท",
                color = Color.Black, // Black
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}


private fun openTabian(context: android.content.Context, tabianNum: String) {
    val intent = Intent(context, TabianCalActivity::class.java)
    intent.putExtra("TABAINNUMBER", tabianNum)
    context.startActivity(intent)
}

@Composable
fun LicenseContactSection() {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_clover),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = "ติดต่อซื้อทะเบียนรถ หรือ เปิดดวงชะตา",
            color = Color(0xFF04B40B),
            fontSize = 14.sp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Button(
            onClick = {
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
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.height(44.dp),
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 10.dp)
        ) {
            Text("CLICK", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
