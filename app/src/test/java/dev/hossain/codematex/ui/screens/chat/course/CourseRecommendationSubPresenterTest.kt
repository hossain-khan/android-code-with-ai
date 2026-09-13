package dev.hossain.codematex.ui.screens.chat.course

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.repository.FakeLearningRepository
import dev.hossain.codematex.data.repository.FakeUserPreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CourseRecommendationSubPresenterTest {
    private val testCourse =
        LearningCourse(
            id = "kotlin-foundations",
            language = "Kotlin",
            title = "Kotlin Foundations",
            description = "Learn Kotlin",
            version = 1,
            chapters = emptyList(),
        )

    @Test
    fun `loads course when showCourseBanner is true and not dismissed`() =
        runTest {
            val fakeRepo = FakeLearningRepository(courses = listOf(testCourse))
            val fakePrefs = FakeUserPreferencesStore()
            val presenter =
                CourseRecommendationSubPresenter(
                    screen = CourseRecommendationSubScreen(topic = CodingTopic.KOTLIN, showCourseBanner = true),
                    learningRepository = fakeRepo,
                    userPreferencesStore = fakePrefs,
                )

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.topic).isEqualTo(CodingTopic.KOTLIN)
                assertThat(state.course).isEqualTo(testCourse)
            }
        }

    @Test
    fun `course is null when showCourseBanner is false`() =
        runTest {
            val fakeRepo = FakeLearningRepository(courses = listOf(testCourse))
            val fakePrefs = FakeUserPreferencesStore()
            val presenter =
                CourseRecommendationSubPresenter(
                    screen = CourseRecommendationSubScreen(topic = CodingTopic.KOTLIN, showCourseBanner = false),
                    learningRepository = fakeRepo,
                    userPreferencesStore = fakePrefs,
                )

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.course).isNull()
            }
        }

    @Test
    fun `course is null when topic is dismissed in preferences`() =
        runTest {
            val fakeRepo = FakeLearningRepository(courses = listOf(testCourse))
            val fakePrefs = FakeUserPreferencesStore()
            fakePrefs.dismissCourseBanner(CodingTopic.KOTLIN)

            val presenter =
                CourseRecommendationSubPresenter(
                    screen = CourseRecommendationSubScreen(topic = CodingTopic.KOTLIN, showCourseBanner = true),
                    learningRepository = fakeRepo,
                    userPreferencesStore = fakePrefs,
                )

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.course).isNull()
            }
        }

    @Test
    fun `Dismiss event dismisses banner in preferences and clears course`() =
        runTest {
            val fakeRepo = FakeLearningRepository(courses = listOf(testCourse))
            val fakePrefs = FakeUserPreferencesStore()
            val presenter =
                CourseRecommendationSubPresenter(
                    screen = CourseRecommendationSubScreen(topic = CodingTopic.KOTLIN, showCourseBanner = true),
                    learningRepository = fakeRepo,
                    userPreferencesStore = fakePrefs,
                )

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.course).isEqualTo(testCourse)

                state.eventSink(CourseRecommendationUiEvent.Dismiss)

                val updatedState = expectMostRecentItem()
                assertThat(updatedState.course).isNull()
                assertThat(fakePrefs.dismissedCourseBannerTopicsFlow.first()).contains(CodingTopic.KOTLIN.name)
            }
        }

    @Test
    fun `StartCourse event dismisses banner and emits NavigateToCourse outer event`() =
        runTest {
            val fakeRepo = FakeLearningRepository(courses = listOf(testCourse))
            val fakePrefs = FakeUserPreferencesStore()
            val presenter =
                CourseRecommendationSubPresenter(
                    screen = CourseRecommendationSubScreen(topic = CodingTopic.KOTLIN, showCourseBanner = true),
                    learningRepository = fakeRepo,
                    userPreferencesStore = fakePrefs,
                )

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.course).isEqualTo(testCourse)

                state.eventSink(CourseRecommendationUiEvent.StartCourse(testCourse.id))

                val updatedState = expectMostRecentItem()
                assertThat(updatedState.course).isNull()
                assertThat(fakePrefs.dismissedCourseBannerTopicsFlow.first()).contains(CodingTopic.KOTLIN.name)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(CourseRecommendationOuterEvent.NavigateToCourse(testCourse.id))
            }
        }
}
