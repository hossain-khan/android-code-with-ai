package dev.hossain.codematex.circuit

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.BuildConfig
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatPresenterTest {
    private val fakeNavigator = FakeNavigator(ChatScreen(topic = CodingTopic.KOTLIN))
    private val fakeLlmEngine = FakeLlmEngine()
    private val fakeModelRepo = FakeModelRepository(selectedModel = testModel())
    private val fakeSessionRepo = FakeChatSessionRepository()

    @Test
    fun `present - emits Loading or Active based on DEV_MODE when no model selected`() =
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
                if (BuildConfig.DEV_MODE) {
                    assertTrue(state is ChatScreen.State.Active)
                } else {
                    assertEquals(ChatScreen.State.Loading, state)
                }
            }
        }
}
