package com.cy.ktmain.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.cy.ktmain.R
import kotlin.math.roundToInt

/**
 * 通用轮播指示器控件，支持指示器小圆点随滑动进度平滑拉伸、颜色渐变过渡。
 */
class CarouselIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var count: Int = 0
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
                invalidate()
            }
        }

    var normalWidthDp: Float = 8f
    var activeWidthDp: Float = 22f
    var dotHeightDp: Float = 8f
    var dotMarginDp: Float = 4f
    var cornerRadiusDp: Float = 20f

    var normalColor: Int = 0xB0BEC5 or (0xFF shl 24)
    var activeColor: Int = ContextCompat.getColor(context, R.color.lab_accent)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()

    private var currentIndex = 0
    private var nextIndex = 0
    private var scrollProgress = 0f

    /**
     * 随着滑动进度更新指示器的形变与混色动画
     */
    fun setScrollProgress(currentIndex: Int, nextIndex: Int, progress: Float) {
        this.currentIndex = currentIndex
        this.nextIndex = nextIndex
        this.scrollProgress = progress.coerceIn(0f, 1f)
        invalidate()
    }

    /**
     * 静态选中某个位置
     */
    fun setSelection(index: Int) {
        setScrollProgress(index, index, 0f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val nWidth = normalWidthDp * density
        val aWidth = activeWidthDp * density
        val h = dotHeightDp * density
        val margin = dotMarginDp * density

        val totalWidth = if (count <= 0) {
            0f
        } else {
            aWidth + ((count - 1) * nWidth) + (count * margin * 2) + paddingLeft + paddingRight
        }
        val totalHeight = h + paddingTop + paddingBottom

        val wSpec = resolveSize(totalWidth.roundToInt(), widthMeasureSpec)
        val hSpec = resolveSize(totalHeight.roundToInt(), heightMeasureSpec)
        setMeasuredDimension(wSpec, hSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (count <= 1) return

        val density = resources.displayMetrics.density
        val nWidth = normalWidthDp * density
        val aWidth = activeWidthDp * density
        val h = dotHeightDp * density
        val margin = dotMarginDp * density
        val radius = cornerRadiusDp * density

        val totalContentWidth = aWidth + (count - 1) * nWidth + count * (margin * 2)
        var startX = (width - totalContentWidth) / 2f
        val startY = (height - h) / 2f

        for (i in 0 until count) {
            val dotWidth: Float
            val dotColor: Int

            when (i) {
                currentIndex -> {
                    dotWidth = aWidth - (aWidth - nWidth) * scrollProgress
                    dotColor = blendColor(activeColor, normalColor, scrollProgress)
                }

                nextIndex -> {
                    dotWidth = nWidth + (aWidth - nWidth) * scrollProgress
                    dotColor = blendColor(normalColor, activeColor, scrollProgress)
                }

                else -> {
                    dotWidth = nWidth
                    dotColor = normalColor
                }
            }

            paint.color = dotColor
            rectF.left = startX + margin
            rectF.top = startY
            rectF.right = startX + margin + dotWidth
            rectF.bottom = startY + h
            canvas.drawRoundRect(rectF, radius, radius, paint)

            startX += dotWidth + margin * 2
        }
    }

    private fun blendColor(startColor: Int, endColor: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val startA = (startColor shr 24) and 0xff
        val startR = (startColor shr 16) and 0xff
        val startG = (startColor shr 8) and 0xff
        val startB = startColor and 0xff

        val endA = (endColor shr 24) and 0xff
        val endR = (endColor shr 16) and 0xff
        val endG = (endColor shr 8) and 0xff
        val endB = endColor and 0xff

        val a = (startA + (f * (endA - startA))).toInt()
        val r = (startR + (f * (endR - startR))).toInt()
        val g = (startG + (f * (endG - startG))).toInt()
        val b = (startB + (f * (endB - startB))).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
