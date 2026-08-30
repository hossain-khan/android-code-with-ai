package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.coroutines.flow.Flow

/**
 * Repository managing persistent chat sessions and their associated messages.
 */
interface ChatSessionRepository {
    /**
     * Returns an observable [Flow] of all chat sessions ordered by last active timestamp descending.
     */
    fun getAllSessions(): Flow<List<ChatSession>>

    /**
     * Retrieves a single chat session by [sessionId], or `null` if not found.
     */
    suspend fun getSession(sessionId: String): ChatSession?

    /**
     * Retrieves all messages associated with [sessionId] ordered sequentially.
     */
    suspend fun getMessages(sessionId: String): List<ChatMessage>

    /**
     * Saves or updates a chat session with [messages], generating an automated summary if needed.
     *
     * @return The unique [sessionId] (either existing or newly generated).
     */
    suspend fun saveSession(
        topic: CodingTopic,
        messages: List<ChatMessage>,
        sessionId: String? = null,
        modelUsed: String? = null,
    ): String

    /**
     * Permanently deletes the session identified by [sessionId] and all its messages.
     */
    suspend fun deleteSession(sessionId: String)

    /**
     * Permanently deletes all chat sessions and messages from storage.
     */
    suspend fun clearAllSessions()
}
