package dev.hossain.codematex.runtime

import dev.hossain.codematex.data.model.ModelConfig

class FakeLlmEngineFactory : LlmEngineFactory {
    data class CreateSessionRequest(
        val modelPath: String,
        val preferredBackend: LlmEngine.Backend,
        val systemInstruction: String?,
        val config: ModelConfig,
    )

    private val sessions = mutableListOf<LlmEngineSession>()
    private var sessionIndex = 0
    var createSessionRequests = mutableListOf<CreateSessionRequest>()
        private set
    var fallbackSessionRequests = mutableListOf<LlmEngine.Backend>()
        private set

    fun addSession(session: LlmEngineSession) {
        sessions.add(session)
    }

    override suspend fun createSession(
        modelPath: String,
        preferredBackend: LlmEngine.Backend,
        systemInstruction: String?,
        config: ModelConfig,
    ): LlmEngineSession {
        createSessionRequests.add(
            CreateSessionRequest(
                modelPath = modelPath,
                preferredBackend = preferredBackend,
                systemInstruction = systemInstruction,
                config = config,
            ),
        )
        if (sessionIndex >= sessions.size) {
            throw IllegalStateException("No fake session configured for index $sessionIndex")
        }
        return sessions[sessionIndex++]
    }

    override suspend fun createFallbackSession(
        modelPath: String,
        failedBackend: LlmEngine.Backend,
        systemInstruction: String?,
        config: ModelConfig,
    ): LlmEngineSession {
        fallbackSessionRequests.add(failedBackend)
        val fallbackBackend =
            when (failedBackend) {
                LlmEngine.Backend.NPU -> LlmEngine.Backend.GPU
                LlmEngine.Backend.GPU -> LlmEngine.Backend.CPU
                LlmEngine.Backend.CPU -> error("CPU backend has no fallback")
            }
        return createSession(
            modelPath = modelPath,
            preferredBackend = fallbackBackend,
            systemInstruction = systemInstruction,
            config = config,
        )
    }

    fun createFakeSession(
        engine: InferenceEngine,
        conversation: InferenceConversation,
        backend: LlmEngine.Backend,
    ): LlmEngineSession = LlmEngineSession(engine, conversation, backend)
}
