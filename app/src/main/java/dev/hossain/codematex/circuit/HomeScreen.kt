package dev.hossain.codematex.circuit

import android.os.Parcelable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.parcelize.Parcelize

@Parcelize
data object HomeScreen : ParcelableScreen {
    @kotlinx.serialization.Serializable
    sealed interface State : CircuitUiState {
        data object Loading : State

        data class Success(
            val recentSessions: List<ChatSession>,
            val topics: List<CodingTopic>,
            val hasDownloadedModel: Boolean,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @kotlinx.serialization.Serializable
    sealed interface Event : CircuitUiEvent {
        data class TopicSelected(
            val topic: CodingTopic,
        ) : Event

        data class SessionClicked(
            val sessionId: String,
        ) : Event

        data object ManageModels : Event

        data object ViewAllSessions : Event
    }
}
