package dev.hossain.codematex.runtime

import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import dev.hossain.codematex.circuit.overlay.ModelConfig

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

    fun createFakeSession(
        engine: Engine,
        conversation: Conversation,
        backend: LlmEngine.Backend,
    ): LlmEngineSession = LlmEngineSession(engine, conversation, backend)
}
