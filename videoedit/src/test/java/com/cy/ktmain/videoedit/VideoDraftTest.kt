package com.cy.ktmain.videoedit

import com.cy.ktmain.videoedit.media.FrameExtractor
import com.cy.ktmain.videoedit.model.PreviewFilter
import com.cy.ktmain.videoedit.model.VideoDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoDraftTest {

    private fun draft(durationMs: Long, startMs: Long = 0, endMs: Long = durationMs) = VideoDraft(
        sourceUri = "content://test",
        localPath = "/tmp/test.mp4",
        durationMs = durationMs,
        trimStartMs = startMs,
        trimEndMs = endMs
    )

    @Test
    fun clampedTrim_swapsReversedRange() {
        val result = draft(10_000).clampedTrim(startMs = 4_000, endMs = 1_000)
        assertEquals(1_000L, result.trimStartMs)
        assertEquals(4_000L, result.trimEndMs)
    }

    @Test
    fun clampedTrim_enforcesMinimumDuration() {
        val result = draft(10_000).clampedTrim(startMs = 5_000, endMs = 5_200)
        assertTrue(result.trimEndMs - result.trimStartMs >= VideoDraft.MIN_TRIM_MS)
    }

    @Test
    fun clampedTrim_clampsToDurationBounds() {
        val result = draft(5_000).clampedTrim(startMs = -1_000, endMs = 99_000)
        assertEquals(0L, result.trimStartMs)
        assertEquals(5_000L, result.trimEndMs)
    }

    @Test
    fun defaultFilterIsNone() {
        assertEquals(PreviewFilter.NONE, draft(1_000).filterId)
    }
}

class FrameExtractorTest {

    @Test
    fun sampleTimes_coversFullDuration() {
        val times = FrameExtractor.sampleTimes(durationMs = 10_000, count = 5)
        assertEquals(5, times.size)
        assertEquals(0L, times.first())
        assertEquals(10_000L, times.last())
    }

    @Test
    fun sampleTimes_handlesZeroDuration() {
        val times = FrameExtractor.sampleTimes(durationMs = 0, count = 3)
        assertEquals(3, times.size)
        assertTrue(times.all { it == 0L })
    }

    @Test
    fun sampleTimes_singleFrame() {
        val times = FrameExtractor.sampleTimes(durationMs = 8_000, count = 1)
        assertEquals(listOf(0L), times)
    }

    @Test
    fun formatTime_formatsAsMmSs() {
        assertEquals("00:05", FrameExtractor.formatTime(5_000))
        assertEquals("01:01", FrameExtractor.formatTime(61_000))
    }
}
