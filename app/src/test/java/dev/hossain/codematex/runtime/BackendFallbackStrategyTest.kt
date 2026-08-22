package dev.hossain.codematex.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackendFallbackStrategyTest {
    private lateinit var strategy: DefaultBackendFallbackStrategy

    @Before
    fun setUp() {
        strategy = DefaultBackendFallbackStrategy()
    }

    @Test
    fun `resolveStartBackend returns preferred backend when supported`() {
        assertEquals(LlmEngine.Backend.GPU, strategy.resolveStartBackend(LlmEngine.Backend.GPU))
        assertEquals(LlmEngine.Backend.NPU, strategy.resolveStartBackend(LlmEngine.Backend.NPU))
        assertEquals(LlmEngine.Backend.CPU, strategy.resolveStartBackend(LlmEngine.Backend.CPU))
    }

    @Test
    fun `resolveStartBackend falls back to CPU when preferred is unsupported`() {
        strategy.markUnsupported(LlmEngine.Backend.GPU)

        assertEquals(LlmEngine.Backend.CPU, strategy.resolveStartBackend(LlmEngine.Backend.GPU))
    }

    @Test
    fun `nextBackend follows NPU to GPU to CPU`() {
        assertEquals(LlmEngine.Backend.GPU, strategy.nextBackend(LlmEngine.Backend.NPU))
        assertEquals(LlmEngine.Backend.CPU, strategy.nextBackend(LlmEngine.Backend.GPU))
        assertNull(strategy.nextBackend(LlmEngine.Backend.CPU))
    }

    @Test
    fun `markUnsupported records failed backend`() {
        assertFalse(strategy.isUnsupported(LlmEngine.Backend.GPU))

        strategy.markUnsupported(LlmEngine.Backend.GPU)

        assertTrue(strategy.isUnsupported(LlmEngine.Backend.GPU))
        assertFalse(strategy.isUnsupported(LlmEngine.Backend.NPU))
        assertFalse(strategy.isUnsupported(LlmEngine.Backend.CPU))
    }

    @Test
    fun `CPU is never marked as unsupported across instances`() {
        strategy.markUnsupported(LlmEngine.Backend.CPU)

        assertTrue(strategy.isUnsupported(LlmEngine.Backend.CPU))
        assertNull(strategy.nextBackend(LlmEngine.Backend.CPU))
    }
}
