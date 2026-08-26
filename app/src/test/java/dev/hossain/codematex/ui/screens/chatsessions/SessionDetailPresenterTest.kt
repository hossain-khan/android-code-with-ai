package dev.hossain.codematex.ui.screens.chatsessions

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.FakeChatSessionRepository
import dev.hossain.codematex.data.repository.testSession
import dev.hossain.codematex.ui.screens.chat.ChatScreen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [SessionDetailPresenter].
 */
class SessionDetailPresenterTest {
    private val testMessages =
        listOf(
            ChatMessage.User("How do I use data classes in Kotlin?"),
            ChatMessage.Agent("Data classes automatically generate equals, hashCode, and toString."),
        )

    @Test
    fun `given session exists - emits success with session and message`() =
        runTest {
            val session = testSession(id = "s1", topic = CodingTopic.KOTLIN)
            val fakeRepo = FakeChatSessionRepository(sessions = listOf(session), messages = testMessages)
            val navigator = FakeNavigator(SessionDetailScreen("s1"))
            val presenter = SessionDetailPresenter(navigator, SessionDetailScreen("s1"), fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as SessionDetailScreen.State.Success
                assertEquals(session, state.session)
                assertEquals(testMessages, state.messages)
            }
        }

    @Test
    fun `given session missing - emits not found state with session id`() =
        runTest {
            val fakeRepo = FakeChatSessionRepository(sessions = emptyList())
            val navigator = FakeNavigator(SessionDetailScreen("unknown"))
            val presenter =
                SessionDetailPresenter(navigator, SessionDetailScreen("unknown"), fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as SessionDetailScreen.State.NotFound
                assertEquals("unknown", state.sessionId)
            }
        }

    @Test
    fun `given session load error - emits error state and retry succeeds`() =
        runTest {
            val session = testSession(id = "s1", topic = CodingTopic.KOTLIN)
            val fakeRepo =
                FakeChatSessionRepository(
                    sessions = listOf(session),
                    messages = testMessages,
                    getSessionException = java.io.IOException("Database unavailable"),
                )
            val navigator = FakeNavigator(SessionDetailScreen("s1"))
            val presenter = SessionDetailPresenter(navigator, SessionDetailScreen("s1"), fakeRepo)

            presenter.test {
                val errorState = expectMostRecentItem() as SessionDetailScreen.State.Error
                assertEquals("Database unavailable", errorState.message)

                // Clear the error and trigger retry
                fakeRepo.getSessionException = null
                errorState.eventSink(SessionDetailScreen.Event.Retry)

                val successState = expectMostRecentItem() as SessionDetailScreen.State.Success
                assertEquals(session, successState.session)
                assertEquals(testMessages, successState.messages)
            }
        }

    @Test
    fun `given resume session event - navigates to chat screen`() =
        runTest {
            val session = testSession(id = "s1", topic = CodingTopic.PYTHON)
            val fakeRepo = FakeChatSessionRepository(sessions = listOf(session), messages = testMessages)
            val navigator = FakeNavigator(SessionDetailScreen("s1"))
            val presenter = SessionDetailPresenter(navigator, SessionDetailScreen("s1"), fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as SessionDetailScreen.State.Success
                state.eventSink(SessionDetailScreen.Event.ResumeSession)
                assertEquals(
                    ChatScreen(topic = CodingTopic.PYTHON, sessionId = "s1"),
                    navigator.awaitNextScreen(),
                )
            }
        }

    @Test
    fun `given delete session event - deletes session and pops navigator`() =
        runTest {
            val session = testSession(id = "s1")
            val fakeRepo = FakeChatSessionRepository(sessions = listOf(session))
            val navigator = FakeNavigator(SessionDetailScreen("s1"))
            val presenter = SessionDetailPresenter(navigator, SessionDetailScreen("s1"), fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as SessionDetailScreen.State.Success
                state.eventSink(SessionDetailScreen.Event.DeleteSession)
                navigator.awaitPop()
                assertEquals(listOf("s1"), fakeRepo.deletedSessionIds)
            }
        }

    @Test
    fun `given back event - pops navigator`() =
        runTest {
            val session = testSession(id = "s1")
            val fakeRepo = FakeChatSessionRepository(sessions = listOf(session))
            val navigator = FakeNavigator(SessionDetailScreen("s1"))
            val presenter = SessionDetailPresenter(navigator, SessionDetailScreen("s1"), fakeRepo)

            presenter.test {
                val state = expectMostRecentItem() as SessionDetailScreen.State.Success
                state.eventSink(SessionDetailScreen.Event.Back)
                navigator.awaitPop()
            }
        }
}
