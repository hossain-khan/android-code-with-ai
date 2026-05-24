package dev.hossain.codematex.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import com.slack.circuit.codegen.annotations.CircuitInject
import kotlinx.coroutines.flow.catch

@AssistedInject
class SessionHistoryPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: SessionHistoryScreen,
    private val sessionRepository: ChatSessionRepository,
) : Presenter<SessionHistoryScreen.State> {

    @Composable
    override fun present(): SessionHistoryScreen.State {
        var sessions by rememberRetained { mutableStateOf<List<ChatSession>>(emptyList()) }
        var isLoading by rememberRetained { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            sessionRepository.getAllSessions()
                .catch { isLoading = false }
                .collect { list ->
                    sessions = list
                    isLoading = false
                }
        }

        val eventSink: (SessionHistoryScreen.Event) -> Unit = { event ->
            when (event) {
                is SessionHistoryScreen.Event.OpenSession -> {
                    val session = sessions.find { it.id == event.sessionId }
                    if (session != null) {
                        navigator.goTo(ChatScreen(topic = session.topic, sessionId = session.id))
                    }
                }
                is SessionHistoryScreen.Event.DeleteSession -> {
                    // TODO: Implement delete
                }
            }
        }

        return if (isLoading) {
            SessionHistoryScreen.State.Loading
        } else {
            SessionHistoryScreen.State.Success(
                sessions = sessions,
                eventSink = eventSink,
            )
        }
    }

    @CircuitInject(SessionHistoryScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: SessionHistoryScreen,
        ): SessionHistoryPresenter
    }
}
