package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.local.MessageEntity
import dev.hossain.codematex.data.local.SessionDao
import dev.hossain.codematex.data.local.SessionEntity
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.runtime.LlmEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ChatSessionRepositoryImpl
    @Inject
    constructor(
        private val sessionDao: SessionDao,
        @Suppress("UNUSED_PARAMETER") private val llmEngine: LlmEngine,
    ) : ChatSessionRepository {
        override fun getAllSessions(): Flow<List<ChatSession>> =
            sessionDao.getAllSessions().map { entities ->
                entities.map { it.toChatSession() }
            }

        override suspend fun getSession(sessionId: String): ChatSession? = getAllSessions().first().firstOrNull { it.id == sessionId }

        override suspend fun getMessages(sessionId: String): List<ChatMessage> =
            sessionDao.getMessages(sessionId).map { it.toChatMessage() }

        override suspend fun saveSession(
            topic: CodingTopic,
            messages: List<ChatMessage>,
        ) {
            val sessionId = System.currentTimeMillis().toString()
            val title =
                messages
                    .filterIsInstance<ChatMessage.User>()
                    .firstOrNull()
                    ?.content
                    ?.take(50) ?: "Untitled"

            val summary = generateSummary(messages)

            sessionDao.upsertSession(
                SessionEntity(
                    id = sessionId,
                    topic = topic.name,
                    title = title,
                    summary = summary,
                    messageCount = messages.size,
                    lastActiveAt = System.currentTimeMillis(),
                    modelUsed = "current",
                ),
            )
            sessionDao.insertMessages(
                messages.mapIndexed { index, msg ->
                    msg.toMessageEntity(sessionId, index)
                },
            )
        }

        private suspend fun generateSummary(messages: List<ChatMessage>): String {
            val conversationText =
                messages
                    .joinToString("\n") { msg ->
                        when (msg) {
                            is ChatMessage.User -> "User: ${msg.content}"
                            is ChatMessage.Agent -> "AI: ${msg.content.take(200)}"
                            else -> ""
                        }
                    }.take(1000)

            if (conversationText.isBlank()) {
                return "Empty session"
            }

            var summary = ""
            try {
                llmEngine.runInference(
                    "Summarize this coding learning session in 1-2 sentences: $conversationText",
                ) { token, done ->
                    if (!done) {
                        summary += token
                    }
                }
            } catch (e: Exception) {
                // Fallback if summary generation fails
            }

            return summary.ifBlank { "Coding session about ${messages.size} messages" }
        }

        override suspend fun deleteSession(sessionId: String) {
            sessionDao.deleteMessages(sessionId)
            sessionDao.deleteSession(sessionId)
        }

        private fun SessionEntity.toChatSession(): ChatSession =
            ChatSession(
                id = id,
                topic = CodingTopic.valueOf(topic),
                title = title,
                summary = summary,
                messageCount = messageCount,
                lastActiveAt = lastActiveAt,
                modelUsed = modelUsed,
            )

        private fun MessageEntity.toChatMessage(): ChatMessage =
            when (type) {
                "user" -> ChatMessage.User(content)
                "agent" -> ChatMessage.Agent(content)
                "error" -> ChatMessage.Error(content)
                "system" -> ChatMessage.System(content)
                else -> ChatMessage.System(content)
            }

        private fun ChatMessage.toMessageEntity(
            sessionId: String,
            index: Int,
        ): MessageEntity =
            MessageEntity(
                sessionId = sessionId,
                type =
                    when (this) {
                        is ChatMessage.User -> "user"
                        is ChatMessage.Agent -> "agent"
                        is ChatMessage.Error -> "error"
                        is ChatMessage.System -> "system"
                    },
                content =
                    when (this) {
                        is ChatMessage.User -> content
                        is ChatMessage.Agent -> content
                        is ChatMessage.Error -> message
                        is ChatMessage.System -> info
                    },
                timestamp = System.currentTimeMillis(),
                orderIndex = index,
            )
    }
