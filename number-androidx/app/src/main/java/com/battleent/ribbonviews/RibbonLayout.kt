package com.battleent.ribbonviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.numberniceic.R

class RibbonLayout : RelativeLayout {

    private var headerText: String = ""
    private var bottomText: String = ""
    private var headerRibbonColor: Int = Color.RED
    private var bottomRibbonColor: Int = Color.RED
    private var headerTextColor: Int = Color.WHITE
    private var bottomTextColor: Int = Color.WHITE
    private var headerTextSize: Float = 16f // In pixels
    private var bottomTextSize: Float = 16f
    private var headerPadding: Float = 10f
    private var bottomPadding: Float = 10f
    private var headerRibbonRadius: Float = 0f
    private var showHeader: Boolean = false
    private var showBottom: Boolean = false
    private var bottomHeight: Float = 0f

    private val paintRibbon = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        setWillNotDraw(false)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.RibbonLayout)

            headerText = a.getString(R.styleable.RibbonLayout_header_text) ?: ""
            bottomText = a.getString(R.styleable.RibbonLayout_bottom_text) ?: ""
            
            headerRibbonColor = a.getColor(R.styleable.RibbonLayout_header_ribbonColor, Color.RED)
            bottomRibbonColor = a.getColor(R.styleable.RibbonLayout_bottom_ribbonColor, Color.RED)
            
            headerTextColor = a.getColor(R.styleable.RibbonLayout_header_textColor, Color.WHITE)
            bottomTextColor = a.getColor(R.styleable.RibbonLayout_bottom_textColor, Color.WHITE)
            
            // Handle Dimensions: Safely accept integer values (treating them as sp/dp) to support legacy XMLs
            headerTextSize = getTextSizeAttr(a, R.styleable.RibbonLayout_header_textSize, 40f) 
            bottomTextSize = getTextSizeAttr(a, R.styleable.RibbonLayout_bottom_textSize, 40f)
            
            headerPadding = getDimensionAttr(a, R.styleable.RibbonLayout_header_padding, 20f)
            bottomPadding = getDimensionAttr(a, R.styleable.RibbonLayout_bottom_padding, 20f)
            
            headerRibbonRadius = getDimensionAttr(a, R.styleable.RibbonLayout_header_ribbonRadius, 0f)
            
            bottomHeight = getDimensionAttr(a, R.styleable.RibbonLayout_bottom_height, 0f)

            showHeader = a.getBoolean(R.styleable.RibbonLayout_show_header, false)
            showBottom = a.getBoolean(R.styleable.RibbonLayout_show_bottom, false)

            a.recycle()
        }
    }

    private fun getDimensionAttr(a: android.content.res.TypedArray, index: Int, defValue: Float): Float {
        if (!a.hasValue(index)) return defValue
        try {
            // First try to resolve as a dimension (dp, sp, px, etc.)
            return a.getDimension(index, defValue)
        } catch (e: Exception) {
            // Fallback: Try as integer and treat as DP
            try {
                val value = a.getInt(index, 0)
                return value * resources.displayMetrics.density
            } catch (e2: Exception) {
                return defValue
            }
        }
    }

    private fun getTextSizeAttr(a: android.content.res.TypedArray, index: Int, defValue: Float): Float {
        if (!a.hasValue(index)) return defValue
        try {
             return a.getDimension(index, defValue)
        } catch (e: Exception) {
             // Fallback: Try as integer and treat as SP
             try {
                 val value = a.getInt(index, 0)
                 return value * resources.displayMetrics.scaledDensity
             } catch (e2: Exception) {
                 return defValue
             }
        }
    }

    fun setHeaderText(text: String) {
        this.headerText = text
        invalidate()
    }

    fun setHeaderRibbonColor(color: Int) {
        this.headerRibbonColor = color
        invalidate()
    }

    fun setBottomText(text: String) {
        this.bottomText = text
        invalidate()
    }

    fun setBottomRibbonColor(color: Int) {
        this.bottomRibbonColor = color
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        if (showHeader && headerText.isNotEmpty()) {
            drawRibbon(canvas, headerText, headerRibbonColor, headerTextColor, headerTextSize, headerPadding, headerRibbonRadius, isHeader = true)
        }

        if (showBottom && bottomText.isNotEmpty()) {
            drawRibbon(canvas, bottomText, bottomRibbonColor, bottomTextColor, bottomTextSize, bottomPadding, 0f, isHeader = false)
        }
    }

    private fun drawRibbon(canvas: Canvas, text: String, ribbonColor: Int, textColor: Int, textSize: Float, padding: Float, radius: Float, isHeader: Boolean) {
        paintText.color = textColor
        paintText.textSize = textSize
        
        val textWidth = paintText.measureText(text)
        val fontMetrics = paintText.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        
        val rectWidth = textWidth + (padding * 2)
        val rectHeight = textHeight + (padding * 2)

        paintRibbon.color = ribbonColor
        paintRibbon.style = Paint.Style.FILL

        val rect = RectF()
        if (isHeader) {
            // Draw at top-left
            rect.set(0f, 0f, rectWidth, rectHeight)
            
            if (radius > 0) {
                 // Draw simplified rounded rect
                 canvas.drawRoundRect(rect, radius, radius, paintRibbon)
            } else {
                 canvas.drawRect(rect, paintRibbon)
            }
            
             // Draw text centered
             // Baseline y = start y - ascent (ascent is negative)
             // y = padding + (textHeight/2) + (abs(ascent)/2) - (descent/2) ?
             // fontMetrics.ascent is top relative to baseline (negative).
             // Simple centering:
             val textY = padding - fontMetrics.ascent 
             canvas.drawText(text, padding, textY, paintText)
        } else {
            // Draw at bottom-left (consistent with header style, but at bottom)
            val yPos = height.toFloat() - rectHeight
            rect.set(0f, yPos, rectWidth, height.toFloat())
            canvas.drawRect(rect, paintRibbon)
            val textY = yPos + padding - fontMetrics.ascent
            canvas.drawText(text, padding, textY, paintText)
        }
    }
}
