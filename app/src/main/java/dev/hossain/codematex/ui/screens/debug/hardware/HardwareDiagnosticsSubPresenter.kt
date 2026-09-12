package dev.hossain.codematex.ui.screens.debug.hardware

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.BuildConfig
import dev.hossain.codematex.system.DebugMemoryProvider
import dev.hossain.codematex.system.DeviceMemoryProvider
import dev.hossain.codematex.system.HardwareEligibilityChecker
import dev.hossain.codematex.system.MemoryCompatibilityPolicy
import dev.hossain.codematex.ui.screens.debug.LITERT_LM_VERSION
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

class HardwareDiagnosticsSubPresenter(
    private val screen: HardwareDiagnosticsSubScreen,
    private val hardwareEligibilityChecker: HardwareEligibilityChecker,
    private val deviceMemoryProvider: DeviceMemoryProvider,
    private val debugMemoryProvider: DebugMemoryProvider,
    private val isDevMode: () -> Boolean = { BuildConfig.DEV_MODE },
) : SubPresenter<HardwareDiagnosticsOuterEvent, HardwareDiagnosticsSubState> {
    @Composable
    override fun present(outerEventSink: (HardwareDiagnosticsOuterEvent) -> Unit): HardwareDiagnosticsSubState {
        val eligibility = remember { hardwareEligibilityChecker.checkEligibility() }
        val devModeActive = remember { isDevMode() }

        val runtimeSpecs =
            remember(screen.activeBackendName, devModeActive) {
                mapOf(
                    "Inference Runtime" to "Google LiteRT-LM",
                    "Runtime Version" to LITERT_LM_VERSION,
                    "Active Backend" to (screen.activeBackendName ?: "Idle / Unloaded"),
                    "GPU Acceleration" to "OpenCL / Vulkan",
                    "NPU Acceleration" to "Qualcomm Hexagon / NNAPI",
                    "CPU Fallback" to "XNNPACK SIMD (FP32/FP16)",
                    "Dev Mode Bypass" to
                        if (devModeActive) {
                            "Active (Bypassing RAM checks)"
                        } else {
                            "Disabled (8GB RAM required)"
                        },
                )
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
                val totalMemoryBytes = deviceMemoryProvider.getTotalMemoryBytes()
                val detectedRamGb = MemoryCompatibilityPolicy.toDecimalGigabytes(totalMemoryBytes)
                val is64Bit = Build.SUPPORTED_64_BIT_ABIS?.isNotEmpty() == true

                mapOf(
                    "Manufacturer" to manufacturer,
                    "Device Model" to deviceModel,
                    "Android OS" to "Android $release (API $sdkInt)",
                    "CPU Cores" to "$cores cores",
                    "Supported ABIs" to abis,
                    "64-bit Architecture" to if (is64Bit) "Yes (arm64-v8a)" else "No (32-bit only)",
                    "Total System RAM" to "${"%.1f".format(debugMemoryProvider.getDebugMemoryStats().ramTotalGb)} GB",
                    "Authoritative RAM" to "${"%.2f".format(detectedRamGb)} GB ($totalMemoryBytes bytes)",
                )
            }

        return HardwareDiagnosticsSubState(
            deviceInfo = deviceInfo,
            runtimeSpecs = runtimeSpecs,
            eligibility = eligibility,
            isDevMode = devModeActive,
        )
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class HardwareDiagnosticsSubPresenterFactory(
    private val hardwareEligibilityChecker: HardwareEligibilityChecker,
    private val deviceMemoryProvider: DeviceMemoryProvider,
    private val debugMemoryProvider: DebugMemoryProvider,
) : SubPresenterFactory {
    override fun create(screen: SubScreen<*>): SubPresenter<*, *>? =
        when (screen) {
            is HardwareDiagnosticsSubScreen -> {
                HardwareDiagnosticsSubPresenter(
                    screen = screen,
                    hardwareEligibilityChecker = hardwareEligibilityChecker,
                    deviceMemoryProvider = deviceMemoryProvider,
                    debugMemoryProvider = debugMemoryProvider,
                )
            }

            else -> {
                null
            }
        }
}
