package dev.hossain.codematex.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/**
 * In-memory fake of [ModelDownloadPreferences] for unit tests.
 */
class FakeModelDownloadPreferences(
    downloadOverWifiOnly: Boolean = true,
) : ModelDownloadPreferences {
    override val downloadOverWifiOnlyFlow: Flow<Boolean>
        field = MutableStateFlow(downloadOverWifiOnly)

    var shouldThrowOnWrite: Boolean = false

    override suspend fun getDownloadOverWifiOnly(): Boolean = downloadOverWifiOnlyFlow.value

    override suspend fun setDownloadOverWifiOnly(enabled: Boolean) {
        if (shouldThrowOnWrite) {
            throw IOException("Fake disk write failure")
        }
        downloadOverWifiOnlyFlow.value = enabled
    }
}
