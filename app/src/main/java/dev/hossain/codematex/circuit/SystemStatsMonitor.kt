package dev.hossain.codematex.circuit

import dev.hossain.codematex.system.DeviceMemoryProvider
import dev.hossain.codematex.system.SystemResourceStats
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Abstraction over CPU/RAM polling used to display live system stats during
 * LLM model preparation and inference.
 */
interface SystemStatsMonitor {
    /**
     * Polls CPU and memory stats while [isActive] returns true and emits
     * formatted strings via [onStats].
     */
    suspend fun monitorWhileActive(
        isActive: () -> Boolean,
        onStats: (String) -> Unit,
    )

    /**
     * Polls CPU and memory stats while [isActive] returns true and emits
     * structured [SystemResourceStats] via [onMetrics].
     */
    suspend fun monitorMetricsWhileActive(
        isActive: () -> Boolean,
        onMetrics: (SystemResourceStats) -> Unit,
    )
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultSystemStatsMonitor
    @Inject
    constructor(
        private val deviceMemoryProvider: DeviceMemoryProvider,
    ) : SystemStatsMonitor {
        override suspend fun monitorWhileActive(
            isActive: () -> Boolean,
            onStats: (String) -> Unit,
        ) {
            monitorMetricsWhileActive(isActive) { metrics ->
                onStats(metrics.formattedSummary)
            }
        }

        override suspend fun monitorMetricsWhileActive(
            isActive: () -> Boolean,
            onMetrics: (SystemResourceStats) -> Unit,
        ) {
            var prevTicks = deviceMemoryProvider.getProcessCpuTicks()
            var prevTime = System.currentTimeMillis()
            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

            // Emit initial memory reading immediately without waiting for the first delay cycle
            val initialMem = deviceMemoryProvider.getMemoryStats()
            onMetrics(
                SystemResourceStats(
                    cpuPercent = 0f,
                    ramUsedGb = initialMem.usedGb,
                    ramTotalGb = initialMem.totalGb,
                ),
            )

            while (isActive()) {
                delay(750)
                val now = System.currentTimeMillis()
                val elapsedSec = (now - prevTime) / 1000f
                val currentTicks = deviceMemoryProvider.getProcessCpuTicks()

                if (elapsedSec > 0.1f) {
                    val ticksDiff = (currentTicks - prevTicks).coerceAtLeast(0L)
                    val cpuUsage = ((ticksDiff / 100f) / elapsedSec) * 100f
                    val scaledCpu = (cpuUsage / cores).coerceIn(0f, 100f)

                    val mem = deviceMemoryProvider.getMemoryStats()
                    onMetrics(
                        SystemResourceStats(
                            cpuPercent = scaledCpu,
                            ramUsedGb = mem.usedGb,
                            ramTotalGb = mem.totalGb,
                        ),
                    )

                    prevTicks = currentTicks
                    prevTime = now
                }
            }
        }
    }
