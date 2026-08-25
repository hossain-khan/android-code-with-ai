package dev.hossain.codematex.system

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Snapshot of real-time device hardware telemetry (CPU & RAM utilization).
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
