package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.TutorPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * In-memory fake of [UserPreferencesStore] for unit tests.
 */
class FakeUserPreferencesStore(
    initialSelectedPersona: TutorPersona = TutorPersona.SENIOR_ENGINEER,
) : UserPreferencesStore {
    private val _selectedPersonaFlow = MutableStateFlow(initialSelectedPersona)

    override val selectedPersonaFlow: Flow<TutorPersona> = _selectedPersonaFlow.asStateFlow()

    var shouldThrowOnWrite: Boolean = false

    override suspend fun getSelectedPersona(): TutorPersona = _selectedPersonaFlow.value

    override suspend fun setSelectedPersona(persona: TutorPersona) {
        if (shouldThrowOnWrite) {
            throw java.io.IOException("Fake disk write failure")
        }
        _selectedPersonaFlow.value = persona
    }
}
