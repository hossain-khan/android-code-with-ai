package dev.hossain.codematex.data

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.system.FakeDeviceMemoryProvider
import dev.hossain.codematex.system.SystemResourceStats
import dev.hossain.codematex.util.DeviceMemory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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

            assertThat(emitted).isNotEmpty()
            assertThat(emitted.first().cpuPercent).isWithin(0.01f).of(0f)
            assertThat(emitted.first().ramUsedGb).isWithin(0.01f).of(3.5f)
            assertThat(emitted.first().ramTotalGb).isWithin(0.01f).of(8.0f)
            assertThat(emitted.first().ramFraction).isWithin(0.01f).of(3.5f / 8.0f)
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

            assertThat(emitted).isNotEmpty()
            assertThat(emitted.first()).isEqualTo("CPU: 0% • RAM: 4.0 GB / 8.0 GB")
        }

    @Test
    fun `monitorMetricsWhileActive periodically emits updated stats during active loop`() =
        runTest {
            fakeMemoryProvider.returnedMemoryStats = DeviceMemory.MemoryStats(usedGb = 3.5f, totalGb = 8.0f)
            fakeMemoryProvider.returnedProcessCpuTicks = 1000L
            val emitted = mutableListOf<SystemResourceStats>()

            var active = true
            val job =
                launch {
                    monitor.monitorMetricsWhileActive(
                        isActive = { active },
                        onMetrics = { emitted.add(it) },
                    )
                }

            runCurrent()
            assertThat(emitted).hasSize(1)

            // Allow at least 150ms wall-clock to pass so elapsedSec > 0.1f
            Thread.sleep(150)
            fakeMemoryProvider.returnedProcessCpuTicks = 1200L
            fakeMemoryProvider.returnedMemoryStats = DeviceMemory.MemoryStats(usedGb = 4.0f, totalGb = 8.0f)

            advanceTimeBy(800)
            runCurrent()

            active = false
            advanceTimeBy(800)
            runCurrent()
            job.cancel()

            assertThat(emitted.size).isAtLeast(2)
            val latest = emitted.last()
            assertThat(latest.ramUsedGb).isWithin(0.01f).of(4.0f)
            assertThat(latest.cpuPercent).isGreaterThan(0f)
        }

    @Test
    fun `SystemResourceStats calculates fractions and handles bounds`() {
        val stats = SystemResourceStats(cpuPercent = 50f, ramUsedGb = 4f, ramTotalGb = 8f)
        assertThat(stats.cpuFraction).isWithin(0.01f).of(0.5f)
        assertThat(stats.ramFraction).isWithin(0.01f).of(0.5f)
        assertThat(stats.formattedSummary).isEqualTo("CPU: 50% • RAM: 4.0 GB / 8.0 GB")

        val zeroTotalStats = SystemResourceStats(cpuPercent = 120f, ramUsedGb = 5f, ramTotalGb = 0f)
        assertThat(zeroTotalStats.cpuFraction).isWithin(0.01f).of(1.0f)
        assertThat(zeroTotalStats.ramFraction).isWithin(0.01f).of(0f)
    }
}
