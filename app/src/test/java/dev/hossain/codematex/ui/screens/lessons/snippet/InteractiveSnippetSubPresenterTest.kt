package dev.hossain.codematex.ui.screens.lessons.snippet

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.domain.runner.FakePlaygroundCodeRunner
import dev.hossain.codematex.domain.runner.PlaygroundExecutionResult
import dev.hossain.codematex.system.FakeNetworkMonitor
import kotlinx.coroutines.test.runTest
import org.junit.Test

class InteractiveSnippetSubPresenterTest {
    private val fakeRunner = FakePlaygroundCodeRunner()
    private val fakeNetworkMonitor = FakeNetworkMonitor()

    private val testScreen =
        InteractiveSnippetSubScreen(
            snippetId = "snippet-1",
            code = "println!(\"Hello\");",
            language = "rust",
            topic = CodingTopic.RUST,
        )

    private fun createPresenter(
        screen: InteractiveSnippetSubScreen = testScreen,
        runner: FakePlaygroundCodeRunner = fakeRunner,
        networkMonitor: FakeNetworkMonitor = fakeNetworkMonitor,
    ): InteractiveSnippetSubPresenter =
        InteractiveSnippetSubPresenter(
            screen = screen,
            playgroundCodeRunner = runner,
            networkMonitor = networkMonitor,
        )

    @Test
    fun `initial state reflects screen properties and is online`() =
        runTest {
            fakeNetworkMonitor.setOnline(true)
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.code).isEqualTo("println!(\"Hello\");")
                assertThat(state.language).isEqualTo("rust")
                assertThat(state.topic).isEqualTo(CodingTopic.RUST)
                assertThat(state.isOnline).isTrue()
                assertThat(state.executionState).isEqualTo(SnippetExecutionState.Idle)
            }
        }

    @Test
    fun `RunSnippet with success updates execution state and emits outer event`() =
        runTest {
            fakeRunner.resultToReturn = PlaygroundExecutionResult.Success("Hello\n")
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(InteractiveSnippetUiEvent.RunSnippet)

                val updatedState = expectMostRecentItem()
                assertThat(updatedState.executionState).isEqualTo(SnippetExecutionState.Success("Hello\n"))

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(
                    InteractiveSnippetOuterEvent.ExecutionFinished(
                        snippetId = "snippet-1",
                        isSuccess = true,
                    ),
                )
            }
        }

    @Test
    fun `RunSnippet with compilation error updates state to CompilationError`() =
        runTest {
            fakeRunner.resultToReturn = PlaygroundExecutionResult.CompilationError("mismatched types")
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(InteractiveSnippetUiEvent.RunSnippet)

                val updatedState = expectMostRecentItem()
                assertThat(updatedState.executionState).isEqualTo(
                    SnippetExecutionState.CompilationError("mismatched types"),
                )

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(
                    InteractiveSnippetOuterEvent.ExecutionFinished(
                        snippetId = "snippet-1",
                        isSuccess = false,
                    ),
                )
            }
        }

    @Test
    fun `RunSnippet with network error updates state to Error`() =
        runTest {
            fakeRunner.resultToReturn = PlaygroundExecutionResult.NetworkError("Network down")
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(InteractiveSnippetUiEvent.RunSnippet)

                val updatedState = expectMostRecentItem()
                assertThat(updatedState.executionState).isEqualTo(
                    SnippetExecutionState.Error("Network down"),
                )

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(
                    InteractiveSnippetOuterEvent.ExecutionFinished(
                        snippetId = "snippet-1",
                        isSuccess = false,
                    ),
                )
            }
        }

    @Test
    fun `DismissOutput resets execution state to Idle`() =
        runTest {
            fakeRunner.resultToReturn = PlaygroundExecutionResult.Success("Output")
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(InteractiveSnippetUiEvent.RunSnippet)

                val runningState = expectMostRecentItem()
                assertThat(runningState.executionState).isInstanceOf(SnippetExecutionState.Success::class.java)

                runningState.eventSink(InteractiveSnippetUiEvent.DismissOutput)
                val dismissedState = expectMostRecentItem()
                assertThat(dismissedState.executionState).isEqualTo(SnippetExecutionState.Idle)
            }
        }

    @Test
    fun `network offline updates isOnline reactively`() =
        runTest {
            fakeNetworkMonitor.setOnline(true)
            val presenter = createPresenter()

            presenter.test {
                val initial = expectMostRecentItem()
                assertThat(initial.isOnline).isTrue()

                fakeNetworkMonitor.setOnline(false)
                val updated = awaitItem()
                assertThat(updated.isOnline).isFalse()
            }
        }
}
