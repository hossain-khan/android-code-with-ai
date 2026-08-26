package dev.hossain.codematex.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Unit tests for [ThroughputTracker].
 */
class ThroughputTrackerTest {
    @Test
    fun `recordToken ignores empty callback and does not increment token count`() {
        var timeNano = 0L
        val tracker = ThroughputTracker(clockNano = { timeNano })

        val emptyResult = tracker.recordToken("")
        assertEquals("TTFT: -- • Speed: -- t/s (0 tokens)", emptyResult)

        timeNano = 50.milliseconds.inWholeNanoseconds
        val firstResult = tracker.recordToken("Hello")
        assertEquals("TTFT: 50ms • Speed: -- t/s (1 tokens)", firstResult)

        // Terminal blank callback should not increment tokens
        val blankResult = tracker.recordToken("")
        assertEquals("TTFT: 50ms • Speed: -- t/s (1 tokens)", blankResult)
    }

    @Test
    fun `recordToken calculates speed accurately across multiple generated tokens`() {
        var timeNano = 0L
        val tracker = ThroughputTracker(clockNano = { timeNano })

        timeNano = 50.milliseconds.inWholeNanoseconds // 50ms TTFT
        tracker.recordToken("Hello")

        timeNano = 150.milliseconds.inWholeNanoseconds // 100ms decode for 2 tokens = 20.0 t/s
        val result = tracker.recordToken(" world")

        assertEquals("TTFT: 50ms • Speed: 20.0 t/s (2 tokens)", result)

        timeNano = 250.milliseconds.inWholeNanoseconds // 200ms decode for 3 tokens = 15.0 t/s
        val result3 = tracker.recordToken("!")
        assertEquals("TTFT: 50ms • Speed: 15.0 t/s (3 tokens)", result3)
    }

    @Test
    fun `finalize returns exact expected final formatted metric`() {
        var timeNano = 0L
        val tracker = ThroughputTracker(clockNano = { timeNano })

        timeNano = 100.milliseconds.inWholeNanoseconds // 100ms TTFT
        tracker.recordToken("Kotlin")

        timeNano = 300.milliseconds.inWholeNanoseconds // 200ms decode for 2 tokens
        tracker.recordToken(" Coroutines")

        timeNano = 500.milliseconds.inWholeNanoseconds // 400ms decode for 4 tokens total
        tracker.recordToken(" Flow")
        tracker.recordToken(" StateFlow")

        val result = tracker.finalize()
        assertEquals("TTFT: 100ms • Speed: 10.0 t/s", result)
        assertFalse(result.contains("tokens"))
    }

    @Test
    fun `finalize handles zero tokens on immediate completion or cancellation`() {
        val tracker = ThroughputTracker(clockNano = { 0L })

        val result = tracker.finalize()

        assertEquals("Speed: 0.0 t/s", result)
    }

    @Test
    fun `ttft is never negative even if clock reports non-increasing values`() {
        var timeNano = 100.milliseconds.inWholeNanoseconds
        val tracker = ThroughputTracker(clockNano = { timeNano })

        // Perturb clock backward
        timeNano = 50.milliseconds.inWholeNanoseconds
        val result = tracker.recordToken("token")

        assertTrue(result.startsWith("TTFT: 0ms"))
    }
}
