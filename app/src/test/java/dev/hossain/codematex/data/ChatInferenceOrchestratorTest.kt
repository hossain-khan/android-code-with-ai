package dev.hossain.codematex.data

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.repository.FakeChatSessionRepository
import dev.hossain.codematex.data.repository.FakeModelConfigStore
import dev.hossain.codematex.data.repository.FakeUserPreferencesStore
import dev.hossain.codematex.data.repository.ModelConfigStore
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.runtime.FakeLlmEngine
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.runtime.LowMemoryException
import dev.hossain.codematex.system.FakeSystemMemoryManager
import dev.hossain.codematex.system.MemoryHeadroomResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChatInferenceOrchestratorTest {
    private val configStore: ModelConfigStore = FakeModelConfigStore()
    private val fakeEngine = FakeLlmEngine()
    private val topicPromptProvider = DefaultTopicPromptProvider()
    private val fakeMemoryManager = FakeSystemMemoryManager()
    private val fakeUserPreferencesStore = FakeUserPreferencesStore()

    private fun createOrchestrator(
        messages: List<ChatMessage> = emptyList(),
        memoryManager: FakeSystemMemoryManager = fakeMemoryManager,
        userPreferencesStore: FakeUserPreferencesStore = fakeUserPreferencesStore,
    ): ChatInferenceOrchestrator =
        DefaultChatInferenceOrchestrator(
            llmEngine = fakeEngine,
            sessionRepository = FakeChatSessionRepository(messages = messages),
            configStore = configStore,
            topicPromptProvider = topicPromptProvider,
            systemMemoryManager = memoryManager,
            userPreferencesStore = userPreferencesStore,
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

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).hasSize(2)
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

            assertThat(result.getOrThrow()).containsExactlyElementsIn(existing).inOrder()
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

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message).isEqualTo("Init failed")
        }

    @Test
    fun `sendMessage emits tokens and done event without terminal empty token`() =
        runTest {
            fakeEngine.responseTokens = listOf("Hello", " world", "")

            val events = createOrchestrator().sendMessage("Hi").toList()

            assertThat(events)
                .containsExactly(
                    ChatInferenceEvent.Token("Hello"),
                    ChatInferenceEvent.Token(" world"),
                    ChatInferenceEvent.Done,
                ).inOrder()
        }

    @Test
    fun `stop delegates to engine`() {
        createOrchestrator().stop()

        assertThat(fakeEngine.stopCalls).isEqualTo(1)
    }

    @Test
    fun `resetConversation delegates to engine`() =
        runTest {
            createOrchestrator().resetConversation(CodingTopic.PYTHON)

            assertThat(fakeEngine.resetCalls).isEqualTo(1)
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
        assertThat(createOrchestrator().getActiveBackend()).isEqualTo(LlmEngine.Backend.CPU)
    }

    @Test
    fun `sendMessage emits BackendFailed and retries when backend fails`() =
        runTest {
            fakeEngine.backendFailureBackend = LlmEngine.Backend.GPU
            fakeEngine.responseTokens = listOf("CPU ", "response", "")

            val events = createOrchestrator().sendMessage("Hi").toList()

            assertThat(events)
                .containsExactly(
                    ChatInferenceEvent.BackendFailed(LlmEngine.Backend.GPU),
                    ChatInferenceEvent.Token("CPU "),
                    ChatInferenceEvent.Token("response"),
                    ChatInferenceEvent.Done,
                ).inOrder()
            assertThat(fakeEngine.runInferenceCalls).isEqualTo(2)
        }

    @Test
    fun `sendMessage retries through NPU and GPU failures before CPU succeeds`() =
        runTest {
            fakeEngine.backendFailureBackends =
                listOf(
                    LlmEngine.Backend.NPU,
                    LlmEngine.Backend.GPU,
                )
            fakeEngine.responseTokens = listOf("CPU ", "response", "")

            val events = createOrchestrator().sendMessage("Hi").toList()

            assertThat(events)
                .containsExactly(
                    ChatInferenceEvent.BackendFailed(LlmEngine.Backend.NPU),
                    ChatInferenceEvent.BackendFailed(LlmEngine.Backend.GPU),
                    ChatInferenceEvent.Token("CPU "),
                    ChatInferenceEvent.Token("response"),
                    ChatInferenceEvent.Done,
                ).inOrder()
            assertThat(fakeEngine.runInferenceCalls).isEqualTo(3)
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

            assertThat(fakeEngine.runInferenceCalls).isEqualTo(1)
        }

    @Test
    fun `initialize retrieves and uses model-specific configuration`() =
        runTest {
            val customConfig = ModelConfig(temperature = 1.4f, topK = 75, topP = 0.85f, maxTokens = 1024)
            configStore.setConfig("google/gemma-2-2b-it", customConfig)

            val result =
                createOrchestrator().initialize(
                    model = testModel(),
                    topic = CodingTopic.KOTLIN,
                    sessionId = "session-1",
                    existingMessages = emptyList(),
                )

            assertThat(result.isSuccess).isTrue()
            assertThat(fakeEngine.lastConfig).isEqualTo(customConfig)
        }

    @Test
    fun `initialize fails with LowMemoryException when memory is constrained`() =
        runTest {
            val constrainedMemoryManager =
                FakeSystemMemoryManager(
                    headroomResult =
                        MemoryHeadroomResult.Constrained(
                            availMemBytes = 500_000_000L,
                            requiredBytes = 1_200_000_000L,
                            isLowMemory = true,
                        ),
                )

            val result =
                createOrchestrator(
                    memoryManager = constrainedMemoryManager,
                ).initialize(
                    model = testModel(),
                    topic = CodingTopic.KOTLIN,
                    sessionId = null,
                    existingMessages = emptyList(),
                )

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()).isInstanceOf(LowMemoryException::class.java)
            assertThat(fakeEngine.initializeCalls).isEqualTo(0)
        }

    @Test
    fun `initialize succeeds when model is already loaded in memory even if available memory is low`() =
        runTest {
            val constrainedMemoryManager =
                FakeSystemMemoryManager(
                    headroomResult =
                        MemoryHeadroomResult.Constrained(
                            availMemBytes = 250_000_000L,
                            requiredBytes = 1_200_000_000L,
                            isLowMemory = false,
                        ),
                )

            // Mark model as already loaded in memory
            fakeEngine.loadedModelPath = "/models/gemma.task"

            val result =
                createOrchestrator(
                    memoryManager = constrainedMemoryManager,
                ).initialize(
                    model = testModel(),
                    topic = CodingTopic.KOTLIN,
                    sessionId = null,
                    existingMessages = emptyList(),
                )

            assertThat(result.isSuccess).isTrue()
            assertThat(fakeEngine.initializeCalls).isEqualTo(1)
        }
}
