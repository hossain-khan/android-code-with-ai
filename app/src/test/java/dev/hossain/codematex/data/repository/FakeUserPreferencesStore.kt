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
    initialWifiOnlyDownload: Boolean = true,
    initialShowLineNumbers: Boolean = true,
    initialHapticFeedback: Boolean = true,
    initialRamEvictionMinutes: Int = 3,
) : UserPreferencesStore {
    override val selectedPersonaFlow: Flow<TutorPersona>
        field = MutableStateFlow(initialSelectedPersona)

    override val dismissedCourseBannerTopicsFlow: Flow<Set<String>>
        field = MutableStateFlow(initialDismissedTopics)

    override val isOnboardingCompletedFlow: Flow<Boolean>
        field = MutableStateFlow(initialOnboardingCompleted)

    override val isWifiOnlyDownloadEnabledFlow: Flow<Boolean>
        field = MutableStateFlow(initialWifiOnlyDownload)

    override val showLineNumbersFlow: Flow<Boolean>
        field = MutableStateFlow(initialShowLineNumbers)

    override val hapticFeedbackEnabledFlow: Flow<Boolean>
        field = MutableStateFlow(initialHapticFeedback)

    override val ramEvictionMinutesFlow: Flow<Int>
        field = MutableStateFlow(initialRamEvictionMinutes)

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

    override suspend fun isWifiOnlyDownloadEnabled(): Boolean = isWifiOnlyDownloadEnabledFlow.value

    override suspend fun setWifiOnlyDownloadEnabled(enabled: Boolean) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        isWifiOnlyDownloadEnabledFlow.value = enabled
    }

    override suspend fun isShowLineNumbersEnabled(): Boolean = showLineNumbersFlow.value

    override suspend fun setShowLineNumbers(show: Boolean) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        showLineNumbersFlow.value = show
    }

    override suspend fun isHapticFeedbackEnabled(): Boolean = hapticFeedbackEnabledFlow.value

    override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        hapticFeedbackEnabledFlow.value = enabled
    }

    override suspend fun getRamEvictionMinutes(): Int = ramEvictionMinutesFlow.value

    override suspend fun setRamEvictionMinutes(minutes: Int) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        ramEvictionMinutesFlow.value = minutes
    }
}
