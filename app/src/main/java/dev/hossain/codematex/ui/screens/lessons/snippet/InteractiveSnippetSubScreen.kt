package dev.hossain.codematex.ui.screens.lessons.snippet

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.serialization.Serializable

/**
 * State of playground execution for an interactive code snippet.
 */
@Immutable
@Serializable
sealed interface SnippetExecutionState {
    /** Snippet is idle, waiting for user execution. */
    data object Idle : SnippetExecutionState

    /** Snippet is currently compiling and executing on the remote playground backend. */
    data object Compiling : SnippetExecutionState

    /** Snippet compiled and executed successfully with standard output. */
    data class Success(
        val output: String,
    ) : SnippetExecutionState

    /** Compilation failed with compiler diagnostic messages. */
    data class CompilationError(
        val diagnostic: String,
    ) : SnippetExecutionState

    /** Remote execution encountered a network, timeout, or system error. */
    data class Error(
        val message: String,
    ) : SnippetExecutionState
}

/**
 * Outer events emitted by [InteractiveSnippetSubPresenter] to parent screens.
 */
sealed interface InteractiveSnippetOuterEvent : SubCircuitOuterEvent {
    /** Emitted when execution concludes with success or failure. */
    data class ExecutionFinished(
        val snippetId: String,
        val isSuccess: Boolean,
    ) : InteractiveSnippetOuterEvent
}

/**
 * SubScreen identifying an interactive runnable code snippet block.
 *
 * @param snippetId Unique identifier for this code snippet block within the lesson.
 * @param code Source code content to display and execute.
 * @param language Target programming language identifier (e.g., "kotlin", "rust", "python").
 * @param topic Associated topic for visual styling and accent tinting.
 */
data class InteractiveSnippetSubScreen(
    val snippetId: String,
    val code: String,
    val language: String,
    val topic: CodingTopic,
) : SubScreen<InteractiveSnippetOuterEvent>

/**
 * UI State for [InteractiveSnippetSubScreen].
 */
@Immutable
data class InteractiveSnippetSubState(
    val code: String,
    val language: String,
    val topic: CodingTopic,
    val isOnline: Boolean = true,
    val executionState: SnippetExecutionState = SnippetExecutionState.Idle,
    val eventSink: (InteractiveSnippetUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * UI Events handled by [InteractiveSnippetSubPresenter].
 */
sealed interface InteractiveSnippetUiEvent {
    /** Execute the snippet on the online playground runner. */
    data object RunSnippet : InteractiveSnippetUiEvent

    /** Dismiss the displayed output/error card. */
    data object DismissOutput : InteractiveSnippetUiEvent
}
