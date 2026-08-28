package dev.hossain.codematex.ui.screens.lessons

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.repository.FakeLearningRepository
import dev.hossain.codematex.data.repository.KotlinCourseContent
import dev.hossain.codematex.data.repository.PythonCourseContent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LessonCatalogPresenterTest {
    private val fakeLearningRepository = FakeLearningRepository()

    @Test
    fun `given bundled courses - emits success state with courses`() =
        runTest {
            val navigator = FakeNavigator(LessonCatalogScreen)
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen,
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonCatalogScreen.State.Success
                assertThat(state.courses).hasSize(2)
                assertThat(state.courses.first().id).isEqualTo(KotlinCourseContent.COURSE_ID)
                assertThat(state.courses[1].id).isEqualTo(PythonCourseContent.COURSE_ID)
            }
        }

    @Test
    fun `given open course event - navigates to chapter screen`() =
        runTest {
            val navigator = FakeNavigator(LessonCatalogScreen)
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen,
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonCatalogScreen.State.Success
                state.eventSink(LessonCatalogScreen.Event.OpenCourse(KotlinCourseContent.COURSE_ID))

                assertThat(navigator.awaitNextScreen()).isEqualTo(ChapterScreen(KotlinCourseContent.COURSE_ID))
            }
        }

    @Test
    fun `given back event - pops navigator`() =
        runTest {
            val navigator = FakeNavigator(LessonCatalogScreen)
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen,
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonCatalogScreen.State.Success
                state.eventSink(LessonCatalogScreen.Event.Back)

                navigator.awaitPop()
            }
        }

    @Test
    fun `given open python course event - navigates to python chapter screen`() =
        runTest {
            val navigator = FakeNavigator(LessonCatalogScreen)
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen,
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonCatalogScreen.State.Success
                state.eventSink(LessonCatalogScreen.Event.OpenCourse(PythonCourseContent.COURSE_ID))

                assertThat(navigator.awaitNextScreen()).isEqualTo(ChapterScreen(PythonCourseContent.COURSE_ID))
            }
        }
}
