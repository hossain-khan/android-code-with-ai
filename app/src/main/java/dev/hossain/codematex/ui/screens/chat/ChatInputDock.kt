package dev.hossain.codematex.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.system.SystemResourceStats
import dev.hossain.codematex.ui.component.LiveHardwareTelemetryBars
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.hossain.codematex.util.formatShortModelName

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GeneratingIndicator(accentColor: Color) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularWavyProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = accentColor,
                )
                Text(
                    text = "Thinking...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun SaveErrorBanner(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onRetry,
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ChatInputField(
    state: ChatScreen.State.Active,
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (state.isPreparing) {
                val shortModelName = formatShortModelName(state.modelName)
                val labelText =
                    if (shortModelName.isNotBlank()) {
                        "Initializing $shortModelName model..."
                    } else {
                        "Initializing model..."
                    }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = state.topic.visualInfo.accentColor,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    state.systemResourceStats?.let { stats ->
                        LiveHardwareTelemetryBars(
                            stats = stats,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            cpuColor = state.topic.visualInfo.accentColor,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Ask about ${state.topic.displayName}...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                keyboardOptions =
                    KeyboardOptions(
                        imeAction = ImeAction.Send,
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !state.isGenerating && !state.isPreparing) {
                                onSendMessage()
                            }
                        },
                    ),
                shape = MaterialTheme.shapes.extraLarge,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    ),
                trailingIcon = {
                    if (state.isGenerating) {
                        FilledIconButton(
                            onClick = { state.eventSink(ChatScreen.Event.StopGeneration) },
                            colors =
                                IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                        }
                    } else {
                        FilledIconButton(
                            enabled = inputText.isNotBlank() && !state.isPreparing,
                            onClick = onSendMessage,
                            colors =
                                IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Default.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                maxLines = 4,
            )
        }
    }
}

// ==========================================
// Previews
// ==========================================

private val sampleInputDockState =
    ChatScreen.State.Active(
        topic = CodingTopic.KOTLIN,
        modelName = "gemma-4-E2B-it-litert-lm",
        activeBackend = "GPU",
        modelSize = "2,588 MB",
        modelMemory = "Requires 4GB RAM",
        configInfo = "Temp: 0.7 • Top-K: 40 • Top-P: 1.0",
        throughputInfo = "TTFT: 480ms • Speed: 14.2 t/s",
        systemStatsInfo = null,
        persona = TutorPersona.SENIOR_ENGINEER,
        isPreparing = false,
        isGenerating = false,
        messages = emptyList(),
        eventSink = {},
    )

@ThemePreviews
@Composable
private fun ChatInputFieldPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            ChatInputField(
                state = sampleInputDockState,
                inputText = "Tell me about Compose State",
                onInputTextChanged = {},
                onSendMessage = {},
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ChatInputFieldPreparingPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            ChatInputField(
                state =
                    sampleInputDockState.copy(
                        isPreparing = true,
                        systemResourceStats =
                            SystemResourceStats(
                                cpuPercent = 42f,
                                ramUsedGb = 3.8f,
                                ramTotalGb = 8.0f,
                            ),
                    ),
                inputText = "",
                onInputTextChanged = {},
                onSendMessage = {},
            )
        }
    }
}

@ThemePreviews
@Composable
private fun SaveErrorBannerPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            SaveErrorBanner(
                errorMessage = "Failed to save conversation: SQLite disk full",
                onRetry = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
