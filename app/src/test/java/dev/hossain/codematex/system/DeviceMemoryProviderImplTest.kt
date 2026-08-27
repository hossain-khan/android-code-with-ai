package dev.hossain.codematex.system

import android.content.Context
import android.content.ContextWrapper
import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.util.DeviceMemory
import org.junit.Test

class DeviceMemoryProviderImplTest {
    @Test
    fun `getMemoryStats delegates to injected provider`() {
        val expectedStats = DeviceMemory.MemoryStats(usedGb = 3.5f, totalGb = 16.0f)
        var providerContext: Context? = null
        val fakeContext = ContextWrapper(null)
        val provider =
            DeviceMemoryProviderImpl(
                context = fakeContext,
                memoryStatsProvider = { context ->
                    providerContext = context
                    expectedStats
                },
                cpuTicksProvider = { 0L },
            )

        val stats = provider.getMemoryStats()

        assertThat(stats).isEqualTo(expectedStats)
        assertThat(providerContext).isSameInstanceAs(fakeContext)
    }

    @Test
    fun `getProcessCpuTicks delegates to injected provider`() {
        var called = false
        val provider =
            DeviceMemoryProviderImpl(
                context = ContextWrapper(null),
                memoryStatsProvider = { DeviceMemory.MemoryStats(0f, 0f) },
                cpuTicksProvider = {
                    called = true
                    42L
                },
            )

        val ticks = provider.getProcessCpuTicks()

        assertThat(ticks).isEqualTo(42L)
        assertThat(called).isTrue()
    }
}
