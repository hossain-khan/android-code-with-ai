package dev.hossain.codematex.ui.screens.lessons.quiz

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.CodingTopic

/**
 * Outer events emitted by [LessonQuizSubPresenter] to parent presentation layers.
 */
sealed interface LessonQuizOuterEvent : SubCircuitOuterEvent {
    /**
     * Emitted when a learner selects an answer to the quiz.
     *
     * @param quizId Unique identifier of the answered quiz.
     * @param selectedOptionIndex The 0-based option index chosen by the user.
     * @param isCorrect Whether the chosen option matches the correct answer.
     */
    data class QuizAnswered(
        val quizId: String,
        val selectedOptionIndex: Int,
        val isCorrect: Boolean,
    ) : LessonQuizOuterEvent
}

/**
 * Isolated SubCircuit screen definition for an interactive lesson quiz block.
 *
 * @param quizId Unique identifier for this quiz within the lesson.
 * @param question The question text displayed to the learner.
 * @param options The list of candidate answer choices.
 * @param answerIndex The 0-based index of the correct option.
 * @param explanation Explanatory text revealed after the learner submits an answer.
 * @param topic The coding topic metadata used for theme accent colors.
 */
data class LessonQuizSubScreen(
    val quizId: String,
    val question: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String,
    val topic: CodingTopic,
) : SubScreen<LessonQuizOuterEvent>

/**
 * UI state for [LessonQuizSubScreen].
 *
 * @param question The question prompt.
 * @param options Candidate answers.
 * @param answerIndex Correct answer index.
 * @param explanation Explanation text.
 * @param topic Coding topic for accent styling.
 * @param selectedOptionIndex Index of the option selected by the user, or null if unselected.
 * @param eventSink Sink for handling UI events from user interaction.
 */
@Immutable
data class LessonQuizSubState(
    val question: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String,
    val topic: CodingTopic,
    val selectedOptionIndex: Int? = null,
    val eventSink: (LessonQuizUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * User interaction events supported by [LessonQuizSubUi].
 */
sealed interface LessonQuizUiEvent {
    /**
     * User tapped on an option at [index].
     */
    data class SelectOption(
        val index: Int,
    ) : LessonQuizUiEvent
}
