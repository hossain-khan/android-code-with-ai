package dev.hossain.codematex.data

import java.util.Locale

/**
 * Tracks LLM inference throughput metrics: time-to-first-token (TTFT) and
 * decode speed in tokens per second using a monotonic time source.
 *
 * This class is stateful and intended to be created at the start of a single
 * inference request.
 *
 * @param clockNano time source returning monotonic time in nanoseconds; defaults to [System.nanoTime].
 */
class ThroughputTracker(
    private val clockNano: () -> Long = { System.nanoTime() },
) {
    private val startNano = clockNano()
    private var tokenCount = 0
    private var firstTokenNano = 0L

    /**
     * Records a partial token and returns a human-readable throughput string.
     * Empty strings are ignored so terminal or blank signals do not inflate the token count.
     *
     * @param partialToken the token emitted by the LLM.
     */
    fun recordToken(partialToken: String): String {
        if (partialToken.isEmpty()) {
            return formatCurrentProgress()
        }

        val now = clockNano()
        if (firstTokenNano == 0L) {
            firstTokenNano = now
        }
        tokenCount++

        return formatCurrentProgress(now)
    }

    private fun formatCurrentProgress(nowNano: Long = clockNano()): String {
        if (tokenCount == 0 || firstTokenNano == 0L) {
            return "TTFT: -- • Speed: -- t/s (0 tokens)"
        }

        val ttftMs = maxOf(0L, (firstTokenNano - startNano) / 1_000_000L)
        val decodeNano = maxOf(0L, nowNano - firstTokenNano)
        val decodeMs = decodeNano / 1_000_000L

        return if (decodeMs > 0) {
            val speed = (tokenCount.toDouble() * 1_000_000_000.0) / decodeNano.toDouble()
            "TTFT: ${ttftMs}ms • Speed: ${String.format(Locale.US, "%.1f", speed)} t/s ($tokenCount tokens)"
        } else {
            "TTFT: ${ttftMs}ms • Speed: -- t/s ($tokenCount tokens)"
        }
    }

    /**
     * Returns the final throughput summary after generation completes.
     */
    fun finalize(): String {
        if (tokenCount == 0 || firstTokenNano == 0L) {
            return "Speed: 0.0 t/s"
        }

        val now = clockNano()
        val ttftMs = maxOf(0L, (firstTokenNano - startNano) / 1_000_000L)
        val decodeNano = maxOf(0L, now - firstTokenNano)

        val speed =
            if (decodeNano > 0) {
                (tokenCount.toDouble() * 1_000_000_000.0) / decodeNano.toDouble()
            } else {
                0.0
            }
        val speedText = String.format(Locale.US, "%.1f", speed)

        return if (ttftMs > 0) {
            "TTFT: ${ttftMs}ms • Speed: $speedText t/s"
        } else {
            "Speed: $speedText t/s"
        }
    }
}
