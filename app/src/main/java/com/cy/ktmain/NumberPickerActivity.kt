package com.cy.ktmain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class NumberPickerActivity : AppCompatActivity() {

    private lateinit var recyclerPicker: RecyclerView
    private lateinit var recyclerValue: TextView
    private lateinit var pickerAdapter: NumberAdapter
    private val snapHelper = LinearSnapHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_picker)

        findViewById<MaterialToolbar>(R.id.pickerToolbar).setNavigationOnClickListener {
            finish()
        }

        setupNativePicker()
        setupRecyclerPicker()
    }

    private fun setupNativePicker() {
        val valueLabel = findViewById<TextView>(R.id.nativeValue)
        findViewById<NumberPicker>(R.id.nativePicker).apply {
            minValue = MIN_VALUE
            maxValue = MAX_VALUE
            value = INITIAL_VALUE
            wrapSelectorWheel = true
            descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            valueLabel.showValue(value)
            setOnValueChangedListener { _, _, newValue -> valueLabel.showValue(newValue) }
        }
    }

    private fun setupRecyclerPicker() {
        recyclerPicker = findViewById(R.id.recyclerPicker)
        recyclerValue = findViewById(R.id.recyclerValue)
        pickerAdapter = NumberAdapter((MIN_VALUE..MAX_VALUE).toList()) { position ->
            recyclerPicker.smoothScrollToPosition(position)
        }

        recyclerPicker.apply {
            layoutManager = LinearLayoutManager(this@NumberPickerActivity)
            adapter = pickerAdapter
            setHasFixedSize(true)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) updateRecyclerSelection()
                }
            })
        }
        snapHelper.attachToRecyclerView(recyclerPicker)

        recyclerPicker.post {
            recyclerPicker.scrollToPosition(INITIAL_VALUE - MIN_VALUE)
            updateRecyclerSelection()
        }
    }

    private fun updateRecyclerSelection() {
        val layoutManager = recyclerPicker.layoutManager as LinearLayoutManager
        val snappedView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(snappedView)
        if (position == RecyclerView.NO_POSITION) return
        pickerAdapter.setSelectedPosition(position)
        recyclerValue.showValue(pickerAdapter.valueAt(position))
    }

    private fun TextView.showValue(value: Int) {
        text = getString(R.string.number_picker_current_value, value)
    }

    private class NumberAdapter(
        private val values: List<Int>,
        private val onNumberClick: (Int) -> Unit
    ) : RecyclerView.Adapter<NumberAdapter.NumberHolder>() {

        private var selectedPosition = INITIAL_VALUE - MIN_VALUE

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberHolder {
            val item = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_picker_number, parent, false) as TextView
            return NumberHolder(item, onNumberClick)
        }

        override fun onBindViewHolder(holder: NumberHolder, position: Int) {
            holder.bind(values[position], position == selectedPosition)
        }

        override fun getItemCount(): Int = values.size

        fun valueAt(position: Int): Int = values[position]

        fun setSelectedPosition(position: Int) {
            if (selectedPosition == position) return
            val previous = selectedPosition
            selectedPosition = position
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
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
                label.textSize = if (isSelected) 24f else 18f
                label.alpha = if (isSelected) 1f else 0.68f
            }
        }
    }

    private companion object {
        const val MIN_VALUE = 0
        const val MAX_VALUE = 100
        const val INITIAL_VALUE = 18
    }
}
