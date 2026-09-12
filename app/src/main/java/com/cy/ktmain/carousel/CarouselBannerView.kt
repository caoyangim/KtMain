package com.cy.ktmain.carousel

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.cy.ktmain.R
import com.cy.ktmain.widgets.CarouselIndicatorView
import kotlin.math.abs

data class CarouselItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val tag: String,
    val startColor: Int,
    val endColor: Int
)

class CarouselBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val recyclerView = RecyclerView(context)
    private val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    private val snapHelper = LinearSnapHelper()
    private val indicatorView = CarouselIndicatorView(context)

    private val handler = Handler(Looper.getMainLooper())
    private var autoScrollRunnable: Runnable? = null

    private var items: List<CarouselItem> = emptyList()
    private var currentPosition: Int = 0
    private var isAutoScrollRunning: Boolean = false
    private var isAutoScrollEnabled: Boolean = true
    private var autoScrollIntervalMs: Long = 3000L

    private var onItemClickListener: ((CarouselItem, Int) -> Unit)? = null
    private var onPageChangeListener: ((adapterPosition: Int, realIndex: Int) -> Unit)? = null

    private val adapter = CarouselAdapter()

    init {
        clipChildren = false
        clipToPadding = false

        recyclerView.apply {
            layoutManager = this@CarouselBannerView.layoutManager
            adapter = this@CarouselBannerView.adapter
            clipToPadding = false
            clipChildren = false
            overScrollMode = OVER_SCROLL_NEVER
            setHasFixedSize(true)
            itemAnimator = null

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    updateItemTransforms()
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        updateCenteredPosition()
                        if (isAutoScrollEnabled) {
                            startAutoScroll()
                        }
                    } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        stopAutoScrollInternal()
                    }
                }
            })
        }
        snapHelper.attachToRecyclerView(recyclerView)
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        setupIndicatorContainer()
    }

    private fun setupIndicatorContainer() {
        val indicatorLp = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (12 * resources.displayMetrics.density).toInt()
        }
        addView(indicatorView, indicatorLp)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0) return
        val itemWidth = (w * ITEM_WIDTH_PERCENT).toInt()
        val horizontalPadding = (w - itemWidth) / 2
        recyclerView.setPadding(horizontalPadding, 0, horizontalPadding, 0)
        adapter.notifyDataSetChanged()

        post {
            if (currentPosition >= 0 && items.isNotEmpty()) {
                recyclerView.scrollToPosition(currentPosition)
                post {
                    snapToPosition(currentPosition)
                    updateItemTransforms()
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> stopAutoScrollInternal()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isAutoScrollEnabled) startAutoScroll()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    fun setItems(items: List<CarouselItem>) {
        this.items = items
        adapter.notifyDataSetChanged()
        if (items.isEmpty()) return

        val centerOffset = if (items.size > 1) {
            (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % items.size)
        } else {
            0
        }
        currentPosition = centerOffset

        recyclerView.scrollToPosition(centerOffset)
        post {
            snapToPosition(centerOffset)
            updateItemTransforms()
            updateIndicators()
            notifyPageChanged()
        }
    }

    fun setOnItemClickListener(listener: (CarouselItem, Int) -> Unit) {
        this.onItemClickListener = listener
    }

    fun setOnPageChangeListener(listener: (adapterPosition: Int, realIndex: Int) -> Unit) {
        this.onPageChangeListener = listener
    }

    fun startAutoScroll() {
        isAutoScrollEnabled = true
        stopAutoScrollInternal()
        if (items.size <= 1) return

        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (height > 0 && items.size > 1) {
                    currentPosition++
                    smoothScrollToTargetPosition(currentPosition)
                }
                handler.postDelayed(this, autoScrollIntervalMs)
            }
        }
        handler.postDelayed(autoScrollRunnable!!, autoScrollIntervalMs)
        isAutoScrollRunning = true
        notifyPageChanged()
    }

    fun stopAutoScroll() {
        isAutoScrollEnabled = false
        stopAutoScrollInternal()
        notifyPageChanged()
    }

    fun isAutoScrolling(): Boolean = isAutoScrollRunning && isAutoScrollEnabled

    fun scrollToNext() {
        if (items.size <= 1) return
        currentPosition++
        smoothScrollToTargetPosition(currentPosition)
    }

    fun scrollToPrevious() {
        if (items.size <= 1) return
        currentPosition--
        smoothScrollToTargetPosition(currentPosition)
    }

    fun scrollToAdapterPosition(targetPosition: Int, smoothScroll: Boolean = false) {
        if (items.isEmpty()) return
        currentPosition = targetPosition
        if (smoothScroll) {
            smoothScrollToTargetPosition(targetPosition)
        } else {
            recyclerView.scrollToPosition(targetPosition)
            post {
                snapToPosition(targetPosition)
                updateItemTransforms()
                updateCenteredPosition()
            }
        }
    }

    fun getCurrentRealIndex(): Int = getRealIndexForPosition(currentPosition)

    fun getCurrentAdapterPosition(): Int = currentPosition

    fun getRealCount(): Int = items.size

    private fun getRealIndexForPosition(position: Int): Int {
        if (items.isEmpty()) return 0
        return ((position % items.size) + items.size) % items.size
    }

    private fun stopAutoScrollInternal() {
        autoScrollRunnable?.let { handler.removeCallbacks(it) }
        autoScrollRunnable = null
        isAutoScrollRunning = false
    }

    private fun smoothScrollToTargetPosition(targetPosition: Int) {
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 300f / displayMetrics.densityDpi
            }

            override fun calculateDtToFit(
                viewStart: Int,
                viewEnd: Int,
                boxStart: Int,
                boxEnd: Int,
                snapPreference: Int
            ): Int {
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
            }
        }
        smoothScroller.targetPosition = targetPosition
        layoutManager.startSmoothScroll(smoothScroller)
    }

    private fun snapToPosition(position: Int) {
        val view = layoutManager.findViewByPosition(position) ?: return
        val distances = snapHelper.calculateDistanceToFinalSnap(layoutManager, view) ?: return
        recyclerView.scrollBy(distances[0], distances[1])
    }

    private fun updateCenteredPosition() {
        val snapView = snapHelper.findSnapView(layoutManager) ?: return
        val pos = layoutManager.getPosition(snapView)
        if (pos == RecyclerView.NO_POSITION) return

        val n = items.size
        if (n > 1) {
            val safeThreshold = n * 1000
            if (pos < safeThreshold || pos > Int.MAX_VALUE - safeThreshold) {
                val centerOffset = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % n)
                val realIndex = ((pos % n) + n) % n
                val newPos = centerOffset + realIndex
                currentPosition = newPos
                recyclerView.scrollToPosition(newPos)
                recyclerView.post {
                    snapToPosition(newPos)
                    updateItemTransforms()
                }
            } else {
                currentPosition = pos
            }
        } else {
            currentPosition = pos
        }

        notifyPageChanged()
        updateIndicators()
        updateItemTransforms()
    }

    private fun updateItemTransforms() {
        val width = recyclerView.width
        if (width <= 0 || items.isEmpty()) return

        val centerX = width / 2f
        val itemWidth = width * ITEM_WIDTH_PERCENT
        if (itemWidth <= 0f) return

        var maxFactor = -1f
        var maxFactorChildOffset = 0f
        var maxFactorChildPos = RecyclerView.NO_POSITION

        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val childCenterX = (child.left + child.right) / 2f
            val distance = abs(centerX - childCenterX)

            val factor = (1f - distance / itemWidth).coerceIn(0f, 1f)

            val scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * factor

            val cardBg = child.findViewById<View>(R.id.cardBackground)
            if (cardBg != null && cardBg.height > 0) {
                child.pivotX = child.width / 2f
                child.pivotY = cardBg.top + cardBg.height / 2f
            } else {
                child.pivotX = child.width / 2f
                child.pivotY = child.height / 2f
            }

            child.scaleX = scale
            child.scaleY = scale

            val alpha = if (distance <= itemWidth) {
                MIN_ALPHA + (1f - MIN_ALPHA) * factor
            } else {
                val fadeOutFactor = (1f - (distance - itemWidth) / itemWidth).coerceIn(0f, 1f)
                MIN_ALPHA * fadeOutFactor
            }
            child.alpha = alpha

            child.translationZ = factor * 12f

            val ratioView = child.findViewById<TextView>(R.id.cardRatio)
            ratioView?.text = String.format(java.util.Locale.US, "%.2f", factor)

            if (factor > maxFactor) {
                maxFactor = factor
                maxFactorChildOffset = (centerX - childCenterX) / itemWidth
                maxFactorChildPos = layoutManager.getPosition(child)
            }
        }

        val count = items.size
        if (count > 1 && maxFactorChildPos != RecyclerView.NO_POSITION) {
            val currentRealIndex = getRealIndexForPosition(maxFactorChildPos)
            val nextRealIndex = if (maxFactorChildOffset >= 0) {
                (currentRealIndex + 1) % count
            } else {
                (currentRealIndex - 1 + count) % count
            }
            val progress = (1f - maxFactor).coerceIn(0f, 1f)
            indicatorView.setScrollProgress(currentRealIndex, nextRealIndex, progress)
        }
    }

    private fun updateIndicators() {
        indicatorView.count = items.size
        indicatorView.setSelection(getCurrentRealIndex())
    }

    private fun notifyPageChanged() {
        onPageChangeListener?.invoke(currentPosition, getCurrentRealIndex())
    }

    override fun onDetachedFromWindow() {
        stopAutoScrollInternal()
        super.onDetachedFromWindow()
    }

    private inner class CarouselAdapter : RecyclerView.Adapter<CarouselAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_carousel_card, parent, false)

            val parentWidth = parent.width.takeIf { it > 0 } ?: parent.resources.displayMetrics.widthPixels
            val itemWidth = (parentWidth * ITEM_WIDTH_PERCENT).toInt()
            val lp = view.layoutParams
            lp.width = itemWidth
            view.layoutParams = lp

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (items.isEmpty()) return
            val realIndex = getRealIndexForPosition(position)
            holder.bind(items[realIndex], realIndex, position)

            val parentWidth = recyclerView.width.takeIf { it > 0 } ?: recyclerView.resources.displayMetrics.widthPixels
            val targetWidth = (parentWidth * ITEM_WIDTH_PERCENT).toInt()
            if (holder.itemView.layoutParams.width != targetWidth) {
                holder.itemView.layoutParams.width = targetWidth
                holder.itemView.requestLayout()
            }
            recyclerView.post { updateItemTransforms() }
        }

        override fun getItemCount(): Int {
            if (items.isEmpty()) return 0
            if (items.size == 1) return 1
            return Int.MAX_VALUE
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardBackground: RelativeLayout = itemView.findViewById(R.id.cardBackground)
            private val cardTag: TextView = itemView.findViewById(R.id.cardTag)
            private val cardTitle: TextView = itemView.findViewById(R.id.cardTitle)
            private val cardSubtitle: TextView = itemView.findViewById(R.id.cardSubtitle)

            fun bind(item: CarouselItem, realIndex: Int, adapterPosition: Int) {
                cardTag.text = item.tag
                cardTitle.text = item.title
                cardSubtitle.text = item.subtitle

                val gradient = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(item.startColor, item.endColor)
                ).apply {
                    cornerRadius = 12f * itemView.resources.displayMetrics.density
                }
                cardBackground.background = gradient

                itemView.setOnClickListener {
                    if (adapterPosition != currentPosition) {
                        currentPosition = adapterPosition
                        smoothScrollToTargetPosition(adapterPosition)
                    } else {
                        onItemClickListener?.invoke(item, realIndex)
                    }
                }
            }
        }
    }

    private companion object {
        const val ITEM_WIDTH_PERCENT = 1f / 3f
        const val MIN_SCALE = 0.88f
        const val MAX_SCALE = 1.00f
        const val MIN_ALPHA = 0.85f
    }
}
