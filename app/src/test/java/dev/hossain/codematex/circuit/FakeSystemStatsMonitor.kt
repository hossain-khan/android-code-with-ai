package dev.hossain.codematex.circuit

/**
 * In-memory fake of [SystemStatsMonitor] for unit tests.
 */
class FakeSystemStatsMonitor : SystemStatsMonitor {
    var statsToEmit: List<String> = emptyList()
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
}
