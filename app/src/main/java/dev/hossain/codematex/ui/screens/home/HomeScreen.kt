package dev.hossain.codematex.ui.screens.home

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import dev.hossain.codematex.data.model.CodingTopic
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Root landing dashboard screen coordinating the application's primary feature hubs:
 * active model banner, interactive guided courses, topic selector, and recent sessions.
 */
@Parcelize
data object HomeScreen : ParcelableScreen {
    /**
     * UI state contract for [HomeScreen].
     */
    @Immutable
    @Serializable
    sealed interface State : CircuitUiState {
        /**
         * State displayed when the device does not satisfy hardware eligibility criteria (e.g. RAM).
         *
         * @param reason Human-readable explanation of the eligibility issue.
         * @param detectedRamGb Detected total RAM in gigabytes.
         * @param minRequiredRamGb Minimum required RAM in gigabytes for on-device inference.
         * @param is64BitSupported Whether the device architecture supports 64-bit runtime.
         * @param eventSink Sink to dispatch user actions such as dismissing the warning.
         */
        data class IneligibleDevice(
            val reason: String,
            val detectedRamGb: Double,
            val minRequiredRamGb: Double,
            val is64BitSupported: Boolean,
            val eventSink: (Event) -> Unit,
        ) : State

        /**
         * State displayed when the dashboard is active and ready.
         * Individual dashboard sections are rendered autonomously via CircuitX SubCircuits.
         *
         * @param eventSink Sink to dispatch high-level navigation and configuration events.
         */
        data class Success(
            val eventSink: (Event) -> Unit,
        ) : State
    }

    /**
     * User actions and navigation events dispatched from [HomeScreen].
     */
    @Serializable
    sealed interface Event : CircuitUiEvent {
        /** Navigate to chat screen with the specified [topic]. */
        data class TopicSelected(
            val topic: CodingTopic,
        ) : Event

        /** Navigate to an existing session chat screen by [topic] and [sessionId]. */
        data class SessionSelected(
            val topic: CodingTopic,
            val sessionId: String,
        ) : Event

        /** Navigate to the chapter overview for [courseId]. */
        data class CourseClicked(
            val courseId: String,
        ) : Event

        /** Navigate to model management / picker screen. */
        data object ManageModels : Event

        /** Navigate to guided lesson catalog screen. */
        data object GuidedLessons : Event

        /** Navigate to full session history screen. */
        data object ViewAllSessions : Event

        /** Navigate to onboarding app tour. */
        data object AppTour : Event

        /** Navigate to settings screen. */
        data object OpenSettings : Event

        /** Dismiss the hardware ineligibility warning dialog/screen. */
        data object DismissIneligibilityWarning : Event
    }
}
