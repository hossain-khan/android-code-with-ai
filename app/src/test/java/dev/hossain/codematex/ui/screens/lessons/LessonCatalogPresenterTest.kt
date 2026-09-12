package dev.hossain.codematex.ui.screens.lessons

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.FakeLearningRepository
import dev.hossain.codematex.data.repository.course.GoCourseContent
import dev.hossain.codematex.data.repository.course.KotlinCourseContent
import dev.hossain.codematex.data.repository.course.PythonCourseContent
import dev.hossain.codematex.data.repository.course.RustByExampleCourseContent
import dev.hossain.codematex.data.repository.course.RustCourseContent
import dev.hossain.codematex.data.repository.course.SwiftCourseContent
import dev.hossain.codematex.data.repository.course.TypeScriptCourseContent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LessonCatalogPresenterTest {
    private val fakeLearningRepository = FakeLearningRepository()

    @Test
    fun `given bundled courses - emits success state with courses and topic metadata`() =
        runTest {
            val navigator = FakeNavigator(LessonCatalogScreen())
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen(),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonCatalogScreen.State.Success
                assertThat(state.courses).hasSize(7)
                assertThat(state.allCourses).hasSize(7)
                assertThat(state.selectedTopic).isNull()
                assertThat(state.courses.first().id).isEqualTo(KotlinCourseContent.COURSE_ID)
                assertThat(state.courses[1].id).isEqualTo(PythonCourseContent.COURSE_ID)
                assertThat(state.courses[2].id).isEqualTo(TypeScriptCourseContent.COURSE_ID)
                assertThat(state.courses[3].id).isEqualTo(GoCourseContent.COURSE_ID)
                assertThat(state.courses[4].id).isEqualTo(RustCourseContent.COURSE_ID)
                assertThat(state.courses[5].id).isEqualTo(RustByExampleCourseContent.COURSE_ID)
                assertThat(state.courses[6].id).isEqualTo(SwiftCourseContent.COURSE_ID)

                assertThat(state.availableTopics)
                    .containsExactly(
                        CodingTopic.KOTLIN,
                        CodingTopic.PYTHON,
                        CodingTopic.TYPESCRIPT,
                        CodingTopic.GO,
                        CodingTopic.RUST,
                        CodingTopic.SWIFT,
                    ).inOrder()

                assertThat(state.courseCountsByTopic).containsEntry(CodingTopic.KOTLIN, 1)
                assertThat(state.courseCountsByTopic).containsEntry(CodingTopic.PYTHON, 1)
                assertThat(state.courseCountsByTopic).containsEntry(CodingTopic.TYPESCRIPT, 1)
                assertThat(state.courseCountsByTopic).containsEntry(CodingTopic.GO, 1)
                assertThat(state.courseCountsByTopic).containsEntry(CodingTopic.RUST, 2)
                assertThat(state.courseCountsByTopic).containsEntry(CodingTopic.SWIFT, 1)
            }
        }

    @Test
    fun `given initial topic in screen - emits success state filtered by that topic`() =
        runTest {
            val screen = LessonCatalogScreen(initialTopic = CodingTopic.RUST)
            val navigator = FakeNavigator(screen)
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = screen,
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonCatalogScreen.State.Success
                assertThat(state.selectedTopic).isEqualTo(CodingTopic.RUST)
                assertThat(state.allCourses).hasSize(7)
                assertThat(state.courses).hasSize(2)
                assertThat(state.courses.map { it.id })
                    .containsExactly(
                        RustCourseContent.COURSE_ID,
                        RustByExampleCourseContent.COURSE_ID,
                    ).inOrder()
            }
        }

    @Test
    fun `given select topic event - filters courses and updates selectedTopic`() =
        runTest {
            val navigator = FakeNavigator(LessonCatalogScreen())
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen(),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val initialState = expectMostRecentItem() as LessonCatalogScreen.State.Success
                assertThat(initialState.courses).hasSize(7)
                assertThat(initialState.selectedTopic).isNull()

                initialState.eventSink(LessonCatalogScreen.Event.SelectTopic(CodingTopic.GO))

                val filteredState = expectMostRecentItem() as LessonCatalogScreen.State.Success
                assertThat(filteredState.selectedTopic).isEqualTo(CodingTopic.GO)
                assertThat(filteredState.courses).hasSize(1)
                assertThat(filteredState.courses.first().id).isEqualTo(GoCourseContent.COURSE_ID)

                filteredState.eventSink(LessonCatalogScreen.Event.SelectTopic(null))

                val resetState = expectMostRecentItem() as LessonCatalogScreen.State.Success
                assertThat(resetState.selectedTopic).isNull()
                assertThat(resetState.courses).hasSize(7)
            }
        }

    @Test
    fun `given open course event - navigates to chapter screen`() =
        runTest {
            val navigator = FakeNavigator(LessonCatalogScreen())
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen(),
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
            val navigator = FakeNavigator(LessonCatalogScreen())
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen(),
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
            val navigator = FakeNavigator(LessonCatalogScreen())
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen(),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonCatalogScreen.State.Success
                state.eventSink(LessonCatalogScreen.Event.OpenCourse(PythonCourseContent.COURSE_ID))

                assertThat(navigator.awaitNextScreen()).isEqualTo(ChapterScreen(PythonCourseContent.COURSE_ID))
            }
        }

    @Test
    fun `given open typescript course event - navigates to typescript chapter screen`() =
        runTest {
            val navigator = FakeNavigator(LessonCatalogScreen())
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen(),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonCatalogScreen.State.Success
                state.eventSink(LessonCatalogScreen.Event.OpenCourse(TypeScriptCourseContent.COURSE_ID))

                assertThat(navigator.awaitNextScreen()).isEqualTo(ChapterScreen(TypeScriptCourseContent.COURSE_ID))
            }
        }

    @Test
    fun `given open go course event - navigates to go chapter screen`() =
        runTest {
            val navigator = FakeNavigator(LessonCatalogScreen())
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen(),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonCatalogScreen.State.Success
                state.eventSink(LessonCatalogScreen.Event.OpenCourse(GoCourseContent.COURSE_ID))

                assertThat(navigator.awaitNextScreen()).isEqualTo(ChapterScreen(GoCourseContent.COURSE_ID))
            }
        }

    @Test
    fun `given open rust course event - navigates to rust chapter screen`() =
        runTest {
            val navigator = FakeNavigator(LessonCatalogScreen())
            val presenter =
                LessonCatalogPresenter(
                    navigator = navigator,
                    screen = LessonCatalogScreen(),
                    learningRepository = fakeLearningRepository,
                )

            presenter.test {
                val state = expectMostRecentItem() as LessonCatalogScreen.State.Success
                state.eventSink(LessonCatalogScreen.Event.OpenCourse(RustCourseContent.COURSE_ID))

                assertThat(navigator.awaitNextScreen()).isEqualTo(ChapterScreen(RustCourseContent.COURSE_ID))
            }
        }
}
