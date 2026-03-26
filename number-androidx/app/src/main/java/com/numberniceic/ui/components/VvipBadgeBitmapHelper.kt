package com.numberniceic.ui.components

import android.content.Context
import android.graphics.*
import android.util.TypedValue

/**
 * วาด VVIP Badge เป็น [Bitmap] ด้วย Android Canvas API
 * ใช้แทน [VvipBadgeComponent] (Compose) ในที่ที่ไม่มี Compose lifecycle
 * เช่น AppCompat Toolbar menu action view
 *
 * Visual style เทียบเท่า VvipBadgeComponent:
 *  - วงกลมพื้นหลังสีดำ
 *  - ขอบทองเมทัลลิก (SweepGradient)
 *  - ข้อความ "VVIP" ไล่สีทองเมทัลลิก (LinearGradient)
 *  - Shadow ใต้ตัวหนังสือ
 */
object VvipBadgeBitmapHelper {

    /**
     * สร้าง Bitmap ขนาด [sizeDp] x [sizeDp] dp
     * @param borderAngleDeg มุมหมุนของ SweepGradient ขอบ (0–360) สำหรับทำ animation
     */
    fun createBadgeBitmap(context: Context, sizeDp: Int, borderAngleDeg: Float = 0f): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (sizeDp * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = size / 2f
        val cy = size / 2f
        val borderWidth = size * 0.12f
        val innerRadius = cx - borderWidth / 2f

        // ── 1. พื้นหลังสีดำ ──────────────────────────────────────────────────
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }
        canvas.drawCircle(cx, cy, cx, bgPaint)

        // ── 2. ขอบทองเมทัลลิก (SweepGradient + rotate matrix) ────────────────
        val goldColors = intArrayOf(
            Color.parseColor("#D4AF37"), // Metallic Gold
            Color.parseColor("#FFD700"), // Bright Gold
            Color.parseColor("#FFF8E1"), // White Gold Highlight
            Color.parseColor("#FFD700"), // Bright Gold
            Color.parseColor("#AA8800"), // Deep Gold Shadow
            Color.parseColor("#D4AF37")  // Metallic Gold (close loop)
        )
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            val sweep = SweepGradient(cx, cy, goldColors, null)
            val matrix = Matrix().also { it.postRotate(borderAngleDeg, cx, cy) }
            sweep.setLocalMatrix(matrix)
            shader = sweep
        }
        canvas.drawCircle(cx, cy, innerRadius, borderPaint)

        // ── 3. ข้อความ "VVIP" ไล่สีทอง ──────────────────────────────────────
        val fontSize = size * 0.30f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSize
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(fontSize * 0.15f, 0f, fontSize * 0.08f, Color.argb(140, 0, 0, 0))
        }

        // วัดขนาดข้อความเพื่อกำหนด gradient
        val textBounds = Rect()
        textPaint.getTextBounds("VVIP", 0, 4, textBounds)
        val textTop = cy - textBounds.height() / 2f
        val textBottom = cy + textBounds.height() / 2f

        textPaint.shader = LinearGradient(
            0f, textTop, 0f, textBottom,
            intArrayOf(
                Color.parseColor("#D4AF37"),
                Color.parseColor("#FFD700"),
                Color.parseColor("#FFF8E1"),
                Color.parseColor("#FFD700"),
                Color.parseColor("#D4AF37")
            ),
            floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )

        // baseline y
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("VVIP", cx, textY, textPaint)

        return bitmap
    }

    /**
     * สร้างตราสัญลักษณ์ 'MB' (Member) แบบพรีเมียม (Metallic Blue Style)
     */
    fun createMbBadgeBitmap(context: Context, sizeDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (sizeDp * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = size / 2f
        val cy = size / 2f
        val borderWidth = size * 0.10f
        val innerRadius = cx - borderWidth / 2f

        // ── 1. พื้นหลังสีดำเข้ม (เพื่อให้ Gradient เด่น) ──────────────────────────
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
        }
        canvas.drawCircle(cx, cy, cx, bgPaint)

        // ── 2. ขอบเงินเมทัลลิก (Metallic Silver/Blue) ────────────────────────
        val silverColors = intArrayOf(
            Color.parseColor("#B0BEC5"), // Silver Grey
            Color.parseColor("#CFD8DC"), // Light Silver
            Color.parseColor("#FFFFFF"), // White Highlight
            Color.parseColor("#90A4AE"), // Steel Blue
            Color.parseColor("#B0BEC5")  // Silver Grey
        )
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            shader = SweepGradient(cx, cy, silverColors, null)
        }
        canvas.drawCircle(cx, cy, innerRadius, borderPaint)

        // ── 3. ข้อความ "MB" สีฟ้าเมทัลลิก ─────────────────────────────────────
        val fontSize = size * 0.38f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = fontSize
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(fontSize * 0.1f, 0f, 0f, Color.parseColor("#442196F3"))
        }

        val textBounds = Rect()
        textPaint.getTextBounds("MB", 0, 2, textBounds)
        val textTop = cy - textBounds.height() / 2f
        val textBottom = cy + textBounds.height() / 2f

        textPaint.shader = LinearGradient(
            0f, textTop, 0f, textBottom,
            intArrayOf(
                Color.parseColor("#64B5F6"), // Light Blue
                Color.parseColor("#2196F3"), // Blue
                Color.parseColor("#1976D2"), // Deep Blue
                Color.parseColor("#2196F3")  // Blue
            ),
            null,
            Shader.TileMode.CLAMP
        )

        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("MB", cx, textY, textPaint)

        return bitmap
    }

    /**
     * สร้างตราสัญลักษณ์ 'ADM' (Admin) แบบวงกลมสีแดงพรีเมียม
     */
    fun createAdmBadgeBitmap(context: Context, sizeDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (sizeDp * density).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = size / 2f
        val cy = size / 2f

        // ── 1. พื้นหลังวงกลมไล่เฉดสีทอง ─────────────────────────────────────────
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, size.toFloat(),
                intArrayOf(
                    Color.parseColor("#FFD700"), // Bright Gold
                    Color.parseColor("#D4AF37"), // Metallic Gold
                    Color.parseColor("#8A6508")  // Dark Gold
                ),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, cx * 0.9f, bgPaint)

        // ── 2. ขอบสีดำบางๆ ──────────────────────────────────────────────────
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = size * 0.03f
            alpha = 40
        }
        canvas.drawCircle(cx, cy, cx * 0.9f, borderPaint)

        // ── 3. ข้อความ "ADM" สีดำ ────────────────────────────────────────────
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = size * 0.38f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("ADM", cx, textY, textPaint)

        return bitmap
    }
}
