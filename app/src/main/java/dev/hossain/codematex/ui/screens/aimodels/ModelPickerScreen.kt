package dev.hossain.codematex.ui.screens.aimodels

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.AiModel
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
            val downloadOverWifiOnly: Boolean = true,
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

        data class ToggleWifiOnly(
            val enabled: Boolean,
        ) : Event

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
    }
}
