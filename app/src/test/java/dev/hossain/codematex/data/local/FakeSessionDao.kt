package dev.hossain.codematex.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSessionDao(
    sessions: List<SessionEntity> = emptyList(),
    private val messages: List<MessageEntity> = emptyList(),
) : SessionDao {
    private val sessionsFlow = MutableStateFlow(sessions)
    val calls = mutableListOf<String>()
    val upsertedSessions = mutableListOf<SessionEntity>()
    val insertedMessages = mutableListOf<List<MessageEntity>>()

    override fun getAllSessions(): Flow<List<SessionEntity>> = sessionsFlow

    override suspend fun getMessages(sessionId: String): List<MessageEntity> = messages.filter { it.sessionId == sessionId }

    override suspend fun upsertSession(session: SessionEntity) {
        calls += "upsertSession"
        upsertedSessions += session
        sessionsFlow.value = sessionsFlow.value.filterNot { it.id == session.id } + session
    }

    override suspend fun insertMessages(messages: List<MessageEntity>) {
        calls += "insertMessages"
        insertedMessages += messages
    }

    override suspend fun deleteSession(sessionId: String) {
        calls += "deleteSession:$sessionId"
        sessionsFlow.value = sessionsFlow.value.filterNot { it.id == sessionId }
    }

    override suspend fun deleteMessages(sessionId: String) {
        calls += "deleteMessages:$sessionId"
    }
}
