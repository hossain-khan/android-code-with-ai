package dev.hossain.codematex.ui.screens.chatsessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.hossain.codematex.ui.screens.chat.ChatScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

@AssistedInject
class SessionDetailPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: SessionDetailScreen,
    private val sessionRepository: ChatSessionRepository,
) : Presenter<SessionDetailScreen.State> {
    @Composable
    override fun present(): SessionDetailScreen.State {
        var session by rememberRetained { mutableStateOf<ChatSession?>(null) }
        var messages by rememberRetained { mutableStateOf<List<ChatMessage>>(emptyList()) }
        var isLoading by rememberRetained { mutableStateOf(true) }
        var isNotFound by rememberRetained { mutableStateOf(false) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        var retryTrigger by rememberRetained { mutableIntStateOf(0) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(screen.sessionId, retryTrigger) {
            isLoading = true
            isNotFound = false
            errorMessage = null
            try {
                val loadedSession = sessionRepository.getSession(screen.sessionId)
                if (loadedSession == null) {
                    isNotFound = true
                } else {
                    session = loadedSession
                    messages = sessionRepository.getMessages(screen.sessionId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "SessionDetailPresenter: Failed to load session ${screen.sessionId}")
                errorMessage = e.message ?: "Failed to load session details"
            } finally {
                isLoading = false
            }
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

                SessionDetailScreen.Event.Retry -> {
                    retryTrigger++
                }

                SessionDetailScreen.Event.Back -> {
                    navigator.pop()
                }
            }
        }

        return when {
            isLoading -> {
                SessionDetailScreen.State.Loading
            }

            errorMessage != null -> {
                SessionDetailScreen.State.Error(errorMessage!!, eventSink)
            }

            isNotFound -> {
                SessionDetailScreen.State.NotFound(screen.sessionId, eventSink)
            }

            session != null -> {
                SessionDetailScreen.State.Success(
                    session = session!!,
                    messages = messages,
                    eventSink = eventSink,
                )
            }

            else -> {
                SessionDetailScreen.State.NotFound(screen.sessionId, eventSink)
            }
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
