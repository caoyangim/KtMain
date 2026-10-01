package com.cy.ktmain.videoedit.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Horizontal film-strip of frames; the item under the center line is the cover.
 */
class FrameStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RecyclerView(context, attrs) {

    interface OnCoverChangedListener {
        fun onCoverSelected(index: Int, timeMs: Long)
    }

    var onCoverChangedListener: OnCoverChangedListener? = null

    private val adapter = FrameAdapter()
    private var frames: List<FrameItem> = emptyList()
    private var snapped = false

    data class FrameItem(val timeMs: Long, val bitmap: Bitmap?)

    init {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        setAdapter(adapter)
        itemAnimator = null
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == SCROLL_STATE_IDLE) {
                    snapToCenter()
                }
            }
        })
        post { snapToCenter(notify = false) }
    }

    fun submit(items: List<FrameItem>) {
        frames = items
        adapter.notifyDataSetChanged()
        post {
            if (!snapped && items.isNotEmpty()) {
                snapToCenter(notify = true)
                snapped = true
            }
        }
    }

    fun centerIndex(): Int {
        if (frames.isEmpty()) return 0
        val lm = layoutManager as? LinearLayoutManager ?: return 0
        val center = width / 2f
        var best = 0
        var bestDist = Float.MAX_VALUE
        for (i in 0 until lm.itemCount) {
            val child = lm.findViewByPosition(i) ?: continue
            val childCenter = (child.left + child.right) / 2f
            val dist = abs(childCenter - center)
            if (dist < bestDist) {
                bestDist = dist
                best = i
            }
        }
        return best
    }

    fun centerFrame(): FrameItem? = frames.getOrNull(centerIndex())

    private fun snapToCenter(notify: Boolean = true) {
        if (frames.isEmpty()) return
        val index = centerIndex()
        val lm = layoutManager as? LinearLayoutManager ?: return
        val child = lm.findViewByPosition(index)
        if (child != null) {
            val dx = (child.left + child.right) / 2 - width / 2
            if (abs(dx) > 0) scrollBy(dx, 0)
        } else {
            scrollToPosition(index)
        }
        if (notify) {
            frames.getOrNull(index)?.let {
                onCoverChangedListener?.onCoverSelected(index, it.timeMs)
            }
        }
    }

    private inner class FrameAdapter : Adapter<FrameHolder>() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FrameHolder {
            val view = FrameThumbView(parent.context)
            val lp = LayoutParams(
                (parent.resources.displayMetrics.density * 56).roundToInt(),
                LayoutParams.MATCH_PARENT
            )
            view.layoutParams = lp
            return FrameHolder(view)
        }

        override fun getItemCount(): Int = frames.size

        override fun onBindViewHolder(holder: FrameHolder, position: Int) {
            holder.bind(frames[position], position)
        }
    }

    private class FrameHolder(private val view: FrameThumbView) : ViewHolder(view) {
        fun bind(item: FrameItem, position: Int) {
            view.bind(item.bitmap, position)
        }
    }

    private class FrameThumbView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 10f * resources.displayMetrics.density
        }
        private val src = Rect()
        private val dst = Rect()
        private var bitmap: Bitmap? = null
        private var label: String = ""
        private val gap = 1f * resources.displayMetrics.density

        fun bind(bitmap: Bitmap?, position: Int) {
            this.bitmap = bitmap
            this.label = (position + 1).toString()
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            paint.color = if (bitmap == null) 0xFF2A2A2A.toInt() else Color.BLACK
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            val bmp = bitmap
            if (bmp != null && !bmp.isRecycled) {
                src.set(0, 0, bmp.width, bmp.height)
                dst.set(gap.toInt(), gap.toInt(), width - gap.toInt(), height - gap.toInt())
                canvas.drawBitmap(bmp, src, dst, paint)
            }
            canvas.drawText(label, width / 2f, height - 8f * resources.displayMetrics.density, textPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        return super.onTouchEvent(e)
    }
}
