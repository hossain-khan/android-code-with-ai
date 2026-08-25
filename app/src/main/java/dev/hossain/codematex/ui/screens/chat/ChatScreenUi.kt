package dev.hossain.codematex.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import dev.hossain.codematex.ui.overlay.TutorPersonaBottomSheet
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.system.SystemResourceStats
import dev.hossain.codematex.ui.component.LiveHardwareTelemetryBars
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.TopicVisualInfo
import dev.hossain.codematex.ui.theme.visualInfo
import dev.hossain.codematex.util.formatShortModelName
import dev.zacsweers.metro.AppScope
import kotlinx.coroutines.launch

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
            ChatErrorLayout(state = state, modifier = modifier)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatErrorLayout(
    state: ChatScreen.State.Error,
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
                    .radialGradientScrim(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
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
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.size(72.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }

                    Text(
                        text = "Model Initialization Failed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { state.eventSink(ChatScreen.Event.Retry) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Retry Initialization",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { state.eventSink(ChatScreen.Event.OpenModelPicker) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = "Choose Another Model",
                            fontWeight = FontWeight.SemiBold,
                        )
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
    var showPersonaPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val visualInfo = state.topic.visualInfo
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    if (showPersonaPicker) {
        TutorPersonaBottomSheet(
            selectedPersona = state.persona,
            onPersonaSelected = { persona ->
                state.eventSink(ChatScreen.Event.SelectPersona(persona))
            },
            onDismiss = { showPersonaPicker = false },
        )
    }

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
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier =
                            Modifier
                                .padding(end = 8.dp)
                                .clickable { showPersonaPicker = true },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = state.persona.iconGlyph,
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                text = state.persona.shortName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
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
                            enabled = !state.isPreparing && !state.isGenerating,
                            onPromptSelected = { prompt ->
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                state.eventSink(ChatScreen.Event.SendMessage(prompt))
                            },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        ChatMessageList(
                            state = state,
                            listState = listState,
                            visualInfo = visualInfo,
                            modifier = Modifier.weight(1f),
                            onCopyMessage = {
                                state.eventSink(ChatScreen.Event.CopyMessage(it))
                            },
                        )
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
                        enabled = !state.isPreparing && !state.isGenerating,
                        onPromptSelected = { prompt ->
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            state.eventSink(ChatScreen.Event.SendMessage(prompt))
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    ChatMessageList(
                        state = state,
                        listState = listState,
                        visualInfo = visualInfo,
                        modifier = Modifier.weight(1f),
                        onCopyMessage = {
                            state.eventSink(ChatScreen.Event.CopyMessage(it))
                        },
                    )
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

@Composable
private fun ChatMessageList(
    state: ChatScreen.State.Active,
    listState: LazyListState,
    visualInfo: TopicVisualInfo,
    modifier: Modifier = Modifier,
    onCopyMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    // Track whether the user has explicitly scrolled up to view message history
    var userScrolledUp by remember { mutableStateOf(false) }

    // Check if the viewport is currently anchored at the bottom (index 0 in reverseLayout)
    val isAtBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 60
        }
    }

    // Detect user manual scroll gesture: if scrolling away from bottom, mark userScrolledUp
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userScrolledUp = !isAtBottom
        }
    }

    // Auto-scroll on new message added (user sent message or new session)
    val messageCount = state.messages.size
    LaunchedEffect(messageCount) {
        userScrolledUp = false
        listState.scrollToItem(0)
    }

    // Smart auto-scroll during token streaming (token count increases) as long as user hasn't scrolled up
    val lastMessageContentLength = (state.messages.lastOrNull() as? ChatMessage.Agent)?.content?.length ?: 0
    LaunchedEffect(lastMessageContentLength, state.isGenerating) {
        if (state.isGenerating && !userScrolledUp) {
            listState.scrollToItem(0)
        }
    }

    // Floating pill visibility condition: show when user is scrolled away from bottom
    val showJumpToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 80
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                    onCopy = onCopyMessage,
                )
            }
        }

        // Floating "Jump to Bottom ↓" pill
        AnimatedVisibility(
            visible = showJumpToBottom,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier =
                    Modifier.clickable {
                        userScrolledUp = false
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom",
                        modifier = Modifier.size(18.dp),
                        tint = if (state.isGenerating) visualInfo.accentColor else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (state.isGenerating) "New Response ↓" else "Jump to Bottom ↓",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyChatTopicStarters(
    visualInfo: TopicVisualInfo,
    enabled: Boolean = true,
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
            textAlign = TextAlign.Center,
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
                            .clickable(enabled = enabled) { onPromptSelected(prompt) },
                    shape = MaterialTheme.shapes.medium,
                    border =
                        BorderStroke(
                            1.dp,
                            if (enabled) {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            },
                        ),
                    colors =
                        CardDefaults.outlinedCardColors(
                            containerColor =
                                if (enabled) {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f)
                                },
                        ),
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color =
                            if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GeneratingIndicator(accentColor: Color) {
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
                    text = formatShortModelName(state.modelName),
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

            state.systemResourceStats?.let { stats ->
                LiveHardwareTelemetryBars(
                    stats = stats,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            } ?: state.systemStatsInfo?.let { stats ->
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
    visualAccent: Color,
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
                        text = formatShortModelName(state.modelName),
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

                    // Collapsed speed indicator if active or benchmark metrics when done
                    if (!isExpanded) {
                        if (state.isGenerating) {
                            Text(
                                text = "• Generating...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else if (state.throughputInfo != null) {
                            Text(
                                text = "• ${state.throughputInfo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
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

                state.systemResourceStats?.let { stats ->
                    LiveHardwareTelemetryBars(
                        stats = stats,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                } ?: state.systemStatsInfo?.let { stats ->
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

// ==========================================
// Previews
// ==========================================

// ==========================================
// Previews
// ==========================================

private val sampleActiveChatState =
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
        messages =
            listOf(
                ChatMessage.User(
                    content = "How do I launch a coroutine safely in a ViewModel?",
                ),
                ChatMessage.Agent(
                    content =
                        "In Android development, use `viewModelScope.launch` which is bound to the ViewModel's lifecycle:\n\n" +
                            "```kotlin\n" +
                            "class MyViewModel : ViewModel() {\n" +
                            "    fun loadData() {\n" +
                            "        viewModelScope.launch {\n" +
                            "            val result = repository.fetchData()\n" +
                            "            _state.value = result\n" +
                            "        }\n" +
                            "    }\n" +
                            "}\n" +
                            "```\n" +
                            "This coroutine is automatically cancelled when the ViewModel is cleared.",
                ),
            ),
        eventSink = {},
    )

@DevicePreviews
@Composable
private fun ChatScreenActivePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        ChatLayout(state = sampleActiveChatState)
    }
}

@ThemePreviews
@Composable
private fun EmptyChatTopicStartersPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            EmptyChatTopicStarters(
                visualInfo = CodingTopic.KOTLIN.visualInfo,
                onPromptSelected = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ModelTechnicalInfoPanelPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            ModelTechnicalInfoPanel(state = sampleActiveChatState)
        }
    }
}

@ThemePreviews
@Composable
private fun ChatInputFieldPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            ChatInputField(
                state = sampleActiveChatState,
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
                    sampleActiveChatState.copy(
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
