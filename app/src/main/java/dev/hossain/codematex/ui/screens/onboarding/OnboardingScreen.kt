package dev.hossain.codematex.ui.screens.onboarding

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Screen for first-time user onboarding and app feature introduction.
 */
@Parcelize
data object OnboardingScreen : ParcelableScreen {
    @IgnoredOnParcel
    const val GITHUB_ISSUES_URL = "https://github.com/hossain-khan/android-code-with-ai/issues"

    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        val eventSink: (Event) -> Unit

        data class Content(
            val currentPage: Int = 0,
            val pageCount: Int = 4,
            val hasDownloadedModel: Boolean = false,
            override val eventSink: (Event) -> Unit,
        ) : State
    }

    @Serializable
    sealed interface Event : CircuitUiEvent {
        data class PageChanged(
            val page: Int,
        ) : Event

        data object NextClicked : Event

        data object SkipClicked : Event

        data object GetStartedClicked : Event

        data object OpenFeedbackClicked : Event
    }
}
