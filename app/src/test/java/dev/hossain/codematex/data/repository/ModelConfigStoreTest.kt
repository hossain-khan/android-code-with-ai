package dev.hossain.codematex.data.repository

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.ModelConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [ModelConfigStore].
 */
class ModelConfigStoreTest {
    private val store: ModelConfigStore = ModelConfigStoreImpl()

    @Test
    fun `given default state - config returns default model config`() {
        assertThat(store.config).isEqualTo(ModelConfig())
        assertThat(store.configFlow.value).isEqualTo(ModelConfig())
    }

    @Test
    fun `given new config - update config updates current config and emits to flow`() =
        runTest {
            val newConfig = ModelConfig(temperature = 0.2f, topK = 10, topP = 0.9f, maxTokens = 512)

            store.updateConfig(newConfig)

            assertThat(store.config).isEqualTo(newConfig)
            assertThat(store.configFlow.first()).isEqualTo(newConfig)
        }
}
