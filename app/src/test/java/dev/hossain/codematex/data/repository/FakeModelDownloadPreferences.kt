package dev.hossain.codematex.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory fake of [ModelDownloadPreferences] for unit tests.
 */
class FakeModelDownloadPreferences(
    downloadOverWifiOnly: Boolean = true,
) : ModelDownloadPreferences {
    override val downloadOverWifiOnlyFlow: Flow<Boolean>
        field = MutableStateFlow(downloadOverWifiOnly)

    override var downloadOverWifiOnly: Boolean
        get() = downloadOverWifiOnlyFlow.value
        set(value) {
            downloadOverWifiOnlyFlow.value = value
        }
}
