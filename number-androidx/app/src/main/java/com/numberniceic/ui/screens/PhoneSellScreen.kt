package com.numberniceic.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.numberniceic.R
import com.numberniceic.data.apicollectiondao.PhoneSellNumberCollectionDao
import com.numberniceic.data.phone.PhoneNumberItem
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import com.numberniceic.ui.phone.PhoneCalAct
import com.numberniceic.utils.PhoneContextManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun PhoneSellScreen(
    navController: NavController,
    showContactSection: Boolean = true
) {
    val context = LocalContext.current
    var phoneData by remember { mutableStateOf<PhoneSellNumberCollectionDao?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var refreshCount by remember { mutableStateOf(0) }

    LaunchedEffect(refreshCount) {
        isLoading = true
        isError = false
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getPhoneSellList().enqueue(object : Callback<PhoneSellNumberCollectionDao> {
            override fun onResponse(call: Call<PhoneSellNumberCollectionDao>, response: Response<PhoneSellNumberCollectionDao>) {
                if (response.isSuccessful && response.body() != null) {
                    phoneData = response.body()
                    isLoading = false
                } else {
                    isLoading = false
                    isError = true
                }
            }
            override fun onFailure(call: Call<PhoneSellNumberCollectionDao>, t: Throwable) {
                isLoading = false
                isError = true
                Log.e("PhoneSellScreen", "Error: ${t.message}")
            }
        })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF457E1E))
        } else if (isError) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ไม่สามารถโหลดข้อมูลได้", color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { refreshCount++ }) {
                    Text("ลองใหม่")
                }
            }
        } else {
            val allPhones = phoneData?.phonenumberSell ?: emptyList()
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (showContactSection) {
                    item {
                        ContactLineSection()
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icon_luckycat),
                            contentDescription = null,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "เบอร์โทรศัพท์มงคล VIP",
                            color = Color(0xFF04B40B),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(allPhones) { item ->
                    PhoneSellItemRow(item) {
                        val intent = Intent(context, PhoneCalAct::class.java)
                        intent.putExtra("PHONENUMBER", item.phoneNumber)
                        intent.putExtra("PHONEPRICE", item.phonePrice)
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactLineSection() {
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
            text = "ติดต่อซื้อเบอร์มงคล หรือ เปิดดวงชะตา",
            color = Color(0xFF04B40B),
            fontSize = 14.sp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
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
            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 10.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE9F6EA))
        ) {
            Text("CLICK", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PhoneSellItemRow(item: PhoneNumberItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val iconRes = when (item.phone_group?.lowercase()) {
                "vip", "viptop4" -> R.drawable.ic_vip02
                "d" -> R.drawable.icon_diamond02
                "g" -> R.drawable.icon_gold01
                "s", "b" -> R.drawable.ic_clover
                else -> R.drawable.smartphone
            }

            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(54.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(12.dp))

            val priceDouble = item.phonePrice?.toDoubleOrNull() ?: 0.0
            val formattedPrice = java.text.DecimalFormat("#,###").format(priceDouble)
            val phoneNum = PhoneContextManager.getFormatPhoneNumber(item.phoneNumber ?: "")
            val sum = item.phoneSum ?: "-"

            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFF2B8FE8), fontWeight = FontWeight.Bold)) {
                        append(phoneNum)
                    }
                    append(" ")
                    withStyle(style = SpanStyle(color = Color(0xFFFF8F00), fontWeight = FontWeight.Bold)) {
                        append("($sum)")
                    }
                    append(" ")
                    withStyle(style = SpanStyle(color = Color(0xFF2B8FE8), fontWeight = FontWeight.Normal)) {
                        append("ราคา $formattedPrice บ.")
                    }
                },
                modifier = Modifier.weight(1f),
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
