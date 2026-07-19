package dev.hossain.codematex.util

import android.app.ActivityManager
import android.content.Context

object DeviceMemory {
    fun getDeviceRamGb(context: Context): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return (memoryInfo.totalMem / (1024 * 1024 * 1024)).toInt()
    }

    fun isModelCompatible(
        modelMinRamGb: Int,
        deviceRamGb: Int,
    ): Boolean = modelMinRamGb == 0 || deviceRamGb >= modelMinRamGb

    data class MemoryStats(
        val usedGb: Float,
        val totalGb: Float,
    )

    fun getMemoryStats(context: Context): MemoryStats {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalGb = memoryInfo.totalMem / 1_000_000_000f
        val availGb = memoryInfo.availMem / 1_000_000_000f
        val usedGb = totalGb - availGb
        return MemoryStats(usedGb, totalGb)
    }

    fun getProcessCpuTicks(): Long =
        try {
            val stat =
                java.io
                    .File("/proc/self/stat")
                    .readText()
                    .split(" ")
            val utime = stat[13].toLong()
            val stime = stat[14].toLong()
            utime + stime
        } catch (e: Exception) {
            0L
        }
}
