package com.cy.ktmain

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cy.ktmain.bluetooth.BluetoothTileActivity
import com.cy.ktmain.carousel.CarouselActivity
import com.cy.ktmain.carousel.CarouselLayoutManagerActivity
import com.cy.ktmain.carousel.FoldableVPActivity
import com.cy.ktmain.lock.SyncInterruptActivity
import com.cy.ktmain.picker.NumberPickerActivity
import com.cy.ktmain.utils.GridSpacingItemDecoration
import com.cy.ktmain.utils.WindowWidthSizeClass
import com.cy.ktmain.utils.gridSpanCount
import com.cy.ktmain.utils.setupEdgeToEdgeInsets
import com.cy.ktmain.videoedit.VideoEditActivity
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
            title = R.string.module_bluetooth_title,
            description = R.string.module_bluetooth_description,
            badge = R.string.module_bluetooth_badge,
            badgeBackground = R.drawable.bg_badge_bluetooth,
            status = ModuleStatus.READY,
            detail = R.string.module_bluetooth_detail,
            destination = BluetoothTileActivity::class.java
        ),
        LabModule(
            title = R.string.module_carousel_title,
            description = R.string.module_carousel_description,
            badge = R.string.module_carousel_badge,
            badgeBackground = R.drawable.bg_badge_carousel,
            status = ModuleStatus.READY,
            detail = R.string.module_carousel_detail,
            destination = CarouselActivity::class.java
        ),
        LabModule(
            title = R.string.module_clm_title,
            description = R.string.module_clm_description,
            badge = R.string.module_clm_badge,
            badgeBackground = R.drawable.bg_badge_clm,
            status = ModuleStatus.READY,
            detail = R.string.module_clm_detail,
            destination = CarouselLayoutManagerActivity::class.java
        ),
        LabModule(
            title = R.string.module_foldable_vp_title,
            description = R.string.module_foldable_vp_description,
            badge = R.string.module_foldable_vp_badge,
            badgeBackground = R.drawable.bg_badge_clm,
            status = ModuleStatus.READY,
            detail = R.string.module_foldable_vp_detail,
            destination = FoldableVPActivity::class.java
        ),
        LabModule(
            title = R.string.module_videoedit_title,
            description = R.string.module_videoedit_description,
            badge = R.string.module_videoedit_badge,
            badgeBackground = R.drawable.bg_badge_ui,
            status = ModuleStatus.READY,
            detail = R.string.module_videoedit_detail,
            destination = VideoEditActivity::class.java
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
        setupEdgeToEdgeInsets(
            rootView = findViewById(R.id.homeRoot),
            toolbar = findViewById(R.id.homeToolbar)
        )

        val container = findViewById<RecyclerView>(R.id.moduleContainer)
        val widthClass = WindowWidthSizeClass.fromWidthDp(resources.configuration.screenWidthDp)
        val spanCount = widthClass.gridSpanCount()
        val footerPosition = modules.size + 1
        val layoutManager = GridLayoutManager(this, spanCount)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (position == HEADER_POSITION || position == footerPosition) spanCount else 1
        }
        container.layoutManager = layoutManager
        container.adapter = HomeAdapter(modules) { openModule(it) }
        container.addItemDecoration(
            GridSpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.module_grid_spacing))
        )
    }

    private fun openModule(module: LabModule) {
        module.destination?.let { destination ->
            startActivity(Intent(this, destination))
        } ?: MaterialAlertDialogBuilder(this)
            .setTitle(module.title)
            .setMessage(module.detail)
            .setPositiveButton(R.string.module_dialog_action, null)
            .show()
    }

    private class HomeAdapter(
        private val modules: List<LabModule>,
        private val onModuleClick: (LabModule) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val footerPosition = modules.size + 1

        override fun getItemViewType(position: Int): Int = when (position) {
            0 -> TYPE_HEADER
            footerPosition -> TYPE_FOOTER
            else -> TYPE_MODULE
        }

        override fun getItemCount(): Int = modules.size + 2

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                TYPE_HEADER -> HeaderViewHolder(
                    inflater.inflate(R.layout.item_home_header, parent, false)
                )
                TYPE_FOOTER -> FooterViewHolder(
                    inflater.inflate(R.layout.item_home_footer, parent, false)
                )
                else -> ModuleViewHolder(
                    inflater.inflate(R.layout.item_test_module, parent, false) as MaterialCardView
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderViewHolder -> holder.bind(modules)
                is ModuleViewHolder -> holder.bind(modules[position - 1], onModuleClick)
                is FooterViewHolder -> Unit
            }
        }

        class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bind(modules: List<LabModule>) {
                itemView.findViewById<TextView>(R.id.readyCount).text =
                    modules.count { it.status == ModuleStatus.READY }.toString()
                itemView.findViewById<TextView>(R.id.plannedCount).text =
                    modules.count { it.status == ModuleStatus.PLANNED }.toString()
            }
        }

        class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        class ModuleViewHolder(private val card: MaterialCardView) : RecyclerView.ViewHolder(card) {

            fun bind(module: LabModule, onClick: (LabModule) -> Unit) {
                card.findViewById<TextView>(R.id.moduleBadge).apply {
                    setText(module.badge)
                    setBackgroundResource(module.badgeBackground)
                }
                card.findViewById<TextView>(R.id.moduleTitle).setText(module.title)
                card.findViewById<TextView>(R.id.moduleDescription).setText(module.description)
                card.findViewById<Chip>(R.id.moduleStatus).apply {
                    setText(module.status.label)
                    chipBackgroundColor = ColorStateList.valueOf(
                        ContextCompat.getColor(card.context, module.status.background)
                    )
                    setTextColor(ContextCompat.getColor(card.context, module.status.foreground))
                }
                card.setOnClickListener { onClick(module) }
            }
        }

        private companion object {
            const val TYPE_HEADER = 0
            const val TYPE_MODULE = 1
            const val TYPE_FOOTER = 2
        }
    }

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

    private companion object {
        const val HEADER_POSITION = 0
    }
}
