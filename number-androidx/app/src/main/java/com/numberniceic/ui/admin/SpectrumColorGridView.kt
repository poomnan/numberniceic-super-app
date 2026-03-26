package com.numberniceic.ui.admin

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Custom view that draws a color spectrum grid (hue x lightness matrix)
 * with a draggable selector cursor — mimics the iOS color picker grid.
 */
class SpectrumColorGridView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private val selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 3f
    }
    private val selectorShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 5f
    }

    var onColorChanged: ((Int) -> Unit)? = null

    private var selectorX = 0f
    private var selectorY = 0f
    private var currentColor: Int = Color.RED

    private val COLS = 24  // hue columns
    private val ROWS = 14  // lightness rows (top = white, bottom = black)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            buildBitmap(w, h)
            // Restore cursor to current color position
            positionSelectorFromColor(currentColor, w, h)
        }
    }

    private fun buildBitmap(w: Int, h: Int) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cellW = w.toFloat() / COLS
        val cellH = h.toFloat() / ROWS
        val hsv = FloatArray(3)
        val paint = Paint()

        for (col in 0 until COLS) {
            for (row in 0 until ROWS) {
                // Hue: leftmost col = grayscale column, rest = hue 0-360
                val isGrayCol = (col == 0)
                if (isGrayCol) {
                    val gray = (255 * (1f - row.toFloat() / (ROWS - 1))).toInt().coerceIn(0, 255)
                    paint.color = Color.rgb(gray, gray, gray)
                } else {
                    val hue = (col - 1).toFloat() / (COLS - 2) * 360f
                    val totalRows = ROWS.toFloat()
                    val midRow = totalRows / 2f
                    
                    if (row < midRow) {
                        // แถวบน: ไล่จาก ขาว -> สีแท้ (เพิ่ม Saturation, Fix Value 100%)
                        hsv[0] = hue
                        hsv[1] = row / midRow // จาก 0 (ขาว) ไป 1 (สด)
                        hsv[2] = 1.0f
                    } else {
                        // แถวล่าง: ไล่จาก สีแท้ -> ดำ (Fix Saturation 100%, ลด Value)
                        hsv[0] = hue
                        hsv[1] = 1.0f
                        hsv[2] = 1.0f - (row - midRow) / midRow // จาก 1 (สว่าง) ไป 0 (มืด)
                    }
                    paint.color = Color.HSVToColor(hsv)
                }
                canvas.drawRect(
                    col * cellW, row * cellH,
                    (col + 1) * cellW, (row + 1) * cellH,
                    paint
                )
            }
        }
        bitmap = bmp
    }

    override fun onDraw(canvas: Canvas) {
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        val r = 14f
        canvas.drawCircle(selectorX, selectorY, r + 2f, selectorShadowPaint)
        canvas.drawCircle(selectorX, selectorY, r, selectorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val x = event.x.coerceIn(0f, width.toFloat() - 1)
                val y = event.y.coerceIn(0f, height.toFloat() - 1)
                selectorX = x
                selectorY = y
                val color = colorAtPos(x, y)
                currentColor = color
                onColorChanged?.invoke(color)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun colorAtPos(x: Float, y: Float): Int {
        val bmp = bitmap ?: return Color.WHITE
        val px = x.toInt().coerceIn(0, bmp.width - 1)
        val py = y.toInt().coerceIn(0, bmp.height - 1)
        return bmp.getPixel(px, py)
    }

    fun setColor(color: Int) {
        currentColor = color
        if (width > 0 && height > 0) {
            positionSelectorFromColor(color, width, height)
            invalidate()
        }
    }

    private fun positionSelectorFromColor(color: Int, w: Int, h: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val hue = hsv[0]
        val sat = hsv[1]
        val value = hsv[2]

        val isGray = sat < 0.1f
        val cellW = w.toFloat() / COLS
        val cellH = h.toFloat() / ROWS

        if (isGray) {
            selectorX = cellW / 2
            val gray = (Color.red(color) / 255f)
            selectorY = ((1f - gray) * (ROWS - 1) * cellH + cellH / 2).coerceIn(0f, h.toFloat())
        } else {
            val col = 1 + hue / 360f * (COLS - 2)
            val row = (1f - value) / 0.9f * (ROWS - 1)
            selectorX = (col * cellW + cellW / 2).coerceIn(0f, w.toFloat())
            selectorY = (row * cellH + cellH / 2).coerceIn(0f, h.toFloat())
        }
    }
}
