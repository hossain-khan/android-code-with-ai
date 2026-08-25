package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.local.FakeSessionDao
import dev.hossain.codematex.data.local.MessageEntity
import dev.hossain.codematex.data.local.SessionEntity
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.domain.summary.FakeSessionSummaryGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
            assertEquals(1, sessions.size)
            assertEquals("s1", sessions[0].id)
            assertEquals(CodingTopic.PYTHON, sessions[0].topic)
            assertEquals("Kotlin session", sessions[0].title)
            assertEquals("A session about Kotlin", sessions[0].summary)
            assertEquals(2, sessions[0].messageCount)
            assertEquals(1_000L, sessions[0].lastActiveAt)
            assertEquals("gemma-2-2b-it", sessions[0].modelUsed)
        }

    @Test
    fun `given session exists - get session returns matching session`() =
        runTest {
            val dao =
                FakeSessionDao(
                    sessions = listOf(testSessionEntity(id = "s1"), testSessionEntity(id = "s2")),
                )
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            assertEquals("s2", repository.getSession("s2")?.id)
        }

    @Test
    fun `given session missing - get session returns null`() =
        runTest {
            val dao = FakeSessionDao(sessions = listOf(testSessionEntity(id = "s1")))
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            assertNull(repository.getSession("unknown"))
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
            assertEquals(5, messages.size)

            val typesAndContent =
                messages.map {
                    when (it) {
                        is ChatMessage.User -> "user" to it.content
                        is ChatMessage.Agent -> "agent" to it.content
                        is ChatMessage.Error -> "error" to it.message
                        is ChatMessage.System -> "system" to it.info
                    }
                }
            assertEquals(
                listOf(
                    "user" to "u",
                    "agent" to "a",
                    "error" to "e",
                    "system" to "s",
                    // Unknown types fall back to System messages
                    "system" to "fallback",
                ),
                typesAndContent,
            )

            // Message IDs stored in Room should be restored exactly.
            assertEquals(listOf("msg-0", "msg-1", "msg-2", "msg-3", "msg-4"), messages.map { it.id })
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
            assertEquals("Explain Kotlin coroutines", session.title)
            assertEquals(CodingTopic.KOTLIN.stableId, session.topic)
            assertEquals(2, session.messageCount)
        }

    @Test
    fun `given long user message - save session truncates title to fifty characters`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)
            val longMessage = "a".repeat(100)

            repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User(longMessage)))

            assertEquals("a".repeat(50), dao.upsertedSessions.single().title)
        }

    @Test
    fun `given no user message - save session title is untitled`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.Agent("Hello!")))

            assertEquals("Untitled", dao.upsertedSessions.single().title)
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

            assertEquals("Generated summary", dao.upsertedSessions.single().summary)
            assertEquals(1, summaryGenerator.generateSummaryCalls)
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
                assertEquals("Summary failed", e.message)
            }
        }

    @Test
    fun `given empty messages - save session stores generated summary`() =
        runTest {
            val dao = FakeSessionDao()
            summaryGenerator.summaryToReturn = "Empty session"
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.saveSession(CodingTopic.KOTLIN, emptyList())

            assertEquals("Empty session", dao.upsertedSessions.single().summary)
            assertEquals(1, summaryGenerator.generateSummaryCalls)
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
            assertEquals(4, entities.size)
            assertEquals(listOf("user", "agent", "error", "system"), entities.map { it.type })
            assertEquals(listOf("question", "answer", "boom", "note"), entities.map { it.content })
            assertEquals(listOf(0, 1, 2, 3), entities.map { it.orderIndex })
            val sessionId = dao.upsertedSessions.single().id
            assertTrue(entities.all { it.sessionId == sessionId })
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

            assertEquals(2, dao.upsertedSessions.size)
            assertTrue(dao.upsertedSessions.all { it.id == "existing-session" })
            assertEquals(
                listOf(
                    "replaceSession:existing-session",
                    "replaceSession:existing-session",
                ),
                dao.calls,
            )
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

            assertEquals("existing-session", returnedId)
        }

    @Test
    fun `given no session id - save session returns generated id`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            val returnedId = repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("Hi")))

            assertEquals(dao.upsertedSessions.single().id, returnedId)
            assertTrue(returnedId.isNotBlank())
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

            assertEquals("gemma-4-E2B", dao.upsertedSessions.single().modelUsed)
        }

    @Test
    fun `given no model used - save session defaults to unknown`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("Hi")))

            assertEquals("unknown", dao.upsertedSessions.single().modelUsed)
        }

    @Test
    fun `given messages to save - save session upserts session before inserting messages`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("Hi")))

            assertEquals(listOf("replaceSession:${dao.upsertedSessions.single().id}"), dao.calls)
        }

    @Test
    fun `given existing session - delete session deletes messages before session`() =
        runTest {
            val dao = FakeSessionDao(sessions = listOf(testSessionEntity(id = "s1")))
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            repository.deleteSession("s1")

            assertEquals(listOf("deleteMessages:s1", "deleteSession:s1"), dao.calls)
            assertTrue(repository.getAllSessions().first().isEmpty())
        }

    @Test
    fun `given unknown topic stored - get all sessions maps to unknown topic`() =
        runTest {
            val dao = FakeSessionDao(sessions = listOf(testSessionEntity(id = "s1", topic = "corrupt-topic")))
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            val sessions = repository.getAllSessions().first()

            assertEquals(CodingTopic.UNKNOWN, sessions.single().topic)
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

            assertEquals(1, messages.size)
            val system = messages.single() as ChatMessage.System
            assertEquals("fallback", system.info)
        }

    @Test
    fun `given no session id - save session generates unique ids for rapid calls`() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, summaryGenerator)

            val id1 = repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("A")))
            val id2 = repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("B")))

            assertTrue(id1.isNotBlank())
            assertTrue(id2.isNotBlank())
            assertNotEquals(id1, id2)
            assertEquals(2, dao.upsertedSessions.size)
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
                assertEquals("replaceSession failed", e.message)
            }
        }
}
