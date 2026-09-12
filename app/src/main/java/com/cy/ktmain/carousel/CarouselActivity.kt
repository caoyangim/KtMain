package com.cy.ktmain.carousel

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.cy.ktmain.R
import com.cy.ktmain.utils.setupEdgeToEdgeInsets
import com.cy.ktmain.widgets.CarouselArcIndicatorView
import com.cy.ktmain.widgets.CarouselIndicatorView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class CarouselActivity : AppCompatActivity() {

    private lateinit var carouselBannerView: CarouselBannerView
    private lateinit var statusText: TextView
    private lateinit var btnToggleAutoScroll: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carousel)
        setupEdgeToEdgeInsets(
            rootView = findViewById(R.id.carouselRoot),
            toolbar = findViewById(R.id.carouselToolbar),
        )

        findViewById<MaterialToolbar>(R.id.carouselToolbar).setNavigationOnClickListener {
            finish()
        }

        carouselBannerView = findViewById(R.id.carouselBanner)
        val arcIndicator = findViewById<CarouselArcIndicatorView>(R.id.carouselArcIndicator)
        val classicIndicator = findViewById<CarouselIndicatorView>(R.id.carouselClassicIndicator)

        statusText = findViewById(R.id.carouselStatusText)
        btnToggleAutoScroll = findViewById(R.id.btnToggleAutoScroll)

        val sampleItems = listOf(
            CarouselItem(
                id = 1,
                title = "Jetpack Compose",
                subtitle = "声明式 UI 架构与现代界面构建",
                tag = "CARD 01",
                startColor = "#1A237E".toColorInt(),
                endColor = "#3F51B5".toColorInt()
            ),
            CarouselItem(
                id = 2,
                title = "Coroutines Flow",
                subtitle = "异步数据流与响应式编程实践",
                tag = "CARD 02",
                startColor = "#004D40".toColorInt(),
                endColor = "#00897B".toColorInt()
            ),
            CarouselItem(
                id = 3,
                title = "Kotlin Multiplatform",
                subtitle = "跨平台逻辑共享与底层架构演进",
                tag = "CARD 03",
                startColor = "#4A148C".toColorInt(),
                endColor = "#8E24AA".toColorInt()
            ),
            CarouselItem(
                id = 4,
                title = "Android XR Experience",
                subtitle = "空间计算与沉浸式交互技术探索",
                tag = "CARD 04",
                startColor = "#880E4F".toColorInt(),
                endColor = "#D81B60".toColorInt()
            ),
            CarouselItem(
                id = 5,
                title = "Performance Profiling",
                subtitle = "卡顿分析与 Perfetto Trace 性能调优",
                tag = "CARD 05",
                startColor = "#E65100".toColorInt(),
                endColor = "#FB8C00".toColorInt()
            ),
            CarouselItem(
                id = 6,
                title = "AppFunctions & AI",
                subtitle = "系统级 Agent 指令对接与端侧 AI 调度",
                tag = "CARD 06",
                startColor = "#311B92".toColorInt(),
                endColor = "#5E35B1".toColorInt()
            ),
            CarouselItem(
                id = 7,
                title = "Navigation 3",
                subtitle = "全新 Scene 与多 Backstack 声明式路由",
                tag = "CARD 07",
                startColor = "#004D40".toColorInt(),
                endColor = "#00796B".toColorInt()
            ),
            CarouselItem(
                id = 8,
                title = "CameraX Pipeline",
                subtitle = "生命周期感知与图像流高性能处理",
                tag = "CARD 08",
                startColor = "#BF360C".toColorInt(),
                endColor = "#E64A19".toColorInt()
            )
        )

        // 独立指示器初始化数据
        arcIndicator.setItems(sampleItems) { it.id.toString() }
        classicIndicator.count = sampleItems.size

        val tvDirLog = findViewById<TextView>(R.id.tvDirLog)
        arcIndicator.onDirChangeListener = { dir, continuousRealIndex, progress, currIndex, nxtIndex, tLog ->
            val dirStatus = when (dir) {
                +1f -> "+1.0 (正向 / 右滑 ➔)"
                -1f -> "-1.0 (反向 / 左滑 ⬅)"
                else -> "0.0 (静止 ⏸)"
            }
            tvDirLog.text = getString(
                R.string.carousel_dir_log_format,
                dirStatus,
                currIndex,
                nxtIndex,
                continuousRealIndex,
                progress,
                tLog
            )
        }

        // 独立控件间通过标准事件回调实现完全解耦联动
        carouselBannerView.apply {
            setItems(sampleItems)

            setOnScrollProgressListener { curr, next, progress ->
                arcIndicator.setScrollProgress(curr, next, progress)
                classicIndicator.setScrollProgress(curr, next, progress)
            }

            setOnItemClickListener { item, realIndex ->
                Toast.makeText(
                    this@CarouselActivity,
                    getString(R.string.carousel_click_toast, realIndex + 1, item.title),
                    Toast.LENGTH_SHORT
                ).show()
            }

            setOnPageChangeListener { adapterPosition, realIndex ->
                updateStatus(adapterPosition, realIndex)
            }

            startAutoScroll()
        }

        btnToggleAutoScroll.setOnClickListener {
            if (carouselBannerView.isAutoScrolling()) {
                carouselBannerView.stopAutoScroll()
            } else {
                carouselBannerView.startAutoScroll()
            }
            val adapterPosition = carouselBannerView.getCurrentAdapterPosition()
            val realIndex = carouselBannerView.getCurrentRealIndex()
            updateStatus(adapterPosition, realIndex)
        }

        findViewById<MaterialButton>(R.id.btnPrev).setOnClickListener {
            carouselBannerView.scrollToPrevious()
        }

        findViewById<MaterialButton>(R.id.btnNext).setOnClickListener {
            carouselBannerView.scrollToNext()
        }

        setupJumpControls()
    }

    private fun setupJumpControls() {
        val etJumpTarget = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etJumpTarget)

        findViewById<MaterialButton>(R.id.btnJumpPage).setOnClickListener {
            val targetPos = etJumpTarget.text?.toString()?.toIntOrNull()
            if (targetPos != null && targetPos >= 0) {
                carouselBannerView.scrollToAdapterPosition(targetPos, false)
                Toast.makeText(
                    this,
                    getString(R.string.carousel_toast_jumped_pos, targetPos),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.carousel_toast_invalid_pos),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        findViewById<MaterialButton>(R.id.btnJumpPos0).setOnClickListener {
            carouselBannerView.scrollToAdapterPosition(0, false)
            Toast.makeText(
                this,
                getString(R.string.carousel_toast_jumped_pos, 0),
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<MaterialButton>(R.id.btnJumpPosMax).setOnClickListener {
            val targetPos = Int.MAX_VALUE - 10
            carouselBannerView.scrollToAdapterPosition(targetPos, false)
            Toast.makeText(
                this,
                getString(R.string.carousel_toast_jumped_pos, targetPos),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateStatus(adapterPosition: Int, realIndex: Int) {
        val totalRealCount = carouselBannerView.getRealCount()
        val isRunning = carouselBannerView.isAutoScrolling()

        val statusStr = if (isRunning) {
            getString(R.string.carousel_status_playing)
        } else {
            getString(R.string.carousel_status_paused)
        }

        btnToggleAutoScroll.text = if (isRunning) {
            getString(R.string.carousel_action_stop)
        } else {
            getString(R.string.carousel_action_start)
        }

        statusText.text = getString(
            R.string.carousel_status_info,
            adapterPosition,
            realIndex + 1,
            totalRealCount,
            statusStr
        )
    }
}
