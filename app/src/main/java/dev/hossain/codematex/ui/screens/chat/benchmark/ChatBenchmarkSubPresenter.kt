package dev.hossain.codematex.ui.screens.chat.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.data.SystemStatsMonitor
import dev.hossain.codematex.system.SystemResourceStats
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * SubPresenter managing live system telemetry monitoring for the chat screen sticky benchmark panel.
 */
class ChatBenchmarkSubPresenter(
    private val screen: ChatBenchmarkSubScreen,
    private val systemStatsMonitor: SystemStatsMonitor,
) : SubPresenter<ChatBenchmarkOuterEvent, ChatBenchmarkSubState> {
    @Composable
    override fun present(outerEventSink: (ChatBenchmarkOuterEvent) -> Unit): ChatBenchmarkSubState {
        var systemResourceStats by remember { mutableStateOf<SystemResourceStats?>(null) }
        var systemStatsInfo by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(screen.isGenerating, screen.isPreparing) {
            if (screen.isGenerating || screen.isPreparing) {
                systemStatsMonitor.monitorMetricsWhileActive(
                    isActive = { screen.isGenerating || screen.isPreparing },
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
}

/**
 * SubPresenter factory contributing [ChatBenchmarkSubPresenter] into Metro DI [AppScope].
 */
@ContributesIntoSet(AppScope::class)
@Inject
class ChatBenchmarkSubPresenterFactory(
    private val systemStatsMonitor: SystemStatsMonitor,
) : SubPresenterFactory {
    override fun create(screen: SubScreen<*>): SubPresenter<*, *>? =
        when (screen) {
            is ChatBenchmarkSubScreen -> ChatBenchmarkSubPresenter(screen, systemStatsMonitor)
            else -> null
        }
}
