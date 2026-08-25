package dev.hossain.codematex.data

import dev.hossain.codematex.system.FakeDeviceMemoryProvider
import dev.hossain.codematex.system.SystemResourceStats
import dev.hossain.codematex.util.DeviceMemory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSystemStatsMonitorTest {
    private val fakeMemoryProvider = FakeDeviceMemoryProvider()
    private val monitor = DefaultSystemStatsMonitor(fakeMemoryProvider)

    @Test
    fun `monitorMetricsWhileActive immediately emits initial RAM stats`() =
        runTest {
            fakeMemoryProvider.returnedMemoryStats = DeviceMemory.MemoryStats(usedGb = 3.5f, totalGb = 8.0f)
            val emitted = mutableListOf<SystemResourceStats>()

            val job =
                launch {
                    monitor.monitorMetricsWhileActive(
                        isActive = { true },
                        onMetrics = { emitted.add(it) },
                    )
                }

            runCurrent()
            job.cancel()

            assertTrue(emitted.isNotEmpty())
            assertEquals(0f, emitted.first().cpuPercent, 0.01f)
            assertEquals(3.5f, emitted.first().ramUsedGb, 0.01f)
            assertEquals(8.0f, emitted.first().ramTotalGb, 0.01f)
            assertEquals(3.5f / 8.0f, emitted.first().ramFraction, 0.01f)
        }

    @Test
    fun `monitorWhileActive formats summary string properly`() =
        runTest {
            fakeMemoryProvider.returnedMemoryStats = DeviceMemory.MemoryStats(usedGb = 4.0f, totalGb = 8.0f)
            val emitted = mutableListOf<String>()

            val job =
                launch {
                    monitor.monitorWhileActive(
                        isActive = { true },
                        onStats = { emitted.add(it) },
                    )
                }

            runCurrent()
            job.cancel()

            assertTrue(emitted.isNotEmpty())
            assertEquals("CPU: 0% • RAM: 4.0 GB / 8.0 GB", emitted.first())
        }

    @Test
    fun `SystemResourceStats calculates fractions and handles bounds`() {
        val stats = SystemResourceStats(cpuPercent = 50f, ramUsedGb = 4f, ramTotalGb = 8f)
        assertEquals(0.5f, stats.cpuFraction, 0.01f)
        assertEquals(0.5f, stats.ramFraction, 0.01f)
        assertEquals("CPU: 50% • RAM: 4.0 GB / 8.0 GB", stats.formattedSummary)

        val zeroTotalStats = SystemResourceStats(cpuPercent = 120f, ramUsedGb = 5f, ramTotalGb = 0f)
        assertEquals(1.0f, zeroTotalStats.cpuFraction, 0.01f)
        assertEquals(0f, zeroTotalStats.ramFraction, 0.01f)
    }
}
