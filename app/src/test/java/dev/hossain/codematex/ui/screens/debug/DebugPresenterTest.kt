package dev.hossain.codematex.ui.screens.debug

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.repository.FakeModelConfigStore
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.runtime.FakeLlmEngine
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DebugMemoryProvider
import dev.hossain.codematex.system.DebugMemoryStats
import dev.hossain.codematex.system.MemorySnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeDebugMemoryProvider(
    var stats: DebugMemoryStats =
        DebugMemoryStats(
            nativeAllocatedMb = 1200f,
            nativeTotalMb = 2048f,
            nativeFreeMb = 848f,
            jvmUsedMb = 50f,
            jvmTotalMb = 100f,
            jvmMaxMb = 512f,
            ramUsedGb = 4f,
            ramTotalGb = 8f,
            ramAvailGb = 4f,
            isLowMemory = false,
            cpuPercent = 15f,
        ),
    var snapshot: MemorySnapshot =
        MemorySnapshot(
            nativeAllocatedBytes = 1_000_000_000L,
            jvmUsedBytes = 50_000_000L,
            systemAvailBytes = 4_000_000_000L,
        ),
) : DebugMemoryProvider {
    var triggerGcCalls = 0

    override fun getDebugMemoryStats(): DebugMemoryStats = stats

    override fun captureSnapshot(): MemorySnapshot = snapshot

    override fun triggerGc(): Long {
        triggerGcCalls++
        return 10_000_000L
    }
}

class DebugPresenterTest {
    private val downloadedModel =
        testModel(
            id = "google/gemma-2-2b-it",
            downloadStatus = DownloadStatus.DOWNLOADED,
            localPath = "/mock/path/gemma.bin",
        )

    private val notDownloadedModel =
        testModel(
            id = "google/gemma-4-e2b",
            downloadStatus = DownloadStatus.NOT_DOWNLOADED,
            localPath = null,
        )

    @Test
    fun `initial state loads models and selects first downloaded model`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel, notDownloadedModel))
            val fakeLlmEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                DebugPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    modelRepository = fakeRepo,
                    llmEngine = fakeLlmEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(state.models).containsExactly(downloadedModel, notDownloadedModel).inOrder()
                assertThat(state.selectedModel).isEqualTo(downloadedModel)
                assertThat(state.selectedBackend).isEqualTo(LlmEngine.Backend.GPU)
                assertThat(state.telemetryStats.nativeAllocatedMb).isEqualTo(1200f)
            }
        }

    @Test
    fun `load model initializes LLM engine and captures memory delta`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeLlmEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                DebugPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    modelRepository = fakeRepo,
                    llmEngine = fakeLlmEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.LoadModel)

                val updatedState = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(fakeLlmEngine.initializeCalls).isEqualTo(1)
                assertThat(updatedState.isModelLoaded).isTrue()
                assertThat(updatedState.loadedModelName).isEqualTo(downloadedModel.name)
                assertThat(updatedState.lastLoadDelta).isNotNull()
            }
        }

    @Test
    fun `unload model calls engine cleanup and triggers GC`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeLlmEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                DebugPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    modelRepository = fakeRepo,
                    llmEngine = fakeLlmEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.UnloadModel)

                val updatedState = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(fakeLlmEngine.cleanupCalls).isEqualTo(1)
                assertThat(fakeMemoryProvider.triggerGcCalls).isEqualTo(1)
                assertThat(updatedState.isModelLoaded).isFalse()
                assertThat(updatedState.loadedModelName).isNull()
                assertThat(updatedState.lastUnloadDelta).isNotNull()
            }
        }

    @Test
    fun `run benchmark streams tokens and computes metrics`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeLlmEngine =
                FakeLlmEngine().apply {
                    responseTokens = listOf("Hello", " from", " on-device", " LLM!")
                }
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                DebugPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    modelRepository = fakeRepo,
                    llmEngine = fakeLlmEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.RunBenchmark)

                val updatedState = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(fakeLlmEngine.isolatedInferenceCalls).isEqualTo(1)
                assertThat(updatedState.benchmarkTokens).isEqualTo("Hello from on-device LLM!")
                assertThat(updatedState.benchmarkTotalTokens).isEqualTo(4)
                assertThat(updatedState.benchmarkTtftMs).isNotNull()
            }
        }

    @Test
    fun `trigger GC event invokes GC on memory provider`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeLlmEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                DebugPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    modelRepository = fakeRepo,
                    llmEngine = fakeLlmEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.TriggerGc)

                val updatedState = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(fakeMemoryProvider.triggerGcCalls).isEqualTo(1)
                assertThat(updatedState.statusMessage).contains("Garbage collection completed")
            }
        }

    @Test
    fun `back event pops navigator`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeLlmEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                DebugPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    modelRepository = fakeRepo,
                    llmEngine = fakeLlmEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.Back)
                assertThat(navigator.awaitPop()).isNotNull()
            }
        }
}
