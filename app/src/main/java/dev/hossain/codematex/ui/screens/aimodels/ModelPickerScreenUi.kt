package dev.hossain.codematex.ui.screens.aimodels

import android.Manifest
import android.os.Build
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.model.formattedContextWindow
import dev.hossain.codematex.data.model.formattedSize
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DeviceMemoryInfo
import dev.hossain.codematex.system.ModelCompatibility
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.overlay.AppInfoBottomSheet
import dev.hossain.codematex.ui.overlay.ModelConfigBottomSheet
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.util.formatShortModelName
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

        is ModelPickerScreen.State.Error -> {
            ModelPickerErrorLayout(state, modifier)
        }

        is ModelPickerScreen.State.Success -> {
            ModelPickerLayout(state, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerErrorLayout(
    state: ModelPickerScreen.State.Error,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("AI Models") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(ModelPickerScreen.Event.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .radialGradientScrim(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.92f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.size(64.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    Text(
                        text = "Failed to Load Models",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Button(
                        onClick = { state.eventSink(ModelPickerScreen.Event.Retry) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { state.eventSink(ModelPickerScreen.Event.Back) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text("Go Back", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalPermissionsApi::class,
)
@Composable
private fun ModelPickerLayout(
    state: ModelPickerScreen.State.Success,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    var showAppInfo by remember { mutableStateOf(false) }

    val notificationPermissionState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            null
        }

    if (showAppInfo) {
        AppInfoBottomSheet(onDismiss = { showAppInfo = false })
    }

    state.configuredModel?.let { configuredModel ->
        ModelConfigBottomSheet(
            model = configuredModel,
            initialConfig = state.configuredModelConfig ?: ModelConfig(),
            onSaveConfig = { config ->
                state.eventSink(ModelPickerScreen.Event.SaveModelConfig(configuredModel, config))
            },
            onResetConfig = {
                state.eventSink(ModelPickerScreen.Event.ResetModelConfig(configuredModel))
            },
            onDismiss = {
                state.eventSink(ModelPickerScreen.Event.DismissModelConfig)
            },
        )
    }

    Scaffold(
        modifier =
            modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .radialGradientScrim(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Models", fontWeight = FontWeight.Bold)
                        val selectedModel = state.models.firstOrNull { it.isSelected }
                        if (selectedModel != null) {
                            Text(
                                text = "Active: ${selectedModel.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(ModelPickerScreen.Event.Back) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAppInfo = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About CodeMateX",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = if (isExpanded) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DeviceMemoryBanner(deviceMemoryInfo = state.deviceMemoryInfo)
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                DownloadSettingsCard(
                    downloadOverWifiOnly = state.downloadOverWifiOnly,
                    onToggleWifiOnly = { state.eventSink(ModelPickerScreen.Event.ToggleWifiOnly(it)) },
                )
            }

            if (state.models.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No AI models available at this time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(state.models) { model ->
                    val compatibility = state.modelCompatibility[model.id] ?: ModelCompatibility.Incompatible("Unknown compatibility")
                    val isCompatible = compatibility is ModelCompatibility.Compatible
                    ModelCard(
                        model = model,
                        compatibility = compatibility,
                        onDownload = {
                            if (isCompatible) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    notificationPermissionState?.status?.isGranted == false
                                ) {
                                    notificationPermissionState.launchPermissionRequest()
                                }
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
                        onDelete = {
                            state.eventSink(ModelPickerScreen.Event.Delete(model))
                        },
                        onConfigure = {
                            state.eventSink(ModelPickerScreen.Event.OpenModelConfig(model))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceMemoryBanner(
    deviceMemoryInfo: DeviceMemoryInfo,
    modifier: Modifier = Modifier,
) {
    val ramFormatter = remember { DecimalFormat("#,##0.0") }
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .radialGradientScrim(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Device Memory",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${ramFormatter.format(deviceMemoryInfo.displayTotalGb)} ${deviceMemoryInfo.displayLabel} Total RAM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Models requiring more RAM than available may be disabled for stability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModelCard(
    model: AiModel,
    compatibility: ModelCompatibility,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onConfigure: () -> Unit,
) {
    val isCompatible = compatibility is ModelCompatibility.Compatible
    val uriHandler = LocalUriHandler.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = {
                Text(
                    text = "Delete ${formatShortModelName(model.displayName)}?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text =
                        "Are you sure you want to delete this model? " +
                            "This will permanently remove the model file from your device and free up ${model.formattedSize} of storage space.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        shape = MaterialTheme.shapes.large,
        border =
            if (model.isSelected) {
                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header Row: Model Title + Size Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false),
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = model.formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Model Description
            if (model.description.isNotBlank()) {
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Specs & Badges Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = "Requires ${model.minDeviceMemoryInGb}GB RAM",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (model.quantization.isNotBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = model.quantization,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                model.formattedContextWindow?.let { contextWindow ->
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            text = contextWindow,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = "LiteRT-LM",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = model.license,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Source & Provenance Link
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            uriHandler.openUri(model.modelRepoUrl)
                        },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Source:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = "Hugging Face (${model.publisher})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in browser",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            if (model.downloadStatus == DownloadStatus.DOWNLOADING) {
                val animatedProgress by animateFloatAsState(
                    targetValue = model.downloadProgress.coerceIn(0, 100) / 100f,
                    animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
                    label = "DownloadProgressAnimation",
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearWavyProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Downloading model weights...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${model.downloadProgress}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (!isCompatible) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        val reason = (compatibility as? ModelCompatibility.Incompatible)?.reason ?: "Insufficient RAM"
                        Text(
                            reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            } else if (model.downloadStatus == DownloadStatus.FAILED) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            model.downloadErrorMessage?.takeIf { it.isNotBlank() }
                                ?: "Download failed. Check your network or disk space and try again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (model.downloadStatus == DownloadStatus.DOWNLOADING) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel Download")
                    }
                } else if (model.downloadStatus == DownloadStatus.DOWNLOADED) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (model.isSelected) {
                            FilledTonalButton(
                                onClick = onSelect,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Active Model")
                            }
                        } else {
                            Button(
                                onClick = onSelect,
                                enabled = isCompatible,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Model")
                            }
                        }
                    }

                    OutlinedIconButton(
                        onClick = onConfigure,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Configure ${model.displayName}",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    OutlinedIconButton(
                        onClick = { showDeleteConfirmation = true },
                        colors =
                            IconButtonDefaults.outlinedIconButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete ${model.displayName}",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            when {
                                isCompatible && model.downloadStatus == DownloadStatus.NOT_DOWNLOADED -> onDownload()
                                isCompatible && model.downloadStatus == DownloadStatus.FAILED -> onDownload()
                            }
                        },
                        enabled = isCompatible,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val icon =
                            when {
                                model.downloadStatus == DownloadStatus.FAILED -> Icons.Default.CloudDownload
                                else -> Icons.Default.CloudDownload
                            }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            when {
                                !isCompatible -> "Insufficient RAM"
                                model.downloadStatus == DownloadStatus.NOT_DOWNLOADED -> "Download Model"
                                model.downloadStatus == DownloadStatus.FAILED -> "Retry Download"
                                else -> "Download Model"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadSettingsCard(
    downloadOverWifiOnly: Boolean,
    onToggleWifiOnly: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Download on Wi-Fi only",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Prevent large model downloads (2.6GB–3.7GB) from consuming cellular data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = downloadOverWifiOnly,
                onCheckedChange = onToggleWifiOnly,
            )
        }
    }
}

// ==========================================
// Previews
// ==========================================

private val sampleModels =
    listOf(
        AiModel(
            id = "litert-community/gemma-4-E2B-it-litert-lm",
            name = "gemma-4-E2B-it-litert-lm",
            displayName = "Gemma 4-E2B IT",
            description = "Instruction-tuned on-device coding model with superior reasoning.",
            sizeBytes = 2_588_000_000L,
            localPath = "models/gemma-4-E2B.bin",
            preferredBackend = LlmEngine.Backend.GPU,
            minDeviceMemoryInGb = 4,
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
            modelRepoUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm",
            license = "Apache 2.0",
            publisher = "Google",
            downloadStatus = DownloadStatus.DOWNLOADED,
            isSelected = true,
            contextWindow = 8192,
            quantization = "INT4",
            promptFormat = "GEMMA",
        ),
        AiModel(
            id = "litert-community/Phi-4-mini-instruct",
            name = "Phi-4-mini-instruct",
            displayName = "Phi-4 Mini Instruct (3.8B)",
            description = "Advanced multi-step reasoning for devices with high memory capacity.",
            sizeBytes = 3_910_090_752L,
            localPath = null,
            preferredBackend = LlmEngine.Backend.GPU,
            minDeviceMemoryInGb = 14,
            downloadUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct",
            modelRepoUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct",
            license = "MIT",
            publisher = "Google LiteRT Community",
            downloadStatus = DownloadStatus.NOT_DOWNLOADED,
            isSelected = false,
            contextWindow = 128000,
            quantization = "Q8",
            promptFormat = "PHI",
        ),
        AiModel(
            id = "litert-community/Qwen2.5-Coder-1.5B-Instruct",
            name = "Qwen2.5-Coder-1.5B-Instruct",
            displayName = "Qwen 2.5 Coder 1.5B",
            description = "Best lightweight model for code explanations, syntax fixes, and Q&A.",
            sizeBytes = 1_117_385_648L,
            localPath = null,
            preferredBackend = LlmEngine.Backend.CPU,
            minDeviceMemoryInGb = 3,
            downloadUrl =
                "https://light-llm-storage.gohk.xyz/models/litert-community/Qwen2.5-Coder-1.5B-Instruct/Qwen2.5-Coder-1.5B-Instruct_int4.litertlm",
            modelRepoUrl = "https://huggingface.co/litert-community/Qwen2.5-Coder-1.5B-Instruct",
            license = "Apache 2.0",
            publisher = "Google LiteRT Community",
            downloadStatus = DownloadStatus.NOT_DOWNLOADED,
            isSelected = false,
            contextWindow = 32768,
            quantization = "INT4",
            promptFormat = "CHATML",
        ),
        AiModel(
            id = "litert-community/Qwen3-0.6B",
            name = "Qwen3-0.6B",
            displayName = "Qwen 3 0.6B",
            description = "Ultra-compact efficient model designed for high-speed local inference on resource-constrained devices.",
            sizeBytes = 614_236_160L,
            localPath = null,
            preferredBackend = LlmEngine.Backend.CPU,
            minDeviceMemoryInGb = 4,
            downloadUrl =
                "https://light-llm-storage.gohk.xyz/models/litert-community/Qwen3-0.6B/Qwen3-0.6B.litertlm",
            modelRepoUrl = "https://huggingface.co/litert-community/Qwen3-0.6B",
            license = "Apache 2.0",
            publisher = "Google LiteRT Community",
            downloadStatus = DownloadStatus.NOT_DOWNLOADED,
            isSelected = false,
            contextWindow = 4096,
            quantization = "INT8",
            promptFormat = "CHATML",
        ),
    )

@DevicePreviews
@Composable
private fun ModelPickerScreenPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        ModelPickerLayout(
            state =
                ModelPickerScreen.State.Success(
                    models = sampleModels,
                    deviceMemoryInfo = DeviceMemoryInfo(totalBytes = 12_000_000_000L, displayTotalGb = 12.0, displayLabel = "GB"),
                    modelCompatibility = sampleModels.associate { it.id to ModelCompatibility.Compatible },
                    downloadOverWifiOnly = true,
                    eventSink = {},
                ),
        )
    }
}

@ThemePreviews
@Composable
private fun DeviceMemoryBannerPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        DeviceMemoryBanner(
            deviceMemoryInfo = DeviceMemoryInfo(totalBytes = 12_000_000_000L, displayTotalGb = 12.0, displayLabel = "GB"),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@ThemePreviews
@Composable
private fun DownloadSettingsCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        DownloadSettingsCard(
            downloadOverWifiOnly = true,
            onToggleWifiOnly = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@ThemePreviews
@Composable
private fun ModelCardPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModelCard(
                model = sampleModels[0],
                compatibility = ModelCompatibility.Compatible,
                onDownload = {},
                onCancel = {},
                onSelect = {},
                onDelete = {},
                onConfigure = {},
            )
            ModelCard(
                model = sampleModels[1],
                compatibility = ModelCompatibility.Incompatible("Requires 3GB RAM (Device has 12GB)"),
                onDownload = {},
                onCancel = {},
                onSelect = {},
                onDelete = {},
                onConfigure = {},
            )
        }
    }
}

@ThemePreviews
@DevicePreviews
@Composable
private fun ModelPickerErrorPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        ModelPickerErrorLayout(
            state =
                ModelPickerScreen.State.Error(
                    message = "Unable to connect to Hugging Face repository. Check your network connection.",
                    eventSink = {},
                ),
        )
    }
}
