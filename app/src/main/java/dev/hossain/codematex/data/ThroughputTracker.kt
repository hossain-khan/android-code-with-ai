package dev.hossain.codematex.data

import java.util.Locale
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit

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
     * Number of tokens processed so far.
     */
    val currentTokenCount: Int
        get() = tokenCount

    /**
     * Time to first token in milliseconds, or null if no token has been emitted yet.
     */
    val ttftMs: Long?
        get() =
            if (firstTokenNano > 0L) {
                (firstTokenNano - startNano).nanoseconds.inWholeMilliseconds.coerceAtLeast(0)
            } else {
                null
            }

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

        val ttft = (firstTokenNano - startNano).nanoseconds
        val ttftMs = ttft.inWholeMilliseconds.coerceAtLeast(0)
        val decodeDuration = (nowNano - firstTokenNano).nanoseconds
        val decodeSeconds = decodeDuration.toDouble(DurationUnit.SECONDS)

        return if (decodeDuration.inWholeMilliseconds > 0) {
            val speed = tokenCount.toDouble() / decodeSeconds
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
        val ttft = (firstTokenNano - startNano).nanoseconds
        val ttftMs = ttft.inWholeMilliseconds.coerceAtLeast(0)
        val decodeDuration = (now - firstTokenNano).nanoseconds
        val decodeSeconds = decodeDuration.toDouble(DurationUnit.SECONDS)

        val speed =
            if (decodeDuration.inWholeMilliseconds > 0) {
                tokenCount.toDouble() / decodeSeconds
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
