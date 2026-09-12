package dev.hossain.codematex.ui.screens.debug.telemetry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.system.DebugMemoryStats
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@Composable
fun TelemetrySubUi(
    state: TelemetrySubState,
    modifier: Modifier = Modifier,
) {
    LiveMemoryDashboard(
        stats = state.stats,
        onTriggerGc = { state.eventSink(TelemetryUiEvent.TriggerGc) },
        modifier = modifier,
    )
}

@Composable
private fun LiveMemoryDashboard(
    stats: DebugMemoryStats,
    onTriggerGc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Real-Time Memory & CPU",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                OutlinedButton(
                    onClick = onTriggerGc,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Text("Run GC", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Native Heap Meter
            MemoryMeterRow(
                label = "Native Heap (C++ LLM)",
                valueText = "${"%.1f".format(stats.nativeAllocatedMb)} / ${"%.1f".format(stats.nativeTotalMb)} MB",
                fraction = if (stats.nativeTotalMb > 0f) stats.nativeAllocatedMb / stats.nativeTotalMb else 0f,
                tint = MaterialTheme.colorScheme.primary,
                subValueText = "${"%.1f".format(stats.nativeFreeMb)} MB free in native heap",
            )

            // JVM Heap Meter
            MemoryMeterRow(
                label = "JVM Heap (App)",
                valueText = "${"%.1f".format(stats.jvmUsedMb)} / ${"%.1f".format(stats.jvmMaxMb)} MB",
                fraction = if (stats.jvmMaxMb > 0f) stats.jvmUsedMb / stats.jvmMaxMb else 0f,
                tint = MaterialTheme.colorScheme.secondary,
                subValueText = "${"%.1f".format(stats.jvmTotalMb)} MB committed JVM heap",
            )

            // System RAM Meter
            MemoryMeterRow(
                label = "Device RAM",
                valueText = "${"%.2f".format(stats.ramUsedGb)} / ${"%.2f".format(stats.ramTotalGb)} GB",
                fraction = if (stats.ramTotalGb > 0f) stats.ramUsedGb / stats.ramTotalGb else 0f,
                tint = if (stats.isLowMemory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                subValueText = "${"%.2f".format(stats.ramAvailGb)} GB available" + if (stats.isLowMemory) " • ⚠️ Low Memory Flag" else "",
            )
        }
    }
}

@Composable
private fun MemoryMeterRow(
    label: String,
    valueText: String,
    fraction: Float,
    tint: Color,
    subValueText: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = tint,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        if (subValueText != null) {
            Text(
                text = subValueText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class TelemetrySubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is TelemetrySubScreen -> {
                SubUi<TelemetrySubState> { state, modifier ->
                    TelemetrySubUi(state, modifier)
                }
            }

            else -> {
                null
            }
        }
}

@ThemePreviews
@Composable
private fun TelemetrySubUiPreview() {
    CodeWithAIAppTheme {
        Surface {
            TelemetrySubUi(
                state =
                    TelemetrySubState(
                        stats =
                            DebugMemoryStats(
                                nativeAllocatedMb = 450.5f,
                                nativeTotalMb = 600.0f,
                                nativeFreeMb = 149.5f,
                                jvmUsedMb = 65.2f,
                                jvmMaxMb = 256.0f,
                                jvmTotalMb = 120.0f,
                                ramUsedGb = 3.8f,
                                ramTotalGb = 8.0f,
                                ramAvailGb = 4.2f,
                                isLowMemory = false,
                            ),
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
