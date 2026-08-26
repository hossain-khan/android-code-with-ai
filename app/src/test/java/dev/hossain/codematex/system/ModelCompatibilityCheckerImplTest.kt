package dev.hossain.codematex.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCompatibilityCheckerImplTest {
    @Test
    fun `given dev mode - any model is compatible`() {
        val provider = FakeDeviceMemoryProvider().apply { returnedTotalBytes = 1L }
        val checker = ModelCompatibilityCheckerImpl(provider, isDevMode = { true })

        assertEquals(ModelCompatibility.Compatible, checker.checkCompatibility(modelMinRamGb = 12))
    }

    @Test
    fun `given model has no requirement - returns compatible`() {
        val provider = FakeDeviceMemoryProvider().apply { returnedTotalBytes = 1L }
        val checker = ModelCompatibilityCheckerImpl(provider, isDevMode = { false })

        assertEquals(ModelCompatibility.Compatible, checker.checkCompatibility(modelMinRamGb = 0))
    }

    @Test
    fun `given device meets 8GB requirement - returns compatible`() {
        val provider =
            FakeDeviceMemoryProvider().apply {
                returnedTotalBytes = MemoryCompatibilityPolicy.minimumRequiredBytes(8)
            }
        val checker = ModelCompatibilityCheckerImpl(provider, isDevMode = { false })

        assertEquals(ModelCompatibility.Compatible, checker.checkCompatibility(modelMinRamGb = 8))
    }

    @Test
    fun `given device below 8GB requirement - returns incompatible with reason`() {
        val provider =
            FakeDeviceMemoryProvider().apply {
                returnedTotalBytes = MemoryCompatibilityPolicy.minimumRequiredBytes(8) - 1
            }
        val checker = ModelCompatibilityCheckerImpl(provider, isDevMode = { false })

        val result = checker.checkCompatibility(modelMinRamGb = 8) as ModelCompatibility.Incompatible

        assertTrue(result.reason.contains("Requires 8GB RAM"))
    }

    @Test
    fun `getDeviceMemoryInfo returns decimal gigabytes and GB label`() {
        val totalBytes = (8.5 * MemoryCompatibilityPolicy.BYTES_PER_GB).toLong()
        val provider =
            FakeDeviceMemoryProvider().apply {
                returnedTotalBytes = totalBytes
            }
        val checker = ModelCompatibilityCheckerImpl(provider, isDevMode = { false })

        val info = checker.getDeviceMemoryInfo()

        assertEquals(totalBytes, info.totalBytes)
        assertEquals(8.5, info.displayTotalGb, 0.001)
        assertEquals("GB", info.displayLabel)
    }
}
