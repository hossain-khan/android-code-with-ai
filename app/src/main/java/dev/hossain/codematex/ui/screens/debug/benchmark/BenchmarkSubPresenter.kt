package dev.hossain.codematex.ui.screens.debug.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.repository.ModelConfigStore
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.ui.screens.debug.DEFAULT_BENCHMARK_CONFIG
import dev.hossain.codematex.ui.screens.debug.DEFAULT_BENCHMARK_PROMPT
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

class BenchmarkSubPresenter(
    private val screen: BenchmarkSubScreen,
    private val llmEngine: LlmEngine,
    private val configStore: ModelConfigStore,
) : SubPresenter<BenchmarkOuterEvent, BenchmarkSubState> {
    @Composable
    override fun present(outerEventSink: (BenchmarkOuterEvent) -> Unit): BenchmarkSubState {
        val scope = rememberCoroutineScope()

        var benchmarkPrompt by rememberRetained { mutableStateOf(DEFAULT_BENCHMARK_PROMPT) }
        var benchmarkConfig by rememberRetained { mutableStateOf(DEFAULT_BENCHMARK_CONFIG) }
        var isBenchmarking by rememberRetained { mutableStateOf(false) }
        var benchmarkTokens by rememberRetained { mutableStateOf("") }
        var benchmarkTtftMs by rememberRetained { mutableStateOf<Long?>(null) }
        var benchmarkSpeedTps by rememberRetained { mutableStateOf<Float?>(null) }
        var benchmarkTotalTokens by rememberRetained { mutableIntStateOf(0) }
        var benchmarkDurationMs by rememberRetained { mutableStateOf<Long?>(null) }

        return BenchmarkSubState(
            benchmarkPrompt = benchmarkPrompt,
            benchmarkConfig = benchmarkConfig,
            isBenchmarking = isBenchmarking,
            benchmarkTokens = benchmarkTokens,
            benchmarkTtftMs = benchmarkTtftMs,
            benchmarkSpeedTps = benchmarkSpeedTps,
            benchmarkTotalTokens = benchmarkTotalTokens,
            benchmarkDurationMs = benchmarkDurationMs,
            isModelLoaded = screen.isModelLoaded,
            eventSink = { event ->
                when (event) {
                    is BenchmarkUiEvent.UpdatePrompt -> {
                        benchmarkPrompt = event.prompt
                    }

                    is BenchmarkUiEvent.UpdateTemperature -> {
                        benchmarkConfig = benchmarkConfig.copy(temperature = event.temperature)
                    }

                    is BenchmarkUiEvent.UpdateTopK -> {
                        benchmarkConfig = benchmarkConfig.copy(topK = event.topK)
                    }

                    is BenchmarkUiEvent.UpdateTopP -> {
                        benchmarkConfig = benchmarkConfig.copy(topP = event.topP)
                    }

                    is BenchmarkUiEvent.UpdateMaxTokens -> {
                        benchmarkConfig = benchmarkConfig.copy(maxTokens = event.maxTokens)
                    }

                    is BenchmarkUiEvent.ApplyPreset -> {
                        benchmarkConfig = event.preset.config
                        outerEventSink(
                            BenchmarkOuterEvent.ShowSnackbar(
                                "Applied sampler preset: ${event.preset.label} (${event.preset.description})",
                            ),
                        )
                        Timber.d("BenchmarkSubPresenter: Applied sampler preset: %s", event.preset.name)
                    }

                    BenchmarkUiEvent.ResetConfig -> {
                        benchmarkConfig = DEFAULT_BENCHMARK_CONFIG
                        outerEventSink(BenchmarkOuterEvent.ShowSnackbar("Reset sampler configuration to defaults."))
                        Timber.d("BenchmarkSubPresenter: Reset sampler configuration to defaults")
                    }

                    BenchmarkUiEvent.RunBenchmark -> {
                        if (!screen.isModelLoaded) {
                            outerEventSink(
                                BenchmarkOuterEvent.ShowSnackbar("Load a model first before running inference benchmark."),
                            )
                            return@BenchmarkSubState
                        }

                        scope.launch {
                            isBenchmarking = true
                            benchmarkTokens = ""
                            benchmarkTtftMs = null
                            benchmarkSpeedTps = null
                            benchmarkTotalTokens = 0
                            benchmarkDurationMs = null
                            outerEventSink(BenchmarkOuterEvent.ShowSnackbar("Running benchmark evaluation..."))
                            Timber.i(
                                "BenchmarkSubPresenter [BENCHMARK_START]: promptLength=%d, temp=%.2f, topK=%d, topP=%.2f, maxTokens=%d",
                                benchmarkPrompt.length,
                                benchmarkConfig.temperature,
                                benchmarkConfig.topK,
                                benchmarkConfig.topP,
                                benchmarkConfig.maxTokens,
                            )

                            val startTime = System.currentTimeMillis()
                            var firstTokenTime: Long? = null
                            var tokenCount = 0

                            try {
                                llmEngine.runInferenceIsolated(
                                    input = benchmarkPrompt,
                                    systemInstruction = "You are a concise coding assistant.",
                                    config = benchmarkConfig,
                                ) { partial, done ->
                                    val now = System.currentTimeMillis()
                                    if (firstTokenTime == null && partial.isNotEmpty()) {
                                        val ttft = now - startTime
                                        firstTokenTime = now
                                        benchmarkTtftMs = ttft
                                        Timber.d("BenchmarkSubPresenter [BENCHMARK_TTFT]: ttftMs=%d", ttft)
                                    }

                                    if (partial.isNotEmpty()) {
                                        tokenCount++
                                        benchmarkTotalTokens = tokenCount
                                        benchmarkTokens += partial

                                        val decodeTimeSec = (now - (firstTokenTime ?: startTime)) / 1000f
                                        if (decodeTimeSec > 0.05f) {
                                            benchmarkSpeedTps = tokenCount / decodeTimeSec
                                        }
                                    }

                                    if (done) {
                                        val totalDuration = now - startTime
                                        benchmarkDurationMs = totalDuration
                                        val finalDecodeSec = (now - (firstTokenTime ?: startTime)) / 1000f
                                        if (finalDecodeSec > 0f) {
                                            benchmarkSpeedTps = tokenCount / finalDecodeSec
                                        }
                                        outerEventSink(
                                            BenchmarkOuterEvent.ShowSnackbar(
                                                "Benchmark finished: $tokenCount tokens in ${totalDuration}ms " +
                                                    "(TTFT: ${benchmarkTtftMs ?: 0}ms, Speed: ${"%.1f".format(
                                                        benchmarkSpeedTps ?: 0f,
                                                    )} t/s)",
                                            ),
                                        )
                                        Timber.i(
                                            "BenchmarkSubPresenter [BENCHMARK_SUCCESS]: tokens=%d, durationMs=%d, ttftMs=%d, speedTps=%.2f",
                                            tokenCount,
                                            totalDuration,
                                            benchmarkTtftMs ?: 0,
                                            benchmarkSpeedTps ?: 0f,
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "BenchmarkSubPresenter [BENCHMARK_ERROR]: Inference benchmark failed")
                                outerEventSink(BenchmarkOuterEvent.ShowSnackbar("Benchmark error: ${e.message}"))
                            } finally {
                                isBenchmarking = false
                            }
                        }
                    }

                    BenchmarkUiEvent.StopBenchmark -> {
                        llmEngine.stop()
                        isBenchmarking = false
                        outerEventSink(BenchmarkOuterEvent.ShowSnackbar("Benchmark stopped by user."))
                        Timber.i("BenchmarkSubPresenter [BENCHMARK_CANCELLED]: Benchmark stopped by user")
                    }
                }
            },
        )
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class BenchmarkSubPresenterFactory(
    private val llmEngine: LlmEngine,
    private val configStore: ModelConfigStore,
) : SubPresenterFactory {
    override fun create(screen: SubScreen<*>): SubPresenter<*, *>? =
        when (screen) {
            is BenchmarkSubScreen -> {
                BenchmarkSubPresenter(
                    screen = screen,
                    llmEngine = llmEngine,
                    configStore = configStore,
                )
            }

            else -> {
                null
            }
        }
}
