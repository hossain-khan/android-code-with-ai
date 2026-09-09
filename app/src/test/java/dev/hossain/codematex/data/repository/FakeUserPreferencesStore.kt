package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.CodeBlockPreset
import dev.hossain.codematex.data.model.CodeBlockSettings
import dev.hossain.codematex.data.model.CodeFontSize
import dev.hossain.codematex.data.model.CodeTheme
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DeveloperProfile
import dev.hossain.codematex.data.model.TutorPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import java.io.IOException

/**
 * In-memory fake of [UserPreferencesStore] for unit tests.
 */
class FakeUserPreferencesStore(
    initialSelectedPersona: TutorPersona = TutorPersona.SENIOR_ENGINEER,
    initialDismissedTopics: Set<String> = emptySet(),
    initialOnboardingCompleted: Boolean = false,
    initialWifiOnlyDownload: Boolean = true,
    initialShowLineNumbers: Boolean = false,
    initialHapticFeedback: Boolean = true,
    initialRamEvictionMinutes: Int = 3,
    initialCodeTheme: CodeTheme = CodeTheme.TOMORROW,
    initialShowLanguageLabel: Boolean = true,
    initialShowCopyButton: Boolean = true,
    initialCodeBlockPreset: CodeBlockPreset = CodeBlockPreset.COMPACT,
    initialCodeFontSize: CodeFontSize = CodeFontSize.MEDIUM,
    initialDeveloperProfile: DeveloperProfile = DeveloperProfile(),
) : UserPreferencesStore {
    override val selectedPersonaFlow: Flow<TutorPersona>
        field = MutableStateFlow(initialSelectedPersona)

    override val dismissedCourseBannerTopicsFlow: Flow<Set<String>>
        field = MutableStateFlow(initialDismissedTopics)

    override val isOnboardingCompletedFlow: Flow<Boolean>
        field = MutableStateFlow(initialOnboardingCompleted)

    override val isWifiOnlyDownloadEnabledFlow: Flow<Boolean>
        field = MutableStateFlow(initialWifiOnlyDownload)

    override val showLineNumbersFlow: Flow<Boolean>
        field = MutableStateFlow(initialShowLineNumbers)

    override val hapticFeedbackEnabledFlow: Flow<Boolean>
        field = MutableStateFlow(initialHapticFeedback)

    override val ramEvictionMinutesFlow: Flow<Int>
        field = MutableStateFlow(initialRamEvictionMinutes)

    override val codeThemeFlow: Flow<CodeTheme>
        field = MutableStateFlow(initialCodeTheme)

    override val showLanguageLabelFlow: Flow<Boolean>
        field = MutableStateFlow(initialShowLanguageLabel)

    override val showCopyButtonFlow: Flow<Boolean>
        field = MutableStateFlow(initialShowCopyButton)

    override val codeBlockPresetFlow: Flow<CodeBlockPreset>
        field = MutableStateFlow(initialCodeBlockPreset)

    override val codeFontSizeFlow: Flow<CodeFontSize>
        field = MutableStateFlow(initialCodeFontSize)

    override val codeBlockSettingsFlow: Flow<CodeBlockSettings>
        get() =
            combine(
                codeThemeFlow,
                combine(showLineNumbersFlow, showLanguageLabelFlow, showCopyButtonFlow) { lines, lang, copy ->
                    Triple(lines, lang, copy)
                },
                combine(codeBlockPresetFlow, codeFontSizeFlow) { preset, font ->
                    preset to font
                },
            ) { theme, (lines, lang, copy), (preset, font) ->
                CodeBlockSettings(
                    theme = theme,
                    showLineNumbers = lines,
                    showLanguageLabel = lang,
                    showCopyButton = copy,
                    preset = preset,
                    fontSize = font,
                )
            }

    override val developerProfileFlow: Flow<DeveloperProfile>
        field = MutableStateFlow(initialDeveloperProfile)

    var shouldThrowOnWrite: Boolean = false

    override suspend fun getSelectedPersona(): TutorPersona = selectedPersonaFlow.value

    override suspend fun setSelectedPersona(persona: TutorPersona) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        selectedPersonaFlow.value = persona
    }

    override suspend fun dismissCourseBanner(topic: CodingTopic) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        dismissedCourseBannerTopicsFlow.value = dismissedCourseBannerTopicsFlow.value + topic.name
    }

    override suspend fun isOnboardingCompleted(): Boolean = isOnboardingCompletedFlow.value

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        isOnboardingCompletedFlow.value = completed
    }

    override suspend fun isWifiOnlyDownloadEnabled(): Boolean = isWifiOnlyDownloadEnabledFlow.value

    override suspend fun setWifiOnlyDownloadEnabled(enabled: Boolean) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        isWifiOnlyDownloadEnabledFlow.value = enabled
    }

    override suspend fun isShowLineNumbersEnabled(): Boolean = showLineNumbersFlow.value

    override suspend fun setShowLineNumbers(show: Boolean) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        showLineNumbersFlow.value = show
    }

    override suspend fun isHapticFeedbackEnabled(): Boolean = hapticFeedbackEnabledFlow.value

    override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        hapticFeedbackEnabledFlow.value = enabled
    }

    override suspend fun getRamEvictionMinutes(): Int = ramEvictionMinutesFlow.value

    override suspend fun setRamEvictionMinutes(minutes: Int) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        ramEvictionMinutesFlow.value = minutes
    }

    override suspend fun getCodeTheme(): CodeTheme = codeThemeFlow.value

    override suspend fun setCodeTheme(theme: CodeTheme) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        codeThemeFlow.value = theme
    }

    override suspend fun isShowLanguageLabelEnabled(): Boolean = showLanguageLabelFlow.value

    override suspend fun setShowLanguageLabel(show: Boolean) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        showLanguageLabelFlow.value = show
    }

    override suspend fun isShowCopyButtonEnabled(): Boolean = showCopyButtonFlow.value

    override suspend fun setShowCopyButton(show: Boolean) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        showCopyButtonFlow.value = show
    }

    override suspend fun getCodeBlockPreset(): CodeBlockPreset = codeBlockPresetFlow.value

    override suspend fun setCodeBlockPreset(preset: CodeBlockPreset) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        codeBlockPresetFlow.value = preset
    }

    override suspend fun getCodeFontSize(): CodeFontSize = codeFontSizeFlow.value

    override suspend fun setCodeFontSize(fontSize: CodeFontSize) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        codeFontSizeFlow.value = fontSize
    }

    override suspend fun getDeveloperProfile(): DeveloperProfile = developerProfileFlow.value

    override suspend fun setDeveloperProfile(profile: DeveloperProfile) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        developerProfileFlow.value = profile
    }
}
