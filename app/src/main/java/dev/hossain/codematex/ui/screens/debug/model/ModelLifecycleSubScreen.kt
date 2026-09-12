package dev.hossain.codematex.ui.screens.debug.model

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.MemoryDelta

/**
 * Outer events emitted by [ModelLifecycleSubPresenter] to the parent screen.
 */
sealed interface ModelLifecycleOuterEvent : SubCircuitOuterEvent {
    data class ShowSnackbar(
        val message: String,
    ) : ModelLifecycleOuterEvent

    data class ModelLoaded(
        val modelName: String,
        val backend: LlmEngine.Backend,
    ) : ModelLifecycleOuterEvent

    data object ModelUnloaded : ModelLifecycleOuterEvent
}

/**
 * UI State for the Model Lifecycle SubCircuit.
 */
@Immutable
data class ModelLifecycleSubState(
    val models: List<AiModel> = emptyList(),
    val selectedModel: AiModel? = null,
    val selectedBackend: LlmEngine.Backend = LlmEngine.Backend.GPU,
    val isModelLoaded: Boolean = false,
    val loadedModelName: String? = null,
    val activeBackend: LlmEngine.Backend? = null,
    val isLoadingModel: Boolean = false,
    val isUnloadingModel: Boolean = false,
    val lastLoadDelta: MemoryDelta? = null,
    val lastUnloadDelta: MemoryDelta? = null,
    val eventSink: (ModelLifecycleUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * UI Events handled by [ModelLifecycleSubPresenter].
 */
sealed interface ModelLifecycleUiEvent {
    data class SelectModel(
        val model: AiModel,
    ) : ModelLifecycleUiEvent

    data class SelectBackend(
        val backend: LlmEngine.Backend,
    ) : ModelLifecycleUiEvent

    data object LoadModel : ModelLifecycleUiEvent

    data object UnloadModel : ModelLifecycleUiEvent

    data class DeleteModel(
        val model: AiModel,
    ) : ModelLifecycleUiEvent
}

/**
 * SubScreen marker for the model lifecycle, loading profiler, and on-disk model weights cards.
 */
data object ModelLifecycleSubScreen : SubScreen<ModelLifecycleOuterEvent>
