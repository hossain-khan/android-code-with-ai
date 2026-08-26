package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ModelSelectionStoreImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: ModelSelectionStore

    @Before
    fun setUp() {
        dataStore =
            PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { tempFolder.newFile("test_model_selection.preferences_pb") },
            )
        store = ModelSelectionStoreImpl(dataStore)
    }

    @Test
    fun `given uninitialized store - getSelectedModelId returns null`() =
        runTest(testDispatcher) {
            assertNull(store.getSelectedModelId())
            assertNull(store.selectedModelIdFlow.first())
        }

    @Test
    fun `given setSelectedModelId - write is awaited and immediately observable`() =
        runTest(testDispatcher) {
            store.setSelectedModelId("google/gemma-2-2b-it")

            assertEquals("google/gemma-2-2b-it", store.getSelectedModelId())
            assertEquals("google/gemma-2-2b-it", store.selectedModelIdFlow.first())
        }

    @Test
    fun `given null modelId - selection is removed`() =
        runTest(testDispatcher) {
            store.setSelectedModelId("google/gemma-2-2b-it")
            assertEquals("google/gemma-2-2b-it", store.getSelectedModelId())

            store.setSelectedModelId(null)
            assertNull(store.getSelectedModelId())
            assertNull(store.selectedModelIdFlow.first())
        }

    @Test
    fun `given sequential model selections - flow emits in correct order`() =
        runTest(testDispatcher) {
            val collected = mutableListOf<String?>()
            val job =
                launch {
                    store.selectedModelIdFlow.take(3).toList(collected)
                }

            store.setSelectedModelId("model-1")
            store.setSelectedModelId("model-2")

            job.join()

            assertEquals(listOf(null, "model-1", "model-2"), collected)
        }

    @Test(expected = java.io.IOException::class)
    fun `given failing datastore write - setSelectedModelId propagates exception to caller`() =
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
            val failingStore = ModelSelectionStoreImpl(failingDataStore)
            failingStore.setSelectedModelId("model-1")
        }
}
