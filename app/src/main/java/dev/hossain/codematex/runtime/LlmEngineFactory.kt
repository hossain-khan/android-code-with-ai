package dev.hossain.codematex.runtime

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LiteRtLmJniException
import com.google.ai.edge.litertlm.SamplerConfig
import dev.hossain.codematex.ui.overlay.ModelConfig
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Result of creating a LiteRT-LM engine session.
 */
data class LlmEngineSession(
    val engine: InferenceEngine,
    val conversation: InferenceConversation,
    val backend: LlmEngine.Backend,
)

/**
 * Factory responsible for creating a LiteRT-LM [Engine] and [Conversation]
 * with hardware backend fallback.
 *
 * Encapsulates the GPU/NPU/CPU fallback loop so that [LlmEngineImpl] does not
 * duplicate it across initialization, inference, and history restoration.
 */
interface LlmEngineFactory {
    /**
     * Creates an engine session for [modelPath] using [preferredBackend],
     * falling back through NPU -> GPU -> CPU as needed.
     *
     * Previously failed backends are tracked by [BackendFallbackStrategy] and
     * skipped on future attempts.
     *
     * @throws LiteRtLmJniException if the preferred or fallback backend cannot be initialized.
     * @throws Exception if a non-backend error occurs or no backend can initialize the model.
     */
    suspend fun createSession(
        modelPath: String,
        preferredBackend: LlmEngine.Backend,
        systemInstruction: String?,
        config: ModelConfig,
    ): LlmEngineSession
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultLlmEngineFactory
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val backendFallbackStrategy: BackendFallbackStrategy,
    ) : LlmEngineFactory {
        override suspend fun createSession(
            modelPath: String,
            preferredBackend: LlmEngine.Backend,
            systemInstruction: String?,
            config: ModelConfig,
        ): LlmEngineSession =
            withContext(Dispatchers.Default) {
                var actualBackend = backendFallbackStrategy.resolveStartBackend(preferredBackend)
                var session: LlmEngineSession? = null

                while (session == null) {
                    var engine: Engine? = null
                    try {
                        Timber.d("LlmEngineFactory: Attempting to initialize engine with backend=$actualBackend")
                        Timber.d(
                            "LlmEngineFactory: Config parameters - MaxTokens: ${config.maxTokens}, " +
                                "Temp: ${config.temperature}, Top-K: ${config.topK}, Top-P: ${config.topP}, " +
                                "SystemPrompt length: ${systemInstruction?.length ?: 0}",
                        )

                        val engineConfig =
                            EngineConfig(
                                modelPath = modelPath,
                                backend = actualBackend.toLiteRtBackend(),
                                maxNumTokens = config.maxTokens,
                            )

                        engine = Engine(engineConfig).also { it.initialize() }

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
                                        com.google.ai.edge.litertlm.Content
                                            .Text(it)
                                            .let { content -> Contents.of(content) }
                                    },
                                samplerConfig = samplerConfig,
                            )

                        val conversation = engine.createConversation(conversationConfig)

                        Timber.d("LlmEngineFactory: Engine initialized successfully with backend=$actualBackend")
                        session =
                            LlmEngineSession(
                                DefaultInferenceEngine(engine),
                                DefaultInferenceConversation(conversation),
                                actualBackend,
                            )
                    } catch (e: LiteRtLmJniException) {
                        Timber.w(e, "LlmEngineFactory: Backend $actualBackend not supported, attempting fallback")
                        backendFallbackStrategy.markUnsupported(actualBackend)

                        actualBackend =
                            backendFallbackStrategy.nextBackend(actualBackend)
                                ?: run {
                                    Timber.e("LlmEngineFactory: CPU backend failed. No further fallback available.")
                                    throw e
                                }

                        // Close the partially initialized engine before trying the next backend.
                        try {
                            engine?.close()
                        } catch (closeError: Exception) {
                            Timber.w(closeError, "LlmEngineFactory: Error closing failed engine")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "LlmEngineFactory: Non-backend error initializing engine with backend=$actualBackend")
                        throw e
                    }
                }

                session
            }

        private fun LlmEngine.Backend.toLiteRtBackend(): com.google.ai.edge.litertlm.Backend =
            when (this) {
                LlmEngine.Backend.CPU -> Backend.CPU()
                LlmEngine.Backend.GPU -> Backend.GPU()
                LlmEngine.Backend.NPU -> Backend.NPU(context.applicationInfo.nativeLibraryDir)
            }
    }
