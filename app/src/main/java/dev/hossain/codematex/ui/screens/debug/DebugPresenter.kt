package dev.hossain.codematex.ui.screens.debug

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.data.model.AiModel
import dev.hossain.codematex.data.model.DownloadStatus
import dev.hossain.codematex.data.repository.ModelConfigStore
import dev.hossain.codematex.data.repository.ModelRepository
import dev.hossain.codematex.domain.runner.PlaygroundCodeRunner
import dev.hossain.codematex.domain.runner.PlaygroundExecutionResult
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DebugMemoryProvider
import dev.hossain.codematex.system.DebugMemoryStats
import dev.hossain.codematex.system.MemoryDelta
import dev.hossain.codematex.system.NetworkMonitor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

/**
 * Circuit Presenter for [DebugScreen], providing on-device LLM runtime profiling, edge code runner diagnostics,
 * and memory diagnostics.
 *
 * This presenter manages:
 * - **Model Loading Diagnostics**: Measures initialization latency (TTFL in milliseconds) and memory growth
 *   across Native C++ heap, JVM heap, and device RAM.
 * - **Model Unload & Cleanup**: Executes [LlmEngine.cleanup] to close active conversations and engine handles,
 *   triggers JVM garbage collection, and measures reclaimed native and system memory.
 * - **Real-Time Telemetry**: Periodically samples [DebugMemoryStats] (Native Heap, JVM Heap, Device RAM, CPU)
 *   every 750ms while the screen is active.
 * - **Inference Benchmarking**: Streams tokens on an isolated session to evaluate Time-to-First-Token (TTFT),
 *   decode throughput (tokens/second), and generation duration.
 * - **Edge Code Runner Diagnostics**: Runs multi-language snippets (Kotlin, Go, Rust, Python, TypeScript) via
 *   [PlaygroundCodeRunner] to evaluate Cloudflare Workers proxy health, roundtrip latency, and stdout/stderr.
 * - **Device & Storage Inspection**: Extracts hardware specifications and lists downloaded model files on disk.
 *
 * @param navigator Circuit navigator for screen transitions.
 * @param screen Screen argument representation for [DebugScreen].
 * @param modelRepository Repository providing available and downloaded LLM models.
 * @param llmEngine High-level on-device LiteRT-LM runtime engine.
 * @param configStore Model sampler and generation configuration store.
 * @param debugMemoryProvider Low-level memory sampler providing native heap, JVM heap, and system RAM metrics.
 * @param codeRunner Edge playground code execution runner.
 * @param networkMonitor Network connectivity monitor for online/offline mode.
 */
@AssistedInject
class DebugPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: DebugScreen,
    private val modelRepository: ModelRepository,
    private val llmEngine: LlmEngine,
    private val configStore: ModelConfigStore,
    private val debugMemoryProvider: DebugMemoryProvider,
    private val codeRunner: PlaygroundCodeRunner,
    private val networkMonitor: NetworkMonitor,
) : Presenter<DebugScreen.State> {
    /**
     * Assisted injection factory for [DebugPresenter].
     */
    @CircuitInject(DebugScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: DebugScreen,
        ): DebugPresenter
    }

    @Composable
    override fun present(): DebugScreen.State {
        val scope = rememberCoroutineScope()

        var models by rememberRetained { mutableStateOf<List<AiModel>>(emptyList()) }
        var selectedModel by rememberRetained { mutableStateOf<AiModel?>(null) }
        var selectedBackend by rememberRetained { mutableStateOf(LlmEngine.Backend.GPU) }

        var isModelLoaded by rememberRetained { mutableStateOf(llmEngine.getActiveBackend() != null) }
        var loadedModelName by rememberRetained { mutableStateOf<String?>(null) }
        var activeBackend by rememberRetained { mutableStateOf(llmEngine.getActiveBackend()) }
        var isLoadingModel by rememberRetained { mutableStateOf(false) }
        var isUnloadingModel by rememberRetained { mutableStateOf(false) }

        var lastLoadDelta by rememberRetained { mutableStateOf<MemoryDelta?>(null) }
        var lastUnloadDelta by rememberRetained { mutableStateOf<MemoryDelta?>(null) }
        var statusMessage by rememberRetained { mutableStateOf<String?>("Debugger ready. Select a model to profile.") }

        var telemetryStats by remember { mutableStateOf(debugMemoryProvider.getDebugMemoryStats()) }

        var benchmarkPrompt by rememberRetained { mutableStateOf(DEFAULT_BENCHMARK_PROMPT) }
        var isBenchmarking by rememberRetained { mutableStateOf(false) }
        var benchmarkTokens by rememberRetained { mutableStateOf("") }
        var benchmarkTtftMs by rememberRetained { mutableStateOf<Long?>(null) }
        var benchmarkSpeedTps by rememberRetained { mutableStateOf<Float?>(null) }
        var benchmarkTotalTokens by rememberRetained { mutableIntStateOf(0) }
        var benchmarkDurationMs by rememberRetained { mutableStateOf<Long?>(null) }

        var isOnline by rememberRetained { mutableStateOf(true) }
        var runnerSelectedLang by rememberRetained { mutableStateOf(DEFAULT_RUNNER_LANGUAGE) }
        var runnerSnippetCode by rememberRetained {
            mutableStateOf(DEFAULT_RUNNER_SNIPPETS[DEFAULT_RUNNER_LANGUAGE] ?: "")
        }
        var isRunningSnippet by rememberRetained { mutableStateOf(false) }
        var runnerResult by rememberRetained { mutableStateOf<PlaygroundExecutionResult?>(null) }
        var runnerDurationMs by rememberRetained { mutableStateOf<Long?>(null) }
        var isPingingProxy by rememberRetained { mutableStateOf(false) }
        var proxyPingMs by rememberRetained { mutableStateOf<Long?>(null) }
        var proxyPingError by rememberRetained { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            networkMonitor.isOnline.collect { online ->
                isOnline = online
            }
        }

        val deviceInfo =
            remember {
                val manufacturer =
                    Build.MANUFACTURER?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: "Generic"
                val brand = Build.BRAND.orEmpty()
                val model = Build.MODEL.orEmpty()
                val deviceModel = "$brand $model".trim().ifEmpty { "Android Device" }
                val release = Build.VERSION.RELEASE ?: "Unknown"
                val sdkInt = Build.VERSION.SDK_INT
                val abis = Build.SUPPORTED_ABIS?.joinToString(", ") ?: "arm64-v8a"
                val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

                mapOf(
                    "Manufacturer" to manufacturer,
                    "Device Model" to deviceModel,
                    "Android OS" to "Android $release (API $sdkInt)",
                    "CPU Cores" to "$cores cores",
                    "Supported ABIs" to abis,
                    "Total RAM" to "${"%.1f".format(debugMemoryProvider.getDebugMemoryStats().ramTotalGb)} GB",
                )
            }

        LaunchedEffect(Unit) {
            modelRepository.getAvailableModels().collect { list ->
                models = list
                if (selectedModel == null) {
                    selectedModel = list.firstOrNull { it.downloadStatus == DownloadStatus.DOWNLOADED } ?: list.firstOrNull()
                }
            }
        }

        // Real-time telemetry monitoring loop
        LaunchedEffect(Unit) {
            while (isActive) {
                val stats = debugMemoryProvider.getDebugMemoryStats()
                telemetryStats = stats
                Timber.d(
                    "DebugPresenter [TELEMETRY]: nativeAllocatedMb=%.1f, jvmUsedMb=%.1f, ramUsedGb=%.2f/%.2f, isLowMemory=%b",
                    stats.nativeAllocatedMb,
                    stats.jvmUsedMb,
                    stats.ramUsedGb,
                    stats.ramTotalGb,
                    stats.isLowMemory,
                )
                delay(750.milliseconds)
            }
        }

        return DebugScreen.State.Success(
            models = models,
            selectedModel = selectedModel,
            selectedBackend = selectedBackend,
            isModelLoaded = isModelLoaded,
            loadedModelName = loadedModelName,
            activeBackend = activeBackend,
            isLoadingModel = isLoadingModel,
            isUnloadingModel = isUnloadingModel,
            lastLoadDelta = lastLoadDelta,
            lastUnloadDelta = lastUnloadDelta,
            statusMessage = statusMessage,
            telemetryStats = telemetryStats,
            benchmarkPrompt = benchmarkPrompt,
            isBenchmarking = isBenchmarking,
            benchmarkTokens = benchmarkTokens,
            benchmarkTtftMs = benchmarkTtftMs,
            benchmarkSpeedTps = benchmarkSpeedTps,
            benchmarkTotalTokens = benchmarkTotalTokens,
            benchmarkDurationMs = benchmarkDurationMs,
            deviceInfo = deviceInfo,
            isOnline = isOnline,
            runnerSelectedLang = runnerSelectedLang,
            runnerSnippetCode = runnerSnippetCode,
            isRunningSnippet = isRunningSnippet,
            runnerResult = runnerResult,
            runnerDurationMs = runnerDurationMs,
            isPingingProxy = isPingingProxy,
            proxyPingMs = proxyPingMs,
            proxyPingError = proxyPingError,
        ) { event ->
            when (event) {
                is DebugScreen.Event.SelectModel -> {
                    selectedModel = event.model
                    statusMessage = "Selected model: ${event.model.name}"
                    Timber.d("DebugPresenter: Selected model: ${event.model.name} (${event.model.id})")
                }

                is DebugScreen.Event.SelectBackend -> {
                    selectedBackend = event.backend
                    statusMessage = "Set test backend to: ${event.backend.name}"
                    Timber.d("DebugPresenter: Target backend changed to: ${event.backend.name}")
                }

                // Handles explicit model loading and initialization latency/memory measurement
                DebugScreen.Event.LoadModel -> {
                    val model = selectedModel
                    if (model == null) {
                        statusMessage = "No model selected."
                        return@Success
                    }
                    if (model.downloadStatus != DownloadStatus.DOWNLOADED && model.localPath == null) {
                        statusMessage = "Model weights are not downloaded on device."
                        return@Success
                    }

                    scope.launch {
                        isLoadingModel = true
                        statusMessage = "Initializing ${model.name} on ${selectedBackend.name}..."
                        Timber.i(
                            "DebugPresenter [LOAD_START]: model=%s, backend=%s, path=%s",
                            model.name,
                            selectedBackend.name,
                            model.localPath,
                        )
                        val beforeSnap = debugMemoryProvider.captureSnapshot()

                        try {
                            val config = configStore.getConfig(model.id)
                            llmEngine.initialize(
                                modelPath = model.localPath ?: "",
                                backend = selectedBackend,
                                systemInstruction = "You are a helpful coding assistant.",
                                config = config,
                            )
                            val afterSnap = debugMemoryProvider.captureSnapshot()
                            val delta = afterSnap.diffFrom(beforeSnap)
                            lastLoadDelta = delta
                            isModelLoaded = true
                            loadedModelName = model.name
                            activeBackend = llmEngine.getActiveBackend()
                            statusMessage =
                                "Loaded in ${delta.durationMs}ms on ${activeBackend?.name ?: selectedBackend.name}. " +
                                "Native Δ: ${"%.1f".format(
                                    delta.deltaNativeMb,
                                )} MB, RAM Δ: ${"%.1f".format(delta.deltaSystemMb)} MB"
                            Timber.i(
                                "DebugPresenter [LOAD_SUCCESS]: model=%s, activeBackend=%s, durationMs=%d, deltaNativeMb=%.1f, deltaJvmMb=%.1f, deltaSystemMb=%.1f",
                                model.name,
                                activeBackend?.name ?: selectedBackend.name,
                                delta.durationMs,
                                delta.deltaNativeMb,
                                delta.deltaJvmMb,
                                delta.deltaSystemMb,
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "DebugPresenter [LOAD_ERROR]: Failed initializing %s on %s", model.name, selectedBackend.name)
                            statusMessage = "Load failed: ${e.message}"
                        } finally {
                            isLoadingModel = false
                        }
                    }
                }

                // Closes engine handles and triggers JVM GC to measure memory reclaimed
                DebugScreen.Event.UnloadModel -> {
                    scope.launch {
                        isUnloadingModel = true
                        val modelToUnload = loadedModelName ?: "Current Model"
                        statusMessage = "Unloading model from memory and releasing native buffers..."
                        Timber.i("DebugPresenter [UNLOAD_START]: model=%s", modelToUnload)
                        val beforeSnap = debugMemoryProvider.captureSnapshot()

                        try {
                            llmEngine.cleanup()
                            val gcBytes = debugMemoryProvider.triggerGc()
                            val afterSnap = debugMemoryProvider.captureSnapshot()
                            val delta = afterSnap.diffFrom(beforeSnap)
                            lastUnloadDelta = delta
                            isModelLoaded = false
                            loadedModelName = null
                            activeBackend = null
                            statusMessage =
                                "Unloaded in ${delta.durationMs}ms. " +
                                "Native freed: ${"%.1f".format(
                                    -delta.deltaNativeMb,
                                )} MB, System RAM freed: ${"%.1f".format(-delta.deltaSystemMb)} MB"
                            Timber.i(
                                "DebugPresenter [UNLOAD_SUCCESS]: model=%s, durationMs=%d, freedNativeMb=%.1f, freedJvmMb=%.1f, freedSystemMb=%.1f, gcReclaimedMb=%.1f",
                                modelToUnload,
                                delta.durationMs,
                                -delta.deltaNativeMb,
                                -delta.deltaJvmMb,
                                -delta.deltaSystemMb,
                                gcBytes / (1024f * 1024f),
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "DebugPresenter [UNLOAD_ERROR]: Failed unloading %s", modelToUnload)
                            statusMessage = "Unload error: ${e.message}"
                        } finally {
                            isUnloadingModel = false
                        }
                    }
                }

                is DebugScreen.Event.UpdateBenchmarkPrompt -> {
                    benchmarkPrompt = event.prompt
                }

                // Runs isolated inference to evaluate TTFT, decode throughput (t/s), and generation duration
                DebugScreen.Event.RunBenchmark -> {
                    if (!isModelLoaded && selectedModel?.localPath == null) {
                        statusMessage = "Load a model first before running inference benchmark."
                        return@Success
                    }

                    scope.launch {
                        isBenchmarking = true
                        benchmarkTokens = ""
                        benchmarkTtftMs = null
                        benchmarkSpeedTps = null
                        benchmarkTotalTokens = 0
                        benchmarkDurationMs = null
                        statusMessage = "Running benchmark evaluation..."
                        Timber.i(
                            "DebugPresenter [BENCHMARK_START]: model=%s, promptLength=%d",
                            selectedModel?.name ?: loadedModelName ?: "Unknown",
                            benchmarkPrompt.length,
                        )

                        val startTime = System.currentTimeMillis()
                        var firstTokenTime: Long? = null
                        var tokenCount = 0

                        try {
                            val currentConfig = selectedModel?.let { configStore.getConfig(it.id) } ?: configStore.config
                            llmEngine.runInferenceIsolated(
                                input = benchmarkPrompt,
                                systemInstruction = "You are a concise coding assistant.",
                                config = currentConfig,
                            ) { partial, done ->
                                val now = System.currentTimeMillis()
                                if (firstTokenTime == null && partial.isNotEmpty()) {
                                    val ttft = now - startTime
                                    firstTokenTime = now
                                    benchmarkTtftMs = ttft
                                    Timber.d("DebugPresenter [BENCHMARK_TTFT]: ttftMs=%d", ttft)
                                }

                                if (partial.isNotEmpty()) {
                                    tokenCount++
                                    benchmarkTotalTokens = tokenCount
                                    benchmarkTokens += partial

                                    val decodeTimeSec = (now - (firstTokenTime ?: startTime)) / 1000f
                                    if (decodeTimeSec > 0.05f) {
                                        benchmarkSpeedTps = tokenCount / decodeTimeSec
                                    }
                                }

                                if (done) {
                                    val totalDuration = now - startTime
                                    benchmarkDurationMs = totalDuration
                                    val finalDecodeSec = (now - (firstTokenTime ?: startTime)) / 1000f
                                    if (finalDecodeSec > 0f) {
                                        benchmarkSpeedTps = tokenCount / finalDecodeSec
                                    }
                                    statusMessage =
                                        "Benchmark finished: $tokenCount tokens in ${totalDuration}ms " +
                                        "(TTFT: ${benchmarkTtftMs ?: 0}ms, Speed: ${"%.1f".format(benchmarkSpeedTps ?: 0f)} t/s)"
                                    Timber.i(
                                        "DebugPresenter [BENCHMARK_SUCCESS]: tokens=%d, durationMs=%d, ttftMs=%d, speedTps=%.2f",
                                        tokenCount,
                                        totalDuration,
                                        benchmarkTtftMs ?: 0,
                                        benchmarkSpeedTps ?: 0f,
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "DebugPresenter [BENCHMARK_ERROR]: Inference benchmark failed")
                            statusMessage = "Benchmark error: ${e.message}"
                        } finally {
                            isBenchmarking = false
                        }
                    }
                }

                // Cancels ongoing token generation
                DebugScreen.Event.StopBenchmark -> {
                    llmEngine.stop()
                    isBenchmarking = false
                    statusMessage = "Benchmark stopped by user."
                    Timber.i("DebugPresenter [BENCHMARK_CANCELLED]: Benchmark stopped by user")
                }

                // Requests explicit JVM Garbage Collection and updates telemetry
                DebugScreen.Event.TriggerGc -> {
                    val reclaimedBytes = debugMemoryProvider.triggerGc()
                    val reclaimedMb = reclaimedBytes / (1024f * 1024f)
                    telemetryStats = debugMemoryProvider.getDebugMemoryStats()
                    statusMessage = "Garbage collection completed. Reclaimed ${"%.2f".format(reclaimedMb)} MB of JVM heap."
                    Timber.i("DebugPresenter [GC_TRIGGER]: reclaimedJvmMb=%.2f", reclaimedMb)
                }

                is DebugScreen.Event.DeleteModel -> {
                    scope.launch {
                        modelRepository.deleteModel(event.model)
                        statusMessage = "Deleted weights for ${event.model.name}."
                    }
                }

                is DebugScreen.Event.SelectRunnerLanguage -> {
                    runnerSelectedLang = event.language
                    runnerSnippetCode = DEFAULT_RUNNER_SNIPPETS[event.language] ?: ""
                    runnerResult = null
                    runnerDurationMs = null
                    statusMessage = "Selected runner language: ${event.language.replaceFirstChar { it.uppercase() }}"
                    Timber.d("DebugPresenter: Selected runner language: %s", event.language)
                }

                is DebugScreen.Event.UpdateRunnerSnippet -> {
                    runnerSnippetCode = event.code
                }

                DebugScreen.Event.ResetRunnerSnippet -> {
                    runnerSnippetCode = DEFAULT_RUNNER_SNIPPETS[runnerSelectedLang] ?: ""
                    statusMessage = "Reset snippet for ${runnerSelectedLang.replaceFirstChar { it.uppercase() }}."
                    Timber.d("DebugPresenter: Reset snippet for %s", runnerSelectedLang)
                }

                DebugScreen.Event.RunRunnerSnippet -> {
                    scope.launch {
                        isRunningSnippet = true
                        runnerResult = null
                        runnerDurationMs = null
                        statusMessage = "Executing ${runnerSelectedLang.replaceFirstChar { it.uppercase() }} snippet at edge..."
                        Timber.i(
                            "DebugPresenter [RUNNER_START]: lang=%s, codeLength=%d",
                            runnerSelectedLang,
                            runnerSnippetCode.length,
                        )
                        val start = System.currentTimeMillis()
                        try {
                            val result = codeRunner.runSnippet(runnerSnippetCode, runnerSelectedLang)
                            val duration = System.currentTimeMillis() - start
                            runnerDurationMs = duration
                            runnerResult = result
                            statusMessage =
                                when (result) {
                                    is PlaygroundExecutionResult.Success -> {
                                        "Snippet executed successfully in ${duration}ms via edge proxy."
                                    }

                                    is PlaygroundExecutionResult.CompilationError -> {
                                        "Compilation error in ${duration}ms."
                                    }

                                    is PlaygroundExecutionResult.NetworkError -> {
                                        "Network error in ${duration}ms: ${result.message}"
                                    }
                                }
                            Timber.i(
                                "DebugPresenter [RUNNER_RESULT]: lang=%s, durationMs=%d, result=%s",
                                runnerSelectedLang,
                                duration,
                                result::class.simpleName,
                            )
                        } catch (e: Exception) {
                            val duration = System.currentTimeMillis() - start
                            runnerDurationMs = duration
                            val err = PlaygroundExecutionResult.NetworkError(e.message ?: "Execution failed")
                            runnerResult = err
                            statusMessage = "Runner error: ${e.message}"
                            Timber.e(e, "DebugPresenter [RUNNER_ERROR]: Execution failed")
                        } finally {
                            isRunningSnippet = false
                        }
                    }
                }

                DebugScreen.Event.PingProxy -> {
                    scope.launch {
                        isPingingProxy = true
                        proxyPingMs = null
                        proxyPingError = null
                        statusMessage = "Testing Cloudflare edge proxy reachability..."
                        val start = System.currentTimeMillis()
                        try {
                            val pingResult = codeRunner.runSnippet("println(\"ping\")", "kotlin")
                            val duration = System.currentTimeMillis() - start
                            when (pingResult) {
                                is PlaygroundExecutionResult.Success, is PlaygroundExecutionResult.CompilationError -> {
                                    proxyPingMs = duration
                                    statusMessage = "Proxy reachable! Roundtrip latency: ${duration}ms."
                                    Timber.i("DebugPresenter [PING_SUCCESS]: durationMs=%d", duration)
                                }

                                is PlaygroundExecutionResult.NetworkError -> {
                                    proxyPingError = pingResult.message
                                    statusMessage = "Proxy unreachable: ${pingResult.message}"
                                    Timber.w("DebugPresenter [PING_FAIL]: error=%s", pingResult.message)
                                }
                            }
                        } catch (e: Exception) {
                            proxyPingError = e.message ?: "Ping request failed"
                            statusMessage = "Ping failed: ${e.message}"
                            Timber.e(e, "DebugPresenter [PING_ERROR]: Ping request failed")
                        } finally {
                            isPingingProxy = false
                        }
                    }
                }

                DebugScreen.Event.Back -> {
                    navigator.pop()
                }
            }
        }
    }
}
