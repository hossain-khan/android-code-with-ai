package dev.hossain.codematex.data

import dev.hossain.codematex.data.ChatInferenceEvent
import dev.hossain.codematex.data.ChatInferenceOrchestrator
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.runtime.LlmEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * In-memory fake of [dev.hossain.codematex.data.ChatInferenceOrchestrator] for unit tests.
 */
class FakeChatInferenceOrchestrator : ChatInferenceOrchestrator {
    var initializeResult: Result<List<ChatMessage>> = Result.success(emptyList())
    var messageEvents: List<ChatInferenceEvent> = emptyList()
    var activeBackendValue: LlmEngine.Backend? = null

    var initializeCalls = mutableListOf<InitializeCall>()
    var stopCalls = 0
    var resetConversationTopics = mutableListOf<CodingTopic>()
    var resetConversationPersonas = mutableListOf<TutorPersona>()
    var sendMessageInputs = mutableListOf<String>()

    data class InitializeCall(
        val model: AiModel,
        val topic: CodingTopic,
        val sessionId: String?,
        val existingMessages: List<ChatMessage>,
        val persona: TutorPersona = TutorPersona.SENIOR_ENGINEER,
    )

    override suspend fun initialize(
        model: AiModel,
        topic: CodingTopic,
        sessionId: String?,
        existingMessages: List<ChatMessage>,
        persona: TutorPersona,
    ): Result<List<ChatMessage>> {
        initializeCalls += InitializeCall(model, topic, sessionId, existingMessages, persona)
        return initializeResult
    }

    override fun stop() {
        stopCalls++
    }

    override suspend fun resetConversation(
        topic: CodingTopic,
        persona: TutorPersona,
    ) {
        resetConversationTopics += topic
        resetConversationPersonas += persona
    }

    override fun getActiveBackend(): LlmEngine.Backend? = activeBackendValue

    override suspend fun sendMessage(input: String): Flow<ChatInferenceEvent> {
        sendMessageInputs += input
        return flow { messageEvents.forEach { emit(it) } }
    }
}
