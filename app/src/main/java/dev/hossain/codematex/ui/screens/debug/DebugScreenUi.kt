package dev.hossain.codematex.ui.screens.debug

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.subcircuit.SubCircuitContent
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.screens.debug.benchmark.BenchmarkOuterEvent
import dev.hossain.codematex.ui.screens.debug.benchmark.BenchmarkSubScreen
import dev.hossain.codematex.ui.screens.debug.database.DatabaseDiagnosticsOuterEvent
import dev.hossain.codematex.ui.screens.debug.database.DatabaseDiagnosticsSubScreen
import dev.hossain.codematex.ui.screens.debug.hardware.HardwareDiagnosticsOuterEvent
import dev.hossain.codematex.ui.screens.debug.hardware.HardwareDiagnosticsSubScreen
import dev.hossain.codematex.ui.screens.debug.model.ModelLifecycleOuterEvent
import dev.hossain.codematex.ui.screens.debug.model.ModelLifecycleSubScreen
import dev.hossain.codematex.ui.screens.debug.runner.EdgeRunnerOuterEvent
import dev.hossain.codematex.ui.screens.debug.runner.EdgeRunnerSubScreen
import dev.hossain.codematex.ui.screens.debug.telemetry.TelemetryOuterEvent
import dev.hossain.codematex.ui.screens.debug.telemetry.TelemetrySubScreen
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(screen = DebugScreen::class, scope = AppScope::class)
@Composable
fun DebugScreenContent(
    state: DebugScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            state.eventSink(DebugScreen.Event.ClearStatusMessage)
        }
    }

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
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Status & Diagnostics Banner
            item {
                StatusBanner(state = state)
            }

            // Real-time Memory & CPU Meters
            item {
                SubCircuitContent(
                    screen = TelemetrySubScreen,
                    outerEventSink = { event ->
                        when (event) {
                            is TelemetryOuterEvent.ShowSnackbar -> {
                                scope.launch { snackbarHostState.showSnackbar(event.message) }
                            }
                        }
                    },
                )
            }

            // Model Loading, Profiling & Weights Inspector
            item {
                SubCircuitContent(
                    screen = ModelLifecycleSubScreen,
                    outerEventSink = { event ->
                        when (event) {
                            is ModelLifecycleOuterEvent.ModelLoaded -> {
                                state.eventSink(DebugScreen.Event.ModelLoaded(event.modelName))
                            }

                            ModelLifecycleOuterEvent.ModelUnloaded -> {
                                state.eventSink(DebugScreen.Event.ModelUnloaded)
                            }

                            is ModelLifecycleOuterEvent.ShowSnackbar -> {
                                scope.launch { snackbarHostState.showSnackbar(event.message) }
                            }
                        }
                    },
                )
            }

            // Inference Performance & Benchmark Runner
            item {
                SubCircuitContent(
                    screen =
                        BenchmarkSubScreen(
                            isModelLoaded = state.isModelLoaded,
                            activeModelName = state.loadedModelName,
                        ),
                    outerEventSink = { event ->
                        when (event) {
                            is BenchmarkOuterEvent.ShowSnackbar -> {
                                scope.launch { snackbarHostState.showSnackbar(event.message) }
                            }
                        }
                    },
                )
            }

            // Edge Code Runner & Sandbox Diagnostics
            item {
                SubCircuitContent(
                    screen = EdgeRunnerSubScreen,
                    outerEventSink = { event ->
                        when (event) {
                            is EdgeRunnerOuterEvent.ShowSnackbar -> {
                                scope.launch { snackbarHostState.showSnackbar(event.message) }
                            }
                        }
                    },
                )
            }

            // Hardware, Runtime & Eligibility Diagnostics
            item {
                SubCircuitContent(
                    screen =
                        HardwareDiagnosticsSubScreen(
                            activeBackendName = if (state.isModelLoaded) state.loadedModelName else null,
                        ),
                    outerEventSink = {},
                )
            }

            // Database, Learning Progress & Chat Sessions Diagnostics
            item {
                SubCircuitContent(
                    screen = DatabaseDiagnosticsSubScreen,
                    outerEventSink = { event ->
                        when (event) {
                            is DatabaseDiagnosticsOuterEvent.ShowSnackbar -> {
                                scope.launch { snackbarHostState.showSnackbar(event.message) }
                            }
                        }
                    },
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
                        text =
                            if (state.isModelLoaded) {
                                "Engine Loaded${state.loadedModelName?.let { " ($it)" } ?: ""}"
                            } else {
                                "Engine Idle / Unloaded"
                            },
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

@ThemePreviews
@DevicePreviews
@Composable
private fun DebugScreenPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            DebugScreenContent(
                state =
                    DebugScreen.State.Success(
                        isModelLoaded = true,
                        loadedModelName = "Gemma 2B",
                        statusMessage = "Debugger ready.",
                        eventSink = {},
                    ),
            )
        }
    }
}
