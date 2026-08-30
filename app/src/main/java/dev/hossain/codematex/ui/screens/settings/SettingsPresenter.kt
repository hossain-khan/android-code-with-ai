package dev.hossain.codematex.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.BuildConfig
import dev.hossain.codematex.data.model.CodeTheme
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.data.repository.ChatSessionRepository
import dev.hossain.codematex.data.repository.ModelDownloadPreferences
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.data.repository.UserPreferencesStore
import dev.hossain.codematex.ui.screens.aimodels.ModelPickerScreen
import dev.hossain.codematex.ui.screens.onboarding.OnboardingScreen
import dev.hossain.codematex.ui.screens.settings.code.CodeBlockSettingsScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Presenter managing user settings state, preference updates, storage stats, and data purges.
 */
@AssistedInject
class SettingsPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: SettingsScreen,
    private val userPreferencesStore: UserPreferencesStore,
    private val modelRepository: ModelRepository,
    private val chatSessionRepository: ChatSessionRepository,
    private val modelDownloadPreferences: ModelDownloadPreferences,
) : Presenter<SettingsScreen.State> {
    @Composable
    override fun present(): SettingsScreen.State {
        val coroutineScope = rememberCoroutineScope()

        val selectedPersona by userPreferencesStore.selectedPersonaFlow
            .collectAsState(initial = TutorPersona.SENIOR_ENGINEER)
        val isWifiOnlyDownload by userPreferencesStore.isWifiOnlyDownloadEnabledFlow
            .collectAsState(initial = true)
        val showLineNumbers by userPreferencesStore.showLineNumbersFlow
            .collectAsState(initial = true)
        val codeTheme by userPreferencesStore.codeThemeFlow
            .collectAsState(initial = CodeTheme.TOMORROW)
        val hapticFeedbackEnabled by userPreferencesStore.hapticFeedbackEnabledFlow
            .collectAsState(initial = true)
        val ramEvictionMinutes by userPreferencesStore.ramEvictionMinutesFlow
            .collectAsState(initial = 3)

        val availableModels by modelRepository
            .getAvailableModels()
            .collectAsState(initial = emptyList())
        val sessions by chatSessionRepository
            .getAllSessions()
            .collectAsState(initial = emptyList())

        var showPersonaDialog by rememberRetained { mutableStateOf(false) }
        var showRamEvictionDialog by rememberRetained { mutableStateOf(false) }
        var showClearHistoryConfirmation by rememberRetained { mutableStateOf(false) }

        val downloadedModels = availableModels.filter { it.downloadStatus == DownloadStatus.DOWNLOADED }
        val storageUsedBytes = downloadedModels.sumOf { it.sizeBytes }

        val eventSink: (SettingsScreen.Event) -> Unit = { event ->
            when (event) {
                is SettingsScreen.Event.PersonaSelected -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setSelectedPersona(event.persona)
                        } catch (e: Exception) {
                            Timber.e(e, "SettingsPresenter: Failed to update persona")
                        }
                    }
                    showPersonaDialog = false
                }

                is SettingsScreen.Event.ShowPersonaDialog -> {
                    showPersonaDialog = event.show
                }

                is SettingsScreen.Event.WifiOnlyToggled -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setWifiOnlyDownloadEnabled(event.enabled)
                            modelDownloadPreferences.setDownloadOverWifiOnly(event.enabled)
                        } catch (e: Exception) {
                            Timber.e(e, "SettingsPresenter: Failed to update wifi only download")
                        }
                    }
                }

                is SettingsScreen.Event.LineNumbersToggled -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setShowLineNumbers(event.enabled)
                        } catch (e: Exception) {
                            Timber.e(e, "SettingsPresenter: Failed to update line numbers")
                        }
                    }
                }

                is SettingsScreen.Event.HapticsToggled -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setHapticFeedbackEnabled(event.enabled)
                        } catch (e: Exception) {
                            Timber.e(e, "SettingsPresenter: Failed to update haptics")
                        }
                    }
                }

                is SettingsScreen.Event.RamEvictionSelected -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setRamEvictionMinutes(event.minutes)
                        } catch (e: Exception) {
                            Timber.e(e, "SettingsPresenter: Failed to update RAM eviction minutes")
                        }
                    }
                    showRamEvictionDialog = false
                }

                is SettingsScreen.Event.ShowRamEvictionDialog -> {
                    showRamEvictionDialog = event.show
                }

                SettingsScreen.Event.CodeBlockSettingsClicked -> {
                    navigator.goTo(CodeBlockSettingsScreen)
                }

                SettingsScreen.Event.ManageModelsClicked -> {
                    navigator.goTo(ModelPickerScreen)
                }

                is SettingsScreen.Event.ShowClearHistoryDialog -> {
                    showClearHistoryConfirmation = event.show
                }

                SettingsScreen.Event.ConfirmClearHistory -> {
                    coroutineScope.launch {
                        try {
                            chatSessionRepository.clearAllSessions()
                            Timber.d("SettingsPresenter: Successfully cleared all chat sessions")
                        } catch (e: Exception) {
                            Timber.e(e, "SettingsPresenter: Failed to clear chat history")
                        }
                    }
                    showClearHistoryConfirmation = false
                }

                SettingsScreen.Event.ReplayTourClicked -> {
                    navigator.goTo(OnboardingScreen)
                }

                SettingsScreen.Event.OpenFeedbackClicked -> {
                    // Handled in UI via LocalUriHandler
                }

                SettingsScreen.Event.BackClicked -> {
                    navigator.pop()
                }
            }
        }

        return SettingsScreen.State.Content(
            selectedPersona = selectedPersona,
            isWifiOnlyDownload = isWifiOnlyDownload,
            showLineNumbers = showLineNumbers,
            codeTheme = codeTheme,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            ramEvictionMinutes = ramEvictionMinutes,
            storageUsedBytes = storageUsedBytes,
            downloadedModelCount = downloadedModels.size,
            sessionCount = sessions.size,
            showClearHistoryConfirmation = showClearHistoryConfirmation,
            showPersonaDialog = showPersonaDialog,
            showRamEvictionDialog = showRamEvictionDialog,
            appVersion = "v${BuildConfig.VERSION_NAME}",
            eventSink = eventSink,
        )
    }

    @CircuitInject(SettingsScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
            navigator: Navigator,
            screen: SettingsScreen,
        ): SettingsPresenter
    }
}
