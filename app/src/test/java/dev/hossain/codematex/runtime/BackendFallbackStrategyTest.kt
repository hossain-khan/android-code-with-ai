package dev.hossain.codematex.runtime

import com.google.common.truth.Truth.assertThat
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
        assertThat(strategy.resolveStartBackend(LlmEngine.Backend.GPU)).isEqualTo(LlmEngine.Backend.GPU)
        assertThat(strategy.resolveStartBackend(LlmEngine.Backend.NPU)).isEqualTo(LlmEngine.Backend.NPU)
        assertThat(strategy.resolveStartBackend(LlmEngine.Backend.CPU)).isEqualTo(LlmEngine.Backend.CPU)
    }

    @Test
    fun `resolveStartBackend falls back through chain when preferred is unsupported`() {
        strategy.markUnsupported(LlmEngine.Backend.NPU)

        assertThat(strategy.resolveStartBackend(LlmEngine.Backend.NPU)).isEqualTo(LlmEngine.Backend.GPU)
    }

    @Test
    fun `resolveStartBackend skips multiple unsupported backends`() {
        strategy.markUnsupported(LlmEngine.Backend.NPU)
        strategy.markUnsupported(LlmEngine.Backend.GPU)

        assertThat(strategy.resolveStartBackend(LlmEngine.Backend.NPU)).isEqualTo(LlmEngine.Backend.CPU)
    }

    @Test
    fun `resolveStartBackend falls back to CPU when preferred GPU is unsupported`() {
        strategy.markUnsupported(LlmEngine.Backend.GPU)

        assertThat(strategy.resolveStartBackend(LlmEngine.Backend.GPU)).isEqualTo(LlmEngine.Backend.CPU)
    }

    @Test
    fun `nextBackend follows NPU to GPU to CPU`() {
        assertThat(strategy.nextBackend(LlmEngine.Backend.NPU)).isEqualTo(LlmEngine.Backend.GPU)
        assertThat(strategy.nextBackend(LlmEngine.Backend.GPU)).isEqualTo(LlmEngine.Backend.CPU)
        assertThat(strategy.nextBackend(LlmEngine.Backend.CPU)).isNull()
    }

    @Test
    fun `markUnsupported records failed backend`() {
        assertThat(strategy.isUnsupported(LlmEngine.Backend.GPU)).isFalse()

        strategy.markUnsupported(LlmEngine.Backend.GPU)

        assertThat(strategy.isUnsupported(LlmEngine.Backend.GPU)).isTrue()
        assertThat(strategy.isUnsupported(LlmEngine.Backend.NPU)).isFalse()
        assertThat(strategy.isUnsupported(LlmEngine.Backend.CPU)).isFalse()
    }

    @Test
    fun `CPU is never marked as unsupported across instances`() {
        strategy.markUnsupported(LlmEngine.Backend.CPU)

        assertThat(strategy.isUnsupported(LlmEngine.Backend.CPU)).isTrue()
        assertThat(strategy.nextBackend(LlmEngine.Backend.CPU)).isNull()
    }
}
