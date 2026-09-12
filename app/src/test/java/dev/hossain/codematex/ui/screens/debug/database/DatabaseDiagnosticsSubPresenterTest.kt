package dev.hossain.codematex.ui.screens.debug.database

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.local.FakeSessionDao
import dev.hossain.codematex.data.local.MessageEntity
import dev.hossain.codematex.data.local.SessionEntity
import dev.hossain.codematex.data.repository.FakeLearningRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DatabaseDiagnosticsSubPresenterTest {
    @Test
    fun `presenter emits aggregated database and curriculum metrics`() =
        runTest {
            val fakeRepo = FakeLearningRepository()
            val fakeDao =
                FakeSessionDao(
                    sessions =
                        listOf(
                            SessionEntity("s1", "kotlin", "Title 1", "Summary 1", 1, 1000L, "gemma"),
                            SessionEntity("s2", "python", "Title 2", "Summary 2", 2, 2000L, "gemma"),
                        ),
                    messages =
                        listOf(
                            MessageEntity(
                                sessionId = "s1",
                                messageId = "m1",
                                type = "user",
                                content = "Hi",
                                timestamp = 1000L,
                                orderIndex = 0,
                            ),
                        ),
                )
            val presenter = DatabaseDiagnosticsSubPresenter(fakeRepo, fakeDao)

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.stats.totalCourses).isEqualTo(fakeRepo.courses.size)
                assertThat(state.stats.totalBundledLessons).isEqualTo(fakeRepo.courses.sumOf { it.lessonCount })
                assertThat(state.stats.totalSessions).isEqualTo(2)
                assertThat(state.stats.totalMessages).isEqualTo(1)
                assertThat(state.stats.completedLessons).isEqualTo(0)
            }
        }

    @Test
    fun `seeding progress updates completed lessons and emits snackbar`() =
        runTest {
            val fakeRepo = FakeLearningRepository()
            val fakeDao = FakeSessionDao()
            val presenter = DatabaseDiagnosticsSubPresenter(fakeRepo, fakeDao)

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(DatabaseDiagnosticsUiEvent.SeedProgress)

                val updatedState = expectMostRecentItem()
                val expectedCompleted = fakeRepo.courses.sumOf { course -> minOf(3, course.chapters.flatMap { it.lessons }.size) }
                assertThat(updatedState.stats.completedLessons).isEqualTo(expectedCompleted)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isInstanceOf(DatabaseDiagnosticsOuterEvent.ShowSnackbar::class.java)
                assertThat((outerEvent as DatabaseDiagnosticsOuterEvent.ShowSnackbar).message).contains("Seeded sample progress")
            }
        }

    @Test
    fun `resetting progress clears completions and emits snackbar`() =
        runTest {
            val fakeRepo = FakeLearningRepository()
            fakeRepo.seedSampleProgress(lessonsPerCourse = 3)
            val fakeDao = FakeSessionDao()
            val presenter = DatabaseDiagnosticsSubPresenter(fakeRepo, fakeDao)

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.stats.completedLessons).isGreaterThan(0)

                state.eventSink(DatabaseDiagnosticsUiEvent.ResetProgress)

                val updatedState = expectMostRecentItem()
                assertThat(updatedState.stats.completedLessons).isEqualTo(0)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isInstanceOf(DatabaseDiagnosticsOuterEvent.ShowSnackbar::class.java)
                assertThat((outerEvent as DatabaseDiagnosticsOuterEvent.ShowSnackbar).message).contains("reset to 0%")
            }
        }

    @Test
    fun `clearing sessions clears database and emits snackbar`() =
        runTest {
            val fakeRepo = FakeLearningRepository()
            val fakeDao =
                FakeSessionDao(
                    sessions = listOf(SessionEntity("s1", "kotlin", "Title", "Summary", 1, 1000L, "gemma")),
                )
            val presenter = DatabaseDiagnosticsSubPresenter(fakeRepo, fakeDao)

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.stats.totalSessions).isEqualTo(1)

                state.eventSink(DatabaseDiagnosticsUiEvent.ClearSessions)

                val updatedState = expectMostRecentItem()
                assertThat(updatedState.stats.totalSessions).isEqualTo(0)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isInstanceOf(DatabaseDiagnosticsOuterEvent.ShowSnackbar::class.java)
                assertThat(
                    (outerEvent as DatabaseDiagnosticsOuterEvent.ShowSnackbar).message,
                ).contains("All chat sessions and messages cleared")
            }
        }
}
