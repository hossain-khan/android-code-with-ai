package dev.hossain.codematex.circuit

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [SessionHistoryPresenter].
 */
class SessionHistoryPresenterTest {
    @Test
    fun `given sessions with multiple topics - computes available topics and filters correctly`() =
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
    fun `given open session event - navigates to chat screen`() =
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
    fun `given back event - pops navigator`() =
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
