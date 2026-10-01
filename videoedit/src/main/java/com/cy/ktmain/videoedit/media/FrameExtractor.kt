package com.cy.ktmain.videoedit.media

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Samples a fixed number of frames for the cover strip.
 * All work is off the main thread; thumbnails are downsampled.
 */
class FrameExtractor {

    data class Frame(val timeMs: Long, val bitmap: Bitmap?)

    suspend fun extract(
        path: String,
        count: Int = DEFAULT_COUNT,
        maxWidth: Int = DEFAULT_MAX_WIDTH,
    ): List<Frame> = withContext(Dispatchers.IO) {
        val safeCount = count.coerceIn(1, MAX_COUNT)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            sampleTimes(durationMs, safeCount).map { timeMs ->
                val bitmap = runCatching {
                    retriever.getFrameAtTime(
                        timeMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )?.let { scaleDown(it, maxWidth) }
                }.getOrNull()
                Frame(timeMs, bitmap)
            }
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun extractCover(path: String, timeMs: Long, maxWidth: Int = DEFAULT_MAX_WIDTH): Bitmap? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(path)
                runCatching {
                    retriever.getFrameAtTime(
                        timeMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )?.let { scaleDown(it, maxWidth) }
                }.getOrNull()
            } finally {
                runCatching { retriever.release() }
            }
        }

    companion object {
        const val DEFAULT_COUNT = 16
        const val DEFAULT_MAX_WIDTH = 160
        const val MAX_COUNT = 40

        fun sampleTimes(durationMs: Long, count: Int): List<Long> {
            if (count <= 0) return emptyList()
            if (durationMs <= 0) return List(count) { 0L }
            if (count == 1) return listOf(0L)
            val last = durationMs
            return (0 until count).map { index ->
                durationMs * index / (count - 1L)
            }.map { it.coerceIn(0L, last) }
        }

        fun formatTime(ms: Long): String {
            val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
            val m = totalSec / 60
            val s = totalSec % 60
            return java.util.Locale.US.let { String.format(it, "%02d:%02d", m, s) }
        }

        private fun scaleDown(source: Bitmap, maxWidth: Int): Bitmap {
            if (source.width <= maxWidth) return source
            val ratio = maxWidth.toFloat() / source.width
            val height = (source.height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(source, maxWidth, height, true)
            if (scaled !== source) source.recycle()
            return scaled
        }
    }
}
