package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.LearningLesson
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
data class LessonScreen(
    val lessonId: String,
) : ParcelableScreen {
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        data object Loading : State

        data class Success(
            val lesson: LearningLesson,
            val course: dev.hossain.codematex.data.model.LearningCourse,
            val isCompleted: Boolean,
            val nextLessonId: String?,
            val eventSink: (Event) -> Unit,
        ) : State

        data class NotFound(
            val message: String,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data object MarkCompleted : Event

        data object NextLesson : Event

        data object AskAi : Event

        data object Back : Event
    }
}
