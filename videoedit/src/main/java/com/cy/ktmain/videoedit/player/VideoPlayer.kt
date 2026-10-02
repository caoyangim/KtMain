package com.cy.ktmain.videoedit.player

import android.net.Uri
import android.view.Surface

/**
 * Replaceable player contract. Screens must depend only on this API,
 * never on Media3 / ExoPlayer types directly.
 */
interface VideoPlayer {

    fun prepare(uri: Uri)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setSurface(surface: Surface?)

    fun currentPositionMs(): Long

    fun durationMs(): Long

    fun isPlaying(): Boolean

    fun setListener(listener: Listener?)

    fun release()

    interface Listener {
        fun onPrepared(durationMs: Long)
        fun onCompleted()
        fun onError(message: String)
    }
}
