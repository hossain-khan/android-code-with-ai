package dev.hossain.codematex.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryCompatibilityPolicyTest {
    @Test
    fun `minimum required bytes for 8GB model is 7_730_941_132`() {
        assertEquals(7_730_941_132L, MemoryCompatibilityPolicy.minimumRequiredBytes(8))
    }

    @Test
    fun `minimum required bytes for 12GB model is 11_596_411_699`() {
        assertEquals(11_596_411_699L, MemoryCompatibilityPolicy.minimumRequiredBytes(12))
    }

    @Test
    fun `typical 8GB marketed device with 7_4 GiB reported is compatible with 8GB model`() {
        // 7.4 GiB in bytes.
        val totalBytes = (7.4 * 1024 * 1024 * 1024).toLong()

        assertTrue(MemoryCompatibilityPolicy.isCompatible(modelMinRamGb = 8, totalMemoryBytes = totalBytes))
    }

    @Test
    fun `device just below 8GB threshold is incompatible with 8GB model`() {
        val totalBytes = MemoryCompatibilityPolicy.minimumRequiredBytes(8) - 1

        assertFalse(MemoryCompatibilityPolicy.isCompatible(modelMinRamGb = 8, totalMemoryBytes = totalBytes))
    }

    @Test
    fun `typical 12GB marketed device with 11_2 GiB reported is compatible with 12GB model`() {
        val totalBytes = (11.2 * 1024 * 1024 * 1024).toLong()

        assertTrue(MemoryCompatibilityPolicy.isCompatible(modelMinRamGb = 12, totalMemoryBytes = totalBytes))
    }

    @Test
    fun `device just below 12GB threshold is incompatible with 12GB model`() {
        val totalBytes = MemoryCompatibilityPolicy.minimumRequiredBytes(12) - 1

        assertFalse(MemoryCompatibilityPolicy.isCompatible(modelMinRamGb = 12, totalMemoryBytes = totalBytes))
    }

    @Test
    fun `model with no requirement is always compatible`() {
        assertTrue(MemoryCompatibilityPolicy.isCompatible(modelMinRamGb = 0, totalMemoryBytes = 0))
    }

    @Test
    fun `toDecimalGigabytes converts bytes correctly`() {
        val bytes = 8L * MemoryCompatibilityPolicy.BYTES_PER_GB
        assertEquals(8.0, MemoryCompatibilityPolicy.toDecimalGigabytes(bytes), 0.001)
    }
}
