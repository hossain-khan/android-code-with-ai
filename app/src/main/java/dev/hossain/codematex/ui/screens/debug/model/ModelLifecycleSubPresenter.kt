package dev.hossain.codematex.ui.screens.debug.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.repository.ModelConfigStore
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DebugMemoryProvider
import dev.hossain.codematex.system.MemoryDelta
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

class ModelLifecycleSubPresenter(
    private val modelRepository: ModelRepository,
    private val llmEngine: LlmEngine,
    private val configStore: ModelConfigStore,
    private val debugMemoryProvider: DebugMemoryProvider,
) : SubPresenter<ModelLifecycleOuterEvent, ModelLifecycleSubState> {
    @Composable
    override fun present(outerEventSink: (ModelLifecycleOuterEvent) -> Unit): ModelLifecycleSubState {
        val scope = rememberCoroutineScope()

        var models by rememberRetained { mutableStateOf<List<AiModel>>(emptyList()) }
        var selectedModel by rememberRetained { mutableStateOf<AiModel?>(null) }
        var selectedBackend by rememberRetained { mutableStateOf(LlmEngine.Backend.GPU) }
        var isModelLoaded by rememberRetained { mutableStateOf(false) }
        var loadedModelName by rememberRetained { mutableStateOf<String?>(null) }
        var activeBackend by rememberRetained { mutableStateOf(llmEngine.getActiveBackend()) }
        var isLoadingModel by rememberRetained { mutableStateOf(false) }
        var isUnloadingModel by rememberRetained { mutableStateOf(false) }
        var lastLoadDelta by rememberRetained { mutableStateOf<MemoryDelta?>(null) }
        var lastUnloadDelta by rememberRetained { mutableStateOf<MemoryDelta?>(null) }

        LaunchedEffect(Unit) {
            modelRepository.getAvailableModels().collect { list ->
                models = list
                if (selectedModel == null) {
                    selectedModel = list.firstOrNull { it.downloadStatus == DownloadStatus.DOWNLOADED } ?: list.firstOrNull()
                }
            }
        }

        return ModelLifecycleSubState(
            models = models,
            selectedModel = selectedModel,
            selectedBackend = selectedBackend,
            isModelLoaded = isModelLoaded,
            loadedModelName = loadedModelName,
            activeBackend = activeBackend,
            isLoadingModel = isLoadingModel,
            isUnloadingModel = isUnloadingModel,
            lastLoadDelta = lastLoadDelta,
            lastUnloadDelta = lastUnloadDelta,
            eventSink = { event ->
                when (event) {
                    is ModelLifecycleUiEvent.SelectModel -> {
                        selectedModel = event.model
                    }

                    is ModelLifecycleUiEvent.SelectBackend -> {
                        selectedBackend = event.backend
                    }

                    ModelLifecycleUiEvent.LoadModel -> {
                        val model = selectedModel
                        if (model == null) {
                            outerEventSink(ModelLifecycleOuterEvent.ShowSnackbar("No model selected."))
                            return@ModelLifecycleSubState
                        }
                        if (model.downloadStatus != DownloadStatus.DOWNLOADED && model.localPath == null) {
                            outerEventSink(ModelLifecycleOuterEvent.ShowSnackbar("Model weights are not downloaded on device."))
                            return@ModelLifecycleSubState
                        }

                        scope.launch {
                            isLoadingModel = true
                            outerEventSink(
                                ModelLifecycleOuterEvent.ShowSnackbar("Initializing ${model.name} on ${selectedBackend.name}..."),
                            )
                            val beforeSnap = debugMemoryProvider.captureSnapshot()

                            try {
                                val config = configStore.getConfig(model.id)
                                llmEngine.initialize(
                                    modelPath = model.localPath ?: "",
                                    backend = selectedBackend,
                                    systemInstruction = "You are a helpful coding assistant.",
                                    config = config,
                                )
                                val afterSnap = debugMemoryProvider.captureSnapshot()
                                val delta = afterSnap.diffFrom(beforeSnap)
                                lastLoadDelta = delta
                                isModelLoaded = true
                                loadedModelName = model.name
                                val backend = llmEngine.getActiveBackend() ?: selectedBackend
                                activeBackend = backend
                                outerEventSink(ModelLifecycleOuterEvent.ModelLoaded(model.name, backend))
                                outerEventSink(
                                    ModelLifecycleOuterEvent.ShowSnackbar(
                                        "Loaded in ${delta.durationMs}ms on ${backend.name}. " +
                                            "Native Δ: ${"%.1f".format(
                                                delta.deltaNativeMb,
                                            )} MB, RAM Δ: ${"%.1f".format(delta.deltaSystemMb)} MB",
                                    ),
                                )
                            } catch (e: Exception) {
                                Timber.e(
                                    e,
                                    "ModelLifecycleSubPresenter [LOAD_ERROR]: Failed initializing %s on %s",
                                    model.name,
                                    selectedBackend.name,
                                )
                                outerEventSink(ModelLifecycleOuterEvent.ShowSnackbar("Load failed: ${e.message}"))
                            } finally {
                                isLoadingModel = false
                            }
                        }
                    }

                    ModelLifecycleUiEvent.UnloadModel -> {
                        scope.launch {
                            isUnloadingModel = true
                            val modelToUnload = loadedModelName ?: "Current Model"
                            val beforeSnap = debugMemoryProvider.captureSnapshot()

                            try {
                                llmEngine.cleanup()
                                debugMemoryProvider.triggerGc()
                                val afterSnap = debugMemoryProvider.captureSnapshot()
                                val delta = afterSnap.diffFrom(beforeSnap)
                                lastUnloadDelta = delta
                                isModelLoaded = false
                                loadedModelName = null
                                activeBackend = null
                                outerEventSink(ModelLifecycleOuterEvent.ModelUnloaded)
                                outerEventSink(
                                    ModelLifecycleOuterEvent.ShowSnackbar(
                                        "Unloaded in ${delta.durationMs}ms. " +
                                            "Native freed: ${"%.1f".format(
                                                -delta.deltaNativeMb,
                                            )} MB, System RAM freed: ${"%.1f".format(-delta.deltaSystemMb)} MB",
                                    ),
                                )
                            } catch (e: Exception) {
                                Timber.e(e, "ModelLifecycleSubPresenter [UNLOAD_ERROR]: Failed unloading %s", modelToUnload)
                                outerEventSink(ModelLifecycleOuterEvent.ShowSnackbar("Unload error: ${e.message}"))
                            } finally {
                                isUnloadingModel = false
                            }
                        }
                    }

                    is ModelLifecycleUiEvent.DeleteModel -> {
                        scope.launch {
                            modelRepository.deleteModel(event.model)
                            outerEventSink(ModelLifecycleOuterEvent.ShowSnackbar("Deleted weights for ${event.model.name}."))
                        }
                    }
                }
            },
        )
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class ModelLifecycleSubPresenterFactory(
    private val modelRepository: ModelRepository,
    private val llmEngine: LlmEngine,
    private val configStore: ModelConfigStore,
    private val debugMemoryProvider: DebugMemoryProvider,
) : SubPresenterFactory {
    override fun create(screen: SubScreen<*>): SubPresenter<*, *>? =
        when (screen) {
            is ModelLifecycleSubScreen -> {
                ModelLifecycleSubPresenter(
                    modelRepository = modelRepository,
                    llmEngine = llmEngine,
                    configStore = configStore,
                    debugMemoryProvider = debugMemoryProvider,
                )
            }

            else -> {
                null
            }
        }
}
