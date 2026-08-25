package dev.hossain.codematex.data

import android.content.ContextWrapper
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.repository.FakeChatSessionRepository
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.runtime.FakeLlmEngine
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.ui.overlay.ModelConfigStore
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatInferenceOrchestratorTest {
    private val fakeContext: ContextWrapper = ContextWrapper(null)
    private val configStore = ModelConfigStore(fakeContext)
    private val fakeEngine = FakeLlmEngine()
    private val topicPromptProvider = DefaultTopicPromptProvider()

    private fun createOrchestrator(messages: List<ChatMessage> = emptyList()) =
        DefaultChatInferenceOrchestrator(
            llmEngine = fakeEngine,
            sessionRepository = FakeChatSessionRepository(messages = messages),
            configStore = configStore,
            topicPromptProvider = topicPromptProvider,
        )

    private fun testModel() =
        testModel(
            id = "google/gemma-2-2b-it",
            downloadStatus = DownloadStatus.DOWNLOADED,
            localPath = "/models/gemma.task",
        )

    @Test
    fun `initialize returns success and loads messages when session exists`() =
        runTest {
            val result =
                createOrchestrator(
                    messages = listOf(ChatMessage.User("Hi"), ChatMessage.Agent("Hello")),
                ).initialize(
                    model = testModel(),
                    topic = CodingTopic.KOTLIN,
                    sessionId = "session-1",
                    existingMessages = emptyList(),
                )

            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow().size)
        }

    @Test
    fun `initialize uses existing messages when not empty`() =
        runTest {
            val existing = listOf(ChatMessage.User("Existing"))

            val result =
                createOrchestrator().initialize(
                    model = testModel(),
                    topic = CodingTopic.KOTLIN,
                    sessionId = "session-1",
                    existingMessages = existing,
                )

            assertEquals(existing, result.getOrThrow())
        }

    @Test
    fun `initialize returns failure when engine fails`() =
        runTest {
            fakeEngine.shouldThrow = RuntimeException("Init failed")

            val result =
                createOrchestrator().initialize(
                    model = testModel(),
                    topic = CodingTopic.KOTLIN,
                    sessionId = null,
                    existingMessages = emptyList(),
                )

            assertTrue(result.isFailure)
            assertEquals("Init failed", result.exceptionOrNull()?.message)
        }

    @Test
    fun `sendMessage emits tokens and done event`() =
        runTest {
            fakeEngine.responseTokens = listOf("Hello", " world", "")

            val events = createOrchestrator().sendMessage("Hi").toList()

            assertEquals(
                listOf(
                    ChatInferenceEvent.Token("Hello"),
                    ChatInferenceEvent.Token(" world"),
                    ChatInferenceEvent.Token(""),
                    ChatInferenceEvent.Done,
                ),
                events,
            )
        }

    @Test
    fun `stop delegates to engine`() {
        createOrchestrator().stop()

        assertEquals(1, fakeEngine.stopCalls)
    }

    @Test
    fun `resetConversation delegates to engine`() =
        runTest {
            createOrchestrator().resetConversation(CodingTopic.PYTHON)

            assertEquals(1, fakeEngine.resetCalls)
        }

    @Test(expected = kotlinx.coroutines.CancellationException::class)
    fun `initialize rethrows CancellationException when coroutine cancelled`() =
        runTest {
            fakeEngine.shouldThrow = kotlinx.coroutines.CancellationException("The coroutine scope left the composition")

            createOrchestrator().initialize(
                model = testModel(),
                topic = CodingTopic.KOTLIN,
                sessionId = null,
                existingMessages = emptyList(),
            )
        }

    @Test
    fun `getActiveBackend returns engine backend`() {
        assertEquals(LlmEngine.Backend.CPU, createOrchestrator().getActiveBackend())
    }

    @Test
    fun `sendMessage emits BackendFailed and retries when backend fails`() =
        runTest {
            fakeEngine.backendFailureBackend = LlmEngine.Backend.GPU
            fakeEngine.responseTokens = listOf("CPU ", "response", "")

            val events = createOrchestrator().sendMessage("Hi").toList()

            assertEquals(
                listOf(
                    ChatInferenceEvent.BackendFailed(LlmEngine.Backend.GPU),
                    ChatInferenceEvent.Token("CPU "),
                    ChatInferenceEvent.Token("response"),
                    ChatInferenceEvent.Token(""),
                    ChatInferenceEvent.Done,
                ),
                events,
            )
            assertEquals(2, fakeEngine.runInferenceCalls)
        }

    @Test
    fun `sendMessage does not retry when CPU backend fails`() =
        runTest {
            fakeEngine.backendFailureBackend = LlmEngine.Backend.CPU
            fakeEngine.responseTokens = listOf("CPU ", "response", "")

            try {
                createOrchestrator().sendMessage("Hi").toList()
            } catch (e: Exception) {
                // Expected.
            }

            assertEquals(1, fakeEngine.runInferenceCalls)
        }
}
