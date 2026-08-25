package dev.hossain.codematex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ThroughputTracker].
 */
class ThroughputTrackerTest {
    @Test
    fun `recordToken returns prefill before first real token`() {
        var time = 0L
        val tracker = ThroughputTracker(clock = { time })

        val result = tracker.recordToken("Hello")

        assertTrue(result.contains("TTFT"))
        assertTrue(result.contains("Speed: -- t/s"))
        assertTrue(result.contains("1 tokens"))
    }

    @Test
    fun `recordToken calculates speed after first token`() {
        var time = 0L
        val tracker = ThroughputTracker(clock = { time })

        tracker.recordToken("Hello")
        time = 100L
        val result = tracker.recordToken(" world")

        assertTrue(result.contains("TTFT"))
        assertTrue(result.contains("t/s"))
        assertTrue(result.contains("2 tokens"))
    }

    @Test
    fun `finalize returns final summary without token count`() {
        var time = 0L
        val tracker = ThroughputTracker(clock = { time })

        time = 50L
        tracker.recordToken("Hello")
        time = 150L

        val result = tracker.finalize()

        assertTrue(result.contains("TTFT"))
        assertTrue(result.contains("t/s"))
        assertTrue(!result.contains("tokens"))
    }

    @Test
    fun `finalize handles zero tokens`() {
        val tracker = ThroughputTracker(clock = { 0L })

        val result = tracker.finalize()

        assertEquals("Speed: 0.0 t/s", result)
    }
}
