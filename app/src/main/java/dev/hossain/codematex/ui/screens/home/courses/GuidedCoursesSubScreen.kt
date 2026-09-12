package dev.hossain.codematex.ui.screens.home.courses

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.LearningCourse

/**
 * Outer events emitted by [GuidedCoursesSubPresenter].
 */
sealed interface GuidedCoursesOuterEvent : SubCircuitOuterEvent {
    data class NavigateToCourse(
        val courseId: String,
    ) : GuidedCoursesOuterEvent

    data object NavigateToAllCourses : GuidedCoursesOuterEvent
}

/**
 * UI State for the Guided Courses SubCircuit.
 */
@Immutable
data class GuidedCoursesSubState(
    val availableCourses: List<LearningCourse> = emptyList(),
    val eventSink: (GuidedCoursesUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * UI Events handled by [GuidedCoursesSubPresenter].
 */
sealed interface GuidedCoursesUiEvent {
    data class CourseClicked(
        val courseId: String,
    ) : GuidedCoursesUiEvent

    data object ViewAllCourses : GuidedCoursesUiEvent
}

/**
 * SubScreen marker for the guided courses section.
 */
data object GuidedCoursesSubScreen : SubScreen<GuidedCoursesOuterEvent>
