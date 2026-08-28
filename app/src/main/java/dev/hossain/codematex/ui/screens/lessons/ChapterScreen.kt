package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.CourseProgress
import dev.hossain.codematex.data.model.LearningCourse
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
data class ChapterScreen(
    val courseId: String,
) : ParcelableScreen {
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        data object Loading : State

        data class Success(
            val course: LearningCourse,
            val progress: CourseProgress,
            val eventSink: (Event) -> Unit,
        ) : State

        data class NotFound(
            val message: String,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data class OpenLesson(
            val lessonId: String,
        ) : Event

        data object ResetProgress : Event

        data object Back : Event
    }
}
