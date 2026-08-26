package dev.hossain.codematex.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.ui.component.LiveHardwareTelemetryBars
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.util.formatShortModelName

@Composable
internal fun SupportingBenchmarkingCard(
    state: ChatScreen.State.Active,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatShortModelName(state.modelName),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                state.activeBackend?.let { backend ->
                    val isAccelerated = backend == "GPU" || backend == "NPU"
                    val containerColor =
                        if (isAccelerated) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    val textColor =
                        if (isAccelerated) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }

                    Surface(
                        color = containerColor,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = backend,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = textColor,
                        )
                    }
                }
            }

            state.modelSize?.let {
                Text(
                    text = "Size: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.modelMemory?.let {
                Text(
                    text = "Memory: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.configInfo?.let { config ->
                Text(
                    text = "Sampler: $config",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.throughputInfo?.let { throughput ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = throughput,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            state.systemResourceStats?.let { stats ->
                LiveHardwareTelemetryBars(
                    stats = stats,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            } ?: state.systemStatsInfo?.let { stats ->
                Text(
                    text = stats,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
internal fun ModelTechnicalInfoPanel(
    state: ChatScreen.State.Active,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = formatShortModelName(state.modelName),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    // Backend Badge
                    state.activeBackend?.let { backend ->
                        val isAccelerated = backend == "GPU" || backend == "NPU"
                        val containerColor =
                            if (isAccelerated) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        val textColor =
                            if (isAccelerated) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }

                        Surface(
                            color = containerColor,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                text = backend,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = textColor,
                            )
                        }
                    }

                    // Collapsed speed indicator if active or benchmark metrics when done
                    if (!isExpanded) {
                        if (state.isGenerating) {
                            Text(
                                text = "• Generating...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else if (state.throughputInfo != null) {
                            Text(
                                text = "• ${state.throughputInfo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse info" else "Expand info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isExpanded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.modelSize?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.modelMemory?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                state.configInfo?.let { config ->
                    Text(
                        text = config,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.throughputInfo?.let { throughput ->
                    Text(
                        text = throughput,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                state.systemResourceStats?.let { stats ->
                    LiveHardwareTelemetryBars(
                        stats = stats,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                } ?: state.systemStatsInfo?.let { stats ->
                    Text(
                        text = stats,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

// ==========================================
// Previews
// ==========================================

@ThemePreviews
@Composable
private fun ModelTechnicalInfoPanelPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            ModelTechnicalInfoPanel(
                state =
                    ChatScreen.State.Active(
                        topic = CodingTopic.KOTLIN,
                        modelName = "gemma-4-E2B-it-litert-lm",
                        activeBackend = "GPU",
                        modelSize = "2,588 MB",
                        modelMemory = "Requires 4GB RAM",
                        configInfo = "Temp: 0.7 • Top-K: 40 • Top-P: 1.0",
                        throughputInfo = "TTFT: 480ms • Speed: 14.2 t/s",
                        systemStatsInfo = null,
                        persona = TutorPersona.SENIOR_ENGINEER,
                        isPreparing = false,
                        isGenerating = false,
                        messages = emptyList(),
                        eventSink = {},
                    ),
            )
        }
    }
}
