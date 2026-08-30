package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.ChatSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class FakeChatSessionRepository(
    private val sessions: List<ChatSession> = emptyList(),
    private val messages: List<ChatMessage> = emptyList(),
    var getException: Exception? = null,
    var getSessionException: Exception? = null,
    var saveException: Exception? = null,
) : ChatSessionRepository {
    var savedSessions = mutableListOf<Triple<CodingTopic, List<ChatMessage>, String?>>()
    var deletedSessionIds = mutableListOf<String>()
    var clearAllSessionsCalled = false

    override fun getAllSessions(): Flow<List<ChatSession>> = getException?.let { ex -> flow { throw ex } } ?: flowOf(sessions)

    override suspend fun getSession(sessionId: String): ChatSession? {
        if (getSessionException != null) throw getSessionException!!
        return sessions.find { it.id == sessionId }
    }

    override suspend fun getMessages(sessionId: String): List<ChatMessage> = messages

    override suspend fun saveSession(
        topic: CodingTopic,
        messages: List<ChatMessage>,
        sessionId: String?,
        modelUsed: String?,
    ): String {
        if (saveException != null) throw saveException!!
        val effectiveSessionId = sessionId ?: "generated-session-id"
        savedSessions.add(Triple(topic, messages, sessionId))
        return effectiveSessionId
    }

    override suspend fun deleteSession(sessionId: String) {
        deletedSessionIds.add(sessionId)
    }

    override suspend fun clearAllSessions() {
        clearAllSessionsCalled = true
    }
}

fun testSession(
    id: String = "session-1",
    topic: CodingTopic = CodingTopic.KOTLIN,
    title: String = "Test session",
    summary: String = "A test session about Kotlin",
    messageCount: Int = 5,
): ChatSession =
    ChatSession(
        id = id,
        topic = topic,
        title = title,
        summary = summary,
        messageCount = messageCount,
        lastActiveAt = System.currentTimeMillis(),
        modelUsed = "gemma-2-2b-it",
    )
