package dev.hossain.codematex.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.circuit.overlay.ModelConfigStore
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.hossain.codematex.data.repository.ModelRepository
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
    private val chatInferenceOrchestrator: ChatInferenceOrchestrator,
    private val systemStatsMonitor: SystemStatsMonitor,
) : Presenter<ChatScreen.State> {
    @Composable
    override fun present(): ChatScreen.State {
        var messages by rememberRetained { mutableStateOf<List<ChatMessage>>(emptyList()) }
        var isGenerating by rememberRetained { mutableStateOf(false) }
        var isPreparing by rememberRetained { mutableStateOf(false) }
        var persona by rememberRetained { mutableStateOf(TutorPersona.SENIOR_ENGINEER) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        var initTrigger by rememberRetained { mutableStateOf(0) }
        var throughputInfo by rememberRetained { mutableStateOf<String?>(null) }
        var systemStatsInfo by rememberRetained { mutableStateOf<String?>(null) }
        var availableModels by rememberRetained { mutableStateOf<List<AiModel>>(emptyList()) }
        var activeModel by rememberRetained { mutableStateOf<AiModel?>(null) }

        LaunchedEffect(Unit) {
            val initial = modelRepository.getSelectedModel()
            if (initial != null) {
                activeModel = initial
            }
            modelRepository.getAvailableModels().collect { models ->
                availableModels = models
                val selected = modelRepository.getSelectedModel()
                if (selected?.id != activeModel?.id || selected?.localPath != activeModel?.localPath) {
                    activeModel = selected
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

        LaunchedEffect(activeModel?.id, activeModel?.localPath, initTrigger, persona) {
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                Timber.d("ChatPresenter: Model initialization cancelled")
                throw e
            } finally {
                isPreparing = false
            }
        }

        LaunchedEffect(isGenerating) {
            if (isGenerating) {
                systemStatsMonitor.monitorWhileActive(
                    isActive = { isGenerating },
                    onStats = { systemStatsInfo = it },
                )
            } else {
                systemStatsInfo = null
            }
        }

        val scope = rememberCoroutineScope()

        val eventSink: (ChatScreen.Event) -> Unit = { event ->
            when (event) {
                is ChatScreen.Event.SendMessage -> {
                    if (!isGenerating && !isPreparing) {
                        isGenerating = true
                        val input = event.text
                        Timber.d("ChatPresenter: Starting inference. Input: '${input.take(100)}' (length: ${input.length})")

                        messages = messages + ChatMessage.User(input)
                        messages = messages + ChatMessage.Agent(content = "", isStreaming = true)
                        throughputInfo = "Prefilling..."

                        scope.launch {
                            try {
                                val throughputTracker = ThroughputTracker()
                                chatInferenceOrchestrator.sendMessage(input).collect { inferenceEvent ->
                                    when (inferenceEvent) {
                                        is ChatInferenceEvent.Token -> {
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
                                            throughputTracker.recordToken("")
                                            throughputInfo = throughputTracker.finalize()
                                            isGenerating = false
                                            Timber.d("ChatPresenter: Saving session message history...")
                                            sessionRepository.saveSession(screen.topic, messages)
                                        }
                                    }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.e(e, "ChatPresenter: Inference failed")
                                isGenerating = false
                                throughputInfo = "Error: ${e.message}"
                                messages = messages.dropLast(1) + ChatMessage.Error(e.message ?: "Inference failed")
                                initTrigger++
                            }
                        }
                    }
                }

                is ChatScreen.Event.SelectPersona -> {
                    if (persona != event.persona) {
                        Timber.d("ChatPresenter: Switching persona to ${event.persona.name}")
                        persona = event.persona
                        messages =
                            messages +
                            ChatMessage.System("Switched tutor persona to ${event.persona.iconGlyph} ${event.persona.displayName}")
                        chatInferenceOrchestrator.resetConversation(screen.topic, event.persona)
                    }
                }

                ChatScreen.Event.StopGeneration -> {
                    Timber.d("ChatPresenter: StopGeneration event received. Stopping LLM engine...")
                    chatInferenceOrchestrator.stop()
                    isGenerating = false
                    val lastAgent = messages.lastOrNull() as? ChatMessage.Agent
                    if (lastAgent != null && lastAgent.isStreaming) {
                        messages = messages.dropLast(1) + lastAgent.copy(isStreaming = false)
                    }
                }

                ChatScreen.Event.ResetSession -> {
                    Timber.d("ChatPresenter: ResetSession event received. Clearing message history and resetting engine...")
                    messages = emptyList()
                    throughputInfo = null
                    systemStatsInfo = null
                    chatInferenceOrchestrator.resetConversation(screen.topic, persona)
                }

                ChatScreen.Event.Retry -> {
                    initTrigger++
                }

                is ChatScreen.Event.CopyMessage -> {}

                ChatScreen.Event.OpenModelPicker -> {
                    navigator.goTo(ModelPickerScreen)
                }

                ChatScreen.Event.Back -> {
                    navigator.pop()
                }
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
                val sizeMb = model.sizeBytes / 1_000_000
                val sizeText = "$sizeMb MB"
                val memoryText = "Requires ${model.minDeviceMemoryInGb}GB RAM"
                val config = configStore.config
                val configText = "Temp: ${config.temperature}, Top-K: ${config.topK}, Top-P: ${config.topP}"

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
                    topic = screen.topic,
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
