package dev.hossain.codematex.ui.screens.debug.hardware

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.system.HardwareEligibility

/**
 * Outer events emitted by [HardwareDiagnosticsSubPresenter].
 */
sealed interface HardwareDiagnosticsOuterEvent : SubCircuitOuterEvent

/**
 * UI State for the Hardware Diagnostics SubCircuit.
 */
@Immutable
data class HardwareDiagnosticsSubState(
    val deviceInfo: Map<String, String> = emptyMap(),
    val runtimeSpecs: Map<String, String> = emptyMap(),
    val eligibility: HardwareEligibility = HardwareEligibility.Eligible,
    val isDevMode: Boolean = false,
) : SubCircuitUiState

/**
 * SubScreen marker for the hardware and runtime specifications card.
 */
data class HardwareDiagnosticsSubScreen(
    val activeBackendName: String? = null,
) : SubScreen<HardwareDiagnosticsOuterEvent>
