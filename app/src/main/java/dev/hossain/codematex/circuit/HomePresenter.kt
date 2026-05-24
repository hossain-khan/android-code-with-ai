package dev.hossain.codematex.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.hossain.codematex.data.repository.ModelRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.catch

@AssistedInject
class HomePresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: HomeScreen,
    private val sessionRepository: ChatSessionRepository,
    private val modelRepository: ModelRepository,
) : Presenter<HomeScreen.State> {
    @Composable
    override fun present(): HomeScreen.State {
        var recentSessions by rememberRetained { mutableStateOf<List<ChatSession>>(emptyList()) }
        var isLoading by rememberRetained { mutableStateOf(true) }

        val hasDownloadedModel = modelRepository.getSelectedModel() != null

        LaunchedEffect(Unit) {
            sessionRepository
                .getAllSessions()
                .catch { isLoading = false }
                .collect { sessions ->
                    recentSessions = sessions.take(5)
                    isLoading = false
                }
        }

        val eventSink: (HomeScreen.Event) -> Unit = { event ->
            when (event) {
                is HomeScreen.Event.TopicSelected -> {
                    navigator.goTo(ChatScreen(topic = event.topic))
                }

                is HomeScreen.Event.SessionClicked -> {
                    val session = recentSessions.find { it.id == event.sessionId }
                    if (session != null) {
                        navigator.goTo(ChatScreen(topic = session.topic, sessionId = session.id))
                    }
                }

                HomeScreen.Event.ManageModels -> {
                    navigator.goTo(ModelPickerScreen)
                }

                HomeScreen.Event.ViewAllSessions -> {
                    navigator.goTo(SessionHistoryScreen)
                }
            }
        }

        return if (isLoading) {
            HomeScreen.State.Loading
        } else {
            HomeScreen.State.Success(
                recentSessions = recentSessions,
                topics = CodingTopic.entries,
                hasDownloadedModel = hasDownloadedModel,
                eventSink = eventSink,
            )
        }
    }

    @CircuitInject(HomeScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: HomeScreen,
        ): HomePresenter
    }
}
