package dev.hossain.codematex.ui.screens.debug.benchmark

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.ui.screens.debug.DEFAULT_BENCHMARK_CONFIG
import dev.hossain.codematex.ui.screens.debug.DEFAULT_BENCHMARK_PROMPT
import dev.hossain.codematex.ui.screens.debug.DebugScreen.BenchmarkSamplerPreset

/**
 * Outer events emitted by [BenchmarkSubPresenter].
 */
sealed interface BenchmarkOuterEvent : SubCircuitOuterEvent {
    data class ShowSnackbar(
        val message: String,
    ) : BenchmarkOuterEvent
}

/**
 * UI State for the Inference Benchmark SubCircuit.
 */
@Immutable
data class BenchmarkSubState(
    val benchmarkPrompt: String = DEFAULT_BENCHMARK_PROMPT,
    val benchmarkConfig: ModelConfig = DEFAULT_BENCHMARK_CONFIG,
    val isBenchmarking: Boolean = false,
    val benchmarkTokens: String = "",
    val benchmarkTtftMs: Long? = null,
    val benchmarkSpeedTps: Float? = null,
    val benchmarkTotalTokens: Int = 0,
    val benchmarkDurationMs: Long? = null,
    val isModelLoaded: Boolean = false,
    val eventSink: (BenchmarkUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * UI Events handled by [BenchmarkSubPresenter].
 */
sealed interface BenchmarkUiEvent {
    data class UpdatePrompt(
        val prompt: String,
    ) : BenchmarkUiEvent

    data class UpdateTemperature(
        val temperature: Float,
    ) : BenchmarkUiEvent

    data class UpdateTopK(
        val topK: Int,
    ) : BenchmarkUiEvent

    data class UpdateTopP(
        val topP: Float,
    ) : BenchmarkUiEvent

    data class UpdateMaxTokens(
        val maxTokens: Int,
    ) : BenchmarkUiEvent

    data class ApplyPreset(
        val preset: BenchmarkSamplerPreset,
    ) : BenchmarkUiEvent

    data object ResetConfig : BenchmarkUiEvent

    data object RunBenchmark : BenchmarkUiEvent

    data object StopBenchmark : BenchmarkUiEvent
}

/**
 * SubScreen marker for the inference and throughput benchmark card.
 */
data class BenchmarkSubScreen(
    val isModelLoaded: Boolean = false,
    val activeModelName: String? = null,
) : SubScreen<BenchmarkOuterEvent>
