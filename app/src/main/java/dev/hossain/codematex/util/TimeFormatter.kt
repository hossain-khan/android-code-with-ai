package dev.hossain.codematex.util

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Formats a given epoch timestamp in milliseconds into a concise, human-readable relative time string
 * using Kotlin's [kotlin.time.Duration] API.
 *
 * @param timestampMillis Epoch timestamp in milliseconds.
 * @param nowMillis Current epoch timestamp in milliseconds, defaults to [System.currentTimeMillis].
 * @return Formatted relative time string (e.g., "Just now", "5m ago", "2h ago", "3d ago", "2w ago").
 */
fun formatRelativeTime(
    timestampMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    if (timestampMillis <= 0L) return ""

    val elapsed = nowMillis - timestampMillis
    if (elapsed < 0L) return "Just now"

    val duration = elapsed.milliseconds
    return when {
        duration < 1.minutes -> "Just now"
        duration < 1.hours -> "${duration.inWholeMinutes}m ago"
        duration < 1.days -> "${duration.inWholeHours}h ago"
        duration < 7.days -> "${duration.inWholeDays}d ago"
        duration < 30.days -> "${duration.inWholeDays / 7}w ago"
        duration < 365.days -> "${duration.inWholeDays / 30}mo ago"
        else -> "${duration.inWholeDays / 365}y ago"
    }
}
