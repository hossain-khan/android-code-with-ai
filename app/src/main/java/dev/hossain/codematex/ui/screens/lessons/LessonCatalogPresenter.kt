package dev.hossain.codematex.ui.screens.lessons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class LessonCatalogPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: LessonCatalogScreen,
    private val learningRepository: LearningRepository,
) : Presenter<LessonCatalogScreen.State> {
    @Composable
    override fun present(): LessonCatalogScreen.State {
        var courses by rememberRetained { mutableStateOf<List<LearningCourse>>(emptyList()) }
        var progress by rememberRetained { mutableStateOf<Map<String, CourseProgress>>(emptyMap()) }
        var isLoading by rememberRetained { mutableStateOf(true) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        var retryTrigger by rememberRetained { mutableIntStateOf(0) }

        LaunchedEffect(retryTrigger) {
            isLoading = true
            errorMessage = null
            learningRepository
                .getCourses()
                .catch { error ->
                    if (error is CancellationException) throw error
                    Timber.e(error, "LessonCatalogPresenter: Failed to load courses")
                    errorMessage = error.message ?: "Failed to load lessons"
                    isLoading = false
                }.collect { loadedCourses ->
                    courses = loadedCourses
                    isLoading = false
                    loadedCourses.forEach { course ->
                        launch {
                            learningRepository.observeCourseProgress(course.id).collect { value ->
                                progress = progress + (course.id to value)
                            }
                        }
                    }
                }
        }

        val eventSink: (LessonCatalogScreen.Event) -> Unit = { event ->
            when (event) {
                is LessonCatalogScreen.Event.OpenCourse -> {
                    navigator.goTo(ChapterScreen(event.courseId))
                }

                LessonCatalogScreen.Event.Retry -> {
                    retryTrigger++
                }

                LessonCatalogScreen.Event.Back -> {
                    navigator.pop()
                }
            }
        }

        return when {
            isLoading -> LessonCatalogScreen.State.Loading
            errorMessage != null -> LessonCatalogScreen.State.Error(errorMessage!!, eventSink)
            else -> LessonCatalogScreen.State.Success(courses, progress, eventSink)
        }
    }

    @CircuitInject(LessonCatalogScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        /** Creates a presenter for the supplied Circuit navigator and screen. */
        fun create(
            navigator: Navigator,
            screen: LessonCatalogScreen,
        ): LessonCatalogPresenter
    }
}
