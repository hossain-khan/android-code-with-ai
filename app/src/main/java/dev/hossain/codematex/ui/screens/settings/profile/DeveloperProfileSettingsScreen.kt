package dev.hossain.codematex.ui.screens.settings.profile

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.DeveloperExperienceLevel
import dev.hossain.codematex.data.model.DeveloperProfile
import dev.hossain.codematex.data.model.DeveloperProfilePreset
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Screen for customizing the developer's experience level, primary tech stack, and custom prompt directives.
 */
@Parcelize
data object DeveloperProfileSettingsScreen : ParcelableScreen {
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        val eventSink: (Event) -> Unit

        data class Content(
            val profile: DeveloperProfile = DeveloperProfile(),
            val generatedPromptSnippet: String = "",
            override val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data class EnabledToggled(
            val enabled: Boolean,
        ) : Event

        data class ExperienceLevelSelected(
            val level: DeveloperExperienceLevel,
        ) : Event

        data class PrimaryStackChanged(
            val stack: String,
        ) : Event

        data class CustomDirectivesChanged(
            val directives: String,
        ) : Event

        data class PresetApplied(
            val preset: DeveloperProfilePreset,
        ) : Event

        data object ResetClicked : Event

        data object BackClicked : Event
    }
}
