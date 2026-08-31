package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.local.MessageEntity
import dev.hossain.codematex.data.local.SessionDao
import dev.hossain.codematex.data.local.SessionEntity
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ChatMessageKind
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.domain.summary.SessionSummaryGenerator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
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
        ): String {
            val startTime = System.currentTimeMillis()
            val effectiveSessionId = sessionId ?: UUID.randomUUID().toString()
            val title =
                messages
                    .filterIsInstance<ChatMessage.User>()
                    .firstOrNull()
                    ?.content
                    ?.take(TITLE_MAX_LENGTH) ?: "Untitled"

            val summary = summaryGenerator.generateSummary(messages)

            sessionDao.replaceSession(
                SessionEntity(
                    id = effectiveSessionId,
                    topic = topic.stableId,
                    title = title,
                    summary = summary,
                    messageCount = messages.size,
                    lastActiveAt = System.currentTimeMillis(),
                    modelUsed = modelUsed ?: "unknown",
                ),
                messages.mapIndexed { index, msg ->
                    msg.toMessageEntity(effectiveSessionId, index)
                },
            )

            val elapsedMs = System.currentTimeMillis() - startTime
            Timber.d(
                "ChatSessionRepository: Saved session '%s' in %d ms (topic=%s, messages=%d, title='%s', summary='%s')",
                effectiveSessionId,
                elapsedMs,
                topic.name,
                messages.size,
                title,
                summary,
            )

            return effectiveSessionId
        }

        override suspend fun deleteSession(sessionId: String) {
            sessionDao.deleteMessages(sessionId)
            sessionDao.deleteSession(sessionId)
        }

        override suspend fun clearAllSessions() {
            sessionDao.clearAll()
        }

        private fun SessionEntity.toChatSession(): ChatSession =
            ChatSession(
                id = id,
                topic = CodingTopic.fromStableId(topic),
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
            return when (ChatMessageKind.fromStableId(type)) {
                ChatMessageKind.USER -> ChatMessage.User(content, restoredId)

                ChatMessageKind.AGENT -> ChatMessage.Agent(content, isStreaming = false, restoredId)

                ChatMessageKind.ERROR -> ChatMessage.Error(content, restoredId)

                ChatMessageKind.SYSTEM,
                ChatMessageKind.UNKNOWN,
                -> ChatMessage.System(content, restoredId)
            }
        }

        private fun ChatMessage.toMessageEntity(
            sessionId: String,
            index: Int,
        ): MessageEntity {
            val (type, content) =
                when (this) {
                    is ChatMessage.User -> ChatMessageKind.USER.stableId to content
                    is ChatMessage.Agent -> ChatMessageKind.AGENT.stableId to content
                    is ChatMessage.Error -> ChatMessageKind.ERROR.stableId to message
                    is ChatMessage.System -> ChatMessageKind.SYSTEM.stableId to info
                }
            return MessageEntity(
                sessionId = sessionId,
                messageId = id,
                type = type,
                content = content,
                timestamp = System.currentTimeMillis(),
                orderIndex = index,
            )
        }

        private companion object {
            private const val TITLE_MAX_LENGTH = 50
        }
    }
