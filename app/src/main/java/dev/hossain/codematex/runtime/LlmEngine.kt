package dev.hossain.codematex.runtime

import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.ui.overlay.ModelConfig

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
