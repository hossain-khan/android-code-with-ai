package dev.hossain.codematex.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import dev.hossain.codematex.system.SystemResourceStats
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews

/**
 * Modular, ultra-compact hardware telemetry bar displaying live CPU and RAM resource utilization.
 *
 * Visual layout:
 * ```
 * ┌─────────────────────────────────────────────────────────────┐
 * │                 ⏳ Initializing Gemma 2B...                 │
 * │                                                             │
 * │ CPU       42%                 RAM          3.8 / 8.0 GB     │
 * │ ═════════════                 ═════════════════════════     │
 * └─────────────────────────────────────────────────────────────┘
 * ```
 *
 * @param stats Real-time snapshot of CPU percentage and used/total device RAM.
 * @param modifier Modifier for styling and layout positioning.
 * @param cpuColor Color used for the CPU progress track. Defaults to [MaterialTheme.colorScheme.primary].
 * @param ramColor Color used for the RAM progress track. Defaults to [MaterialTheme.colorScheme.tertiary].
 */
@Composable
fun LiveHardwareTelemetryBars(
    stats: SystemResourceStats,
    modifier: Modifier = Modifier,
    cpuColor: Color = MaterialTheme.colorScheme.primary,
    ramColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
            )
        }
    }
}
