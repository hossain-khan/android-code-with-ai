package dev.hossain.codematex.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DeviceMemory].
 */
class DeviceMemoryTest {
    @Test
    fun `given model has no ram requirement - is model compatible is always true`() {
        assertTrue(DeviceMemory.isModelCompatible(modelMinRamGb = 0, deviceRamGb = 0))
        assertTrue(DeviceMemory.isModelCompatible(modelMinRamGb = 0, deviceRamGb = 4))
    }

    @Test
    fun `given device meets ram requirement - is model compatible is true`() {
        assertTrue(DeviceMemory.isModelCompatible(modelMinRamGb = 8, deviceRamGb = 8))
        assertTrue(DeviceMemory.isModelCompatible(modelMinRamGb = 8, deviceRamGb = 12))
    }

    @Test
    fun `given device below ram requirement - is model compatible is false`() {
        assertFalse(DeviceMemory.isModelCompatible(modelMinRamGb = 8, deviceRamGb = 7))
        assertFalse(DeviceMemory.isModelCompatible(modelMinRamGb = 12, deviceRamGb = 8))
    }

    @Test
    fun `given any platform - get process cpu ticks never returns negative value`() {
        // On platforms without /proc (e.g. macOS dev machines) this falls back to 0.
        assertTrue(DeviceMemory.getProcessCpuTicks() >= 0L)
    }
}
