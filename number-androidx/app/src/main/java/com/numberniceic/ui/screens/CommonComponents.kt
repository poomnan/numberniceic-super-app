package com.numberniceic.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.numberniceic.R

val browaFont = FontFamily(Font(R.font.browa_0, FontWeight.Normal))

@Composable
fun LicenseInfoRow(prefix: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
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

@Composable
fun CommonInfoRibbon(
    title: String,
    items: List<Pair<String, String>>,
    headerColor: Color = Color(0xFF3A9900)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D2D2D))
                    .padding(start = 15.dp, top = 36.dp, end = 16.dp, bottom = 14.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = browaFont
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF5C5C5C))
                    .padding(16.dp)
            ) {
                items.forEach { (prefix, text) ->
                    LicenseInfoRow(prefix, text)
                }
            }
        }
    }
}

data class DayItem(val name: String, val value: String, val iconRes: Int)

@Composable
fun DayDropdown(
    selectedDayIndex: Int,
    onDaySelected: (Int) -> Unit
) {
    val days = listOf(
        DayItem("--วันเกิด--", "BirthDay", R.drawable.happy),
        DayItem("จันทร์", "monday", R.drawable.d_moon),
        DayItem("อังคาร", "tuesday", R.drawable.d_mars),
        DayItem("พุธ (กลางวัน)", "wednesday1", R.drawable.d_mercury),
        DayItem("พุธ (กลางคืน)", "wednesday2", R.drawable.d_mercury),
        DayItem("พฤหัสบดี", "thursday", R.drawable.d_jupiter),
        DayItem("ศุกร์", "friday", R.drawable.d_asteroid),
        DayItem("เสาร์", "saturday", R.drawable.d_saturn),
        DayItem("อาทิตย์", "sunday", R.drawable.d_sun)
    )

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { expanded = true }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentItem = days.getOrElse(selectedDayIndex) { days[0] }
            Image(
                 painter = painterResource(id = currentItem.iconRes),
                 contentDescription = null,
                 modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = currentItem.name,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = browaFont,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.White
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(200.dp)
                .background(Color.White)
        ) {
            days.forEachIndexed { index, day ->
                DropdownMenuItem(
                    modifier = Modifier.background(if (index % 2 == 0) Color.White else Color(0xFFF3F3F2)),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = day.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                day.name, 
                                color = Color(0xFFFF6A13), // Master item_spinner.xml text color
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    onClick = {
                        onDaySelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}
