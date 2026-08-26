package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.TutorPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/**
 * In-memory fake of [UserPreferencesStore] for unit tests.
 */
class FakeUserPreferencesStore(
    initialSelectedPersona: TutorPersona = TutorPersona.SENIOR_ENGINEER,
) : UserPreferencesStore {
    override val selectedPersonaFlow: Flow<TutorPersona>
        field = MutableStateFlow(initialSelectedPersona)

    var shouldThrowOnWrite: Boolean = false

    override suspend fun getSelectedPersona(): TutorPersona = selectedPersonaFlow.value

    override suspend fun setSelectedPersona(persona: TutorPersona) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        selectedPersonaFlow.value = persona
    }
}
