package dev.hossain.codematex.circuit

import android.os.Parcelable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dev.hossain.codematex.data.model.AiModel
import kotlinx.parcelize.Parcelize

@Parcelize
data object ModelPickerScreen : Screen {
    @kotlinx.serialization.Serializable
    sealed interface State : CircuitUiState {
        data object Loading : State
        data class Success(
            val models: List<AiModel>,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @kotlinx.serialization.Serializable
    sealed interface Event : CircuitUiEvent {
        data class Download(val model: AiModel) : Event
        data class CancelDownload(val model: AiModel) : Event
        data class Delete(val model: AiModel) : Event
        data class Select(val model: AiModel) : Event
    }
}
