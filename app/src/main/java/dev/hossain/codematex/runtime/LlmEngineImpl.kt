package dev.hossain.codematex.runtime

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import dev.hossain.codematex.circuit.overlay.ModelConfig
import dev.hossain.codematex.data.model.ChatMessage
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
 * - LiteRT Android Delegates: https://ai.google.dev/edge/litert/android/delegates
 * - Gemma On-Device GPU Inference: https://ai.google.dev/gemma/docs/gpu_inference
 */
class LlmEngineImpl(
    private val context: Context,
) : LlmEngine {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentSystemInstruction: String? = null
    private var currentConfig: ModelConfig = ModelConfig()
    private var activeBackend: LlmEngine.Backend? = null
    private var activeCallback: MessageCallback? = null

    override fun getActiveBackend(): LlmEngine.Backend? = activeBackend

    /**
     * Initializes the LiteRT LLM engine.
     *
     * To ensure peak performance, this method implements a hardware acceleration fallback strategy.
     * Since on-device LLM inference (e.g. Gemma 2B) is extremely compute-heavy on CPU, it defaults
     * to GPU or NPU if supported by the device. If the preferred hardware backend fails to initialize
     * (e.g. due to driver incompatibilities), it automatically falls back sequentially to lower backends
     * (NPU -> GPU -> CPU) to guarantee execution.
     *
     * See:
     * - LiteRT Hardware Delegates: https://ai.google.dev/edge/litert/android/delegates
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

        cleanup()
        currentSystemInstruction = systemInstruction
        currentConfig = config

        withContext(Dispatchers.Default) {
            var actualBackend = backend
            var success = false

            while (!success) {
                var newEngine: Engine? = null
                try {
                    Timber.d("LlmEngineImpl: Attempting to initialize engine with backend=$actualBackend")
                    Timber.d(
                        "LlmEngineImpl: Config parameters - MaxTokens: ${config.maxTokens}, " +
                            "Temp: ${config.temperature}, Top-K: ${config.topK}, Top-P: ${config.topP}, " +
                            "SystemPrompt length: ${systemInstruction?.length ?: 0}",
                    )
                    val engineConfig =
                        EngineConfig(
                            modelPath = modelPath,
                            backend = actualBackend.toLiteRtBackend(),
                            maxNumTokens = config.maxTokens,
                        )

                    newEngine = Engine(engineConfig).also { it.initialize() }

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

                    val newConversation = newEngine.createConversation(conversationConfig)

                    engine = newEngine
                    conversation = newConversation
                    activeBackend = actualBackend
                    success = true
                    Timber.d("LlmEngineImpl: Engine initialized successfully with backend=$actualBackend")
                } catch (e: Exception) {
                    newEngine?.close()
                    Timber.w(e, "LlmEngineImpl: Failed to initialize with backend=$actualBackend")
                    when (actualBackend) {
                        LlmEngine.Backend.NPU -> {
                            actualBackend = LlmEngine.Backend.GPU
                        }

                        LlmEngine.Backend.GPU -> {
                            actualBackend = LlmEngine.Backend.CPU
                        }

                        LlmEngine.Backend.CPU -> {
                            throw e
                        }
                    }
                }
            }
        }
    }

    override suspend fun runInference(
        input: String,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    ) {
        if (engine == null) {
            Timber.w("LlmEngineImpl: Stub model - returning mock response")
            onToken(
                "This is a stub response. In dev mode, the LLM engine is not initialized. Connect a real model file to see actual AI responses.",
                true,
            )
            return
        }

        val conv =
            conversation
                ?: throw IllegalStateException("Engine not initialized. Call initialize() first.")

        withContext(Dispatchers.Default) {
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
                            cont.resumeWithException(throwable)
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

        val conv = conversation ?: return

        val priorMessages = messages.filterIsInstance<ChatMessage.User>() + messages.filterIsInstance<ChatMessage.Agent>()
        if (priorMessages.isEmpty()) return

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

        withContext(Dispatchers.Default) {
            try {
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
            } catch (e: Exception) {
                Timber.w(e, "LlmEngineImpl: Failed to restore history")
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

    private fun LlmEngine.Backend.toLiteRtBackend(): Backend =
        when (this) {
            LlmEngine.Backend.CPU -> Backend.CPU()
            LlmEngine.Backend.GPU -> Backend.GPU()
            LlmEngine.Backend.NPU -> Backend.NPU(context.applicationInfo.nativeLibraryDir)
        }
}
