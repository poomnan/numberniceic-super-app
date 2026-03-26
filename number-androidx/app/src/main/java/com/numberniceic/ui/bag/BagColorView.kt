package com.numberniceic.ui.bag

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.numberniceic.R

/**
 * Custom View สำหรับแสดง 4 กระเป๋า Hermès Constance แยกกัน
 * แต่ละใบมีสีต่างกัน พร้อมฝาปิดโค้งและหัวเข็มขัด H
 */
class BagColorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // สี 4 สีของกระเป๋า (default - White pending admin data)
    var color1: Int = Color.parseColor("#FFFFFF")      // ขาว
    var color2: Int = Color.parseColor("#FFFFFF")      // ขาว
    var color3: Int = Color.parseColor("#FFFFFF")      // ขาว
    var color4: Int = Color.parseColor("#FFFFFF")      // ขาว

    /**
     * ตั้งค่าสีกระเป๋าทั้ง 4 สี
     */
    // Bitmap Cache
    private var cachedBitmap: Bitmap? = null
    private var cachedCanvas: Canvas? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recreateCache(w, h)
    }

    private fun recreateCache(w: Int, h: Int) {
        if (w > 0 && h > 0) {
            cachedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            cachedCanvas = Canvas(cachedBitmap!!)
            drawContent(cachedCanvas!!) // Draw properly using current properties
        }
    }


    fun setBagColors(c1: String, c2: String, c3: String, c4: String) {
        color1 = try { Color.parseColor(c1) } catch (e: Exception) { Color.WHITE }
        color2 = try { Color.parseColor(c2) } catch (e: Exception) { Color.WHITE }
        color3 = try { Color.parseColor(c3) } catch (e: Exception) { Color.WHITE }
        color4 = try { Color.parseColor(c4) } catch (e: Exception) { Color.WHITE }
        // Redraw cache
        if (width > 0 && height > 0) {
            if (cachedCanvas != null) {
                cachedCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                drawContent(cachedCanvas!!)
            } else {
                recreateCache(width, height)
            }
            invalidate()
        }
    }

    fun setBagColors(c1: Int, c2: Int, c3: Int, c4: Int) {
        color1 = c1
        color2 = c2
        color3 = c3
        color4 = c4
        // Redraw cache
        if (width > 0 && height > 0) {
            if (cachedCanvas != null) {
                cachedCanvas!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                drawContent(cachedCanvas!!)
            } else {
                 recreateCache(width, height)
            }
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw from cache - Super Fast!
        cachedBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    // Moved the logic from onDraw to drawContent for caching
    private fun drawContent(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        // คำนวณขนาดและตำแหน่งของกระเป๋าแต่ละใบ (2x2 grid)
        val padding = 16f
        val bagWidth = (width - padding * 3) / 2
        val bagHeight = (height - padding * 3) / 2

        // วาดกระเป๋า 4 ใบ
        drawSingleBag(canvas, padding, padding, bagWidth, bagHeight, color1, 1)
        drawSingleBag(canvas, padding * 2 + bagWidth, padding, bagWidth, bagHeight, color2, 2)
        drawSingleBag(canvas, padding, padding * 2 + bagHeight, bagWidth, bagHeight, color3, 3)
        drawSingleBag(canvas, padding * 2 + bagWidth, padding * 2 + bagHeight, bagWidth, bagHeight, color4, 4)
    }

    /**
     * วาดกระเป๋า Hermès 1 ใบ
     */
    private fun drawSingleBag(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        bagColor: Int,
        seed: Int
    ) {
        // วาดตัวกระเป๋า (body)
        drawBagBody(canvas, x, y, width, height, bagColor)
        
        // วาดฝาปิดโค้ง
        drawBagFlap(canvas, x, y, width, height, bagColor)
        
        // วาดหัวเข็มขัด H
        drawBagClasp(canvas, x, y, width, height)
        
        // วาด texture หนัง
        drawBagTexture(canvas, x, y, width, height, bagColor, seed)
    }

    /**
     * วาดตัวกระเป๋า (body)
     */
    private fun drawBagBody(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, bagColor: Int) {
        // สร้าง Gradient ให้ดูกระเป๋ามีนูนมีมิติ (ไล่สีจากสว่างไปเข้ม)
        val shader = LinearGradient(
            x, y + height * 0.4f, x, y + height,
            intArrayOf(bagColor, darkenColor(bagColor, 0.1f), darkenColor(bagColor, 0.2f)),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        paint.style = Paint.Style.FILL

        val cornerRadius = 12f
        val bodyHeight = height * 0.6f
        val bodyY = y + height * 0.4f

        val bodyPath = Path().apply {
            moveTo(x, bodyY)
            lineTo(x + width, bodyY)
            lineTo(x + width, bodyY + bodyHeight - cornerRadius)
            arcTo(
                RectF(x + width - cornerRadius * 2, bodyY + bodyHeight - cornerRadius * 2, x + width, bodyY + bodyHeight),
                0f, 90f
            )
            lineTo(x + cornerRadius, bodyY + bodyHeight)
            arcTo(
                RectF(x, bodyY + bodyHeight - cornerRadius * 2, x + cornerRadius * 2, bodyY + bodyHeight),
                90f, 90f
            )
            close()
        }
        canvas.drawPath(bodyPath, paint)
        paint.shader = null // Reset shader

        // **เพิ่มรอยเย็บ (Stitching)**
        drawStitching(canvas, bodyPath, bagColor)
    }

    /**
     * วาดฝาปิดโค้ง
     */
    private fun drawBagFlap(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, bagColor: Int) {
        // Gradient สำหรับฝาปิด (แสงเข้าจากด้านบน)
        val shader = LinearGradient(
            x, y, x, y + height * 0.45f,
            intArrayOf(lightenColor(bagColor, 0.1f), bagColor, darkenColor(bagColor, 0.15f)),
            null,
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
        paint.style = Paint.Style.FILL

        val flapHeight = height * 0.45f
        val cornerRadius = 12f

        val flapPath = Path().apply {
            // มุมบนซ้าย
            moveTo(x + cornerRadius, y)
            
            // ขอบบน
            lineTo(x + width - cornerRadius, y)
            arcTo(RectF(x + width - cornerRadius * 2, y, x + width, y + cornerRadius * 2), 270f, 90f)
            
            // ขอบขวา
            lineTo(x + width, y + flapHeight - cornerRadius)
            
            // มุมล่างขวาโค้ง
            arcTo(RectF(x + width - cornerRadius * 2.5f, y + flapHeight - cornerRadius * 1.5f, x + width, y + flapHeight), 0f, 45f)
            
            // ขอบล่างโค้ง (ฝาปิด)
            quadTo(x + width / 2, y + flapHeight + 8, x, y + flapHeight)
            
            // ขอบซ้าย
            lineTo(x, y + cornerRadius)
            arcTo(RectF(x, y, x + cornerRadius * 2, y + cornerRadius * 2), 180f, 90f)
            close()
        }
        
        canvas.drawPath(flapPath, paint)
        paint.shader = null
        
        // วาดเส้นขอบเงาๆ นิดหน่อย
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = darkenColor(bagColor, 0.3f)
        canvas.drawPath(flapPath, paint)

        // **เพิ่มรอยเย็บ (Stitching)**
        drawStitching(canvas, flapPath, bagColor)
    }

    /**
     * วาดรอยเย็บ (Stitching) โดยเว้นตรงกลางไว้ (สำหรับหัวเข็มขัด)
     */
    private fun drawStitching(canvas: Canvas, path: Path, bagColor: Int) {
        val stitchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            // สีด้าย (ใช้สีขาวนวล หรือสีอ่อนกว่ากระเป๋าเล็กน้อย)
            color = Color.parseColor("#E0E0E0") 
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f) // เส้นประลายเย็บ
            alpha = 200
        }
        
        // ย่อ path เข้ามาข้างในเล็กน้อยสำหรับรอยเย็บ
        val matrix = Matrix()
        val bounds = RectF()
        path.computeBounds(bounds, true)
        // Scale path เข้ามา 92% จากจุดกึ่งกลาง
        matrix.setScale(0.92f, 0.92f, bounds.centerX(), bounds.centerY())
        val stitchPath = Path()
        path.transform(matrix, stitchPath)

        // สร้าง Clip Path เพื่อเว้นตรงกลาง (หัวเข็มขัด)
        // เราจะตัดส่วนตรงกลางออกจากการวาด (ใช้ Op.DIFFERENCE)
        val claspAreaWidth = bounds.width() * 0.25f // กว้างกว่าตัว N นิดหน่อย
        val centerRect = RectF(
            bounds.centerX() - claspAreaWidth / 2,
            bounds.top, // เริ่มจากบนสุด
            bounds.centerX() + claspAreaWidth / 2,
            bounds.bottom // ถึงล่างสุด
        )
        
        canvas.save()
        canvas.clipRect(centerRect, Region.Op.DIFFERENCE) // วาดทุกที่ *ยกเว้น* ตรงกลาง
        canvas.drawPath(stitchPath, stitchPaint)
        canvas.restore()
    }

    /**
     * วาดหัวเข็มขัด N (Numbernice)
     */
    private fun drawBagClasp(canvas: Canvas, x: Float, y: Float, width: Float, height: Float) {
        val centerX = x + width / 2
        // ขยับลงมาให้เป็นที่เปิดกระเป๋า (อยู่ตรงขอบฝาปิดพอดี)
        val claspY = y + height * 0.48f 

        // Metal Gradient (สีเงินแวววาว) - ปรับมุมนิดหน่อยให้ดูมีมิติกับตัว N
        val metalShader = LinearGradient(
            centerX - width * 0.1f, claspY - height * 0.1f, centerX + width * 0.1f, claspY + height * 0.1f,
            intArrayOf(Color.parseColor("#F5F5F5"), Color.parseColor("#BDBDBD"), Color.parseColor("#FFFFFF"), Color.parseColor("#9E9E9E")),
            floatArrayOf(0f, 0.4f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )

        val claspPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = metalShader
            style = Paint.Style.FILL
        }

        val claspStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#757575")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val barWidth = width * 0.075f // ความกว้างของแท่งแนวตั้ง (ลดลง 50%)
        val claspHeight = height * 0.24f // ความสูงคงเดิม
        // เพิ่มระยะห่าง (gap) ให้กว้างขึ้นสำหรับตัว N
        val gap = barWidth * 0.6f 

        val topY = claspY - claspHeight / 2
        val bottomY = claspY + claspHeight / 2

        val leftInnerX = centerX - gap
        val leftOuterX = centerX - gap - barWidth
        val rightInnerX = centerX + gap
        val rightOuterX = centerX + gap + barWidth

        // ส่วนซ้าย (ขาตั้งซ้ายของ N)
        val leftRect = RectF(leftOuterX, topY, leftInnerX, bottomY)
        canvas.drawRoundRect(leftRect, 2f, 2f, claspPaint)
        canvas.drawRoundRect(leftRect, 2f, 2f, claspStrokePaint)

        // ส่วนขวา (ขาตั้งขวาของ N)
        val rightRect = RectF(rightInnerX, topY, rightOuterX, bottomY)
        canvas.drawRoundRect(rightRect, 2f, 2f, claspPaint)
        canvas.drawRoundRect(rightRect, 2f, 2f, claspStrokePaint)

        // เส้นทแยง (Diagonal ของ N)
        // เชื่อมจาก "บนซ้ายของช่องว่าง" ไป "ล่างขวาของช่องว่าง" (และกินเนื้อเข้าไปในแท่งเพื่อความเนียน)
        val diagonalThickness = barWidth * 0.85f // ความหนาเส้นทแยง
        val diagPath = Path().apply {
            moveTo(leftInnerX, topY) // จุดเริ่ม: มุมบนของแท่งซ้าย (ด้านใน)
            lineTo(leftInnerX - diagonalThickness, topY) // ขยับเข้าไปในแท่งซ้ายหน่อย
            lineTo(rightInnerX, bottomY) // จุดจบ: มุมล่างของแท่งขวา (ด้านใน)
            lineTo(rightInnerX + diagonalThickness, bottomY) // ขยับเข้าไปในแท่งขวาหน่อย
            close()
        }
        
        canvas.drawPath(diagPath, claspPaint)
        // วาดเส้นขอบ (อาจต้อง clip หรือวาดแยกเพื่อให้สวยงาม แต่วาดทับไปก่อนเพื่อความชัด)
        
        // เงาใต้หัวเข็มขัด
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40000000")
            style = Paint.Style.FILL
        }
        // เงาซ้าย
        canvas.drawRoundRect(RectF(leftRect.left + 2, leftRect.top + 2, leftRect.right + 2, leftRect.bottom + 2), 2f, 2f, shadowPaint)
        // เงาขวา (ทับซ้อนกันนิดหน่อยตรงกลางไม่เป็นไร เพราะเป็นเงา)
        canvas.drawRoundRect(RectF(rightRect.left + 2, rightRect.top + 2, rightRect.right + 2, rightRect.bottom + 2), 2f, 2f, shadowPaint)
        
        // วาด N อีกทีทับเงา (เพื่อให้ตัว N ลอยอยู่เหนือเงา)
        canvas.drawRoundRect(leftRect, 2f, 2f, claspPaint)
        canvas.drawRoundRect(leftRect, 2f, 2f, claspStrokePaint)
        canvas.drawRoundRect(rightRect, 2f, 2f, claspPaint)
        canvas.drawRoundRect(rightRect, 2f, 2f, claspStrokePaint)
        canvas.drawPath(diagPath, claspPaint)
    }

    /**
     * วาด Ostrich leather texture
     */
    private fun drawBagTexture(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        bagColor: Int,
        seed: Int
    ) {
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = darkenColor(bagColor, 0.2f) // จุดเข้มกว่าสีพื้น
        }

        val random = java.util.Random(seed.toLong())
        val dotCount = 40

        for (i in 0 until dotCount) {
            val dotX = x + random.nextFloat() * width
            val dotY = y + random.nextFloat() * height
            
            // ไม่วาดทับหัวเข็มขัด
            val centerX = x + width / 2
            val claspAreaWidth = width * 0.2f
            if (dotX > centerX - claspAreaWidth && dotX < centerX + claspAreaWidth && dotY < y + height * 0.3f) {
                continue
            }

            val dotRadius = 1f + random.nextFloat() * 1.5f
            canvas.drawCircle(dotX, dotY, dotRadius, dotPaint)
            
            // ไฮไลท์จุดเล็กๆ เพื่อให้ดูนูน
            dotPaint.color = lighteningColor(bagColor, 0.3f)
            canvas.drawCircle(dotX - 0.5f, dotY - 0.5f, dotRadius * 0.4f, dotPaint)
            dotPaint.color = darkenColor(bagColor, 0.2f) // คืนค่าสีเดิม
        }
    }

    /**
     * ทำให้สีเข้มขึ้น
     */
    private fun darkenColor(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= (1f - factor)
        return Color.HSVToColor(hsv)
    }

    /**
     * ทำให้สีสว่างขึ้น
     */
    private fun lightenColor(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] + factor).coerceAtMost(1f)
        return Color.HSVToColor(hsv)
    }
    
    // Alias for lightenColor used in texture
    private fun lighteningColor(color: Int, factor: Float): Int = lightenColor(color, factor)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (340 * resources.displayMetrics.density).toInt()
        val desiredHeight = (340 * resources.displayMetrics.density).toInt()

        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }
}
