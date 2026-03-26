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
import androidx.compose.ui.layout.ContentScale
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
import com.numberniceic.ui.home.HomeCalAct
import com.numberniceic.utils.HomeContextManager

@Composable
fun HomeNumScreen(navController: NavController) {
    val context = LocalContext.current
    var homeNumInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    fun calculate() {
        if (homeNumInput.isNotEmpty()) {
            if (HomeContextManager.checkString(homeNumInput)) {
                Toast.makeText(context, "ข้อมูลไม่ถูกต้อง!!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(context, HomeCalAct::class.java)
                intent.putExtra("homeNum", homeNumInput)
                context.startActivity(intent)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF515151))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).background(Color(0xFFC5C5C5))) {
            // 1. Purple Input Section
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFB841CC)).padding(bottom = 8.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.icon_home02),
                            contentDescription = null,
                            modifier = Modifier.size(45.dp)
                        )
                        Box(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
                            if (homeNumInput.isEmpty()) {
                                Text("บ้านเลขที่", color = Color.Gray, fontSize = 45.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                            BasicTextField(
                                value = homeNumInput,
                                onValueChange = { if (it.all { c -> c.isDigit() || c == '/' }) homeNumInput = it },
                                textStyle = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Bold, fontFamily = browaFont, textAlign = TextAlign.Center),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(
                        onClick = { homeNumInput = "" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB841CC)),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_stop_48), null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ล้าง")
                    }
                    Spacer(Modifier.width(9.dp))
                    Button(
                        onClick = { calculate() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB841CC)),
                        shape = RoundedCornerShape(9.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_clover), null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ถอดรหัส")
                    }
                }
            }

            // 2. Contact Row
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFAF9F6)).padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(painter = painterResource(R.drawable.ic_clover), null, modifier = Modifier.padding(start = 9.dp).size(24.dp))
                Text(
                    text = "ติดต่อซื้อป้ายเลขที่ หรือ เปิดดวงชะตา",
                    color = Color(0xFF04B40B),
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f).padding(horizontal = 9.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("line://ti/p/~@n956364599"))) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B106)),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.padding(end = 9.dp).height(44.dp),
                    contentPadding = PaddingValues(horizontal = 26.dp, vertical = 10.dp)
                ) {
                    Text("CLICK", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // 3. Ribbon Info
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

            // 4. Example Images Section (with black title bg as in fragment_home.xml but simplified for index)
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF515151)).padding(vertical = 4.dp)) {
                Text(
                    "ตัวอย่างป้ายบ้านเลขที่ของเรา",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 15.dp, bottom = 4.dp)
                )
                val images = listOf(R.drawable.icon_homebox01, R.drawable.icon_homebox02, R.drawable.ex_home02, R.drawable.ex_home03, R.drawable.ex_home04)
                images.forEach { img ->
                    Image(
                        painter = painterResource(img),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp),
                        contentScale = ContentScale.FillWidth
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
