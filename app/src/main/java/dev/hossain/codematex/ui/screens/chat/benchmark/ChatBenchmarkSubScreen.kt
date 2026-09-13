package dev.hossain.codematex.ui.screens.chat.benchmark

import androidx.compose.runtime.Immutable
import com.slack.circuit.subcircuit.SubCircuitOuterEvent
import com.slack.circuit.subcircuit.SubCircuitUiState
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.system.ContextUsageStats
import dev.hossain.codematex.system.SystemResourceStats

/**
 * Outer events emitted by [ChatBenchmarkSubPresenter] to parent presentation layers.
 */
sealed interface ChatBenchmarkOuterEvent : SubCircuitOuterEvent

/**
 * SubScreen identifying the technical benchmarking and live hardware telemetry section of ChatScreen.
 *
 * @param isExpanded Whether the layout is an expanded tablet/desktop sidebar card or a compact collapsible panel.
 * @param modelName Human-readable name of the active model.
 * @param activeBackend Hardware acceleration backend name (e.g., "GPU", "CPU", "NPU").
 * @param modelSize Formatted disk size of the model.
 * @param modelMemory Minimum device memory requirement string.
 * @param configInfo Formatted sampler configuration (temperature, top-k, top-p).
 * @param throughputInfo Formatted real-time token throughput (TTFT and decode speed).
 * @param contextStats Context window utilization statistics.
 * @param isGenerating Whether the LLM is actively streaming response tokens.
 * @param isPreparing Whether the model is initializing or restoring conversation history.
 */
data class ChatBenchmarkSubScreen(
    val isExpanded: Boolean = false,
    val modelName: String,
    val activeBackend: String? = null,
    val modelSize: String? = null,
    val modelMemory: String? = null,
    val configInfo: String? = null,
    val throughputInfo: String? = null,
    val contextStats: ContextUsageStats? = null,
    val isGenerating: Boolean = false,
    val isPreparing: Boolean = false,
) : SubScreen<ChatBenchmarkOuterEvent>

/**
 * UI State for [ChatBenchmarkSubScreen].
 */
@Immutable
data class ChatBenchmarkSubState(
    val isExpanded: Boolean = false,
    val modelName: String,
    val activeBackend: String? = null,
    val modelSize: String? = null,
    val modelMemory: String? = null,
    val configInfo: String? = null,
    val throughputInfo: String? = null,
    val systemResourceStats: SystemResourceStats? = null,
    val systemStatsInfo: String? = null,
    val contextStats: ContextUsageStats? = null,
    val isGenerating: Boolean = false,
    val isPreparing: Boolean = false,
) : SubCircuitUiState
