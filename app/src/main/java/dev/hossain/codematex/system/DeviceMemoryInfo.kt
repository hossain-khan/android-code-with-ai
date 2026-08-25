package dev.hossain.codematex.system

import kotlinx.serialization.Serializable

/**
 * Structured device memory information suitable for the UI layer.
 *
 * [totalBytes] is the authoritative value used for compatibility decisions.
 * [displayTotalGb] is a decimal-gigabyte rendering of [totalBytes] for display.
 * [displayLabel] is the unit label (e.g. "GB") so the UI never invents its own unit.
 */
@Serializable
data class DeviceMemoryInfo(
    val totalBytes: Long,
    val displayTotalGb: Double,
    val displayLabel: String,
)
