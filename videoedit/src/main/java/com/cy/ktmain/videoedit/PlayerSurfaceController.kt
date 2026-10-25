package com.cy.ktmain.videoedit

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import com.cy.ktmain.videoedit.player.VideoPlayer
import kotlin.math.roundToInt

/**
 * Binds a [SurfaceView] to a [VideoPlayer] and tears the surface down safely.
 * Letterboxes the surface to the video's aspect ratio so frames are never stretched.
 */
class PlayerSurfaceController(
    private val surfaceView: SurfaceView,
    private val player: VideoPlayer,
) : SurfaceHolder.Callback {

    private var prepared = false
    private var videoWidth = 0
    private var videoHeight = 0

    private val containerLayoutListener =
        View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> applyAspectRatio() }

    fun attach() {
        surfaceView.holder.addCallback(this)
        (surfaceView.parent as? View)?.addOnLayoutChangeListener(containerLayoutListener)
    }

    fun detach() {
        (surfaceView.parent as? View)?.removeOnLayoutChangeListener(containerLayoutListener)
        surfaceView.holder.removeCallback(this)
        player.setSurface(null)
    }

    fun prepare(uri: Uri, autoPlay: Boolean = true) {
        player.setListener(object : VideoPlayer.Listener {
            override fun onPrepared(durationMs: Long) {
                prepared = true
                if (autoPlay) player.play()
            }

            override fun onCompleted() {
                player.seekTo(0)
                if (autoPlay) player.play()
            }

            override fun onError(message: String) {
                prepared = false
            }

            override fun onVideoSizeChanged(widthPx: Int, heightPx: Int) {
                videoWidth = widthPx
                videoHeight = heightPx
                applyAspectRatio()
            }
        })
        player.prepare(uri)
    }

    fun playInRange(startMs: Long, endMs: Long) {
        if (!prepared) return
        val pos = player.currentPositionMs()
        if (pos < startMs || pos >= endMs) {
            player.seekTo(startMs)
        }
        player.play()
    }

    fun pause() = player.pause()

    fun release() {
        detach()
        player.release()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        player.setSurface(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        player.setSurface(null)
    }

    /**
     * 把 SurfaceView 缩放到与视频一致的宽高比，居中放置在父容器内（letterbox），
     * 避免 ExoPlayer 对 Surface 默认的 SCALE_TO_FILL 拉伸变形。
     */
    private fun applyAspectRatio() {
        if (videoWidth <= 0 || videoHeight <= 0) return
        val container = surfaceView.parent as? ViewGroup ?: return
        val containerW = container.width
        val containerH = container.height
        if (containerW <= 0 || containerH <= 0) return
        val scale = minOf(
            containerW.toFloat() / videoWidth,
            containerH.toFloat() / videoHeight
        )
        val targetW = (videoWidth * scale).roundToInt().coerceAtLeast(1)
        val targetH = (videoHeight * scale).roundToInt().coerceAtLeast(1)
        val lp = surfaceView.layoutParams
        if (lp.width != targetW || lp.height != targetH) {
            lp.width = targetW
            lp.height = targetH
            surfaceView.layoutParams = lp
        }
    }
}
