package dev.hossain.codematex.ui.screens.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.ChatInferenceEvent
import dev.hossain.codematex.data.ChatInferenceOrchestrator
import dev.hossain.codematex.data.SystemStatsMonitor
import dev.hossain.codematex.data.ThroughputTracker
import dev.hossain.codematex.data.TopicPromptProvider
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.data.model.formattedSize
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.hossain.codematex.data.repository.ModelConfigStore
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.data.repository.UserPreferencesStore
import dev.hossain.codematex.data.repository.course.LearningRepository
import dev.hossain.codematex.system.ContextUsageStats
import dev.hossain.codematex.system.SystemResourceStats
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import dev.hossain.codematex.ui.screens.lessons.ChapterScreen
import dev.hossain.codematex.util.TokenEstimator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class ChatPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ChatScreen,
    private val modelRepository: ModelRepository,
    private val sessionRepository: ChatSessionRepository,
    private val configStore: ModelConfigStore,
    private val userPreferencesStore: UserPreferencesStore,
    private val chatInferenceOrchestrator: ChatInferenceOrchestrator,
    private val systemStatsMonitor: SystemStatsMonitor,
    private val topicPromptProvider: TopicPromptProvider,
    private val learningRepository: LearningRepository,
) : Presenter<ChatScreen.State> {
    @Composable
    override fun present(): ChatScreen.State {
        var messages by rememberRetained { mutableStateOf<List<ChatMessage>>(emptyList()) }
        var currentSessionId by rememberRetained { mutableStateOf(screen.sessionId) }
        var isGenerating by rememberRetained { mutableStateOf(false) }
        var isPreparing by rememberRetained { mutableStateOf(false) }
        var persona by rememberRetained { mutableStateOf(TutorPersona.SENIOR_ENGINEER) }
        var modelConfig by rememberRetained { mutableStateOf(configStore.config) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        var saveErrorMessage by rememberRetained { mutableStateOf<String?>(null) }
        var initTrigger by rememberRetained { mutableIntStateOf(0) }
        var throughputInfo by rememberRetained { mutableStateOf<String?>(null) }
        var systemStatsInfo by rememberRetained { mutableStateOf<String?>(null) }
        var systemResourceStats by rememberRetained { mutableStateOf<SystemResourceStats?>(null) }
        var availableModels by rememberRetained { mutableStateOf<List<AiModel>>(emptyList()) }
        var activeModel by rememberRetained { mutableStateOf<AiModel?>(null) }
        var availableCourse by rememberRetained { mutableStateOf<LearningCourse?>(null) }
        var hasSentInitialPrompt by rememberRetained { mutableStateOf(false) }

        LaunchedEffect(screen.topic) {
            availableCourse = learningRepository.getCourseForTopic(screen.topic)
        }

        LaunchedEffect(Unit) {
            val initial = modelRepository.getSelectedModel()
            if (initial != null) {
                activeModel = initial
            }
            launch {
                modelRepository.getAvailableModels().collect { models ->
                    availableModels = models
                    val selected = modelRepository.getSelectedModel()
                    if (selected?.id != activeModel?.id || selected?.localPath != activeModel?.localPath) {
                        activeModel = selected
                    }
                }
            }
            launch {
                userPreferencesStore.selectedPersonaFlow.collect { storedPersona ->
                    persona = storedPersona
                }
            }
        }

        LaunchedEffect(activeModel?.id) {
            val modelId = activeModel?.id
            if (modelId != null) {
                configStore.getConfigFlow(modelId).collect { currentConfig ->
                    modelConfig = currentConfig
                }
            } else {
                configStore.configFlow.collect { currentConfig ->
                    modelConfig = currentConfig
                }
            }
        }

        // Instantly load messages from disk into UI state so the user sees their conversation immediately.
        LaunchedEffect(screen.sessionId) {
            if (screen.sessionId != null && messages.isEmpty()) {
                Timber.d("ChatPresenter: Instantly loading messages for session=${screen.sessionId}")
                messages = sessionRepository.getMessages(screen.sessionId)
            }
        }

        LaunchedEffect(activeModel?.id, activeModel?.localPath, initTrigger) {
            val model = activeModel
            if (model == null) {
                Timber.w("ChatPresenter: No model selected")
                return@LaunchedEffect
            }
            Timber.d("ChatPresenter: Initializing model=${model.name}, path=${model.localPath}, persona=${persona.name}")
            isPreparing = true
            errorMessage = null
            try {
                val result =
                    chatInferenceOrchestrator.initialize(
                        model = model,
                        topic = screen.topic,
                        sessionId = screen.sessionId,
                        existingMessages = messages,
                        persona = persona,
                    )
                result
                    .onSuccess { loadedMessages ->
                        if (loadedMessages.isNotEmpty()) {
                            messages = loadedMessages
                        }
                        Timber.d("ChatPresenter: Model initialized successfully")
                    }.onFailure { error ->
                        Timber.e(error, "ChatPresenter: Model initialization failed")
                        errorMessage = error.message
                    }
            } catch (e: CancellationException) {
                Timber.d("ChatPresenter: Model initialization cancelled")
                throw e
            } finally {
                isPreparing = false
            }
        }

        LaunchedEffect(isGenerating, isPreparing) {
            if (isGenerating || isPreparing) {
                systemStatsMonitor.monitorMetricsWhileActive(
                    isActive = { isGenerating || isPreparing },
                    onMetrics = { stats ->
                        systemResourceStats = stats
                        systemStatsInfo = stats.formattedSummary
                    },
                )
            } else {
                systemResourceStats = null
                systemStatsInfo = null
            }
        }

        val scope = rememberCoroutineScope()

        val eventSink: (ChatScreen.Event) -> Unit = { event ->
            when (event) {
                is ChatScreen.Event.SendMessage -> {
                    if (!isGenerating && !isPreparing && event.text.isNotBlank()) {
                        isGenerating = true
                        saveErrorMessage = null
                        val input = event.text
                        Timber.d("ChatPresenter: Starting inference. Input: '${input.take(100)}' (length: ${input.length})")

                        messages = messages + ChatMessage.User(input)
                        messages = messages + ChatMessage.Agent(content = "", isStreaming = true)
                        throughputInfo = "Prefilling..."

                        scope.launch {
                            // Capture the active model at inference start so the saved session
                            // is tagged with the model that actually generated the response.
                            val currentModel = activeModel
                            val modelName = currentModel?.name ?: "Unknown"

                            val throughputTracker = ThroughputTracker()
                            try {
                                chatInferenceOrchestrator.sendMessage(input).collect { inferenceEvent ->
                                    when (inferenceEvent) {
                                        is ChatInferenceEvent.Token -> {
                                            val isFirstToken =
                                                throughputTracker.currentTokenCount == 0 &&
                                                    inferenceEvent.partialToken.isNotEmpty()
                                            val lastAgent = messages.last() as? ChatMessage.Agent
                                            if (lastAgent != null) {
                                                messages =
                                                    messages.dropLast(1) +
                                                    lastAgent.copy(
                                                        content = lastAgent.content + inferenceEvent.partialToken,
                                                        isStreaming = true,
                                                    )
                                            }
                                            throughputInfo = throughputTracker.recordToken(inferenceEvent.partialToken)
                                            if (isFirstToken) {
                                                val ttftText = throughputTracker.ttftMs?.let { "${it}ms" } ?: "--"
                                                Timber.d(
                                                    "ChatPresenter: First token received (TTFT: $ttftText). Streaming response started...",
                                                )
                                            }
                                        }

                                        ChatInferenceEvent.Done -> {
                                            val lastAgent = messages.last() as? ChatMessage.Agent
                                            if (lastAgent != null) {
                                                messages =
                                                    messages.dropLast(1) +
                                                    lastAgent.copy(
                                                        content = lastAgent.content,
                                                        isStreaming = false,
                                                    )
                                            }
                                            val finalThroughput = throughputTracker.finalize()
                                            throughputInfo = finalThroughput
                                            isGenerating = false
                                            Timber.d(
                                                "ChatPresenter: Inference completed. $finalThroughput (total tokens: ${throughputTracker.currentTokenCount})",
                                            )

                                            if (screen.saveToHistory) {
                                                // Save session message history in an independent try-catch block
                                                // so persistence failures do not discard or overwrite the generated response.
                                                Timber.d("ChatPresenter: Saving session message history...")
                                                try {
                                                    currentSessionId =
                                                        sessionRepository.saveSession(
                                                            topic = screen.topic,
                                                            messages = messages,
                                                            sessionId = currentSessionId,
                                                            modelUsed = modelName,
                                                        )
                                                    saveErrorMessage = null
                                                } catch (e: CancellationException) {
                                                    throw e
                                                } catch (e: Exception) {
                                                    Timber.e(e, "ChatPresenter: Failed to save session history")
                                                    saveErrorMessage = e.message ?: "Failed to save conversation"
                                                }
                                            } else {
                                                Timber.d("ChatPresenter: Ephemeral session active, skipping persistence.")
                                                saveErrorMessage = null
                                            }
                                        }

                                        is ChatInferenceEvent.BackendFailed -> {
                                            Timber.w(
                                                "ChatPresenter: Backend ${inferenceEvent.backend} failed, clearing partial output for fallback",
                                            )
                                            val lastAgent = messages.last() as? ChatMessage.Agent
                                            if (lastAgent != null && lastAgent.isStreaming) {
                                                messages =
                                                    messages.dropLast(1) +
                                                    lastAgent.copy(
                                                        content = "",
                                                        isStreaming = true,
                                                    )
                                            }
                                            throughputInfo = "Falling back from ${inferenceEvent.backend}..."
                                        }
                                    }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.e(
                                    e,
                                    "ChatPresenter: Inference failed after ${throughputTracker.currentTokenCount} tokens",
                                )
                                isGenerating = false
                                throughputInfo = "Error: ${e.message}"
                                messages = messages.dropLast(1) + ChatMessage.Error(e.message ?: "Inference failed")
                                initTrigger++
                            }
                        }
                    }
                }

                is ChatScreen.Event.SelectPersona -> {
                    if (!isGenerating && !isPreparing && persona != event.persona) {
                        Timber.d("ChatPresenter: Switching persona to ${event.persona.name}")
                        val newPersona = event.persona
                        persona = newPersona
                        val notice =
                            ChatMessage.System("Switched tutor persona to ${newPersona.iconGlyph} ${newPersona.displayName}")
                        val updatedMessages = messages + notice
                        messages = updatedMessages
                        scope.launch {
                            userPreferencesStore.setSelectedPersona(newPersona)
                            isPreparing = true
                            try {
                                chatInferenceOrchestrator.switchPersona(screen.topic, newPersona, updatedMessages)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.e(e, "ChatPresenter: Error switching persona")
                            } finally {
                                isPreparing = false
                            }
                        }
                    }
                }

                ChatScreen.Event.StopGeneration -> {
                    if (isGenerating) {
                        Timber.d("ChatPresenter: StopGeneration event received. Stopping LLM engine...")
                        chatInferenceOrchestrator.stop()
                        isGenerating = false
                        val lastAgent = messages.lastOrNull() as? ChatMessage.Agent
                        if (lastAgent != null && lastAgent.isStreaming) {
                            messages = messages.dropLast(1) + lastAgent.copy(isStreaming = false)
                        }
                    }
                }

                ChatScreen.Event.ResetSession -> {
                    if (!isGenerating && !isPreparing) {
                        Timber.d("ChatPresenter: ResetSession event received. Clearing message history and resetting engine...")
                        messages = emptyList()
                        currentSessionId = null
                        saveErrorMessage = null
                        throughputInfo = null
                        systemStatsInfo = null
                        systemResourceStats = null
                        scope.launch {
                            isPreparing = true
                            try {
                                chatInferenceOrchestrator.resetConversation(screen.topic, persona)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.e(e, "ChatPresenter: Error resetting conversation")
                            } finally {
                                isPreparing = false
                            }
                        }
                    }
                }

                ChatScreen.Event.Retry -> {
                    initTrigger++
                }

                ChatScreen.Event.RetrySave -> {
                    if (saveErrorMessage != null && !isGenerating && !isPreparing) {
                        scope.launch {
                            try {
                                currentSessionId =
                                    sessionRepository.saveSession(
                                        topic = screen.topic,
                                        messages = messages,
                                        sessionId = currentSessionId,
                                        modelUsed = activeModel?.name ?: "Unknown",
                                    )
                                saveErrorMessage = null
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.e(e, "ChatPresenter: Retry save failed")
                                saveErrorMessage = e.message ?: "Failed to save conversation"
                            }
                        }
                    }
                }

                is ChatScreen.Event.CopyMessage -> {}

                ChatScreen.Event.OpenModelPicker -> {
                    navigator.goTo(ModelPickerScreen)
                }

                is ChatScreen.Event.OpenCourse -> {
                    navigator.goTo(ChapterScreen(event.courseId))
                }

                ChatScreen.Event.Back -> {
                    navigator.pop()
                }
            }
        }

        LaunchedEffect(isPreparing, activeModel, screen.initialPrompt) {
            val prompt = screen.initialPrompt
            if (prompt != null &&
                !hasSentInitialPrompt &&
                !isPreparing &&
                activeModel?.downloadStatus == DownloadStatus.DOWNLOADED &&
                messages.isEmpty()
            ) {
                hasSentInitialPrompt = true
                eventSink(ChatScreen.Event.SendMessage(prompt))
            }
        }

        return when {
            errorMessage != null -> {
                ChatScreen.State.Error(errorMessage!!, screen.topic, eventSink)
            }

            activeModel == null -> {
                val hasDownloadedModels =
                    availableModels.any { it.downloadStatus == DownloadStatus.DOWNLOADED }
                ChatScreen.State.NoModelSelected(
                    hasDownloadedModels = hasDownloadedModels,
                    topic = screen.topic,
                    eventSink = eventSink,
                )
            }

            else -> {
                val model =
                    activeModel
                        ?: return@present ChatScreen.State.Error(
                            "No model available",
                            screen.topic,
                            eventSink,
                        )
                val sizeText = model.formattedSize
                val memoryText = "Requires ${model.minDeviceMemoryInGb}GB RAM"
                val configText = "Temp: ${modelConfig.temperature}, Top-K: ${modelConfig.topK}, Top-P: ${modelConfig.topP}"

                val contextStats =
                    if (model.contextWindow > 0) {
                        val systemPrompt = topicPromptProvider.buildSystemPrompt(screen.topic, persona)
                        val usedTokens = TokenEstimator.estimateConversationTokens(systemPrompt, messages)
                        ContextUsageStats(
                            usedTokens = usedTokens,
                            maxTokens = model.contextWindow,
                        )
                    } else {
                        null
                    }

                ChatScreen.State.Active(
                    messages = messages,
                    isGenerating = isGenerating,
                    isPreparing = isPreparing,
                    modelName = model.displayName,
                    persona = persona,
                    activeBackend = chatInferenceOrchestrator.getActiveBackend()?.name,
                    modelSize = sizeText,
                    modelMemory = memoryText,
                    configInfo = configText,
                    throughputInfo = throughputInfo,
                    systemStatsInfo = systemStatsInfo,
                    systemResourceStats = systemResourceStats,
                    contextStats = contextStats,
                    saveErrorMessage = saveErrorMessage,
                    topic = screen.topic,
                    saveToHistory = screen.saveToHistory,
                    availableCourse = availableCourse,
                    eventSink = eventSink,
                )
            }
        }
    }

    @CircuitInject(ChatScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: ChatScreen,
        ): ChatPresenter
    }
}
