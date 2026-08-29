package dev.hossain.codematex.ui.screens.debug

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DebugMemoryStats
import dev.hossain.codematex.system.MemoryDelta
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
data object DebugScreen : ParcelableScreen {
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        data class Success(
            val models: List<AiModel>,
            val selectedModel: AiModel?,
            val selectedBackend: LlmEngine.Backend,
            val isModelLoaded: Boolean,
            val loadedModelName: String?,
            val activeBackend: LlmEngine.Backend?,
            val isLoadingModel: Boolean,
            val isUnloadingModel: Boolean,
            val lastLoadDelta: MemoryDelta? = null,
            val lastUnloadDelta: MemoryDelta? = null,
            val statusMessage: String? = null,
            val telemetryStats: DebugMemoryStats = DebugMemoryStats(),
            val benchmarkPrompt: String = DEFAULT_BENCHMARK_PROMPT,
            val isBenchmarking: Boolean = false,
            val benchmarkTokens: String = "",
            val benchmarkTtftMs: Long? = null,
            val benchmarkSpeedTps: Float? = null,
            val benchmarkTotalTokens: Int = 0,
            val benchmarkDurationMs: Long? = null,
            val deviceInfo: Map<String, String> = emptyMap(),
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data class SelectModel(
            val model: AiModel,
        ) : Event

        data class SelectBackend(
            val backend: LlmEngine.Backend,
        ) : Event

        data object LoadModel : Event

        data object UnloadModel : Event

        data class UpdateBenchmarkPrompt(
            val prompt: String,
        ) : Event

        data object RunBenchmark : Event

        data object StopBenchmark : Event

        data object TriggerGc : Event

        data class DeleteModel(
            val model: AiModel,
        ) : Event

        data object Back : Event
    }
}

internal const val DEFAULT_BENCHMARK_PROMPT =
    "Write a concise Kotlin function that computes Fibonacci numbers using recursion with memoization."
