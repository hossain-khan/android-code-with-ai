package dev.hossain.codematex.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.data.repository.UserPreferencesStore
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import dev.hossain.codematex.ui.screens.home.HomeScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Presenter for managing onboarding steps, user preferences persistence, and initial routing.
 */
@AssistedInject
class OnboardingPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: OnboardingScreen,
    private val userPreferencesStore: UserPreferencesStore,
    private val modelRepository: ModelRepository,
) : Presenter<OnboardingScreen.State> {
    @Composable
    override fun present(): OnboardingScreen.State {
        var currentPage by rememberRetained { mutableIntStateOf(0) }
        val coroutineScope = rememberCoroutineScope()
        val hasDownloadedModel = modelRepository.getSelectedModel() != null

        val completeOnboardingAndNavigate: () -> Unit = {
            coroutineScope.launch {
                try {
                    userPreferencesStore.setOnboardingCompleted(true)
                    Timber.d("OnboardingPresenter: Onboarding marked completed")
                } catch (e: Exception) {
                    Timber.e(e, "OnboardingPresenter: Failed to save onboarding completed state")
                }

                if (hasDownloadedModel) {
                    navigator.resetRoot(HomeScreen)
                } else {
                    navigator.resetRoot(HomeScreen)
                    navigator.goTo(ModelPickerScreen)
                }
            }
        }

        val eventSink: (OnboardingScreen.Event) -> Unit = { event ->
            when (event) {
                is OnboardingScreen.Event.PageChanged -> {
                    currentPage = event.page.coerceIn(0, 3)
                }

                OnboardingScreen.Event.NextClicked -> {
                    if (currentPage < 3) {
                        currentPage++
                    } else {
                        completeOnboardingAndNavigate()
                    }
                }

                OnboardingScreen.Event.SkipClicked -> {
                    completeOnboardingAndNavigate()
                }

                OnboardingScreen.Event.GetStartedClicked -> {
                    completeOnboardingAndNavigate()
                }

                OnboardingScreen.Event.OpenFeedbackClicked -> {
                    // Handled by UI via LocalUriHandler
                }
            }
        }

        return OnboardingScreen.State.Content(
            currentPage = currentPage,
            pageCount = 4,
            hasDownloadedModel = hasDownloadedModel,
            eventSink = eventSink,
        )
    }

    @CircuitInject(OnboardingScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
            navigator: Navigator,
            screen: OnboardingScreen,
        ): OnboardingPresenter
    }
}
