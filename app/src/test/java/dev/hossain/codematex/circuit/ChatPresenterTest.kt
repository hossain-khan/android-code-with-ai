package dev.hossain.codematex.circuit

import android.content.Context
import android.content.ContextWrapper
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.circuit.overlay.ModelConfigStore
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DownloadStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPresenterTest {
    private val fakeContext: Context = ContextWrapper(null)

    private val configStore = ModelConfigStore(fakeContext)
    private val fakeEngine = FakeLlmEngine()
    private val fakeSessionRepo = FakeChatSessionRepository()

    @Test
    fun noModelSelected_whenNoDownloadedModels_returnsNoModelSelectedWithFalse() =
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
                    llmEngine = fakeEngine,
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    context = fakeContext,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                assertFalse(state.hasDownloadedModels)
                assertEquals(CodingTopic.KOTLIN, state.topic)
            }
        }

    @Test
    fun noModelSelected_whenModelsDownloaded_returnsNoModelSelectedWithTrue() =
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
                    llmEngine = fakeEngine,
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    context = fakeContext,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                assertTrue(state.hasDownloadedModels)
                assertEquals(CodingTopic.ANDROID, state.topic)
            }
        }

    @Test
    fun openModelPicker_navigatesToModelPickerScreen() =
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
                    llmEngine = fakeEngine,
                    modelRepository = fakeModelRepo,
                    sessionRepository = fakeSessionRepo,
                    configStore = configStore,
                    context = fakeContext,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChatScreen.State.NoModelSelected
                state.eventSink(ChatScreen.Event.OpenModelPicker)
                assertEquals(ModelPickerScreen, navigator.awaitNextScreen())
            }
        }
}
