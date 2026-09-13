package dev.hossain.codematex.ui.screens.aimodels.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.formattedContextWindow
import dev.hossain.codematex.data.model.formattedSize
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.ModelCompatibility
import dev.hossain.codematex.ui.animation.ActiveModelBadgeSharedKey
import dev.hossain.codematex.ui.animation.ActiveModelCardSharedKey
import dev.hossain.codematex.ui.animation.ActiveModelTitleSharedKey
import dev.hossain.codematex.ui.animation.sharedBoundsNav
import dev.hossain.codematex.ui.animation.sharedElementNav
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews

/**
 * Card displaying an AI model's metadata, hardware compatibility status, download progress,
 * and lifecycle actions (download, select, configure, delete).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModelCard(
    model: AiModel,
    compatibility: ModelCompatibility,
    transitionScope: SharedElementTransitionScope? = null,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCompatible = compatibility is ModelCompatibility.Compatible
    val uriHandler = LocalUriHandler.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        DeleteModelDialog(
            modelName = model.displayName,
            modelSize = model.formattedSize,
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (model.isSelected) {
                        Modifier.sharedBoundsNav(transitionScope, ActiveModelCardSharedKey)
                    } else {
                        Modifier
                    },
                ),
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
                    modifier =
                        Modifier
                            .weight(1f, fill = false)
                            .then(
                                if (model.isSelected) {
                                    Modifier.sharedBoundsNav(transitionScope, ActiveModelTitleSharedKey)
                                } else {
                                    Modifier
                                },
                            ),
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
                        val statusText =
                            if (model.downloadProgress == 0) {
                                "Queued (Waiting for Wi-Fi or network)..."
                            } else {
                                "Downloading model weights..."
                            }
                        Text(
                            statusText,
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
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .sharedElementNav(transitionScope, ActiveModelBadgeSharedKey),
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

// ==========================================
// Previews
// ==========================================

private val previewModel =
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
    )

@OptIn(ExperimentalSharedTransitionApi::class)
@ThemePreviews
@Composable
private fun ModelCardActivePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        PreviewSharedElementTransitionLayout {
            SharedElementTransitionScope {
                Surface(modifier = Modifier.padding(16.dp)) {
                    ModelCard(
                        model = previewModel,
                        compatibility = ModelCompatibility.Compatible,
                        transitionScope = this,
                        onDownload = {},
                        onCancel = {},
                        onSelect = {},
                        onDelete = {},
                        onConfigure = {},
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@ThemePreviews
@Composable
private fun ModelCardDownloadingPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        PreviewSharedElementTransitionLayout {
            SharedElementTransitionScope {
                Surface(modifier = Modifier.padding(16.dp)) {
                    ModelCard(
                        model =
                            previewModel.copy(
                                isSelected = false,
                                downloadStatus = DownloadStatus.DOWNLOADING,
                                downloadProgress = 45,
                            ),
                        compatibility = ModelCompatibility.Compatible,
                        transitionScope = this,
                        onDownload = {},
                        onCancel = {},
                        onSelect = {},
                        onDelete = {},
                        onConfigure = {},
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@ThemePreviews
@Composable
private fun ModelCardIncompatiblePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        PreviewSharedElementTransitionLayout {
            SharedElementTransitionScope {
                Surface(modifier = Modifier.padding(16.dp)) {
                    ModelCard(
                        model =
                            previewModel.copy(
                                isSelected = false,
                                downloadStatus = DownloadStatus.NOT_DOWNLOADED,
                                minDeviceMemoryInGb = 16,
                            ),
                        compatibility = ModelCompatibility.Incompatible("Device has 8.0GB RAM. Model requires 16GB."),
                        transitionScope = this,
                        onDownload = {},
                        onCancel = {},
                        onSelect = {},
                        onDelete = {},
                        onConfigure = {},
                    )
                }
            }
        }
    }
}
