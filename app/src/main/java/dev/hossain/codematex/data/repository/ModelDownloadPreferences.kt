package dev.hossain.codematex.data.repository

import android.content.Context
import androidx.core.content.edit
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import javax.inject.Inject

/**
 * Persistent preferences for model downloads.
 *
 * Tests can supply an in-memory fake to avoid depending on Android
 * [android.content.SharedPreferences].
 */
interface ModelDownloadPreferences {
    /**
     * Whether model downloads are restricted to unmetered Wi-Fi networks only.
     * Defaults to `true` to protect users against unexpected cellular data consumption
     * on multi-gigabyte models.
     */
    var downloadOverWifiOnly: Boolean
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelDownloadPreferencesImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ModelDownloadPreferences {
        private val prefs = context.getSharedPreferences("download_prefs", Context.MODE_PRIVATE)

        override var downloadOverWifiOnly: Boolean
            get() = prefs.getBoolean(KEY_WIFI_ONLY, true)
            set(value) {
                prefs.edit { putBoolean(KEY_WIFI_ONLY, value) }
            }

        companion object {
            private const val KEY_WIFI_ONLY = "download_over_wifi_only"
        }
    }
