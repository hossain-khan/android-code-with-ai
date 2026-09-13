package dev.hossain.codematex.ui.screens.lessons.quiz

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LessonQuizSubPresenterTest {
    private val testScreen =
        LessonQuizSubScreen(
            quizId = "quiz-1",
            question = "What keyword is used to declare an immutable variable in Kotlin?",
            options = listOf("var", "val", "let", "const"),
            answerIndex = 1,
            explanation = "In Kotlin, 'val' declares a read-only (immutable) variable.",
            topic = CodingTopic.KOTLIN,
        )

    private fun createPresenter(screen: LessonQuizSubScreen = testScreen): LessonQuizSubPresenter = LessonQuizSubPresenter(screen = screen)

    @Test
    fun `initial state reflects screen properties and has null selection`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.question).isEqualTo(testScreen.question)
                assertThat(state.options).isEqualTo(testScreen.options)
                assertThat(state.answerIndex).isEqualTo(1)
                assertThat(state.explanation).isEqualTo(testScreen.explanation)
                assertThat(state.topic).isEqualTo(CodingTopic.KOTLIN)
                assertThat(state.selectedOptionIndex).isNull()
            }
        }

    @Test
    fun `selecting correct option updates state and emits QuizAnswered with isCorrect true`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(LessonQuizUiEvent.SelectOption(1))

                val updatedState = expectMostRecentItem()
                assertThat(updatedState.selectedOptionIndex).isEqualTo(1)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(
                    LessonQuizOuterEvent.QuizAnswered(
                        quizId = "quiz-1",
                        selectedOptionIndex = 1,
                        isCorrect = true,
                    ),
                )
            }
        }

    @Test
    fun `selecting incorrect option updates state and emits QuizAnswered with isCorrect false`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(LessonQuizUiEvent.SelectOption(0))

                val updatedState = expectMostRecentItem()
                assertThat(updatedState.selectedOptionIndex).isEqualTo(0)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(
                    LessonQuizOuterEvent.QuizAnswered(
                        quizId = "quiz-1",
                        selectedOptionIndex = 0,
                        isCorrect = false,
                    ),
                )
            }
        }

    @Test
    fun `changing selected option updates state and emits another outer event`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(LessonQuizUiEvent.SelectOption(0))

                val firstUpdate = expectMostRecentItem()
                assertThat(firstUpdate.selectedOptionIndex).isEqualTo(0)
                val firstEvent = outerEvents.awaitEvent() as LessonQuizOuterEvent.QuizAnswered
                assertThat(firstEvent.isCorrect).isFalse()

                firstUpdate.eventSink(LessonQuizUiEvent.SelectOption(1))

                val secondUpdate = expectMostRecentItem()
                assertThat(secondUpdate.selectedOptionIndex).isEqualTo(1)
                val secondEvent = outerEvents.awaitEvent() as LessonQuizOuterEvent.QuizAnswered
                assertThat(secondEvent.isCorrect).isTrue()
            }
        }
}
