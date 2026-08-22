package dev.hossain.codematex.circuit

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.window.core.layout.WindowSizeClass
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.visualInfo
import dev.hossain.codematex.util.formatShortModelName
import dev.zacsweers.metro.AppScope

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@CircuitInject(screen = ChatScreen::class, scope = AppScope::class)
@Composable
fun ChatScreenContent(
    state: ChatScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ChatScreen.State.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator()
            }
        }

        is ChatScreen.State.NoModelSelected -> {
            NoModelSelectedLayout(state = state, modifier = modifier)
        }

        is ChatScreen.State.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    IconButton(onClick = { state.eventSink(ChatScreen.Event.Retry) }) {
                        Text("Retry")
                    }
                }
            }
        }

        is ChatScreen.State.Active -> {
            ChatLayout(state, modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoModelSelectedLayout(
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

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
private fun ChatLayout(
    state: ChatScreen.State.Active,
    modifier: Modifier = Modifier,
) {
    // Keep the screen awake while the model is actively streaming tokens
    val currentView = LocalView.current
    DisposableEffect(state.isGenerating) {
        currentView.keepScreenOn = state.isGenerating
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val visualInfo = state.topic.visualInfo
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .radialGradientScrim(visualInfo.accentColor.copy(alpha = 0.15f)),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = visualInfo.accentColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.5f)),
                        ) {
                            Text(
                                text = visualInfo.iconGlyph,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = visualInfo.accentColor,
                            )
                        }
                        Text(state.topic.displayName, fontWeight = FontWeight.Bold)
                    }
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(ChatScreen.Event.Back) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (isExpanded) {
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                ) {
                    if (state.messages.isEmpty()) {
                        EmptyChatTopicStarters(
                            visualInfo = visualInfo,
                            onPromptSelected = { prompt ->
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                state.eventSink(ChatScreen.Event.SendMessage(prompt))
                            },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            state = listState,
                            reverseLayout = true,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (state.isGenerating) {
                                item {
                                    GeneratingIndicator(visualInfo.accentColor)
                                }
                            }
                            items(state.messages.reversed(), key = { it.id }) { message ->
                                MessageBubble(
                                    message = message,
                                    visualAccent = visualInfo.accentColor,
                                    onCopy = {
                                        state.eventSink(ChatScreen.Event.CopyMessage(it))
                                    },
                                )
                            }
                        }
                    }

                    ChatInputField(
                        state = state,
                        inputText = inputText,
                        onInputTextChanged = { inputText = it },
                        onSendMessage = {
                            if (inputText.isNotBlank()) {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                state.eventSink(ChatScreen.Event.SendMessage(inputText))
                                inputText = ""
                            }
                        },
                    )
                }

                Surface(
                    modifier =
                        Modifier
                            .width(360.dp)
                            .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Model Telemetry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        SupportingBenchmarkingCard(state)
                    }
                }
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding(),
            ) {
                ModelTechnicalInfoPanel(state)

                if (state.messages.isEmpty()) {
                    EmptyChatTopicStarters(
                        visualInfo = visualInfo,
                        onPromptSelected = { prompt ->
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            state.eventSink(ChatScreen.Event.SendMessage(prompt))
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = listState,
                        reverseLayout = true,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.isGenerating) {
                            item {
                                GeneratingIndicator(visualInfo.accentColor)
                            }
                        }
                        items(state.messages.reversed(), key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                visualAccent = visualInfo.accentColor,
                                onCopy = {
                                    state.eventSink(ChatScreen.Event.CopyMessage(it))
                                },
                            )
                        }
                    }
                }

                ChatInputField(
                    state = state,
                    inputText = inputText,
                    onInputTextChanged = { inputText = it },
                    onSendMessage = {
                        if (inputText.isNotBlank()) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            state.eventSink(ChatScreen.Event.SendMessage(inputText))
                            inputText = ""
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChatTopicStarters(
    visualInfo: dev.hossain.codematex.ui.theme.TopicVisualInfo,
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = visualInfo.accentColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.4f)),
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = visualInfo.iconGlyph,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = visualInfo.accentColor,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Learn ${visualInfo.topic.displayName}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = visualInfo.tagline,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = visualInfo.accentColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                "Suggested Questions",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            visualInfo.starterPrompts.forEach { prompt ->
                OutlinedCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPromptSelected(prompt) },
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    colors =
                        CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GeneratingIndicator(accentColor: androidx.compose.ui.graphics.Color) {
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
                CircularWavyProgressIndicator(modifier = Modifier.size(16.dp))
                Text(
                    "Generating response...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatInputField(
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

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                            if (inputText.isNotBlank() && !state.isGenerating) {
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
                            enabled = inputText.isNotBlank(),
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

@Composable
private fun SupportingBenchmarkingCard(
    state: ChatScreen.State.Active,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.modelName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                state.activeBackend?.let { backend ->
                    val isAccelerated = backend == "GPU" || backend == "NPU"
                    val containerColor =
                        if (isAccelerated) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    val textColor =
                        if (isAccelerated) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }

                    Surface(
                        color = containerColor,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = backend,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = textColor,
                        )
                    }
                }
            }

            state.modelSize?.let {
                Text(
                    text = "Size: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.modelMemory?.let {
                Text(
                    text = "Memory: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.configInfo?.let { config ->
                Text(
                    text = "Sampler: $config",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.throughputInfo?.let { throughput ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = throughput,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            state.systemStatsInfo?.let { stats ->
                Text(
                    text = stats,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    visualAccent: androidx.compose.ui.graphics.Color,
    onCopy: (String) -> Unit,
) {
    val context = LocalContext.current

    if (message is ChatMessage.User) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = 48.dp),
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(end = 24.dp),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = visualAccent,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "CodeMateX",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = visualAccent,
                            )
                        }

                        IconButton(
                            modifier = Modifier.size(24.dp),
                            onClick = {
                                val content =
                                    when (message) {
                                        is ChatMessage.Agent -> message.content
                                        is ChatMessage.Error -> message.message
                                        is ChatMessage.System -> message.info
                                        is ChatMessage.User -> message.content
                                    }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Chat message", content))
                            },
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }

                    val messageContent =
                        when (message) {
                            is ChatMessage.Agent -> message.content.ifEmpty { "..." }
                            is ChatMessage.Error -> "Error: ${message.message}"
                            is ChatMessage.System -> message.info
                            is ChatMessage.User -> message.content
                        }

                    ChatMessageMarkdown(
                        content = messageContent,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Markdown rendering component for chat message bubbles using [compose-richtext](https://github.com/halilozercan/compose-richtext).
 *
 * Configured with compact font scaling and monospace code formatting optimized for mobile chat bubble readability.
 */
@Composable
private fun ChatMessageMarkdown(
    content: String,
    modifier: Modifier = Modifier,
) {
    val currentTheme = MaterialTheme.colorScheme
    val chatMarkdownStyle =
        remember(currentTheme) {
            RichTextStyle(
                paragraphSpacing = 6.sp,
                headingStyle = { level: Int, defaultStyle: TextStyle ->
                    when (level) {
                        0 -> defaultStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        1 -> defaultStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        2 -> defaultStyle.copy(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                        else -> defaultStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                codeBlockStyle =
                    CodeBlockStyle(
                        textStyle =
                            TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                lineHeight = 16.sp,
                            ),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(currentTheme.surfaceContainerLowest)
                                .padding(8.dp),
                    ),
            )
        }

    ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp)) {
        RichText(
            style = chatMarkdownStyle,
            modifier = modifier,
        ) {
            Markdown(content = content)
        }
    }
}

@Composable
private fun ModelTechnicalInfoPanel(
    state: ChatScreen.State.Active,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = state.modelName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    // Backend Badge
                    state.activeBackend?.let { backend ->
                        val isAccelerated = backend == "GPU" || backend == "NPU"
                        val containerColor =
                            if (isAccelerated) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        val textColor =
                            if (isAccelerated) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }

                        Surface(
                            color = containerColor,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                text = backend,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = textColor,
                            )
                        }
                    }

                    // Collapsed speed indicator if active
                    if (!isExpanded && state.throughputInfo != null) {
                        Text(
                            text = "• Generating...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse info" else "Expand info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isExpanded) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    state.modelSize?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.modelMemory?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                state.configInfo?.let { config ->
                    Text(
                        text = config,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.throughputInfo?.let { throughput ->
                    Text(
                        text = throughput,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                state.systemStatsInfo?.let { stats ->
                    Text(
                        text = stats,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
