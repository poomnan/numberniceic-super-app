package com.numberniceic.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.numberniceic.R
import com.numberniceic.ui.namenick.NameNickListAct
import com.numberniceic.data.apicollectiondao.NickNameCollectionDao
import com.numberniceic.data.tabian.PairsMiracle
import com.numberniceic.https.ApiService
import com.numberniceic.https.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun NameNickScreen(navController: NavController) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf("") }
    var dayPosition by remember { mutableIntStateOf(0) }
    var nicknameResult by remember { mutableStateOf<NickNameCollectionDao?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val daysEng = listOf("BirthDay", "monday", "tuesday", "wednesday1", "wednesday2", "thursday", "friday", "saturday", "sunday")

    // Dynamic colors based on result presence
    val isResult = nicknameResult != null
    val screenBg = if (isResult) Color(0xFF818080) else Color.White
    val headerBg = if (isResult) Color(0xFFFFB300) else Color(0xFFFFB042)
    val actionRowBg = if (isResult) Color(0xFF6CBD15) else Color(0xFFFDD835)
    val buttonBg = if (isResult) Color(0xFF6CBD15) else Color(0xFF00B106)

    fun calculate() {
        if (dayPosition == 0 || nameInput.isEmpty()) return
        isLoading = true
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        apiService.getNicknameDetail(nameInput, daysEng[dayPosition]).enqueue(object : Callback<NickNameCollectionDao> {
            override fun onResponse(call: Call<NickNameCollectionDao>, response: Response<NickNameCollectionDao>) {
                isLoading = false
                if (response.isSuccessful) {
                    nicknameResult = response.body()
                } else {
                    Toast.makeText(context, "ไม่พบข้อมูล!!", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<NickNameCollectionDao>, t: Throwable) {
                isLoading = false
                Toast.makeText(context, "เครือข่ายขัดข้อง!!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Box(modifier = Modifier.fillMaxSize().background(screenBg)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            // 1. Input Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = if(isResult) 0.dp else 6.dp),
                colors = CardDefaults.cardColors(containerColor = if(isResult) Color.White else Color(0x8FFFFFFF)),
                shape = if(isResult) RoundedCornerShape(0.dp) else RoundedCornerShape(6.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).background(headerBg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "เลือกวันเกิด ",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = browaFont,
                            modifier = Modifier.weight(2f).padding(start = 10.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            DayDropdown(selectedDayIndex = dayPosition) { newIndex ->
                                dayPosition = newIndex
                                if (newIndex != 0 && nameInput.isNotEmpty()) calculate()
                            }
                        }
                    }

                    // Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "พิมพ์ชื่อเล่น : ", fontSize = 24.sp, color = Color.Black)
                        Box(modifier = Modifier.width(180.dp).padding(start = 4.dp)) {
                            if (nameInput.isEmpty()) Text("ชื่อเล่น", color = Color.Gray, fontSize = 24.sp)
                            BasicTextField(
                                value = nameInput,
                                onValueChange = { if (it.length <= 16) nameInput = it },
                                textStyle = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, fontFamily = browaFont, color = Color.Black),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                cursorBrush = SolidColor(Color.Black),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp).offset(y = 2.dp).background(Color.Gray))
                        }
                    }

                    // Bottom Action Column
                    Column(modifier = Modifier.fillMaxWidth().background(actionRowBg)) {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Button(
                                onClick = { nameInput = ""; dayPosition = 0; nicknameResult = null },
                                colors = ButtonDefaults.buttonColors(containerColor = buttonBg),
                                shape = RoundedCornerShape(9.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(painterResource(R.drawable.ic_stop_48), null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if(isResult) "ล้าง" else "ล้างข้อมูล")
                            }
                            Spacer(Modifier.width(9.dp))
                            Button(
                                onClick = { if(dayPosition != 0 && nameInput.isNotEmpty()) calculate() else Toast.makeText(context, "กรุณาพิมพ์ชื่อ และ เลือกวันเกิด!!", Toast.LENGTH_SHORT).show() },
                                colors = ButtonDefaults.buttonColors(containerColor = buttonBg),
                                shape = RoundedCornerShape(9.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(painterResource(R.drawable.ic_clover), null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ถอดรหัส")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(if(isResult) 0.dp else 11.dp).background(Color(0xFFFFB042)))
                    }
                }
            }

            // Results Section
            nicknameResult?.let { res ->
                // Section: คำนวณเลขศาสตร์
                Text(
                    "คำนวณเลขศาสตร์",
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF404040)).padding(vertical = 4.dp, horizontal = 6.dp),
                    color = Color(0xFFFDD835),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = browaFont
                )
                
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF0EEED)).padding(horizontal = 14.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    val gradeInfo = calculateGrade(res.sumSatNickName, res.pairsMiracle ?: emptyList())
                    Text("รวมเลขศาสตร์ชื่อ ${res.nickname} = ${res.sumSatNickName}${if(res.pairSatNickName?.fang != null) " แฝง ${res.pairSatNickName.fang}" else ""}", modifier = Modifier.weight(1f), color = Color(0xFF3C3434), fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = browaFont)
                    Text(gradeInfo.first, color = gradeInfo.second, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = browaFont)
                }

                Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(9.dp)) {
                    Text("เลขเรียงชื่อเล่น : ", fontSize = 18.sp, color = Color.Black)
                    Text(res.satNickName?.joinToString("  ") { it.xNum.toString() } ?: "-", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                
                Divider(modifier = Modifier.padding(horizontal = 3.dp), color = Color.DarkGray)

                Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(9.dp)) {
                    Text("ตัวอักษรกาลกิณี : ", fontSize = 18.sp, color = Color.Black)
                    Text(if(res.kName.isNullOrEmpty()) "ไม่มีตัวกาลกิณี" else res.kName.joinToString(" ") { "{ $it }" }, color = Color(0xFFFB8C00), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // Section: คำนวณพลังเงา
                Text(
                    "คำนวณพลังเงา",
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF404040)).padding(vertical = 4.dp, horizontal = 6.dp),
                    color = Color(0xFFFDD835),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = browaFont
                )

                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F1EE)).padding(horizontal = 14.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    val gradeInfo = calculateGrade(res.sumShaNickName, res.pairsMiracle ?: emptyList())
                    Text("ค่าพลังเงาชื่อเล่น ${res.nickname} = ${res.sumShaNickName}${if(res.pairShaNickName?.fang != null) " แฝง ${res.pairShaNickName.fang}" else ""}", modifier = Modifier.weight(1f), color = Color(0xFF3C3434), fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = browaFont)
                    Text(gradeInfo.first, color = gradeInfo.second, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = browaFont)
                }
            } ?: run {
                // Intro Ribbon Section
                CommonInfoRibbon(
                    title = "สิ่งที่ต้องคำนึงถึง",
                    items = listOf(
                        "1. " to stringResource(R.string.namenick_recoment01),
                        "2. " to stringResource(R.string.namenick_recoment02),
                        "3. " to stringResource(R.string.namenick_recoment03),
                        "4. " to stringResource(R.string.namenick_recoment04),
                    )
                )
            }

            // Contact Bar
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFAF9F6)).padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painter = painterResource(R.drawable.ic_clover), null, modifier = Modifier.padding(start = 9.dp).size(24.dp))
                Text(
                    text = "ติดต่อเพื่อตั้งชื่อสกุล หรือ เปิดดวงชะตา",
                    color = Color(0xFF04B40B),
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f).padding(horizontal = 9.dp),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("line://ti/p/~@n956364599"))) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B106)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(end = 9.dp)
                ) {
                    Text("CLICK", fontWeight = FontWeight.Bold)
                }
            }
            
            // Legacy Select Nickname Button (Restored)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        val intent = Intent(context, NameNickListAct::class.java)
                        intent.putExtra("call_fragmentx", "nickname")
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF428319)),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_eye_48),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp), // Slightly larger icon to match visual weight
                        tint = Color.White
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "เลือกชื่อเล่นไทย - ENG ตามวันเกิด\nUPGRADE !",
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = browaFont,
                        lineHeight = 24.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(80.dp))
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x44000000)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

fun calculateGrade(sum: Int, pairs: List<PairsMiracle>): Pair<String, Color> {
    if (sum == 0) return "" to Color.Transparent
    val pair = pairs.find { it.pairnumber?.toIntOrNull() == sum }
    
    val isRisky = sum in listOf(26, 62, 23, 32, 40, 4)
    if (isRisky) return "ดีแต่เสี่ยง" to Color(0xFFFF8F00) // Amber

    return when (pair?.pairtype) {
        "D10" -> "(ดีเยี่ยม)" to Color(0xFF4E8F09)
        "D8" -> "(ดีมาก)" to Color(0xFF4E8F09)
        "D5" -> "(ดี)" to Color(0xFF4E8F09)
        "R10", "R7", "R5" -> "(อันตราย)" to Color(0xFFF30A2D)
        else -> "" to Color(0xFFFF8F00)
    }
}
