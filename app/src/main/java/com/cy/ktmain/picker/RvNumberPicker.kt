package com.cy.ktmain.picker

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.cy.ktmain.R
import kotlin.math.abs

class RvNumberPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var minValue: Int = 0
        set(value) {
            field = value
            if (maxValue < value) maxValue = value
            rebuildValues()
        }

    var maxValue: Int = 0
        set(value) {
            field = value.coerceAtLeast(minValue)
            rebuildValues()
        }

    var value: Int
        get() = currentValue
        set(value) = selectValue(value, false, false)

    private val recyclerView = RecyclerView(context)
    private val layoutManager = LinearLayoutManager(context)
    private val snapHelper = LinearSnapHelper()
    private val adapter = NumberAdapter { position ->
        recyclerView.smoothScrollToPosition(position)
    }
    private var listener: ((oldValue: Int, newValue: Int) -> Unit)? = null
    private var currentValue: Int = 0
    private var pendingPosition: Int? = null

    init {
        clipChildren = false
        addView(createSelectionView())
        addView(recyclerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        recyclerView.apply {
            layoutManager = this@RvNumberPicker.layoutManager
            adapter = this@RvNumberPicker.adapter
            clipToPadding = false
            overScrollMode = OVER_SCROLL_NEVER
            setHasFixedSize(true)
            itemAnimator = null
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    updateItemTransforms()
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) updateCenteredSelection()
                }
            })
        }
        snapHelper.attachToRecyclerView(recyclerView)
        rebuildValues()
    }

    fun setOnValueChangedListener(listener: ((oldValue: Int, newValue: Int) -> Unit)?) {
        this.listener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val verticalPadding = ((h - itemHeightPx) / 2).coerceAtLeast(0)
        recyclerView.setPadding(0, verticalPadding, 0, verticalPadding)
        pendingPosition?.let {
            recyclerView.scrollToPosition(it)
            pendingPosition = null
        }
        post {
            updateCenteredSelection()
            updateItemTransforms()
        }
    }

    private fun createSelectionView() = android.view.View(context).apply {
        background = ContextCompat.getDrawable(context, R.drawable.bg_picker_selection)
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, itemHeightPx)
        layoutParams.gravity = android.view.Gravity.CENTER_VERTICAL
        this.layoutParams = layoutParams
    }

    private fun rebuildValues() {
        val safeValue = normalizeValue(currentValue)
        currentValue = safeValue
        adapter.submitValues((minValue..maxValue).toList(), safeValue - minValue)
        scrollToValue(safeValue, false)
    }

    private fun selectValue(requestedValue: Int, notify: Boolean, smoothScroll: Boolean) {
        val selectedValue = normalizeValue(requestedValue)
        val previous = currentValue
        currentValue = selectedValue
        adapter.setSelectedPosition(selectedValue - minValue)
        scrollToValue(selectedValue, smoothScroll)
        if (notify && previous != selectedValue) listener?.invoke(previous, selectedValue)
    }

    private fun scrollToValue(selectedValue: Int, smoothScroll: Boolean) {
        val position = selectedValue - minValue
        if (height == 0) {
            pendingPosition = position
        } else if (smoothScroll) {
            recyclerView.smoothScrollToPosition(position)
        } else {
            recyclerView.scrollToPosition(position)
            recyclerView.post {
                snapToCenteredChild()
                updateItemTransforms()
            }
        }
    }

    private fun updateCenteredSelection() {
        val snappedView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(snappedView)
        if (position == RecyclerView.NO_POSITION) return
        val selectedValue = adapter.valueAt(position)
        val previous = currentValue
        currentValue = selectedValue
        adapter.setSelectedPosition(position)
        updateItemTransforms()
        recyclerView.postOnAnimation(::updateItemTransforms)
        if (previous != selectedValue) listener?.invoke(previous, selectedValue)
    }

    private fun snapToCenteredChild() {
        val view = layoutManager.findViewByPosition(currentValue - minValue) ?: return
        val distance = snapHelper.calculateDistanceToFinalSnap(layoutManager, view) ?: return
        recyclerView.scrollBy(distance[0], distance[1])
    }

    private fun updateItemTransforms() {
        if (recyclerView.height == 0) return
        val centerY = recyclerView.height / 2f
        val influenceDistance = recyclerView.height * 0.52f
        for (index in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(index)
            val distance = abs(centerY - (child.top + child.bottom) / 2f)
            val influence = (1f - distance / influenceDistance).coerceIn(0f, 1f)
            val scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * influence
            child.scaleX = scale
            child.scaleY = scale
            child.alpha = MIN_ALPHA + (1f - MIN_ALPHA) * influence
        }
    }

    private fun normalizeValue(requestedValue: Int): Int {
        return requestedValue.coerceIn(minValue, maxValue)
    }

    private val itemHeightPx: Int
        get() = (ITEM_HEIGHT_DP * resources.displayMetrics.density).toInt()

    private class NumberAdapter(
        private val onNumberClick: (Int) -> Unit
    ) : RecyclerView.Adapter<NumberAdapter.NumberHolder>() {

        private var values: List<Int> = emptyList()
        private var selectedPosition: Int = RecyclerView.NO_POSITION

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberHolder {
            val item = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_picker_number, parent, false) as TextView
            return NumberHolder(item, onNumberClick)
        }

        override fun onBindViewHolder(holder: NumberHolder, position: Int) {
            holder.bind(values[position], position == selectedPosition)
        }

        override fun getItemCount(): Int = values.size

        fun submitValues(values: List<Int>, selectedPosition: Int) {
            this.values = values
            this.selectedPosition = selectedPosition
            notifyDataSetChanged()
        }

        fun valueAt(position: Int): Int = values[position]

        fun setSelectedPosition(position: Int) {
            if (selectedPosition == position) return
            val previous = selectedPosition
            selectedPosition = position
            if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous)
            notifyItemChanged(position)
        }

        class NumberHolder(
            private val label: TextView,
            onNumberClick: (Int) -> Unit
        ) : RecyclerView.ViewHolder(label) {

            init {
                label.setOnClickListener {
                    bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                        ?.let(onNumberClick)
                }
            }

            fun bind(value: Int, isSelected: Boolean) {
                label.text = value.toString()
                label.setTextColor(
                    ContextCompat.getColor(
                        label.context,
                        if (isSelected) R.color.lab_accent else R.color.lab_text_secondary
                    )
                )
                label.textSize = 20f
            }
        }
    }

    private companion object {
        const val ITEM_HEIGHT_DP = 56f
        const val MIN_SCALE = 0.8f
        const val MAX_SCALE = 1.32f
        const val MIN_ALPHA = 0.46f
    }
}
