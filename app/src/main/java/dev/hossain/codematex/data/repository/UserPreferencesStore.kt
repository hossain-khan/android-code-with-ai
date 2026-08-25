package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.hossain.codematex.data.model.TutorPersona
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Persistent user preferences storage across app launches and sessions.
 */
interface UserPreferencesStore {
    /**
     * The preferred AI Tutor persona selected by the user.
     * Defaults to [TutorPersona.SENIOR_ENGINEER].
     */
    var selectedPersona: TutorPersona

    /**
     * Observable flow of the selected tutor persona.
     */
    val selectedPersonaFlow: Flow<TutorPersona>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class UserPreferencesStoreImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : UserPreferencesStore {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val personaState: StateFlow<TutorPersona> =
            dataStore.data
                .map { prefs ->
                    val storedName = prefs[KEY_SELECTED_PERSONA] ?: return@map TutorPersona.SENIOR_ENGINEER
                    try {
                        TutorPersona.valueOf(storedName)
                    } catch (e: IllegalArgumentException) {
                        Timber.w(e, "Unknown stored persona '$storedName', defaulting to ${TutorPersona.SENIOR_ENGINEER}")
                        TutorPersona.SENIOR_ENGINEER
                    }
                }.stateIn(
                    scope = scope,
                    started = SharingStarted.Eagerly,
                    initialValue = TutorPersona.SENIOR_ENGINEER,
                )

        override val selectedPersonaFlow: Flow<TutorPersona>
            get() = personaState

        override var selectedPersona: TutorPersona
            get() = personaState.value
            set(value) {
                scope.launch {
                    dataStore.edit { prefs ->
                        prefs[KEY_SELECTED_PERSONA] = value.name
                    }
                }
            }

        companion object {
            private val KEY_SELECTED_PERSONA = stringPreferencesKey("selected_tutor_persona")
        }
    }
