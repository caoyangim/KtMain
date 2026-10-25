package com.cy.ktmain.videoedit.media

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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
                    fetchFrame(retriever, timeMs, maxWidth)
                }.getOrNull()
                Frame(timeMs, bitmap)
            }
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun extractCover(path: String, timeMs: Long, maxWidth: Int = DEFAULT_COVER_MAX_WIDTH): Bitmap? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(path)
                runCatching {
                    fetchFrame(retriever, timeMs, maxWidth)
                }.getOrNull()
            } finally {
                runCatching { retriever.release() }
            }
        }

    /**
     * API 27+ 用 getScaledFrameAtTime 在解码阶段直接降采样；
     * 低版本回退为 getFrameAtTime 全尺寸解码后再 scaleDown。
     */
    private fun fetchFrame(
        retriever: MediaMetadataRetriever,
        timeMs: Long,
        maxWidth: Int,
    ): Bitmap? {
        val timeUs = timeMs * 1000L
        val dims = scaledDims(retriever, maxWidth)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && dims != null) {
            retriever.getScaledFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                dims.first,
                dims.second
            )
        } else {
            retriever.getFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )?.let { scaleDown(it, maxWidth) }
        }
    }

    /**
     * 按视频宽高比计算宽度不超过 maxWidth 的目标尺寸；元数据缺失时返回 null。
     * 注意：WIDTH/HEIGHT 元数据是旋转前的原始尺寸，竖拍视频需按旋转角交换宽高。
     */
    private fun scaledDims(retriever: MediaMetadataRetriever, maxWidth: Int): Pair<Int, Int>? {
        var width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull() ?: return null
        var height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull() ?: return null
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            ?.toIntOrNull() ?: 0
        if (rotation == 90 || rotation == 270) {
            val tmp = width
            width = height
            height = tmp
        }
        if (width <= 0 || height <= 0) return null
        if (width <= maxWidth) return width to height
        val ratio = maxWidth.toFloat() / width
        return maxWidth to (height * ratio).roundToInt().coerceAtLeast(1)
    }

    companion object {
        const val DEFAULT_COUNT = 16
        const val DEFAULT_MAX_WIDTH = 160
        const val DEFAULT_COVER_MAX_WIDTH = 720
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
