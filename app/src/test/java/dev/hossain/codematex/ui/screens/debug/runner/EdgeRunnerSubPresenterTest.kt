package dev.hossain.codematex.ui.screens.debug.runner

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.domain.runner.FakePlaygroundCodeRunner
import dev.hossain.codematex.domain.runner.PlaygroundExecutionResult
import dev.hossain.codematex.system.FakeNetworkMonitor
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EdgeRunnerSubPresenterTest {
    @Test
    fun `presenter changes language and updates snippet`() =
        runTest {
            val fakeRunner = FakePlaygroundCodeRunner()
            val fakeNetwork = FakeNetworkMonitor(initialIsOnline = true)
            val presenter = EdgeRunnerSubPresenter(fakeRunner, fakeNetwork)

            presenter.test {
                val initialState = expectMostRecentItem()
                assertThat(initialState.selectedLanguage).isEqualTo("kotlin")

                initialState.eventSink(EdgeRunnerUiEvent.SelectLanguage("python"))
                val pythonState = expectMostRecentItem()
                assertThat(pythonState.selectedLanguage).isEqualTo("python")
                assertThat(pythonState.snippetCode).contains("Python")
            }
        }

    @Test
    fun `executing code returns success result and emits outer snackbar event`() =
        runTest {
            val fakeRunner =
                FakePlaygroundCodeRunner(
                    resultToReturn = PlaygroundExecutionResult.Success(output = "Hello Antigravity!\n"),
                )
            val fakeNetwork = FakeNetworkMonitor(initialIsOnline = true)
            val presenter = EdgeRunnerSubPresenter(fakeRunner, fakeNetwork)

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(EdgeRunnerUiEvent.RunSnippet)

                val resultState = expectMostRecentItem()
                assertThat(resultState.isRunningSnippet).isFalse()
                assertThat(resultState.runnerResult).isInstanceOf(PlaygroundExecutionResult.Success::class.java)
                assertThat((resultState.runnerResult as PlaygroundExecutionResult.Success).output).isEqualTo("Hello Antigravity!\n")

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isInstanceOf(EdgeRunnerOuterEvent.ShowSnackbar::class.java)
            }
        }

    @Test
    fun `pinging proxy returns duration and emits outer snackbar event`() =
        runTest {
            val fakeRunner =
                FakePlaygroundCodeRunner(
                    resultToReturn = PlaygroundExecutionResult.Success(output = "ping\n"),
                )
            val fakeNetwork = FakeNetworkMonitor(initialIsOnline = true)
            val presenter = EdgeRunnerSubPresenter(fakeRunner, fakeNetwork)

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(EdgeRunnerUiEvent.PingProxy)

                val pingedState = expectMostRecentItem()
                assertThat(pingedState.isPingingProxy).isFalse()
                assertThat(pingedState.proxyPingMs).isNotNull()

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isInstanceOf(EdgeRunnerOuterEvent.ShowSnackbar::class.java)
                assertThat((outerEvent as EdgeRunnerOuterEvent.ShowSnackbar).message).contains("Proxy reachable")
            }
        }
}
