package dev.hossain.codematex.util

import android.app.ActivityManager
import android.content.Context
import java.io.File

object DeviceMemory {
    fun getTotalMemoryBytes(context: Context): Long =
        getTotalMemoryBytes { memoryInfo ->
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
        }

    fun getTotalMemoryBytes(getMemoryInfo: (ActivityManager.MemoryInfo) -> Unit): Long {
        val memoryInfo = ActivityManager.MemoryInfo()
        getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem
    }

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
        val bytesPerGb =
            dev.hossain.codematex.system.MemoryCompatibilityPolicy.BYTES_PER_GB
                .toFloat()
        val totalGb = memoryInfo.totalMem / bytesPerGb
        val availGb = memoryInfo.availMem / bytesPerGb
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
