package dev.hossain.codematex.data.repository

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.local.FakeSessionDao
import dev.hossain.codematex.data.local.MessageEntity
import dev.hossain.codematex.data.local.SessionEntity
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.domain.summary.FakeSessionSummaryGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ChatSessionRepositoryImpl].
 */
class ChatSessionRepositoryImplTest {
    private lateinit var summaryGenerator: FakeSessionSummaryGenerator

    @Before
    fun setUp() {
        summaryGenerator = FakeSessionSummaryGenerator()
    }

    private fun testSessionEntity(
        id: String = "s1",
        topic: String = CodingTopic.KOTLIN.stableId,
    ) = SessionEntity(
        id = id,
        topic = topic,
        title = "Kotlin session",
        summary = "A session about Kotlin",
        messageCount = 2,
        lastActiveAt = 1_000L,
        modelUsed = "gemma-2-2b-it",
    )

    private fun testMessageEntity(
        sessionId: String = "s1",
        type: String,
        content: String = "content",
        orderIndex: Int = 0,
        messageId: String = "msg-$orderIndex",
    ) = MessageEntity(
        sessionId = sessionId,
        messageId = messageId,
        type = type,
        content = content,
        timestamp = 1_000L,
        orderIndex = orderIndex,
    )

    @Test
    fun `given session entities - get all sessions maps entities to chat sessions`() =
        runTest {
            val dao = FakeSessionDao(sessions = listOf(testSessionEntity(id = "s1", topic = CodingTopic.PYTHON.stableId)))
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            val sessions = repository.getAllSessions().first()
            assertThat(sessions).hasSize(1)
            assertThat(sessions[0].id).isEqualTo("s1")
            assertThat(sessions[0].topic).isEqualTo(CodingTopic.PYTHON)
            assertThat(sessions[0].title).isEqualTo("Kotlin session")
            assertThat(sessions[0].summary).isEqualTo("A session about Kotlin")
            assertThat(sessions[0].messageCount).isEqualTo(2)
            assertThat(sessions[0].lastActiveAt).isEqualTo(1_000L)
            assertThat(sessions[0].modelUsed).isEqualTo("gemma-2-2b-it")
        }

    @Test
    fun `given session exists - get session returns matching session`() =
        runTest {
            val dao =
                FakeSessionDao(
                    sessions = listOf(testSessionEntity(id = "s1"), testSessionEntity(id = "s2")),
                )
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            assertThat(repository.getSession("s2")?.id).isEqualTo("s2")
        }

    @Test
    fun `given session missing - get session returns null`() =
        runTest {
            val dao = FakeSessionDao(sessions = listOf(testSessionEntity(id = "s1")))
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            assertThat(repository.getSession("unknown")).isNull()
        }

    @Test
    fun `given message entities of all types - get messages maps entity types to chat messages`() =
        runTest {
            val dao =
                FakeSessionDao(
                    messages =
                        listOf(
                            testMessageEntity(type = "user", content = "u", orderIndex = 0),
                            testMessageEntity(type = "agent", content = "a", orderIndex = 1),
                            testMessageEntity(type = "error", content = "e", orderIndex = 2),
                            testMessageEntity(type = "system", content = "s", orderIndex = 3),
                            testMessageEntity(type = "unknown-type", content = "fallback", orderIndex = 4),
                            testMessageEntity(sessionId = "other", type = "user", content = "x", orderIndex = 0),
                        ),
                )
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            val messages = repository.getMessages("s1")
            assertThat(messages).hasSize(5)

            val typesAndContent =
                messages.map {
                    when (it) {
                        is ChatMessage.User -> "user" to it.content
                        is ChatMessage.Agent -> "agent" to it.content
                        is ChatMessage.Error -> "error" to it.message
                        is ChatMessage.System -> "system" to it.info
                    }
                }
            assertThat(typesAndContent)
                .containsExactly(
                    "user" to "u",
                    "agent" to "a",
                    "error" to "e",
                    "system" to "s",
                    // Unknown types fall back to System messages
                    "system" to "fallback",
                ).inOrder()

            // Message IDs stored in Room should be restored exactly.
            assertThat(messages.map { it.id })
                .containsExactly(
                    "msg-0",
                    "msg-1",
                    "msg-2",
                    "msg-3",
                    "msg-4",
                ).inOrder()
        }

    @Test
    fun `given user message present - save session uses first user message as title`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)
            val messages =
                listOf(
                    ChatMessage.User("Explain Kotlin coroutines"),
                    ChatMessage.Agent("Coroutines are lightweight threads."),
                )

            repository.saveSession(CodingTopic.KOTLIN, messages)

            val session = dao.upsertedSessions.single()
            assertThat(session.title).isEqualTo("Explain Kotlin coroutines")
            assertThat(session.topic).isEqualTo(CodingTopic.KOTLIN.stableId)
            assertThat(session.messageCount).isEqualTo(2)
        }

    @Test
    fun `given long user message - save session truncates title to fifty characters`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)
            val longMessage = "a".repeat(100)

            repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User(longMessage)))

            assertThat(dao.upsertedSessions.single().title).isEqualTo("a".repeat(50))
        }

    @Test
    fun `given no user message - save session title is untitled`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.Agent("Hello!")))

            assertThat(dao.upsertedSessions.single().title).isEqualTo("Untitled")
        }

    @Test
    fun `given summary generator returns summary - save session stores summary`() =
        runTest {
            val dao = FakeSessionDao()
            summaryGenerator.summaryToReturn = "Generated summary"
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)
            val messages =
                listOf(
                    ChatMessage.User("Explain Kotlin coroutines"),
                    ChatMessage.Agent("Coroutines are lightweight threads."),
                )

            repository.saveSession(CodingTopic.KOTLIN, messages)

            assertThat(dao.upsertedSessions.single().summary).isEqualTo("Generated summary")
            assertThat(summaryGenerator.generateSummaryCalls).isEqualTo(1)
        }

    @Test
    fun `given summary generator throws - save session propagates exception`() =
        runTest {
            val dao = FakeSessionDao()
            summaryGenerator.shouldThrow = RuntimeException("Summary failed")
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)
            val messages =
                listOf(
                    ChatMessage.User("Explain Kotlin coroutines"),
                    ChatMessage.Agent("Coroutines are lightweight threads."),
                )

            try {
                repository.saveSession(CodingTopic.KOTLIN, messages)
                throw AssertionError("Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertThat(e.message).isEqualTo("Summary failed")
            }
        }

    @Test
    fun `given empty messages - save session stores generated summary`() =
        runTest {
            val dao = FakeSessionDao()
            summaryGenerator.summaryToReturn = "Empty session"
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.saveSession(CodingTopic.KOTLIN, emptyList())

            assertThat(dao.upsertedSessions.single().summary).isEqualTo("Empty session")
            assertThat(summaryGenerator.generateSummaryCalls).isEqualTo(1)
        }

    @Test
    fun `given all message types - save session maps message types and order to entities`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)
            val messages =
                listOf(
                    ChatMessage.User("question"),
                    ChatMessage.Agent("answer"),
                    ChatMessage.Error("boom"),
                    ChatMessage.System("note"),
                )

            repository.saveSession(CodingTopic.KOTLIN, messages)

            val entities = dao.insertedMessages.single()
            assertThat(entities).hasSize(4)
            assertThat(entities.map { it.type }).containsExactly("user", "agent", "error", "system").inOrder()
            assertThat(entities.map { it.content }).containsExactly("question", "answer", "boom", "note").inOrder()
            assertThat(entities.map { it.orderIndex }).containsExactly(0, 1, 2, 3).inOrder()
            val sessionId = dao.upsertedSessions.single().id
            assertThat(entities.all { it.sessionId == sessionId }).isTrue()
        }

    @Test
    fun `given existing session id - save session reuses id and deletes old messages`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.saveSession(
                CodingTopic.KOTLIN,
                listOf(ChatMessage.User("First")),
                sessionId = "existing-session",
            )

            repository.saveSession(
                CodingTopic.KOTLIN,
                listOf(ChatMessage.User("Updated")),
                sessionId = "existing-session",
            )

            assertThat(dao.upsertedSessions).hasSize(2)
            assertThat(dao.upsertedSessions.all { it.id == "existing-session" }).isTrue()
            assertThat(dao.calls)
                .containsExactly(
                    "replaceSession:existing-session",
                    "replaceSession:existing-session",
                ).inOrder()
        }

    @Test
    fun `given existing session id - save session returns same id`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            val returnedId =
                repository.saveSession(
                    CodingTopic.KOTLIN,
                    listOf(ChatMessage.User("Hi")),
                    sessionId = "existing-session",
                )

            assertThat(returnedId).isEqualTo("existing-session")
        }

    @Test
    fun `given no session id - save session returns generated id`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            val returnedId = repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("Hi")))

            assertThat(returnedId).isEqualTo(dao.upsertedSessions.single().id)
            assertThat(returnedId).isNotEmpty()
        }

    @Test
    fun `given model used - save session persists model name`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.saveSession(
                CodingTopic.KOTLIN,
                listOf(ChatMessage.User("Hi")),
                modelUsed = "gemma-4-E2B",
            )

            assertThat(dao.upsertedSessions.single().modelUsed).isEqualTo("gemma-4-E2B")
        }

    @Test
    fun `given no model used - save session defaults to unknown`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("Hi")))

            assertThat(dao.upsertedSessions.single().modelUsed).isEqualTo("unknown")
        }

    @Test
    fun `given messages to save - save session upserts session before inserting messages`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("Hi")))

            assertThat(dao.calls).containsExactly("replaceSession:${dao.upsertedSessions.single().id}")
        }

    @Test
    fun `given existing session - delete session deletes messages before session`() =
        runTest {
            val dao = FakeSessionDao(sessions = listOf(testSessionEntity(id = "s1")))
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.deleteSession("s1")

            assertThat(dao.calls).containsExactly("deleteMessages:s1", "deleteSession:s1").inOrder()
            assertThat(repository.getAllSessions().first()).isEmpty()
        }

    @Test
    fun `given unknown topic stored - get all sessions maps to unknown topic`() =
        runTest {
            val dao = FakeSessionDao(sessions = listOf(testSessionEntity(id = "s1", topic = "corrupt-topic")))
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            val sessions = repository.getAllSessions().first()

            assertThat(sessions.single().topic).isEqualTo(CodingTopic.UNKNOWN)
        }

    @Test
    fun `given unknown message type stored - get messages maps to system message`() =
        runTest {
            val dao =
                FakeSessionDao(
                    messages =
                        listOf(
                            testMessageEntity(type = "corrupt-type", content = "fallback", orderIndex = 0),
                        ),
                )
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            val messages = repository.getMessages("s1")

            assertThat(messages).hasSize(1)
            val system = messages.single() as ChatMessage.System
            assertThat(system.info).isEqualTo("fallback")
        }

    @Test
    fun `given no session id - save session generates unique ids for rapid calls`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            val id1 = repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("A")))
            val id2 = repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("B")))

            assertThat(id1).isNotEmpty()
            assertThat(id2).isNotEmpty()
            assertThat(id1).isNotEqualTo(id2)
            assertThat(dao.upsertedSessions).hasSize(2)
        }

    @Test
    fun `given replace session throws - save session propagates exception`() =
        runTest {
            val dao = FakeSessionDao(throwOnReplace = true)
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            try {
                repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("Hi")))
                throw AssertionError("Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertThat(e.message).isEqualTo("replaceSession failed")
            }
        }
}
