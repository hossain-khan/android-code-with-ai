package dev.hossain.codematex.ui.screens.chat

import android.content.Context
import android.content.ContextWrapper
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.ChatInferenceEvent
import dev.hossain.codematex.data.FakeChatInferenceOrchestrator
import dev.hossain.codematex.data.FakeSystemStatsMonitor
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.data.repository.FakeChatSessionRepository
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.data.repository.FakeUserPreferencesStore
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.system.SystemResourceStats
import dev.hossain.codematex.ui.overlay.ModelConfigStore
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ChatPresenter].
 */
class ChatPresenterTest {
    private val fakeContext: Context = ContextWrapper(null)

    private val configStore = ModelConfigStore(fakeContext)
    private val fakeSessionRepo = FakeChatSessionRepository()
    private val fakeChatInferenceOrchestrator = FakeChatInferenceOrchestrator()
    private val fakeSystemStatsMonitor = FakeSystemStatsMonitor()
    private val fakeUserPreferencesStore = FakeUserPreferencesStore()

    @Test
    fun `given no downloaded models - emits no model selected with false`() =
        runTest {
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(testModel(downloadStatus = DownloadStatus.NOT_DOWNLOADED)),
                    selectedModel = null,
                )
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    userPreferencesStore = fakeUserPreferencesStore,
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                assertFalse(state.hasDownloadedModels)
                assertEquals(CodingTopic.KOTLIN, state.topic)
            }
        }

    @Test
    fun `given models downloaded - emits no model selected with true`() =
        runTest {
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(testModel(downloadStatus = DownloadStatus.DOWNLOADED)),
                    selectedModel = null,
                )
            val navigator = FakeNavigator(ChatScreen(CodingTopic.ANDROID))
            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.ANDROID),
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    userPreferencesStore = fakeUserPreferencesStore,
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                assertTrue(state.hasDownloadedModels)
                assertEquals(CodingTopic.ANDROID, state.topic)
            }
        }

    @Test
    fun `given open model picker event - navigates to model picker screen`() =
        runTest {
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = emptyList(),
                    selectedModel = null,
                )
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    userPreferencesStore = fakeUserPreferencesStore,
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                state.eventSink(ChatScreen.Event.OpenModelPicker)
                assertEquals(ModelPickerScreen, navigator.awaitNextScreen())
            }
        }

    @Test
    fun `given model is selected - emits active state with model details and default persona`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    userPreferencesStore = fakeUserPreferencesStore,
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                assertFalse(state.isPreparing)
                assertFalse(state.isGenerating)
                assertEquals(CodingTopic.KOTLIN, state.topic)
                assertEquals(TutorPersona.SENIOR_ENGINEER, state.persona)
            }
        }

    @Test
    fun `given stored persona in user preferences - initializes active state with stored persona`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val customPreferencesStore = FakeUserPreferencesStore(initialSelectedPersona = TutorPersona.INTERVIEW_COACH)
            val navigator = FakeNavigator(ChatScreen(CodingTopic.PYTHON))
            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.PYTHON),
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    userPreferencesStore = customPreferencesStore,
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                assertEquals(CodingTopic.PYTHON, state.topic)
                assertEquals(TutorPersona.INTERVIEW_COACH, state.persona)
            }
        }

    @Test
    fun `given select persona event - updates active persona, resets engine, and persists to user preferences`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val preferencesStore = FakeUserPreferencesStore(initialSelectedPersona = TutorPersona.SENIOR_ENGINEER)
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    userPreferencesStore = preferencesStore,
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                state.eventSink(ChatScreen.Event.SelectPersona(TutorPersona.BEGINNER_FRIENDLY))

                val updatedState = expectMostRecentItem() as ChatScreen.State.Active
                assertEquals(TutorPersona.BEGINNER_FRIENDLY, updatedState.persona)
                assertEquals(TutorPersona.BEGINNER_FRIENDLY, preferencesStore.selectedPersona)
                assertTrue(fakeChatInferenceOrchestrator.resetConversationPersonas.contains(TutorPersona.BEGINNER_FRIENDLY))
            }
        }

    @Test
    fun `given system stats monitor emits metrics - emits active state with systemResourceStats`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val customStatsMonitor = FakeSystemStatsMonitor()
            customStatsMonitor.resourceStatsToEmit =
                listOf(
                    SystemResourceStats(
                        cpuPercent = 45f,
                        ramUsedGb = 3.5f,
                        ramTotalGb = 8.0f,
                    ),
                )

            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.messageEvents = listOf(ChatInferenceEvent.Token("Response"))
            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))

            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    userPreferencesStore = fakeUserPreferencesStore,
                    chatInferenceOrchestrator = fakeOrchestrator,
                    systemStatsMonitor = customStatsMonitor,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                state.eventSink(ChatScreen.Event.SendMessage("Explain ViewModel"))

                val generatingState = expectMostRecentItem() as ChatScreen.State.Active
                assertEquals(45f, generatingState.systemResourceStats?.cpuPercent ?: 0f, 0.01f)
                assertEquals(3.5f, generatingState.systemResourceStats?.ramUsedGb ?: 0f, 0.01f)
                assertEquals(8.0f, generatingState.systemResourceStats?.ramTotalGb ?: 0f, 0.01f)
            }
        }

    @Test
    fun `given resumed session - reset clears messages and sessionId and next message creates new session`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val resumedSessionId = "resumed-session-123"
            val existingMessages = listOf(ChatMessage.User("Prior question"), ChatMessage.Agent("Prior answer"))
            val sessionRepo = FakeChatSessionRepository(messages = existingMessages)

            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.initializeResult = Result.success(existingMessages)
            fakeOrchestrator.messageEvents = listOf(ChatInferenceEvent.Token("Fresh answer"), ChatInferenceEvent.Done)

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN, sessionId = resumedSessionId))
            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN, sessionId = resumedSessionId),
                    modelRepository = fakeModelRepo,
                    sessionRepository = sessionRepo,
                    configStore = configStore,
                    userPreferencesStore = fakeUserPreferencesStore,
                    chatInferenceOrchestrator = fakeOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                assertEquals(2, initialState.messages.size)

                // Reset session
                initialState.eventSink(ChatScreen.Event.ResetSession)
                val resetState = expectMostRecentItem() as ChatScreen.State.Active
                assertTrue(resetState.messages.isEmpty())

                // Next message should create a new conversation (passing sessionId = null to saveSession)
                resetState.eventSink(ChatScreen.Event.SendMessage("New turn"))
                expectMostRecentItem() // generation & save completes

                val lastSaved = sessionRepo.savedSessions.lastOrNull()
                assertNotNull(lastSaved)
                assertNull("SessionId must be null for new conversation after reset", lastSaved?.third)
            }
        }

    @Test
    fun `given persona switch with existing messages - calls switchPersona with history and updates persona`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val existingMessages = listOf(ChatMessage.User("Existing question"), ChatMessage.Agent("Existing answer"))
            val sessionRepo = FakeChatSessionRepository(messages = existingMessages)

            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.initializeResult = Result.success(existingMessages)

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = sessionRepo,
                    configStore = configStore,
                    userPreferencesStore = fakeUserPreferencesStore,
                    chatInferenceOrchestrator = fakeOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                initialState.eventSink(ChatScreen.Event.SelectPersona(TutorPersona.INTERVIEW_COACH))

                val updatedState = expectMostRecentItem() as ChatScreen.State.Active
                assertEquals(TutorPersona.INTERVIEW_COACH, updatedState.persona)

                val switchCall = fakeOrchestrator.switchPersonaCalls.lastOrNull()
                assertNotNull(switchCall)
                assertEquals(TutorPersona.INTERVIEW_COACH, switchCall?.persona)
                assertTrue(switchCall?.messages?.any { it is ChatMessage.System } == true)
            }
        }

    @Test
    fun `given persistence failure after inference done - preserves assistant response and exposes saveErrorMessage`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val sessionRepo = FakeChatSessionRepository()
            sessionRepo.saveException = java.io.IOException("Disk full")

            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.messageEvents = listOf(ChatInferenceEvent.Token("Generated code answer"), ChatInferenceEvent.Done)

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = sessionRepo,
                    configStore = configStore,
                    userPreferencesStore = fakeUserPreferencesStore,
                    chatInferenceOrchestrator = fakeOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                initialState.eventSink(ChatScreen.Event.SendMessage("Write quicksort"))

                val postDoneState = expectMostRecentItem() as ChatScreen.State.Active
                assertFalse(postDoneState.isGenerating)
                assertEquals("Disk full", postDoneState.saveErrorMessage)

                // Verify the generated assistant response was NOT deleted or replaced with an Error message
                val lastMessage = postDoneState.messages.lastOrNull()
                assertTrue("Expected Agent message but was $lastMessage", lastMessage is ChatMessage.Agent)
                assertEquals("Generated code answer", (lastMessage as ChatMessage.Agent).content)
                assertFalse(lastMessage.isStreaming)
            }
        }

    @Test
    fun `given save error - retry save persists conversation and clears error`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val sessionRepo = FakeChatSessionRepository()
            sessionRepo.saveException = java.io.IOException("Temporary DB lock")

            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.messageEvents = listOf(ChatInferenceEvent.Token("Response"), ChatInferenceEvent.Done)

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = sessionRepo,
                    configStore = configStore,
                    userPreferencesStore = fakeUserPreferencesStore,
                    chatInferenceOrchestrator = fakeOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                initialState.eventSink(ChatScreen.Event.SendMessage("Hello"))

                val failedState = expectMostRecentItem() as ChatScreen.State.Active
                assertEquals("Temporary DB lock", failedState.saveErrorMessage)

                // Database lock clears
                sessionRepo.saveException = null

                failedState.eventSink(ChatScreen.Event.RetrySave)
                val recoveredState = expectMostRecentItem() as ChatScreen.State.Active
                assertNull(recoveredState.saveErrorMessage)
                assertEquals(1, sessionRepo.savedSessions.size)
            }
        }

    @Test
    fun `given active generation - stop generation transitions streaming message to terminal state`() =
        runTest {
            val model = testModel(id = "litert-community/gemma-4-E2B-it-litert-lm", downloadStatus = DownloadStatus.DOWNLOADED)
            val fakeModelRepo =
                FakeModelRepository(
                    availableModels = listOf(model),
                    selectedModel = model,
                )
            val fakeOrchestrator = FakeChatInferenceOrchestrator()
            fakeOrchestrator.messageEvents = listOf(ChatInferenceEvent.Token("Partial output"))

            val navigator = FakeNavigator(ChatScreen(CodingTopic.KOTLIN))
            val presenter =
                ChatPresenter(
                    navigator = navigator,
                    screen = ChatScreen(CodingTopic.KOTLIN),
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    userPreferencesStore = fakeUserPreferencesStore,
                    chatInferenceOrchestrator = fakeOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ChatScreen.State.Active
                initialState.eventSink(ChatScreen.Event.SendMessage("Long question"))

                val streamingState = expectMostRecentItem() as ChatScreen.State.Active
                assertTrue(streamingState.isGenerating)

                streamingState.eventSink(ChatScreen.Event.StopGeneration)
                val stoppedState = expectMostRecentItem() as ChatScreen.State.Active
                assertFalse(stoppedState.isGenerating)
                val lastMessage = stoppedState.messages.lastOrNull() as? ChatMessage.Agent
                assertNotNull(lastMessage)
                assertFalse(lastMessage?.isStreaming == true)
                assertEquals(1, fakeOrchestrator.stopCalls)
            }
        }
}
