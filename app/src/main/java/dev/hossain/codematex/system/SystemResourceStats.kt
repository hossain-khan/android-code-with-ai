package dev.hossain.codematex.system

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Snapshot of real-time device hardware telemetry (CPU & RAM utilization).
 *
 * Typically displayed in the initialization dock and benchmark scorecard:
 * ```
 * ┌─────────────────────────────────────────────────────────────┐
 * │                 ⏳ Initializing Gemma 2B...                 │
 * │                                                             │
 * │ CPU       42%                 RAM          3.8 / 8.0 GB     │
 * │ ═════════════                 ═════════════════════════     │
 * └─────────────────────────────────────────────────────────────┘
 * ```
 *
 * @property cpuPercent Estimated process/system CPU usage percentage (0.0 to 100.0).
 * @property ramUsedGb Device RAM currently in use in gigabytes.
 * @property ramTotalGb Total device RAM available in gigabytes.
 */
@Immutable
@Serializable
data class SystemResourceStats(
    val cpuPercent: Float = 0f,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
) {
    /**
     * RAM usage normalized to 0.0 .. 1.0 fraction.
     */
    val ramFraction: Float
        get() = if (ramTotalGb > 0f) (ramUsedGb / ramTotalGb).coerceIn(0f, 1f) else 0f

    /**
     * Process/system CPU usage normalized to 0.0 .. 1.0 fraction.
     */
    val cpuFraction: Float
        get() = (cpuPercent / 100f).coerceIn(0f, 1f)

    /**
     * Human-readable single-line summary string.
     */
    val formattedSummary: String
        get() = "CPU: ${"%.0f".format(cpuPercent)}% • RAM: ${"%.1f".format(ramUsedGb)} GB / ${"%.1f".format(ramTotalGb)} GB"
}
