package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.coroutines.flow.Flow

interface ChatSessionRepository {
    fun getAllSessions(): Flow<List<ChatSession>>
    suspend fun getSession(sessionId: String): ChatSession?
    suspend fun getMessages(sessionId: String): List<ChatMessage>
    suspend fun saveSession(topic: CodingTopic, messages: List<ChatMessage>)
    suspend fun deleteSession(sessionId: String)
}
