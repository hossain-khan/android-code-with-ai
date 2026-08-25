package dev.hossain.codematex.ui.screens.chatsessions

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
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.hossain.codematex.ui.screens.chat.ChatScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@AssistedInject
class SessionHistoryPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: SessionHistoryScreen,
    private val sessionRepository: ChatSessionRepository,
) : Presenter<SessionHistoryScreen.State> {
    @Composable
    override fun present(): SessionHistoryScreen.State {
        var sessions by rememberRetained { mutableStateOf<List<ChatSession>>(emptyList()) }
        var selectedTopic by rememberRetained { mutableStateOf<CodingTopic?>(null) }
        var isLoading by rememberRetained { mutableStateOf(true) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            sessionRepository
                .getAllSessions()
                .catch { isLoading = false }
                .collect { list ->
                    sessions = list
                    isLoading = false
                }
        }

        val availableTopics =
            remember(sessions) {
                sessions.map { it.topic }.distinct()
            }

        // If the selected topic no longer exists in sessions (e.g. after deletion), reset filter
        if (selectedTopic != null && !availableTopics.contains(selectedTopic)) {
            selectedTopic = null
        }

        val filteredSessions =
            remember(sessions, selectedTopic) {
                if (selectedTopic == null) {
                    sessions
                } else {
                    sessions.filter { it.topic == selectedTopic }
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
                    scope.launch {
                        sessionRepository.deleteSession(event.sessionId)
                    }
                }

                is SessionHistoryScreen.Event.SelectTopicFilter -> {
                    selectedTopic = if (selectedTopic == event.topic) null else event.topic
                }

                SessionHistoryScreen.Event.Back -> {
                    navigator.pop()
                }
            }
        }

        return if (isLoading) {
            SessionHistoryScreen.State.Loading
        } else {
            SessionHistoryScreen.State.Success(
                sessions = filteredSessions,
                allSessions = sessions,
                availableTopics = availableTopics,
                selectedTopic = selectedTopic,
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
