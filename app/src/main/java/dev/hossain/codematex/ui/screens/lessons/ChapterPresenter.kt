package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.CourseProgress
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.repository.LearningRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class ChapterPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ChapterScreen,
    private val learningRepository: LearningRepository,
) : Presenter<ChapterScreen.State> {
    @Composable
    override fun present(): ChapterScreen.State {
        var course by rememberRetained { mutableStateOf<LearningCourse?>(null) }
        var progress by rememberRetained { mutableStateOf<CourseProgress?>(null) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        LaunchedEffect(screen.courseId) {
            try {
                course = learningRepository.getCourse(screen.courseId)
                if (course != null) {
                    learningRepository.observeCourseProgress(screen.courseId).collect {
                        progress = it
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.e(error, "ChapterPresenter: Failed to load course")
                errorMessage = error.message ?: "Failed to load course"
            }
        }

        val eventSink: (ChapterScreen.Event) -> Unit = { event ->
            when (event) {
                is ChapterScreen.Event.OpenLesson -> {
                    navigator.goTo(LessonScreen(event.lessonId))
                }

                ChapterScreen.Event.ResetProgress -> {
                    scope.launch { learningRepository.resetCourseProgress(screen.courseId) }
                }

                ChapterScreen.Event.Back -> {
                    navigator.pop()
                }
            }
        }

        return when {
            course != null && progress != null -> ChapterScreen.State.Success(course!!, progress!!, eventSink)
            errorMessage != null -> ChapterScreen.State.NotFound(errorMessage!!, eventSink)
            course == null -> ChapterScreen.State.Loading
            else -> ChapterScreen.State.Loading
        }
    }

    @CircuitInject(ChapterScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        /** Creates a presenter for the supplied Circuit navigator and screen. */
        fun create(
            navigator: Navigator,
            screen: ChapterScreen,
        ): ChapterPresenter
    }
}
