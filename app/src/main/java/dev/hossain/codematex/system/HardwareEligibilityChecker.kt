package dev.hossain.codematex.system

import android.os.Build
import dev.hossain.codematex.BuildConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Hardware eligibility status for running on-device LLMs.
 *
 * References:
 * - Android Memory Management: https://developer.android.com/topic/performance/memory-overview
 * - Android 64-bit ABIs: https://developer.android.com/ndk/guides/abis
 * - Google Play Device Targeting: https://developer.android.com/google/play/device-targeting
 */
sealed interface HardwareEligibility {
    data object Eligible : HardwareEligibility

    data class Ineligible(
        val reason: String,
        val detectedRamGb: Double,
        val minRequiredRamGb: Double = 8.0,
        val is64BitSupported: Boolean,
    ) : HardwareEligibility
}

interface HardwareEligibilityChecker {
    fun checkEligibility(): HardwareEligibility
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class HardwareEligibilityCheckerImpl(
    private val deviceMemoryProvider: DeviceMemoryProvider,
    private val is64BitSupported: () -> Boolean = { Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() },
    private val isDevMode: () -> Boolean = { BuildConfig.DEV_MODE },
) : HardwareEligibilityChecker {
    override fun checkEligibility(): HardwareEligibility {
        // In DEV_MODE, bypass hardware restrictions so developers can test on emulators
        if (isDevMode()) {
            return HardwareEligibility.Eligible
        }

        val is64Bit = is64BitSupported()
        val memoryStats = deviceMemoryProvider.getMemoryStats()
        // Round to two decimals to avoid Float->Double precision issues at the threshold.
        val detectedGb = kotlin.math.round(memoryStats.totalGb.toDouble() * 100) / 100.0

        // 1. 64-bit Architecture Verification
        if (!is64Bit) {
            return HardwareEligibility.Ineligible(
                reason = "This app requires a modern 64-bit processor (arm64-v8a).",
                detectedRamGb = detectedGb,
                is64BitSupported = false,
            )
        }

        // 2. RAM Verification
        // Note: Devices sold with 8 GB RAM typically report ~7.2 GB - 7.5 GB to the kernel
        // due to hardware and baseband reservations.
        val minRequiredRamGb = 7.2
        if (detectedGb < minRequiredRamGb) {
            return HardwareEligibility.Ineligible(
                reason = "On-device AI models require at least 8 GB RAM for stable execution.",
                detectedRamGb = detectedGb,
                minRequiredRamGb = minRequiredRamGb,
                is64BitSupported = true,
            )
        }

        return HardwareEligibility.Eligible
    }
}
