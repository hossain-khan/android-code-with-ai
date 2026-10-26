package dev.hossain.codematex.data

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.data.repository.FakeChatSessionRepository
import dev.hossain.codematex.data.repository.FakeModelConfigStore
import dev.hossain.codematex.data.repository.FakeUserPreferencesStore
import dev.hossain.codematex.data.repository.ModelConfigStore
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.runtime.DEV_STUB_MODEL_PATH
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
            val sessionMessages = listOf(ChatMessage.User("Hi"), ChatMessage.Agent("Hello"))
            val result =
                createOrchestrator(
                    messages = sessionMessages,
                ).initialize(
                    model = testModel(),
                    topic = CodingTopic.KOTLIN,
                    sessionId = "session-1",
                    existingMessages = emptyList(),
                )

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).hasSize(2)
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(1)
            assertThat(fakeEngine.restoredMessages).containsExactly(sessionMessages)
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
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(1)
            assertThat(fakeEngine.restoredMessages).containsExactly(existing)
        }

    @Test
    fun `initialize with sessionId null and non-empty existingMessages restores history to engine`() =
        runTest {
            val existing = listOf(ChatMessage.User("Question"), ChatMessage.Agent("Answer"))

            val result =
                createOrchestrator().initialize(
                    model = testModel(),
                    topic = CodingTopic.KOTLIN,
                    sessionId = null,
                    existingMessages = existing,
                )

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).containsExactlyElementsIn(existing).inOrder()
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(1)
            assertThat(fakeEngine.restoredMessages).containsExactly(existing)
        }

    @Test
    fun `initialize with sessionId null and empty existingMessages does not restore history`() =
        runTest {
            val result =
                createOrchestrator().initialize(
                    model = testModel(),
                    topic = CodingTopic.KOTLIN,
                    sessionId = null,
                    existingMessages = emptyList(),
                )

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrThrow()).isEmpty()
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(0)
        }

    @Test
    fun `initialize with in-flight streaming message sanitizes streaming state before restoring history`() =
        runTest {
            val existing =
                listOf(
                    ChatMessage.User("Write code"),
                    ChatMessage.Agent("Here is the", isStreaming = true),
                )

            val result =
                createOrchestrator().initialize(
                    model = testModel(),
                    topic = CodingTopic.KOTLIN,
                    sessionId = null,
                    existingMessages = existing,
                )

            assertThat(result.isSuccess).isTrue()
            val loaded = result.getOrThrow()
            assertThat(loaded).hasSize(2)
            assertThat((loaded.last() as ChatMessage.Agent).isStreaming).isFalse()
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(1)
            val restored = fakeEngine.restoredMessages.single()
            assertThat((restored.last() as ChatMessage.Agent).isStreaming).isFalse()
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
    fun `sendMessage restores existing conversation history on fallback session when backend fails`() =
        runTest {
            fakeEngine.backendFailureBackend = LlmEngine.Backend.GPU
            fakeEngine.responseTokens = listOf("CPU ", "response", "")

            val priorMessages =
                listOf(
                    ChatMessage.User("First turn"),
                    ChatMessage.Agent("First reply"),
                )

            val orchestrator = createOrchestrator()
            val events = orchestrator.sendMessage("Second turn", priorMessages).toList()

            assertThat(events)
                .containsExactly(
                    ChatInferenceEvent.BackendFailed(LlmEngine.Backend.GPU),
                    ChatInferenceEvent.Token("CPU "),
                    ChatInferenceEvent.Token("response"),
                    ChatInferenceEvent.Done,
                ).inOrder()
            assertThat(fakeEngine.runInferenceCalls).isEqualTo(2)
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(1)
            assertThat(fakeEngine.restoredMessages).containsExactly(priorMessages)
        }

    @Test
    fun `sendMessage restores activeMessages from initialize on fallback session when backend fails`() =
        runTest {
            fakeEngine.backendFailureBackend = LlmEngine.Backend.GPU
            fakeEngine.responseTokens = listOf("CPU ", "response", "")

            val initialMessages =
                listOf(
                    ChatMessage.User("Loaded turn"),
                    ChatMessage.Agent("Loaded reply"),
                )

            val orchestrator = createOrchestrator()
            orchestrator.initialize(
                model = testModel(),
                topic = CodingTopic.KOTLIN,
                sessionId = "test-session",
                existingMessages = initialMessages,
            )

            // Initial history restore during initialize
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(1)

            val events = orchestrator.sendMessage("Follow up").toList()

            assertThat(events)
                .containsExactly(
                    ChatInferenceEvent.BackendFailed(LlmEngine.Backend.GPU),
                    ChatInferenceEvent.Token("CPU "),
                    ChatInferenceEvent.Token("response"),
                    ChatInferenceEvent.Done,
                ).inOrder()
            // Second history restore should happen during backend failure fallback
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(2)
            assertThat(fakeEngine.restoredMessages.last()).isEqualTo(initialMessages)
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

    @Test
    fun `initialize releases previously loaded model from memory before checking headroom when switching models`() =
        runTest {
            fakeEngine.isInitializedValue = true
            fakeEngine.loadedModelPath = "/models/old_model.litertlm"

            val result =
                createOrchestrator().initialize(
                    model = testModel(localPath = "/models/new_model.litertlm"),
                    topic = CodingTopic.KOTLIN,
                    sessionId = null,
                    existingMessages = emptyList(),
                )

            assertThat(result.isSuccess).isTrue()
            assertThat(fakeEngine.cleanupCalls).isEqualTo(1)
            assertThat(fakeEngine.initializeCalls).isEqualTo(1)
        }

    @Test
    fun `sendMessage lazily re-initializes engine and restores history when engine was evicted from memory`() =
        runTest {
            val initialMessages =
                listOf(
                    ChatMessage.User("Prior question"),
                    ChatMessage.Agent("Prior answer"),
                )
            val model = testModel(id = "google/gemma-2-2b-it", localPath = "/models/gemma.task")
            val orchestrator = createOrchestrator()

            // Initial model load
            orchestrator.initialize(
                model = model,
                topic = CodingTopic.KOTLIN,
                sessionId = "session-1",
                existingMessages = initialMessages,
            )
            assertThat(fakeEngine.initializeCalls).isEqualTo(1)
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(1)

            // Simulate external memory eviction (e.g. SystemMemoryManager trim or background timeout)
            fakeEngine.cleanup()
            assertThat(fakeEngine.isInitialized()).isFalse()

            // User returns and sends a message without manual reload
            val events = orchestrator.sendMessage("Follow-up after eviction", initialMessages).toList()

            // Verify the engine was automatically re-initialized and history restored before dispatch
            assertThat(fakeEngine.initializeCalls).isEqualTo(2)
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(2)
            assertThat(fakeEngine.restoredMessages.last()).containsExactlyElementsIn(initialMessages).inOrder()
            assertThat(events)
                .containsExactly(
                    ChatInferenceEvent.Token("test response"),
                    ChatInferenceEvent.Done,
                ).inOrder()
        }

    @Test
    fun `sendMessage fails with LowMemoryException when memory is constrained during lazy reload after eviction`() =
        runTest {
            val initialMessages =
                listOf(
                    ChatMessage.User("Prior question"),
                    ChatMessage.Agent("Prior answer"),
                )
            val model = testModel(id = "google/gemma-2-2b-it", localPath = "/models/gemma.task")
            val orchestrator = createOrchestrator()

            orchestrator.initialize(
                model = model,
                topic = CodingTopic.KOTLIN,
                sessionId = "session-1",
                existingMessages = initialMessages,
            )

            // Simulate external eviction
            fakeEngine.cleanup()
            assertThat(fakeEngine.isInitialized()).isFalse()

            // Memory is now constrained when user attempts to send message
            fakeMemoryManager.headroomResult =
                MemoryHeadroomResult.Constrained(
                    availMemBytes = 300_000_000L,
                    requiredBytes = 1_500_000_000L,
                    isLowMemory = true,
                )

            var caughtException: Throwable? = null
            try {
                orchestrator.sendMessage("Follow-up question", initialMessages).toList()
            } catch (e: Throwable) {
                caughtException = e
            }

            assertThat(caughtException).isInstanceOf(LowMemoryException::class.java)
            assertThat(fakeEngine.initializeCalls).isEqualTo(1) // No second initialization occurred
        }

    @Test
    fun `sendMessage skips lazy reload when model is dev stub model`() =
        runTest {
            val model = testModel(id = "stub-model", localPath = DEV_STUB_MODEL_PATH)
            val orchestrator = createOrchestrator()

            orchestrator.initialize(
                model = model,
                topic = CodingTopic.KOTLIN,
                sessionId = "session-1",
                existingMessages = emptyList(),
            )
            val initialInitCalls = fakeEngine.initializeCalls

            orchestrator.sendMessage("Hello stub", emptyList()).toList()

            // Does not attempt to re-initialize stub model on sendMessage
            assertThat(fakeEngine.initializeCalls).isEqualTo(initialInitCalls)
            assertThat(fakeEngine.isInitialized()).isFalse()
        }

    @Test
    fun `switchPersona updates active persona and lazy reload restores updated persona prompt after eviction`() =
        runTest {
            val initialMessages = listOf(ChatMessage.User("Hello"))
            val model = testModel(id = "google/gemma-2-2b-it", localPath = "/models/gemma.task")
            val orchestrator = createOrchestrator()

            orchestrator.initialize(
                model = model,
                topic = CodingTopic.KOTLIN,
                sessionId = "session-1",
                existingMessages = initialMessages,
                persona = TutorPersona.SENIOR_ENGINEER,
            )

            // User switches persona mid-conversation
            orchestrator.switchPersona(
                topic = CodingTopic.PYTHON,
                persona = TutorPersona.BEGINNER_FRIENDLY,
                messages = initialMessages,
            )

            // Simulate external eviction
            fakeEngine.cleanup()
            assertThat(fakeEngine.isInitialized()).isFalse()

            // User sends message triggering lazy reload
            orchestrator.sendMessage("Explain decorators", initialMessages).toList()

            // Verify lazy reload used updated persona and topic
            assertThat(fakeEngine.initializeCalls).isEqualTo(2)
            val expectedPrompt =
                topicPromptProvider.buildSystemPrompt(
                    topic = CodingTopic.PYTHON,
                    persona = TutorPersona.BEGINNER_FRIENDLY,
                    developerProfile = fakeUserPreferencesStore.getDeveloperProfile(),
                )
            assertThat(fakeEngine.lastSystemInstruction).isEqualTo(expectedPrompt)
        }

    @Test
    fun `sendMessage restores history when previous inference was stopped or cancelled`() =
        runTest {
            val initialMessages =
                listOf(
                    ChatMessage.User("Prior question"),
                    ChatMessage.Agent("Prior answer"),
                )
            val model = testModel(id = "google/gemma-2-2b-it", localPath = "/models/gemma.task")
            val orchestrator = createOrchestrator()

            orchestrator.initialize(
                model = model,
                topic = CodingTopic.KOTLIN,
                sessionId = "session-1",
                existingMessages = initialMessages,
            )
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(1)

            // User taps "Stop" during generation
            orchestrator.stop()
            assertThat(fakeEngine.needsHistoryRestoration()).isTrue()

            // User sends follow-up message
            val followUpMessages = initialMessages + ChatMessage.User("Follow-up question")
            orchestrator.sendMessage("Follow-up question", followUpMessages).toList()

            // Verify history was restored to the newly created conversation before running inference
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(2)
            assertThat(fakeEngine.restoredMessages.last()).containsExactlyElementsIn(followUpMessages).inOrder()
            assertThat(fakeEngine.needsHistoryRestoration()).isFalse()
        }

    @Test
    fun `new session does not restore stale messages from previous session after cancellation`() =
        runTest {
            val session1Messages =
                listOf(
                    ChatMessage.User("Session 1 question"),
                    ChatMessage.Agent("Session 1 answer"),
                )
            val model = testModel(id = "google/gemma-2-2b-it", localPath = "/models/gemma.task")
            val orchestrator = createOrchestrator()

            // Session 1
            orchestrator.initialize(
                model = model,
                topic = CodingTopic.KOTLIN,
                sessionId = "session-1",
                existingMessages = session1Messages,
            )
            assertThat(fakeEngine.restoredMessages.last()).containsExactlyElementsIn(session1Messages)

            // Switch to new session with no existing messages
            orchestrator.initialize(
                model = model,
                topic = CodingTopic.KOTLIN,
                sessionId = "session-2",
                existingMessages = emptyList(),
            )

            // Stop triggered on session 2
            orchestrator.stop()

            // First message in session 2
            orchestrator.sendMessage("First question in session 2", emptyList()).toList()

            // Does not restore session 1 messages into session 2
            assertThat(fakeEngine.restoreHistoryCalls).isEqualTo(1)
            assertThat(fakeEngine.restoredMessages.last()).containsExactlyElementsIn(session1Messages)
        }
}
