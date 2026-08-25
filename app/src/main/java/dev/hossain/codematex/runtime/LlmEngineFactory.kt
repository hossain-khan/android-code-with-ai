package dev.hossain.codematex.runtime

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LiteRtLmJniException
import com.google.ai.edge.litertlm.SamplerConfig
import dev.hossain.codematex.di.ApplicationContext
import dev.hossain.codematex.ui.overlay.ModelConfig
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
 * Factory for creating [InferenceEngine] instances.
 *
 * Abstracted so [DefaultLlmEngineFactory] can be unit-tested without loading JNI libraries.
 */
interface NativeEngineFactory {
    /**
     * Creates a new [InferenceEngine] for [config]. The returned engine is not yet initialized;
     * callers are responsible for invoking [InferenceEngine.initialize].
     */
    fun create(config: EngineConfig): InferenceEngine
}

/**
 * Production implementation that creates a native [Engine] and wraps it in an [InferenceEngine].
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultNativeEngineFactory
    @Inject
    constructor() : NativeEngineFactory {
        override fun create(config: EngineConfig): InferenceEngine = DefaultInferenceEngine(Engine(config))
    }

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
        private val nativeEngineFactory: NativeEngineFactory,
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
                    var inferenceEngine: InferenceEngine? = null
                    var inferenceConversation: InferenceConversation? = null
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

                        inferenceEngine = nativeEngineFactory.create(engineConfig).also { it.initialize() }

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

                        inferenceConversation = inferenceEngine.createConversation(conversationConfig)

                        Timber.d("LlmEngineFactory: Engine initialized successfully with backend=$actualBackend")
                        session =
                            LlmEngineSession(
                                inferenceEngine,
                                inferenceConversation,
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
                    } catch (e: Exception) {
                        Timber.e(e, "LlmEngineFactory: Non-backend error initializing engine with backend=$actualBackend")
                        throw e
                    } finally {
                        // If we did not successfully build a session, release any partially initialized
                        // native resources so the next fallback attempt (or the caller) starts clean.
                        if (session == null) {
                            try {
                                inferenceConversation?.close()
                            } catch (closeError: Exception) {
                                Timber.w(closeError, "LlmEngineFactory: Error closing failed conversation")
                            }
                            try {
                                inferenceEngine?.close()
                            } catch (closeError: Exception) {
                                Timber.w(closeError, "LlmEngineFactory: Error closing failed engine")
                            }
                        }
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
