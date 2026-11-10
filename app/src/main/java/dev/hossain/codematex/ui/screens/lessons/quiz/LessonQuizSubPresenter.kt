package dev.hossain.codematex.ui.screens.lessons.quiz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.subcircuit.SubCircuitInject
import com.slack.circuit.subcircuit.SubPresenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

/**
 * SubPresenter managing answer selection, validation, and outer event notifications for [LessonQuizSubScreen].
 */
@AssistedInject
class LessonQuizSubPresenter(
    @Assisted private val screen: LessonQuizSubScreen,
) : SubPresenter<LessonQuizOuterEvent, LessonQuizSubState> {
    @Composable
    override fun present(outerEventSink: (LessonQuizOuterEvent) -> Unit): LessonQuizSubState {
        var selectedOptionIndex by rememberRetained(screen.quizId) { mutableStateOf<Int?>(null) }

        return LessonQuizSubState(
            question = screen.question,
            options = screen.options,
            answerIndex = screen.answerIndex,
            explanation = screen.explanation,
            topic = screen.topic,
            selectedOptionIndex = selectedOptionIndex,
            eventSink = { event ->
                when (event) {
                    is LessonQuizUiEvent.SelectOption -> {
                        selectedOptionIndex = event.index
                        outerEventSink(
                            LessonQuizOuterEvent.QuizAnswered(
                                quizId = screen.quizId,
                                selectedOptionIndex = event.index,
                                isCorrect = event.index == screen.answerIndex,
                            ),
                        )
                    }
                }
            },
        )
    }

    @SubCircuitInject(LessonQuizSubScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(screen: LessonQuizSubScreen): LessonQuizSubPresenter
    }
}
