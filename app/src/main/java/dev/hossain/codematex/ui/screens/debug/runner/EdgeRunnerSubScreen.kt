package dev.hossain.codematex.ui.screens.debug.runner

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.domain.runner.PlaygroundExecutionResult
import dev.hossain.codematex.ui.screens.debug.DEFAULT_RUNNER_LANGUAGE
import dev.hossain.codematex.ui.screens.debug.DEFAULT_RUNNER_SNIPPETS

/**
 * Outer events emitted by [EdgeRunnerSubPresenter].
 */
sealed interface EdgeRunnerOuterEvent : SubCircuitOuterEvent {
    data class ShowSnackbar(
        val message: String,
    ) : EdgeRunnerOuterEvent
}

/**
 * UI State for the Edge Runner Diagnostics SubCircuit.
 */
@Immutable
data class EdgeRunnerSubState(
    val selectedLanguage: String = DEFAULT_RUNNER_LANGUAGE,
    val snippetCode: String = DEFAULT_RUNNER_SNIPPETS[DEFAULT_RUNNER_LANGUAGE] ?: "",
    val isRunningSnippet: Boolean = false,
    val runnerResult: PlaygroundExecutionResult? = null,
    val runnerDurationMs: Long? = null,
    val isPingingProxy: Boolean = false,
    val proxyPingMs: Long? = null,
    val proxyPingError: String? = null,
    val isOnline: Boolean = true,
    val eventSink: (EdgeRunnerUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * UI Events handled by [EdgeRunnerSubPresenter].
 */
sealed interface EdgeRunnerUiEvent {
    data class SelectLanguage(
        val language: String,
    ) : EdgeRunnerUiEvent

    data class UpdateSnippet(
        val code: String,
    ) : EdgeRunnerUiEvent

    data object ResetSnippet : EdgeRunnerUiEvent

    data object RunSnippet : EdgeRunnerUiEvent

    data object PingProxy : EdgeRunnerUiEvent
}

/**
 * SubScreen marker for the Cloudflare Workers Edge Runner diagnostics card.
 */
data object EdgeRunnerSubScreen : SubScreen<EdgeRunnerOuterEvent>
