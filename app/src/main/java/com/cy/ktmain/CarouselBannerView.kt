package com.cy.ktmain

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
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
    private val indicatorContainer = LinearLayout(context)

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
        indicatorContainer.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
        }
        val indicatorLp = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = (12 * resources.displayMetrics.density).toInt()
        }
        addView(indicatorContainer, indicatorLp)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0) return
        val itemWidth = (w * ITEM_WIDTH_PERCENT).toInt()
        val horizontalPadding = (w - itemWidth) / 2
        recyclerView.setPadding(horizontalPadding, 0, horizontalPadding, 0)

        post {
            if (currentPosition > 0) {
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
        if (items.isEmpty()) return

        val centerOffset = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % items.size)
        currentPosition = centerOffset

        recyclerView.scrollToPosition(centerOffset)
        post {
            snapToPosition(centerOffset)
            updateItemTransforms()
            rebuildIndicators()
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
        if (items.isEmpty()) return

        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (height > 0 && items.isNotEmpty()) {
                    currentPosition++
                    recyclerView.smoothScrollToPosition(currentPosition)
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
        if (items.isEmpty()) return
        currentPosition++
        recyclerView.smoothScrollToPosition(currentPosition)
    }

    fun scrollToPrevious() {
        if (items.isEmpty()) return
        currentPosition--
        recyclerView.smoothScrollToPosition(currentPosition)
    }

    fun getCurrentRealIndex(): Int {
        if (items.isEmpty()) return 0
        return ((currentPosition % items.size) + items.size) % items.size
    }

    fun getCurrentAdapterPosition(): Int = currentPosition

    fun getRealCount(): Int = items.size

    private fun stopAutoScrollInternal() {
        autoScrollRunnable?.let { handler.removeCallbacks(it) }
        autoScrollRunnable = null
        isAutoScrollRunning = false
    }

    private fun snapToPosition(position: Int) {
        val view = layoutManager.findViewByPosition(position) ?: return
        val distances = snapHelper.calculateDistanceToFinalSnap(layoutManager, view) ?: return
        recyclerView.scrollBy(distances[0], distances[1])
    }

    private fun updateCenteredPosition() {
        val snapView = snapHelper.findSnapView(layoutManager) ?: return
        val pos = layoutManager.getPosition(snapView)
        if (pos != RecyclerView.NO_POSITION && pos != currentPosition) {
            currentPosition = pos
            notifyPageChanged()
            updateIndicators()
        }
        updateItemTransforms()
    }

    private fun updateItemTransforms() {
        val width = recyclerView.width
        if (width <= 0) return

        val centerX = width / 2f
        val itemWidth = width * ITEM_WIDTH_PERCENT
        val maxDistance = itemWidth * 0.95f

        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val childCenterX = (child.left + child.right) / 2f
            val distance = abs(centerX - childCenterX)
            val factor = (1f - distance / maxDistance).coerceIn(0f, 1f)

            val scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * factor
            child.scaleX = scale
            child.scaleY = scale

            val alpha = MIN_ALPHA + (1f - MIN_ALPHA) * factor
            child.alpha = alpha

            child.translationZ = factor * 12f
        }
    }

    private fun rebuildIndicators() {
        indicatorContainer.removeAllViews()
        val count = items.size
        if (count <= 1) return

        val density = resources.displayMetrics.density
        val normalWidth = (8 * density).toInt()
        val activeWidth = (22 * density).toInt()
        val height = (8 * density).toInt()
        val margin = (4 * density).toInt()

        val realIndex = getCurrentRealIndex()

        for (i in 0 until count) {
            val dot = View(context)
            val isSelected = (i == realIndex)
            val dotWidth = if (isSelected) activeWidth else normalWidth

            val lp = LinearLayout.LayoutParams(dotWidth, height).apply {
                setMargins(margin, 0, margin, 0)
            }
            dot.layoutParams = lp
            dot.background = createDotDrawable(isSelected)
            indicatorContainer.addView(dot)
        }
    }

    private fun updateIndicators() {
        val count = items.size
        if (count <= 1 || indicatorContainer.childCount != count) return

        val density = resources.displayMetrics.density
        val normalWidth = (8 * density).toInt()
        val activeWidth = (22 * density).toInt()
        val realIndex = getCurrentRealIndex()

        for (i in 0 until count) {
            val dot = indicatorContainer.getChildAt(i)
            val isSelected = (i == realIndex)
            val lp = dot.layoutParams as LinearLayout.LayoutParams
            val targetWidth = if (isSelected) activeWidth else normalWidth
            if (lp.width != targetWidth) {
                lp.width = targetWidth
                dot.layoutParams = lp
                dot.background = createDotDrawable(isSelected)
            }
        }
    }

    private fun createDotDrawable(isSelected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f
            setColor(if (isSelected) ContextCompat.getColor(context, R.color.lab_accent) else "#B0BEC5".toColorInt())
        }
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
            val realIndex = ((position % items.size) + items.size) % items.size
            holder.bind(items[realIndex], realIndex, position)
        }

        override fun getItemCount(): Int = if (items.isEmpty()) 0 else 10

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
                    cornerRadius = 20f * itemView.resources.displayMetrics.density
                }
                cardBackground.background = gradient

                itemView.setOnClickListener {
                    if (adapterPosition != currentPosition) {
                        currentPosition = adapterPosition
                        recyclerView.smoothScrollToPosition(adapterPosition)
                    } else {
                        onItemClickListener?.invoke(item, realIndex)
                    }
                }
            }
        }
    }

    private companion object {
        const val ITEM_WIDTH_PERCENT = 0.52f
        const val MIN_SCALE = 0.78f
        const val MAX_SCALE = 1.0f
        const val MIN_ALPHA = 0.65f
    }
}
