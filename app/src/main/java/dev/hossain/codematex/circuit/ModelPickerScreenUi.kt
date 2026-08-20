package dev.hossain.codematex.circuit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.util.DeviceMemory
import dev.zacsweers.metro.AppScope
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(screen = ModelPickerScreen::class, scope = AppScope::class)
@Composable
fun ModelPickerScreenContent(
    state: ModelPickerScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ModelPickerScreen.State.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }

        is ModelPickerScreen.State.Success -> {
            ModelPickerLayout(state, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ModelPickerLayout(
    state: ModelPickerScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val deviceRamGb = remember { DeviceMemory.getDeviceRamGb(context) }
    val sizeFormatter = remember { DecimalFormat("#,### MB") }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Models") },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (state.models.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("No models available yet")
            }
        } else {
            LazyVerticalGrid(
                columns = if (isExpanded) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.models) { model ->
                    val isCompatible = DeviceMemory.isModelCompatible(model.minDeviceMemoryInGb, deviceRamGb)
                    ModelCard(
                        model = model,
                        sizeFormatter = sizeFormatter,
                        isCompatible = isCompatible,
                        deviceRamGb = deviceRamGb,
                        onDownload = {
                            if (isCompatible) {
                                state.eventSink(ModelPickerScreen.Event.Download(model))
                            }
                        },
                        onCancel = {
                            state.eventSink(ModelPickerScreen.Event.CancelDownload(model))
                        },
                        onSelect = {
                            if (isCompatible) {
                                state.eventSink(ModelPickerScreen.Event.Select(model))
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModelCard(
    model: AiModel,
    sizeFormatter: DecimalFormat,
    isCompatible: Boolean,
    deviceRamGb: Int,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onSelect: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(model.displayName, style = MaterialTheme.typography.titleMedium)
            Text(sizeFormatter.format(model.sizeBytes / 1_000_000), style = MaterialTheme.typography.bodySmall)
            Text("Requires ${model.minDeviceMemoryInGb}GB RAM", style = MaterialTheme.typography.labelSmall)

            if (model.downloadStatus == DownloadStatus.DOWNLOADING) {
                val progress = model.downloadProgress.coerceIn(0, 100) / 100f
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                )
                Text(
                    text = "Downloading: ${model.downloadProgress}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                Text(model.downloadStatus.name, style = MaterialTheme.typography.labelSmall)
            }

            if (!isCompatible) {
                Surface(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "Not compatible: requires ${model.minDeviceMemoryInGb}GB RAM, device has ${deviceRamGb}GB",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (model.downloadStatus == DownloadStatus.DOWNLOADING) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }
                } else {
                    Button(
                        onClick = {
                            when {
                                isCompatible && model.downloadStatus == DownloadStatus.NOT_DOWNLOADED -> onDownload()
                                isCompatible && model.downloadStatus == DownloadStatus.FAILED -> onDownload()
                                isCompatible && model.downloadStatus == DownloadStatus.DOWNLOADED -> onSelect()
                            }
                        },
                        enabled = isCompatible,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            when {
                                !isCompatible -> "Insufficient RAM"
                                model.downloadStatus == DownloadStatus.NOT_DOWNLOADED -> "Download"
                                model.downloadStatus == DownloadStatus.DOWNLOADED -> "Select"
                                model.downloadStatus == DownloadStatus.FAILED -> "Retry"
                                else -> "Download"
                            },
                        )
                    }
                }
            }
        }
    }
}
