package dev.hossain.codematex.data.repository

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Persistent preferences for model downloads.
 *
 * Delegates to [UserPreferencesStore] as the single authoritative owner of user preferences.
 * Tests can supply an in-memory fake to avoid depending on Android DataStore.
 */
interface ModelDownloadPreferences {
    /**
     * Observable flow of the Wi-Fi only download preference.
     */
    val downloadOverWifiOnlyFlow: Flow<Boolean>

    /**
     * Whether model downloads are currently restricted to unmetered Wi-Fi networks only.
     */
    suspend fun getDownloadOverWifiOnly(): Boolean

    /**
     * Persists the Wi-Fi only download preference, awaiting completion.
     */
    suspend fun setDownloadOverWifiOnly(enabled: Boolean)
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelDownloadPreferencesImpl
    @Inject
    constructor(
        private val userPreferencesStore: UserPreferencesStore,
    ) : ModelDownloadPreferences {
        override val downloadOverWifiOnlyFlow: Flow<Boolean> =
            userPreferencesStore.isWifiOnlyDownloadEnabledFlow

        override suspend fun getDownloadOverWifiOnly(): Boolean = userPreferencesStore.isWifiOnlyDownloadEnabled()

        override suspend fun setDownloadOverWifiOnly(enabled: Boolean) {
            userPreferencesStore.setWifiOnlyDownloadEnabled(enabled)
        }
    }
