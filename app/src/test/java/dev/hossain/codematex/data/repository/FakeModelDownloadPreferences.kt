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
    private val _downloadOverWifiOnlyFlow = MutableStateFlow(downloadOverWifiOnly)

    override val downloadOverWifiOnlyFlow: Flow<Boolean> = _downloadOverWifiOnlyFlow.asStateFlow()

    var shouldThrowOnWrite: Boolean = false

    override suspend fun getDownloadOverWifiOnly(): Boolean = _downloadOverWifiOnlyFlow.value

    override suspend fun setDownloadOverWifiOnly(enabled: Boolean) {
        if (shouldThrowOnWrite) {
            throw java.io.IOException("Fake disk write failure")
        }
        _downloadOverWifiOnlyFlow.value = enabled
    }
}
