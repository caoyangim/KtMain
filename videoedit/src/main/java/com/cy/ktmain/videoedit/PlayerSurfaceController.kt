package com.cy.ktmain.videoedit

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.cy.ktmain.videoedit.player.VideoPlayer

/**
 * Binds a [SurfaceView] to a [VideoPlayer] and tears the surface down safely.
 */
class PlayerSurfaceController(
    private val surfaceView: SurfaceView,
    private val player: VideoPlayer,
) : SurfaceHolder.Callback {

    private var prepared = false

    fun attach() {
        surfaceView.holder.addCallback(this)
    }

    fun detach() {
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
}
