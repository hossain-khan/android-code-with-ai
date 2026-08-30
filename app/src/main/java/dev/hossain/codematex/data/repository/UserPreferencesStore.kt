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
import dev.hossain.codematex.data.model.CodingTopic
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

        companion object {
            private val KEY_SELECTED_PERSONA = stringPreferencesKey("selected_tutor_persona")
            private val KEY_DISMISSED_COURSE_BANNER_TOPICS = stringSetPreferencesKey("dismissed_course_banner_topics")
            private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
            private val KEY_WIFI_ONLY_DOWNLOAD = booleanPreferencesKey("wifi_only_download")
            private val KEY_SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
            private val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback_enabled")
            private val KEY_RAM_EVICTION_MINUTES = intPreferencesKey("ram_eviction_minutes")
        }
    }
