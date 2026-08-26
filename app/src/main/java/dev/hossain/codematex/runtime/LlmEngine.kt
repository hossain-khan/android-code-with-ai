package dev.hossain.codematex.runtime

import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ModelConfig

interface LlmEngine {
    suspend fun initialize(
        modelPath: String,
        backend: Backend = Backend.CPU,
        systemInstruction: String? = null,
        config: ModelConfig = ModelConfig(),
    )

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

    fun stop()

    suspend fun resetConversation(
        systemInstruction: String? = null,
        config: ModelConfig = ModelConfig(),
    )

    suspend fun restoreHistory(messages: List<ChatMessage>)

    fun getActiveBackend(): Backend?

    fun cleanup()

    enum class Backend {
        CPU,
        GPU,
        NPU,
    }
}
