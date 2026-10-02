package com.cy.ktmain.carousel

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.cy.ktmain.R
import com.cy.ktmain.utils.setupEdgeToEdgeInsets
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.carousel.CarouselStrategy
import com.google.android.material.carousel.FullScreenCarouselStrategy
import com.google.android.material.carousel.MultiBrowseCarouselStrategy
import com.google.android.material.carousel.UncontainedCarouselStrategy
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial

class SafeCarouselLayoutManager @JvmOverloads constructor(
    strategy: CarouselStrategy = MultiBrowseCarouselStrategy(),
    @RecyclerView.Orientation orientation: Int = RecyclerView.HORIZONTAL
) : CarouselLayoutManager(strategy, orientation) {

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (_: IllegalArgumentException) {
            try {
                setCarouselStrategy(MultiBrowseCarouselStrategy())
                super.onLayoutChildren(recycler, state)
            } catch (_: Throwable) {
                // 防护初始测量异常
            }
        }
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        return try {
            super.scrollHorizontallyBy(dx, recycler, state)
        } catch (_: IllegalArgumentException) {
            try {
                setCarouselStrategy(MultiBrowseCarouselStrategy())
                super.scrollHorizontallyBy(dx, recycler, state)
            } catch (_: Throwable) {
                0
            }
        }
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        return try {
            super.scrollVerticallyBy(dy, recycler, state)
        } catch (_: IllegalArgumentException) {
            try {
                setCarouselStrategy(MultiBrowseCarouselStrategy())
                super.scrollVerticallyBy(dy, recycler, state)
            } catch (_: Throwable) {
                0
            }
        }
    }
}

class CarouselLayoutManagerActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: CarouselLayoutManager
    private val snapHelper = CarouselSnapHelper()

    private lateinit var tvStrategyDesc: TextView
    private lateinit var tvStatus: TextView
    private lateinit var cardContainer: View

    private var currentStrategyName = "MultiBrowse"
    private var isHorizontal = true
    private var isSnapEnabled = true
    private var currentAlignment = CarouselLayoutManager.ALIGNMENT_START

    private val sampleItems = listOf(
        CarouselItem(
            id = 1,
            title = "Jetpack Compose",
            subtitle = "声明式 UI 动态架构与现代界面构建",
            tag = "CARD 01",
            startColor = "#1A237E".toColorInt(),
            endColor = "#3F51B5".toColorInt()
        ),
        CarouselItem(
            id = 2,
            title = "Coroutines Flow",
            subtitle = "响应式数据流与结构化并发实践",
            tag = "CARD 02",
            startColor = "#004D40".toColorInt(),
            endColor = "#00897B".toColorInt()
        ),
        CarouselItem(
            id = 3,
            title = "Kotlin Multiplatform",
            subtitle = "跨平台共享逻辑与底层架构演进",
            tag = "CARD 03",
            startColor = "#4A148C".toColorInt(),
            endColor = "#8E24AA".toColorInt()
        ),
        CarouselItem(
            id = 4,
            title = "Android XR",
            subtitle = "空间计算与 Glimmer 沉浸式交互探索",
            tag = "CARD 04",
            startColor = "#880E4F".toColorInt(),
            endColor = "#D81B60".toColorInt()
        ),
        CarouselItem(
            id = 5,
            title = "Performance Profiling",
            subtitle = "Perfetto Trace 与多线程卡顿分析",
            tag = "CARD 05",
            startColor = "#E65100".toColorInt(),
            endColor = "#FB8C00".toColorInt()
        ),
        CarouselItem(
            id = 6,
            title = "AppFunctions & Agent",
            subtitle = "系统级 AI 指令对接与端侧调度",
            tag = "CARD 06",
            startColor = "#311B92".toColorInt(),
            endColor = "#5E35B1".toColorInt()
        ),
        CarouselItem(
            id = 7,
            title = "Navigation 3",
            subtitle = "全新 Scene 与多 Backstack 路由模式",
            tag = "CARD 07",
            startColor = "#004D40".toColorInt(),
            endColor = "#00796B".toColorInt()
        ),
        CarouselItem(
            id = 8,
            title = "CameraX Pipeline",
            subtitle = "生命周期感知与高帧率图像流处理",
            tag = "CARD 08",
            startColor = "#BF360C".toColorInt(),
            endColor = "#E64A19".toColorInt()
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carousel_layout_manager)

        setupEdgeToEdgeInsets(
            rootView = findViewById(R.id.clmRoot),
            toolbar = findViewById(R.id.clmToolbar)
        )

        findViewById<MaterialToolbar>(R.id.clmToolbar).setNavigationOnClickListener {
            finish()
        }

        recyclerView = findViewById(R.id.clmRecyclerView)
        tvStrategyDesc = findViewById(R.id.tvStrategyDesc)
        tvStatus = findViewById(R.id.tvClmStatus)
        cardContainer = findViewById(R.id.clmCardContainer)

        layoutManager = SafeCarouselLayoutManager(MultiBrowseCarouselStrategy())
        layoutManager.carouselAlignment = currentAlignment

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = ClmAdapter(sampleItems)

        snapHelper.attachToRecyclerView(recyclerView)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateStatus()
                }
            }
        })

        setupStrategyChoice()
        setupOptionsChoice()
        setupActions()

        updateStrategyDescription()
        updateStatus()
    }

    private fun setupStrategyChoice() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupStrategy)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val strategy: CarouselStrategy = when (checkedIds.first()) {
                R.id.chipHero -> {
                    currentStrategyName = "Hero"
                    currentAlignment = CarouselLayoutManager.ALIGNMENT_CENTER
                    layoutManager.carouselAlignment = currentAlignment
                    findViewById<com.google.android.material.chip.Chip>(R.id.chipAlignCenter).isChecked = true
                    MultiBrowseCarouselStrategy()
                }
                R.id.chipUncontained -> {
                    currentStrategyName = "Uncontained"
                    UncontainedCarouselStrategy()
                }
                R.id.chipFullScreen -> {
                    currentStrategyName = "FullScreen"
                    FullScreenCarouselStrategy()
                }
                else -> {
                    currentStrategyName = "MultiBrowse"
                    MultiBrowseCarouselStrategy()
                }
            }
            layoutManager.setCarouselStrategy(strategy)
            updateStrategyDescription()
            recyclerView.post { updateStatus() }
        }
    }

    private fun setupOptionsChoice() {
        findViewById<ChipGroup>(R.id.chipGroupOrientation).setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            isHorizontal = checkedIds.first() == R.id.chipHoriz
            val orientation = if (isHorizontal) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            layoutManager.orientation = orientation

            val params = cardContainer.layoutParams
            params.height = if (isHorizontal) {
                (220 * resources.displayMetrics.density).toInt()
            } else {
                (360 * resources.displayMetrics.density).toInt()
            }
            cardContainer.layoutParams = params

            recyclerView.adapter?.notifyDataSetChanged()
            recyclerView.post { updateStatus() }
        }

        findViewById<ChipGroup>(R.id.chipGroupAlignment).setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentAlignment = if (checkedIds.first() == R.id.chipAlignStart) {
                CarouselLayoutManager.ALIGNMENT_START
            } else {
                CarouselLayoutManager.ALIGNMENT_CENTER
            }
            layoutManager.carouselAlignment = currentAlignment
            recyclerView.post { updateStatus() }
        }

        val switchSnap = findViewById<SwitchMaterial>(R.id.switchSnap)
        switchSnap.setOnCheckedChangeListener { _, isChecked ->
            isSnapEnabled = isChecked
            if (isChecked) {
                snapHelper.attachToRecyclerView(recyclerView)
            } else {
                snapHelper.attachToRecyclerView(null)
            }
            recyclerView.post { updateStatus() }
        }
    }

    private fun setupActions() {
        findViewById<MaterialButton>(R.id.btnPrev).setOnClickListener {
            val currPos = getClosestPosition()
            if (currPos > 0) {
                recyclerView.smoothScrollToPosition(currPos - 1)
            }
        }

        findViewById<MaterialButton>(R.id.btnNext).setOnClickListener {
            val currPos = getClosestPosition()
            if (currPos < sampleItems.size - 1) {
                recyclerView.smoothScrollToPosition(currPos + 1)
            }
        }

        findViewById<MaterialButton>(R.id.btnFirst).setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }

        findViewById<MaterialButton>(R.id.btnLast).setOnClickListener {
            recyclerView.smoothScrollToPosition(sampleItems.size - 1)
        }
    }

    private fun getClosestPosition(): Int {
        if (!isSnapEnabled || layoutManager.childCount == 0) return 0
        return try {
            val snapView = snapHelper.findSnapView(layoutManager) ?: return 0
            val pos = layoutManager.getPosition(snapView)
            if (pos != RecyclerView.NO_POSITION) pos else 0
        } catch (_: Throwable) {
            0
        }
    }

    private fun updateStrategyDescription() {
        val descRes = when (currentStrategyName) {
            "Hero" -> R.string.clm_strategy_desc_hero
            "Uncontained" -> R.string.clm_strategy_desc_uncontained
            "FullScreen" -> R.string.clm_strategy_desc_fullscreen
            else -> R.string.clm_strategy_desc_multibrowse
        }
        tvStrategyDesc.setText(descRes)
    }

    private fun updateStatus() {
        val orientationStr = if (isHorizontal) "Horizontal" else "Vertical"
        val snapStr = if (isSnapEnabled) "已启用" else "已禁用"
        val alignStr = if (currentAlignment == CarouselLayoutManager.ALIGNMENT_START) "Start" else "Center"
        val focalPos = getClosestPosition()

        tvStatus.text = getString(
            R.string.clm_status_info,
            currentStrategyName,
            orientationStr,
            snapStr,
            alignStr,
            focalPos + 1,
            sampleItems.size
        )
    }

    private inner class ClmAdapter(
        private val items: List<CarouselItem>
    ) : RecyclerView.Adapter<ClmAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_carousel_layout_manager, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val lp = holder.itemView.layoutParams
            if (isHorizontal) {
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            holder.itemView.layoutParams = lp
            holder.bind(items[position], position)
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val bg: RelativeLayout = itemView.findViewById(R.id.clmCardBackground)
            private val tvTag: TextView = itemView.findViewById(R.id.tvClmTag)
            private val tvIndex: TextView = itemView.findViewById(R.id.tvClmIndex)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvClmTitle)
            private val tvSubtitle: TextView = itemView.findViewById(R.id.tvClmSubtitle)

            fun bind(item: CarouselItem, position: Int) {
                tvTag.text = item.tag
                tvIndex.text = itemView.context.getString(R.string.clm_index_format, position + 1, items.size)
                tvTitle.text = item.title
                tvSubtitle.text = item.subtitle

                val gradient = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(item.startColor, item.endColor)
                ).apply {
                    cornerRadius = 12f * itemView.resources.displayMetrics.density
                }
                bg.background = gradient

                itemView.setOnClickListener {
                    Toast.makeText(
                        this@CarouselLayoutManagerActivity,
                        getString(R.string.clm_click_toast, position + 1, item.tag, item.title),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
