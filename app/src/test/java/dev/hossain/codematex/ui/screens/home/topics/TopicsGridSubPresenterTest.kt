package dev.hossain.codematex.ui.screens.home.topics

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.repository.FakeLearningRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TopicsGridSubPresenterTest {
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
        )

    @Test
    fun `emits topics and topicsWithCourses from repository`() =
        runTest {
            val fakeRepo = FakeLearningRepository(courses = testCourses)
            val presenter = TopicsGridSubPresenter(TopicsGridSubScreen(isCompact = false), learningRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.topics).containsExactlyElementsIn(CodingTopic.entries)
                assertThat(state.topicsWithCourses).containsExactly(CodingTopic.KOTLIN)
                assertThat(state.isCompact).isFalse()
            }
        }

    @Test
    fun `reflects isCompact flag from screen`() =
        runTest {
            val fakeRepo = FakeLearningRepository(courses = testCourses)
            val presenter = TopicsGridSubPresenter(TopicsGridSubScreen(isCompact = true), learningRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.isCompact).isTrue()
            }
        }

    @Test
    fun `TopicSelected emits NavigateToTopic outer event`() =
        runTest {
            val fakeRepo = FakeLearningRepository(courses = testCourses)
            val presenter = TopicsGridSubPresenter(TopicsGridSubScreen(), learningRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(TopicsGridUiEvent.TopicSelected(CodingTopic.PYTHON))

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(TopicsGridOuterEvent.NavigateToTopic(CodingTopic.PYTHON))
            }
        }
}
