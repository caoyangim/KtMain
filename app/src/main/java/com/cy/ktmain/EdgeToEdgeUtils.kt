package com.cy.ktmain

import android.util.TypedValue
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar

fun ComponentActivity.setupEdgeToEdgeInsets(
    rootView: View,
    toolbar: MaterialToolbar? = null,
) {
    enableEdgeToEdge()

    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.isAppearanceLightStatusBars = false
    windowInsetsController.isAppearanceLightNavigationBars = true

    val initialToolbarPaddingStart = toolbar?.paddingStart ?: 0
    val initialToolbarPaddingEnd = toolbar?.paddingEnd ?: 0
    val initialToolbarPaddingBottom = toolbar?.paddingBottom ?: 0

    val typedValue = TypedValue()
    val actionBarHeight = if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
        TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
    } else {
        (56 * resources.displayMetrics.density).toInt()
    }

    ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        toolbar?.let { tb ->
            val lp = tb.layoutParams
            lp.height = actionBarHeight + systemBars.top
            tb.layoutParams = lp

            tb.setPadding(
                initialToolbarPaddingStart + systemBars.left,
                systemBars.top,
                initialToolbarPaddingEnd + systemBars.right,
                initialToolbarPaddingBottom
            )
        }

        rootView.setPadding(
            systemBars.left,
            0,
            systemBars.right,
            systemBars.bottom
        )

        insets
    }
}
