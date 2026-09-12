package dev.hossain.codematex.ui.screens.debug

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.model.ModelConfig
import dev.hossain.codematex.data.repository.FakeModelConfigStore
import dev.hossain.codematex.data.repository.FakeModelRepository
import dev.hossain.codematex.data.repository.ModelConfigStore
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.data.repository.testModel
import dev.hossain.codematex.domain.runner.FakePlaygroundCodeRunner
import dev.hossain.codematex.domain.runner.PlaygroundCodeRunner
import dev.hossain.codematex.domain.runner.PlaygroundExecutionResult
import dev.hossain.codematex.runtime.FakeLlmEngine
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DebugMemoryProvider
import dev.hossain.codematex.system.DebugMemoryStats
import dev.hossain.codematex.system.DeviceMemoryProvider
import dev.hossain.codematex.system.FakeDeviceMemoryProvider
import dev.hossain.codematex.system.FakeHardwareEligibilityChecker
import dev.hossain.codematex.system.FakeNetworkMonitor
import dev.hossain.codematex.system.HardwareEligibility
import dev.hossain.codematex.system.HardwareEligibilityChecker
import dev.hossain.codematex.system.MemorySnapshot
import dev.hossain.codematex.system.NetworkMonitor
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

    private fun createPresenter(
        modelRepository: ModelRepository = FakeModelRepository(availableModels = listOf(downloadedModel)),
        llmEngine: LlmEngine = FakeLlmEngine(),
        configStore: ModelConfigStore = FakeModelConfigStore(),
        debugMemoryProvider: DebugMemoryProvider = FakeDebugMemoryProvider(),
        codeRunner: PlaygroundCodeRunner = FakePlaygroundCodeRunner(),
        networkMonitor: NetworkMonitor = FakeNetworkMonitor(),
        hardwareEligibilityChecker: HardwareEligibilityChecker = FakeHardwareEligibilityChecker(),
        deviceMemoryProvider: DeviceMemoryProvider = FakeDeviceMemoryProvider(),
        isDevMode: () -> Boolean = { false },
        navigator: Navigator = FakeNavigator(DebugScreen),
        screen: DebugScreen = DebugScreen,
    ): DebugPresenter =
        DebugPresenter(
            navigator = navigator,
            screen = screen,
            modelRepository = modelRepository,
            llmEngine = llmEngine,
            configStore = configStore,
            debugMemoryProvider = debugMemoryProvider,
            codeRunner = codeRunner,
            networkMonitor = networkMonitor,
            hardwareEligibilityChecker = hardwareEligibilityChecker,
            deviceMemoryProvider = deviceMemoryProvider,
            isDevMode = isDevMode,
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
                createPresenter(
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
                createPresenter(
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
                createPresenter(
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
                createPresenter(
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
                createPresenter(
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
    fun `select model and select backend update state`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel, notDownloadedModel))
            val fakeLlmEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    modelRepository = fakeRepo,
                    llmEngine = fakeLlmEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.SelectModel(notDownloadedModel))

                val updatedModel = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updatedModel.selectedModel).isEqualTo(notDownloadedModel)
                assertThat(updatedModel.statusMessage).contains("Selected model: ${notDownloadedModel.name}")

                updatedModel.eventSink(DebugScreen.Event.SelectBackend(LlmEngine.Backend.CPU))
                val updatedBackend = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updatedBackend.selectedBackend).isEqualTo(LlmEngine.Backend.CPU)
                assertThat(updatedBackend.statusMessage).contains("Set test backend to: CPU")
            }
        }

    @Test
    fun `update benchmark prompt updates state`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeLlmEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    modelRepository = fakeRepo,
                    llmEngine = fakeLlmEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.UpdateBenchmarkPrompt("Write a fibonacci function in Kotlin"))

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.benchmarkPrompt).isEqualTo("Write a fibonacci function in Kotlin")
            }
        }

    @Test
    fun `load model when weights not downloaded updates status message`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(notDownloadedModel))
            val fakeLlmEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                createPresenter(
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

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.statusMessage).contains("not downloaded")
                assertThat(fakeLlmEngine.initializeCalls).isEqualTo(0)
            }
        }

    @Test
    fun `load model failure updates status message and resets loading state`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeLlmEngine =
                FakeLlmEngine().apply {
                    activeBackendValue = null
                    shouldThrow = RuntimeException("Init failed")
                }
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                createPresenter(
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
                testScheduler.runCurrent()

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.statusMessage).contains("Load failed: Init failed")
                assertThat(updated.isLoadingModel).isFalse()
                assertThat(updated.isModelLoaded).isFalse()
            }
        }

    @Test
    fun `unload model failure updates status message and resets unloading state`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeLlmEngine = FakeLlmEngine().apply { cleanupThrows = RuntimeException("Cleanup native failed") }
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                createPresenter(
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
                testScheduler.runCurrent()

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.statusMessage).contains("Unload error: Cleanup native failed")
                assertThat(updated.isUnloadingModel).isFalse()
            }
        }

    @Test
    fun `run benchmark when model not loaded and no local path sets status message`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(notDownloadedModel))
            val fakeLlmEngine = FakeLlmEngine().apply { activeBackendValue = null }
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                createPresenter(
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

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.statusMessage).contains("Load a model first")
                assertThat(fakeLlmEngine.isolatedInferenceCalls).isEqualTo(0)
            }
        }

    @Test
    fun `run benchmark failure sets error status message`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeLlmEngine = FakeLlmEngine().apply { shouldThrow = RuntimeException("Inference crashed") }
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                createPresenter(
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

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.statusMessage).contains("Benchmark error: Inference crashed")
                assertThat(updated.isBenchmarking).isFalse()
            }
        }

    @Test
    fun `stop benchmark calls engine stop and updates status`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeLlmEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    modelRepository = fakeRepo,
                    llmEngine = fakeLlmEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.StopBenchmark)

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(fakeLlmEngine.stopCalls).isEqualTo(1)
                assertThat(updated.isBenchmarking).isFalse()
                assertThat(updated.statusMessage).contains("Benchmark stopped by user")
            }
        }

    @Test
    fun `delete model invokes repository delete`() =
        runTest {
            val fakeRepo = FakeModelRepository(availableModels = listOf(downloadedModel))
            val fakeLlmEngine = FakeLlmEngine()
            val fakeConfigStore = FakeModelConfigStore()
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val navigator = FakeNavigator(DebugScreen)

            val presenter =
                createPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    modelRepository = fakeRepo,
                    llmEngine = fakeLlmEngine,
                    configStore = fakeConfigStore,
                    debugMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.DeleteModel(downloadedModel))

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(fakeRepo.deleteCalls).containsExactly(downloadedModel)
                assertThat(updated.statusMessage).contains("Deleted weights for ${downloadedModel.name}")
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
                createPresenter(
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

    @Test
    fun `initial state sets default runner language, snippet, and online status`() =
        runTest {
            val fakeNetworkMonitor = FakeNetworkMonitor(initialIsOnline = true)
            val presenter = createPresenter(networkMonitor = fakeNetworkMonitor)

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(state.isOnline).isTrue()
                assertThat(state.runnerSelectedLang).isEqualTo("kotlin")
                assertThat(state.runnerSnippetCode).contains("Hello from CodeMateX Edge Runner (Kotlin)!")
                assertThat(state.isRunningSnippet).isFalse()
                assertThat(state.runnerResult).isNull()
                assertThat(state.isPingingProxy).isFalse()
            }
        }

    @Test
    fun `select runner language updates language and pre-populates snippet`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.SelectRunnerLanguage("rust"))

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.runnerSelectedLang).isEqualTo("rust")
                assertThat(updated.runnerSnippetCode).contains("Hello from CodeMateX Edge Runner (Rust)!")
                assertThat(updated.statusMessage).contains("Selected runner language: Rust")
            }
        }

    @Test
    fun `update runner snippet modifies code without changing language`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                val customCode = "fun main() { println(\"Custom!\") }"
                state.eventSink(DebugScreen.Event.UpdateRunnerSnippet(customCode))

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.runnerSnippetCode).isEqualTo(customCode)
                assertThat(updated.runnerSelectedLang).isEqualTo("kotlin")
            }
        }

    @Test
    fun `reset runner snippet restores default snippet for active language`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.UpdateRunnerSnippet("println(\"modified\");"))
                val modifiedState = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(modifiedState.runnerSnippetCode).isEqualTo("println(\"modified\");")

                modifiedState.eventSink(DebugScreen.Event.ResetRunnerSnippet)
                val resetState = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(resetState.runnerSnippetCode).contains("Hello from CodeMateX Edge Runner (Kotlin)!")
                assertThat(resetState.statusMessage).contains("Reset snippet for Kotlin.")
            }
        }

    @Test
    fun `run runner snippet executes code and updates state with success result and duration`() =
        runTest {
            val fakeRunner =
                FakePlaygroundCodeRunner(
                    resultToReturn = PlaygroundExecutionResult.Success("Sum: 15"),
                )
            val presenter = createPresenter(codeRunner = fakeRunner)

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.RunRunnerSnippet)

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(fakeRunner.lastCode).isEqualTo(state.runnerSnippetCode)
                assertThat(fakeRunner.lastLanguage).isEqualTo("kotlin")
                assertThat(updated.runnerResult).isEqualTo(PlaygroundExecutionResult.Success("Sum: 15"))
                assertThat(updated.runnerDurationMs).isNotNull()
                assertThat(updated.isRunningSnippet).isFalse()
                assertThat(updated.statusMessage).contains("Snippet executed successfully")
            }
        }

    @Test
    fun `run runner snippet handles compilation error result`() =
        runTest {
            val fakeRunner =
                FakePlaygroundCodeRunner(
                    resultToReturn = PlaygroundExecutionResult.CompilationError("Unresolved reference: foo"),
                )
            val presenter = createPresenter(codeRunner = fakeRunner)

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.RunRunnerSnippet)

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.runnerResult).isEqualTo(
                    PlaygroundExecutionResult.CompilationError("Unresolved reference: foo"),
                )
                assertThat(updated.statusMessage).contains("Compilation error")
                assertThat(updated.isRunningSnippet).isFalse()
            }
        }

    @Test
    fun `run runner snippet handles network error result`() =
        runTest {
            val fakeRunner =
                FakePlaygroundCodeRunner(
                    resultToReturn = PlaygroundExecutionResult.NetworkError("Host unreachable"),
                )
            val presenter = createPresenter(codeRunner = fakeRunner)

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.RunRunnerSnippet)

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.runnerResult).isEqualTo(
                    PlaygroundExecutionResult.NetworkError("Host unreachable"),
                )
                assertThat(updated.statusMessage).contains("Network error in")
                assertThat(updated.isRunningSnippet).isFalse()
            }
        }

    @Test
    fun `ping proxy measures latency when proxy is reachable`() =
        runTest {
            val fakeRunner =
                FakePlaygroundCodeRunner(
                    resultToReturn = PlaygroundExecutionResult.Success("ping"),
                )
            val presenter = createPresenter(codeRunner = fakeRunner)

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.PingProxy)

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.proxyPingMs).isNotNull()
                assertThat(updated.proxyPingError).isNull()
                assertThat(updated.isPingingProxy).isFalse()
                assertThat(updated.statusMessage).contains("Proxy reachable!")
            }
        }

    @Test
    fun `ping proxy records error when unreachable`() =
        runTest {
            val fakeRunner =
                FakePlaygroundCodeRunner(
                    resultToReturn = PlaygroundExecutionResult.NetworkError("Timeout connection"),
                )
            val presenter = createPresenter(codeRunner = fakeRunner)

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.PingProxy)

                val updated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(updated.proxyPingMs).isNull()
                assertThat(updated.proxyPingError).isEqualTo("Timeout connection")
                assertThat(updated.isPingingProxy).isFalse()
                assertThat(updated.statusMessage).contains("Proxy unreachable: Timeout connection")
            }
        }

    @Test
    fun `network monitor updates online status dynamically`() =
        runTest {
            val fakeNetworkMonitor = FakeNetworkMonitor(initialIsOnline = true)
            val presenter = createPresenter(networkMonitor = fakeNetworkMonitor)

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(state.isOnline).isTrue()

                fakeNetworkMonitor.setOnline(false)
                val offlineState = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(offlineState.isOnline).isFalse()

                fakeNetworkMonitor.setOnline(true)
                val backOnlineState = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(backOnlineState.isOnline).isTrue()
            }
        }

    @Test
    fun `initial state contains hardware eligibility and runtime specs`() =
        runTest {
            val fakeEligibilityChecker = FakeHardwareEligibilityChecker(result = HardwareEligibility.Eligible)
            val fakeMemoryProvider = FakeDeviceMemoryProvider()
            fakeMemoryProvider.returnedTotalBytes = 8_000_000_000L // 8.00 GB

            val presenter =
                createPresenter(
                    hardwareEligibilityChecker = fakeEligibilityChecker,
                    deviceMemoryProvider = fakeMemoryProvider,
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(state.hardwareEligibility).isEqualTo(HardwareEligibility.Eligible)
                assertThat(state.isDevMode).isFalse()

                // Verify runtime specs
                assertThat(state.runtimeSpecs["Inference Runtime"]).isEqualTo("Google LiteRT-LM")
                assertThat(state.runtimeSpecs["Runtime Version"]).isEqualTo(LITERT_LM_VERSION)
                assertThat(state.runtimeSpecs["GPU Acceleration"]).contains("OpenCL")
                assertThat(state.runtimeSpecs["NPU Acceleration"]).contains("Hexagon")
                assertThat(state.runtimeSpecs["CPU Fallback"]).contains("XNNPACK")
                assertThat(state.runtimeSpecs["Dev Mode Bypass"]).contains("Disabled")

                // Verify device info includes authoritative RAM bytes and arch check
                assertThat(state.deviceInfo["Authoritative RAM"]).isEqualTo("8.00 GB (8000000000 bytes)")
                assertThat(state.deviceInfo).containsKey("64-bit Architecture")
            }
        }

    @Test
    fun `ineligible hardware propagates ineligible state to debug screen`() =
        runTest {
            val fakeEligibilityChecker =
                FakeHardwareEligibilityChecker(
                    result =
                        HardwareEligibility.Ineligible(
                            reason = "Device has 3.5 GB RAM; minimum is 8.0 GB",
                            detectedRamGb = 3.5,
                            is64BitSupported = true,
                        ),
                )

            val presenter = createPresenter(hardwareEligibilityChecker = fakeEligibilityChecker)

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(state.hardwareEligibility).isInstanceOf(HardwareEligibility.Ineligible::class.java)
                val ineligible = state.hardwareEligibility as HardwareEligibility.Ineligible
                assertThat(ineligible.reason).contains("3.5 GB RAM")
            }
        }

    @Test
    fun `dev mode bypass activates when isDevMode is true`() =
        runTest {
            val presenter =
                createPresenter(
                    isDevMode = { true },
                )

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(state.isDevMode).isTrue()
                assertThat(state.runtimeSpecs["Dev Mode Bypass"]).contains("Active")
            }
        }

    @Test
    fun `initial state contains default benchmark sampler config`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(state.benchmarkConfig).isEqualTo(DEFAULT_BENCHMARK_CONFIG)
                assertThat(state.benchmarkConfig.temperature).isEqualTo(0.8f)
                assertThat(state.benchmarkConfig.topK).isEqualTo(40)
                assertThat(state.benchmarkConfig.topP).isEqualTo(0.95f)
                assertThat(state.benchmarkConfig.maxTokens).isEqualTo(512)
            }
        }

    @Test
    fun `updating benchmark sampler parameters updates state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success

                state.eventSink(DebugScreen.Event.UpdateBenchmarkTemperature(0.25f))
                val tempUpdated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(tempUpdated.benchmarkConfig.temperature).isEqualTo(0.25f)

                tempUpdated.eventSink(DebugScreen.Event.UpdateBenchmarkTopK(10))
                val topKUpdated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(topKUpdated.benchmarkConfig.topK).isEqualTo(10)

                topKUpdated.eventSink(DebugScreen.Event.UpdateBenchmarkTopP(0.85f))
                val topPUpdated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(topPUpdated.benchmarkConfig.topP).isEqualTo(0.85f)

                topPUpdated.eventSink(DebugScreen.Event.UpdateBenchmarkMaxTokens(256))
                val maxTokensUpdated = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(maxTokensUpdated.benchmarkConfig.maxTokens).isEqualTo(256)
            }
        }

    @Test
    fun `applying sampler preset updates benchmark config state`() =
        runTest {
            val presenter = createPresenter()

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success

                state.eventSink(DebugScreen.Event.ApplySamplerPreset(DebugScreen.BenchmarkSamplerPreset.GREEDY))
                val greedyState = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(greedyState.benchmarkConfig).isEqualTo(DebugScreen.BenchmarkSamplerPreset.GREEDY.config)
                assertThat(greedyState.benchmarkConfig.temperature).isEqualTo(0.1f)
                assertThat(greedyState.benchmarkConfig.topK).isEqualTo(1)

                greedyState.eventSink(DebugScreen.Event.ApplySamplerPreset(DebugScreen.BenchmarkSamplerPreset.CREATIVE))
                val creativeState = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(creativeState.benchmarkConfig).isEqualTo(DebugScreen.BenchmarkSamplerPreset.CREATIVE.config)
                assertThat(creativeState.benchmarkConfig.temperature).isEqualTo(1.0f)
                assertThat(creativeState.benchmarkConfig.topK).isEqualTo(80)

                creativeState.eventSink(DebugScreen.Event.ResetBenchmarkConfig)
                val resetState = expectMostRecentItem() as DebugScreen.State.Success
                assertThat(resetState.benchmarkConfig).isEqualTo(DEFAULT_BENCHMARK_CONFIG)
            }
        }

    @Test
    fun `running benchmark propagates custom benchmarkConfig to llmEngine runInferenceIsolated`() =
        runTest {
            val fakeEngine = FakeLlmEngine()
            val customConfig =
                ModelConfig(
                    temperature = 0.15f,
                    topK = 5,
                    topP = 0.8f,
                    maxTokens = 128,
                )

            val presenter = createPresenter(llmEngine = fakeEngine)

            presenter.test {
                val state = expectMostRecentItem() as DebugScreen.State.Success

                // Load model first
                state.eventSink(DebugScreen.Event.LoadModel)
                val loadedState = expectMostRecentItem() as DebugScreen.State.Success

                // Apply custom config
                loadedState.eventSink(DebugScreen.Event.UpdateBenchmarkTemperature(customConfig.temperature))
                val tempState = expectMostRecentItem() as DebugScreen.State.Success
                tempState.eventSink(DebugScreen.Event.UpdateBenchmarkTopK(customConfig.topK))
                val topKState = expectMostRecentItem() as DebugScreen.State.Success
                topKState.eventSink(DebugScreen.Event.UpdateBenchmarkTopP(customConfig.topP))
                val topPState = expectMostRecentItem() as DebugScreen.State.Success
                topPState.eventSink(DebugScreen.Event.UpdateBenchmarkMaxTokens(customConfig.maxTokens))
                val readyState = expectMostRecentItem() as DebugScreen.State.Success

                // Run benchmark
                readyState.eventSink(DebugScreen.Event.RunBenchmark)
                expectMostRecentItem()

                assertThat(fakeEngine.isolatedInferenceCalls).isEqualTo(1)
                assertThat(fakeEngine.lastIsolatedConfig).isEqualTo(customConfig)
            }
        }
}
