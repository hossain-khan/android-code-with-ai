package dev.hossain.codematex.circuit

import android.os.Parcelable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import dev.hossain.codematex.data.model.ChatSession
import kotlinx.parcelize.Parcelize

@Parcelize
data object SessionHistoryScreen : Screen {
    @kotlinx.serialization.Serializable
    sealed interface State : CircuitUiState {
        data object Loading : State
        data class Success(
            val sessions: List<ChatSession>,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @kotlinx.serialization.Serializable
    sealed interface Event : CircuitUiEvent {
        data class OpenSession(val sessionId: String) : Event
        data class DeleteSession(val sessionId: String) : Event
    }
}
