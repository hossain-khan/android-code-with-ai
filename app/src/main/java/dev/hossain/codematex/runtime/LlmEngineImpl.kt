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
        cleanup()
        currentSystemInstruction = systemInstruction

        withContext(Dispatchers.Default) {
            val engineConfig =
                EngineConfig(
                    modelPath = modelPath,
                    backend = backend.toLiteRtBackend(),
                    maxNumTokens = 2048,
                )

            engine = Engine(engineConfig).also { it.initialize() }

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
        // LiteRT-LM Conversation doesn't support adding history directly.
        // Messages are loaded into UI state; the model conversation starts fresh.
        // TODO: Consider using Session API for history replay if needed.
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
