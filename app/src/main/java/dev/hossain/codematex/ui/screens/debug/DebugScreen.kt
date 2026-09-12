package dev.hossain.codematex.ui.screens.debug

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.ModelConfig
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
data object DebugScreen : ParcelableScreen {
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        data class Success(
            val isModelLoaded: Boolean = false,
            val loadedModelName: String? = null,
            val statusMessage: String? = null,
            val eventSink: (Event) -> Unit,
        ) : State
    }

    @Immutable
    @Serializable
    data class DebugDatabaseStats(
        val completedLessons: Int = 0,
        val inProgressLessons: Int = 0,
        val totalBundledLessons: Int = 0,
        val totalCourses: Int = 0,
        val completedCourses: Int = 0,
        val totalQuizzes: Int = 0,
        val totalSessions: Int = 0,
        val totalMessages: Int = 0,
    )

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
        data object Back : Event

        data class ShowSnackbar(
            val message: String,
        ) : Event

        data object ClearStatusMessage : Event

        data class ModelLoaded(
            val modelName: String,
        ) : Event

        data object ModelUnloaded : Event
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
