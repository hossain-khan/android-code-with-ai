package dev.hossain.codematex.ui.screens.home.topics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.subcircuit.SubCircuitInject
import com.slack.circuit.subcircuit.SubPresenter
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.course.LearningRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

/**
 * SubPresenter providing selectable coding topics and identifying which topics have interactive courses.
 */
@AssistedInject
class TopicsGridSubPresenter(
    @Assisted private val screen: TopicsGridSubScreen,
    private val learningRepository: LearningRepository,
) : SubPresenter<TopicsGridOuterEvent, TopicsGridSubState> {
    @Composable
    override fun present(outerEventSink: (TopicsGridOuterEvent) -> Unit): TopicsGridSubState {
        var topicsWithCourses by remember { mutableStateOf<Set<CodingTopic>>(emptySet()) }

        LaunchedEffect(Unit) {
            topicsWithCourses = learningRepository.getTopicsWithCourses()
        }

        return TopicsGridSubState(
            topics = CodingTopic.selectableEntries,
            topicsWithCourses = topicsWithCourses,
            isCompact = screen.isCompact,
            eventSink = { event ->
                when (event) {
                    is TopicsGridUiEvent.TopicSelected -> {
                        outerEventSink(TopicsGridOuterEvent.NavigateToTopic(event.topic))
                    }
                }
            },
        )
    }

    @SubCircuitInject(TopicsGridSubScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(screen: TopicsGridSubScreen): TopicsGridSubPresenter
    }
}
