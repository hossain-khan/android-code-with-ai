package dev.hossain.codematex.ui.screens.debug.database

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.ui.screens.debug.DebugScreen.DebugDatabaseStats

/**
 * Outer events emitted by [DatabaseDiagnosticsSubPresenter].
 */
sealed interface DatabaseDiagnosticsOuterEvent : SubCircuitOuterEvent {
    data class ShowSnackbar(
        val message: String,
    ) : DatabaseDiagnosticsOuterEvent
}

/**
 * UI State for the Database Diagnostics SubCircuit.
 */
@Immutable
data class DatabaseDiagnosticsSubState(
    val stats: DebugDatabaseStats = DebugDatabaseStats(),
    val eventSink: (DatabaseDiagnosticsUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * UI Events handled by [DatabaseDiagnosticsSubPresenter].
 */
sealed interface DatabaseDiagnosticsUiEvent {
    data object ResetProgress : DatabaseDiagnosticsUiEvent

    data object SeedProgress : DatabaseDiagnosticsUiEvent

    data object ClearSessions : DatabaseDiagnosticsUiEvent
}

/**
 * SubScreen marker for the curriculum progress and Room database inspector card.
 */
data object DatabaseDiagnosticsSubScreen : SubScreen<DatabaseDiagnosticsOuterEvent>
