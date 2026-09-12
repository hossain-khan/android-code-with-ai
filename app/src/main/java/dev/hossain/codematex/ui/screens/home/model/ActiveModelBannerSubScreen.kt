package dev.hossain.codematex.ui.screens.home.model

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen

/**
 * Outer events emitted by [ActiveModelBannerSubPresenter].
 */
sealed interface ActiveModelBannerOuterEvent : SubCircuitOuterEvent {
    data object NavigateToModelPicker : ActiveModelBannerOuterEvent
}

/**
 * UI State for the Active Model Hero Banner SubCircuit.
 */
@Immutable
data class ActiveModelBannerSubState(
    val hasDownloadedModel: Boolean = false,
    val selectedModelName: String? = null,
    val isModelInMemory: Boolean = false,
    val memoryBackend: String? = null,
    val eventSink: (ActiveModelBannerUiEvent) -> Unit = {},
) : SubCircuitUiState

/**
 * UI Events handled by [ActiveModelBannerSubPresenter].
 */
sealed interface ActiveModelBannerUiEvent {
    data object ManageModels : ActiveModelBannerUiEvent
}

/**
 * SubScreen marker for the active model hero banner.
 */
data object ActiveModelBannerSubScreen : SubScreen<ActiveModelBannerOuterEvent>
