package com.cy.ktmain.videoedit.player

import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Default [VideoPlayer] backed by Media3 ExoPlayer.
 * Swap via [VideoPlayerFactory] without touching screen code.
 */
class Media3VideoPlayer(context: Context) : VideoPlayer {

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()
    private var listener: VideoPlayer.Listener? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> listener?.onPrepared(player.duration.coerceAtLeast(0L))
                Player.STATE_ENDED -> listener?.onCompleted()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            listener?.onError(error.message ?: error.errorCodeName)
        }
    }

    init {
        player.addListener(playerListener)
        player.playWhenReady = false
    }

    override fun prepare(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    override fun play() {
        player.playWhenReady = true
        player.play()
    }

    override fun pause() {
        player.playWhenReady = false
        player.pause()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun setSurface(surface: Surface?) {
        player.setVideoSurface(surface)
    }

    override fun currentPositionMs(): Long = player.currentPosition.coerceAtLeast(0L)

    override fun durationMs(): Long = player.duration.coerceAtLeast(0L)

    override fun isPlaying(): Boolean = player.isPlaying

    override fun setListener(listener: VideoPlayer.Listener?) {
        this.listener = listener
    }

    override fun release() {
        player.removeListener(playerListener)
        player.release()
        this.listener = null
    }
}
