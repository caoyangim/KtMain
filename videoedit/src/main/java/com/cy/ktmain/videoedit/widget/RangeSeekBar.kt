package com.cy.ktmain.videoedit.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Two-thumb range bar for trim start/end, normalized to [0, 1].
 */
class RangeSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    interface OnRangeChangeListener {
        fun onRangeChanged(startRatio: Float, endRatio: Float, fromUser: Boolean)
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF3A3A3A.toInt()
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6C5CE7.toInt()
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
    }
    private val barRect = RectF()
    private val selectedRect = RectF()

    private val thumbRadius: Float
    private val barHeight: Float

    var startRatio: Float = 0f
        private set
    var endRatio: Float = 1f
        private set

    var minRange: Float = 0.02f
    var listener: OnRangeChangeListener? = null

    private var activeThumb = 0 // 0 none, 1 start, 2 end

    init {
        val density = resources.displayMetrics.density
        thumbRadius = 12f * density
        barHeight = 6f * density
    }

    fun setRange(start: Float, end: Float, notify: Boolean = true) {
        val s = start.coerceIn(0f, 1f)
        val e = end.coerceIn(0f, 1f)
        startRatio = min(s, e)
        endRatio = max(s, e)
        if (endRatio - startRatio < minRange) {
            endRatio = (startRatio + minRange).coerceAtMost(1f)
            startRatio = (endRatio - minRange).coerceAtLeast(0f)
        }
        invalidate()
        if (notify) listener?.onRangeChanged(startRatio, endRatio, false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cy = height / 2f
        val left = paddingLeft + thumbRadius
        val right = width - paddingRight - thumbRadius
        val barWidth = max(right - left, 1f)

        barRect.set(left, cy - barHeight / 2f, right, cy + barHeight / 2f)
        canvas.drawRoundRect(barRect, barHeight, barHeight, barPaint)

        val selLeft = left + barWidth * startRatio
        val selRight = left + barWidth * endRatio
        selectedRect.set(selLeft, cy - barHeight / 2f, selRight, cy + barHeight / 2f)
        canvas.drawRoundRect(selectedRect, barHeight, barHeight, selectedPaint)

        canvas.drawCircle(selLeft, cy, thumbRadius, thumbPaint)
        canvas.drawCircle(selRight, cy, thumbRadius, thumbPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeThumb = closestThumb(event.x)
                parent?.requestDisallowInterceptTouchEvent(true)
                updateFromX(event.x, fromUser = true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeThumb != 0) {
                    updateFromX(event.x, fromUser = true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeThumb = 0
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun closestThumb(x: Float): Int {
        val left = paddingLeft + thumbRadius
        val right = width - paddingRight - thumbRadius
        val barWidth = max(right - left, 1f)
        val startX = left + barWidth * startRatio
        val endX = left + barWidth * endRatio
        return if (abs(x - startX) <= abs(x - endX)) 1 else 2
    }

    private fun updateFromX(x: Float, fromUser: Boolean) {
        val left = paddingLeft + thumbRadius
        val right = width - paddingRight - thumbRadius
        val barWidth = max(right - left, 1f)
        val ratio = ((x - left) / barWidth).coerceIn(0f, 1f)
        when (activeThumb) {
            1 -> {
                startRatio = min(ratio, endRatio - minRange).coerceAtLeast(0f)
            }
            2 -> {
                endRatio = max(ratio, startRatio + minRange).coerceAtMost(1f)
            }
            else -> return
        }
        invalidate()
        listener?.onRangeChanged(startRatio, endRatio, fromUser)
    }
}
