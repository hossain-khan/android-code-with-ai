package dev.hossain.codematex.ui.screens.aimodels

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.window.core.layout.WindowSizeClass
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.model.formattedSize
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DeviceMemoryInfo
import dev.hossain.codematex.system.ModelCompatibility
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.overlay.ModelConfigBottomSheet
import dev.hossain.codematex.ui.screens.aimodels.components.DeviceMemoryBanner
import dev.hossain.codematex.ui.screens.aimodels.components.ModelCard
import dev.hossain.codematex.ui.screens.aimodels.components.NotificationRationaleDialog
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import timber.log.Timber

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
)
@CircuitInject(screen = ModelPickerScreen::class, scope = AppScope::class)
@Composable
fun ModelPickerScreenContent(
    state: ModelPickerScreen.State,
    modifier: Modifier = Modifier,
) {
    if (SharedElementTransitionScope.isAvailable) {
        SharedElementTransitionScope {
            ModelPickerScreenInnerContent(state = state, modifier = modifier, transitionScope = this)
        }
    } else {
        ModelPickerScreenInnerContent(state = state, modifier = modifier, transitionScope = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ModelPickerScreenInnerContent(
    state: ModelPickerScreen.State,
    modifier: Modifier = Modifier,
    transitionScope: SharedElementTransitionScope? = null,
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
            ModelPickerLayout(state, modifier, transitionScope)
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
    ExperimentalPermissionsApi::class,
)
@Composable
private fun ModelPickerLayout(
    state: ModelPickerScreen.State.Success,
    modifier: Modifier = Modifier,
    transitionScope: SharedElementTransitionScope? = null,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE) }
    var pendingDownloadModel by remember { mutableStateOf<AiModel?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val notificationPermissionState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            null
        }

    val triggerDownload: (AiModel) -> Unit = { model ->
        state.eventSink(ModelPickerScreen.Event.Download(model))
    }

    if (showPermissionRationale && pendingDownloadModel != null) {
        val modelToDownload = pendingDownloadModel!!
        NotificationRationaleDialog(
            modelSize = modelToDownload.formattedSize,
            onEnableNotifications = {
                Timber.d("ModelPicker: User agreed to enable notifications for ${modelToDownload.displayName}")
                showPermissionRationale = false
                pendingDownloadModel = null
                prefs.edit { putBoolean("has_prompted_notifications", true) }
                notificationPermissionState?.launchPermissionRequest()
                triggerDownload(modelToDownload)
            },
            onDismiss = {
                Timber.d("ModelPicker: User opted out of notifications for ${modelToDownload.displayName}")
                showPermissionRationale = false
                pendingDownloadModel = null
                prefs.edit { putBoolean("has_prompted_notifications", true) }
                triggerDownload(modelToDownload)
            },
        )
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
                    val compatibility =
                        state.modelCompatibility[model.id]
                            ?: ModelCompatibility.Incompatible("Unknown compatibility")
                    val isCompatible = compatibility is ModelCompatibility.Compatible
                    ModelCard(
                        model = model,
                        compatibility = compatibility,
                        transitionScope = transitionScope,
                        onDownload = {
                            if (isCompatible) {
                                val hasPrompted = prefs.getBoolean("has_prompted_notifications", false)
                                val needsRationale =
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        notificationPermissionState?.status?.isGranted == false &&
                                        !hasPrompted

                                if (needsRationale) {
                                    Timber.d("ModelPicker: Showing in-context notification permission rationale for ${model.displayName}")
                                    pendingDownloadModel = model
                                    showPermissionRationale = true
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        notificationPermissionState?.status?.isGranted == false
                                    ) {
                                        notificationPermissionState.launchPermissionRequest()
                                    }
                                    triggerDownload(model)
                                }
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

@OptIn(ExperimentalSharedTransitionApi::class)
@DevicePreviews
@Composable
private fun ModelPickerScreenPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        PreviewSharedElementTransitionLayout {
            SharedElementTransitionScope {
                ModelPickerLayout(
                    state =
                        ModelPickerScreen.State.Success(
                            models = sampleModels,
                            deviceMemoryInfo = DeviceMemoryInfo(totalBytes = 12_000_000_000L, displayTotalGb = 12.0, displayLabel = "GB"),
                            modelCompatibility = sampleModels.associate { it.id to ModelCompatibility.Compatible },
                            eventSink = {},
                        ),
                    transitionScope = this,
                )
            }
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
