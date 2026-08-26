package dev.hossain.codematex.ui.screens.chatsessions

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.FakeChatSessionRepository
import dev.hossain.codematex.data.repository.testSession
import dev.hossain.codematex.ui.screens.chat.ChatScreen
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

    @Test
    fun `given repository error - emits error state and retry succeeds`() =
        runTest {
            val sessions = listOf(testSession(id = "s1", topic = CodingTopic.KOTLIN))
            val fakeRepo =
                FakeChatSessionRepository(
                    sessions = sessions,
                    getException = java.io.IOException("Database read failed"),
                )
            val navigator = FakeNavigator(SessionHistoryScreen)
            val presenter =
                SessionHistoryPresenter(
                    navigator = navigator,
                    screen = SessionHistoryScreen,
                    sessionRepository = fakeRepo,
                )

            presenter.test {
                val errorState = expectMostRecentItem() as SessionHistoryScreen.State.Error
                assertEquals("Database read failed", errorState.message)

                // Clear error and retry
                fakeRepo.getException = null
                errorState.eventSink(SessionHistoryScreen.Event.Retry)

                val successState = expectMostRecentItem() as SessionHistoryScreen.State.Success
                assertEquals(1, successState.allSessions.size)
                assertEquals("s1", successState.allSessions.first().id)
            }
        }

    @Test
    fun `given selected topic filter disappears - effective topic resets to null without mutation`() =
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
                // Select topic that exists
                state.eventSink(SessionHistoryScreen.Event.SelectTopicFilter(CodingTopic.KOTLIN))
                val filteredState = expectMostRecentItem() as SessionHistoryScreen.State.Success
                assertEquals(CodingTopic.KOTLIN, filteredState.selectedTopic)

                // Select topic that does not exist in sessions
                filteredState.eventSink(SessionHistoryScreen.Event.SelectTopicFilter(CodingTopic.RUST))
                val resetFilterState = expectMostRecentItem() as SessionHistoryScreen.State.Success
                assertNull(resetFilterState.selectedTopic)
                assertEquals(1, resetFilterState.sessions.size)
            }
        }
}
