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
    private val itemWidthPx = (56f * resources.displayMetrics.density).roundToInt()
    private var frames: List<FrameItem> = emptyList()
    private var snapped = false
    private var selectedIndex = -1

    data class FrameItem(val timeMs: Long, val bitmap: Bitmap?)

    init {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        setAdapter(adapter)
        itemAnimator = null
    }

    fun submit(items: List<FrameItem>) {
        frames = items
        adapter.notifyDataSetChanged()
        post {
            if (!snapped && items.isNotEmpty()) {
                selectPosition(0, animate = false)
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0) return
        // 左右留白 = (宽 - item宽)/2，保证首末帧也能滚动对齐中线（点击的滚动增量不被边界钳制）
        val pad = ((w - itemWidthPx) / 2f).roundToInt().coerceAtLeast(0)
        if (paddingLeft != pad || paddingRight != pad) {
            setPadding(pad, paddingTop, pad, paddingBottom)
        }
    }

    /** 点击选中：立即更新选中边框与封面回调，再尽力把该帧平滑滚动对齐中线。 */
    private fun selectPosition(position: Int, animate: Boolean = true) {
        if (position !in frames.indices || position == selectedIndex) return
        val old = selectedIndex
        selectedIndex = position
        if (old in frames.indices) adapter.notifyItemChanged(old)
        adapter.notifyItemChanged(position)
        frames.getOrNull(position)?.let {
            onCoverChangedListener?.onCoverSelected(position, it.timeMs)
        }
        val lm = layoutManager as? LinearLayoutManager ?: return
        val child = lm.findViewByPosition(position)
        if (child != null) {
            val dx = (child.left + child.right) / 2 - width / 2
            if (dx != 0) {
                if (animate) smoothScrollBy(dx, 0) else scrollBy(dx, 0)
            }
        } else {
            scrollToPosition(position)
            post {
                val c = (layoutManager as? LinearLayoutManager)?.findViewByPosition(position)
                    ?: return@post
                val dx = (c.left + c.right) / 2 - width / 2
                if (dx != 0) scrollBy(dx, 0)
            }
        }
    }

    private inner class FrameAdapter : Adapter<FrameHolder>() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FrameHolder {
            val view = FrameThumbView(parent.context)
            val lp = LayoutParams(itemWidthPx, LayoutParams.MATCH_PARENT)
            view.layoutParams = lp
            return FrameHolder(view)
        }

        override fun getItemCount(): Int = frames.size

        override fun onBindViewHolder(holder: FrameHolder, position: Int) {
            holder.bind(frames[position], position, position == selectedIndex)
            holder.itemView.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos == NO_POSITION || pos == selectedIndex) return@setOnClickListener
                selectPosition(pos)
            }
        }
    }

    private class FrameHolder(private val view: FrameThumbView) : ViewHolder(view) {
        fun bind(item: FrameItem, position: Int, selected: Boolean) {
            view.bind(item.bitmap, position, selected)
        }
    }

    private class FrameThumbView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 10f * resources.displayMetrics.density
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF6C5CE7.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f * resources.displayMetrics.density
        }
        private val src = Rect()
        private val dst = Rect()
        private var bitmap: Bitmap? = null
        private var label: String = ""
        private var selected: Boolean = false
        private val gap = 1f * resources.displayMetrics.density
        private val borderHalf = borderPaint.strokeWidth / 2f

        fun bind(bitmap: Bitmap?, position: Int, selected: Boolean) {
            this.bitmap = bitmap
            this.label = (position + 1).toString()
            this.selected = selected
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
            if (selected) {
                canvas.drawRect(
                    borderHalf, borderHalf,
                    width - borderHalf, height - borderHalf,
                    borderPaint
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        return super.onTouchEvent(e)
    }
}
