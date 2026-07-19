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
import dev.hossain.codematex.BuildConfig
import dev.hossain.codematex.circuit.overlay.ModelConfigStore
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.DevModels
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.runtime.LlmEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class ChatPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ChatScreen,
    private val llmEngine: LlmEngine,
    private val modelRepository: ModelRepository,
    private val sessionRepository: ChatSessionRepository,
    private val configStore: ModelConfigStore,
) : Presenter<ChatScreen.State> {
    @Composable
    override fun present(): ChatScreen.State {
        var messages by rememberRetained { mutableStateOf<List<ChatMessage>>(emptyList()) }
        var isGenerating by rememberRetained { mutableStateOf(false) }
        var isPreparing by rememberRetained { mutableStateOf(false) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        var initTrigger by rememberRetained { mutableStateOf(0) }

        var activeModel = modelRepository.getSelectedModel()

        if (BuildConfig.DEV_MODE && activeModel == null) {
            Timber.d("ChatPresenter: DEV_MODE - Auto-selecting stub model")
            activeModel = DevModels.STUB_MODEL
        }

        LaunchedEffect(activeModel, initTrigger) {
            if (activeModel == null) {
                Timber.w("ChatPresenter: No model selected")
                return@LaunchedEffect
            }
            Timber.d("ChatPresenter: Initializing model=${activeModel.name}, path=${activeModel.localPath}")
            isPreparing = true
            try {
                llmEngine.initialize(
                    modelPath = activeModel.localPath ?: "",
                    backend = activeModel.preferredBackend,
                    systemInstruction = buildSystemPrompt(screen.topic),
                    config = configStore.config,
                )
                Timber.d("ChatPresenter: Model initialized successfully")
                if (screen.sessionId != null && messages.isEmpty()) {
                    Timber.d("ChatPresenter: Restoring session=${screen.sessionId}")
                    messages = sessionRepository.getMessages(screen.sessionId)
                    llmEngine.restoreHistory(messages)
                }
            } catch (e: Exception) {
                Timber.e(e, "ChatPresenter: Model initialization failed")
                errorMessage = e.message
            }
            isPreparing = false
        }

        val scope = rememberCoroutineScope()

        val eventSink: (ChatScreen.Event) -> Unit = { event ->
            when (event) {
                is ChatScreen.Event.SendMessage -> {
                    if (!isGenerating) {
                        isGenerating = true
                        val input = event.text
                        Timber.d("ChatPresenter: Sending message: ${input.take(50)}...")

                        messages = messages + ChatMessage.User(input)
                        messages = messages + ChatMessage.Agent(content = "", isStreaming = true)

                        scope.launch {
                            try {
                                llmEngine.runInference(input) { partialToken, done ->
                                    scope.launch {
                                        val lastAgent = messages.last() as? ChatMessage.Agent
                                        if (lastAgent != null) {
                                            messages = messages.dropLast(1) +
                                                lastAgent.copy(
                                                    content = lastAgent.content + partialToken,
                                                    isStreaming = !done,
                                                )
                                        }
                                        if (done) {
                                            isGenerating = false
                                            Timber.d("ChatPresenter: Inference complete, saving session")
                                            sessionRepository.saveSession(screen.topic, messages)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                Timber.e(e, "ChatPresenter: Inference failed")
                                isGenerating = false
                                messages = messages.dropLast(1) + ChatMessage.Error(e.message ?: "Inference failed")
                                initTrigger++
                            }
                        }
                    }
                }

                ChatScreen.Event.StopGeneration -> {
                    llmEngine.stop()
                    isGenerating = false
                }

                ChatScreen.Event.ResetSession -> {
                    messages = emptyList()
                    llmEngine.resetConversation(buildSystemPrompt(screen.topic), configStore.config)
                }

                ChatScreen.Event.Retry -> {
                    initTrigger++
                }

                is ChatScreen.Event.CopyMessage -> {}
            }
        }

        return when {
            errorMessage != null -> {
                ChatScreen.State.Error(errorMessage!!, eventSink)
            }

            activeModel == null -> {
                ChatScreen.State.Loading
            }

            else -> {
                ChatScreen.State.Active(
                    messages = messages,
                    isGenerating = isGenerating,
                    isPreparing = isPreparing,
                    modelName = activeModel.name,
                    topic = screen.topic,
                    eventSink = eventSink,
                )
            }
        }
    }

    private fun buildSystemPrompt(topic: dev.hossain.codematex.data.model.CodingTopic): String =
        """You are a coding tutor specializing in ${topic.displayName}.
           |Explain concepts clearly with examples. Use markdown for code blocks.
           |Keep explanations concise but thorough.
        """.trimMargin()

    @CircuitInject(ChatScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: ChatScreen,
        ): ChatPresenter
    }
}
