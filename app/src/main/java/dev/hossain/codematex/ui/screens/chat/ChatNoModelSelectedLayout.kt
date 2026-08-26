package dev.hossain.codematex.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.visualInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NoModelSelectedLayout(
    state: ChatScreen.State.NoModelSelected,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val visualInfo = state.topic.visualInfo

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = visualInfo.accentColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
                        ) {
                            Text(
                                text = visualInfo.iconGlyph,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = visualInfo.accentColor,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                        Text(
                            text = state.topic.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(ChatScreen.Event.Back) }) {
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
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = visualInfo.accentColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.5.dp, visualInfo.accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.size(72.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (state.hasDownloadedModels) Icons.Default.Tune else Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = visualInfo.accentColor,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }

                    Text(
                        text = if (state.hasDownloadedModels) "Select an On-Device Model" else "No AI Model Downloaded",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text =
                            if (state.hasDownloadedModels) {
                                "You have downloaded models ready on your device. Select an active model to start chatting with your AI coding tutor."
                            } else {
                                "CodeMateX runs 100% locally on your device for privacy and offline tutoring. Download an open LLM model (e.g. Gemma) to get started."
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { state.eventSink(ChatScreen.Event.OpenModelPicker) },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = visualInfo.accentColor,
                                contentColor = MaterialTheme.colorScheme.surface,
                            ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = if (state.hasDownloadedModels) Icons.Default.Tune else Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = if (state.hasDownloadedModels) "Select AI Model" else "Download AI Models",
                                fontWeight = FontWeight.Bold,
                            )
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
private fun NoModelSelectedLayoutPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            NoModelSelectedLayout(
                state =
                    ChatScreen.State.NoModelSelected(
                        topic = CodingTopic.KOTLIN,
                        hasDownloadedModels = false,
                        eventSink = {},
                    ),
            )
        }
    }
}
