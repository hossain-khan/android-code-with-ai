package dev.hossain.codematex.runtime

import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ModelConfig

/**
 * High-level on-device Large Language Model execution engine.
 *
 * Interfaces with the LiteRT-LM runtime to manage model initialization, hardware acceleration,
 * conversation session resets, history restoration, and token streaming.
 */
interface LlmEngine {
    /**
     * Initializes the LLM engine for the model at [modelPath] using [backend] acceleration.
     */
    suspend fun initialize(
        modelPath: String,
        backend: Backend = Backend.CPU,
        systemInstruction: String? = null,
        config: ModelConfig = ModelConfig(),
    )

    /**
     * Streams inference tokens for [input] on the active conversation session.
     */
    suspend fun runInference(
        input: String,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    )

    /**
     * Runs inference on a temporary conversation that is isolated from the active chat
     * conversation. The loaded engine is reused, so no second model is loaded. The temporary
     * conversation is closed before this method returns.
     */
    suspend fun runInferenceIsolated(
        input: String,
        systemInstruction: String? = null,
        config: ModelConfig = ModelConfig(),
        onToken: (partialResult: String, done: Boolean) -> Unit,
    )

    /**
     * Cancels any currently active token generation process.
     */
    fun stop()

    /**
     * Replaces the active conversation with a new conversation session using [systemInstruction] and [config].
     */
    suspend fun resetConversation(
        systemInstruction: String? = null,
        config: ModelConfig = ModelConfig(),
    )

    /**
     * Replays prior [messages] sequentially into the model's KV-cache context.
     */
    suspend fun restoreHistory(messages: List<ChatMessage>)

    /**
     * Returns the currently active execution [Backend] (CPU, GPU, or NPU), or `null` if uninitialized.
     */
    fun getActiveBackend(): Backend?

    /**
     * Returns `true` if a model engine session is currently initialized and residing in memory.
     */
    fun isInitialized(): Boolean = getActiveBackend() != null

    /**
     * Returns `true` if the specific model at [modelPath] is currently loaded in memory.
     * When `true`, subsequent initializations can reuse the compiled in-memory engine without
     * allocating additional device RAM.
     */
    fun isModelLoaded(modelPath: String): Boolean = isInitialized()

    /**
     * Closes active conversations and releases native C++ memory allocations and engine handles.
     *
     * Suspends because it first cancels any in-flight inference and then closes the native handles
     * while holding the engine's serialization lock, so it can never free native memory out from
     * under a running `sendMessageAsync` call (see issue #307).
     */
    suspend fun cleanup()

    /**
     * Hardware execution acceleration backends supported by the on-device runtime.
     */
    enum class Backend {
        CPU,
        GPU,
        NPU,
    }
}
