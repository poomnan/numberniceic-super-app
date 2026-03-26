package com.numberniceic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*

@Composable
fun VvipBadgeComponent(size: Int = 48) {
    val shimmerTransition = rememberInfiniteTransition(label = "vvip_shimmer")
    
    // ✨ Animation สำหรับการหมุนเงาตามขอบ (Border Rotation)
    val borderRotation by shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_rotation"
    )

    val blackBackground = Color(0xFF000000)
    val goldColor = Color(0xFFFFD700)
    
    // 🎨 ขอบสีทองคำเมทัลลิกแบบสมจริง (เงาสะท้อนระดับสูง)
    val rotatingBorderBrush = Brush.sweepGradient(
        colors = listOf(
            Color(0xFFD4AF37), // Metallic Gold
            Color(0xFFFFD700), // Bright Gold
            Color(0xFFFFF8E1), // Highlight (White Gold)
            Color(0xFFFFD700), // Bright Gold
            Color(0xFFAA8800), // Deep Gold Shadow
            Color(0xFFD4AF37)  // Metallic Gold
        )
    )

    // 🎨 รัศมีเรืองแสงแบบหมุน (Rotating Aura) สำหรับหลังตัวหนังสือ
    val rotatingTextGlowBrush = Brush.sweepGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.4f),
            Color.Transparent
        )
    )

    val displaySize = size.toFloat()
    
    Box(
        modifier = Modifier.size((displaySize * 1.5f).dp),
        contentAlignment = Alignment.Center
    ) {
        // 🎖️ ตัว Badge
        Box(
            modifier = Modifier
                .size(size.dp),
            contentAlignment = Alignment.Center
        ) {
            // 1. พื้นหลังดำ (นิ่งสนิท)
            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(blackBackground))
            
            // 2. ขอบทองที่หมุนได้ (แสงวิ่งรอบขอบ)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = borderRotation }
                    .border(
                        width = 5.dp,
                        brush = rotatingBorderBrush,
                        shape = CircleShape
                    )
            )
        }

        // 🌟 รัศมีเรืองแสงหลังตัวหนังสือที่หมุนตาม (Rotating Glow - ปรับขนาดเท่าพื้นหลัง)
        Box(
            modifier = Modifier
                .size(size.dp) // 📏 ปรับขนาดให้เท่ากับพื้นหลังสีดำตามคำขอ
                .graphicsLayer { rotationZ = borderRotation + 180f }
                .background(rotatingTextGlowBrush, shape = CircleShape)
                // .blur(radius = 12.dp) // ❌ นำออกเพื่อป้องกันการแครชใน Android 11 ลงไป
        )

        // 🎨 สีทองเมทัลลิกแบบหลายเฉด (Deep Gold -> Bright Gold -> Deep Gold)
        val metallicGoldBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFD4AF37), // Metallic Gold
                Color(0xFFFFD700), // Bright Gold
                Color(0xFFFFF8E1), // White Gold Highlight
                Color(0xFFFFD700),
                Color(0xFFD4AF37)
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, 100f) // ไล่จากบนลงล่างเพื่อสร้างมิติ
        )

        val fontSize = (displaySize * 0.32f).sp
        
        Text(
            text = "VVIP",
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                brush = metallicGoldBrush,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    offset = Offset(0f, 2f),
                    blurRadius = 4f
                )
            )
        )
        
        // 🌟 เลเยอร์เรืองแสง (Glow) ด้านหน้าตัวหนังสือ (ทำให้น่าหลงใหลขึ้นเยอะ)
        Text(
            text = "VVIP",
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                color = Color.Transparent,
                shadow = Shadow(
                    color = goldColor.copy(alpha = 0.8f),
                    offset = Offset(0f, 0f),
                    blurRadius = 12f
                )
            )
        )
    }
}
