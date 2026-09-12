package dev.hossain.codematex.ui.screens.debug.benchmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.ui.component.MarkdownMessage
import dev.hossain.codematex.ui.screens.debug.DEFAULT_BENCHMARK_CONFIG
import dev.hossain.codematex.ui.screens.debug.DebugScreen.BenchmarkSamplerPreset
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlin.math.roundToInt

@Composable
fun BenchmarkSubUi(
    state: BenchmarkSubState,
    modifier: Modifier = Modifier,
) {
    InferenceBenchmarkCard(
        state = state,
        modifier = modifier,
    )
}

@Composable
internal fun InferenceBenchmarkCard(
    state: BenchmarkSubState,
    modifier: Modifier = Modifier,
) {
    var isSamplerExpanded by rememberSaveable { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isSamplerExpanded) 180f else 0f,
        label = "sampler_expand_rotation",
    )

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
                            BenchmarkUiEvent.UpdatePrompt("Write a hello world program in Kotlin."),
                        )
                    },
                    label = { Text("Hello World (Warmup)") },
                )
                SuggestionChip(
                    onClick = {
                        state.eventSink(
                            BenchmarkUiEvent.UpdatePrompt(
                                "Write a concise Kotlin function that computes Fibonacci numbers using recursion with memoization.",
                            ),
                        )
                    },
                    label = { Text("Fibonacci") },
                )
                SuggestionChip(
                    onClick = {
                        state.eventSink(
                            BenchmarkUiEvent.UpdatePrompt(
                                "Implement a complete Merge Sort algorithm in Rust with generic type constraints and test assertions.",
                            ),
                        )
                    },
                    label = { Text("Merge Sort") },
                )
            }

            OutlinedTextField(
                value = state.benchmarkPrompt,
                onValueChange = { state.eventSink(BenchmarkUiEvent.UpdatePrompt(it)) },
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
                                    onClick = { state.eventSink(BenchmarkUiEvent.ResetConfig) },
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
                                BenchmarkSamplerPreset.entries.forEach { preset ->
                                    val isSelected =
                                        state.benchmarkConfig.temperature == preset.config.temperature &&
                                            state.benchmarkConfig.topK == preset.config.topK &&
                                            state.benchmarkConfig.topP == preset.config.topP
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            state.eventSink(BenchmarkUiEvent.ApplyPreset(preset))
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
                                    state.eventSink(BenchmarkUiEvent.UpdateTemperature(rounded))
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
                                    state.eventSink(BenchmarkUiEvent.UpdateTopK(it.roundToInt()))
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
                                    state.eventSink(BenchmarkUiEvent.UpdateTopP(rounded))
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
                                    state.eventSink(BenchmarkUiEvent.UpdateMaxTokens(stepped))
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
                    onClick = { state.eventSink(BenchmarkUiEvent.RunBenchmark) },
                    enabled = !state.isBenchmarking && state.isModelLoaded,
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
                        Text(if (state.isModelLoaded) "Run Benchmark" else "Load Model to Benchmark")
                    }
                }

                if (state.isBenchmarking) {
                    OutlinedButton(
                        onClick = { state.eventSink(BenchmarkUiEvent.StopBenchmark) },
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
internal fun BenchmarkSliderItem(
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
private fun MetricPill(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class BenchmarkSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is BenchmarkSubScreen -> {
                SubUi<BenchmarkSubState> { state, modifier ->
                    BenchmarkSubUi(state = state, modifier = modifier)
                }
            }

            else -> {
                null
            }
        }
}

@ThemePreviews
@Composable
private fun BenchmarkSubUiPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            BenchmarkSubUi(
                state =
                    BenchmarkSubState(
                        benchmarkPrompt = "Write a quicksort implementation in Kotlin.",
                        benchmarkConfig = DEFAULT_BENCHMARK_CONFIG,
                        isBenchmarking = false,
                        benchmarkTokens = "```kotlin\nfun <T : Comparable<T>> List<T>.quickSort(): List<T> = ...\n```",
                        benchmarkTtftMs = 345,
                        benchmarkSpeedTps = 24.5f,
                        benchmarkTotalTokens = 120,
                        benchmarkDurationMs = 5200,
                        isModelLoaded = true,
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
