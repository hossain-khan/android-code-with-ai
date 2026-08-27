package dev.hossain.codematex.ui.screens.aimodels

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.repository.FakeModelConfigStore
import dev.hossain.codematex.data.repository.FakeModelDownloadPreferences
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.system.DeviceMemoryInfo
import dev.hossain.codematex.system.ModelCompatibility
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [ModelPickerPresenter].
 */
class ModelPickerPresenterTest {
    private val downloadedModel =
        testModel(id = "google/gemma-2-2b-it", downloadStatus = DownloadStatus.DOWNLOADED)
    private val remoteModel =
        testModel(id = "google/gemma-4-e2b", downloadStatus = DownloadStatus.NOT_DOWNLOADED, localPath = null)

    private val fakeCompatibilityChecker =
        object : dev.hossain.codematex.system.ModelCompatibilityChecker {
            override fun checkCompatibility(modelMinRamGb: Int): ModelCompatibility = ModelCompatibility.Compatible

            override fun getDeviceMemoryInfo(): DeviceMemoryInfo =
                DeviceMemoryInfo(totalBytes = 12_000_000_000L, displayTotalGb = 12.0, displayLabel = "GB")
        }

    @Test
    fun `given models load - emits success with model list and wifi only state`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel, remoteModel))
            val fakePrefs = FakeModelDownloadPreferences(downloadOverWifiOnly = true)
            val fakeConfigStore = FakeModelConfigStore()
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(state.models).containsExactly(downloadedModel, remoteModel).inOrder()
                assertThat(state.downloadOverWifiOnly).isTrue()
                assertThat(state.modelCompatibility.values.all { it == ModelCompatibility.Compatible }).isTrue()
            }
        }

    @Test
    fun `given toggle wifi only event - updates preferences and state`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakePrefs = FakeModelDownloadPreferences(downloadOverWifiOnly = true)
            val fakeConfigStore = FakeModelConfigStore()
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(state.downloadOverWifiOnly).isTrue()

                state.eventSink(ModelPickerScreen.Event.ToggleWifiOnly(false))

                val updatedState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(updatedState.downloadOverWifiOnly).isFalse()
                assertThat(fakePrefs.getDownloadOverWifiOnly()).isFalse()
            }
        }

    @Test
    fun `given model load fails - emits error state and retry succeeds`() =
        runTest {
            val fakeRepo =
                FakeModelRepository(
                    availableModels = listOf(downloadedModel),
                    getException = IllegalStateException("Storage error"),
                )
            val fakePrefs = FakeModelDownloadPreferences()
            val fakeConfigStore = FakeModelConfigStore()
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val errorState = expectMostRecentItem() as ModelPickerScreen.State.Error
                assertThat(errorState.message).isEqualTo("Storage error")

                // Clear exception and retry
                fakeRepo.getException = null
                errorState.eventSink(ModelPickerScreen.Event.Retry)

                val successState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(successState.models).containsExactly(downloadedModel)
            }
        }

    @Test
    fun `given back event - pops navigator`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakePrefs = FakeModelDownloadPreferences()
            val fakeConfigStore = FakeModelConfigStore()
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.Back)
                navigator.awaitPop()
            }
        }

    @Test
    fun `given download event - starts model download`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(remoteModel))
            val fakePrefs = FakeModelDownloadPreferences()
            val fakeConfigStore = FakeModelConfigStore()
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.Download(remoteModel))
                assertThat(fakeRepo.downloadCalls).containsExactly(remoteModel)
            }
        }

    @Test
    fun `given cancel download event - cancels model download`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(remoteModel))
            val fakePrefs = FakeModelDownloadPreferences()
            val fakeConfigStore = FakeModelConfigStore()
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.CancelDownload(remoteModel))
                assertThat(fakeRepo.cancelDownloadCalls).containsExactly(remoteModel)
            }
        }

    @Test
    fun `given delete event - deletes model`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakePrefs = FakeModelDownloadPreferences()
            val fakeConfigStore = FakeModelConfigStore()
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.Delete(downloadedModel))
                assertThat(fakeRepo.deleteCalls).containsExactly(downloadedModel)
            }
        }

    @Test
    fun `given select event - selects model and pops navigator`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakePrefs = FakeModelDownloadPreferences()
            val fakeConfigStore = FakeModelConfigStore()
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.Select(downloadedModel))
                navigator.awaitPop()
                assertThat(fakeRepo.getSelectedModel()).isEqualTo(downloadedModel)
            }
        }

    @Test
    fun `given open model config event - sets configured model and loads config`() =
        runTest {
            val customConfig = ModelConfig(temperature = 1.2f, topK = 60, topP = 0.9f, maxTokens = 4096)
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakePrefs = FakeModelDownloadPreferences()
            val fakeConfigStore = FakeModelConfigStore()
            fakeConfigStore.setConfig(downloadedModel.id, customConfig)
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(state.configuredModel).isNull()

                state.eventSink(ModelPickerScreen.Event.OpenModelConfig(downloadedModel))

                val updatedState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(updatedState.configuredModel).isEqualTo(downloadedModel)
                assertThat(updatedState.configuredModelConfig).isEqualTo(customConfig)
            }
        }

    @Test
    fun `given save model config event - persists config to store and dismisses sheet`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakePrefs = FakeModelDownloadPreferences()
            val fakeConfigStore = FakeModelConfigStore()
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.OpenModelConfig(downloadedModel))
                expectMostRecentItem()

                val newConfig = ModelConfig(temperature = 0.3f, topK = 15, topP = 0.8f, maxTokens = 1024)
                state.eventSink(ModelPickerScreen.Event.SaveModelConfig(downloadedModel, newConfig))

                val updatedState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(updatedState.configuredModel).isNull()
                assertThat(fakeConfigStore.getConfig(downloadedModel.id)).isEqualTo(newConfig)
            }
        }

    @Test
    fun `given reset model config event - resets config in store and dismisses sheet`() =
        runTest {
            val customConfig = ModelConfig(temperature = 1.8f, topK = 90, topP = 0.5f, maxTokens = 512)
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakePrefs = FakeModelDownloadPreferences()
            val fakeConfigStore = FakeModelConfigStore()
            fakeConfigStore.setConfig(downloadedModel.id, customConfig)
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.OpenModelConfig(downloadedModel))
                expectMostRecentItem()

                state.eventSink(ModelPickerScreen.Event.ResetModelConfig(downloadedModel))

                val updatedState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(updatedState.configuredModel).isNull()
                assertThat(fakeConfigStore.getConfig(downloadedModel.id)).isEqualTo(ModelConfig())
            }
        }

    @Test
    fun `given dismiss model config event - clears configured model state`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakePrefs = FakeModelDownloadPreferences()
            val fakeConfigStore = FakeModelConfigStore()
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter =
                ModelPickerPresenter(navigator, ModelPickerScreen, fakeRepo, fakePrefs, fakeCompatibilityChecker, fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.OpenModelConfig(downloadedModel))
                expectMostRecentItem()

                state.eventSink(ModelPickerScreen.Event.DismissModelConfig)

                val updatedState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(updatedState.configuredModel).isNull()
                assertThat(updatedState.configuredModelConfig).isNull()
            }
        }
}
