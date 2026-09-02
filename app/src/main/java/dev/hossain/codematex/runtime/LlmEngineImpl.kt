package dev.hossain.codematex.runtime

import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ModelConfig
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sentinel path used in dev mode to skip loading the real LiteRT-LM engine.
 */
internal const val DEV_STUB_MODEL_PATH = "/dev/null"

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

    @Volatile
    private var isConversationCancelled = false

    /**
     * Serializes all engine operations. LiteRT-LM does not support concurrent
     * [sendMessageAsync] calls on the same [com.google.ai.edge.litertlm.Conversation];
     * this mutex prevents interleaving initialization, inference, reset, and history
     * restoration from multiple coroutines.
     */
    private val engineMutex = Mutex()

    override fun getActiveBackend(): LlmEngine.Backend? = activeBackend

    override fun isInitialized(): Boolean = engine != null

    override fun isModelLoaded(modelPath: String): Boolean = engine != null && currentModelPath == modelPath

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
    ) = engineMutex.withLock {
        if (modelPath == DEV_STUB_MODEL_PATH) {
            Timber.w("LlmEngineImpl: Stub model detected - skipping LiteRT-LM initialization")
            return@withLock
        }

        // If the model is already loaded in memory, reuse the compiled engine and only reset the conversation.
        // This avoids reloading multi-gigabyte model weights from disk and recompiling delegates (~5ms vs ~5000ms).
        if (engine != null && currentModelPath == modelPath) {
            Timber.d("LlmEngineImpl [IN_MEMORY_REUSE]: Reusing already loaded in-memory engine for modelPath=$modelPath")
            currentSystemInstruction = systemInstruction
            currentConfig = config
            resetConversationLocked(systemInstruction, config)
            return@withLock
        }

        if (engine != null) {
            Timber.d(
                "LlmEngineImpl [MODEL_SWITCH]: Switching model from '%s' to '%s'. Cleaning up previous native engine from RAM...",
                currentModelPath,
                modelPath,
            )
        } else {
            Timber.d(
                "LlmEngineImpl [INIT]: Initializing model at '%s' with preferredBackend=%s",
                modelPath,
                backend,
            )
        }

        cleanupLocked()
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
        Timber.d(
            "LlmEngineImpl [INIT_COMPLETE]: Successfully initialized model '%s' with activeBackend=%s",
            modelPath,
            activeBackend,
        )
    }

    override suspend fun runInference(
        input: String,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    ) = engineMutex.withLock {
        if (engine == null) {
            throw IllegalStateException("LLM engine is not initialized. Please wait for model initialization to complete.")
        }

        if (isConversationCancelled || conversation == null) {
            Timber.d("LlmEngineImpl [RECREATE_CONVERSATION]: Recreating native conversation handle following cancellation")
            resetConversationLocked(currentSystemInstruction, currentConfig)
            isConversationCancelled = false
        }

        try {
            executeInference(input, onToken)
        } catch (e: kotlinx.coroutines.CancellationException) {
            isConversationCancelled = true
            Timber.d("LlmEngineImpl: Inference was cancelled by user")
            throw e
        } catch (e: java.util.concurrent.CancellationException) {
            isConversationCancelled = true
            Timber.d("LlmEngineImpl: Inference was cancelled by user (Java CancellationException)")
            throw e
        } catch (e: BackendFailureException) {
            if (e.failedBackend != LlmEngine.Backend.CPU) {
                Timber.w(
                    e,
                    "LlmEngineImpl: Inference failed on ${e.failedBackend}. Creating fallback session...",
                )
                recreateSessionAfterFailure(e.failedBackend)
            }
            // Re-throw so the orchestrator can signal the presenter to discard partial output
            // before retrying on the newly-created fallback session.
            throw e
        }
    }

    override suspend fun runInferenceIsolated(
        input: String,
        systemInstruction: String?,
        config: ModelConfig,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    ) = engineMutex.withLock {
        check(engine != null) {
            "LLM engine is not initialized. Please wait for model initialization to complete."
        }

        // Create a conversation that is separate from the active chat conversation so that
        // summary prompts and responses cannot pollute the user's live context. The loaded
        // engine is reused, so no second model is loaded. A failure here intentionally does
        // NOT fall back to CPU and does NOT touch the active chat session.
        val isolatedConversation =
            createConversationLocked(systemInstruction, config)
                ?: throw IllegalStateException("Failed to create isolated conversation")

        try {
            executeInferenceOnConversation(
                input = input,
                conv = isolatedConversation,
                onToken = onToken,
                backend = activeBackend ?: LlmEngine.Backend.CPU,
            )
        } finally {
            closeQuietly(isolatedConversation)
        }
    }

    private suspend fun recreateSessionAfterFailure(failedBackend: LlmEngine.Backend) {
        val engineToClose = engine
        val conversationToClose = conversation
        try {
            val session =
                llmEngineFactory.createFallbackSession(
                    modelPath = currentModelPath,
                    failedBackend = failedBackend,
                    systemInstruction = currentSystemInstruction,
                    config = currentConfig,
                )
            engine = session.engine
            conversation = session.conversation
            activeBackend = session.backend
        } catch (e: Throwable) {
            // The failed hardware session is no longer usable. Clear it out before rethrowing
            // so we do not leave multi-gigabyte native allocations pinned.
            engine = null
            conversation = null
            activeBackend = null
            closeQuietly(conversationToClose)
            closeQuietly(engineToClose)
            throw e
        }
        closeQuietly(conversationToClose)
        closeQuietly(engineToClose)
    }

    private suspend fun executeInference(
        input: String,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    ) {
        val conv =
            conversation
                ?: throw IllegalStateException("Engine not initialized. Call initialize() first.")
        executeInferenceOnConversation(input, conv, onToken)
    }

    private suspend fun executeInferenceOnConversation(
        input: String,
        conv: InferenceConversation,
        onToken: (partialResult: String, done: Boolean) -> Unit,
        backend: LlmEngine.Backend = activeBackend ?: LlmEngine.Backend.CPU,
    ) {
        withContext(dispatcher) {
            suspendCancellableCoroutine<Unit> { cont ->
                val callback =
                    SafeCallback(
                        cont = cont,
                        backend = backend,
                        onContent = { text -> onToken(text, false) },
                        onTerminal = { onToken("", true) },
                    )
                activeCallback = callback
                conv.sendMessageAsync(input, callback)
                cont.invokeOnCancellation {
                    callback.cancel()
                    cancelNativeProcess(conv)
                }
            }
        }
    }

    override fun stop() {
        isConversationCancelled = true
        try {
            conversation?.cancelProcess()
        } catch (throwable: Throwable) {
            logCallbackFailure(throwable, "LlmEngineImpl: Error cancelling process on conversation")
        }
    }

    override suspend fun resetConversation(
        systemInstruction: String?,
        config: ModelConfig,
    ) = engineMutex.withLock {
        resetConversationLocked(systemInstruction, config)
    }

    /**
     * Internal version of [resetConversation] that must be called while already
     * holding [engineMutex]. Used by [initialize] when reusing an in-memory engine.
     */
    private fun resetConversationLocked(
        systemInstruction: String?,
        config: ModelConfig,
    ) {
        currentSystemInstruction = systemInstruction
        currentConfig = config
        closeQuietly(conversation)
        conversation = createConversationLocked(systemInstruction, config)
    }

    /**
     * Creates a new [InferenceConversation] from the currently loaded engine. Must be called
     * while holding [engineMutex]. Returns `null` if no engine is loaded.
     */
    private fun createConversationLocked(
        systemInstruction: String?,
        config: ModelConfig,
    ): InferenceConversation? {
        val loadedEngine = engine ?: return null

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

        return loadedEngine.createConversation(conversationConfig)
    }

    override suspend fun restoreHistory(messages: List<ChatMessage>) =
        engineMutex.withLock {
            if (engine == null) return@withLock

            if (isConversationCancelled || conversation == null) {
                Timber.d(
                    "LlmEngineImpl [RECREATE_CONVERSATION]: Recreating native conversation handle following cancellation before history restore",
                )
                resetConversationLocked(currentSystemInstruction, currentConfig)
                isConversationCancelled = false
            }

            val priorMessages = messages.filter { it is ChatMessage.User || it is ChatMessage.Agent }
            if (priorMessages.isEmpty()) return@withLock

            while (true) {
                try {
                    executeRestoreHistory(priorMessages)
                    return@withLock
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: java.util.concurrent.CancellationException) {
                    throw e
                } catch (e: BackendFailureException) {
                    if (e.failedBackend == LlmEngine.Backend.CPU) {
                        throw e
                    }
                    Timber.w(
                        e,
                        "LlmEngineImpl: History restoration failed on ${e.failedBackend}. Creating fallback session...",
                    )
                    recreateSessionAfterFailure(e.failedBackend)
                }
            }
        }

    private suspend fun executeRestoreHistory(priorMessages: List<ChatMessage>) {
        val conv = conversation ?: return
        executeRestoreHistoryOnConversation(priorMessages, conv)
    }

    private suspend fun executeRestoreHistoryOnConversation(
        priorMessages: List<ChatMessage>,
        conv: InferenceConversation,
    ) {
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
            suspendCancellableCoroutine<Unit> { cont ->
                val callback =
                    SafeCallback(
                        cont = cont,
                        backend = activeBackend ?: LlmEngine.Backend.CPU,
                        onContent = { },
                        onTerminal = { Timber.d("LlmEngineImpl: Context restoration complete") },
                        onError = { throwable -> Timber.w(throwable, "LlmEngineImpl: Context restoration failed") },
                    )
                activeCallback = callback
                conv.sendMessageAsync(contextPrompt, callback)
                cont.invokeOnCancellation {
                    callback.cancel()
                    cancelNativeProcess(conv)
                }
            }
        }
    }

    private fun cancelNativeProcess(conversation: InferenceConversation) {
        isConversationCancelled = true
        try {
            conversation.cancelProcess()
        } catch (throwable: Throwable) {
            logCallbackFailure(throwable, "LlmEngineImpl: Error cancelling native inference")
        }
    }

    /**
     * Cancels any in-flight inference, then closes native handles while holding [engineMutex].
     *
     * Closing the native conversation/engine without the lock could free them while [runInference]
     * is still executing `sendMessageAsync` on another thread (this is reachable from
     * `onTrimMemory` on the main thread), causing a native use-after-free. Cancelling first lets the
     * in-flight inference unwind and release the mutex so the close happens safely. See issue #307.
     */
    override suspend fun cleanup() {
        stop()
        engineMutex.withLock {
            cleanupLocked()
        }
    }

    /**
     * Closes and clears native handles. Must be called while holding [engineMutex] (either via
     * [cleanup] or from another already-locked engine operation such as [initialize]).
     */
    private fun cleanupLocked() {
        val hadEngine = engine != null
        val hadConversation = conversation != null
        val previousPath = currentModelPath
        val previousBackend = activeBackend

        if (hadEngine || hadConversation) {
            Timber.d(
                "LlmEngineImpl [CLEANUP]: Freeing native engine & conversation resources from RAM (previousModel='%s', backend=%s)",
                previousPath,
                previousBackend,
            )
        }
        closeQuietly(conversation)
        closeQuietly(engine)
        conversation = null
        engine = null
        currentModelPath = ""
        activeBackend = null
        activeCallback = null
        isConversationCancelled = false
    }

    /**
     * Closes a native [InferenceConversation], swallowing any exception so that cleanup does not
     * mask the original failure that triggered it.
     */
    private fun closeQuietly(conversation: InferenceConversation?) {
        if (conversation == null) return
        try {
            Timber.d("LlmEngineImpl [NATIVE_FREE]: Closing native InferenceConversation and freeing context/KV-cache")
            conversation.close()
        } catch (e: Exception) {
            Timber.w(e, "LlmEngineImpl: Error closing conversation")
        }
    }

    /**
     * Closes a native [InferenceEngine], swallowing any exception so that cleanup does not
     * mask the original failure that triggered it.
     */
    private fun closeQuietly(engine: InferenceEngine?) {
        if (engine == null) return
        try {
            Timber.d("LlmEngineImpl [NATIVE_FREE]: Closing native InferenceEngine and unmapping model weights")
            engine.close()
        } catch (e: Exception) {
            Timber.w(e, "LlmEngineImpl: Error closing engine")
        }
    }

    /**
     * Combines an idempotent continuation with a terminal guard so that the entire callback
     * (including token dispatch) is ignored after the first terminal signal.
     */
    private inner class SafeCallback(
        private val cont: CancellableContinuation<Unit>,
        private val backend: LlmEngine.Backend,
        private val onContent: (String) -> Unit,
        private val onTerminal: () -> Unit,
        private val onError: ((Throwable) -> Unit)? = null,
    ) : MessageCallback {
        private val resumed = AtomicBoolean(false)
        private val terminal = AtomicBoolean(false)

        override fun onMessage(message: com.google.ai.edge.litertlm.Message) {
            if (terminal.get()) return
            try {
                val text =
                    message.contents.contents.joinToString("") { content ->
                        when (content) {
                            is com.google.ai.edge.litertlm.Content.Text -> content.text
                            else -> ""
                        }
                    }
                onContent(text)
            } catch (throwable: Throwable) {
                logCallbackFailure(throwable, "LlmEngineImpl: Error processing incoming JNI token message")
            }
        }

        override fun onDone() {
            if (!terminal.compareAndSet(false, true)) return
            Timber.d("LlmEngineImpl: Native inference completed successfully (onDone)")
            notifyTerminal()
            completeSuccessfully()
        }

        override fun onError(throwable: Throwable) {
            if (!terminal.compareAndSet(false, true)) return
            runNoThrow("LlmEngineImpl: Error notifying callback observer") {
                onError?.invoke(throwable)
            }

            if (isCancellation(throwable)) {
                isConversationCancelled = true
                runNoThrow("LlmEngineImpl: Error logging native cancellation") {
                    Timber.d("LlmEngineImpl: LiteRT-LM reported task cancellation")
                }
                notifyTerminal()
                completeSuccessfully()
            } else {
                val exceptionToResume =
                    if (throwable is com.google.ai.edge.litertlm.LiteRtLmJniException) {
                        BackendFailureException(backend, throwable)
                    } else {
                        throwable
                    }
                completeWithException(exceptionToResume)
            }
        }

        fun cancel() {
            terminal.compareAndSet(false, true)
            resumed.compareAndSet(false, true)
        }

        private fun notifyTerminal() {
            runNoThrow("LlmEngineImpl: Error notifying terminal callback") {
                onTerminal()
            }
        }

        private fun completeSuccessfully() {
            if (!resumed.compareAndSet(false, true)) return
            runNoThrow("LlmEngineImpl: Error completing inference continuation") {
                cont.resume(Unit)
            }
        }

        private fun completeWithException(throwable: Throwable) {
            if (!resumed.compareAndSet(false, true)) return
            runNoThrow("LlmEngineImpl: Error completing inference continuation exceptionally") {
                cont.resumeWithException(throwable)
            }
        }

        private inline fun runNoThrow(
            logMessage: String,
            block: () -> Unit,
        ) {
            try {
                block()
            } catch (throwable: Throwable) {
                logCallbackFailure(throwable, logMessage)
            }
        }
    }
}

private fun isCancellation(throwable: Throwable): Boolean =
    throwable is java.util.concurrent.CancellationException ||
        throwable is kotlinx.coroutines.CancellationException ||
        throwable.message?.contains("cancel", ignoreCase = true) == true

private fun logCallbackFailure(
    throwable: Throwable,
    message: String,
) {
    try {
        Timber.e(throwable, message)
    } catch (_: Throwable) {
        // JNI callbacks must never throw into native code, including when logging fails.
    }
}
