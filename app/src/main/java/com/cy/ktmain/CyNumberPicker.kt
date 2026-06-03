package com.cy.ktmain

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.roundToInt

class CyNumberPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var minValue: Int = 0
        set(value) {
            field = value
            if (maxValue < value) maxValue = value
            setValueInternal(this.value, false)
        }

    var maxValue: Int = 0
        set(value) {
            field = value.coerceAtLeast(minValue)
            setValueInternal(this.value, false)
        }

    var value: Int
        get() = currentValue
        set(value) = setValueInternal(value, false)

    var wrapSelectorWheel: Boolean = true

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.NORMAL
        )
    }
    private val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = color(R.color.lab_accent_soft)
    }
    private val selectionStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = color(R.color.lab_accent)
    }
    private val selectedTextColor = color(R.color.lab_accent)
    private val normalTextColor = color(R.color.lab_text_secondary)
    private val scroller = OverScroller(context)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minimumFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maximumFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private val selectionBounds = RectF()

    private var listener: ((oldValue: Int, newValue: Int) -> Unit)? = null
    private var currentValue = 0
    private var scrollOffset = 0f
    private var lastTouchY = 0f
    private var lastScrollerY = 0
    private var velocityTracker: VelocityTracker? = null
    private var isDragging = false

    private val itemHeight: Float
        get() = dp(56f)

    init {
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        setValueInternal(0, false)
    }

    fun setOnValueChangedListener(listener: ((oldValue: Int, newValue: Int) -> Unit)?) {
        this.listener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (itemHeight * 3.2f + paddingTop + paddingBottom).roundToInt()
        val resolvedHeight = resolveSize(desiredHeight, heightMeasureSpec)
        val resolvedWidth = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        setMeasuredDimension(resolvedWidth, resolvedHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val selectionHeight = itemHeight
        selectionBounds.set(
            paddingLeft.toFloat(),
            centerY - selectionHeight / 2f,
            (width - paddingRight).toFloat(),
            centerY + selectionHeight / 2f
        )
        canvas.drawRoundRect(selectionBounds, dp(8f), dp(8f), selectionPaint)
        canvas.drawRoundRect(selectionBounds, dp(8f), dp(8f), selectionStrokePaint)

        val influenceDistance = itemHeight * 1.75f
        for (offset in -3..3) {
            val itemValue = valueForOffset(offset) ?: continue
            val y = centerY + offset * itemHeight + scrollOffset
            if (y < -itemHeight || y > height + itemHeight) continue

            val distance = abs(y - centerY)
            val influence = (1f - distance / influenceDistance).coerceIn(0f, 1f)
            val scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * influence
            textPaint.textSize = dp(BASE_TEXT_SIZE_SP * scale)
            textPaint.alpha = (MIN_ALPHA + (255 - MIN_ALPHA) * influence).roundToInt()
            textPaint.color = blendColor(normalTextColor, selectedTextColor, influence)

            val baseline = y - (textPaint.ascent() + textPaint.descent()) / 2f
            canvas.drawText(itemValue.toString(), centerX, baseline, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        velocityTracker = (velocityTracker ?: VelocityTracker.obtain()).also { it.addMovement(event) }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                if (!scroller.isFinished) scroller.abortAnimation()
                lastTouchY = event.y
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.y - lastTouchY
                if (!isDragging && abs(deltaY) > touchSlop) isDragging = true
                if (isDragging) {
                    applyScroll(deltaY)
                    lastTouchY = event.y
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                velocityTracker?.computeCurrentVelocity(1000, maximumFlingVelocity.toFloat())
                val velocityY = velocityTracker?.yVelocity ?: 0f
                if (isDragging && abs(velocityY) >= minimumFlingVelocity) {
                    startFling(velocityY)
                } else {
                    if (!isDragging) performClick()
                    settleToSelection()
                }
                releaseTouch()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                settleToSelection()
                releaseTouch()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun computeScroll() {
        if (!scroller.computeScrollOffset()) return
        val delta = scroller.currY - lastScrollerY
        lastScrollerY = scroller.currY
        applyScroll(delta.toFloat())
        if (scroller.isFinished) settleToSelection() else postInvalidateOnAnimation()
    }

    private fun startFling(velocityY: Float) {
        lastScrollerY = scrollOffset.roundToInt()
        scroller.fling(
            0,
            lastScrollerY,
            0,
            velocityY.roundToInt(),
            0,
            0,
            Int.MIN_VALUE / 4,
            Int.MAX_VALUE / 4
        )
        postInvalidateOnAnimation()
    }

    private fun settleToSelection() {
        if (scrollOffset > itemHeight / 2f) {
            changeValueBy(-1)
            scrollOffset -= itemHeight
        } else if (scrollOffset < -itemHeight / 2f) {
            changeValueBy(1)
            scrollOffset += itemHeight
        }
        val remaining = scrollOffset.roundToInt()
        if (remaining == 0) {
            invalidate()
            return
        }
        lastScrollerY = remaining
        scroller.startScroll(0, remaining, 0, -remaining, SETTLE_DURATION_MS)
        postInvalidateOnAnimation()
    }

    private fun applyScroll(deltaY: Float) {
        scrollOffset += deltaY
        while (scrollOffset >= itemHeight) {
            changeValueBy(-1)
            scrollOffset -= itemHeight
        }
        while (scrollOffset <= -itemHeight) {
            changeValueBy(1)
            scrollOffset += itemHeight
        }
        invalidate()
    }

    private fun changeValueBy(step: Int) {
        setValueInternal(currentValue + step, true)
    }

    private fun setValueInternal(requestedValue: Int, notify: Boolean) {
        val nextValue = normalizeValue(requestedValue)
        if (nextValue == currentValue) {
            invalidate()
            return
        }
        val previous = currentValue
        currentValue = nextValue
        contentDescription = nextValue.toString()
        invalidate()
        if (notify) listener?.invoke(previous, nextValue)
    }

    private fun valueForOffset(offset: Int): Int? {
        val rawValue = currentValue + offset
        if (wrapSelectorWheel) return normalizeValue(rawValue)
        return rawValue.takeIf { it in minValue..maxValue }
    }

    private fun normalizeValue(requestedValue: Int): Int {
        if (maxValue <= minValue) return minValue
        if (!wrapSelectorWheel) return requestedValue.coerceIn(minValue, maxValue)
        val size = maxValue - minValue + 1
        return minValue + ((requestedValue - minValue) % size + size) % size
    }

    private fun releaseTouch() {
        parent.requestDisallowInterceptTouchEvent(false)
        velocityTracker?.recycle()
        velocityTracker = null
        isDragging = false
    }

    private fun color(resource: Int): Int = ContextCompat.getColor(context, resource)

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun blendColor(startColor: Int, endColor: Int, amount: Float): Int {
        fun channel(start: Int, end: Int): Int = (start + (end - start) * amount).roundToInt()
        return android.graphics.Color.rgb(
            channel(android.graphics.Color.red(startColor), android.graphics.Color.red(endColor)),
            channel(android.graphics.Color.green(startColor), android.graphics.Color.green(endColor)),
            channel(android.graphics.Color.blue(startColor), android.graphics.Color.blue(endColor))
        )
    }

    private companion object {
        const val BASE_TEXT_SIZE_SP = 20f
        const val MIN_SCALE = 0.78f
        const val MAX_SCALE = 1.46f
        const val MIN_ALPHA = 100
        const val SETTLE_DURATION_MS = 180
    }
}
