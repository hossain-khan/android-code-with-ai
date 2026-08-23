package dev.hossain.codematex.circuit

import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.runtime.LlmEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * In-memory fake of [ChatInferenceOrchestrator] for unit tests.
 */
class FakeChatInferenceOrchestrator : ChatInferenceOrchestrator {
    var initializeResult: Result<List<ChatMessage>> = Result.success(emptyList())
    var messageEvents: List<ChatInferenceEvent> = emptyList()
    var activeBackendValue: LlmEngine.Backend? = null

    var initializeCalls = mutableListOf<InitializeCall>()
    var stopCalls = 0
    var resetConversationTopics = mutableListOf<CodingTopic>()
    var sendMessageInputs = mutableListOf<String>()

    data class InitializeCall(
        val model: AiModel,
        val topic: CodingTopic,
        val sessionId: String?,
        val existingMessages: List<ChatMessage>,
    )

    override suspend fun initialize(
        model: AiModel,
        topic: CodingTopic,
        sessionId: String?,
        existingMessages: List<ChatMessage>,
    ): Result<List<ChatMessage>> {
        initializeCalls += InitializeCall(model, topic, sessionId, existingMessages)
        return initializeResult
    }

    override fun stop() {
        stopCalls++
    }

    override fun resetConversation(topic: CodingTopic) {
        resetConversationTopics += topic
    }

    override fun getActiveBackend(): LlmEngine.Backend? = activeBackendValue

    override suspend fun sendMessage(input: String): Flow<ChatInferenceEvent> {
        sendMessageInputs += input
        return flow { messageEvents.forEach { emit(it) } }
    }
}
