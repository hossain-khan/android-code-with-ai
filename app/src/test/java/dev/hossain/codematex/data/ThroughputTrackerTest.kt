package dev.hossain.codematex.data

import com.google.common.truth.Truth.assertThat
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
        assertThat(emptyResult).isEqualTo("TTFT: -- • Speed: -- t/s (0 tokens)")

        timeNano = 50.milliseconds.inWholeNanoseconds
        val firstResult = tracker.recordToken("Hello")
        assertThat(firstResult).isEqualTo("TTFT: 50ms • Speed: -- t/s (1 tokens)")

        // Terminal blank callback should not increment tokens
        val blankResult = tracker.recordToken("")
        assertThat(blankResult).isEqualTo("TTFT: 50ms • Speed: -- t/s (1 tokens)")
    }

    @Test
    fun `recordToken calculates speed accurately across multiple generated tokens`() {
        var timeNano = 0L
        val tracker = ThroughputTracker(clockNano = { timeNano })

        timeNano = 50.milliseconds.inWholeNanoseconds // 50ms TTFT
        tracker.recordToken("Hello")

        timeNano = 150.milliseconds.inWholeNanoseconds // 100ms decode for 2 tokens = 20.0 t/s
        val result = tracker.recordToken(" world")

        assertThat(result).isEqualTo("TTFT: 50ms • Speed: 20.0 t/s (2 tokens)")

        timeNano = 250.milliseconds.inWholeNanoseconds // 200ms decode for 3 tokens = 15.0 t/s
        val result3 = tracker.recordToken("!")
        assertThat(result3).isEqualTo("TTFT: 50ms • Speed: 15.0 t/s (3 tokens)")
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
        assertThat(result).isEqualTo("TTFT: 100ms • Speed: 10.0 t/s")
        assertThat(result).doesNotContain("tokens")
    }

    @Test
    fun `finalize handles zero tokens on immediate completion or cancellation`() {
        val tracker = ThroughputTracker(clockNano = { 0L })

        val result = tracker.finalize()

        assertThat(result).isEqualTo("Speed: 0.0 t/s")
    }

    @Test
    fun `ttft is never negative even if clock reports non-increasing values`() {
        var timeNano = 100.milliseconds.inWholeNanoseconds
        val tracker = ThroughputTracker(clockNano = { timeNano })

        // Perturb clock backward
        timeNano = 50.milliseconds.inWholeNanoseconds
        val result = tracker.recordToken("token")

        assertThat(result).startsWith("TTFT: 0ms")
    }
}
