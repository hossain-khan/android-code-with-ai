package dev.hossain.codematex.circuit

import android.content.Context
import android.content.ContextWrapper
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.circuit.overlay.ModelConfigStore
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.system.FakeDeviceMemoryProvider
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
    private val fakeTopicPromptProvider = FakeTopicPromptProvider()

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
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                    topicPromptProvider = fakeTopicPromptProvider,
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
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                    topicPromptProvider = fakeTopicPromptProvider,
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
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                    topicPromptProvider = fakeTopicPromptProvider,
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
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                    topicPromptProvider = fakeTopicPromptProvider,
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
    fun `given select persona event - updates active persona and resets engine`() =
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
                    chatInferenceOrchestrator = fakeChatInferenceOrchestrator,
                    systemStatsMonitor = fakeSystemStatsMonitor,
                    topicPromptProvider = fakeTopicPromptProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.Active
                state.eventSink(ChatScreen.Event.SelectPersona(TutorPersona.BEGINNER_FRIENDLY))

                val updatedState = expectMostRecentItem() as ChatScreen.State.Active
                assertEquals(TutorPersona.BEGINNER_FRIENDLY, updatedState.persona)
                assertTrue(fakeChatInferenceOrchestrator.resetConversationPersonas.contains(TutorPersona.BEGINNER_FRIENDLY))
            }
        }
}
