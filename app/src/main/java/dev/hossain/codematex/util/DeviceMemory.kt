package dev.hossain.codematex.util

import android.app.ActivityManager
import android.content.Context
import java.io.File

object DeviceMemory {
    fun getDeviceRamGb(context: Context): Int =
        getDeviceRamGb { memoryInfo ->
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
        }

    fun getDeviceRamGb(getMemoryInfo: (ActivityManager.MemoryInfo) -> Unit): Int {
        val memoryInfo = ActivityManager.MemoryInfo()
        getMemoryInfo(memoryInfo)
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

    fun getMemoryStats(context: Context): MemoryStats =
        getMemoryStats { memoryInfo ->
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
        }

    fun getMemoryStats(getMemoryInfo: (ActivityManager.MemoryInfo) -> Unit): MemoryStats {
        val memoryInfo = ActivityManager.MemoryInfo()
        getMemoryInfo(memoryInfo)
        val totalGb = memoryInfo.totalMem / 1_000_000_000f
        val availGb = memoryInfo.availMem / 1_000_000_000f
        val usedGb = totalGb - availGb
        return MemoryStats(usedGb, totalGb)
    }

    fun getProcessCpuTicks(statFilePath: String = "/proc/self/stat"): Long =
        try {
            val stat =
                File(statFilePath)
                    .readText()
                    .trim()
                    .split(Regex("\\s+"))
            val utime = stat[13].toLong()
            val stime = stat[14].toLong()
            utime + stime
        } catch (e: Exception) {
            0L
        }
}
