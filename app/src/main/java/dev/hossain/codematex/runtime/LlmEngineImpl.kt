package dev.hossain.codematex.runtime

import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import dev.hossain.codematex.circuit.overlay.ModelConfig
import dev.hossain.codematex.data.model.ChatMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of [LlmEngine] that orchestrates on-device LLM inference using Google's LiteRT-LM.
 *
 * LiteRT (formerly TensorFlow Lite) is optimized for edge AI workloads.
 * For hardware acceleration details and performance optimization guidelines, see:
 * - Google AI Edge LiteRT: https://ai.google.dev/edge/litert
 * - TensorFlow Lite GPU Delegate: https://www.tensorflow.org/lite/performance/gpu#android
 * - Google AI Edge LiteRT-LM: https://github.com/google-ai-edge/LiteRT-LM
 *
 * @param llmEngineFactory Factory responsible for creating the native engine session with
 * hardware backend fallback. Consolidating fallback logic in the factory keeps this class
 * focused on inference orchestration and prevents duplication across initialization,
 * inference, and history restoration.
 */
class LlmEngineImpl(
    private val llmEngineFactory: LlmEngineFactory,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : LlmEngine {
    private var engine: InferenceEngine? = null
    private var conversation: InferenceConversation? = null
    private var currentModelPath: String = ""
    private var currentSystemInstruction: String? = null
    private var currentConfig: ModelConfig = ModelConfig()
    private var activeBackend: LlmEngine.Backend? = null
    private var activeCallback: MessageCallback? = null

    override fun getActiveBackend(): LlmEngine.Backend? = activeBackend

    /**
     * Initializes the LiteRT LLM engine.
     *
     * Hardware backend fallback is delegated to [llmEngineFactory], which tries the preferred
     * backend and falls back through NPU -> GPU -> CPU as needed.
     */
    override suspend fun initialize(
        modelPath: String,
        backend: LlmEngine.Backend,
        systemInstruction: String?,
        config: ModelConfig,
    ) {
        if (modelPath == "/dev/null") {
            Timber.w("LlmEngineImpl: Stub model detected - skipping LiteRT-LM initialization")
            return
        }

        // If the model is already loaded in memory, reuse the compiled engine and only reset the conversation.
        // This avoids reloading multi-gigabyte model weights from disk and recompiling delegates (~5ms vs ~5000ms).
        if (engine != null && currentModelPath == modelPath) {
            Timber.d("LlmEngineImpl: Reusing already loaded in-memory engine for modelPath=$modelPath")
            currentSystemInstruction = systemInstruction
            currentConfig = config
            resetConversation(systemInstruction, config)
            return
        }

        cleanup()
        currentModelPath = modelPath
        currentSystemInstruction = systemInstruction
        currentConfig = config

        val session =
            llmEngineFactory.createSession(
                modelPath = modelPath,
                preferredBackend = backend,
                systemInstruction = systemInstruction,
                config = config,
            )

        engine = session.engine
        conversation = session.conversation
        activeBackend = session.backend
    }

    override suspend fun runInference(
        input: String,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    ) {
        if (engine == null) {
            throw IllegalStateException("LLM engine is not initialized. Please wait for model initialization to complete.")
        }

        try {
            executeInference(input, onToken)
        } catch (e: kotlinx.coroutines.CancellationException) {
            Timber.d("LlmEngineImpl: Inference was cancelled by user")
            throw e
        } catch (e: java.util.concurrent.CancellationException) {
            Timber.d("LlmEngineImpl: Inference was cancelled by user (Java CancellationException)")
            throw e
        } catch (e: Exception) {
            val failedBackend = activeBackend
            if (failedBackend != null && failedBackend != LlmEngine.Backend.CPU) {
                Timber.w(
                    e,
                    "LlmEngineImpl: Inference failed on hardware acceleration ($failedBackend). Falling back to CPU...",
                )
                recreateSessionWithCpu()
                executeInference(input, onToken)
            } else {
                throw e
            }
        }
    }

    private suspend fun recreateSessionWithCpu() {
        val session =
            llmEngineFactory.createSession(
                modelPath = currentModelPath,
                preferredBackend = LlmEngine.Backend.CPU,
                systemInstruction = currentSystemInstruction,
                config = currentConfig,
            )
        engine = session.engine
        conversation = session.conversation
        activeBackend = session.backend
    }

    private suspend fun executeInference(
        input: String,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    ) {
        val conv =
            conversation
                ?: throw IllegalStateException("Engine not initialized. Call initialize() first.")

        withContext(dispatcher) {
            suspendCancellableCoroutine { cont ->
                val callback =
                    object : MessageCallback {
                        override fun onMessage(message: com.google.ai.edge.litertlm.Message) {
                            try {
                                val text =
                                    message.contents.contents.joinToString("") { content ->
                                        when (content) {
                                            is com.google.ai.edge.litertlm.Content.Text -> content.text
                                            else -> ""
                                        }
                                    }
                                onToken(text, false)
                            } catch (e: Exception) {
                                Timber.e(e, "LlmEngineImpl: Error processing incoming JNI token message")
                            }
                        }

                        override fun onDone() {
                            onToken("", true)
                            cont.resume(Unit)
                        }

                        override fun onError(throwable: Throwable) {
                            if (throwable is java.util.concurrent.CancellationException ||
                                throwable is kotlinx.coroutines.CancellationException ||
                                throwable.message?.contains("cancel", ignoreCase = true) == true
                            ) {
                                Timber.d("LlmEngineImpl: LiteRT-LM reported task cancellation")
                                onToken("", true)
                                cont.resume(Unit)
                            } else {
                                cont.resumeWithException(throwable)
                            }
                        }
                    }
                activeCallback = callback
                conv.sendMessageAsync(input, callback)
                cont.invokeOnCancellation { conv.cancelProcess() }
            }
        }
    }

    override fun stop() {
        conversation?.cancelProcess()
    }

    override fun resetConversation(
        systemInstruction: String?,
        config: ModelConfig,
    ) {
        currentSystemInstruction = systemInstruction
        currentConfig = config
        conversation?.close()

        val samplerConfig =
            SamplerConfig(
                topK = config.topK,
                topP = config.topP.toDouble(),
                temperature = config.temperature.toDouble(),
            )

        val conversationConfig =
            ConversationConfig(
                systemInstruction =
                    systemInstruction?.let {
                        Contents.of(
                            com.google.ai.edge.litertlm.Content
                                .Text(it),
                        )
                    },
                samplerConfig = samplerConfig,
            )

        conversation = engine?.createConversation(conversationConfig)
    }

    override suspend fun restoreHistory(messages: List<ChatMessage>) {
        if (engine == null) return

        val priorMessages = messages.filterIsInstance<ChatMessage.User>() + messages.filterIsInstance<ChatMessage.Agent>()
        if (priorMessages.isEmpty()) return

        try {
            executeRestoreHistory(priorMessages)
        } catch (e: Exception) {
            val failedBackend = activeBackend
            if (failedBackend != null && failedBackend != LlmEngine.Backend.CPU) {
                Timber.w(
                    e,
                    "LlmEngineImpl: History restoration failed on $failedBackend. Falling back to CPU...",
                )
                recreateSessionWithCpu()
                executeRestoreHistory(priorMessages)
            } else {
                Timber.w(e, "LlmEngineImpl: Failed to restore history")
            }
        }
    }

    private suspend fun executeRestoreHistory(priorMessages: List<ChatMessage>) {
        val conv = conversation ?: return

        Timber.d("LlmEngineImpl: Restoring ${priorMessages.size} prior messages to conversation context")

        val contextPrompt =
            buildString {
                append("Here is the prior conversation context. Do not respond to this message, just acknowledge it internally:\n\n")
                priorMessages.forEach { msg ->
                    val (role, text) =
                        when (msg) {
                            is ChatMessage.User -> "User" to msg.content
                            is ChatMessage.Agent -> "Assistant" to msg.content
                            else -> return@forEach
                        }
                    append("$role: $text\n\n")
                }
                append("--- End of prior conversation ---")
            }

        withContext(dispatcher) {
            suspendCancellableCoroutine { cont ->
                val callback =
                    object : MessageCallback {
                        override fun onMessage(message: com.google.ai.edge.litertlm.Message) {
                            // Ignore response - we just want to seed context
                        }

                        override fun onDone() {
                            Timber.d("LlmEngineImpl: Context restoration complete")
                            cont.resume(Unit)
                        }

                        override fun onError(throwable: Throwable) {
                            Timber.w(throwable, "LlmEngineImpl: Context restoration failed")
                            cont.resumeWithException(throwable)
                        }
                    }
                activeCallback = callback
                conv.sendMessageAsync(contextPrompt, callback)
                cont.invokeOnCancellation { conv.cancelProcess() }
            }
        }
    }

    override fun cleanup() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
        activeBackend = null
        activeCallback = null
    }
}
