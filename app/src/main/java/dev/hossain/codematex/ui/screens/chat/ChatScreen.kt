package dev.hossain.codematex.ui.screens.chat

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.system.SystemResourceStats
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
data class ChatScreen(
    val topic: CodingTopic,
    val sessionId: String? = null,
) : ParcelableScreen {
    @Serializable
    sealed interface State : CircuitUiState {
        data object Loading : State

        data class NoModelSelected(
            val hasDownloadedModels: Boolean,
            val topic: CodingTopic,
            val eventSink: (Event) -> Unit,
        ) : State

        data class Active(
            val messages: List<ChatMessage>,
            val isGenerating: Boolean,
            val isPreparing: Boolean,
            val modelName: String,
            val persona: TutorPersona = TutorPersona.SENIOR_ENGINEER,
            val activeBackend: String?,
            val modelSize: String?,
            val modelMemory: String?,
            val configInfo: String?,
            val throughputInfo: String?,
            val systemStatsInfo: String?,
            val systemResourceStats: SystemResourceStats? = null,
            val topic: CodingTopic,
            val eventSink: (Event) -> Unit,
        ) : State

        data class Error(
            val message: String,
            val topic: CodingTopic,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data class SendMessage(
            val text: String,
        ) : Event

        data class SelectPersona(
            val persona: TutorPersona,
        ) : Event

        data object StopGeneration : Event

        data object ResetSession : Event

        data object Retry : Event

        data class CopyMessage(
            val content: String,
        ) : Event

        data object OpenModelPicker : Event

        data object Back : Event
    }
}
