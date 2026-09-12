package dev.hossain.codematex.ui.screens.debug

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.domain.runner.PlaygroundExecutionResult
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DebugMemoryStats
import dev.hossain.codematex.system.HardwareEligibility
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
            val benchmarkConfig: ModelConfig = DEFAULT_BENCHMARK_CONFIG,
            val isBenchmarking: Boolean = false,
            val benchmarkTokens: String = "",
            val benchmarkTtftMs: Long? = null,
            val benchmarkSpeedTps: Float? = null,
            val benchmarkTotalTokens: Int = 0,
            val benchmarkDurationMs: Long? = null,
            val deviceInfo: Map<String, String> = emptyMap(),
            val hardwareEligibility: HardwareEligibility = HardwareEligibility.Eligible,
            val isDevMode: Boolean = false,
            val runtimeSpecs: Map<String, String> = emptyMap(),
            val isOnline: Boolean = true,
            val runnerSelectedLang: String = DEFAULT_RUNNER_LANGUAGE,
            val runnerSnippetCode: String = DEFAULT_RUNNER_SNIPPETS[DEFAULT_RUNNER_LANGUAGE] ?: "",
            val isRunningSnippet: Boolean = false,
            val runnerResult: PlaygroundExecutionResult? = null,
            val runnerDurationMs: Long? = null,
            val isPingingProxy: Boolean = false,
            val proxyPingMs: Long? = null,
            val proxyPingError: String? = null,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    enum class BenchmarkSamplerPreset(
        val label: String,
        val description: String,
        val config: ModelConfig,
    ) {
        GREEDY(
            label = "Greedy",
            description = "Temp: 0.1, Top-K: 1",
            config =
                ModelConfig(
                    temperature = 0.1f,
                    topK = 1,
                    topP = 0.95f,
                    maxTokens = 512,
                ),
        ),
        BALANCED(
            label = "Balanced",
            description = "Temp: 0.7, Top-K: 40",
            config =
                ModelConfig(
                    temperature = 0.7f,
                    topK = 40,
                    topP = 0.95f,
                    maxTokens = 512,
                ),
        ),
        CREATIVE(
            label = "Creative",
            description = "Temp: 1.0, Top-K: 80",
            config =
                ModelConfig(
                    temperature = 1.0f,
                    topK = 80,
                    topP = 0.95f,
                    maxTokens = 512,
                ),
        ),
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

        data class UpdateBenchmarkTemperature(
            val temperature: Float,
        ) : Event

        data class UpdateBenchmarkTopK(
            val topK: Int,
        ) : Event

        data class UpdateBenchmarkTopP(
            val topP: Float,
        ) : Event

        data class UpdateBenchmarkMaxTokens(
            val maxTokens: Int,
        ) : Event

        data class ApplySamplerPreset(
            val preset: BenchmarkSamplerPreset,
        ) : Event

        data object ResetBenchmarkConfig : Event

        data object RunBenchmark : Event

        data object StopBenchmark : Event

        data object TriggerGc : Event

        data class DeleteModel(
            val model: AiModel,
        ) : Event

        data class SelectRunnerLanguage(
            val language: String,
        ) : Event

        data class UpdateRunnerSnippet(
            val code: String,
        ) : Event

        data object ResetRunnerSnippet : Event

        data object RunRunnerSnippet : Event

        data object PingProxy : Event

        data object Back : Event
    }
}

internal val DEFAULT_BENCHMARK_CONFIG =
    ModelConfig(
        temperature = 0.8f,
        topK = 40,
        topP = 0.95f,
        maxTokens = 512,
    )

internal const val DEFAULT_BENCHMARK_PROMPT =
    "Write a concise Kotlin function that computes Fibonacci numbers using recursion with memoization."

internal const val DEFAULT_RUNNER_LANGUAGE = "kotlin"

internal val RUNNER_SUPPORTED_LANGUAGES = listOf("kotlin", "go", "rust", "python", "typescript")

internal val DEFAULT_RUNNER_SNIPPETS =
    mapOf(
        "kotlin" to
            """
            fun main() {
                println("Hello from CodeMateX Edge Runner (Kotlin)!")
                val numbers = listOf(1, 2, 3, 4, 5)
                println("Sum: ${'$'}{numbers.sum()}")
            }
            """.trimIndent(),
        "go" to
            """
            package main
            import "fmt"

            func main() {
                fmt.Println("Hello from CodeMateX Edge Runner (Go)!")
            }
            """.trimIndent(),
        "rust" to
            """
            fn main() {
                println!("Hello from CodeMateX Edge Runner (Rust)!");
                let numbers = vec![1, 2, 3, 4, 5];
                println!("Sum: {}", numbers.iter().sum::<i32>());
            }
            """.trimIndent(),
        "python" to
            """
            print("Hello from CodeMateX Edge Runner (Python)!")
            squares = [x**2 for x in range(1, 6)]
            print("Squares:", squares)
            """.trimIndent(),
        "typescript" to
            """
            console.log("Hello from CodeMateX Edge Runner (TypeScript)!");
            const greeting: string = "TypeScript executed at Cloudflare edge";
            console.log(greeting);
            """.trimIndent(),
    )

internal const val LITERT_LM_VERSION = "0.17.0"
