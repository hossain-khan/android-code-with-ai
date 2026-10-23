package dev.hossain.codematex.ui.screens.chat.benchmark

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.subPresenterTestOf
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.FakeSystemStatsMonitor
import dev.hossain.codematex.data.SystemStatsMonitor
import dev.hossain.codematex.system.ContextUsageStats
import dev.hossain.codematex.system.SystemResourceStats
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChatBenchmarkSubPresenterTest {
    private val fakeSystemStatsMonitor = FakeSystemStatsMonitor()
    private val factory = ChatBenchmarkSubPresenterFactory(fakeSystemStatsMonitor)

    private val testScreen =
        ChatBenchmarkSubScreen(
            isExpanded = false,
            modelName = "gemma-2b-it",
            activeBackend = "GPU",
            modelSize = "2,588 MB",
            modelMemory = "Requires 4GB RAM",
            configInfo = "Temp: 0.7 • Top-K: 40 • Top-P: 1.0",
            throughputInfo = "TTFT: 500ms • Speed: 15.0 t/s",
            contextStats = ContextUsageStats(usedTokens = 120, maxTokens = 8192),
            isGenerating = false,
            isPreparing = false,
        )

    private fun createPresenter(screen: ChatBenchmarkSubScreen = testScreen): ChatBenchmarkSubPresenter =
        ChatBenchmarkSubPresenter(
            screen = screen,
            systemStatsMonitor = fakeSystemStatsMonitor,
        )

    @Test
    fun `contentKey returns distinct stable keys for compact and expanded modes`() {
        val compactKey = ChatBenchmarkSubScreen.contentKey(isExpanded = false)
        val expandedKey = ChatBenchmarkSubScreen.contentKey(isExpanded = true)

        assertThat(compactKey).isEqualTo("chat_benchmark_compact")
        assertThat(expandedKey).isEqualTo("chat_benchmark_expanded")
        assertThat(compactKey).isNotEqualTo(expandedKey)
    }

    @Test
    fun `initial state reflects screen properties and system stats are null when idle`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.isExpanded).isFalse()
                assertThat(state.modelName).isEqualTo("gemma-2b-it")
                assertThat(state.activeBackend).isEqualTo("GPU")
                assertThat(state.modelSize).isEqualTo("2,588 MB")
                assertThat(state.modelMemory).isEqualTo("Requires 4GB RAM")
                assertThat(state.configInfo).isEqualTo("Temp: 0.7 • Top-K: 40 • Top-P: 1.0")
                assertThat(state.throughputInfo).isEqualTo("TTFT: 500ms • Speed: 15.0 t/s")
                assertThat(state.contextStats).isEqualTo(ContextUsageStats(120, 8192))
                assertThat(state.systemResourceStats).isNull()
                assertThat(state.systemStatsInfo).isNull()
                assertThat(fakeSystemStatsMonitor.monitorCalls).isEqualTo(0)
            }
        }

    @Test
    fun `when isGenerating is true, system stats are monitored and emitted`() =
        runTest {
            val expectedStats = SystemResourceStats(cpuPercent = 45f, ramUsedGb = 3.5f, ramTotalGb = 8.0f)
            fakeSystemStatsMonitor.resourceStatsToEmit = listOf(expectedStats)
            val presenter = createPresenter(testScreen.copy(isGenerating = true))

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.isGenerating).isTrue()
                assertThat(state.systemResourceStats).isEqualTo(expectedStats)
                assertThat(state.systemStatsInfo).isEqualTo(expectedStats.formattedSummary)
                assertThat(fakeSystemStatsMonitor.monitorCalls).isGreaterThan(0)
            }
        }

    @Test
    fun `when isPreparing is true, system stats are monitored and emitted`() =
        runTest {
            val expectedStats = SystemResourceStats(cpuPercent = 60f, ramUsedGb = 4.0f, ramTotalGb = 8.0f)
            fakeSystemStatsMonitor.resourceStatsToEmit = listOf(expectedStats)
            val presenter = createPresenter(testScreen.copy(isPreparing = true))

            presenter.test {
                val state = expectMostRecentItem()
                assertThat(state.isPreparing).isTrue()
                assertThat(state.systemResourceStats).isEqualTo(expectedStats)
                assertThat(state.systemStatsInfo).isEqualTo(expectedStats.formattedSummary)
                assertThat(fakeSystemStatsMonitor.monitorCalls).isGreaterThan(0)
            }
        }

    @Test
    fun `streaming token updates with stable contentKey retain systemResourceStats and UI expansion state without restarting monitor`() =
        runTest {
            val initialStats = SystemResourceStats(cpuPercent = 42f, ramUsedGb = 3.8f, ramTotalGb = 8.0f)
            var monitorStartCount = 0
            val suspendingMonitor =
                object : SystemStatsMonitor {
                    override suspend fun monitorWhileActive(
                        isActive: () -> Boolean,
                        onStats: (String) -> Unit,
                    ) {
                        monitorMetricsWhileActive(isActive) { onStats(it.formattedSummary) }
                    }

                    override suspend fun monitorMetricsWhileActive(
                        isActive: () -> Boolean,
                        onMetrics: (SystemResourceStats) -> Unit,
                    ) {
                        monitorStartCount++
                        onMetrics(initialStats)
                        awaitCancellation()
                    }
                }
            val subPresenterFactory = ChatBenchmarkSubPresenterFactory(suspendingMonitor)

            var currentScreen by mutableStateOf(
                testScreen.copy(
                    isGenerating = true,
                    throughputInfo = "Prefilling...",
                    contextStats = ContextUsageStats(usedTokens = 100, maxTokens = 8192),
                ),
            )
            var onTogglePanel: (() -> Unit)? = null
            var latestPanelExpandedState = false

            subPresenterTestOf<ChatBenchmarkOuterEvent, ChatBenchmarkSubState>(
                presentFunction = {
                    val presenter =
                        remember(currentScreen) {
                            subPresenterFactory.create(currentScreen) as ChatBenchmarkSubPresenter
                        }
                    key(ChatBenchmarkSubScreen.contentKey(currentScreen.isExpanded)) {
                        val state = presenter.present(outerEventSink = {})
                        var isPanelExpanded by rememberSaveable { mutableStateOf(false) }
                        onTogglePanel = { isPanelExpanded = !isPanelExpanded }
                        latestPanelExpandedState = isPanelExpanded
                        state
                    }
                },
            ) {
                val initialState = expectMostRecentItem()
                assertThat(initialState.isGenerating).isTrue()
                assertThat(initialState.systemResourceStats).isEqualTo(initialStats)
                assertThat(monitorStartCount).isEqualTo(1)
                assertThat(latestPanelExpandedState).isFalse()

                // Simulate user tapping the benchmark panel on compact phone while LLM is streaming
                onTogglePanel?.invoke()
                expectMostRecentItem()
                assertThat(latestPanelExpandedState).isTrue()

                currentScreen =
                    currentScreen.copy(
                        throughputInfo = "TTFT: 320ms • Speed: 12.4 t/s",
                        contextStats = ContextUsageStats(usedTokens = 105, maxTokens = 8192),
                    )

                val token1State = awaitItem()
                assertThat(token1State.throughputInfo).isEqualTo("TTFT: 320ms • Speed: 12.4 t/s")
                assertThat(token1State.contextStats).isEqualTo(ContextUsageStats(usedTokens = 105, maxTokens = 8192))
                // System resource stats must NEVER drop to null between tokens (preventing tablet flicker)
                assertThat(token1State.systemResourceStats).isEqualTo(initialStats)
                assertThat(monitorStartCount).isEqualTo(1)
                assertThat(latestPanelExpandedState).isTrue()

                // Subsequent streamed tokens must preserve both the expanded toggle and live hardware telemetry
                currentScreen =
                    currentScreen.copy(
                        throughputInfo = "TTFT: 320ms • Speed: 14.8 t/s",
                        contextStats = ContextUsageStats(usedTokens = 112, maxTokens = 8192),
                    )

                val token2State = awaitItem()
                assertThat(token2State.throughputInfo).isEqualTo("TTFT: 320ms • Speed: 14.8 t/s")
                assertThat(token2State.contextStats).isEqualTo(ContextUsageStats(usedTokens = 112, maxTokens = 8192))
                assertThat(token2State.systemResourceStats).isEqualTo(initialStats)
                assertThat(monitorStartCount).isEqualTo(1)
                assertThat(latestPanelExpandedState).isTrue()

                // When generation finishes, isActive transitions to false and clears live hardware stats
                // while keeping the user's expanded panel state intact
                currentScreen =
                    currentScreen.copy(
                        isGenerating = false,
                        throughputInfo = "TTFT: 320ms • Speed: 15.1 t/s",
                    )

                val doneState = expectMostRecentItem()
                assertThat(doneState.isGenerating).isFalse()
                assertThat(doneState.systemResourceStats).isNull()
                assertThat(doneState.systemStatsInfo).isNull()
                assertThat(latestPanelExpandedState).isTrue()
            }
        }

    @Test
    fun `transitioning from isPreparing to isGenerating keeps telemetry active without restarting monitor`() =
        runTest {
            val initialStats = SystemResourceStats(cpuPercent = 55f, ramUsedGb = 4.1f, ramTotalGb = 8.0f)
            var monitorStartCount = 0
            val suspendingMonitor =
                object : SystemStatsMonitor {
                    override suspend fun monitorWhileActive(
                        isActive: () -> Boolean,
                        onStats: (String) -> Unit,
                    ) {
                        monitorMetricsWhileActive(isActive) { onStats(it.formattedSummary) }
                    }

                    override suspend fun monitorMetricsWhileActive(
                        isActive: () -> Boolean,
                        onMetrics: (SystemResourceStats) -> Unit,
                    ) {
                        monitorStartCount++
                        onMetrics(initialStats)
                        awaitCancellation()
                    }
                }
            val subPresenterFactory = ChatBenchmarkSubPresenterFactory(suspendingMonitor)

            var currentScreen by mutableStateOf(
                testScreen.copy(
                    isExpanded = true,
                    isPreparing = true,
                    isGenerating = false,
                ),
            )

            subPresenterTestOf<ChatBenchmarkOuterEvent, ChatBenchmarkSubState>(
                presentFunction = {
                    val presenter =
                        remember(currentScreen) {
                            subPresenterFactory.create(currentScreen) as ChatBenchmarkSubPresenter
                        }
                    key(ChatBenchmarkSubScreen.contentKey(currentScreen.isExpanded)) {
                        presenter.present(outerEventSink = {})
                    }
                },
            ) {
                val preparingState = expectMostRecentItem()
                assertThat(preparingState.isPreparing).isTrue()
                assertThat(preparingState.systemResourceStats).isEqualTo(initialStats)
                assertThat(monitorStartCount).isEqualTo(1)

                // Transition directly from preparing to generating
                currentScreen =
                    currentScreen.copy(
                        isPreparing = false,
                        isGenerating = true,
                        throughputInfo = "Prefilling...",
                    )

                val generatingState = awaitItem()
                assertThat(generatingState.isPreparing).isFalse()
                assertThat(generatingState.isGenerating).isTrue()
                assertThat(generatingState.systemResourceStats).isEqualTo(initialStats)
                assertThat(monitorStartCount).isEqualTo(1)
            }
        }
}
