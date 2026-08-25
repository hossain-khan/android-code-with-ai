package dev.hossain.codematex.system

import android.content.Context
import dev.hossain.codematex.di.ApplicationContext
import dev.hossain.codematex.util.DeviceMemory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import javax.inject.Inject

/**
 * Abstraction over device memory and process CPU stats.
 *
 * This interface removes the need for presenters to hold an Android [Context]
 * just to read memory statistics, making them fully testable on the JVM with
 * in-memory fakes.
 */
interface DeviceMemoryProvider {
    /**
     * Returns current memory statistics (used and total RAM).
     */
    fun getMemoryStats(): DeviceMemory.MemoryStats

    /**
     * Returns the total device RAM in bytes. This is the authoritative value for compatibility
     * decisions because it avoids GB/GiB truncation disagreements.
     */
    fun getTotalMemoryBytes(): Long

    /**
     * Returns the current process CPU tick count.
     */
    fun getProcessCpuTicks(): Long
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DeviceMemoryProviderImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val memoryStatsProvider: (Context) -> DeviceMemory.MemoryStats = DeviceMemory::getMemoryStats,
        private val totalMemoryBytesProvider: (Context) -> Long = DeviceMemory::getTotalMemoryBytes,
        private val cpuTicksProvider: () -> Long = DeviceMemory::getProcessCpuTicks,
    ) : DeviceMemoryProvider {
        override fun getMemoryStats(): DeviceMemory.MemoryStats = memoryStatsProvider(context)

        override fun getTotalMemoryBytes(): Long = totalMemoryBytesProvider(context)

        override fun getProcessCpuTicks(): Long = cpuTicksProvider()
    }
