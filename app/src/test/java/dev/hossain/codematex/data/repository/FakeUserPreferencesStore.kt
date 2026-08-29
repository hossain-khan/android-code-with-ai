package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.TutorPersona
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/**
 * In-memory fake of [UserPreferencesStore] for unit tests.
 */
class FakeUserPreferencesStore(
    initialSelectedPersona: TutorPersona = TutorPersona.SENIOR_ENGINEER,
    initialDismissedTopics: Set<String> = emptySet(),
    initialOnboardingCompleted: Boolean = false,
) : UserPreferencesStore {
    override val selectedPersonaFlow: Flow<TutorPersona>
        field = MutableStateFlow(initialSelectedPersona)

    override val dismissedCourseBannerTopicsFlow: Flow<Set<String>>
        field = MutableStateFlow(initialDismissedTopics)

    override val isOnboardingCompletedFlow: Flow<Boolean>
        field = MutableStateFlow(initialOnboardingCompleted)

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
}
