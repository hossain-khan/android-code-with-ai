package dev.hossain.codematex.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.codematex.system.ContextUsageStats
import dev.hossain.codematex.system.SystemResourceStats
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import java.util.Locale

/**
 * Modular, ultra-compact hardware telemetry bar displaying live CPU, RAM, and optional context window utilization.
 *
 * Visual layout:
 * ```
 * ┌─────────────────────────────────────────────────────────────┐
 * │ CPU       42%                 RAM          3.8 / 8.0 GB     │
 * │ ═════════════                 ═════════════════════════     │
 * │                                                             │
 * │ CONTEXT                      1,420 / 8,192 Tokens (17.3%)   │
 * │ ═══════════════════════════════════════════════════════════ │
 * └─────────────────────────────────────────────────────────────┘
 * ```
 *
 * @param stats Real-time snapshot of CPU percentage and used/total device RAM.
 * @param contextStats Optional snapshot of conversation context window token consumption.
 * @param modifier Modifier for styling and layout positioning.
 * @param cpuColor Color used for the CPU progress track. Defaults to [MaterialTheme.colorScheme.primary].
 * @param ramColor Color used for the RAM progress track. Defaults to [MaterialTheme.colorScheme.tertiary].
 * @param contextColor Color used for the context progress track. Defaults to [MaterialTheme.colorScheme.secondary].
 */
@Composable
fun LiveHardwareTelemetryBars(
    stats: SystemResourceStats,
    modifier: Modifier = Modifier,
    contextStats: ContextUsageStats? = null,
    cpuColor: Color = MaterialTheme.colorScheme.primary,
    ramColor: Color = MaterialTheme.colorScheme.tertiary,
    contextColor: Color = MaterialTheme.colorScheme.secondary,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactMetricProgressBar(
                label = "CPU",
                valueText = "${stats.cpuPercent.toInt()}%",
                fraction = stats.cpuFraction,
                indicatorColor = cpuColor,
                modifier = Modifier.weight(1f),
            )

            CompactMetricProgressBar(
                label = "RAM",
                valueText = "${"%.1f".format(stats.ramUsedGb)} / ${"%.0f".format(stats.ramTotalGb)} GB",
                fraction = stats.ramFraction,
                indicatorColor = ramColor,
                modifier = Modifier.weight(1f),
            )
        }

        if (contextStats != null && contextStats.maxTokens > 0) {
            LiveContextTelemetryBar(
                contextStats = contextStats,
                indicatorColor = contextColor,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Modular full-width progress bar displaying conversation context window token consumption.
 *
 * Visual layout:
 * ```
 * ┌─────────────────────────────────────────────────────────────┐
 * │ CONTEXT                      1,420 / 8,192 Tokens (17.3%)   │
 * │ ═══════════════════════════════════════════════════════════ │
 * └─────────────────────────────────────────────────────────────┘
 * ```
 *
 * @param contextStats Context window metrics containing token consumption and capacity limits.
 * @param modifier Modifier for layout styling.
 * @param indicatorColor Progress indicator track color. Defaults to [MaterialTheme.colorScheme.secondary],
 *                       switching to [MaterialTheme.colorScheme.error] when capacity reaches $\ge 80\%$.
 */
@Composable
fun LiveContextTelemetryBar(
    contextStats: ContextUsageStats,
    modifier: Modifier = Modifier,
    indicatorColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val isWarning = contextStats.fraction >= 0.8f
    val effectiveColor = if (isWarning) MaterialTheme.colorScheme.error else indicatorColor
    val percentText = String.format(Locale.US, "%.1f", contextStats.fraction * 100)

    CompactMetricProgressBar(
        label = "CONTEXT",
        valueText = "${contextStats.formattedUsedTokens} / ${contextStats.formattedMaxTokens} Tokens ($percentText%)",
        fraction = contextStats.fraction,
        indicatorColor = effectiveColor,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Reusable single-metric progress bar with extra-small label and 3dp straight line indicator.
 */
@Composable
fun CompactMetricProgressBar(
    label: String,
    valueText: String,
    fraction: Float,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400),
        label = "MetricProgressAnimation",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
            color = indicatorColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@ThemePreviews
@Composable
private fun LiveHardwareTelemetryBarsPreview() {
    CodeWithAIAppTheme {
        Surface {
            LiveHardwareTelemetryBars(
                stats = SystemResourceStats(cpuPercent = 42f, ramUsedGb = 3.8f, ramTotalGb = 8.0f),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun LiveHardwareTelemetryBarsHighLoadPreview() {
    CodeWithAIAppTheme {
        Surface {
            LiveHardwareTelemetryBars(
                stats = SystemResourceStats(cpuPercent = 88f, ramUsedGb = 7.4f, ramTotalGb = 8.0f),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun LiveHardwareTelemetryBarsWithContextPreview() {
    CodeWithAIAppTheme {
        Surface {
            LiveHardwareTelemetryBars(
                stats = SystemResourceStats(cpuPercent = 42f, ramUsedGb = 3.8f, ramTotalGb = 8.0f),
                contextStats = ContextUsageStats(usedTokens = 1420, maxTokens = 8192),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun LiveContextTelemetryBarPreview() {
    CodeWithAIAppTheme {
        Surface {
            LiveContextTelemetryBar(
                contextStats = ContextUsageStats(usedTokens = 1420, maxTokens = 8192),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun LiveContextTelemetryBarWarningPreview() {
    CodeWithAIAppTheme {
        Surface {
            LiveContextTelemetryBar(
                contextStats = ContextUsageStats(usedTokens = 7100, maxTokens = 8192),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun CompactMetricProgressBarPreview() {
    CodeWithAIAppTheme {
        Surface {
            CompactMetricProgressBar(
                label = "CPU",
                valueText = "64%",
                fraction = 0.64f,
                indicatorColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
