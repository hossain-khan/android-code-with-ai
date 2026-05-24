package dev.hossain.codematex.runtime

import dev.hossain.codematex.data.model.ChatMessage

interface LlmEngine {
    suspend fun initialize(
        modelPath: String,
        backend: Backend = Backend.CPU,
        systemInstruction: String? = null,
    )

    suspend fun runInference(
        input: String,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    )

    fun stop()

    fun resetConversation(systemInstruction: String? = null)

    suspend fun restoreHistory(messages: List<ChatMessage>)

    fun cleanup()

    enum class Backend {
        CPU,
        GPU,
        NPU,
    }
}
