package dev.hossain.codematex.ui.screens.aimodels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.repository.ModelConfigStore
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.system.ModelCompatibility
import dev.hossain.codematex.system.ModelCompatibilityChecker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
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
) : Presenter<ModelPickerScreen.State> {
    @Composable
    override fun present(): ModelPickerScreen.State {
        var models by rememberRetained { mutableStateOf<List<AiModel>>(emptyList()) }
        var isLoading by rememberRetained { mutableStateOf(true) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        var retryTrigger by rememberRetained { mutableIntStateOf(0) }
        var configuredModel by rememberRetained { mutableStateOf<AiModel?>(null) }
        var configuredModelConfig by rememberRetained { mutableStateOf<ModelConfig?>(null) }
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
                        modelRepository.deleteModel(event.model)
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
                    navigator.goTo(dev.hossain.codematex.ui.screens.debug.DebugScreen)
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
                    configuredModel = configuredModel,
                    configuredModelConfig = configuredModelConfig,
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
