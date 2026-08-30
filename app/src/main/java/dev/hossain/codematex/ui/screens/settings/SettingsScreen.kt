package dev.hossain.codematex.ui.screens.settings

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.CodeTheme
import dev.hossain.codematex.data.model.DeveloperProfile
import dev.hossain.codematex.data.model.TutorPersona
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Screen for managing user preferences, AI persona, hardware parameters, and local data.
 */
@Parcelize
data object SettingsScreen : ParcelableScreen {
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        val eventSink: (Event) -> Unit

        data class Content(
            val selectedPersona: TutorPersona = TutorPersona.SENIOR_ENGINEER,
            val developerProfile: DeveloperProfile = DeveloperProfile(),
            val isWifiOnlyDownload: Boolean = true,
            val showLineNumbers: Boolean = true,
            val codeTheme: CodeTheme = CodeTheme.TOMORROW,
            val hapticFeedbackEnabled: Boolean = true,
            val ramEvictionMinutes: Int = 3,
            val storageUsedBytes: Long = 0L,
            val downloadedModelCount: Int = 0,
            val sessionCount: Int = 0,
            val showClearHistoryConfirmation: Boolean = false,
            val showPersonaDialog: Boolean = false,
            val showRamEvictionDialog: Boolean = false,
            val appVersion: String = "",
            override val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data class PersonaSelected(
            val persona: TutorPersona,
        ) : Event

        data class ShowPersonaDialog(
            val show: Boolean,
        ) : Event

        data object DeveloperProfileClicked : Event

        data object CodeBlockSettingsClicked : Event

        data object OpenDebugScreen : Event

        data class WifiOnlyToggled(
            val enabled: Boolean,
        ) : Event

        data class LineNumbersToggled(
            val enabled: Boolean,
        ) : Event

        data class HapticsToggled(
            val enabled: Boolean,
        ) : Event

        data class RamEvictionSelected(
            val minutes: Int,
        ) : Event

        data class ShowRamEvictionDialog(
            val show: Boolean,
        ) : Event

        data object ManageModelsClicked : Event

        data class ShowClearHistoryDialog(
            val show: Boolean,
        ) : Event

        data object ConfirmClearHistory : Event

        data object ReplayTourClicked : Event

        data object OpenFeedbackClicked : Event

        data object BackClicked : Event
    }
}
