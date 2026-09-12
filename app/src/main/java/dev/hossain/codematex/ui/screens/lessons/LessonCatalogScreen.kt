package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.CourseProgress
import dev.hossain.codematex.data.model.LearningCourse
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
data class LessonCatalogScreen(
    val initialTopic: CodingTopic? = null,
) : ParcelableScreen {
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        data object Loading : State

        data class Success(
            val allCourses: List<LearningCourse>,
            val courses: List<LearningCourse>,
            val availableTopics: List<CodingTopic>,
            val courseCountsByTopic: Map<CodingTopic, Int>,
            val selectedTopic: CodingTopic?,
            val progress: Map<String, CourseProgress>,
            val eventSink: (Event) -> Unit,
        ) : State

        data class Error(
            val message: String,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data class SelectTopic(
            val topic: CodingTopic?,
        ) : Event

        data class OpenCourse(
            val courseId: String,
        ) : Event

        data object Retry : Event

        data object Back : Event
    }
}
