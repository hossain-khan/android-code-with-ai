package dev.hossain.codematex.ui.screens.chat.course

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.subcircuit.SubCircuitInject
import com.slack.circuit.subcircuit.SubPresenter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.repository.UserPreferencesStore
import dev.hossain.codematex.data.repository.course.LearningRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch

/**
 * SubPresenter observing contextual learning courses for the active coding topic
 * and managing dismissal preferences.
 */
@AssistedInject
class CourseRecommendationSubPresenter(
    @Assisted private val screen: CourseRecommendationSubScreen,
    private val learningRepository: LearningRepository,
    private val userPreferencesStore: UserPreferencesStore,
) : SubPresenter<CourseRecommendationOuterEvent, CourseRecommendationSubState> {
    @Composable
    override fun present(outerEventSink: (CourseRecommendationOuterEvent) -> Unit): CourseRecommendationSubState {
        var course by rememberRetained { mutableStateOf<LearningCourse?>(null) }
        var dismissedTopics by rememberRetained { mutableStateOf<Set<String>>(emptySet()) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            userPreferencesStore.dismissedCourseBannerTopicsFlow.collect { dismissed ->
                dismissedTopics = dismissed
            }
        }

        LaunchedEffect(screen.topic, screen.showCourseBanner, dismissedTopics) {
            val isDismissed = dismissedTopics.contains(screen.topic.name)
            if (screen.showCourseBanner && !isDismissed) {
                course = learningRepository.getCourseForTopic(screen.topic)
            } else {
                course = null
            }
        }

        return CourseRecommendationSubState(
            topic = screen.topic,
            course = course,
            eventSink = { event ->
                when (event) {
                    is CourseRecommendationUiEvent.StartCourse -> {
                        scope.launch {
                            userPreferencesStore.dismissCourseBanner(screen.topic)
                            course = null
                        }
                        outerEventSink(CourseRecommendationOuterEvent.NavigateToCourse(event.courseId))
                    }

                    CourseRecommendationUiEvent.Dismiss -> {
                        scope.launch {
                            userPreferencesStore.dismissCourseBanner(screen.topic)
                            course = null
                        }
                    }
                }
            },
        )
    }

    @SubCircuitInject(CourseRecommendationSubScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(screen: CourseRecommendationSubScreen): CourseRecommendationSubPresenter
    }
}
