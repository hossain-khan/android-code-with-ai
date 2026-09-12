package dev.hossain.codematex.ui.screens.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.domain.runner.PlaygroundExecutionResult
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DebugMemoryStats
import dev.hossain.codematex.system.HardwareEligibility
import dev.hossain.codematex.system.MemoryDelta
import dev.hossain.codematex.ui.component.MarkdownMessage
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.screens.debug.DebugScreen.DebugDatabaseStats
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import kotlin.math.roundToInt

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

            // Edge Code Runner & Sandbox Diagnostics
            item {
                EdgeRunnerDiagnosticsCard(state)
            }

            // Hardware, Runtime & Eligibility Diagnostics
            item {
                HardwareDiagnosticsCard(
                    deviceInfo = state.deviceInfo,
                    runtimeSpecs = state.runtimeSpecs,
                    eligibility = state.hardwareEligibility,
                    isDevMode = state.isDevMode,
                )
            }

            // Downloaded Weights & Disk Storage Inspector
            item {
                StorageInspectorCard(
                    models = state.models,
                    onDeleteModel = { state.eventSink(DebugScreen.Event.DeleteModel(it)) },
                )
            }

            // Course Progress & Room Database Inspector
            item {
                DatabaseDiagnosticsCard(
                    stats = state.databaseStats,
                    onResetProgress = { state.eventSink(DebugScreen.Event.ResetAllLessonProgress) },
                    onSeedProgress = { state.eventSink(DebugScreen.Event.SeedSampleLessonProgress) },
                    onClearSessions = { state.eventSink(DebugScreen.Event.ClearAllChatSessions) },
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
    tint: androidx.compose.ui.graphics.Color,
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
                    val isDownloaded = model.downloadStatus == DownloadStatus.DOWNLOADED
                    FilterChip(
                        selected = state.selectedModel?.id == model.id,
                        onClick = { state.eventSink(DebugScreen.Event.SelectModel(model)) },
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
    var isSamplerExpanded by rememberSaveable { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isSamplerExpanded) 180f else 0f,
        label = "sampler_expand_rotation",
    )

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

            // Sampler & Decoding Tuning Panel
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { isSamplerExpanded = !isSamplerExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Column {
                                Text(
                                    text = "Sampler Configuration",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text =
                                        "T=${"%.2f".format(state.benchmarkConfig.temperature)} • " +
                                            "K=${state.benchmarkConfig.topK} • " +
                                            "P=${"%.2f".format(state.benchmarkConfig.topP)} • " +
                                            "Max=${state.benchmarkConfig.maxTokens}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSamplerExpanded) {
                                TextButton(
                                    onClick = { state.eventSink(DebugScreen.Event.ResetBenchmarkConfig) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                ) {
                                    Text("Reset", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            IconButton(
                                onClick = { isSamplerExpanded = !isSamplerExpanded },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription =
                                        if (isSamplerExpanded) "Collapse sampler settings" else "Expand sampler settings",
                                    modifier = Modifier.rotate(rotationAngle).size(20.dp),
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isSamplerExpanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Sampler Presets
                            Text(
                                text = "One-Tap Presets:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                DebugScreen.BenchmarkSamplerPreset.entries.forEach { preset ->
                                    val isSelected =
                                        state.benchmarkConfig.temperature == preset.config.temperature &&
                                            state.benchmarkConfig.topK == preset.config.topK &&
                                            state.benchmarkConfig.topP == preset.config.topP
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            state.eventSink(DebugScreen.Event.ApplySamplerPreset(preset))
                                        },
                                        label = { Text("${preset.label} (${preset.description})") },
                                    )
                                }
                            }

                            // Temperature Slider
                            BenchmarkSliderItem(
                                title = "Temperature (0.00 - 1.50)",
                                subtitle = "Deterministic code (0.1) vs creative explanations (1.0+)",
                                value = state.benchmarkConfig.temperature,
                                valueRange = 0.0f..1.5f,
                                displayValue = "%.2f".format(state.benchmarkConfig.temperature),
                                onValueChange = {
                                    val rounded = (it * 20f).roundToInt() / 20f
                                    state.eventSink(DebugScreen.Event.UpdateBenchmarkTemperature(rounded))
                                },
                            )

                            // Top-K Slider
                            BenchmarkSliderItem(
                                title = "Top-K (1 - 100)",
                                subtitle = "Limits token candidates to top K highest probabilities",
                                value = state.benchmarkConfig.topK.toFloat(),
                                valueRange = 1f..100f,
                                displayValue = "${state.benchmarkConfig.topK}",
                                onValueChange = {
                                    state.eventSink(DebugScreen.Event.UpdateBenchmarkTopK(it.roundToInt()))
                                },
                            )

                            // Top-P Slider
                            BenchmarkSliderItem(
                                title = "Top-P Nucleus (0.10 - 1.00)",
                                subtitle = "Dynamically samples tokens up to cumulative probability P",
                                value = state.benchmarkConfig.topP,
                                valueRange = 0.1f..1.0f,
                                displayValue = "%.2f".format(state.benchmarkConfig.topP),
                                onValueChange = {
                                    val rounded = (it * 20f).roundToInt() / 20f
                                    state.eventSink(DebugScreen.Event.UpdateBenchmarkTopP(rounded))
                                },
                            )

                            // Max Tokens Slider
                            BenchmarkSliderItem(
                                title = "Max Tokens (64 - 2048)",
                                subtitle = "Maximum output tokens decoded during benchmark evaluation",
                                value = state.benchmarkConfig.maxTokens.toFloat(),
                                valueRange = 64f..2048f,
                                displayValue = "${state.benchmarkConfig.maxTokens}",
                                onValueChange = {
                                    val stepped = ((it / 32f).roundToInt() * 32).coerceIn(64, 2048)
                                    state.eventSink(DebugScreen.Event.UpdateBenchmarkMaxTokens(stepped))
                                },
                            )
                        }
                    }
                }
            }

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
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            MetricPill("TTFT", "${state.benchmarkTtftMs ?: 0}ms")
                            MetricPill("Speed", "${"%.1f".format(state.benchmarkSpeedTps ?: 0f)} t/s")
                            MetricPill("Tokens", "${state.benchmarkTotalTokens}")
                            MetricPill("Total Time", "${state.benchmarkDurationMs ?: 0}ms")
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )

                        Text(
                            text =
                                "Sampler: Temp: ${"%.2f".format(state.benchmarkConfig.temperature)} • " +
                                    "Top-K: ${state.benchmarkConfig.topK} • " +
                                    "Top-P: ${"%.2f".format(state.benchmarkConfig.topP)} • " +
                                    "Max: ${state.benchmarkConfig.maxTokens} tokens",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
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
private fun BenchmarkSliderItem(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.widthIn(min = 52.dp),
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        @Suppress("DEPRECATION")
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EdgeRunnerDiagnosticsCard(state: DebugScreen.State.Success) {
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
            // Header Row with Title & Online Status Badge
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
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Edge Code Runner & Sandbox",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color =
                        if (state.isOnline) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (state.isOnline) Icons.Default.Wifi else Icons.Default.Warning,
                            contentDescription = null,
                            tint =
                                if (state.isOnline) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = if (state.isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color =
                                if (state.isOnline) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                        )
                    }
                }
            }

            // Proxy Latency / Reachability Row
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloudflare Proxy Endpoint",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "code-playground.gohk.xyz",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.proxyPingMs != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = "${state.proxyPingMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        } else if (state.proxyPingError != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    text = "Failed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { state.eventSink(DebugScreen.Event.PingProxy) },
                            enabled = !state.isPingingProxy && state.isOnline,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            if (state.isPingingProxy) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Ping", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Language Selector Chips
            Text(text = "Target Language:", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RUNNER_SUPPORTED_LANGUAGES.forEach { lang ->
                    FilterChip(
                        selected = state.runnerSelectedLang.equals(lang, ignoreCase = true),
                        onClick = { state.eventSink(DebugScreen.Event.SelectRunnerLanguage(lang)) },
                        label = { Text(lang.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // Code Snippet Editor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Test Snippet (${state.runnerSelectedLang}):",
                    style = MaterialTheme.typography.labelMedium,
                )
                TextButton(
                    onClick = { state.eventSink(DebugScreen.Event.ResetRunnerSnippet) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                ) {
                    Text("Reset Snippet", style = MaterialTheme.typography.labelSmall)
                }
            }

            OutlinedTextField(
                value = state.runnerSnippetCode,
                onValueChange = { state.eventSink(DebugScreen.Event.UpdateRunnerSnippet(it)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 10,
                textStyle =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
            )

            // Run Button
            Button(
                onClick = { state.eventSink(DebugScreen.Event.RunRunnerSnippet) },
                enabled = !state.isRunningSnippet && state.isOnline,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isRunningSnippet) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executing at Edge...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run ${state.runnerSelectedLang.replaceFirstChar { it.uppercase() }} Snippet")
                }
            }

            // Execution Result / Terminal View
            state.runnerResult?.let { result ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val (badgeText, badgeBg, badgeFg) =
                                when (result) {
                                    is PlaygroundExecutionResult.Success -> {
                                        Triple(
                                            "Success",
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }

                                    is PlaygroundExecutionResult.CompilationError -> {
                                        Triple(
                                            "Compilation Error",
                                            MaterialTheme.colorScheme.errorContainer,
                                            MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }

                                    is PlaygroundExecutionResult.NetworkError -> {
                                        Triple(
                                            "Network Error",
                                            MaterialTheme.colorScheme.errorContainer,
                                            MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                }

                            Surface(shape = CircleShape, color = badgeBg) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }

                            state.runnerDurationMs?.let { ms ->
                                Text(
                                    text = "Roundtrip: ${ms}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }

                        val outputText =
                            when (result) {
                                is PlaygroundExecutionResult.Success -> result.output
                                is PlaygroundExecutionResult.CompilationError -> result.diagnostic
                                is PlaygroundExecutionResult.NetworkError -> result.message
                            }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = outputText.ifEmpty { "(No output produced)" },
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HardwareDiagnosticsCard(
    deviceInfo: Map<String, String>,
    runtimeSpecs: Map<String, String>,
    eligibility: HardwareEligibility,
    isDevMode: Boolean,
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
            // Header Row: Title + Eligibility Badge
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
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Hardware & Runtime Specs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                val (badgeLabel, badgeBg, badgeFg) =
                    when (eligibility) {
                        is HardwareEligibility.Eligible -> {
                            if (isDevMode) {
                                Triple(
                                    "Eligible (Dev Mode)",
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            } else {
                                Triple(
                                    "Hardware Eligible",
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }

                        is HardwareEligibility.Ineligible -> {
                            Triple(
                                "Ineligible",
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }

                Surface(shape = CircleShape, color = badgeBg) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            // Ineligibility Warning Banner if applicable
            if (eligibility is HardwareEligibility.Ineligible) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = eligibility.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            // Sub-section: Device & Architecture Specs
            Text(
                text = "Device & Architecture",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            deviceInfo.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            // Sub-section: LiteRT-LM Runtime & Acceleration Delegates
            Text(
                text = "LiteRT-LM Runtime & Delegates",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            runtimeSpecs.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

@Composable
internal fun DatabaseDiagnosticsCard(
    stats: DebugDatabaseStats,
    onResetProgress: () -> Unit,
    onSeedProgress: () -> Unit,
    onClearSessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showResetProgressDialog by rememberSaveable { mutableStateOf(false) }
    var showClearSessionsDialog by rememberSaveable { mutableStateOf(false) }

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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Column {
                    Text(
                        text = "Course Progress & Database Inspector",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Curriculum progress and Room database metrics with QA test controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Sub-section: Curricula & Learning Progress
            Text(
                text = "Learning Progress Scorecard",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            val lessonPercent =
                if (stats.totalBundledLessons > 0) {
                    (stats.completedLessons * 100) / stats.totalBundledLessons
                } else {
                    0
                }

            val courseMetrics =
                listOf(
                    "Completed Lessons" to "${stats.completedLessons} / ${stats.totalBundledLessons} ($lessonPercent%)",
                    "In-Progress Lessons" to "${stats.inProgressLessons}",
                    "Course Completion" to "${stats.completedCourses} / ${stats.totalCourses} courses",
                    "Bundled Quizzes" to "${stats.totalQuizzes} quizzes across curricula",
                )

            courseMetrics.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Sub-section: Room Conversation Storage
            Text(
                text = "Room Conversation Storage",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            val sessionMetrics =
                listOf(
                    "Saved Chat Sessions" to "${stats.totalSessions} sessions",
                    "Persisted Message Rows" to "${stats.totalMessages} messages",
                )

            sessionMetrics.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Sub-section: QA Testing Actions
            Text(
                text = "QA Testing Actions",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSeedProgress,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seed Sample Progress (First 3/course)")
                }

                OutlinedButton(
                    onClick = { showResetProgressDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Lesson Progress (0%)")
                }

                OutlinedButton(
                    onClick = { showClearSessionsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Chat Sessions & Messages")
                }
            }
        }
    }

    if (showResetProgressDialog) {
        AlertDialog(
            onDismissRequest = { showResetProgressDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(
                    text = "Reset All Course Progress?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text =
                        "Are you sure you want to reset all lesson progress? " +
                            "This will set completion back to 0% across all curricula. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetProgressDialog = false
                        onResetProgress()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text("Reset Progress")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetProgressDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showClearSessionsDialog) {
        AlertDialog(
            onDismissRequest = { showClearSessionsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(
                    text = "Clear All Chat Sessions?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text =
                        "Are you sure you want to delete all saved conversations? " +
                            "This will wipe all chat sessions and message rows from the Room database. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearSessionsDialog = false
                        onClearSessions()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text("Clear Sessions")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSessionsDialog = false }) {
                    Text("Cancel")
                }
            },
        )
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
                        databaseStats =
                            DebugDatabaseStats(
                                completedLessons = 42,
                                inProgressLessons = 5,
                                totalBundledLessons = 620,
                                totalCourses = 10,
                                completedCourses = 2,
                                totalQuizzes = 45,
                                totalSessions = 12,
                                totalMessages = 158,
                            ),
                        eventSink = {},
                    ),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun EdgeRunnerDiagnosticsCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            EdgeRunnerDiagnosticsCard(
                state =
                    DebugScreen.State.Success(
                        models = emptyList(),
                        selectedModel = null,
                        selectedBackend = LlmEngine.Backend.GPU,
                        isModelLoaded = false,
                        loadedModelName = null,
                        activeBackend = null,
                        isLoadingModel = false,
                        isUnloadingModel = false,
                        isOnline = true,
                        runnerSelectedLang = "kotlin",
                        runnerSnippetCode = "fun main() {\n    println(\"Hello from CodeMateX!\")\n}",
                        runnerResult = PlaygroundExecutionResult.Success("Hello from CodeMateX!\nSum: 15"),
                        runnerDurationMs = 240L,
                        proxyPingMs = 85L,
                        eventSink = {},
                    ),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun EdgeRunnerDiagnosticsCardOfflinePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            EdgeRunnerDiagnosticsCard(
                state =
                    DebugScreen.State.Success(
                        models = emptyList(),
                        selectedModel = null,
                        selectedBackend = LlmEngine.Backend.GPU,
                        isModelLoaded = false,
                        loadedModelName = null,
                        activeBackend = null,
                        isLoadingModel = false,
                        isUnloadingModel = false,
                        isOnline = false,
                        runnerSelectedLang = "rust",
                        runnerSnippetCode = "fn main() {\n    println!(\"Hello Rust!\");\n}",
                        runnerResult =
                            PlaygroundExecutionResult.NetworkError(
                                "Internet connection required to run code on the playground.",
                            ),
                        runnerDurationMs = 12L,
                        eventSink = {},
                    ),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun HardwareDiagnosticsCardEligiblePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            HardwareDiagnosticsCard(
                deviceInfo =
                    mapOf(
                        "Manufacturer" to "Google",
                        "Device Model" to "Pixel 8 Pro",
                        "Android OS" to "Android 15 (API 35)",
                        "CPU Cores" to "8 cores",
                        "Supported ABIs" to "arm64-v8a",
                        "64-bit Architecture" to "Yes (arm64-v8a)",
                        "Total System RAM" to "12.0 GB",
                        "Authoritative RAM" to "11.45 GB (11450000000 bytes)",
                    ),
                runtimeSpecs =
                    mapOf(
                        "Inference Runtime" to "Google LiteRT-LM",
                        "Runtime Version" to LITERT_LM_VERSION,
                        "Active Backend" to "GPU (OpenCL)",
                        "GPU Acceleration" to "OpenCL / Vulkan",
                        "NPU Acceleration" to "Qualcomm Hexagon / NNAPI",
                        "CPU Fallback" to "XNNPACK SIMD (FP32/FP16)",
                        "Dev Mode Bypass" to "Disabled (8GB RAM required)",
                    ),
                eligibility = HardwareEligibility.Eligible,
                isDevMode = false,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun HardwareDiagnosticsCardIneligiblePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            HardwareDiagnosticsCard(
                deviceInfo =
                    mapOf(
                        "Manufacturer" to "Generic",
                        "Device Model" to "Android Emulator",
                        "Android OS" to "Android 14 (API 34)",
                        "CPU Cores" to "4 cores",
                        "Supported ABIs" to "arm64-v8a",
                        "64-bit Architecture" to "Yes (arm64-v8a)",
                        "Total System RAM" to "4.0 GB",
                        "Authoritative RAM" to "3.80 GB (3800000000 bytes)",
                    ),
                runtimeSpecs =
                    mapOf(
                        "Inference Runtime" to "Google LiteRT-LM",
                        "Runtime Version" to LITERT_LM_VERSION,
                        "Active Backend" to "Idle / Unloaded",
                        "GPU Acceleration" to "OpenCL / Vulkan",
                        "NPU Acceleration" to "Qualcomm Hexagon / NNAPI",
                        "CPU Fallback" to "XNNPACK SIMD (FP32/FP16)",
                        "Dev Mode Bypass" to "Disabled (8GB RAM required)",
                    ),
                eligibility =
                    HardwareEligibility.Ineligible(
                        reason = "On-device AI models require at least 8 GB RAM for stable execution.",
                        detectedRamGb = 3.8,
                        minRequiredRamGb = 8.0,
                        is64BitSupported = true,
                    ),
                isDevMode = false,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun InferenceBenchmarkCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            InferenceBenchmarkCard(
                state =
                    DebugScreen.State.Success(
                        models = emptyList(),
                        selectedModel = null,
                        selectedBackend = LlmEngine.Backend.GPU,
                        isModelLoaded = true,
                        loadedModelName = "google/gemma-2-2b-it",
                        activeBackend = LlmEngine.Backend.GPU,
                        isLoadingModel = false,
                        isUnloadingModel = false,
                        benchmarkPrompt = "Write a concise Kotlin function that computes Fibonacci numbers.",
                        benchmarkConfig =
                            ModelConfig(
                                temperature = 0.8f,
                                topK = 40,
                                topP = 0.95f,
                                maxTokens = 512,
                            ),
                        isBenchmarking = false,
                        benchmarkTokens = "```kotlin\nfun fibonacci(n: Int): Int = ...\n```",
                        benchmarkTtftMs = 384L,
                        benchmarkSpeedTps = 18.5f,
                        benchmarkTotalTokens = 120,
                        benchmarkDurationMs = 6800L,
                        eventSink = {},
                    ),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun DatabaseDiagnosticsCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface(modifier = Modifier.padding(16.dp)) {
            DatabaseDiagnosticsCard(
                stats =
                    DebugDatabaseStats(
                        completedLessons = 42,
                        inProgressLessons = 5,
                        totalBundledLessons = 620,
                        totalCourses = 10,
                        completedCourses = 2,
                        totalQuizzes = 45,
                        totalSessions = 12,
                        totalMessages = 158,
                    ),
                onResetProgress = {},
                onSeedProgress = {},
                onClearSessions = {},
            )
        }
    }
}
