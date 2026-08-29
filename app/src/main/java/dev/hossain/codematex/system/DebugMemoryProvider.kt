package dev.hossain.codematex.system

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import androidx.compose.runtime.Immutable
import dev.hossain.codematex.di.ApplicationContext
import dev.hossain.codematex.util.DeviceMemory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * Granular memory statistics for low-level LLM runtime debugging and memory profiling.
 */
@Immutable
@Serializable
data class DebugMemoryStats(
    val nativeAllocatedMb: Float = 0f,
    val nativeTotalMb: Float = 0f,
    val nativeFreeMb: Float = 0f,
    val jvmUsedMb: Float = 0f,
    val jvmTotalMb: Float = 0f,
    val jvmMaxMb: Float = 0f,
    val ramUsedGb: Float = 0f,
    val ramTotalGb: Float = 0f,
    val ramAvailGb: Float = 0f,
    val isLowMemory: Boolean = false,
    val cpuPercent: Float = 0f,
)

/**
 * Snapshot of memory allocations captured before/after an LLM operation.
 */
@Immutable
@Serializable
data class MemorySnapshot(
    val nativeAllocatedBytes: Long,
    val jvmUsedBytes: Long,
    val systemAvailBytes: Long,
    val timestampMs: Long = System.currentTimeMillis(),
) {
    /**
     * Calculates the memory difference relative to [previous].
     * Positive values indicate memory expansion (growth); negative values indicate memory reclaimed (freed).
     */
    fun diffFrom(previous: MemorySnapshot): MemoryDelta {
        val bytesToMb = 1024f * 1024f
        val deltaNativeMb = (nativeAllocatedBytes - previous.nativeAllocatedBytes) / bytesToMb
        val deltaJvmMb = (jvmUsedBytes - previous.jvmUsedBytes) / bytesToMb
        val deltaSystemMb = (previous.systemAvailBytes - systemAvailBytes) / bytesToMb
        val durationMs = (timestampMs - previous.timestampMs).coerceAtLeast(0L)
        return MemoryDelta(
            deltaNativeMb = deltaNativeMb,
            deltaJvmMb = deltaJvmMb,
            deltaSystemMb = deltaSystemMb,
            durationMs = durationMs,
        )
    }
}

/**
 * Memory difference metrics resulting from a load, unload, or inference operation.
 */
@Immutable
@Serializable
data class MemoryDelta(
    val deltaNativeMb: Float = 0f,
    val deltaJvmMb: Float = 0f,
    val deltaSystemMb: Float = 0f,
    val durationMs: Long = 0L,
)

/**
 * Abstraction for sampling low-level JVM, Native, and System memory allocations.
 */
interface DebugMemoryProvider {
    /**
     * Captures current instantaneous memory allocations.
     */
    fun getDebugMemoryStats(): DebugMemoryStats

    /**
     * Captures a lightweight snapshot used for delta calculations.
     */
    fun captureSnapshot(): MemorySnapshot

    /**
     * Requests explicit JVM Garbage Collection and returns reclaimed JVM bytes.
     */
    fun triggerGc(): Long
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DebugMemoryProviderImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : DebugMemoryProvider {
        private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

        override fun getDebugMemoryStats(): DebugMemoryStats {
            val bytesToMb = 1024f * 1024f
            val bytesToGb = 1024f * 1024f * 1024f

            val nativeAllocated = Debug.getNativeHeapAllocatedSize() / bytesToMb
            val nativeTotal = Debug.getNativeHeapSize() / bytesToMb
            val nativeFree = Debug.getNativeHeapFreeSize() / bytesToMb

            val runtime = Runtime.getRuntime()
            val jvmTotal = runtime.totalMemory() / bytesToMb
            val jvmFree = runtime.freeMemory() / bytesToMb
            val jvmUsed = (runtime.totalMemory() - runtime.freeMemory()) / bytesToMb
            val jvmMax = runtime.maxMemory() / bytesToMb

            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)

            val totalGb = memoryInfo.totalMem / bytesToGb
            val availGb = memoryInfo.availMem / bytesToGb
            val usedGb = (memoryInfo.totalMem - memoryInfo.availMem) / bytesToGb

            return DebugMemoryStats(
                nativeAllocatedMb = nativeAllocated,
                nativeTotalMb = nativeTotal,
                nativeFreeMb = nativeFree,
                jvmUsedMb = jvmUsed,
                jvmTotalMb = jvmTotal,
                jvmMaxMb = jvmMax,
                ramUsedGb = usedGb,
                ramTotalGb = totalGb,
                ramAvailGb = availGb,
                isLowMemory = memoryInfo.lowMemory,
                cpuPercent = 0f,
            )
        }

        override fun captureSnapshot(): MemorySnapshot {
            val runtime = Runtime.getRuntime()
            val jvmUsed = runtime.totalMemory() - runtime.freeMemory()
            val nativeAllocated = Debug.getNativeHeapAllocatedSize()
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)

            return MemorySnapshot(
                nativeAllocatedBytes = nativeAllocated,
                jvmUsedBytes = jvmUsed,
                systemAvailBytes = memoryInfo.availMem,
            )
        }

        override fun triggerGc(): Long {
            val runtime = Runtime.getRuntime()
            val beforeUsed = runtime.totalMemory() - runtime.freeMemory()
            System.gc()
            System.runFinalization()
            val afterUsed = runtime.totalMemory() - runtime.freeMemory()
            return (beforeUsed - afterUsed).coerceAtLeast(0L)
        }
    }
