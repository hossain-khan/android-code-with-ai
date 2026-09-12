package dev.hossain.codematex.ui.screens.debug.model

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.MemoryDelta
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@Composable
fun ModelLifecycleSubUi(
    state: ModelLifecycleSubState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ModelLifecycleCard(state = state)
        StorageInspectorCard(
            models = state.models,
            onDeleteModel = { state.eventSink(ModelLifecycleUiEvent.DeleteModel(it)) },
        )
    }
}

@Composable
internal fun ModelLifecycleCard(state: ModelLifecycleSubState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Model Lifecycle & Load Profiler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Model Selection Chips
            Text(text = "Select Model to Profile:", style = MaterialTheme.typography.labelMedium)
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.models.forEach { model ->
                    val isDownloaded = model.downloadStatus == DownloadStatus.DOWNLOADED
                    FilterChip(
                        selected = state.selectedModel?.id == model.id,
                        onClick = { state.eventSink(ModelLifecycleUiEvent.SelectModel(model)) },
                        leadingIcon =
                            if (isDownloaded) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Downloaded",
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        label = {
                            Text(
                                text = model.displayName.ifBlank { model.name },
                                maxLines = 1,
                            )
                        },
                    )
                }
            }

            // Backend Override Chips
            Text(text = "Target Hardware Backend:", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(LlmEngine.Backend.GPU, LlmEngine.Backend.NPU, LlmEngine.Backend.CPU).forEach { backend ->
                    FilterChip(
                        selected = state.selectedBackend == backend,
                        onClick = { state.eventSink(ModelLifecycleUiEvent.SelectBackend(backend)) },
                        label = { Text(backend.name) },
                    )
                }
            }

            // Actions: Load vs Unload
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { state.eventSink(ModelLifecycleUiEvent.LoadModel) },
                    enabled = !state.isLoadingModel && !state.isUnloadingModel,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isLoadingModel) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading...")
                    } else {
                        Text(if (state.isModelLoaded) "Reload Engine" else "Load Model")
                    }
                }

                OutlinedButton(
                    onClick = { state.eventSink(ModelLifecycleUiEvent.UnloadModel) },
                    enabled = state.isModelLoaded && !state.isLoadingModel && !state.isUnloadingModel,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isUnloadingModel) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Freeing...")
                    } else {
                        Text("Unload & Free")
                    }
                }
            }

            // Delta Scorecards
            state.lastLoadDelta?.let { delta ->
                DeltaScorecard(
                    title = "Last Load Result",
                    delta = delta,
                    isLoad = true,
                )
            }

            state.lastUnloadDelta?.let { delta ->
                DeltaScorecard(
                    title = "Last Unload & Cleanup Result",
                    delta = delta,
                    isLoad = false,
                )
            }
        }
    }
}

@Composable
internal fun DeltaScorecard(
    title: String,
    delta: MemoryDelta,
    isLoad: Boolean,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${delta.durationMs}ms duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricPill("Native Δ", "${if (delta.deltaNativeMb >= 0) "+" else ""}${"%.1f".format(delta.deltaNativeMb)} MB")
                MetricPill("JVM Δ", "${if (delta.deltaJvmMb >= 0) "+" else ""}${"%.1f".format(delta.deltaJvmMb)} MB")
                MetricPill("RAM Δ", "${if (delta.deltaSystemMb >= 0) "+" else ""}${"%.1f".format(delta.deltaSystemMb)} MB")
            }
        }
    }
}

@Composable
internal fun MetricPill(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
internal fun StorageInspectorCard(
    models: List<AiModel>,
    onDeleteModel: (AiModel) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Model Weights on Disk",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (models.none { it.downloadStatus == DownloadStatus.DOWNLOADED }) {
                Text(
                    text = "No models currently downloaded to device storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                models.filter { it.downloadStatus == DownloadStatus.DOWNLOADED }.forEach { model ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = model.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = model.localPath ?: "Unknown path",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { onDeleteModel(model) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Model Weights",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class ModelLifecycleSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is ModelLifecycleSubScreen -> {
                SubUi<ModelLifecycleSubState> { state, modifier ->
                    ModelLifecycleSubUi(state, modifier)
                }
            }

            else -> {
                null
            }
        }
}

@ThemePreviews
@Composable
private fun ModelLifecycleCardPreview() {
    CodeWithAIAppTheme {
        Surface {
            ModelLifecycleSubUi(
                state =
                    ModelLifecycleSubState(
                        models =
                            listOf(
                                AiModel(
                                    id = "gemma-2-2b",
                                    name = "Gemma 2B",
                                    displayName = "Gemma 2B Instruct",
                                    description = "Fast on-device model",
                                    downloadUrl = "https://example.com/gemma.bin",
                                    sizeBytes = 2_000_000_000L,
                                    preferredBackend = LlmEngine.Backend.GPU,
                                    downloadStatus = DownloadStatus.DOWNLOADED,
                                    localPath = "/data/data/models/gemma.bin",
                                ),
                            ),
                        isModelLoaded = true,
                        loadedModelName = "Gemma 2B",
                        activeBackend = LlmEngine.Backend.GPU,
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
