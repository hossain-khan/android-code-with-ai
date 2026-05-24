package dev.hossain.codematex.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch

@AssistedInject
class SessionDetailPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: SessionDetailScreen,
    private val sessionRepository: ChatSessionRepository,
) : Presenter<SessionDetailScreen.State> {
    @Composable
    override fun present(): SessionDetailScreen.State {
        var session by rememberRetained { mutableStateOf<ChatSession?>(null) }
        var isLoading by rememberRetained { mutableStateOf(true) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(screen.sessionId) {
            session = sessionRepository.getSession(screen.sessionId)
            isLoading = false
        }

        val eventSink: (SessionDetailScreen.Event) -> Unit = { event ->
            when (event) {
                SessionDetailScreen.Event.ResumeSession -> {
                    session?.let {
                        navigator.goTo(ChatScreen(topic = it.topic, sessionId = it.id))
                    }
                }

                SessionDetailScreen.Event.DeleteSession -> {
                    scope.launch {
                        sessionRepository.deleteSession(screen.sessionId)
                        navigator.pop()
                    }
                }

                SessionDetailScreen.Event.Back -> {
                    navigator.pop()
                }
            }
        }

        return if (isLoading) {
            SessionDetailScreen.State.Loading
        } else if (session != null) {
            SessionDetailScreen.State.Success(
                session = session!!,
                eventSink = eventSink,
            )
        } else {
            SessionDetailScreen.State.Loading
        }
    }

    @CircuitInject(SessionDetailScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: SessionDetailScreen,
        ): SessionDetailPresenter
    }
}
