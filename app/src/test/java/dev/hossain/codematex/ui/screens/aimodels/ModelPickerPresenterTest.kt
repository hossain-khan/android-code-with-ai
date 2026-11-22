package dev.hossain.codematex.ui.screens.aimodels

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.repository.FakeModelConfigStore
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.runtime.FakeLlmEngine
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DeviceMemoryInfo
import dev.hossain.codematex.system.FakeDeviceMemoryProvider
import dev.hossain.codematex.system.ModelCompatibility
import dev.hossain.codematex.system.ModelCompatibilityChecker
import dev.hossain.codematex.ui.screens.debug.DebugScreen
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
        object : ModelCompatibilityChecker {
            override fun checkCompatibility(modelMinRamGb: Int): ModelCompatibility = ModelCompatibility.Compatible

            override fun getDeviceMemoryInfo(): DeviceMemoryInfo =
                DeviceMemoryInfo(totalBytes = 12_000_000_000L, displayTotalGb = 12.0, displayLabel = "GB")
        }

    private fun createPresenter(
        navigator: FakeNavigator = FakeNavigator(ModelPickerScreen),
        modelRepository: FakeModelRepository = FakeModelRepository(),
        modelConfigStore: FakeModelConfigStore = FakeModelConfigStore(),
        deviceMemoryProvider: FakeDeviceMemoryProvider = FakeDeviceMemoryProvider(),
        llmEngine: FakeLlmEngine = FakeLlmEngine(),
    ): ModelPickerPresenter =
        ModelPickerPresenter(
            navigator = navigator,
            screen = ModelPickerScreen,
            modelRepository = modelRepository,
            modelCompatibilityChecker = fakeCompatibilityChecker,
            modelConfigStore = modelConfigStore,
            deviceMemoryProvider = deviceMemoryProvider,
            llmEngine = llmEngine,
        )

    @Test
    fun `given models load - emits success with model list`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel, remoteModel))
            val presenter = createPresenter(modelRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(state.models).containsExactly(downloadedModel, remoteModel).inOrder()
                assertThat(state.modelCompatibility.values.all { it == ModelCompatibility.Compatible }).isTrue()
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
            val presenter = createPresenter(modelRepository = fakeRepo)

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
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter = createPresenter(navigator = navigator, modelRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.Back)

                navigator.awaitPop()
            }
        }

    @Test
    fun `given download event - delegates to repository`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(remoteModel))
            val presenter = createPresenter(modelRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.Download(remoteModel))

                assertThat(fakeRepo.downloadCalls).containsExactly(remoteModel)
            }
        }

    @Test
    fun `given cancel download event - delegates to repository`() =
        runTest {
            val downloadingModel = remoteModel.copy(downloadStatus = DownloadStatus.DOWNLOADING)
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadingModel))
            val presenter = createPresenter(modelRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.CancelDownload(downloadingModel))

                assertThat(fakeRepo.cancelDownloadCalls).containsExactly(downloadingModel)
            }
        }

    @Test
    fun `given delete event - delegates to repository and emits confirmation status`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val presenter = createPresenter(modelRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(state.statusMessage).isNull()

                state.eventSink(ModelPickerScreen.Event.Delete(downloadedModel))

                assertThat(fakeRepo.deleteCalls).containsExactly(downloadedModel)
                val updatedState = awaitItem() as ModelPickerScreen.State.Success
                assertThat(updatedState.statusMessage).contains(downloadedModel.displayName)
                assertThat(updatedState.statusMessage).contains("deleted")

                updatedState.eventSink(ModelPickerScreen.Event.ClearStatusMessage)
                val clearedState = awaitItem() as ModelPickerScreen.State.Success
                assertThat(clearedState.statusMessage).isNull()
            }
        }

    @Test
    fun `given unload model event when engine is loaded - cleans up engine and refreshes memory`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeEngine = FakeLlmEngine()
            fakeEngine.initialize(modelPath = "/models/gemma.litertlm", backend = LlmEngine.Backend.GPU)
            val fakeMemory = FakeDeviceMemoryProvider()
            val presenter =
                createPresenter(
                    modelRepository = fakeRepo,
                    deviceMemoryProvider = fakeMemory,
                    llmEngine = fakeEngine,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(initialState.isModelLoadedInMemory).isTrue()
                assertThat(initialState.activeBackend).isEqualTo(LlmEngine.Backend.GPU)
                assertThat(initialState.availableMemoryGb).isGreaterThan(0f)

                initialState.eventSink(ModelPickerScreen.Event.UnloadModel)

                val unloadedState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(fakeEngine.cleanupCalls).isEqualTo(1)
                assertThat(unloadedState.isModelLoadedInMemory).isFalse()
                assertThat(unloadedState.activeBackend).isNull()
                assertThat(unloadedState.statusMessage).isEqualTo("Model unloaded from memory")
            }
        }

    @Test
    fun `given unload model event when engine is not initialized - does nothing`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeEngine = FakeLlmEngine().apply { isInitializedValue = false }
            val presenter = createPresenter(modelRepository = fakeRepo, llmEngine = fakeEngine)

            presenter.test {
                val initialState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(initialState.isModelLoadedInMemory).isFalse()

                initialState.eventSink(ModelPickerScreen.Event.UnloadModel)

                assertThat(fakeEngine.cleanupCalls).isEqualTo(0)
            }
        }

    @Test
    fun `given unload model event when cleanup throws - emits error status and resets unloading flag`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeEngine =
                FakeLlmEngine().apply {
                    initialize(modelPath = "/models/gemma.litertlm", backend = LlmEngine.Backend.GPU)
                    cleanupThrows = IllegalStateException("Native cleanup failed")
                }
            val presenter = createPresenter(modelRepository = fakeRepo, llmEngine = fakeEngine)

            presenter.test {
                val initialState = expectMostRecentItem() as ModelPickerScreen.State.Success
                initialState.eventSink(ModelPickerScreen.Event.UnloadModel)

                val errorState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(errorState.isUnloadingModel).isFalse()
                assertThat(errorState.statusMessage).isEqualTo("Failed to unload model from memory")
            }
        }

    @Test
    fun `given delete event for currently loaded model - cleans up engine before deleting`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeEngine =
                FakeLlmEngine().apply {
                    initialize(
                        modelPath = downloadedModel.localPath!!,
                        backend = LlmEngine.Backend.GPU,
                    )
                }
            val presenter = createPresenter(modelRepository = fakeRepo, llmEngine = fakeEngine)

            presenter.test {
                val initialState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(initialState.isModelLoadedInMemory).isTrue()

                initialState.eventSink(ModelPickerScreen.Event.Delete(downloadedModel))

                val updatedState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(fakeEngine.cleanupCalls).isEqualTo(1)
                assertThat(fakeRepo.deleteCalls).containsExactly(downloadedModel)
                assertThat(updatedState.isModelLoadedInMemory).isFalse()
                assertThat(updatedState.activeBackend).isNull()
            }
        }

    @Test
    fun `given select event - selects model and pops navigator`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter = createPresenter(navigator = navigator, modelRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.Select(downloadedModel))

                assertThat(fakeRepo.getSelectedModel()).isEqualTo(downloadedModel)
                navigator.awaitPop()
            }
        }

    @Test
    fun `given open model config event - loads config and sets configured model`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val customConfig = ModelConfig(temperature = 0.5f, topK = 30, topP = 0.85f, maxTokens = 1024)
            val fakeConfigStore = FakeModelConfigStore()
            fakeConfigStore.setConfig(downloadedModel.id, customConfig)
            val presenter = createPresenter(modelRepository = fakeRepo, modelConfigStore = fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(state.configuredModel).isNull()
                assertThat(state.configuredModelConfig).isNull()

                state.eventSink(ModelPickerScreen.Event.OpenModelConfig(downloadedModel))

                val updatedState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(updatedState.configuredModel).isEqualTo(downloadedModel)
                assertThat(updatedState.configuredModelConfig).isEqualTo(customConfig)
            }
        }

    @Test
    fun `given dismiss model config event - clears configured model and config`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeConfigStore = FakeModelConfigStore()
            val presenter = createPresenter(modelRepository = fakeRepo, modelConfigStore = fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.OpenModelConfig(downloadedModel))

                val openState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(openState.configuredModel).isEqualTo(downloadedModel)

                openState.eventSink(ModelPickerScreen.Event.DismissModelConfig)

                val dismissedState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(dismissedState.configuredModel).isNull()
                assertThat(dismissedState.configuredModelConfig).isNull()
            }
        }

    @Test
    fun `given save model config event - saves config and dismisses sheet`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeConfigStore = FakeModelConfigStore()
            val newConfig = ModelConfig(temperature = 0.9f, topK = 50, topP = 0.95f, maxTokens = 4096)
            val presenter = createPresenter(modelRepository = fakeRepo, modelConfigStore = fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.OpenModelConfig(downloadedModel))

                val openState = expectMostRecentItem() as ModelPickerScreen.State.Success
                openState.eventSink(ModelPickerScreen.Event.SaveModelConfig(downloadedModel, newConfig))

                val savedState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(savedState.configuredModel).isNull()
                assertThat(savedState.configuredModelConfig).isNull()
                assertThat(fakeConfigStore.getConfig(downloadedModel.id)).isEqualTo(newConfig)
            }
        }

    @Test
    fun `given reset model config event - resets config and dismisses sheet`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val customConfig = ModelConfig(temperature = 0.3f, topK = 10, topP = 0.5f, maxTokens = 512)
            val fakeConfigStore = FakeModelConfigStore()
            fakeConfigStore.setConfig(downloadedModel.id, customConfig)
            val presenter = createPresenter(modelRepository = fakeRepo, modelConfigStore = fakeConfigStore)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.OpenModelConfig(downloadedModel))

                val openState = expectMostRecentItem() as ModelPickerScreen.State.Success
                openState.eventSink(ModelPickerScreen.Event.ResetModelConfig(downloadedModel))

                val resetState = expectMostRecentItem() as ModelPickerScreen.State.Success
                assertThat(resetState.configuredModel).isNull()
                assertThat(resetState.configuredModelConfig).isNull()
                assertThat(fakeConfigStore.getConfig(downloadedModel.id)).isEqualTo(ModelConfig())
            }
        }

    @Test
    fun `given open debug screen event - navigates to DebugScreen`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val navigator = FakeNavigator(ModelPickerScreen)
            val presenter = createPresenter(navigator = navigator, modelRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as ModelPickerScreen.State.Success
                state.eventSink(ModelPickerScreen.Event.OpenDebugScreen)

                assertThat(navigator.awaitNextScreen()).isEqualTo(DebugScreen)
            }
        }
}
