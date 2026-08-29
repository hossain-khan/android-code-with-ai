package dev.hossain.codematex.runtime

/**
 * Exception thrown when the device's physical RAM is critically constrained,
 * preventing safe on-device LLM model initialization without risking a Low Memory Killer (LMK) process termination.
 *
 * ## Official Android Documentation:
 * - [Manage your app's memory](https://developer.android.com/topic/performance/memory/manage-app-memory)
 * - [Low memory killers (LMK) on Android Vitals](https://developer.android.com/topic/performance/vitals/lmk)
 *
 * @property availMemBytes Current available system memory in bytes.
 * @property requiredBytes Minimum required memory headroom in bytes.
 */
class LowMemoryException(
    val availMemBytes: Long,
    val requiredBytes: Long,
    message: String =
        "Device memory is constrained (${availMemBytes / (1024 * 1024)} MB available, " +
            "${requiredBytes / (1024 * 1024)} MB required). Please close other background apps.",
) : RuntimeException(message)
