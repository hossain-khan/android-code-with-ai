package dev.hossain.codematex.circuit

import android.os.Parcelable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatScreen(
    val topic: CodingTopic,
    val sessionId: String? = null,
) : ParcelableScreen {
    @kotlinx.serialization.Serializable
    sealed interface State : CircuitUiState {
        data object Loading : State

        data class Active(
            val messages: List<ChatMessage>,
            val isGenerating: Boolean,
            val isPreparing: Boolean,
            val modelName: String,
            val topic: CodingTopic,
            val eventSink: (Event) -> Unit,
        ) : State

        data class Error(
            val message: String,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @kotlinx.serialization.Serializable
    sealed interface Event : CircuitUiEvent {
        data class SendMessage(
            val text: String,
        ) : Event

        data object StopGeneration : Event

        data object ResetSession : Event

        data object Retry : Event

        data class CopyMessage(
            val content: String,
        ) : Event
    }
}
