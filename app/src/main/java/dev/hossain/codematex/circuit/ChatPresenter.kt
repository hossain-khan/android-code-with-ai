package dev.hossain.codematex.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
        var throughputInfo by rememberRetained { mutableStateOf<String?>(null) }
        var systemStatsInfo by rememberRetained { mutableStateOf<String?>(null) }
        val context = LocalContext.current

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

        LaunchedEffect(isGenerating) {
            if (isGenerating) {
                var prevTicks =
                    dev.hossain.codematex.util.DeviceMemory
                        .getProcessCpuTicks()
                var prevTime = System.currentTimeMillis()
                val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

                while (isGenerating) {
                    kotlinx.coroutines.delay(1000)
                    val now = System.currentTimeMillis()
                    val elapsedSec = (now - prevTime) / 1000f
                    val currentTicks =
                        dev.hossain.codematex.util.DeviceMemory
                            .getProcessCpuTicks()

                    if (elapsedSec > 0.1f) {
                        val ticksDiff = currentTicks - prevTicks
                        val cpuUsage = ((ticksDiff / 100f) / elapsedSec) * 100f
                        val scaledCpu = (cpuUsage / cores).coerceIn(0f, 100f)

                        val mem =
                            dev.hossain.codematex.util.DeviceMemory
                                .getMemoryStats(context)
                        systemStatsInfo =
                            "CPU: ${"%.0f".format(scaledCpu)}% • RAM: ${"%.1f".format(mem.usedGb)} GB / ${"%.1f".format(mem.totalGb)} GB"

                        prevTicks = currentTicks
                        prevTime = now
                    }
                }
            } else {
                systemStatsInfo = null
            }
        }

        val scope = rememberCoroutineScope()

        val eventSink: (ChatScreen.Event) -> Unit = { event ->
            when (event) {
                is ChatScreen.Event.SendMessage -> {
                    if (!isGenerating) {
                        isGenerating = true
                        val input = event.text
                        Timber.d("ChatPresenter: Starting inference. Input: '${input.take(100)}' (length: ${input.length})")

                        messages = messages + ChatMessage.User(input)
                        messages = messages + ChatMessage.Agent(content = "", isStreaming = true)

                        var tokenCount = 0
                        var firstTokenTime = 0L
                        val startTime = System.currentTimeMillis()
                        throughputInfo = "Prefilling..."

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

                                        tokenCount++
                                        if (firstTokenTime == 0L) {
                                            firstTokenTime = System.currentTimeMillis()
                                            val prefillMs = firstTokenTime - startTime
                                            Timber.d("ChatPresenter: First token received! Prefill latency (TTFT): ${prefillMs}ms")
                                        }

                                        val now = System.currentTimeMillis()
                                        val totalPrefillMs = firstTokenTime - startTime
                                        val decodeMs = now - firstTokenTime

                                        if (decodeMs > 0) {
                                            val speed = (tokenCount * 1000f) / decodeMs
                                            throughputInfo =
                                                "TTFT: ${totalPrefillMs}ms • Speed: ${"%.1f".format(speed)} t/s ($tokenCount tokens)"
                                        } else {
                                            throughputInfo = "TTFT: ${totalPrefillMs}ms • Speed: -- t/s ($tokenCount tokens)"
                                        }

                                        if (done) {
                                            isGenerating = false
                                            val totalTimeMs = System.currentTimeMillis() - startTime
                                            val decodeDurationMs = System.currentTimeMillis() - firstTokenTime
                                            val speedText =
                                                if (decodeDurationMs >
                                                    0
                                                ) {
                                                    "%.2f".format((tokenCount * 1000f) / decodeDurationMs)
                                                } else {
                                                    "N/A"
                                                }
                                            Timber.d(
                                                "ChatPresenter: Inference completed successfully. Total tokens: $tokenCount, TTFT: ${firstTokenTime - startTime}ms, Decode speed: $speedText t/s, Total duration: ${totalTimeMs}ms",
                                            )
                                            Timber.d("ChatPresenter: Saving session message history...")
                                            sessionRepository.saveSession(screen.topic, messages)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                Timber.e(e, "ChatPresenter: Inference failed")
                                isGenerating = false
                                throughputInfo = "Error: ${e.message}"
                                messages = messages.dropLast(1) + ChatMessage.Error(e.message ?: "Inference failed")
                                initTrigger++
                            }
                        }
                    }
                }

                ChatScreen.Event.StopGeneration -> {
                    Timber.d("ChatPresenter: StopGeneration event received. Stopping LLM engine...")
                    llmEngine.stop()
                    isGenerating = false
                }

                ChatScreen.Event.ResetSession -> {
                    Timber.d("ChatPresenter: ResetSession event received. Clearing message history and resetting engine...")
                    messages = emptyList()
                    throughputInfo = null
                    systemStatsInfo = null
                    llmEngine.resetConversation(buildSystemPrompt(screen.topic), configStore.config)
                }

                ChatScreen.Event.Retry -> {
                    initTrigger++
                }

                is ChatScreen.Event.CopyMessage -> {}

                ChatScreen.Event.Back -> {
                    navigator.pop()
                }
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
                val sizeMb = activeModel.sizeBytes / 1_000_000
                val sizeText = "$sizeMb MB"
                val memoryText = "Requires ${activeModel.minDeviceMemoryInGb}GB RAM"
                val config = configStore.config
                val configText = "Temp: ${config.temperature}, Top-K: ${config.topK}, Top-P: ${config.topP}"

                ChatScreen.State.Active(
                    messages = messages,
                    isGenerating = isGenerating,
                    isPreparing = isPreparing,
                    modelName = activeModel.displayName,
                    activeBackend = llmEngine.getActiveBackend()?.name,
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
