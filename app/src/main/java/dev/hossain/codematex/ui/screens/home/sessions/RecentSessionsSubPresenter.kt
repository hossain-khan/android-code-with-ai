package dev.hossain.codematex.ui.screens.home.sessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.subcircuit.SubCircuitInject
import com.slack.circuit.subcircuit.SubPresenter
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.catch
import timber.log.Timber

/**
 * SubPresenter that observes stored chat sessions and emits the top 5 recent sessions for the dashboard.
 */
@AssistedInject
class RecentSessionsSubPresenter(
    @Assisted private val screen: RecentSessionsSubScreen,
    private val sessionRepository: ChatSessionRepository,
) : SubPresenter<RecentSessionsOuterEvent, RecentSessionsSubState> {
    @Composable
    override fun present(outerEventSink: (RecentSessionsOuterEvent) -> Unit): RecentSessionsSubState {
        var recentSessions by remember { mutableStateOf<List<ChatSession>>(emptyList()) }

        LaunchedEffect(Unit) {
            sessionRepository
                .getAllSessions()
                .catch { Timber.e(it, "RecentSessionsSubPresenter: Failed to load sessions") }
                .collect { sessions ->
                    recentSessions = sessions.take(5)
                }
        }

        return RecentSessionsSubState(
            recentSessions = recentSessions,
            isExpanded = screen.isExpanded,
            eventSink = { event ->
                when (event) {
                    is RecentSessionsUiEvent.SessionClicked -> {
                        outerEventSink(RecentSessionsOuterEvent.NavigateToSession(event.topic, event.sessionId))
                    }

                    RecentSessionsUiEvent.ViewAllSessions -> {
                        outerEventSink(RecentSessionsOuterEvent.NavigateToAllSessions)
                    }
                }
            },
        )
    }

    @SubCircuitInject(RecentSessionsSubScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(screen: RecentSessionsSubScreen): RecentSessionsSubPresenter
    }
}
