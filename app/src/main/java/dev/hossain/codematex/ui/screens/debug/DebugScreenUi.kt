package dev.hossain.codematex.ui.screens.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DebugMemoryStats
import dev.hossain.codematex.system.MemoryDelta
import dev.hossain.codematex.ui.component.MarkdownMessage
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(screen = DebugScreen::class, scope = AppScope::class)
@Composable
fun DebugScreenContent(
    state: DebugScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Scaffold(
        modifier =
            modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .radialGradientScrim(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = "Model & Runtime Debugger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(DebugScreen.Event.Back) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { state.eventSink(DebugScreen.Event.TriggerGc) }) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Force Garbage Collection",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Status & Diagnostics Banner
            item {
                StatusBanner(state)
            }

            // Real-time Memory & CPU Meters
            item {
                LiveMemoryDashboard(state.telemetryStats, onTriggerGc = { state.eventSink(DebugScreen.Event.TriggerGc) })
            }

            // Model Loading & Unloading Controls
            item {
                ModelLifecycleCard(state)
            }

            // Inference Performance & Benchmark Runner
            item {
                InferenceBenchmarkCard(state)
            }

            // Hardware & Environment Diagnostics
            item {
                HardwareDiagnosticsCard(state.deviceInfo)
            }

            // Downloaded Weights & Disk Storage Inspector
            item {
                StorageInspectorCard(
                    models = state.models,
                    onDeleteModel = { state.eventSink(DebugScreen.Event.DeleteModel(it)) },
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(state: DebugScreen.State.Success) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color =
                            if (state.isModelLoaded) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                        modifier = Modifier.size(10.dp),
                    ) {}
                    Text(
                        text = if (state.isModelLoaded) "Engine Loaded" else "Engine Idle / Unloaded",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (state.isModelLoaded) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }

                if (state.activeBackend != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = "Active: ${state.activeBackend.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            state.statusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun LiveMemoryDashboard(
    stats: DebugMemoryStats,
    onTriggerGc: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
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
                    )
                }

                OutlinedButton(
                    onClick = onTriggerGc,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("Trigger GC", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Native Heap Meter
            MemoryMeterRow(
                label = "Native Heap (C++ LLM)",
                valueText = "${"%.1f".format(stats.nativeAllocatedMb)} / ${"%.1f".format(stats.nativeTotalMb)} MB",
                fraction = if (stats.nativeTotalMb > 0f) stats.nativeAllocatedMb / stats.nativeTotalMb else 0f,
                tint = MaterialTheme.colorScheme.primary,
            )

            // JVM Heap Meter
            MemoryMeterRow(
                label = "JVM Heap (App)",
                valueText = "${"%.1f".format(stats.jvmUsedMb)} / ${"%.1f".format(stats.jvmMaxMb)} MB",
                fraction = if (stats.jvmMaxMb > 0f) stats.jvmUsedMb / stats.jvmMaxMb else 0f,
                tint = MaterialTheme.colorScheme.secondary,
            )

            // System RAM Meter
            MemoryMeterRow(
                label = "Device RAM",
                valueText = "${"%.2f".format(
                    stats.ramUsedGb,
                )} / ${"%.2f".format(stats.ramTotalGb)} GB (Avail: ${"%.2f".format(stats.ramAvailGb)} GB)",
                fraction = if (stats.ramTotalGb > 0f) stats.ramUsedGb / stats.ramTotalGb else 0f,
                tint = if (stats.isLowMemory) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun MemoryMeterRow(
    label: String,
    valueText: String,
    fraction: Float,
    tint: androidx.compose.ui.graphics.Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(text = valueText, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = tint,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelLifecycleCard(state: DebugScreen.State.Success) {
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
                    FilterChip(
                        selected = state.selectedModel?.id == model.id,
                        onClick = { state.eventSink(DebugScreen.Event.SelectModel(model)) },
                        label = {
                            Text(
                                text = model.name + if (model.downloadStatus == DownloadStatus.DOWNLOADED) " (Downloaded)" else "",
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
                        onClick = { state.eventSink(DebugScreen.Event.SelectBackend(backend)) },
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
                    onClick = { state.eventSink(DebugScreen.Event.LoadModel) },
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
                    onClick = { state.eventSink(DebugScreen.Event.UnloadModel) },
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
private fun DeltaScorecard(
    title: String,
    delta: MemoryDelta,
    isLoad: Boolean,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetricPill("Duration", "${delta.durationMs}ms")
                MetricPill(
                    if (isLoad) "Native Δ" else "Native Freed",
                    "${"%.1f".format(if (isLoad) delta.deltaNativeMb else -delta.deltaNativeMb)} MB",
                )
                MetricPill(
                    if (isLoad) "RAM Δ" else "RAM Freed",
                    "${"%.1f".format(if (isLoad) delta.deltaSystemMb else -delta.deltaSystemMb)} MB",
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun InferenceBenchmarkCard(state: DebugScreen.State.Success) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Inference & Throughput Benchmark",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Quick Prompt Presets
            Text(text = "Benchmark Prompt Presets:", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SuggestionChip(
                    onClick = {
                        state.eventSink(
                            DebugScreen.Event.UpdateBenchmarkPrompt("Write a hello world program in Kotlin."),
                        )
                    },
                    label = { Text("Hello World (Warmup)") },
                )
                SuggestionChip(
                    onClick = {
                        state.eventSink(
                            DebugScreen.Event.UpdateBenchmarkPrompt(
                                "Write a concise Kotlin function that computes Fibonacci numbers using recursion with memoization.",
                            ),
                        )
                    },
                    label = { Text("Fibonacci") },
                )
                SuggestionChip(
                    onClick = {
                        state.eventSink(
                            DebugScreen.Event.UpdateBenchmarkPrompt(
                                "Implement a complete Merge Sort algorithm in Rust with generic type constraints and test assertions.",
                            ),
                        )
                    },
                    label = { Text("Merge Sort") },
                )
            }

            OutlinedTextField(
                value = state.benchmarkPrompt,
                onValueChange = { state.eventSink(DebugScreen.Event.UpdateBenchmarkPrompt(it)) },
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { state.eventSink(DebugScreen.Event.RunBenchmark) },
                    enabled = !state.isBenchmarking,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isBenchmarking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Streaming...")
                    } else {
                        Text("Run Benchmark")
                    }
                }

                if (state.isBenchmarking) {
                    OutlinedButton(
                        onClick = { state.eventSink(DebugScreen.Event.StopBenchmark) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
            }

            // Benchmark Telemetry Scorecard
            if (state.benchmarkTtftMs != null || state.benchmarkSpeedTps != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        MetricPill("TTFT", "${state.benchmarkTtftMs ?: 0}ms")
                        MetricPill("Speed", "${"%.1f".format(state.benchmarkSpeedTps ?: 0f)} t/s")
                        MetricPill("Tokens", "${state.benchmarkTotalTokens}")
                        MetricPill("Total Time", "${state.benchmarkDurationMs ?: 0}ms")
                    }
                }
            }

            // Streaming Token Preview Area
            if (state.benchmarkTokens.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Live Model Response:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        MarkdownMessage(
                            content = state.benchmarkTokens,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HardwareDiagnosticsCard(deviceInfo: Map<String, String>) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Hardware & Environment Specs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            deviceInfo.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageInspectorCard(
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

// ==========================================
// Previews
// ==========================================

@ThemePreviews
@Composable
private fun DebugScreenPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            DebugScreenContent(
                state =
                    DebugScreen.State.Success(
                        models =
                            listOf(
                                AiModel(
                                    id = "gemma-4-e2b",
                                    name = "Gemma 4-E2B IT",
                                    displayName = "Gemma 4-E2B IT",
                                    downloadUrl = "https://example.com/gemma",
                                    sizeBytes = 2_684_354_560L,
                                    localPath = "/data/user/0/dev.hossain.codematex/files/models/gemma.bin",
                                    downloadStatus = DownloadStatus.DOWNLOADED,
                                    preferredBackend = LlmEngine.Backend.GPU,
                                    minDeviceMemoryInGb = 8,
                                ),
                            ),
                        selectedModel = null,
                        selectedBackend = LlmEngine.Backend.GPU,
                        isModelLoaded = true,
                        loadedModelName = "Gemma 4-E2B IT",
                        activeBackend = LlmEngine.Backend.GPU,
                        isLoadingModel = false,
                        isUnloadingModel = false,
                        lastLoadDelta = MemoryDelta(deltaNativeMb = 1450f, deltaJvmMb = 12f, deltaSystemMb = 1500f, durationMs = 3400L),
                        telemetryStats =
                            DebugMemoryStats(
                                nativeAllocatedMb = 1850f,
                                nativeTotalMb = 2048f,
                                nativeFreeMb = 198f,
                                jvmUsedMb = 64f,
                                jvmTotalMb = 128f,
                                jvmMaxMb = 512f,
                                ramUsedGb = 4.2f,
                                ramTotalGb = 8.0f,
                                ramAvailGb = 3.8f,
                            ),
                        benchmarkTokens = "Here is a Fibonacci function in Kotlin:\n```kotlin\nfun fib(n: Int): Long { ... }\n```",
                        benchmarkTtftMs = 380L,
                        benchmarkSpeedTps = 14.5f,
                        benchmarkTotalTokens = 42,
                        benchmarkDurationMs = 2800L,
                        deviceInfo =
                            mapOf(
                                "Device Model" to "Google Pixel 8",
                                "Android OS" to "Android 15 (API 35)",
                                "CPU Cores" to "8 cores",
                            ),
                        eventSink = {},
                    ),
            )
        }
    }
}
