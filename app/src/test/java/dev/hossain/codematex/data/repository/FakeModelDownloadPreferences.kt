package dev.hossain.codematex.data.repository

/**
 * In-memory fake of [ModelDownloadPreferences] for unit tests.
 */
class FakeModelDownloadPreferences(
    override var downloadOverWifiOnly: Boolean = true,
) : ModelDownloadPreferences
