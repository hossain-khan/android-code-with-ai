package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.local.MessageEntity
import dev.hossain.codematex.data.local.SessionDao
import dev.hossain.codematex.data.local.SessionEntity
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.domain.summary.SessionSummaryGenerator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ChatSessionRepositoryImpl
    @Inject
    constructor(
        private val sessionDao: SessionDao,
        private val summaryGenerator: SessionSummaryGenerator,
    ) : ChatSessionRepository {
        override fun getAllSessions(): Flow<List<ChatSession>> =
            sessionDao.getAllSessions().map { entities ->
                entities.map { it.toChatSession() }
            }

        override suspend fun getSession(sessionId: String): ChatSession? =
            sessionDao.getSessionById(sessionId).map { it?.toChatSession() }.firstOrNull()

        override suspend fun getMessages(sessionId: String): List<ChatMessage> =
            sessionDao.getMessages(sessionId).map { it.toChatMessage() }

        override suspend fun saveSession(
            topic: CodingTopic,
            messages: List<ChatMessage>,
            sessionId: String?,
            modelUsed: String?,
        ) {
            val effectiveSessionId = sessionId ?: System.currentTimeMillis().toString()
            val title =
                messages
                    .filterIsInstance<ChatMessage.User>()
                    .firstOrNull()
                    ?.content
                    ?.take(TITLE_MAX_LENGTH) ?: "Untitled"

            val summary = summaryGenerator.generateSummary(messages)

            // When updating an existing session, clear its old messages so the stored
            // conversation matches the current message list exactly.
            if (sessionId != null) {
                sessionDao.deleteMessages(effectiveSessionId)
            }

            sessionDao.upsertSession(
                SessionEntity(
                    id = effectiveSessionId,
                    topic = topic.name,
                    title = title,
                    summary = summary,
                    messageCount = messages.size,
                    lastActiveAt = System.currentTimeMillis(),
                    modelUsed = modelUsed ?: "unknown",
                ),
            )
            sessionDao.insertMessages(
                messages.mapIndexed { index, msg ->
                    msg.toMessageEntity(effectiveSessionId, index)
                },
            )
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

        private fun MessageEntity.toChatMessage(): ChatMessage {
            // Messages saved before the messageId column was introduced have an empty
            // value here; generate a fresh ID for those legacy rows.
            val restoredId = messageId.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
            return when (type) {
                "user" -> ChatMessage.User(content, restoredId)
                "agent" -> ChatMessage.Agent(content, isStreaming = false, restoredId)
                "error" -> ChatMessage.Error(content, restoredId)
                "system" -> ChatMessage.System(content, restoredId)
                else -> ChatMessage.System(content, restoredId)
            }
        }

        private fun ChatMessage.toMessageEntity(
            sessionId: String,
            index: Int,
        ): MessageEntity =
            MessageEntity(
                sessionId = sessionId,
                messageId = id,
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

        private companion object {
            private const val TITLE_MAX_LENGTH = 50
        }
    }
