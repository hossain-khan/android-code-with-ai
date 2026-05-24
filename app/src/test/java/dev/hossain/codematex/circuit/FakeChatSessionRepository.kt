package dev.hossain.codematex.circuit

import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.ChatSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeChatSessionRepository(
    private val sessions: List<ChatSession> = emptyList(),
    private val messages: List<ChatMessage> = emptyList(),
    private var getException: Exception? = null,
) : ChatSessionRepository {
    var savedSessions = mutableListOf<Pair<CodingTopic, List<ChatMessage>>>()
    var deletedSessionIds = mutableListOf<String>()

    override fun getAllSessions(): Flow<List<ChatSession>> {
        if (getException != null) throw getException!!
        return flowOf(sessions)
    }

    override suspend fun getSession(sessionId: String): ChatSession? = sessions.find { it.id == sessionId }

    override suspend fun getMessages(sessionId: String): List<ChatMessage> = messages

    override suspend fun saveSession(
        topic: CodingTopic,
        messages: List<ChatMessage>,
    ) {
        savedSessions.add(topic to messages)
    }

    override suspend fun deleteSession(sessionId: String) {
        deletedSessionIds.add(sessionId)
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
