package dev.hossain.codematex.ui.screens.home.topics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.course.LearningRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

class TopicsGridSubPresenter(
    private val screen: TopicsGridSubScreen,
    private val learningRepository: LearningRepository,
) : SubPresenter<TopicsGridOuterEvent, TopicsGridSubState> {
    @Composable
    override fun present(outerEventSink: (TopicsGridOuterEvent) -> Unit): TopicsGridSubState {
        var topicsWithCourses by remember { mutableStateOf<Set<CodingTopic>>(emptySet()) }

        LaunchedEffect(Unit) {
            topicsWithCourses = learningRepository.getTopicsWithCourses()
        }

        return TopicsGridSubState(
            topics = CodingTopic.entries,
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
}

@ContributesIntoSet(AppScope::class)
@Inject
class TopicsGridSubPresenterFactory(
    private val learningRepository: LearningRepository,
) : SubPresenterFactory {
    override fun create(screen: SubScreen<*>): SubPresenter<*, *>? =
        when (screen) {
            is TopicsGridSubScreen -> TopicsGridSubPresenter(screen, learningRepository)
            else -> null
        }
}
