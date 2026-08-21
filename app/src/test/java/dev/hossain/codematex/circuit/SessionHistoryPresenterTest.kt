package dev.hossain.codematex.circuit

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionHistoryPresenterTest {
    @Test
    fun computesAvailableTopicsAndFiltersCorrectly() =
        runTest {
            val sessions =
                listOf(
                    testSession(id = "s1", topic = CodingTopic.KOTLIN),
                    testSession(id = "s2", topic = CodingTopic.ANDROID),
                    testSession(id = "s3", topic = CodingTopic.KOTLIN),
                )
            val fakeRepo = FakeChatSessionRepository(sessions = sessions)
            val navigator = FakeNavigator(SessionHistoryScreen)
            val presenter =
                SessionHistoryPresenter(
                    navigator = navigator,
                    screen = SessionHistoryScreen,
                    sessionRepository = fakeRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as SessionHistoryScreen.State.Success
                assertEquals(3, state.allSessions.size)
                assertEquals(3, state.sessions.size)
                assertEquals(listOf(CodingTopic.KOTLIN, CodingTopic.ANDROID), state.availableTopics)
                assertNull(state.selectedTopic)

                // Select Kotlin filter
                state.eventSink(SessionHistoryScreen.Event.SelectTopicFilter(CodingTopic.KOTLIN))
                val filteredState = expectMostRecentItem() as SessionHistoryScreen.State.Success
                assertEquals(CodingTopic.KOTLIN, filteredState.selectedTopic)
                assertEquals(2, filteredState.sessions.size)

                // Toggle Kotlin filter off
                filteredState.eventSink(SessionHistoryScreen.Event.SelectTopicFilter(CodingTopic.KOTLIN))
                val allState = expectMostRecentItem() as SessionHistoryScreen.State.Success
                assertNull(allState.selectedTopic)
                assertEquals(3, allState.sessions.size)
            }
        }

    @Test
    fun openSession_navigatesToChatScreen() =
        runTest {
            val sessions = listOf(testSession(id = "s1", topic = CodingTopic.KOTLIN))
            val fakeRepo = FakeChatSessionRepository(sessions = sessions)
            val navigator = FakeNavigator(SessionHistoryScreen)
            val presenter =
                SessionHistoryPresenter(
                    navigator = navigator,
                    screen = SessionHistoryScreen,
                    sessionRepository = fakeRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as SessionHistoryScreen.State.Success
                state.eventSink(SessionHistoryScreen.Event.OpenSession("s1"))
                assertEquals(
                    ChatScreen(topic = CodingTopic.KOTLIN, sessionId = "s1"),
                    navigator.awaitNextScreen(),
                )
            }
        }

    @Test
    fun back_popsNavigator() =
        runTest {
            val fakeRepo = FakeChatSessionRepository(sessions = emptyList())
            val navigator = FakeNavigator(SessionHistoryScreen)
            val presenter =
                SessionHistoryPresenter(
                    navigator = navigator,
                    screen = SessionHistoryScreen,
                    sessionRepository = fakeRepo,
                )

            presenter.test {
                val state = expectMostRecentItem() as SessionHistoryScreen.State.Success
                state.eventSink(SessionHistoryScreen.Event.Back)
                navigator.awaitPop()
            }
        }
}
