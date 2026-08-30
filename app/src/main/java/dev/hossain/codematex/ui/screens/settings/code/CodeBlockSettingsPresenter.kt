package dev.hossain.codematex.ui.screens.settings.code

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.CodeBlockSettings
import dev.hossain.codematex.data.repository.UserPreferencesStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Presenter for managing code block customization state and preference updates.
 */
@AssistedInject
class CodeBlockSettingsPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: CodeBlockSettingsScreen,
    private val userPreferencesStore: UserPreferencesStore,
) : Presenter<CodeBlockSettingsScreen.State> {
    @Composable
    override fun present(): CodeBlockSettingsScreen.State {
        val coroutineScope = rememberCoroutineScope()
        val settings by userPreferencesStore.codeBlockSettingsFlow
            .collectAsState(initial = CodeBlockSettings())

        val eventSink: (CodeBlockSettingsScreen.Event) -> Unit = { event ->
            when (event) {
                is CodeBlockSettingsScreen.Event.ThemeSelected -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setCodeTheme(event.theme)
                        } catch (e: Exception) {
                            Timber.e(e, "CodeBlockSettingsPresenter: Failed to update theme")
                        }
                    }
                }

                is CodeBlockSettingsScreen.Event.LineNumbersToggled -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setShowLineNumbers(event.enabled)
                        } catch (e: Exception) {
                            Timber.e(e, "CodeBlockSettingsPresenter: Failed to update line numbers")
                        }
                    }
                }

                is CodeBlockSettingsScreen.Event.LanguageLabelToggled -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setShowLanguageLabel(event.enabled)
                        } catch (e: Exception) {
                            Timber.e(e, "CodeBlockSettingsPresenter: Failed to update language label")
                        }
                    }
                }

                is CodeBlockSettingsScreen.Event.CopyButtonToggled -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setShowCopyButton(event.enabled)
                        } catch (e: Exception) {
                            Timber.e(e, "CodeBlockSettingsPresenter: Failed to update copy button")
                        }
                    }
                }

                is CodeBlockSettingsScreen.Event.PresetSelected -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setCodeBlockPreset(event.preset)
                        } catch (e: Exception) {
                            Timber.e(e, "CodeBlockSettingsPresenter: Failed to update preset")
                        }
                    }
                }

                is CodeBlockSettingsScreen.Event.FontSizeSelected -> {
                    coroutineScope.launch {
                        try {
                            userPreferencesStore.setCodeFontSize(event.fontSize)
                        } catch (e: Exception) {
                            Timber.e(e, "CodeBlockSettingsPresenter: Failed to update font size")
                        }
                    }
                }

                CodeBlockSettingsScreen.Event.BackClicked -> {
                    navigator.pop()
                }
            }
        }

        return CodeBlockSettingsScreen.State.Content(
            settings = settings,
            previewCode = CodeBlockSettingsScreen.SAMPLE_CODE,
            eventSink = eventSink,
        )
    }

    @CircuitInject(CodeBlockSettingsScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(
            navigator: Navigator,
            screen: CodeBlockSettingsScreen,
        ): CodeBlockSettingsPresenter
    }
}
