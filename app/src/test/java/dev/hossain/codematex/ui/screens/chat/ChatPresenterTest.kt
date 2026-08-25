package dev.hossain.codematex.ui.screens.chat

import android.content.Context
import android.content.ContextWrapper
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.ChatInferenceEvent
import dev.hossain.codematex.data.FakeChatInferenceOrchestrator
import dev.hossain.codematex.data.FakeSystemStatsMonitor
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
}
