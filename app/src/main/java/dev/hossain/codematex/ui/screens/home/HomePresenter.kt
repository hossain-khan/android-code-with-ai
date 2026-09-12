package dev.hossain.codematex.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.system.HardwareEligibility
import dev.hossain.codematex.system.HardwareEligibilityChecker
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import dev.hossain.codematex.ui.screens.chat.ChatScreen
import dev.hossain.codematex.ui.screens.chatsessions.SessionHistoryScreen
import dev.hossain.codematex.ui.screens.lessons.ChapterScreen
import dev.hossain.codematex.ui.screens.lessons.LessonCatalogScreen
import dev.hossain.codematex.ui.screens.onboarding.OnboardingScreen
import dev.hossain.codematex.ui.screens.settings.SettingsScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

@AssistedInject
class HomePresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: HomeScreen,
    private val hardwareEligibilityChecker: HardwareEligibilityChecker,
) : Presenter<HomeScreen.State> {
    @Composable
    override fun present(): HomeScreen.State {
        var isWarningDismissed by rememberRetained { mutableStateOf(false) }
        val hardwareEligibility = remember { hardwareEligibilityChecker.checkEligibility() }

        val eventSink: (HomeScreen.Event) -> Unit = { event ->
            when (event) {
                is HomeScreen.Event.TopicSelected -> {
                    navigator.goTo(ChatScreen(topic = event.topic))
                }

                is HomeScreen.Event.SessionSelected -> {
                    navigator.goTo(ChatScreen(topic = event.topic, sessionId = event.sessionId))
                }

                is HomeScreen.Event.CourseClicked -> {
                    navigator.goTo(ChapterScreen(event.courseId))
                }

                HomeScreen.Event.ManageModels -> {
                    navigator.goTo(ModelPickerScreen)
                }

                HomeScreen.Event.GuidedLessons -> {
                    navigator.goTo(LessonCatalogScreen())
                }

                HomeScreen.Event.ViewAllSessions -> {
                    navigator.goTo(SessionHistoryScreen)
                }

                HomeScreen.Event.AppTour -> {
                    navigator.goTo(OnboardingScreen)
                }

                HomeScreen.Event.OpenSettings -> {
                    navigator.goTo(SettingsScreen)
                }

                HomeScreen.Event.DismissIneligibilityWarning -> {
                    isWarningDismissed = true
                }
            }
        }

        if (hardwareEligibility is HardwareEligibility.Ineligible && !isWarningDismissed) {
            return HomeScreen.State.IneligibleDevice(
                reason = hardwareEligibility.reason,
                detectedRamGb = hardwareEligibility.detectedRamGb,
                minRequiredRamGb = hardwareEligibility.minRequiredRamGb,
                is64BitSupported = hardwareEligibility.is64BitSupported,
                eventSink = eventSink,
            )
        }

        return HomeScreen.State.Success(eventSink = eventSink)
    }

    @CircuitInject(HomeScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: HomeScreen,
        ): HomePresenter
    }
}
