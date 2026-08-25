package dev.hossain.codematex.data

/**
 * Tracks LLM inference throughput metrics: time-to-first-token (TTFT) and
 * decode speed in tokens per second.
 *
 * This class is stateful and intended to be created at the start of a single
 * inference request.
 *
 * @param clock time source used for measurements; defaults to [System.currentTimeMillis].
 */
class ThroughputTracker(
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val startTime = clock()
    private var tokenCount = 0
    private var firstTokenTime = 0L

    /**
     * Records a partial token and returns a human-readable throughput string.
     *
     * @param partialToken the token emitted by the LLM (may be empty for the
     *        final done callback).
     */
    fun recordToken(partialToken: String): String {
        tokenCount++
        if (firstTokenTime == 0L && partialToken.isNotBlank()) {
            firstTokenTime = clock()
        }

        val now = clock()
        val totalPrefillMs = firstTokenTime - startTime
        val decodeMs = now - firstTokenTime

        return if (decodeMs > 0) {
            val speed = (tokenCount * 1000f) / decodeMs
            "TTFT: ${totalPrefillMs}ms • Speed: ${"%.1f".format(speed)} t/s ($tokenCount tokens)"
        } else {
            "TTFT: ${totalPrefillMs}ms • Speed: -- t/s ($tokenCount tokens)"
        }
    }

    /**
     * Returns the final throughput summary after generation completes.
     */
    fun finalize(): String {
        val now = clock()
        val decodeTimeSec = (now - firstTokenTime) / 1000f
        val speed = if (decodeTimeSec > 0) tokenCount / decodeTimeSec else 0f
        val speedText = "%.1f".format(speed)
        val ttft = firstTokenTime - startTime
        return if (ttft > 0) {
            "TTFT: ${ttft}ms • Speed: $speedText t/s"
        } else {
            "Speed: $speedText t/s"
        }
    }
}
