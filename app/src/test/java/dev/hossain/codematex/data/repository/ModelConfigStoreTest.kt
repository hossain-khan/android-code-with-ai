package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.ModelConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [ModelConfigStore].
 */
class ModelConfigStoreTest {
    private val store: ModelConfigStore = ModelConfigStoreImpl()

    @Test
    fun `given default state - config returns default model config`() {
        assertEquals(ModelConfig(), store.config)
        assertEquals(ModelConfig(), store.configFlow.value)
    }

    @Test
    fun `given new config - update config updates current config and emits to flow`() =
        runTest {
            val newConfig = ModelConfig(temperature = 0.2f, topK = 10, topP = 0.9f, maxTokens = 512)

            store.updateConfig(newConfig)

            assertEquals(newConfig, store.config)
            assertEquals(newConfig, store.configFlow.first())
        }
}
