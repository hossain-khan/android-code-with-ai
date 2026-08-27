package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ModelDownloadPreferencesImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferences: ModelDownloadPreferences

    @Before
    fun setUp() {
        dataStore =
            PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { tempFolder.newFile("test_download_prefs.preferences_pb") },
            )
        preferences = ModelDownloadPreferencesImpl(dataStore)
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

            preferences.setDownloadOverWifiOnly(true)
            assertThat(preferences.getDownloadOverWifiOnly()).isTrue()
            assertThat(preferences.downloadOverWifiOnlyFlow.first()).isTrue()
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

    @Test(expected = java.io.IOException::class)
    fun `given failing datastore write - setDownloadOverWifiOnly propagates exception to caller`() =
        runTest(testDispatcher) {
            val failingDataStore =
                object : DataStore<Preferences> {
                    override val data: kotlinx.coroutines.flow.Flow<Preferences> =
                        kotlinx.coroutines.flow.flowOf(
                            androidx.datastore.preferences.core
                                .emptyPreferences(),
                        )

                    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                        throw java.io.IOException("Disk write failure")
                }
            val failingPreferences = ModelDownloadPreferencesImpl(failingDataStore)
            failingPreferences.setDownloadOverWifiOnly(false)
        }
}
