package dev.hossain.codematex.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceMemoryTest {
    @Test
    fun isModelCompatible_whenModelHasNoRequirement_isAlwaysCompatible() {
        assertTrue(DeviceMemory.isModelCompatible(modelMinRamGb = 0, deviceRamGb = 0))
        assertTrue(DeviceMemory.isModelCompatible(modelMinRamGb = 0, deviceRamGb = 4))
    }

    @Test
    fun isModelCompatible_whenDeviceMeetsRequirement_isCompatible() {
        assertTrue(DeviceMemory.isModelCompatible(modelMinRamGb = 8, deviceRamGb = 8))
        assertTrue(DeviceMemory.isModelCompatible(modelMinRamGb = 8, deviceRamGb = 12))
    }

    @Test
    fun isModelCompatible_whenDeviceBelowRequirement_isNotCompatible() {
        assertFalse(DeviceMemory.isModelCompatible(modelMinRamGb = 8, deviceRamGb = 7))
        assertFalse(DeviceMemory.isModelCompatible(modelMinRamGb = 12, deviceRamGb = 8))
    }

    @Test
    fun getProcessCpuTicks_neverReturnsNegativeValue() {
        // On platforms without /proc (e.g. macOS dev machines) this falls back to 0.
        assertTrue(DeviceMemory.getProcessCpuTicks() >= 0L)
    }
}
