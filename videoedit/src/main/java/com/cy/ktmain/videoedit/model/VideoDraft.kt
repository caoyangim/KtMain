package com.cy.ktmain.videoedit.model

/**
 * Shared state across select → trim → cover screens.
 * Trim/filter are stored as parameters only; the source file is not re-encoded.
 */
data class VideoDraft(
    val sourceUri: String,
    val localPath: String,
    val durationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = durationMs,
    val filterId: String = PreviewFilter.NONE,
    val coverTimeMs: Long = 0L,
    val coverPath: String? = null,
) {
    val trimDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)

    fun clampedTrim(startMs: Long, endMs: Long, minDurationMs: Long = MIN_TRIM_MS): VideoDraft {
        val duration = durationMs.coerceAtLeast(0L)
        var start = startMs.coerceIn(0L, duration)
        var end = endMs.coerceIn(0L, duration)
        if (end < start) {
            val tmp = start
            start = end
            end = tmp
        }
        if (end - start < minDurationMs) {
            end = (start + minDurationMs).coerceAtMost(duration)
            start = (end - minDurationMs).coerceAtLeast(0L)
        }
        return copy(trimStartMs = start, trimEndMs = end)
    }

    companion object {
        const val MIN_TRIM_MS = 1_000L
    }
}
