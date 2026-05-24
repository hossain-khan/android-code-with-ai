package dev.hossain.codematex.circuit

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPresenterTest {
    private val fakeNavigator = FakeNavigator(ChatScreen(topic = CodingTopic.KOTLIN))
    private val fakeLlmEngine = FakeLlmEngine()
    private val fakeModelRepo = FakeModelRepository(selectedModel = testModel())
    private val fakeSessionRepo = FakeChatSessionRepository()

    @Test
    fun `present - emits valid initial state when no model selected`() =
        runTest {
            val presenter =
                ChatPresenter(
                    navigator = fakeNavigator,
                    screen = ChatScreen(topic = CodingTopic.KOTLIN),
                    llmEngine = fakeLlmEngine,
                    modelRepository = FakeModelRepository(selectedModel = null),
                    sessionRepository = fakeSessionRepo,
                )

            presenter.test {
                val state = awaitItem()
                // DEV_MODE=true returns Active (stub model), DEV_MODE=false returns Loading
                assertTrue(
                    "Expected Loading or Active state but got $state",
                    state is ChatScreen.State.Loading || state is ChatScreen.State.Active,
                )
            }
        }
}
