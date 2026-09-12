package dev.hossain.codematex.ui.screens.debug.benchmark

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.repository.FakeModelConfigStore
import dev.hossain.codematex.runtime.FakeLlmEngine
import dev.hossain.codematex.ui.screens.debug.DEFAULT_BENCHMARK_CONFIG
import dev.hossain.codematex.ui.screens.debug.DEFAULT_BENCHMARK_PROMPT
import dev.hossain.codematex.ui.screens.debug.DebugScreen.BenchmarkSamplerPreset
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BenchmarkSubPresenterTest {
    private val fakeEngine = FakeLlmEngine()
    private val fakeConfigStore = FakeModelConfigStore()

    @Test
    fun `initial state reflects screen model load state and defaults`() =
        runTest {
            val presenter =
                BenchmarkSubPresenter(
                    screen = BenchmarkSubScreen(isModelLoaded = true, activeModelName = "Gemma 2B"),
                    llmEngine = fakeEngine,
                    configStore = fakeConfigStore,
                )

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.isModelLoaded).isTrue()
                assertThat(state.isBenchmarking).isFalse()
                assertThat(state.benchmarkConfig).isEqualTo(DEFAULT_BENCHMARK_CONFIG)
                assertThat(state.benchmarkTokens).isEmpty()
            }
        }

    @Test
    fun `updates configuration parameters when UI events are sent`() =
        runTest {
            val presenter =
                BenchmarkSubPresenter(
                    screen = BenchmarkSubScreen(isModelLoaded = true),
                    llmEngine = fakeEngine,
                    configStore = fakeConfigStore,
                )

            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.benchmarkPrompt).isEqualTo(DEFAULT_BENCHMARK_PROMPT)

                initialState.eventSink(BenchmarkUiEvent.UpdatePrompt("New prompt"))
                val promptState = awaitItem()
                assertThat(promptState.benchmarkPrompt).isEqualTo("New prompt")

                promptState.eventSink(BenchmarkUiEvent.UpdateTemperature(0.5f))
                val tempState = awaitItem()
                assertThat(tempState.benchmarkConfig.temperature).isEqualTo(0.5f)

                tempState.eventSink(BenchmarkUiEvent.UpdateTopK(25))
                val topKState = awaitItem()
                assertThat(topKState.benchmarkConfig.topK).isEqualTo(25)

                topKState.eventSink(BenchmarkUiEvent.UpdateTopP(0.85f))
                val topPState = awaitItem()
                assertThat(topPState.benchmarkConfig.topP).isEqualTo(0.85f)

                topPState.eventSink(BenchmarkUiEvent.UpdateMaxTokens(256))
                val maxTokensState = awaitItem()
                assertThat(maxTokensState.benchmarkConfig.maxTokens).isEqualTo(256)

                maxTokensState.eventSink(BenchmarkUiEvent.ApplyPreset(BenchmarkSamplerPreset.GREEDY))
                val presetState = awaitItem()
                assertThat(presetState.benchmarkConfig).isEqualTo(BenchmarkSamplerPreset.GREEDY.config)
                outerEvents.awaitEvent()

                presetState.eventSink(BenchmarkUiEvent.ResetConfig)
                val resetState = awaitItem()
                assertThat(resetState.benchmarkConfig).isEqualTo(DEFAULT_BENCHMARK_CONFIG)
                outerEvents.awaitEvent()
            }
        }

    @Test
    fun `RunBenchmark when model is not loaded emits warning snackbar`() =
        runTest {
            val presenter =
                BenchmarkSubPresenter(
                    screen = BenchmarkSubScreen(isModelLoaded = false),
                    llmEngine = fakeEngine,
                    configStore = fakeConfigStore,
                )

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(BenchmarkUiEvent.RunBenchmark)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isInstanceOf(BenchmarkOuterEvent.ShowSnackbar::class.java)
                assertThat((outerEvent as BenchmarkOuterEvent.ShowSnackbar).message)
                    .contains("Load a model first")
                assertThat(fakeEngine.isolatedInferenceCalls).isEqualTo(0)
            }
        }

    @Test
    fun `RunBenchmark when model is loaded executes isolated inference and streams tokens`() =
        runTest {
            fakeEngine.responseTokens = listOf("Hello", " ", "World", "!")
            val presenter =
                BenchmarkSubPresenter(
                    screen = BenchmarkSubScreen(isModelLoaded = true),
                    llmEngine = fakeEngine,
                    configStore = fakeConfigStore,
                )

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(BenchmarkUiEvent.RunBenchmark)

                // First outer event is "Running benchmark evaluation..."
                val startSnackbar = outerEvents.awaitEvent()
                assertThat(startSnackbar).isInstanceOf(BenchmarkOuterEvent.ShowSnackbar::class.java)

                // Benchmark completes in fakeEngine
                val finishSnackbar = outerEvents.awaitEvent()
                assertThat((finishSnackbar as BenchmarkOuterEvent.ShowSnackbar).message)
                    .contains("Benchmark finished")

                val finalState = expectMostRecentItem()
                assertThat(finalState.isBenchmarking).isFalse()
                assertThat(finalState.benchmarkTokens).isEqualTo("Hello World!")
                assertThat(finalState.benchmarkTotalTokens).isEqualTo(4)
                assertThat(fakeEngine.isolatedInferenceCalls).isEqualTo(1)
            }
        }

    @Test
    fun `StopBenchmark invokes llmEngine stop and notifies user`() =
        runTest {
            val presenter =
                BenchmarkSubPresenter(
                    screen = BenchmarkSubScreen(isModelLoaded = true),
                    llmEngine = fakeEngine,
                    configStore = fakeConfigStore,
                )

            presenter.test {
                val state = expectMostRecentItem()
                state.eventSink(BenchmarkUiEvent.StopBenchmark)

                val outerEvent = outerEvents.awaitEvent()
                assertThat((outerEvent as BenchmarkOuterEvent.ShowSnackbar).message)
                    .contains("Benchmark stopped")
                assertThat(fakeEngine.stopCalls).isEqualTo(1)
            }
        }
}
