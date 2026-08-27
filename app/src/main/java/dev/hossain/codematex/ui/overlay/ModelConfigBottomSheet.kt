package dev.hossain.codematex.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Material 3 Modal Bottom Sheet for configuring on-device LLM hyperparameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelConfigBottomSheet(
    model: AiModel,
    initialConfig: ModelConfig,
    onSaveConfig: (ModelConfig) -> Unit,
    onResetConfig: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    @Suppress("DEPRECATION")
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        ModelConfigSheetContent(
            model = model,
            initialConfig = initialConfig,
            onSave = { config ->
                scope.launch {
                    sheetState.hide()
                    onSaveConfig(config)
                    onDismiss()
                }
            },
            onReset = {
                scope.launch {
                    sheetState.hide()
                    onResetConfig()
                    onDismiss()
                }
            },
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            },
        )
    }
}

@Composable
private fun ModelConfigSheetContent(
    model: AiModel,
    initialConfig: ModelConfig,
    onSave: (ModelConfig) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var maxTokens by remember(initialConfig) { mutableFloatStateOf(initialConfig.maxTokens.toFloat()) }
    var topK by remember(initialConfig) { mutableFloatStateOf(initialConfig.topK.toFloat()) }
    var topP by remember(initialConfig) { mutableFloatStateOf(initialConfig.topP) }
    var temperature by remember(initialConfig) { mutableFloatStateOf(initialConfig.temperature) }

    val maxTokensLimit = if (model.contextWindow > 0) model.contextWindow.toFloat().coerceIn(4096f, 32768f) else 32768f

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column {
                Text(
                    text = "Configurations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${model.displayName} configs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Sliders & Controls
        ConfigSliderItem(
            title = "Max tokens (512-${maxTokensLimit.toInt()})",
            description =
                "Limits response length. Higher values allow complete code files and detailed explanations; " +
                    "lower values generate quicker, concise answers.",
            value = maxTokens,
            valueRange = 512f..maxTokensLimit,
            steps = 0,
            displayValue = maxTokens.toInt().toString(),
            onValueChange = { maxTokens = it },
        )

        ConfigSliderItem(
            title = "TopK (1-100)",
            description =
                "Limits sampling to the top K most likely words. Lower values make code predictable and strict; " +
                    "higher values allow more varied phrasing.",
            value = topK,
            valueRange = 1f..100f,
            steps = 99,
            displayValue = topK.toInt().toString(),
            onValueChange = { topK = it },
        )

        ConfigSliderItem(
            title = "TopP (0.00-1.00)",
            description =
                "Samples dynamically from the most probable words up to cumulative percentage P. " +
                    "Lower values keep answers focused; higher values allow natural variety.",
            value = topP,
            valueRange = 0.0f..1.0f,
            steps = 0,
            displayValue = String.format(Locale.US, "%.2f", topP),
            onValueChange = { topP = it },
        )

        ConfigSliderItem(
            title = "Temperature (0.00-2.00)",
            description =
                "Controls creativity and randomness. Lower values (0.1–0.3) produce precise, deterministic code; " +
                    "higher values (0.7–1.2) produce more creative and diverse responses.",
            value = temperature,
            valueRange = 0.0f..2.0f,
            steps = 0,
            displayValue = String.format(Locale.US, "%.2f", temperature),
            onValueChange = { temperature = it },
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onReset,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onSave(
                            ModelConfig(
                                temperature = temperature,
                                topK = topK.toInt(),
                                topP = topP,
                                maxTokens = maxTokens.toInt(),
                            ),
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ConfigSliderItem(
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDescription by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            IconButton(
                onClick = { showDescription = !showDescription },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Explain $title",
                    tint =
                        if (showDescription) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        },
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = showDescription,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Slider(
                value = value.coerceIn(valueRange.start, valueRange.endInclusive),
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.weight(1f),
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.widthIn(min = 68.dp),
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

// ==========================================
// Previews
// ==========================================

private val samplePreviewModel =
    AiModel(
        id = "google/gemma-2-2b-it",
        name = "gemma-2-2b-it",
        displayName = "Gemma 2B Coder",
        description = "Optimized on-device model for coding Q&A and concept explanations.",
        sizeBytes = 2_684_354_560L,
        localPath = "/data/data/models/gemma.task",
        preferredBackend = LlmEngine.Backend.GPU,
        minDeviceMemoryInGb = 6,
        downloadUrl = "https://example.com/gemma.task",
        downloadStatus = DownloadStatus.DOWNLOADED,
        contextWindow = 8192,
    )

@ThemePreviews
@Composable
private fun ModelConfigSheetContentPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            ModelConfigSheetContent(
                model = samplePreviewModel,
                initialConfig =
                    ModelConfig(
                        temperature = 0.7f,
                        topK = 40,
                        topP = 0.95f,
                        maxTokens = 4000,
                    ),
                onSave = {},
                onReset = {},
                onDismiss = {},
            )
        }
    }
}
