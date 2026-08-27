package dev.hossain.codematex.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.ModelConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

/**
 * Unit tests for [ModelConfigStore].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ModelConfigStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: ModelConfigStore

    @Before
    fun setUp() {
        dataStore =
            PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { tempFolder.newFile("test_model_config.preferences_pb") },
            )
        store = ModelConfigStoreImpl(dataStore)
    }

    @Test
    fun `given default state - config returns default model config`() {
        assertThat(store.config).isEqualTo(ModelConfig())
        assertThat(store.configFlow.value).isEqualTo(ModelConfig())
    }

    @Test
    fun `given new config - update config updates current config and emits to flow`() =
        runTest(testDispatcher) {
            val newConfig = ModelConfig(temperature = 0.2f, topK = 10, topP = 0.9f, maxTokens = 512)

            store.updateConfig(newConfig)

            assertThat(store.config).isEqualTo(newConfig)
            assertThat(store.configFlow.first()).isEqualTo(newConfig)
        }

    @Test
    fun `given unconfigured model - getConfig returns default model config`() =
        runTest(testDispatcher) {
            assertThat(store.getConfig("gemma-2b")).isEqualTo(ModelConfig())
            assertThat(store.getConfigFlow("gemma-2b").first()).isEqualTo(ModelConfig())
        }

    @Test
    fun `given setConfig for model - custom parameters are persisted and retrieved`() =
        runTest(testDispatcher) {
            val customConfig = ModelConfig(temperature = 1.2f, topK = 60, topP = 0.85f, maxTokens = 4096)

            store.setConfig("gemma-2b", customConfig)

            assertThat(store.getConfig("gemma-2b")).isEqualTo(customConfig)
            assertThat(store.getConfigFlow("gemma-2b").first()).isEqualTo(customConfig)
        }

    @Test
    fun `given multiple models - configs are stored independently`() =
        runTest(testDispatcher) {
            val gemmaConfig = ModelConfig(temperature = 0.5f, topK = 20, topP = 0.9f, maxTokens = 2048)
            val qwenConfig = ModelConfig(temperature = 1.0f, topK = 80, topP = 0.95f, maxTokens = 8192)

            store.setConfig("gemma-2b", gemmaConfig)
            store.setConfig("qwen-3-0.6b", qwenConfig)

            assertThat(store.getConfig("gemma-2b")).isEqualTo(gemmaConfig)
            assertThat(store.getConfig("qwen-3-0.6b")).isEqualTo(qwenConfig)
            assertThat(store.getConfig("phi-4-mini")).isEqualTo(ModelConfig())
        }

    @Test
    fun `given resetConfig for model - config returns to default`() =
        runTest(testDispatcher) {
            val customConfig = ModelConfig(temperature = 1.5f, topK = 90, topP = 0.7f, maxTokens = 1024)
            store.setConfig("gemma-2b", customConfig)
            assertThat(store.getConfig("gemma-2b")).isEqualTo(customConfig)

            store.resetConfig("gemma-2b")

            assertThat(store.getConfig("gemma-2b")).isEqualTo(ModelConfig())
            assertThat(store.getConfigFlow("gemma-2b").first()).isEqualTo(ModelConfig())
        }

    @Test
    fun `given datastore throws IOException on read - recovers gracefully with default config`() =
        runTest(testDispatcher) {
            val failingDataStore =
                object : DataStore<Preferences> {
                    override val data: kotlinx.coroutines.flow.Flow<Preferences> =
                        kotlinx.coroutines.flow.flow { throw IOException("Disk read error") }

                    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                        throw IOException("Disk write error")
                }
            val failingStore = ModelConfigStoreImpl(failingDataStore)

            assertThat(failingStore.getConfig("gemma-2b")).isEqualTo(ModelConfig())
        }
}
