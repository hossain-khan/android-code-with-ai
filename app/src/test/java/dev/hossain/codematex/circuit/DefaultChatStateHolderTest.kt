package dev.hossain.codematex.circuit

import android.content.Context
import android.content.ContextWrapper
import dev.hossain.codematex.circuit.overlay.ModelConfigStore
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.TutorPersona
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DefaultChatStateHolderTest {
    private val fakeContext: Context = ContextWrapper(null)
    private val configStore = ModelConfigStore(fakeContext)
    private val fakeSessionRepo = FakeChatSessionRepository()
    private val fakeChatInferenceOrchestrator = FakeChatInferenceOrchestrator()
    private val fakeSystemStatsMonitor = FakeSystemStatsMonitor()

    private fun createStateHolder(
        screen: ChatScreen = ChatScreen(CodingTopic.KOTLIN),
        modelRepository: FakeModelRepository = FakeModelRepository(),
        sessionRepository: FakeChatSessionRepository = fakeSessionRepo,
    ): DefaultChatStateHolder {
        val holder =
            DefaultChatStateHolder(
                screen = screen,
                modelRepository = modelRepository,
                sessionRepository = sessionRepository,
                configStore = configStore,
                chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                systemStatsMonitor = fakeSystemStatsMonitor,
            )
        return holder
    }

    @Test
    fun `loadAvailableModels sets active model and available models`() =
        runTest {
            val model = testModel(downloadStatus = DownloadStatus.DOWNLOADED)
            val modelRepo = FakeModelRepository(availableModels = listOf(model), selectedModel = model)
            val holder = createStateHolder(modelRepository = modelRepo)
            holder.attachScope(this)

            holder.loadAvailableModels()
            advanceUntilIdle()

            assertEquals(model, holder.activeModel)
            assertEquals(listOf(model), holder.availableModels)
        }

    @Test
    fun `loadSessionMessages loads messages for existing session`() =
        runTest {
            val messages = listOf(ChatMessage.User("Hello"), ChatMessage.Agent("Hi"))
            val sessionRepo = FakeChatSessionRepository(messages = messages)
            val holder =
                createStateHolder(
                    screen = ChatScreen(CodingTopic.KOTLIN, sessionId = "session-1"),
                    sessionRepository = sessionRepo,
                )
            holder.attachScope(this)

            holder.loadSessionMessages()
            advanceUntilIdle()

            assertEquals(messages, holder.messages)
        }

    @Test
    fun `initializeModel success updates messages when loaded`() =
        runTest {
            val model = testModel(downloadStatus = DownloadStatus.DOWNLOADED)
            val loadedMessages = listOf(ChatMessage.User("Prior"), ChatMessage.Agent("Response"))
            fakeChatInferenceOrchestrator.initializeResult = Result.success(loadedMessages)
            val holder =
                createStateHolder(
                    modelRepository = FakeModelRepository(availableModels = listOf(model), selectedModel = model),
                )
            holder.attachScope(this)

            holder.loadAvailableModels()
            advanceUntilIdle()
            holder.initializeModel()
            advanceUntilIdle()

            assertFalse(holder.isPreparing)
            assertNull(holder.errorMessage)
            assertEquals(loadedMessages, holder.messages)
        }

    @Test
    fun `initializeModel failure sets error message`() =
        runTest {
            val model = testModel(downloadStatus = DownloadStatus.DOWNLOADED)
            fakeChatInferenceOrchestrator.initializeResult = Result.failure(RuntimeException("Init failed"))
            val holder =
                createStateHolder(
                    modelRepository = FakeModelRepository(availableModels = listOf(model), selectedModel = model),
                )
            holder.attachScope(this)

            holder.loadAvailableModels()
            advanceUntilIdle()
            holder.initializeModel()
            advanceUntilIdle()

            assertFalse(holder.isPreparing)
            assertEquals("Init failed", holder.errorMessage)
        }

    @Test
    fun `sendMessage streams tokens and saves session`() =
        runTest {
            val model = testModel(downloadStatus = DownloadStatus.DOWNLOADED)
            val holder =
                createStateHolder(
                    modelRepository = FakeModelRepository(selectedModel = model),
                )
            holder.attachScope(this)
            fakeChatInferenceOrchestrator.messageEvents =
                listOf(
                    ChatInferenceEvent.Token("Hello"),
                    ChatInferenceEvent.Token(" world"),
                    ChatInferenceEvent.Done,
                )

            holder.sendMessage("Hi")
            advanceUntilIdle()

            assertFalse(holder.isGenerating)
            assertEquals(2, holder.messages.size)
            assertEquals("Hi", (holder.messages[0] as ChatMessage.User).content)
            assertEquals("Hello world", (holder.messages[1] as ChatMessage.Agent).content)
            assertEquals(1, fakeSessionRepo.savedSessions.size)
        }

    @Test
    fun `sendMessage failure sets error state`() =
        runTest {
            val model = testModel(downloadStatus = DownloadStatus.DOWNLOADED)
            val holder =
                createStateHolder(
                    modelRepository = FakeModelRepository(selectedModel = model),
                )
            holder.attachScope(this)
            fakeChatInferenceOrchestrator.shouldThrow = RuntimeException("Inference failed")

            holder.sendMessage("Hi")
            advanceUntilIdle()

            assertFalse(holder.isGenerating)
            assertEquals("Error: Inference failed", holder.throughputInfo)
            assertTrue(holder.messages.last() is ChatMessage.Error)
        }

    @Test
    fun `stopGeneration delegates to engine stop`() =
        runTest {
            val holder = createStateHolder()
            holder.attachScope(this)

            holder.stopGeneration()

            assertEquals(1, fakeChatInferenceOrchestrator.stopCalls)
        }

    @Test
    fun `resetSession clears messages and delegates to orchestrator`() =
        runTest {
            val holder = createStateHolder()
            holder.attachScope(this)
            holder.sendMessage("Hi")
            advanceUntilIdle()
            assertTrue(holder.messages.isNotEmpty())

            holder.resetSession()
            advanceUntilIdle()

            assertTrue(holder.messages.isEmpty())
            assertNull(holder.throughputInfo)
            assertNull(holder.systemStatsInfo)
            assertTrue(fakeChatInferenceOrchestrator.resetConversationTopics.contains(CodingTopic.KOTLIN))
        }

    @Test
    fun `selectPersona updates persona and delegates to orchestrator`() =
        runTest {
            val holder = createStateHolder()
            holder.attachScope(this)

            holder.selectPersona(TutorPersona.BEGINNER_FRIENDLY)
            advanceUntilIdle()

            assertEquals(TutorPersona.BEGINNER_FRIENDLY, holder.persona)
            assertTrue(fakeChatInferenceOrchestrator.resetConversationPersonas.contains(TutorPersona.BEGINNER_FRIENDLY))
            assertTrue(holder.messages.any { it is ChatMessage.System })
        }

    @Test
    fun `retry increments init trigger`() =
        runTest {
            val holder = createStateHolder()
            assertEquals(0, holder.initTrigger)

            holder.retry()

            assertEquals(1, holder.initTrigger)
        }
}
