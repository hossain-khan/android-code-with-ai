package dev.hossain.codematex.system

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Snapshot of conversation context window token consumption.
 *
 * @property usedTokens Estimated cumulative count of tokens currently in context (system instructions + message history).
 * @property maxTokens Maximum token capacity supported by the active model ([dev.hossain.codematex.data.model.AiModel.contextWindow]).
 */
@Immutable
@Serializable
data class ContextUsageStats(
    val usedTokens: Int,
    val maxTokens: Int,
) {
    /**
     * Fraction of context capacity consumed, clamped between `0.0f` and `1.0f`.
     */
    val fraction: Float
        get() = if (maxTokens > 0) (usedTokens.toFloat() / maxTokens).coerceIn(0f, 1f) else 0f

    /**
     * Percentage of context capacity consumed (0 to 100).
     */
    val percent: Int
        get() = (fraction * 100).toInt()

    /**
     * Formatted count of used tokens with thousands separators (e.g. `"1,420"`).
     */
    val formattedUsedTokens: String
        get() = String.format(Locale.US, "%,d", usedTokens)

    /**
     * Formatted maximum capacity with thousands separators (e.g. `"8,192"`, `"32,768"`).
     */
    val formattedMaxTokens: String
        get() = String.format(Locale.US, "%,d", maxTokens)
}
