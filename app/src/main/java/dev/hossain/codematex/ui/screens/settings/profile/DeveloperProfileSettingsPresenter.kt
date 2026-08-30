package dev.hossain.codematex.ui.screens.settings.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.DeveloperProfile
import dev.hossain.codematex.data.repository.UserPreferencesStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Presenter managing the user's custom developer profile and context customization state.
 */
@AssistedInject
class DeveloperProfileSettingsPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: DeveloperProfileSettingsScreen,
    private val userPreferencesStore: UserPreferencesStore,
) : Presenter<DeveloperProfileSettingsScreen.State> {
    @Composable
    override fun present(): DeveloperProfileSettingsScreen.State {
        val coroutineScope = rememberCoroutineScope()
        val profile by userPreferencesStore.developerProfileFlow
            .collectAsState(initial = DeveloperProfile())

        val eventSink: (DeveloperProfileSettingsScreen.Event) -> Unit = { event ->
            when (event) {
                is DeveloperProfileSettingsScreen.Event.EnabledToggled -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setDeveloperProfile(profile.copy(enabled = event.enabled))
                        } catch (e: Exception) {
                            Timber.e(e, "DeveloperProfileSettingsPresenter: Failed to update enabled state")
                        }
                    }
                }

                is DeveloperProfileSettingsScreen.Event.ExperienceLevelSelected -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setDeveloperProfile(
                                profile.copy(experienceLevel = event.level, enabled = true),
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "DeveloperProfileSettingsPresenter: Failed to update experience level")
                        }
                    }
                }

                is DeveloperProfileSettingsScreen.Event.PrimaryStackChanged -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setDeveloperProfile(profile.copy(primaryStack = event.stack))
                        } catch (e: Exception) {
                            Timber.e(e, "DeveloperProfileSettingsPresenter: Failed to update primary stack")
                        }
                    }
                }

                is DeveloperProfileSettingsScreen.Event.CustomDirectivesChanged -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setDeveloperProfile(profile.copy(customDirectives = event.directives))
                        } catch (e: Exception) {
                            Timber.e(e, "DeveloperProfileSettingsPresenter: Failed to update custom directives")
                        }
                    }
                }

                is DeveloperProfileSettingsScreen.Event.PresetApplied -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setDeveloperProfile(
                                DeveloperProfile(
                                    enabled = true,
                                    experienceLevel = event.preset.experienceLevel,
                                    primaryStack = event.preset.primaryStack,
                                    customDirectives = event.preset.customDirectives,
                                ),
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "DeveloperProfileSettingsPresenter: Failed to apply preset")
                        }
                    }
                }

                DeveloperProfileSettingsScreen.Event.ResetClicked -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setDeveloperProfile(DeveloperProfile())
                        } catch (e: Exception) {
                            Timber.e(e, "DeveloperProfileSettingsPresenter: Failed to reset profile")
                        }
                    }
                }

                DeveloperProfileSettingsScreen.Event.BackClicked -> {
                    navigator.pop()
                }
            }
        }

        val generatedPromptSnippet =
            if (profile.enabled) {
                profile.formatPromptDirectives()
            } else {
                // Show how it would look if enabled for transparent live preview
                profile.copy(enabled = true).formatPromptDirectives()
            }

        return DeveloperProfileSettingsScreen.State.Content(
            profile = profile,
            generatedPromptSnippet = generatedPromptSnippet,
            eventSink = eventSink,
        )
    }

    @CircuitInject(DeveloperProfileSettingsScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
            navigator: Navigator,
            screen: DeveloperProfileSettingsScreen,
        ): DeveloperProfileSettingsPresenter
    }
}
