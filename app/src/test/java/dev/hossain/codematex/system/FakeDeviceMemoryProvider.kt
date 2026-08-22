package dev.hossain.codematex.system

import dev.hossain.codematex.util.DeviceMemory

/**
 * In-memory fake of [DeviceMemoryProvider] for unit tests.
 */
class FakeDeviceMemoryProvider(
    private val memoryStats: DeviceMemory.MemoryStats = DeviceMemory.MemoryStats(usedGb = 2.5f, totalGb = 8.0f),
    private val cpuTicks: Long = 1_000L,
) : DeviceMemoryProvider {
    override fun getMemoryStats(): DeviceMemory.MemoryStats = memoryStats

    override fun getProcessCpuTicks(): Long = cpuTicks
}
