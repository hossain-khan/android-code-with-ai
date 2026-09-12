package dev.hossain.codematex.ui.screens.debug.telemetry

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.system.DebugMemoryStats

/**
 * Outer events emitted by [TelemetrySubPresenter] to the parent screen.
 */
sealed interface TelemetryOuterEvent : SubCircuitOuterEvent {
    data class ShowSnackbar(
        val message: String,
    ) : TelemetryOuterEvent
}

/**
 * UI State for the Telemetry SubCircuit.
 */
@Immutable
data class TelemetrySubState(
    val stats: DebugMemoryStats = DebugMemoryStats(),
    val eventSink: (TelemetryUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * UI Events handled by [TelemetrySubPresenter].
 */
sealed interface TelemetryUiEvent {
    data object TriggerGc : TelemetryUiEvent
}

/**
 * SubScreen marker for the real-time memory telemetry card.
 */
data object TelemetrySubScreen : SubScreen<TelemetryOuterEvent>
