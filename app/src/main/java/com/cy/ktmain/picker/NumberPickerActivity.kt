package com.cy.ktmain.picker

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cy.ktmain.R
import com.cy.ktmain.utils.setupEdgeToEdgeInsets
import com.google.android.material.appbar.MaterialToolbar

class NumberPickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_number_picker)
        setupEdgeToEdgeInsets(
            rootView = findViewById(R.id.pickerRoot),
            toolbar = findViewById(R.id.pickerToolbar),
        )

        findViewById<MaterialToolbar>(R.id.pickerToolbar).setNavigationOnClickListener {
            finish()
        }

        setupCyPicker()
        setupRvPicker()
    }

    private fun setupCyPicker() {
        val valueLabel = findViewById<TextView>(R.id.nativeValue)
        findViewById<CyNumberPicker>(R.id.nativePicker).apply {
            minValue = MIN_VALUE
            maxValue = MAX_VALUE
            value = INITIAL_VALUE
            wrapSelectorWheel = true
            valueLabel.showValue(value)
            setOnValueChangedListener { _, newValue -> valueLabel.showValue(newValue) }
        }
    }

    private fun setupRvPicker() {
        val valueLabel = findViewById<TextView>(R.id.recyclerValue)
        findViewById<RvNumberPicker>(R.id.recyclerPicker).apply {
            minValue = MIN_VALUE
            maxValue = MAX_VALUE
            value = INITIAL_VALUE
            valueLabel.showValue(value)
            setOnValueChangedListener { _, newValue -> valueLabel.showValue(newValue) }
        }
    }

    private fun TextView.showValue(value: Int) {
        text = getString(R.string.number_picker_current_value, value)
    }

    private companion object {
        const val MIN_VALUE = 0
        const val MAX_VALUE = 100
        const val INITIAL_VALUE = 18
    }
}
