package dev.hossain.codematex.circuit

import dev.hossain.codematex.circuit.overlay.ModelConfig
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.runtime.LlmEngine

class FakeLlmEngine : LlmEngine {
    var responseTokens: List<String> = listOf("test response")
    var initializeCalls = 0
    var resetCalls = 0
    var stopCalls = 0
    var cleanupCalls = 0
    var restoreHistoryCalls = 0
    var lastInput: String? = null
    var shouldThrow: Exception? = null

    override suspend fun initialize(
        modelPath: String,
        backend: LlmEngine.Backend,
        systemInstruction: String?,
        config: ModelConfig,
    ) {
        initializeCalls++
    }

    override suspend fun runInference(
        input: String,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    ) {
        lastInput = input
        if (shouldThrow != null) throw shouldThrow!!

        responseTokens.forEachIndexed { index, token ->
            onToken(token, index == responseTokens.lastIndex)
        }
    }

    override fun stop() {
        stopCalls++
    }

    override fun resetConversation(
        systemInstruction: String?,
        config: ModelConfig,
    ) {
        resetCalls++
    }

    override suspend fun restoreHistory(messages: List<ChatMessage>) {
        restoreHistoryCalls++
    }

    override fun cleanup() {
        cleanupCalls++
    }
}
