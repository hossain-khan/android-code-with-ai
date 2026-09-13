package dev.hossain.codematex.ui.screens.lessons.quiz

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * SubPresenter managing answer selection, validation, and outer event notifications for [LessonQuizSubScreen].
 */
class LessonQuizSubPresenter(
    private val screen: LessonQuizSubScreen,
) : SubPresenter<LessonQuizOuterEvent, LessonQuizSubState> {
    @Composable
    override fun present(outerEventSink: (LessonQuizOuterEvent) -> Unit): LessonQuizSubState {
        var selectedOptionIndex by rememberRetained { mutableStateOf<Int?>(null) }

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
}

/**
 * SubPresenter factory contributing [LessonQuizSubPresenter] into Metro DI [AppScope].
 */
@ContributesIntoSet(AppScope::class)
@Inject
class LessonQuizSubPresenterFactory : SubPresenterFactory {
    override fun create(screen: SubScreen<*>): SubPresenter<*, *>? =
        when (screen) {
            is LessonQuizSubScreen -> LessonQuizSubPresenter(screen)
            else -> null
        }
}
