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

/**
 * Checks overall device hardware eligibility (such as 64-bit ABI support and baseline RAM capacity)
 * to run on-device Large Language Models.
 */
interface HardwareEligibilityChecker {
    /**
     * Evaluates device architecture and total available memory, returning [HardwareEligibility.Eligible]
     * or [HardwareEligibility.Ineligible] with the specific failure cause.
     */
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
        val totalBytes = deviceMemoryProvider.getTotalMemoryBytes()
        val detectedGb = MemoryCompatibilityPolicy.toDecimalGigabytes(totalBytes)
        val minRequiredBytes = MemoryCompatibilityPolicy.minimumRequiredBytes(8)
        val minRequiredRamGb = MemoryCompatibilityPolicy.toDecimalGigabytes(minRequiredBytes)

        // 1. 64-bit Architecture Verification & 2. RAM Verification
        // An 8 GB marketed device must report at least ~7.2 GB to the kernel. The same byte-based
        // policy and reservation allowance is used for app-level and per-model eligibility.
        return when {
            !is64Bit -> {
                HardwareEligibility.Ineligible(
                    reason = "This app requires a modern 64-bit processor (arm64-v8a).",
                    detectedRamGb = detectedGb,
                    is64BitSupported = false,
                )
            }

            totalBytes < minRequiredBytes -> {
                HardwareEligibility.Ineligible(
                    reason = "On-device AI models require at least 8 GB RAM for stable execution.",
                    detectedRamGb = detectedGb,
                    minRequiredRamGb = minRequiredRamGb,
                    is64BitSupported = true,
                )
            }

            else -> {
                HardwareEligibility.Eligible
            }
        }
    }
}
