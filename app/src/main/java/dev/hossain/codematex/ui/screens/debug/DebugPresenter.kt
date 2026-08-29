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
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DebugMemoryProvider
import dev.hossain.codematex.system.DebugMemoryStats
import dev.hossain.codematex.system.MemoryDelta
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

@AssistedInject
class DebugPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: DebugScreen,
    private val modelRepository: ModelRepository,
    private val llmEngine: LlmEngine,
    private val configStore: ModelConfigStore,
    private val debugMemoryProvider: DebugMemoryProvider,
) : Presenter<DebugScreen.State> {
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
                telemetryStats = debugMemoryProvider.getDebugMemoryStats()
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
        ) { event ->
            when (event) {
                is DebugScreen.Event.SelectModel -> {
                    selectedModel = event.model
                    statusMessage = "Selected model: ${event.model.name}"
                }

                is DebugScreen.Event.SelectBackend -> {
                    selectedBackend = event.backend
                    statusMessage = "Set test backend to: ${event.backend.name}"
                }

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
                        } catch (e: Exception) {
                            Timber.e(e, "DebugPresenter: Load failed")
                            statusMessage = "Load failed: ${e.message}"
                        } finally {
                            isLoadingModel = false
                        }
                    }
                }

                DebugScreen.Event.UnloadModel -> {
                    scope.launch {
                        isUnloadingModel = true
                        statusMessage = "Unloading model from memory and releasing native buffers..."
                        val beforeSnap = debugMemoryProvider.captureSnapshot()

                        try {
                            llmEngine.cleanup()
                            debugMemoryProvider.triggerGc()
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
                        } catch (e: Exception) {
                            Timber.e(e, "DebugPresenter: Unload failed")
                            statusMessage = "Unload error: ${e.message}"
                        } finally {
                            isUnloadingModel = false
                        }
                    }
                }

                is DebugScreen.Event.UpdateBenchmarkPrompt -> {
                    benchmarkPrompt = event.prompt
                }

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
                                }
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "DebugPresenter: Benchmark failed")
                            statusMessage = "Benchmark error: ${e.message}"
                        } finally {
                            isBenchmarking = false
                        }
                    }
                }

                DebugScreen.Event.StopBenchmark -> {
                    llmEngine.stop()
                    isBenchmarking = false
                    statusMessage = "Benchmark stopped by user."
                }

                DebugScreen.Event.TriggerGc -> {
                    val reclaimedBytes = debugMemoryProvider.triggerGc()
                    val reclaimedMb = reclaimedBytes / (1024f * 1024f)
                    telemetryStats = debugMemoryProvider.getDebugMemoryStats()
                    statusMessage = "Garbage collection completed. Reclaimed ${"%.2f".format(reclaimedMb)} MB of JVM heap."
                }

                is DebugScreen.Event.DeleteModel -> {
                    scope.launch {
                        modelRepository.deleteModel(event.model)
                        statusMessage = "Deleted weights for ${event.model.name}."
                    }
                }

                DebugScreen.Event.Back -> {
                    navigator.pop()
                }
            }
        }
    }
}
