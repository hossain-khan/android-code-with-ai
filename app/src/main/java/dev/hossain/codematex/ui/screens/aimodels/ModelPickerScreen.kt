package dev.hossain.codematex.ui.screens.aimodels

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DeviceMemoryInfo
import dev.hossain.codematex.system.ModelCompatibility
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
data object ModelPickerScreen : ParcelableScreen {
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        data object Loading : State

        data class Success(
            val models: List<AiModel>,
            val deviceMemoryInfo: DeviceMemoryInfo,
            val modelCompatibility: Map<String, ModelCompatibility>,
            val availableMemoryGb: Float = 0f,
            val isModelLoadedInMemory: Boolean = false,
            val activeBackend: LlmEngine.Backend? = null,
            val isUnloadingModel: Boolean = false,
            val configuredModel: AiModel? = null,
            val configuredModelConfig: ModelConfig? = null,
            val statusMessage: String? = null,
            val eventSink: (Event) -> Unit,
        ) : State

        data class Error(
            val message: String,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data object Back : Event

        data object Retry : Event

        data class Download(
            val model: AiModel,
        ) : Event

        data class CancelDownload(
            val model: AiModel,
        ) : Event

        data class Delete(
            val model: AiModel,
        ) : Event

        data class Select(
            val model: AiModel,
        ) : Event

        data class OpenModelConfig(
            val model: AiModel,
        ) : Event

        data object DismissModelConfig : Event

        data class SaveModelConfig(
            val model: AiModel,
            val config: ModelConfig,
        ) : Event

        data class ResetModelConfig(
            val model: AiModel,
        ) : Event

        data object OpenDebugScreen : Event

        data object UnloadModel : Event

        data object ClearStatusMessage : Event
    }
}
