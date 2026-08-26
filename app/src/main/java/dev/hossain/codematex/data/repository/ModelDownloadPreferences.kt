package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
 * Persistent preferences for model downloads.
 *
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
        private val dataStore: DataStore<Preferences>,
    ) : ModelDownloadPreferences {
        override val downloadOverWifiOnlyFlow: Flow<Boolean> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Timber.e(exception, "ModelDownloadPreferencesImpl: Error reading preferences, emitting empty preferences")
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { prefs -> prefs[KEY_WIFI_ONLY] ?: true }
                .distinctUntilChanged()

        override suspend fun getDownloadOverWifiOnly(): Boolean = downloadOverWifiOnlyFlow.first()

        override suspend fun setDownloadOverWifiOnly(enabled: Boolean) {
            dataStore.edit { prefs ->
                prefs[KEY_WIFI_ONLY] = enabled
            }
        }

        companion object {
            private val KEY_WIFI_ONLY = booleanPreferencesKey("download_over_wifi_only")
        }
    }
