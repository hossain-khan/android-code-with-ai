package dev.hossain.codematex.circuit

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.ChatMessage
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDetailPresenterTest {
    private val testMessages =
        listOf(
            ChatMessage.User("How do I use data classes in Kotlin?"),
            ChatMessage.Agent("Data classes automatically generate equals, hashCode, and toString."),
        )

    @Test
    fun whenSessionExists_emitsSuccessWithSessionAndMessages() =
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
    fun whenSessionMissing_staysLoading() =
        runTest {
            val fakeRepo = FakeChatSessionRepository(sessions = emptyList())
            val navigator = FakeNavigator(SessionDetailScreen("unknown"))
            val presenter = SessionDetailPresenter(navigator, SessionDetailScreen("unknown"), fakeRepo)

            presenter.test {
                assertEquals(SessionDetailScreen.State.Loading, expectMostRecentItem())
            }
        }

    @Test
    fun resumeSession_navigatesToChatScreen() =
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
    fun deleteSession_deletesSessionAndPopsNavigator() =
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
    fun back_popsNavigator() =
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
