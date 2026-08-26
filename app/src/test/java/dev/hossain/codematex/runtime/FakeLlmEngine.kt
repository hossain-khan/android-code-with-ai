package dev.hossain.codematex.runtime

import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.runtime.LlmEngine

class FakeLlmEngine : LlmEngine {
    var responseTokens: List<String> = listOf("test response")
    var initializeCalls = 0
    var resetCalls = 0
    var stopCalls = 0
    var cleanupCalls = 0
    var restoreHistoryCalls = 0
    var isolatedInferenceCalls = 0
    var runInferenceCalls = 0
    var lastInput: String? = null
    var shouldThrow: Exception? = null
    var backendFailureBackend: LlmEngine.Backend? = null
    var backendFailureBackends: List<LlmEngine.Backend> = emptyList()

    override suspend fun initialize(
        modelPath: String,
        backend: LlmEngine.Backend,
        systemInstruction: String?,
        config: ModelConfig,
    ) {
        if (shouldThrow != null) throw shouldThrow!!
        initializeCalls++
    }

    override suspend fun runInference(
        input: String,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    ) {
        lastInput = input
        runInferenceCalls++
        if (shouldThrow != null) throw shouldThrow!!

        val failingBackend =
            backendFailureBackends.getOrNull(runInferenceCalls - 1)
                ?: backendFailureBackend?.takeIf { runInferenceCalls == 1 }
        if (failingBackend != null) {
            throw BackendFailureException(failingBackend, RuntimeException("Backend $failingBackend failed"))
        }

        responseTokens.forEachIndexed { index, token ->
            onToken(token, index == responseTokens.lastIndex)
        }
    }

    override suspend fun runInferenceIsolated(
        input: String,
        systemInstruction: String?,
        config: ModelConfig,
        onToken: (partialResult: String, done: Boolean) -> Unit,
    ) {
        isolatedInferenceCalls++
        runInference(input, onToken)
    }

    override fun stop() {
        stopCalls++
    }

    override suspend fun resetConversation(
        systemInstruction: String?,
        config: ModelConfig,
    ) {
        resetCalls++
    }

    override suspend fun restoreHistory(messages: List<ChatMessage>) {
        restoreHistoryCalls++
    }

    override fun getActiveBackend(): LlmEngine.Backend? = LlmEngine.Backend.CPU

    override fun cleanup() {
        cleanupCalls++
    }
}
