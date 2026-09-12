package dev.hossain.codematex.ui.screens.home.topics

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.CodingTopic

/**
 * Navigation events emitted to the parent coordinator screen.
 */
sealed interface TopicsGridOuterEvent : SubCircuitOuterEvent {
    data class NavigateToTopic(
        val topic: CodingTopic,
    ) : TopicsGridOuterEvent
}

/**
 * SubScreen for coding topics selection section.
 */
data class TopicsGridSubScreen(
    val isCompact: Boolean = false,
) : SubScreen<TopicsGridOuterEvent>

/**
 * UI state for the topics section.
 */
@Immutable
data class TopicsGridSubState(
    val topics: List<CodingTopic> = CodingTopic.entries,
    val topicsWithCourses: Set<CodingTopic> = emptySet(),
    val isCompact: Boolean = false,
    val eventSink: (TopicsGridUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * UI events handled within the topics section.
 */
sealed interface TopicsGridUiEvent {
    data class TopicSelected(
        val topic: CodingTopic,
    ) : TopicsGridUiEvent
}
