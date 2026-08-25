package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
import javax.inject.Inject

/**
 * Persistent preferences for model downloads.
 *
 * Tests can supply an in-memory fake to avoid depending on Android DataStore.
 */
interface ModelDownloadPreferences {
    /**
     * Whether model downloads are restricted to unmetered Wi-Fi networks only.
     * Defaults to `true` to protect users against unexpected cellular data consumption
     * on multi-gigabyte models.
     */
    var downloadOverWifiOnly: Boolean

    /**
     * Observable flow of the Wi-Fi only download preference.
     */
    val downloadOverWifiOnlyFlow: Flow<Boolean>
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelDownloadPreferencesImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ModelDownloadPreferences {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val wifiOnlyState: StateFlow<Boolean> =
            dataStore.data
                .map { prefs -> prefs[KEY_WIFI_ONLY] ?: true }
                .stateIn(
                    scope = scope,
                    started = SharingStarted.Eagerly,
                    initialValue = true,
                )

        override val downloadOverWifiOnlyFlow: Flow<Boolean>
            get() = wifiOnlyState

        override var downloadOverWifiOnly: Boolean
            get() = wifiOnlyState.value
            set(value) {
                scope.launch {
                    dataStore.edit { prefs ->
                        prefs[KEY_WIFI_ONLY] = value
                    }
                }
            }

        companion object {
            private val KEY_WIFI_ONLY = booleanPreferencesKey("download_over_wifi_only")
        }
    }
