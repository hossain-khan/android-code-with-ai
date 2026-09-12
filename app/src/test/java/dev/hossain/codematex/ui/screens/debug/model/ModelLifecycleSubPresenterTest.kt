package dev.hossain.codematex.ui.screens.debug.model

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.repository.FakeModelConfigStore
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.runtime.FakeLlmEngine
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.ui.screens.debug.FakeDebugMemoryProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ModelLifecycleSubPresenterTest {
    private val downloadedModel =
        testModel(
            id = "google/gemma-2-2b-it",
            downloadStatus = DownloadStatus.DOWNLOADED,
            localPath = "/mock/path/gemma.bin",
        )

    private val notDownloadedModel =
        testModel(
            id = "google/gemma-4-e2b",
            downloadStatus = DownloadStatus.NOT_DOWNLOADED,
            localPath = null,
        )

    @Test
    fun `presenter selects downloaded model by default and exposes models list`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(notDownloadedModel, downloadedModel))
            val fakeEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()

            val presenter =
                ModelLifecycleSubPresenter(
                    modelRepository = fakeRepo,
                    llmEngine = fakeEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.models).hasSize(2)
                assertThat(state.selectedModel).isEqualTo(downloadedModel)
                assertThat(state.isModelLoaded).isFalse()
            }
        }

    @Test
    fun `LoadModel initializes engine and updates load delta and emits ModelLoaded outer event`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()

            val presenter =
                ModelLifecycleSubPresenter(
                    modelRepository = fakeRepo,
                    llmEngine = fakeEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(ModelLifecycleUiEvent.LoadModel)

                val loadedState = expectMostRecentItem()
                assertThat(loadedState.isModelLoaded).isTrue()
                assertThat(loadedState.loadedModelName).isEqualTo(downloadedModel.name)
                assertThat(loadedState.lastLoadDelta).isNotNull()

                val outerEvent1 = outerEvents.awaitEvent()
                assertThat(outerEvent1).isInstanceOf(ModelLifecycleOuterEvent.ShowSnackbar::class.java)

                val outerEvent2 = outerEvents.awaitEvent()
                assertThat(outerEvent2).isInstanceOf(ModelLifecycleOuterEvent.ModelLoaded::class.java)
                assertThat((outerEvent2 as ModelLifecycleOuterEvent.ModelLoaded).modelName).isEqualTo(downloadedModel.name)
            }
        }

    @Test
    fun `UnloadModel cleans up engine and updates unload delta and emits ModelUnloaded outer event`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()

            val presenter =
                ModelLifecycleSubPresenter(
                    modelRepository = fakeRepo,
                    llmEngine = fakeEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(ModelLifecycleUiEvent.LoadModel)

                val loadedState = expectMostRecentItem()
                assertThat(loadedState.isModelLoaded).isTrue()

                // Clear the 3 load events (Initializing snackbar, ModelLoaded, Loaded snackbar)
                outerEvents.awaitEvent()
                outerEvents.awaitEvent()
                outerEvents.awaitEvent()

                loadedState.eventSink(ModelLifecycleUiEvent.UnloadModel)

                val unloadedState = expectMostRecentItem()
                assertThat(unloadedState.isModelLoaded).isFalse()
                assertThat(unloadedState.loadedModelName).isNull()
                assertThat(unloadedState.lastUnloadDelta).isNotNull()
                assertThat(fakeEngine.cleanupCalls).isEqualTo(1)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(ModelLifecycleOuterEvent.ModelUnloaded)
            }
        }

    @Test
    fun `DeleteModel deletes model from repository and emits snackbar`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()

            val presenter =
                ModelLifecycleSubPresenter(
                    modelRepository = fakeRepo,
                    llmEngine = fakeEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(ModelLifecycleUiEvent.DeleteModel(downloadedModel))

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isInstanceOf(ModelLifecycleOuterEvent.ShowSnackbar::class.java)
                assertThat((outerEvent as ModelLifecycleOuterEvent.ShowSnackbar).message).contains("Deleted weights")
            }
        }
}
