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

@Composable
fun NameSurScreen(navController: NavController) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf("") }
    var surInput by remember { mutableStateOf("") }
    var dayPosition by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    // Since NameSur results open in a new Activity in android-app-master, 
    // we keep the Index colors here. But I'll ensure they are correct.
    val screenBg = Color.White
    val headerBg = Color(0xFFFFB042) // Orange from fragment_name_sur_index.xml
    val actionRowBg = Color(0xFFFDD835) // Yellow
    val buttonBg = Color(0xFF00B106) // Green

    fun calculate() {
        if (dayPosition == 0 || nameInput.isEmpty() || surInput.isEmpty()) {
            Toast.makeText(context, "กรุณาพิมพ์ชื่อ นามสกุล และ เลือกวันเกิด!!", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(context, com.numberniceic.ui.namesur.NameSurCalAct::class.java)
        intent.putExtra("dayPosition", dayPosition)
        intent.putExtra("name", nameInput)
        intent.putExtra("surname", surInput)
        context.startActivity(intent)
    }

    Box(modifier = Modifier.fillMaxSize().background(screenBg)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            // 1. Input Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x8FFFFFFF)),
                shape = RoundedCornerShape(6.dp)
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
                            DayDropdown(selectedDayIndex = dayPosition) { dayPosition = it }
                        }
                    }

                    // Name Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "ชื่อจริง @:", fontSize = 24.sp, color = Color.Black)
                        Box(modifier = Modifier.width(240.dp).padding(start = 9.dp)) {
                            if (nameInput.isEmpty()) Text("ชื่อ", color = Color.Gray, fontSize = 24.sp)
                            BasicTextField(
                                value = nameInput,
                                onValueChange = { if (it.length <= 20) nameInput = it },
                                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = browaFont, color = Color.Black),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Surname Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "นามสกุล :", fontSize = 24.sp, color = Color.Black)
                        Box(modifier = Modifier.width(240.dp).padding(start = 9.dp)) {
                            if (surInput.isEmpty()) Text("สกุล", color = Color.Gray, fontSize = 24.sp)
                            BasicTextField(
                                value = surInput,
                                onValueChange = { if (it.length <= 20) surInput = it },
                                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = browaFont, color = Color.Black),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Action Column
                    Column(modifier = Modifier.fillMaxWidth().background(actionRowBg)) {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Button(
                                onClick = { nameInput = ""; surInput = ""; dayPosition = 0 },
                                colors = ButtonDefaults.buttonColors(containerColor = buttonBg),
                                shape = RoundedCornerShape(9.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(painterResource(R.drawable.ic_stop_48), null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ล้างข้อมูล")
                            }
                            Spacer(Modifier.width(9.dp))
                            Button(
                                onClick = { calculate() },
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
                        // Bottom orange line as in fragment_name_sur_index.xml
                        Box(modifier = Modifier.fillMaxWidth().height(11.dp).background(headerBg))
                    }
                }
            }

            // Contact Row
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

            // Info Ribbon
            CommonInfoRibbon(
                title = "สิ่งที่ต้องคำนึงถึง",
                items = listOf(
                    "1. " to stringResource(R.string.impotant_home01),
                    "2. " to stringResource(R.string.impotan_home02),
                    "3. " to stringResource(R.string.impotant_home03),
                    "4. " to stringResource(R.string.impotant_home04),
                    "5. " to stringResource(R.string.impotant_home05),
                )
            )

            Spacer(Modifier.height(80.dp))
        }
    }
}
