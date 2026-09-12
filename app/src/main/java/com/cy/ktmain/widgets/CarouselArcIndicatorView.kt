package com.cy.ktmain.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 贴边二次贝塞尔曲线指示器控件：
 * 1. 彻底移除人工间距限制，完全贴合 View 边界与 Padding 绘制 (0, h -> w/2, 0 -> w, h)。
 * 2. 沿二次贝塞尔曲线四等分分布 3 个基准锚点，Item 点位与 ID 文字 1:1 沿曲线轨迹平滑动画移动。
 */
class CarouselArcIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var itemIds: List<String> = emptyList()
    private var currentIndex: Int = 0
    private var nextIndex: Int = 0
    private var progress: Float = 0f

    /**
     * 实时 dir、continuousRealIndex、progress、currentIndex、nextIndex 与贝塞尔轨迹 t 参数计算结果监听器
     */
    var onDirChangeListener: ((dir: Float, continuousRealIndex: Float, progress: Float, currentIndex: Int, nextIndex: Int, tLog: String) -> Unit)? = null

    // 颜色配置（高对比度 Indigo & Slate 主题）
    var arcColor: Int = 0xFF6366F1.toInt()       // 不透明鲜艳 Indigo 弧线
    var baseDotOuterColor: Int = 0xC7D2FE or (0x80 shl 24)
    var baseDotInnerColor: Int = 0x6366F1 or (0xFF shl 24)
    var normalDotColor: Int = 0x6366F1 or (0xFF shl 24)
    var highlightDotColor: Int = 0x312E81 or (0xFF shl 24)
    var glowColor: Int = 0x818CF8 or (0x50 shl 24)
    var normalTextColor: Int = 0x475569 or (0xFF shl 24)
    var highlightTextColor: Int = 0x1E1B4B or (0xFF shl 24)
    var badgeBgColor: Int = 0x6366F1 or (0x20 shl 24)

    // 尺寸配置 (dp / sp)
    var normalDotRadiusDp: Float = 5f
    var highlightDotRadiusDp: Float = 9f
    var normalTextSizeSp: Float = 13f
    var highlightTextSizeSp: Float = 18f

    // 画笔
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val baseRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val baseDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val badgeRectF = RectF()
    private val quadPath = Path()

    /**
     * 设置数据集
     * @param items 泛型数据列表
     * @param itemIdFetcher 获取 Item ID 的方法/ Lambda
     */
    fun <T> setItems(items: List<T>, itemIdFetcher: (T) -> String) {
        this.itemIds = items.map(itemIdFetcher)
        invalidate()
    }

    /**
     * 与标准 Indicator 保持一致的统一进度回调方法
     */
    fun setScrollProgress(currentIndex: Int, nextIndex: Int, progress: Float) {
        this.currentIndex = currentIndex
        this.nextIndex = nextIndex
        this.progress = progress.coerceIn(0f, 1f)
        invalidate()
    }

    /**
     * 静态选中某个位置
     */
    fun setSelection(index: Int) {
        setScrollProgress(index, index, 0f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val density = resources.displayMetrics.density
        val defaultHeight = (88 * density).roundToInt()
        val height = getDefaultSize(defaultHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (itemIds.isEmpty()) return

        val count = itemIds.size
        val density = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()

        // 彻底不留任何人工额外间距，完全根据控件边界与 XML 设定的 Padding 贴边绘制
        val left = paddingLeft.toFloat()
        val right = w - paddingRight.toFloat()
        val top = paddingTop.toFloat()
        val bottom = h - paddingBottom.toFloat()

        val usableWidth = right - left
        val controlX = (left + right) / 2f
        // 顶点正好经过 (w/2, top)，导出控制点 Y：
        val controlY = 2f * top - bottom

        // 1. 绘制完全贴边的二次贝塞尔曲线底线 (起点 (left, bottom) -> 顶点 (controlX, top) -> 终点 (right, bottom))
        arcPaint.color = arcColor
        arcPaint.strokeWidth = 3.5f * density
        quadPath.apply {
            rewind()
            moveTo(left, bottom)
            quadTo(controlX, controlY, right, bottom)
        }
        canvas.drawPath(quadPath, arcPaint)

        // 求解贝塞尔曲线上参数 t (0.0 ~ 1.0) 对应的 Y 坐标（X 坐标为 left + t * usableWidth）
        fun getBezierY(t: Float): Float {
            val oneMinusT = 1f - t
            return oneMinusT * oneMinusT * bottom + 2f * oneMinusT * t * controlY + t * t * bottom
        }

        // 2. 绘制曲线上的 3 个基准点锚点 (t = 0.25, 0.50, 0.75，将曲线精准四等分)
        val baseTList = floatArrayOf(0.25f, 0.50f, 0.75f)
        baseRingPaint.strokeWidth = 1.5f * density
        baseRingPaint.color = baseDotOuterColor
        baseDotPaint.color = baseDotInnerColor

        for (bt in baseTList) {
            val bx = left + bt * usableWidth
            val by = getBezierY(bt)
            canvas.drawCircle(bx, by, 5f * density, baseRingPaint)
            canvas.drawCircle(bx, by, 2.5f * density, baseDotPaint)
        }

        // 3. 滑动方向与连续位置计算
        val dir = when {
            currentIndex == nextIndex || progress == 0f -> 0f
            (nextIndex - currentIndex + count) % count == 1 -> +1f
            (currentIndex - nextIndex + count) % count == 1 -> -1f
            nextIndex > currentIndex -> +1f
            else -> -1f
        }

        val continuousRealIndex = (currentIndex.toFloat() + dir * progress + count) % count

        val normalRadius = normalDotRadiusDp * density
        val highlightRadius = highlightDotRadiusDp * density
        val normalTextSize = normalTextSizeSp * density
        val highlightTextSize = highlightTextSizeSp * density

        val tLogList = mutableListOf<String>()

        for (i in 0 until count) {
            var diff = i.toFloat() - continuousRealIndex
            while (diff < -count / 2f) diff += count
            while (diff > count / 2f) diff -= count

            val t = 0.5f + diff * 0.25f
            if (t < -0.08f || t > 1.08f) continue

            val itemId = itemIds[i]
            tLogList.add(String.format(java.util.Locale.US, "ID %s: t=%.3f", itemId, t))

            val px = left + t * usableWidth
            val py = getBezierY(t)

            val highlightFactor = (1f - 4f * abs(t - 0.5f)).coerceIn(0f, 1f)
            val alpha = when {
                t < 0.25f -> ((t + 0.08f) / 0.33f).coerceIn(0f, 1f)
                t > 0.75f -> ((1.08f - t) / 0.33f).coerceIn(0f, 1f)
                else -> 1f
            }

            val alphaInt = (alpha * 255).toInt()
            if (alphaInt <= 0) continue

            val currentRadius = normalRadius + (highlightRadius - normalRadius) * highlightFactor
            val currentColor = blendColor(normalDotColor, highlightDotColor, highlightFactor)

            // 发光外环
            if (highlightFactor > 0f) {
                val glowRadius = currentRadius + 5f * density * highlightFactor
                val glowAlpha = (0x50 * highlightFactor * alpha).toInt().coerceIn(0, 255)
                glowPaint.color = (glowColor and 0x00FFFFFF) or (glowAlpha shl 24)
                canvas.drawCircle(px, py, glowRadius, glowPaint)
            }

            // 实体圆点
            dotPaint.color = currentColor
            dotPaint.alpha = alphaInt
            canvas.drawCircle(px, py, currentRadius, dotPaint)

            // ID 文字
            val idText = "ID $itemId"

            val currentTextSize = normalTextSize + (highlightTextSize - normalTextSize) * highlightFactor
            val currentTextColor = blendColor(normalTextColor, highlightTextColor, highlightFactor)

            textPaint.textSize = currentTextSize
            textPaint.color = currentTextColor
            textPaint.alpha = alphaInt
            textPaint.typeface = if (highlightFactor > 0.4f) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

            val fontMetrics = textPaint.fontMetrics
            val textY = py + currentRadius + 10f * density + (fontMetrics.bottom - fontMetrics.top) / 3f

            // Pill 气泡框
            if (highlightFactor > 0.2f) {
                val textWidth = textPaint.measureText(idText)
                val padH = 8f * density * highlightFactor
                val padV = 3f * density * highlightFactor
                badgeRectF.set(
                    px - textWidth / 2f - padH,
                    textY + fontMetrics.top - padV,
                    px + textWidth / 2f + padH,
                    textY + fontMetrics.bottom + padV
                )
                val bgAlpha = (0x30 * highlightFactor * alpha).toInt().coerceIn(0, 255)
                badgePaint.color = (badgeBgColor and 0x00FFFFFF) or (bgAlpha shl 24)
                val corner = 8f * density
                canvas.drawRoundRect(badgeRectF, corner, corner, badgePaint)
            }

            canvas.drawText(idText, px, textY, textPaint)
        }

        val tLogStr = if (tLogList.isEmpty()) "无" else tLogList.joinToString(" | ")
        onDirChangeListener?.invoke(dir, continuousRealIndex, progress, currentIndex, nextIndex, tLogStr)
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
