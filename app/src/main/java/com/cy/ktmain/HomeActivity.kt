package com.cy.ktmain

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HomeActivity : AppCompatActivity() {

    private val modules = listOf(
        LabModule(
            title = R.string.module_coroutine_title,
            description = R.string.module_coroutine_description,
            badge = R.string.module_coroutine_badge,
            badgeBackground = R.drawable.bg_badge_coroutine,
            status = ModuleStatus.READY,
            detail = R.string.module_coroutine_detail
        ),
        LabModule(
            title = R.string.module_lock_title,
            description = R.string.module_lock_description,
            badge = R.string.module_lock_badge,
            badgeBackground = R.drawable.bg_badge_lock,
            status = ModuleStatus.READY,
            detail = R.string.module_lock_detail,
            destination = SyncInterruptActivity::class.java
        ),
        LabModule(
            title = R.string.module_ui_title,
            description = R.string.module_ui_description,
            badge = R.string.module_ui_badge,
            badgeBackground = R.drawable.bg_badge_ui,
            status = ModuleStatus.READY,
            detail = R.string.module_ui_detail,
            destination = NumberPickerActivity::class.java
        ),
        LabModule(
            title = R.string.module_network_title,
            description = R.string.module_network_description,
            badge = R.string.module_network_badge,
            badgeBackground = R.drawable.bg_badge_network,
            status = ModuleStatus.PLANNED,
            detail = R.string.module_network_detail
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<TextView>(R.id.readyCount).text =
            modules.count { it.status == ModuleStatus.READY }.toString()
        findViewById<TextView>(R.id.plannedCount).text =
            modules.count { it.status == ModuleStatus.PLANNED }.toString()

        val container = findViewById<LinearLayout>(R.id.moduleContainer)
        modules.forEach { module ->
            val card = LayoutInflater.from(this)
                .inflate(R.layout.item_test_module, container, false) as MaterialCardView
            bindModule(card, module)
            container.addView(card)
        }
    }

    private fun bindModule(card: MaterialCardView, module: LabModule) {
        card.findViewById<TextView>(R.id.moduleBadge).apply {
            setText(module.badge)
            setBackgroundResource(module.badgeBackground)
        }
        card.findViewById<TextView>(R.id.moduleTitle).setText(module.title)
        card.findViewById<TextView>(R.id.moduleDescription).setText(module.description)
        card.findViewById<Chip>(R.id.moduleStatus).apply {
            setText(module.status.label)
            chipBackgroundColor = ColorStateList.valueOf(color(module.status.background))
            setTextColor(color(module.status.foreground))
        }
        card.setOnClickListener {
            module.destination?.let { destination ->
                startActivity(Intent(this, destination))
            } ?: MaterialAlertDialogBuilder(this)
                    .setTitle(module.title)
                    .setMessage(module.detail)
                    .setPositiveButton(R.string.module_dialog_action, null)
                    .show()
        }
    }

    private fun color(@ColorRes resource: Int): Int = ContextCompat.getColor(this, resource)

    private data class LabModule(
        @StringRes val title: Int,
        @StringRes val description: Int,
        @StringRes val badge: Int,
        @DrawableRes val badgeBackground: Int,
        val status: ModuleStatus,
        @StringRes val detail: Int,
        val destination: Class<*>? = null
    )

    private enum class ModuleStatus(
        @StringRes val label: Int,
        @ColorRes val background: Int,
        @ColorRes val foreground: Int
    ) {
        READY(R.string.module_status_source, R.color.lab_accent_soft, R.color.lab_accent),
        PLANNED(R.string.module_status_planned, R.color.lab_planned_soft, R.color.lab_planned)
    }
}
