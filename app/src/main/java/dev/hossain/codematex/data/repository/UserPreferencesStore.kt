package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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

        companion object {
            private val KEY_SELECTED_PERSONA = stringPreferencesKey("selected_tutor_persona")
            private val KEY_DISMISSED_COURSE_BANNER_TOPICS = stringSetPreferencesKey("dismissed_course_banner_topics")
        }
    }
