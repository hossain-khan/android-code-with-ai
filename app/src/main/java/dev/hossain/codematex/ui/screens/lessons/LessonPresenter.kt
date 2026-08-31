package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.popUntil
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.model.LessonStatus
import dev.hossain.codematex.data.repository.course.LearningRepository
import dev.hossain.codematex.ui.screens.chat.ChatScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class LessonPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: LessonScreen,
    private val learningRepository: LearningRepository,
) : Presenter<LessonScreen.State> {
    @Composable
    override fun present(): LessonScreen.State {
        var lesson by rememberRetained { mutableStateOf<LearningLesson?>(null) }
        var course by rememberRetained { mutableStateOf<LearningCourse?>(null) }
        var isCompleted by rememberRetained { mutableStateOf(false) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(screen.lessonId) {
            try {
                val loadedLesson = learningRepository.getLesson(screen.lessonId)
                lesson = loadedLesson
                course = learningRepository.getCourseForLesson(screen.lessonId)
                if (loadedLesson != null) {
                    if (course != null) {
                        learningRepository.markLessonStarted(loadedLesson.id)
                        learningRepository
                            .observeLessonStatus(loadedLesson.id)
                            .collect { status ->
                                isCompleted = status == LessonStatus.COMPLETED
                            }
                    } else {
                        errorMessage = "Course for lesson '${screen.lessonId}' not found"
                    }
                } else {
                    errorMessage = "Lesson '${screen.lessonId}' not found"
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.e(error, "LessonPresenter: Failed to load lesson")
                errorMessage = error.message ?: "Failed to load lesson"
            }
        }

        val eventSink: (LessonScreen.Event) -> Unit = { event ->
            when (event) {
                LessonScreen.Event.MarkCompleted -> {
                    scope.launch {
                        learningRepository.markLessonCompleted(screen.lessonId)
                        isCompleted = true
                    }
                }

                LessonScreen.Event.NextLesson -> {
                    nextLessonId(course, lesson?.id)?.let { nextId ->
                        navigator.popUntil { it !is LessonScreen }
                        navigator.goTo(LessonScreen(nextId))
                    }
                }

                LessonScreen.Event.AskAi -> {
                    val currentLesson = lesson
                    if (currentLesson != null) {
                        val currentCourse = course
                        val topic = currentCourse?.topic ?: CodingTopic.KOTLIN
                        val courseTitle = currentCourse?.title ?: topic.displayName
                        val prompt =
                            """I am studying the lesson "${currentLesson.title}" in the $courseTitle course.
                            |
                            |Lesson summary:
                            |${currentLesson.summary}
                            |
                            |Can you explain this concept in depth, show a practical code example, and give me a quick exercise to test my understanding?
                            """.trimMargin()

                        navigator.goTo(
                            ChatScreen(
                                topic = topic,
                                saveToHistory = false,
                                initialPrompt = prompt,
                                showCourseBanner = false,
                            ),
                        )
                    }
                }

                LessonScreen.Event.Back -> {
                    navigator.pop()
                }
            }
        }

        val nextLesson = nextLessonId(course, lesson?.id)
        return when {
            lesson != null && course != null -> LessonScreen.State.Success(lesson!!, course!!, isCompleted, nextLesson, eventSink)
            errorMessage != null -> LessonScreen.State.NotFound(errorMessage!!, eventSink)
            else -> LessonScreen.State.Loading
        }
    }

    private fun nextLessonId(
        course: LearningCourse?,
        currentId: String?,
    ): String? {
        val lessons = course?.chapters?.flatMap { it.lessons }.orEmpty()
        val index = lessons.indexOfFirst { it.id == currentId }
        return lessons.getOrNull(index + 1)?.id
    }

    @CircuitInject(LessonScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        /** Creates a presenter for the supplied Circuit navigator and screen. */
        fun create(
            navigator: Navigator,
            screen: LessonScreen,
        ): LessonPresenter
    }
}
