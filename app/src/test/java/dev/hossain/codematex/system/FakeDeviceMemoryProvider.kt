package dev.hossain.codematex.system

import dev.hossain.codematex.util.DeviceMemory

class FakeDeviceMemoryProvider : DeviceMemoryProvider {
    var returnedMemoryStats = DeviceMemory.MemoryStats(usedGb = 4.0f, totalGb = 8.0f)
    var returnedProcessCpuTicks = 1234L

    override fun getMemoryStats(): DeviceMemory.MemoryStats = returnedMemoryStats

    override fun getProcessCpuTicks(): Long = returnedProcessCpuTicks
}
