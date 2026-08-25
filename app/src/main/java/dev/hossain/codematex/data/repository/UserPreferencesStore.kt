package dev.hossain.codematex.data.repository

import android.content.Context
import androidx.core.content.edit
import dev.hossain.codematex.data.model.TutorPersona
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        @param:ApplicationContext private val context: Context,
    ) : UserPreferencesStore {
        private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        override val selectedPersonaFlow: Flow<TutorPersona>
            field = MutableStateFlow(loadStoredPersona())

        override var selectedPersona: TutorPersona
            get() = selectedPersonaFlow.value
            set(value) {
                prefs.edit { putString(KEY_SELECTED_PERSONA, value.name) }
                selectedPersonaFlow.value = value
            }

        private fun loadStoredPersona(): TutorPersona {
            val storedName = prefs.getString(KEY_SELECTED_PERSONA, null) ?: return TutorPersona.SENIOR_ENGINEER
            return try {
                TutorPersona.valueOf(storedName)
            } catch (e: IllegalArgumentException) {
                Timber.w(e, "Unknown stored persona '$storedName', defaulting to ${TutorPersona.SENIOR_ENGINEER}")
                TutorPersona.SENIOR_ENGINEER
            }
        }

        companion object {
            private const val KEY_SELECTED_PERSONA = "selected_tutor_persona"
        }
    }
