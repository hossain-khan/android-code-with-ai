package dev.hossain.codematex.ui.screens.debug.database

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.local.SessionDao
import dev.hossain.codematex.data.model.LessonBlock
import dev.hossain.codematex.data.model.LessonStatus
import dev.hossain.codematex.data.repository.course.LearningRepository
import dev.hossain.codematex.ui.screens.debug.DebugScreen.DebugDatabaseStats
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber

class DatabaseDiagnosticsSubPresenter(
    private val learningRepository: LearningRepository,
    private val sessionDao: SessionDao,
) : SubPresenter<DatabaseDiagnosticsOuterEvent, DatabaseDiagnosticsSubState> {
    @Composable
    override fun present(outerEventSink: (DatabaseDiagnosticsOuterEvent) -> Unit): DatabaseDiagnosticsSubState {
        val scope = rememberCoroutineScope()
        var databaseStats by rememberRetained { mutableStateOf(DebugDatabaseStats()) }

        LaunchedEffect(Unit) {
            combine(
                learningRepository.getCourses(),
                learningRepository.observeAllProgress(),
                sessionDao.observeSessionCount(),
                sessionDao.observeMessageCount(),
            ) { courses, progressList, sessionCount, messageCount ->
                val completedLessonIds =
                    progressList
                        .filter { it.status == LessonStatus.COMPLETED }
                        .map { it.lessonId }
                        .toSet()
                val totalLessons = courses.sumOf { it.lessonCount }
                val totalCourses = courses.size
                val completedCourses =
                    courses.count { course ->
                        val lessonIds = course.chapters.flatMap { it.lessons }.map { it.id }
                        lessonIds.isNotEmpty() && lessonIds.all { it in completedLessonIds }
                    }
                val totalQuizzes =
                    courses.sumOf { course ->
                        course.chapters.sumOf { chapter ->
                            chapter.lessons.sumOf { lesson ->
                                lesson.blocks.count { it is LessonBlock.Quiz }
                            }
                        }
                    }
                DebugDatabaseStats(
                    completedLessons = completedLessonIds.size,
                    inProgressLessons = progressList.count { it.status == LessonStatus.IN_PROGRESS },
                    totalBundledLessons = totalLessons,
                    totalCourses = totalCourses,
                    completedCourses = completedCourses,
                    totalQuizzes = totalQuizzes,
                    totalSessions = sessionCount,
                    totalMessages = messageCount,
                )
            }.collect { stats ->
                databaseStats = stats
            }
        }

        return DatabaseDiagnosticsSubState(
            stats = databaseStats,
            eventSink = { event ->
                when (event) {
                    DatabaseDiagnosticsUiEvent.ResetProgress -> {
                        scope.launch {
                            learningRepository.resetAllProgress()
                            outerEventSink(
                                DatabaseDiagnosticsOuterEvent.ShowSnackbar("All lesson progress reset to 0%."),
                            )
                            Timber.i("DatabaseDiagnosticsSubPresenter: Reset all lesson progress")
                        }
                    }

                    DatabaseDiagnosticsUiEvent.SeedProgress -> {
                        scope.launch {
                            learningRepository.seedSampleProgress(lessonsPerCourse = 3)
                            outerEventSink(
                                DatabaseDiagnosticsOuterEvent.ShowSnackbar(
                                    "Seeded sample progress (first 3 lessons completed per course).",
                                ),
                            )
                            Timber.i("DatabaseDiagnosticsSubPresenter: Seeded sample lesson progress")
                        }
                    }

                    DatabaseDiagnosticsUiEvent.ClearSessions -> {
                        scope.launch {
                            sessionDao.clearAll()
                            outerEventSink(
                                DatabaseDiagnosticsOuterEvent.ShowSnackbar("All chat sessions and messages cleared."),
                            )
                            Timber.i("DatabaseDiagnosticsSubPresenter: Cleared all chat sessions and messages")
                        }
                    }
                }
            },
        )
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class DatabaseDiagnosticsSubPresenterFactory(
    private val learningRepository: LearningRepository,
    private val sessionDao: SessionDao,
) : SubPresenterFactory {
    override fun create(screen: SubScreen<*>): SubPresenter<*, *>? =
        when (screen) {
            is DatabaseDiagnosticsSubScreen -> {
                DatabaseDiagnosticsSubPresenter(learningRepository, sessionDao)
            }

            else -> {
                null
            }
        }
}
