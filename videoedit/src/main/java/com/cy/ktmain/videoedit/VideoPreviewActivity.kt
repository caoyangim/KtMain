package com.cy.ktmain.videoedit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.SurfaceView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cy.ktmain.videoedit.media.FrameExtractor
import com.cy.ktmain.videoedit.player.VideoPlayerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Lightweight full preview used from the select screen play button.
 */
class VideoPreviewActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: PlayerSurfaceController
    private var trimStartMs = 0L
    private var trimEndMs = Long.MAX_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.videoedit_activity_preview)
        val path = intent.getStringExtra(EXTRA_PATH) ?: run { finish(); return }
        trimStartMs = intent.getLongExtra(EXTRA_TRIM_START, 0L)
        trimEndMs = intent.getLongExtra(EXTRA_TRIM_END, Long.MAX_VALUE)

        val surface = findViewById<SurfaceView>(R.id.previewSurface)
        val label = findViewById<TextView>(R.id.previewLabel)
        label.text = getString(
            R.string.videoedit_preview_range,
            FrameExtractor.formatTime(trimStartMs),
            FrameExtractor.formatTime(if (trimEndMs == Long.MAX_VALUE) 0L else trimEndMs)
        )

        val player = VideoPlayerFactory.create(this)
        controller = PlayerSurfaceController(surface, player)
        controller.attach()
        controller.prepare(
            android.net.Uri.fromFile(java.io.File(path)),
            autoPlay = true
        )
    }

    override fun onPause() {
        super.onPause()
        controller.pause()
    }

    override fun onDestroy() {
        scope.cancel()
        controller.release()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_PATH = "path"
        private const val EXTRA_TRIM_START = "trim_start"
        private const val EXTRA_TRIM_END = "trim_end"

        fun createIntent(
            context: Context,
            path: String,
            trimStartMs: Long,
            trimEndMs: Long,
        ): Intent = Intent(context, VideoPreviewActivity::class.java).apply {
            putExtra(EXTRA_PATH, path)
            putExtra(EXTRA_TRIM_START, trimStartMs)
            putExtra(EXTRA_TRIM_END, trimEndMs)
        }
    }
}
