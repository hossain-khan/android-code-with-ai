package dev.hossain.codematex.ui.screens.chat.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.subcircuit.SubCircuitInject
import com.slack.circuit.subcircuit.SubPresenter
import dev.hossain.codematex.data.SystemStatsMonitor
import dev.hossain.codematex.system.SystemResourceStats
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

/**
 * SubPresenter managing live system telemetry monitoring for the chat screen sticky benchmark panel.
 */
@AssistedInject
class ChatBenchmarkSubPresenter(
    @Assisted private val screen: ChatBenchmarkSubScreen,
    private val systemStatsMonitor: SystemStatsMonitor,
) : SubPresenter<ChatBenchmarkOuterEvent, ChatBenchmarkSubState> {
    @Composable
    override fun present(outerEventSink: (ChatBenchmarkOuterEvent) -> Unit): ChatBenchmarkSubState {
        var systemResourceStats by remember { mutableStateOf<SystemResourceStats?>(null) }
        var systemStatsInfo by remember { mutableStateOf<String?>(null) }
        val isTelemetryActive = screen.isGenerating || screen.isPreparing

        LaunchedEffect(isTelemetryActive) {
            if (isTelemetryActive) {
                systemStatsMonitor.monitorMetricsWhileActive(
                    isActive = { isTelemetryActive },
                    onMetrics = { stats ->
                        systemResourceStats = stats
                        systemStatsInfo = stats.formattedSummary
                    },
                )
            } else {
                systemResourceStats = null
                systemStatsInfo = null
            }
        }

        return ChatBenchmarkSubState(
            isExpanded = screen.isExpanded,
            modelName = screen.modelName,
            activeBackend = screen.activeBackend,
            modelSize = screen.modelSize,
            modelMemory = screen.modelMemory,
            configInfo = screen.configInfo,
            throughputInfo = screen.throughputInfo,
            systemResourceStats = systemResourceStats,
            systemStatsInfo = systemStatsInfo,
            contextStats = screen.contextStats,
            isGenerating = screen.isGenerating,
            isPreparing = screen.isPreparing,
        )
    }

    @SubCircuitInject(ChatBenchmarkSubScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(screen: ChatBenchmarkSubScreen): ChatBenchmarkSubPresenter
    }
}
