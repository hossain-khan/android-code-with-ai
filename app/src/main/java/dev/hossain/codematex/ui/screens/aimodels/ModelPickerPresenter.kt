package dev.hossain.codematex.ui.screens.aimodels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.model.formattedSize
import dev.hossain.codematex.data.repository.ModelConfigStore
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DeviceMemoryProvider
import dev.hossain.codematex.system.ModelCompatibility
import dev.hossain.codematex.system.ModelCompatibilityChecker
import dev.hossain.codematex.ui.screens.debug.DebugScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class ModelPickerPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ModelPickerScreen,
    private val modelRepository: ModelRepository,
    private val modelCompatibilityChecker: ModelCompatibilityChecker,
    private val modelConfigStore: ModelConfigStore,
    private val deviceMemoryProvider: DeviceMemoryProvider,
    private val llmEngine: LlmEngine,
) : Presenter<ModelPickerScreen.State> {
    @Composable
    override fun present(): ModelPickerScreen.State {
        var models by rememberRetained { mutableStateOf<List<AiModel>>(emptyList()) }
        var isLoading by rememberRetained { mutableStateOf(true) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        var retryTrigger by rememberRetained { mutableIntStateOf(0) }
        var configuredModel by rememberRetained { mutableStateOf<AiModel?>(null) }
        var configuredModelConfig by rememberRetained { mutableStateOf<ModelConfig?>(null) }
        var statusMessage by rememberRetained { mutableStateOf<String?>(null) }
        var memoryStats by remember { mutableStateOf(deviceMemoryProvider.getMemoryStats()) }
        var isModelLoadedInMemory by remember { mutableStateOf(llmEngine.isInitialized()) }
        var activeBackend by remember { mutableStateOf(if (isModelLoadedInMemory) llmEngine.getActiveBackend() else null) }
        var isUnloadingModel by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            while (true) {
                memoryStats = deviceMemoryProvider.getMemoryStats()
                val loaded = llmEngine.isInitialized()
                isModelLoadedInMemory = loaded
                activeBackend = if (loaded) llmEngine.getActiveBackend() else null
                delay(2_000L)
            }
        }
        val scope = rememberCoroutineScope()

        LaunchedEffect(retryTrigger) {
            isLoading = true
            errorMessage = null
            modelRepository
                .getAvailableModels()
                .catch { e ->
                    if (e is CancellationException) throw e
                    Timber.e(e, "ModelPickerPresenter: Error loading available models")
                    errorMessage = e.message ?: "Failed to load models"
                    isLoading = false
                }.collect { list ->
                    models = list
                    isLoading = false
                }
        }

        val eventSink: (ModelPickerScreen.Event) -> Unit = { event ->
            when (event) {
                is ModelPickerScreen.Event.Back -> {
                    navigator.pop()
                }

                is ModelPickerScreen.Event.Retry -> {
                    retryTrigger++
                }

                is ModelPickerScreen.Event.Download -> {
                    scope.launch {
                        modelRepository.downloadModel(event.model)
                    }
                }

                is ModelPickerScreen.Event.CancelDownload -> {
                    scope.launch {
                        modelRepository.cancelDownload(event.model)
                    }
                }

                is ModelPickerScreen.Event.Delete -> {
                    scope.launch {
                        try {
                            val localPath = event.model.localPath
                            if (localPath != null && llmEngine.isModelLoaded(localPath)) {
                                llmEngine.cleanup()
                                isModelLoadedInMemory = llmEngine.isInitialized()
                                activeBackend = if (isModelLoadedInMemory) llmEngine.getActiveBackend() else null
                            }
                            modelRepository.deleteModel(event.model)
                            memoryStats = deviceMemoryProvider.getMemoryStats()
                            statusMessage = "${event.model.displayName} deleted (${event.model.formattedSize} freed)"
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.e(e, "ModelPickerPresenter: Error deleting model ${event.model.id}")
                            statusMessage = "Failed to delete ${event.model.displayName}"
                        }
                    }
                }

                is ModelPickerScreen.Event.Select -> {
                    scope.launch {
                        modelRepository.selectModel(event.model)
                        navigator.pop()
                    }
                }

                is ModelPickerScreen.Event.OpenModelConfig -> {
                    configuredModel = event.model
                    scope.launch {
                        configuredModelConfig = modelConfigStore.getConfig(event.model.id)
                    }
                }

                is ModelPickerScreen.Event.DismissModelConfig -> {
                    configuredModel = null
                    configuredModelConfig = null
                }

                is ModelPickerScreen.Event.SaveModelConfig -> {
                    scope.launch {
                        modelConfigStore.setConfig(event.model.id, event.config)
                    }
                    configuredModel = null
                    configuredModelConfig = null
                }

                is ModelPickerScreen.Event.ResetModelConfig -> {
                    scope.launch {
                        modelConfigStore.resetConfig(event.model.id)
                    }
                    configuredModel = null
                    configuredModelConfig = null
                }

                ModelPickerScreen.Event.OpenDebugScreen -> {
                    navigator.goTo(DebugScreen)
                }

                ModelPickerScreen.Event.UnloadModel -> {
                    if (!isUnloadingModel && llmEngine.isInitialized()) {
                        isUnloadingModel = true
                        scope.launch {
                            try {
                                llmEngine.cleanup()
                                isModelLoadedInMemory = llmEngine.isInitialized()
                                activeBackend = if (isModelLoadedInMemory) llmEngine.getActiveBackend() else null
                                memoryStats = deviceMemoryProvider.getMemoryStats()
                                statusMessage = "Model unloaded from memory"
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.e(e, "ModelPickerPresenter: Error unloading model from memory")
                                statusMessage = "Failed to unload model from memory"
                            } finally {
                                isUnloadingModel = false
                            }
                        }
                    }
                }

                ModelPickerScreen.Event.ClearStatusMessage -> {
                    statusMessage = null
                }
            }
        }

        return when {
            isLoading -> {
                ModelPickerScreen.State.Loading
            }

            errorMessage != null -> {
                ModelPickerScreen.State.Error(errorMessage!!, eventSink)
            }

            else -> {
                ModelPickerScreen.State.Success(
                    models = models,
                    deviceMemoryInfo = modelCompatibilityChecker.getDeviceMemoryInfo(),
                    modelCompatibility =
                        models.associate { model ->
                            model.id to modelCompatibilityChecker.checkCompatibility(model.minDeviceMemoryInGb)
                        },
                    availableMemoryGb = memoryStats.availGb,
                    isModelLoadedInMemory = isModelLoadedInMemory,
                    activeBackend = activeBackend,
                    isUnloadingModel = isUnloadingModel,
                    configuredModel = configuredModel,
                    configuredModelConfig = configuredModelConfig,
                    statusMessage = statusMessage,
                    eventSink = eventSink,
                )
            }
        }
    }

    @CircuitInject(ModelPickerScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: ModelPickerScreen,
        ): ModelPickerPresenter
    }
}
