package dev.hossain.codematex.ui.screens.chatsessions

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
data object SessionHistoryScreen : ParcelableScreen {
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        data object Loading : State

        data class Success(
            val sessions: List<ChatSession>,
            val allSessions: List<ChatSession>,
            val availableTopics: List<CodingTopic>,
            val selectedTopic: CodingTopic?,
            val eventSink: (Event) -> Unit,
        ) : State

        data class Error(
            val message: String,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data class OpenSession(
            val sessionId: String,
        ) : Event

        data class DeleteSession(
            val sessionId: String,
        ) : Event

        data class SelectTopicFilter(
            val topic: CodingTopic?,
        ) : Event

        data object Retry : Event

        data object Back : Event
    }
}
