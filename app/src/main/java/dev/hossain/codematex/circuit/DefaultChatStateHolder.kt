package dev.hossain.codematex.circuit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.hossain.codematex.circuit.overlay.ModelConfigStore
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.hossain.codematex.data.repository.ModelRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Default implementation of [ChatStateHolder].
 */
@AssistedInject
class DefaultChatStateHolder(
    @Assisted private val screen: ChatScreen,
    private val modelRepository: ModelRepository,
    private val sessionRepository: ChatSessionRepository,
    private val configStore: ModelConfigStore,
    private val chatInferenceOrchestrator: ChatInferenceOrchestrator,
    private val systemStatsMonitor: SystemStatsMonitor,
) : ChatStateHolder {
    override var messages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set
    override var currentSessionId by mutableStateOf(screen.sessionId)
        private set
    override var isGenerating by mutableStateOf(false)
        private set
    override var isPreparing by mutableStateOf(false)
        private set
    override var persona by mutableStateOf(TutorPersona.SENIOR_ENGINEER)
        private set
    override var errorMessage by mutableStateOf<String?>(null)
        private set
    override var initTrigger by mutableStateOf(0)
        private set
    override var throughputInfo by mutableStateOf<String?>(null)
        private set
    override var systemStatsInfo by mutableStateOf<String?>(null)
        private set
    override var availableModels by mutableStateOf<List<AiModel>>(emptyList())
        private set
    override var activeModel by mutableStateOf<AiModel?>(null)
        private set

    override val activeBackend: String?
        get() = chatInferenceOrchestrator.getActiveBackend()?.name

    private var scope: CoroutineScope? = null

    override fun attachScope(scope: CoroutineScope) {
        this.scope = scope
    }

    override fun loadAvailableModels() {
        scope?.launch {
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
        } ?: Timber.w("DefaultChatStateHolder: loadAvailableModels called before scope attached")
    }

    override fun loadSessionMessages() {
        val sessionId = screen.sessionId ?: return
        if (messages.isNotEmpty()) return

        scope?.launch {
            Timber.d("DefaultChatStateHolder: Instantly loading messages for session=$sessionId")
            messages = sessionRepository.getMessages(sessionId)
        } ?: Timber.w("DefaultChatStateHolder: loadSessionMessages called before scope attached")
    }

    override suspend fun initializeModel() {
        val model = activeModel
        if (model == null) {
            Timber.w("DefaultChatStateHolder: No model selected")
            return
        }

        Timber.d("DefaultChatStateHolder: Initializing model=${model.name}, path=${model.localPath}, persona=${persona.name}")
        isPreparing = true
        errorMessage = null
        try {
            val result =
                chatInferenceOrchestrator.initialize(
                    model = model,
                    topic = screen.topic,
                    sessionId = currentSessionId,
                    existingMessages = messages,
                    persona = persona,
                )
            result
                .onSuccess { loadedMessages ->
                    if (loadedMessages.isNotEmpty()) {
                        messages = loadedMessages
                    }
                    Timber.d("DefaultChatStateHolder: Model initialized successfully")
                }.onFailure { error ->
                    Timber.e(error, "DefaultChatStateHolder: Model initialization failed")
                    errorMessage = error.message
                }
        } catch (e: CancellationException) {
            Timber.d("DefaultChatStateHolder: Model initialization cancelled")
            throw e
        } finally {
            isPreparing = false
        }
    }

    override suspend fun monitorSystemStats() {
        if (isGenerating) {
            systemStatsMonitor.monitorWhileActive(
                isActive = { isGenerating },
                onStats = { systemStatsInfo = it },
            )
        } else {
            systemStatsInfo = null
        }
    }

    override fun sendMessage(text: String) {
        if (isGenerating || isPreparing) return

        isGenerating = true
        Timber.d("DefaultChatStateHolder: Starting inference. Input: '${text.take(100)}' (length: ${text.length})")

        messages = messages + ChatMessage.User(text)
        messages = messages + ChatMessage.Agent(content = "", isStreaming = true)
        throughputInfo = "Prefilling..."

        scope?.launch {
            // Capture the active model at inference start so the saved session
            // is tagged with the model that actually generated the response.
            val modelName = activeModel?.name
            try {
                val throughputTracker = ThroughputTracker()
                chatInferenceOrchestrator.sendMessage(text).collect { inferenceEvent ->
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
                            Timber.d("DefaultChatStateHolder: Saving session message history...")
                            currentSessionId =
                                sessionRepository.saveSession(
                                    topic = screen.topic,
                                    messages = messages,
                                    sessionId = currentSessionId,
                                    modelUsed = modelName,
                                )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "DefaultChatStateHolder: Inference failed")
                isGenerating = false
                throughputInfo = "Error: ${e.message}"
                messages = messages.dropLast(1) + ChatMessage.Error(e.message ?: "Inference failed")
                initTrigger++
            }
        } ?: Timber.w("DefaultChatStateHolder: sendMessage called before scope attached")
    }

    override fun stopGeneration() {
        Timber.d("DefaultChatStateHolder: StopGeneration event received. Stopping LLM engine...")
        chatInferenceOrchestrator.stop()
        isGenerating = false
        val lastAgent = messages.lastOrNull() as? ChatMessage.Agent
        if (lastAgent != null && lastAgent.isStreaming) {
            messages = messages.dropLast(1) + lastAgent.copy(isStreaming = false)
        }
    }

    override fun resetSession() {
        Timber.d("DefaultChatStateHolder: ResetSession event received. Clearing message history and resetting engine...")
        messages = emptyList()
        throughputInfo = null
        systemStatsInfo = null
        scope?.launch {
            chatInferenceOrchestrator.resetConversation(screen.topic, persona)
        } ?: Timber.w("DefaultChatStateHolder: resetSession called before scope attached")
    }

    override fun selectPersona(persona: TutorPersona) {
        if (this.persona == persona) return

        Timber.d("DefaultChatStateHolder: Switching persona to ${persona.name}")
        this.persona = persona
        messages =
            messages +
            ChatMessage.System("Switched tutor persona to ${persona.iconGlyph} ${persona.displayName}")
        scope?.launch {
            chatInferenceOrchestrator.resetConversation(screen.topic, persona)
        } ?: Timber.w("DefaultChatStateHolder: selectPersona called before scope attached")
    }

    override fun retry() {
        initTrigger++
    }

    override fun copyMessage(content: String) {
        // No-op: clipboard integration can be wired here in the future.
    }

    @AssistedFactory
    interface Factory {
        fun create(screen: ChatScreen): DefaultChatStateHolder
    }
}
