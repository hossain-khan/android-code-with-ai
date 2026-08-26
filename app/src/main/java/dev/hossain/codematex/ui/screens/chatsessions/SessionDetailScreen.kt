package dev.hossain.codematex.ui.screens.chatsessions

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ChatSession
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
data class SessionDetailScreen(
    val sessionId: String,
) : ParcelableScreen {
    @Serializable
    sealed interface State : CircuitUiState {
        data object Loading : State

        data class Success(
            val session: ChatSession,
            val messages: List<ChatMessage>,
            val eventSink: (Event) -> Unit,
        ) : State

        data class NotFound(
            val sessionId: String,
            val eventSink: (Event) -> Unit,
        ) : State

        data class Error(
            val message: String,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data object ResumeSession : Event

        data object DeleteSession : Event

        data object Retry : Event

        data object Back : Event
    }
}
