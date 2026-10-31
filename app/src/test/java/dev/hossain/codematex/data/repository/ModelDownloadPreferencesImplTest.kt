package dev.hossain.codematex.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ModelDownloadPreferencesImplTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var userPreferencesStore: FakeUserPreferencesStore
    private lateinit var preferences: ModelDownloadPreferences

    @Before
    fun setUp() {
        userPreferencesStore = FakeUserPreferencesStore(initialWifiOnlyDownload = true)
        preferences = ModelDownloadPreferencesImpl(userPreferencesStore)
    }

    @Test
    fun `given uninitialized preferences - downloadOverWifiOnly defaults to true`() =
        runTest(testDispatcher) {
            assertThat(preferences.getDownloadOverWifiOnly()).isTrue()
            assertThat(preferences.downloadOverWifiOnlyFlow.first()).isTrue()
        }

    @Test
    fun `given setDownloadOverWifiOnly - write is awaited and immediately observable`() =
        runTest(testDispatcher) {
            preferences.setDownloadOverWifiOnly(false)

            assertThat(preferences.getDownloadOverWifiOnly()).isFalse()
            assertThat(preferences.downloadOverWifiOnlyFlow.first()).isFalse()
            assertThat(userPreferencesStore.isWifiOnlyDownloadEnabled()).isFalse()

            preferences.setDownloadOverWifiOnly(true)
            assertThat(preferences.getDownloadOverWifiOnly()).isTrue()
            assertThat(preferences.downloadOverWifiOnlyFlow.first()).isTrue()
            assertThat(userPreferencesStore.isWifiOnlyDownloadEnabled()).isTrue()
        }

    @Test
    fun `given multiple preference toggles - flow emits distinct states in order`() =
        runTest(testDispatcher) {
            val collected = mutableListOf<Boolean>()
            val job =
                launch {
                    preferences.downloadOverWifiOnlyFlow.take(3).toList(collected)
                }

            preferences.setDownloadOverWifiOnly(false)
            preferences.setDownloadOverWifiOnly(true)

            job.join()

            assertThat(collected).containsExactly(true, false, true).inOrder()
        }

    @Test(expected = IOException::class)
    fun `given failing store write - setDownloadOverWifiOnly propagates exception to caller`() =
        runTest(testDispatcher) {
            userPreferencesStore.shouldThrowOnWrite = true
            preferences.setDownloadOverWifiOnly(false)
        }
}
