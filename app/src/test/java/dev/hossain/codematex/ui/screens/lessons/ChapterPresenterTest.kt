package dev.hossain.codematex.ui.screens.lessons

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.repository.FakeLearningRepository
import dev.hossain.codematex.data.repository.KotlinCourseContent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChapterPresenterTest {
    private val fakeLearningRepository = FakeLearningRepository()

    @Test
    fun `given valid course - emits success state with course and progress`() =
        runTest {
            val navigator = FakeNavigator(ChapterScreen(KotlinCourseContent.COURSE_ID))
            val presenter =
                ChapterPresenter(
                    navigator = navigator,
                    screen = ChapterScreen(KotlinCourseContent.COURSE_ID),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChapterScreen.State.Success
                assertThat(state.course.id).isEqualTo(KotlinCourseContent.COURSE_ID)
                assertThat(state.course.chapters).isNotEmpty()
            }
        }

    @Test
    fun `given open lesson event - navigates to lesson screen`() =
        runTest {
            val navigator = FakeNavigator(ChapterScreen(KotlinCourseContent.COURSE_ID))
            val presenter =
                ChapterPresenter(
                    navigator = navigator,
                    screen = ChapterScreen(KotlinCourseContent.COURSE_ID),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChapterScreen.State.Success
                state.eventSink(ChapterScreen.Event.OpenLesson("kotlin-hello-world"))

                assertThat(navigator.awaitNextScreen()).isEqualTo(LessonScreen("kotlin-hello-world"))
            }
        }

    @Test
    fun `given back event - pops navigator`() =
        runTest {
            val navigator = FakeNavigator(ChapterScreen(KotlinCourseContent.COURSE_ID))
            val presenter =
                ChapterPresenter(
                    navigator = navigator,
                    screen = ChapterScreen(KotlinCourseContent.COURSE_ID),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as ChapterScreen.State.Success
                state.eventSink(ChapterScreen.Event.Back)

                navigator.awaitPop()
            }
        }

    @Test
    fun `given non-existent course - emits not found state`() =
        runTest {
            val navigator = FakeNavigator(ChapterScreen("non-existent"))
            val presenter =
                ChapterPresenter(
                    navigator = navigator,
                    screen = ChapterScreen("non-existent"),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state).isInstanceOf(ChapterScreen.State.NotFound::class.java)
            }
        }
}
