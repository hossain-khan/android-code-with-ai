package dev.hossain.codematex.ui.screens.home.model

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.runtime.FakeLlmEngine
import dev.hossain.codematex.runtime.LlmEngine
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ActiveModelBannerSubPresenterTest {
    private val downloadedModel =
        testModel(
            id = "google/gemma-2-2b-it",
            downloadStatus = DownloadStatus.DOWNLOADED,
            localPath = "/mock/path/gemma.bin",
        )

    @Test
    fun `emits downloaded model and in-memory backend when model is loaded`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeEngine =
                FakeLlmEngine().apply {
                    isInitializedValue = true
                    activeBackendValue = LlmEngine.Backend.GPU
                }

            val presenter =
                ActiveModelBannerSubPresenter(
                    modelRepository = fakeRepo,
                    llmEngine = fakeEngine,
                )

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.hasDownloadedModel).isTrue()
                assertThat(state.selectedModelName).isEqualTo(downloadedModel.displayName)
                assertThat(state.isModelInMemory).isTrue()
                assertThat(state.memoryBackend).isEqualTo("GPU")
            }
        }

    @Test
    fun `emits no model selected when repository has no models`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = emptyList())
            val fakeEngine =
                FakeLlmEngine().apply {
                    isInitializedValue = false
                    activeBackendValue = null
                }

            val presenter =
                ActiveModelBannerSubPresenter(
                    modelRepository = fakeRepo,
                    llmEngine = fakeEngine,
                )

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.hasDownloadedModel).isFalse()
                assertThat(state.selectedModelName).isNull()
                assertThat(state.isModelInMemory).isFalse()
                assertThat(state.memoryBackend).isNull()
            }
        }

    @Test
    fun `ManageModels event emits NavigateToModelPicker outer event`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeEngine = FakeLlmEngine()

            val presenter =
                ActiveModelBannerSubPresenter(
                    modelRepository = fakeRepo,
                    llmEngine = fakeEngine,
                )

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(ActiveModelBannerUiEvent.ManageModels)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(ActiveModelBannerOuterEvent.NavigateToModelPicker)
            }
        }
}
