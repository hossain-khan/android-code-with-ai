package dev.hossain.codematex.ui.screens.chat.benchmark

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.data.FakeSystemStatsMonitor
import dev.hossain.codematex.system.ContextUsageStats
import dev.hossain.codematex.system.SystemResourceStats
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ChatBenchmarkSubPresenterTest {
    private val fakeSystemStatsMonitor = FakeSystemStatsMonitor()

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
}
