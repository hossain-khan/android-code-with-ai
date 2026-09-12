package dev.hossain.codematex.ui.screens.home

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
data object HomeScreen : ParcelableScreen {
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        data class IneligibleDevice(
            val reason: String,
            val detectedRamGb: Double,
            val minRequiredRamGb: Double,
            val is64BitSupported: Boolean,
            val eventSink: (Event) -> Unit,
        ) : State

        data class Success(
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data class TopicSelected(
            val topic: CodingTopic,
        ) : Event

        data class SessionSelected(
            val topic: CodingTopic,
            val sessionId: String,
        ) : Event

        data class CourseClicked(
            val courseId: String,
        ) : Event

        data object ManageModels : Event

        data object GuidedLessons : Event

        data object ViewAllSessions : Event

        data object AppTour : Event

        data object OpenSettings : Event

        data object DismissIneligibilityWarning : Event
    }
}
