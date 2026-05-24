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
}
