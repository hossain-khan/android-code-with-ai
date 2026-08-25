package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.TutorPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory fake of [UserPreferencesStore] for unit tests.
 */
class FakeUserPreferencesStore(
    initialSelectedPersona: TutorPersona = TutorPersona.SENIOR_ENGINEER,
) : UserPreferencesStore {
    override val selectedPersonaFlow: Flow<TutorPersona>
        field = MutableStateFlow(initialSelectedPersona)

    override var selectedPersona: TutorPersona
        get() = selectedPersonaFlow.value
        set(value) {
            selectedPersonaFlow.value = value
        }
}
