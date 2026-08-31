package dev.hossain.codematex.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularWavyProgressIndicator
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.ui.component.radialGradientScrim
import dev.hossain.codematex.ui.overlay.TutorPersonaBottomSheet
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.DevicePreviews
import dev.hossain.codematex.ui.theme.visualInfo
import dev.zacsweers.metro.AppScope

@CircuitInject(ChatScreen::class, AppScope::class)
@Composable
fun ChatScreenUi(
    state: ChatScreen.State,
    modifier: Modifier = Modifier,
) {
    ChatScreenContent(state = state, modifier = modifier)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ChatScreenContent(
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
            ChatLayout(state = state, modifier = modifier)
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
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

    val isPersonaSelectionEnabled = !state.isPreparing && !state.isGenerating

    if (showPersonaPicker) {
        TutorPersonaBottomSheet(
            selectedPersona = state.persona,
            onPersonaSelected = { persona ->
                state.eventSink(ChatScreen.Event.SelectPersona(persona))
            },
            onDismiss = { showPersonaPicker = false },
            enabled = isPersonaSelectionEnabled,
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
                        color =
                            if (isPersonaSelectionEnabled) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                            },
                        border =
                            BorderStroke(
                                1.dp,
                                if (isPersonaSelectionEnabled) {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                },
                            ),
                        modifier =
                            Modifier
                                .padding(end = 8.dp)
                                .clickable(enabled = isPersonaSelectionEnabled) { showPersonaPicker = true },
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
                                color =
                                    if (isPersonaSelectionEnabled) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    },
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
                            course = state.availableCourse,
                            onStartCourse = { courseId ->
                                state.eventSink(ChatScreen.Event.OpenCourse(courseId))
                            },
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

                    if (state.saveErrorMessage != null) {
                        SaveErrorBanner(
                            errorMessage = state.saveErrorMessage,
                            onRetry = { state.eventSink(ChatScreen.Event.RetrySave) },
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
                        course = state.availableCourse,
                        onStartCourse = { courseId ->
                            state.eventSink(ChatScreen.Event.OpenCourse(courseId))
                        },
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

                if (state.saveErrorMessage != null) {
                    SaveErrorBanner(
                        errorMessage = state.saveErrorMessage,
                        onRetry = { state.eventSink(ChatScreen.Event.RetrySave) },
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
        dev.hossain.highlight.ui.HighlightThemeProvider(
            lightHighlightTheme =
                dev.hossain.highlight.ui
                    .rememberTomorrowLightTheme(),
            darkHighlightTheme =
                dev.hossain.highlight.ui
                    .rememberTomorrowNightTheme(),
        ) {
            ChatLayout(state = sampleActiveChatState)
        }
    }
}
