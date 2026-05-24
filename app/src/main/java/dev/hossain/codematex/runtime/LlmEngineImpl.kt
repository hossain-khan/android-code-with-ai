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
import dev.hossain.codematex.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LlmEngineImpl(
    private val context: Context,
) : LlmEngine {
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentSystemInstruction: String? = null

    override suspend fun initialize(
        modelPath: String,
        backend: LlmEngine.Backend,
        systemInstruction: String?,
    ) {
        if (modelPath == "/dev/null") {
            Timber.w("LlmEngineImpl: Stub model detected - skipping LiteRT-LM initialization")
            return
        }

        cleanup()
        currentSystemInstruction = systemInstruction

        withContext(Dispatchers.Default) {
            Timber.d("LlmEngineImpl: Initializing engine with path=$modelPath, backend=$backend")
            val engineConfig =
                EngineConfig(
                    modelPath = modelPath,
                    backend = backend.toLiteRtBackend(),
                    maxNumTokens = 2048,
                )

            engine = Engine(engineConfig).also { it.initialize() }
            Timber.d("LlmEngineImpl: Engine initialized")

            val samplerConfig =
                SamplerConfig(
                    topK = 40,
                    topP = 1.0,
                    temperature = 0.7,
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

            conversation = engine!!.createConversation(conversationConfig)
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
                conv.sendMessageAsync(
                    input,
                    object : MessageCallback {
                        override fun onMessage(message: com.google.ai.edge.litertlm.Message) {
                            val text =
                                message.contents.contents.joinToString("") { content ->
                                    when (content) {
                                        is com.google.ai.edge.litertlm.Content.Text -> content.text
                                        else -> ""
                                    }
                                }
                            onToken(text, false)
                        }

                        override fun onDone() {
                            onToken("", true)
                            cont.resume(Unit)
                        }

                        override fun onError(throwable: Throwable) {
                            cont.resumeWithException(throwable)
                        }
                    },
                )
                cont.invokeOnCancellation { conv.cancelProcess() }
            }
        }
    }

    override fun stop() {
        conversation?.cancelProcess()
    }

    override fun resetConversation(systemInstruction: String?) {
        currentSystemInstruction = systemInstruction
        conversation?.close()

        val samplerConfig =
            SamplerConfig(
                topK = 40,
                topP = 1.0,
                temperature = 0.7,
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
                conv.sendMessageAsync(
                    contextPrompt,
                    object : MessageCallback {
                        override fun onMessage(message: com.google.ai.edge.litertlm.Message) {
                            // Ignore response - we just want to seed context
                        }

                        override fun onDone() {
                            Timber.d("LlmEngineImpl: Context restoration complete")
                        }

                        override fun onError(throwable: Throwable) {
                            Timber.w(throwable, "LlmEngineImpl: Context restoration failed")
                        }
                    },
                )
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
    }

    private fun LlmEngine.Backend.toLiteRtBackend(): Backend =
        when (this) {
            LlmEngine.Backend.CPU -> Backend.CPU()
            LlmEngine.Backend.GPU -> Backend.GPU()
            LlmEngine.Backend.NPU -> Backend.NPU(context.applicationInfo.nativeLibraryDir)
        }
}
