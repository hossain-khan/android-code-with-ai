package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dev.hossain.codematex.data.model.CodeBlockPreset
import dev.hossain.codematex.data.model.CodeBlockSettings
import dev.hossain.codematex.data.model.CodeFontSize
import dev.hossain.codematex.data.model.CodeTheme
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.DeveloperExperienceLevel
import dev.hossain.codematex.data.model.DeveloperProfile
import dev.hossain.codematex.data.model.TutorPersona
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Persistent user preferences storage across app launches and sessions.
 */
interface UserPreferencesStore {
    /**
     * Observable flow of the selected tutor persona.
     */
    val selectedPersonaFlow: Flow<TutorPersona>

    /**
     * Observable flow of topics where the user has dismissed the guided course banner in chat.
     */
    val dismissedCourseBannerTopicsFlow: Flow<Set<String>>

    /**
     * Observable flow of whether the user has completed or skipped the first-time onboarding walkthrough.
     */
    val isOnboardingCompletedFlow: Flow<Boolean>

    /**
     * Observable flow of whether model downloads are restricted to Wi-Fi only.
     */
    val isWifiOnlyDownloadEnabledFlow: Flow<Boolean>

    /**
     * Observable flow of whether line numbers are displayed in syntax-highlighted code blocks.
     */
    val showLineNumbersFlow: Flow<Boolean>

    /**
     * Observable flow of whether haptic feedback vibrations are enabled on user actions.
     */
    val hapticFeedbackEnabledFlow: Flow<Boolean>

    /**
     * Observable flow of the idle RAM eviction timeout in minutes before background model unloading.
     */
    val ramEvictionMinutesFlow: Flow<Int>

    /**
     * Observable flow of the selected code block syntax highlighting theme.
     */
    val codeThemeFlow: Flow<CodeTheme>

    /**
     * Observable flow of whether the language identifier badge is shown on code block headers.
     */
    val showLanguageLabelFlow: Flow<Boolean>

    /**
     * Observable flow of whether the copy action button is shown on code block headers.
     */
    val showCopyButtonFlow: Flow<Boolean>

    /**
     * Observable flow of the code block padding and density preset.
     */
    val codeBlockPresetFlow: Flow<CodeBlockPreset>

    /**
     * Observable flow of the code text font size preset.
     */
    val codeFontSizeFlow: Flow<CodeFontSize>

    /**
     * Observable flow of the unified [CodeBlockSettings] snapshot.
     */
    val codeBlockSettingsFlow: Flow<CodeBlockSettings>

    /**
     * Observable flow of the user's custom [DeveloperProfile] and context.
     */
    val developerProfileFlow: Flow<DeveloperProfile>

    /**
     * Returns the currently selected tutor persona, or [TutorPersona.SENIOR_ENGINEER] if none is stored.
     */
    suspend fun getSelectedPersona(): TutorPersona

    /**
     * Persists the preferred [persona], awaiting completion so subsequent reads observe the change immediately.
     */
    suspend fun setSelectedPersona(persona: TutorPersona)

    /**
     * Marks the guided course banner for [topic] as permanently dismissed by the user.
     */
    suspend fun dismissCourseBanner(topic: CodingTopic)

    /**
     * Returns whether the onboarding walkthrough has been completed.
     */
    suspend fun isOnboardingCompleted(): Boolean

    /**
     * Persists the onboarding completion state.
     */
    suspend fun setOnboardingCompleted(completed: Boolean)

    /**
     * Returns whether model downloads are restricted to Wi-Fi only.
     */
    suspend fun isWifiOnlyDownloadEnabled(): Boolean

    /**
     * Persists the Wi-Fi only download constraint.
     */
    suspend fun setWifiOnlyDownloadEnabled(enabled: Boolean)

    /**
     * Returns whether line numbers are enabled in code blocks.
     */
    suspend fun isShowLineNumbersEnabled(): Boolean

    /**
     * Persists the line numbers display preference.
     */
    suspend fun setShowLineNumbers(show: Boolean)

    /**
     * Returns whether haptic feedback is enabled.
     */
    suspend fun isHapticFeedbackEnabled(): Boolean

    /**
     * Persists the haptic feedback preference.
     */
    suspend fun setHapticFeedbackEnabled(enabled: Boolean)

    /**
     * Returns the idle RAM eviction timeout in minutes.
     */
    suspend fun getRamEvictionMinutes(): Int

    /**
     * Persists the idle RAM eviction timeout in minutes.
     */
    suspend fun setRamEvictionMinutes(minutes: Int)

    /**
     * Returns the active code syntax highlighting theme.
     */
    suspend fun getCodeTheme(): CodeTheme

    /**
     * Persists the code syntax highlighting theme.
     */
    suspend fun setCodeTheme(theme: CodeTheme)

    /**
     * Returns whether language labels are displayed on code headers.
     */
    suspend fun isShowLanguageLabelEnabled(): Boolean

    /**
     * Persists the language label display preference.
     */
    suspend fun setShowLanguageLabel(show: Boolean)

    /**
     * Returns whether the copy button is displayed on code headers.
     */
    suspend fun isShowCopyButtonEnabled(): Boolean

    /**
     * Persists the copy button display preference.
     */
    suspend fun setShowCopyButton(show: Boolean)

    /**
     * Returns the active code block layout density preset.
     */
    suspend fun getCodeBlockPreset(): CodeBlockPreset

    /**
     * Persists the code block layout density preset.
     */
    suspend fun setCodeBlockPreset(preset: CodeBlockPreset)

    /**
     * Returns the active code font size preset.
     */
    suspend fun getCodeFontSize(): CodeFontSize

    /**
     * Persists the code font size preset.
     */
    suspend fun setCodeFontSize(fontSize: CodeFontSize)

    /**
     * Returns the user's custom developer profile context snapshot.
     */
    suspend fun getDeveloperProfile(): DeveloperProfile

    /**
     * Persists the user's custom developer profile context.
     */
    suspend fun setDeveloperProfile(profile: DeveloperProfile)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class UserPreferencesStoreImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : UserPreferencesStore {
        override val selectedPersonaFlow: Flow<TutorPersona> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    val storedName = prefs[KEY_SELECTED_PERSONA] ?: return@map TutorPersona.SENIOR_ENGINEER
                    try {
                        TutorPersona.valueOf(storedName)
                    } catch (e: IllegalArgumentException) {
                        Timber.w(e, "Unknown stored persona '$storedName', defaulting to ${TutorPersona.SENIOR_ENGINEER}")
                        TutorPersona.SENIOR_ENGINEER
                    }
                }.distinctUntilChanged()

        override val dismissedCourseBannerTopicsFlow: Flow<Set<String>> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    prefs[KEY_DISMISSED_COURSE_BANNER_TOPICS] ?: emptySet()
                }.distinctUntilChanged()

        override val isOnboardingCompletedFlow: Flow<Boolean> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    prefs[KEY_ONBOARDING_COMPLETED] ?: false
                }.distinctUntilChanged()

        override val isWifiOnlyDownloadEnabledFlow: Flow<Boolean> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    prefs[KEY_WIFI_ONLY_DOWNLOAD] ?: true
                }.distinctUntilChanged()

        override val showLineNumbersFlow: Flow<Boolean> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    prefs[KEY_SHOW_LINE_NUMBERS] ?: true
                }.distinctUntilChanged()

        override val hapticFeedbackEnabledFlow: Flow<Boolean> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    prefs[KEY_HAPTIC_FEEDBACK] ?: true
                }.distinctUntilChanged()

        override val ramEvictionMinutesFlow: Flow<Int> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    prefs[KEY_RAM_EVICTION_MINUTES] ?: 3
                }.distinctUntilChanged()

        override val codeThemeFlow: Flow<CodeTheme> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    val stored = prefs[KEY_CODE_THEME] ?: return@map CodeTheme.TOMORROW
                    try {
                        CodeTheme.valueOf(stored)
                    } catch (e: IllegalArgumentException) {
                        CodeTheme.TOMORROW
                    }
                }.distinctUntilChanged()

        override val showLanguageLabelFlow: Flow<Boolean> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    prefs[KEY_SHOW_LANGUAGE_LABEL] ?: true
                }.distinctUntilChanged()

        override val showCopyButtonFlow: Flow<Boolean> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    prefs[KEY_SHOW_COPY_BUTTON] ?: true
                }.distinctUntilChanged()

        override val codeBlockPresetFlow: Flow<CodeBlockPreset> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    val stored = prefs[KEY_CODE_BLOCK_PRESET] ?: return@map CodeBlockPreset.COMFORTABLE
                    try {
                        CodeBlockPreset.valueOf(stored)
                    } catch (e: IllegalArgumentException) {
                        CodeBlockPreset.COMFORTABLE
                    }
                }.distinctUntilChanged()

        override val codeFontSizeFlow: Flow<CodeFontSize> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    val stored = prefs[KEY_CODE_FONT_SIZE] ?: return@map CodeFontSize.MEDIUM
                    try {
                        CodeFontSize.valueOf(stored)
                    } catch (e: IllegalArgumentException) {
                        CodeFontSize.MEDIUM
                    }
                }.distinctUntilChanged()

        override val codeBlockSettingsFlow: Flow<CodeBlockSettings> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    val theme =
                        prefs[KEY_CODE_THEME]?.let {
                            try {
                                CodeTheme.valueOf(it)
                            } catch (e: IllegalArgumentException) {
                                CodeTheme.TOMORROW
                            }
                        } ?: CodeTheme.TOMORROW
                    val showLines = prefs[KEY_SHOW_LINE_NUMBERS] ?: true
                    val showLang = prefs[KEY_SHOW_LANGUAGE_LABEL] ?: true
                    val showCopy = prefs[KEY_SHOW_COPY_BUTTON] ?: true
                    val preset =
                        prefs[KEY_CODE_BLOCK_PRESET]?.let {
                            try {
                                CodeBlockPreset.valueOf(it)
                            } catch (e: IllegalArgumentException) {
                                CodeBlockPreset.COMFORTABLE
                            }
                        } ?: CodeBlockPreset.COMFORTABLE
                    val fontSize =
                        prefs[KEY_CODE_FONT_SIZE]?.let {
                            try {
                                CodeFontSize.valueOf(it)
                            } catch (e: IllegalArgumentException) {
                                CodeFontSize.MEDIUM
                            }
                        } ?: CodeFontSize.MEDIUM

                    CodeBlockSettings(
                        theme = theme,
                        showLineNumbers = showLines,
                        showLanguageLabel = showLang,
                        showCopyButton = showCopy,
                        preset = preset,
                        fontSize = fontSize,
                    )
                }.distinctUntilChanged()

        override val developerProfileFlow: Flow<DeveloperProfile> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "UserPreferencesStoreImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs ->
                    val enabled = prefs[KEY_DEV_PROFILE_ENABLED] ?: false
                    val experienceLevel =
                        prefs[KEY_DEV_PROFILE_EXPERIENCE]?.let {
                            try {
                                DeveloperExperienceLevel.valueOf(it)
                            } catch (e: IllegalArgumentException) {
                                DeveloperExperienceLevel.INTERMEDIATE
                            }
                        } ?: DeveloperExperienceLevel.INTERMEDIATE
                    val primaryStack = prefs[KEY_DEV_PROFILE_STACK] ?: ""
                    val customDirectives = prefs[KEY_DEV_PROFILE_DIRECTIVES] ?: ""

                    DeveloperProfile(
                        enabled = enabled,
                        experienceLevel = experienceLevel,
                        primaryStack = primaryStack,
                        customDirectives = customDirectives,
                    )
                }.distinctUntilChanged()

        override suspend fun getSelectedPersona(): TutorPersona = selectedPersonaFlow.first()

        override suspend fun setSelectedPersona(persona: TutorPersona) {
            dataStore.edit { prefs ->
                prefs[KEY_SELECTED_PERSONA] = persona.name
            }
        }

        override suspend fun dismissCourseBanner(topic: CodingTopic) {
            dataStore.edit { prefs ->
                val current = prefs[KEY_DISMISSED_COURSE_BANNER_TOPICS] ?: emptySet()
                prefs[KEY_DISMISSED_COURSE_BANNER_TOPICS] = current + topic.name
            }
        }

        override suspend fun isOnboardingCompleted(): Boolean = isOnboardingCompletedFlow.first()

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            dataStore.edit { prefs ->
                prefs[KEY_ONBOARDING_COMPLETED] = completed
            }
        }

        override suspend fun isWifiOnlyDownloadEnabled(): Boolean = isWifiOnlyDownloadEnabledFlow.first()

        override suspend fun setWifiOnlyDownloadEnabled(enabled: Boolean) {
            dataStore.edit { prefs ->
                prefs[KEY_WIFI_ONLY_DOWNLOAD] = enabled
            }
        }

        override suspend fun isShowLineNumbersEnabled(): Boolean = showLineNumbersFlow.first()

        override suspend fun setShowLineNumbers(show: Boolean) {
            dataStore.edit { prefs ->
                prefs[KEY_SHOW_LINE_NUMBERS] = show
            }
        }

        override suspend fun isHapticFeedbackEnabled(): Boolean = hapticFeedbackEnabledFlow.first()

        override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
            dataStore.edit { prefs ->
                prefs[KEY_HAPTIC_FEEDBACK] = enabled
            }
        }

        override suspend fun getRamEvictionMinutes(): Int = ramEvictionMinutesFlow.first()

        override suspend fun setRamEvictionMinutes(minutes: Int) {
            dataStore.edit { prefs ->
                prefs[KEY_RAM_EVICTION_MINUTES] = minutes
            }
        }

        override suspend fun getCodeTheme(): CodeTheme = codeThemeFlow.first()

        override suspend fun setCodeTheme(theme: CodeTheme) {
            dataStore.edit { prefs ->
                prefs[KEY_CODE_THEME] = theme.name
            }
        }

        override suspend fun isShowLanguageLabelEnabled(): Boolean = showLanguageLabelFlow.first()

        override suspend fun setShowLanguageLabel(show: Boolean) {
            dataStore.edit { prefs ->
                prefs[KEY_SHOW_LANGUAGE_LABEL] = show
            }
        }

        override suspend fun isShowCopyButtonEnabled(): Boolean = showCopyButtonFlow.first()

        override suspend fun setShowCopyButton(show: Boolean) {
            dataStore.edit { prefs ->
                prefs[KEY_SHOW_COPY_BUTTON] = show
            }
        }

        override suspend fun getCodeBlockPreset(): CodeBlockPreset = codeBlockPresetFlow.first()

        override suspend fun setCodeBlockPreset(preset: CodeBlockPreset) {
            dataStore.edit { prefs ->
                prefs[KEY_CODE_BLOCK_PRESET] = preset.name
            }
        }

        override suspend fun getCodeFontSize(): CodeFontSize = codeFontSizeFlow.first()

        override suspend fun setCodeFontSize(fontSize: CodeFontSize) {
            dataStore.edit { prefs ->
                prefs[KEY_CODE_FONT_SIZE] = fontSize.name
            }
        }

        override suspend fun getDeveloperProfile(): DeveloperProfile = developerProfileFlow.first()

        override suspend fun setDeveloperProfile(profile: DeveloperProfile) {
            dataStore.edit { prefs ->
                prefs[KEY_DEV_PROFILE_ENABLED] = profile.enabled
                prefs[KEY_DEV_PROFILE_EXPERIENCE] = profile.experienceLevel.name
                prefs[KEY_DEV_PROFILE_STACK] = profile.primaryStack
                prefs[KEY_DEV_PROFILE_DIRECTIVES] = profile.customDirectives
            }
        }

        companion object {
            private val KEY_SELECTED_PERSONA = stringPreferencesKey("selected_tutor_persona")
            private val KEY_DISMISSED_COURSE_BANNER_TOPICS = stringSetPreferencesKey("dismissed_course_banner_topics")
            private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
            private val KEY_WIFI_ONLY_DOWNLOAD = booleanPreferencesKey("wifi_only_download")
            private val KEY_SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
            private val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
            private val KEY_RAM_EVICTION_MINUTES = intPreferencesKey("ram_eviction_minutes")
            private val KEY_CODE_THEME = stringPreferencesKey("code_block_theme")
            private val KEY_SHOW_LANGUAGE_LABEL = booleanPreferencesKey("code_show_language_label")
            private val KEY_SHOW_COPY_BUTTON = booleanPreferencesKey("code_show_copy_button")
            private val KEY_CODE_BLOCK_PRESET = stringPreferencesKey("code_block_preset")
            private val KEY_CODE_FONT_SIZE = stringPreferencesKey("code_font_size")
            private val KEY_DEV_PROFILE_ENABLED = booleanPreferencesKey("dev_profile_enabled")
            private val KEY_DEV_PROFILE_EXPERIENCE = stringPreferencesKey("dev_profile_experience")
            private val KEY_DEV_PROFILE_STACK = stringPreferencesKey("dev_profile_stack")
            private val KEY_DEV_PROFILE_DIRECTIVES = stringPreferencesKey("dev_profile_directives")
        }
    }
