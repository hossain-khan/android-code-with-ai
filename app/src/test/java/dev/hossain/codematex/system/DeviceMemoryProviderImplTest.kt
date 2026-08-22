package dev.hossain.codematex.system

import android.content.Context
import android.content.ContextWrapper
import dev.hossain.codematex.util.DeviceMemory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        assertEquals(expectedStats, stats)
        assertEquals(fakeContext, providerContext)
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

        assertEquals(42L, ticks)
        assertTrue(called)
    }
}
