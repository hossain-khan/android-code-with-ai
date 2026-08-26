package dev.hossain.codematex.ui.screens.chatsessions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber

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
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        var retryTrigger by rememberRetained { mutableIntStateOf(0) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(retryTrigger) {
            isLoading = true
            errorMessage = null
            sessionRepository
                .getAllSessions()
                .catch { e ->
                    if (e is CancellationException) throw e
                    Timber.e(e, "SessionHistoryPresenter: Error loading chat history")
                    errorMessage = e.message ?: "Failed to load chat history"
                    isLoading = false
                }.collect { list ->
                    sessions = list
                    isLoading = false
                }
        }

        val availableTopics =
            remember(sessions) {
                sessions.map { it.topic }.distinct()
            }

        // Derive effective filter state without mutating snapshot state during composition body execution
        val effectiveTopic =
            remember(selectedTopic, availableTopics) {
                if (selectedTopic != null && selectedTopic in availableTopics) {
                    selectedTopic
                } else {
                    null
                }
            }

        val filteredSessions =
            remember(sessions, effectiveTopic) {
                if (effectiveTopic == null) {
                    sessions
                } else {
                    sessions.filter { it.topic == effectiveTopic }
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

                SessionHistoryScreen.Event.Retry -> {
                    retryTrigger++
                }

                SessionHistoryScreen.Event.Back -> {
                    navigator.pop()
                }
            }
        }

        return when {
            isLoading -> {
                SessionHistoryScreen.State.Loading
            }

            errorMessage != null -> {
                SessionHistoryScreen.State.Error(errorMessage!!, eventSink)
            }

            else -> {
                SessionHistoryScreen.State.Success(
                    sessions = filteredSessions,
                    allSessions = sessions,
                    availableTopics = availableTopics,
                    selectedTopic = effectiveTopic,
                    eventSink = eventSink,
                )
            }
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
