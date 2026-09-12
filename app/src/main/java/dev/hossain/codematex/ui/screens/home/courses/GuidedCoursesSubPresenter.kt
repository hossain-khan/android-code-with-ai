package dev.hossain.codematex.ui.screens.home.courses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.repository.course.LearningRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * SubPresenter that loads available interactive courses for the guided courses section on the home screen.
 */
class GuidedCoursesSubPresenter(
    private val learningRepository: LearningRepository,
) : SubPresenter<GuidedCoursesOuterEvent, GuidedCoursesSubState> {
    @Composable
    override fun present(outerEventSink: (GuidedCoursesOuterEvent) -> Unit): GuidedCoursesSubState {
        var availableCourses by rememberRetained { mutableStateOf<List<LearningCourse>>(emptyList()) }

        LaunchedEffect(Unit) {
            learningRepository.getCourses().collect { courses ->
                availableCourses = courses
            }
        }

        return GuidedCoursesSubState(
            availableCourses = availableCourses,
            eventSink = { event ->
                when (event) {
                    is GuidedCoursesUiEvent.CourseClicked -> {
                        outerEventSink(GuidedCoursesOuterEvent.NavigateToCourse(event.courseId))
                    }

                    GuidedCoursesUiEvent.ViewAllCourses -> {
                        outerEventSink(GuidedCoursesOuterEvent.NavigateToAllCourses)
                    }
                }
            },
        )
    }
}

/**
 * Factory providing [GuidedCoursesSubPresenter] for [GuidedCoursesSubScreen].
 */
@ContributesIntoSet(AppScope::class)
@Inject
class GuidedCoursesSubPresenterFactory(
    private val learningRepository: LearningRepository,
) : SubPresenterFactory {
    override fun create(screen: SubScreen<*>): SubPresenter<*, *>? =
        when (screen) {
            is GuidedCoursesSubScreen -> GuidedCoursesSubPresenter(learningRepository)
            else -> null
        }
}
