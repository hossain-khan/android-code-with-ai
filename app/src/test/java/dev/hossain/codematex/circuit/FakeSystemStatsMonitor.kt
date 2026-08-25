package dev.hossain.codematex.circuit

import dev.hossain.codematex.system.SystemResourceStats

/**
 * In-memory fake of [SystemStatsMonitor] for unit tests.
 */
class FakeSystemStatsMonitor : SystemStatsMonitor {
    var statsToEmit: List<String> = emptyList()
    var resourceStatsToEmit: List<SystemResourceStats> = emptyList()
    var monitorCalls = 0

    override suspend fun monitorWhileActive(
        isActive: () -> Boolean,
        onStats: (String) -> Unit,
    ) {
        monitorCalls++
        statsToEmit.forEach { stat ->
            if (!isActive()) return
            onStats(stat)
        }
    }

    override suspend fun monitorMetricsWhileActive(
        isActive: () -> Boolean,
        onMetrics: (SystemResourceStats) -> Unit,
    ) {
        monitorCalls++
        if (resourceStatsToEmit.isNotEmpty()) {
            resourceStatsToEmit.forEach { stat ->
                if (!isActive()) return
                onMetrics(stat)
            }
        } else {
            statsToEmit.forEach { _ ->
                if (!isActive()) return
                onMetrics(SystemResourceStats(cpuPercent = 25f, ramUsedGb = 4f, ramTotalGb = 8f))
            }
        }
    }
}
