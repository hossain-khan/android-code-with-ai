package dev.hossain.codematex.ui.screens.home.courses

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.repository.FakeLearningRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GuidedCoursesSubPresenterTest {
    private val testCourses =
        listOf(
            LearningCourse(
                id = "kotlin-foundations",
                language = "Kotlin",
                title = "Kotlin Foundations",
                description = "Learn Kotlin",
                version = 1,
                chapters = emptyList(),
            ),
            LearningCourse(
                id = "rust-foundations",
                language = "Rust",
                title = "Rust Foundations",
                description = "Learn Rust",
                version = 1,
                chapters = emptyList(),
            ),
        )

    @Test
    fun `emits available courses from repository`() =
        runTest {
            val fakeRepo = FakeLearningRepository(courses = testCourses)
            val presenter = GuidedCoursesSubPresenter(learningRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.availableCourses).hasSize(2)
                assertThat(state.availableCourses.map { it.id }).containsExactly("kotlin-foundations", "rust-foundations")
            }
        }

    @Test
    fun `CourseClicked emits NavigateToCourse outer event`() =
        runTest {
            val fakeRepo = FakeLearningRepository(courses = testCourses)
            val presenter = GuidedCoursesSubPresenter(learningRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(GuidedCoursesUiEvent.CourseClicked("kotlin-foundations"))

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(GuidedCoursesOuterEvent.NavigateToCourse("kotlin-foundations"))
            }
        }

    @Test
    fun `ViewAllCourses emits NavigateToAllCourses outer event`() =
        runTest {
            val fakeRepo = FakeLearningRepository(courses = testCourses)
            val presenter = GuidedCoursesSubPresenter(learningRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(GuidedCoursesUiEvent.ViewAllCourses)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(GuidedCoursesOuterEvent.NavigateToAllCourses)
            }
        }
}
