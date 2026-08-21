package dev.hossain.codematex.system

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dev.hossain.codematex.BuildConfig
import dev.hossain.codematex.di.ApplicationContext
import dev.zacsweers.metro.AppScope
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
class HardwareEligibilityCheckerImpl(
    @ApplicationContext private val context: Context,
) : HardwareEligibilityChecker {
    override fun checkEligibility(): HardwareEligibility {
        // In DEV_MODE, bypass hardware restrictions so developers can test on emulators
        if (BuildConfig.DEV_MODE) {
            return HardwareEligibility.Eligible
        }

        val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalMem = memoryInfo.totalMem
        val detectedGb = totalMem / (1024.0 * 1024.0 * 1024.0)

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
        val minRequiredRamBytes = (7.2 * 1024 * 1024 * 1024).toLong()
        if (totalMem < minRequiredRamBytes) {
            return HardwareEligibility.Ineligible(
                reason = "On-device AI models require at least 8 GB RAM for stable execution.",
                detectedRamGb = detectedGb,
                is64BitSupported = true,
            )
        }

        return HardwareEligibility.Eligible
    }
}
