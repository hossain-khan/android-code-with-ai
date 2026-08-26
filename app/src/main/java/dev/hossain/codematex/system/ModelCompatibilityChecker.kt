package dev.hossain.codematex.system

import dev.hossain.codematex.BuildConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Per-model hardware compatibility result.
 */
@Serializable
sealed interface ModelCompatibility {
    data object Compatible : ModelCompatibility

    /**
     * @param reason A user-facing explanation for why the model cannot be used.
     */
    @Serializable
    data class Incompatible(
        val reason: String,
    ) : ModelCompatibility
}

/**
 * Checks whether a specific model's RAM requirement can be satisfied by this device.
 */
interface ModelCompatibilityChecker {
    /**
     * Returns compatibility information for the given model requirement.
     */
    fun checkCompatibility(modelMinRamGb: Int): ModelCompatibility

    /**
     * Returns structured memory information about the current device.
     */
    fun getDeviceMemoryInfo(): DeviceMemoryInfo
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ModelCompatibilityCheckerImpl(
    private val deviceMemoryProvider: DeviceMemoryProvider,
    private val isDevMode: () -> Boolean = { BuildConfig.DEV_MODE },
) : ModelCompatibilityChecker {
    override fun checkCompatibility(modelMinRamGb: Int): ModelCompatibility {
        // In DEV_MODE, bypass hardware restrictions so developers can test on emulators.
        if (isDevMode()) {
            return ModelCompatibility.Compatible
        }

        if (modelMinRamGb == 0) {
            return ModelCompatibility.Compatible
        }

        val totalBytes = deviceMemoryProvider.getTotalMemoryBytes()
        return if (MemoryCompatibilityPolicy.isCompatible(modelMinRamGb, totalBytes)) {
            ModelCompatibility.Compatible
        } else {
            val info = getDeviceMemoryInfo()
            ModelCompatibility.Incompatible(
                reason =
                    "Requires ${modelMinRamGb}GB RAM (Device has " +
                        "${formatDecimalGb(info.displayTotalGb)}${info.displayLabel})",
            )
        }
    }

    override fun getDeviceMemoryInfo(): DeviceMemoryInfo {
        val totalBytes = deviceMemoryProvider.getTotalMemoryBytes()
        return DeviceMemoryInfo(
            totalBytes = totalBytes,
            displayTotalGb = MemoryCompatibilityPolicy.toDecimalGigabytes(totalBytes),
            displayLabel = "GB",
        )
    }

    private fun formatDecimalGb(value: Double): String =
        if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
}
