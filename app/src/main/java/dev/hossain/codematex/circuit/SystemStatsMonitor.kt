package dev.hossain.codematex.circuit

import dev.hossain.codematex.system.DeviceMemoryProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Abstraction over CPU/RAM polling used to display live system stats during
 * LLM inference.
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
            var prevTicks = deviceMemoryProvider.getProcessCpuTicks()
            var prevTime = System.currentTimeMillis()
            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

            while (isActive()) {
                delay(1000)
                val now = System.currentTimeMillis()
                val elapsedSec = (now - prevTime) / 1000f
                val currentTicks = deviceMemoryProvider.getProcessCpuTicks()

                if (elapsedSec > 0.1f) {
                    val ticksDiff = currentTicks - prevTicks
                    val cpuUsage = ((ticksDiff / 100f) / elapsedSec) * 100f
                    val scaledCpu = (cpuUsage / cores).coerceIn(0f, 100f)

                    val mem = deviceMemoryProvider.getMemoryStats()
                    onStats(
                        "CPU: ${"%.0f".format(scaledCpu)}% • RAM: ${"%.1f".format(mem.usedGb)} GB / ${"%.1f".format(mem.totalGb)} GB",
                    )

                    prevTicks = currentTicks
                    prevTime = now
                }
            }
        }
    }
