package dev.hossain.codematex.ui.screens.home.sessions

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic

/**
 * Navigation events emitted to the parent coordinator screen.
 */
sealed interface RecentSessionsOuterEvent : SubCircuitOuterEvent {
    data class NavigateToSession(
        val topic: CodingTopic,
        val sessionId: String,
    ) : RecentSessionsOuterEvent

    data object NavigateToAllSessions : RecentSessionsOuterEvent
}

/**
 * SubScreen for recent chat sessions dashboard section.
 */
data class RecentSessionsSubScreen(
    val isExpanded: Boolean = false,
) : SubScreen<RecentSessionsOuterEvent>

/**
 * UI state for the recent sessions subcircuit.
 */
@Immutable
data class RecentSessionsSubState(
    val recentSessions: List<ChatSession>,
    val isExpanded: Boolean = false,
    val eventSink: (RecentSessionsUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * Internal UI events for the recent sessions subcircuit.
 */
sealed interface RecentSessionsUiEvent {
    data class SessionClicked(
        val topic: CodingTopic,
        val sessionId: String,
    ) : RecentSessionsUiEvent

    data object ViewAllSessions : RecentSessionsUiEvent
}
