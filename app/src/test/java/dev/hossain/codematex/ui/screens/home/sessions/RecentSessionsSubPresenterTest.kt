package dev.hossain.codematex.ui.screens.home.sessions

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.model.ChatSession
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.repository.FakeChatSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecentSessionsSubPresenterTest {
    private val testSessions =
        (1..7).map { index ->
            ChatSession(
                id = "s-$index",
                topic = CodingTopic.KOTLIN,
                title = "Session $index",
                summary = "Summary $index",
                messageCount = index,
                lastActiveAt = System.currentTimeMillis() - (index * 500L),
                modelUsed = "test-model",
            )
        }

    @Test
    fun `emits up to 5 recent sessions from repository`() =
        runTest {
            val fakeRepo = FakeChatSessionRepository(sessions = testSessions)
            val presenter = RecentSessionsSubPresenter(RecentSessionsSubScreen(), sessionRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.recentSessions).hasSize(5)
                assertThat(state.recentSessions.map { it.id }).containsExactly("s-1", "s-2", "s-3", "s-4", "s-5")
                assertThat(state.isExpanded).isFalse()
            }
        }

    @Test
    fun `reflects isExpanded flag from screen`() =
        runTest {
            val fakeRepo = FakeChatSessionRepository(sessions = testSessions)
            val presenter = RecentSessionsSubPresenter(RecentSessionsSubScreen(isExpanded = true), sessionRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.isExpanded).isTrue()
            }
        }

    @Test
    fun `SessionClicked emits NavigateToSession outer event`() =
        runTest {
            val fakeRepo = FakeChatSessionRepository(sessions = testSessions)
            val presenter = RecentSessionsSubPresenter(RecentSessionsSubScreen(), sessionRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(RecentSessionsUiEvent.SessionClicked(CodingTopic.KOTLIN, "s-1"))

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(
                    RecentSessionsOuterEvent.NavigateToSession(CodingTopic.KOTLIN, "s-1"),
                )
            }
        }

    @Test
    fun `ViewAllSessions emits NavigateToAllSessions outer event`() =
        runTest {
            val fakeRepo = FakeChatSessionRepository(sessions = testSessions)
            val presenter = RecentSessionsSubPresenter(RecentSessionsSubScreen(), sessionRepository = fakeRepo)

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(RecentSessionsUiEvent.ViewAllSessions)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(RecentSessionsOuterEvent.NavigateToAllSessions)
            }
        }
}
