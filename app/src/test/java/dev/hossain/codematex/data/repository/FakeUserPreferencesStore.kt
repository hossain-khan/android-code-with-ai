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
    private val _selectedPersonaFlow = MutableStateFlow(initialSelectedPersona)
    override val selectedPersonaFlow: Flow<TutorPersona> = _selectedPersonaFlow.asStateFlow()

    override var selectedPersona: TutorPersona
        get() = _selectedPersonaFlow.value
        set(value) {
            _selectedPersonaFlow.value = value
        }
}
