package dev.hossain.codematex.circuit.overlay

import android.content.ContextWrapper
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelConfigStoreTest {
    private val store = ModelConfigStore(ContextWrapper(null))

    @Test
    fun `given default state - config returns default model config`() {
        assertEquals(ModelConfig(), store.config)
    }

    @Test
    fun `given new config - update config updates current config`() {
        val newConfig = ModelConfig(temperature = 0.2f, topK = 10, topP = 0.9f, maxTokens = 512)

        store.updateConfig(newConfig)

        assertEquals(newConfig, store.config)
    }
}
