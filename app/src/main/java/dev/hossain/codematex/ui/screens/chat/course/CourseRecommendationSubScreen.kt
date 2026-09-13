package dev.hossain.codematex.ui.screens.chat.course

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LearningCourse

/**
 * Outer events emitted by [CourseRecommendationSubPresenter] to parent screens.
 */
sealed interface CourseRecommendationOuterEvent : SubCircuitOuterEvent {
    /** Emitted when learner selects a recommended course to start. */
    data class NavigateToCourse(
        val courseId: String,
    ) : CourseRecommendationOuterEvent
}

/**
 * SubScreen identifying contextual course recommendations for a chat session.
 *
 * @param topic The active coding topic.
 * @param showCourseBanner Whether the recommendation should be displayed.
 */
data class CourseRecommendationSubScreen(
    val topic: CodingTopic,
    val showCourseBanner: Boolean = true,
) : SubScreen<CourseRecommendationOuterEvent>

/**
 * UI State for [CourseRecommendationSubScreen].
 */
@Immutable
data class CourseRecommendationSubState(
    val topic: CodingTopic,
    val course: LearningCourse? = null,
    val eventSink: (CourseRecommendationUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * UI interaction events for [CourseRecommendationSubUi].
 */
sealed interface CourseRecommendationUiEvent {
    /** User tapped to start the suggested course. */
    data class StartCourse(
        val courseId: String,
    ) : CourseRecommendationUiEvent

    /** User dismissed the course recommendation card. */
    data object Dismiss : CourseRecommendationUiEvent
}
