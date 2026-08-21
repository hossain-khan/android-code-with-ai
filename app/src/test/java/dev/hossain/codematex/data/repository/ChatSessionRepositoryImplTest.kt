package dev.hossain.codematex.data.repository

import dev.hossain.codematex.circuit.FakeLlmEngine
import dev.hossain.codematex.data.local.FakeSessionDao
import dev.hossain.codematex.data.local.MessageEntity
import dev.hossain.codematex.data.local.SessionEntity
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSessionRepositoryImplTest {
    private val fakeEngine = FakeLlmEngine()

    private fun testSessionEntity(
        id: String = "s1",
        topic: String = "KOTLIN",
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
    ) = MessageEntity(
        sessionId = sessionId,
        type = type,
        content = content,
        timestamp = 1_000L,
        orderIndex = orderIndex,
    )

    @Test
    fun getAllSessions_mapsEntitiesToChatSessions() =
        runTest {
            val dao = FakeSessionDao(sessions = listOf(testSessionEntity(id = "s1", topic = "PYTHON")))
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)

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
    fun getSession_whenSessionExists_returnsMatchingSession() =
        runTest {
            val dao =
                FakeSessionDao(
                    sessions = listOf(testSessionEntity(id = "s1"), testSessionEntity(id = "s2")),
                )
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)

            assertEquals("s2", repository.getSession("s2")?.id)
        }

    @Test
    fun getSession_whenSessionMissing_returnsNull() =
        runTest {
            val dao = FakeSessionDao(sessions = listOf(testSessionEntity(id = "s1")))
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)

            assertNull(repository.getSession("unknown"))
        }

    @Test
    fun getMessages_mapsEntityTypesToChatMessages() =
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
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)

            val messages = repository.getMessages("s1")
            assertEquals(5, messages.size)

            // ChatMessage ids are random UUIDs, so compare type + content instead of equality.
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
        }

    @Test
    fun saveSession_usesFirstUserMessageAsTitle() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)
            val messages =
                listOf(
                    ChatMessage.User("Explain Kotlin coroutines"),
                    ChatMessage.Agent("Coroutines are lightweight threads."),
                )

            repository.saveSession(CodingTopic.KOTLIN, messages)

            val session = dao.upsertedSessions.single()
            assertEquals("Explain Kotlin coroutines", session.title)
            assertEquals("KOTLIN", session.topic)
            assertEquals(2, session.messageCount)
        }

    @Test
    fun saveSession_truncatesLongTitlesToFiftyCharacters() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)
            val longMessage = "a".repeat(100)

            repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User(longMessage)))

            assertEquals("a".repeat(50), dao.upsertedSessions.single().title)
        }

    @Test
    fun saveSession_withoutUserMessage_titleIsUntitled() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)

            repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.Agent("Hello!")))

            assertEquals("Untitled", dao.upsertedSessions.single().title)
        }

    @Test
    fun saveSession_generatesSummaryWithLlm() =
        runTest {
            val dao = FakeSessionDao()
            // Real engine emits tokens with done=false, then a final empty token with done=true.
            fakeEngine.responseTokens = listOf("Generated ", "summary", "")
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)
            val messages =
                listOf(
                    ChatMessage.User("Explain Kotlin coroutines"),
                    ChatMessage.Agent("Coroutines are lightweight threads."),
                )

            repository.saveSession(CodingTopic.KOTLIN, messages)

            assertEquals("Generated summary", dao.upsertedSessions.single().summary)
        }

    @Test
    fun saveSession_whenLlmFails_usesFallbackSummary() =
        runTest {
            val dao = FakeSessionDao()
            fakeEngine.shouldThrow = RuntimeException("Inference failed")
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)
            val messages =
                listOf(
                    ChatMessage.User("Explain Kotlin coroutines"),
                    ChatMessage.Agent("Coroutines are lightweight threads."),
                )

            repository.saveSession(CodingTopic.KOTLIN, messages)

            assertEquals("Coding session about 2 messages", dao.upsertedSessions.single().summary)
        }

    @Test
    fun saveSession_withEmptyMessages_summaryIsEmptySessionAndLlmNotCalled() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)

            repository.saveSession(CodingTopic.KOTLIN, emptyList())

            assertEquals("Empty session", dao.upsertedSessions.single().summary)
            assertNull(fakeEngine.lastInput)
        }

    @Test
    fun saveSession_mapsMessageTypesAndOrderToEntities() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)
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
    fun saveSession_upsertsSessionBeforeInsertingMessages() =
        runTest {
            val dao = FakeSessionDao()
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)

            repository.saveSession(CodingTopic.KOTLIN, listOf(ChatMessage.User("Hi")))

            assertEquals(listOf("upsertSession", "insertMessages"), dao.calls)
        }

    @Test
    fun deleteSession_deletesMessagesBeforeSession() =
        runTest {
            val dao = FakeSessionDao(sessions = listOf(testSessionEntity(id = "s1")))
            val repository = ChatSessionRepositoryImpl(dao, fakeEngine)

            repository.deleteSession("s1")

            assertEquals(listOf("deleteMessages:s1", "deleteSession:s1"), dao.calls)
            assertTrue(repository.getAllSessions().first().isEmpty())
        }
}
