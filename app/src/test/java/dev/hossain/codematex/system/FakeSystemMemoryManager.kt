package dev.hossain.codematex.system

import android.app.Application

class FakeSystemMemoryManager(
    var headroomResult: MemoryHeadroomResult = MemoryHeadroomResult.Sufficient,
) : SystemMemoryManager {
    var checkHeadroomCalls = 0
    var trimMemoryCalls = 0
    var lowMemoryCalls = 0
    var lastTrimLevel: Int? = null
    var registered = false

    override fun checkMemoryHeadroom(requiredHeadroomBytes: Long): MemoryHeadroomResult {
        checkHeadroomCalls++
        return headroomResult
    }

    override fun onTrimMemory(level: Int) {
        trimMemoryCalls++
        lastTrimLevel = level
    }

    override fun onLowMemory() {
        lowMemoryCalls++
    }

    override fun register(application: Application) {
        registered = true
    }

    override fun checkHistoricalExitReasons() {}
}
