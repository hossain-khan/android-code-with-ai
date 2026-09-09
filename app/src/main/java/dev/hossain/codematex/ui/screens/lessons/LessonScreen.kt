package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Immutable
@Serializable
sealed interface SnippetExecutionState {
    data object Idle : SnippetExecutionState

    data object Compiling : SnippetExecutionState

    data class Success(
        val output: String,
    ) : SnippetExecutionState

    data class CompilationError(
        val diagnostic: String,
    ) : SnippetExecutionState

    data class Error(
        val message: String,
    ) : SnippetExecutionState
}

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
            val course: LearningCourse,
            val isCompleted: Boolean,
            val nextLessonId: String?,
            val isOnline: Boolean = true,
            val snippetExecutionStates: Map<Int, SnippetExecutionState> = emptyMap(),
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

        data class RunSnippet(
            val blockIndex: Int,
            val code: String,
            val language: String,
        ) : Event

        data class DismissSnippetOutput(
            val blockIndex: Int,
        ) : Event
    }
}
