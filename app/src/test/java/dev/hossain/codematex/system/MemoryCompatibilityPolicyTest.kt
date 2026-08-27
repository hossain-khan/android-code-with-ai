package dev.hossain.codematex.system

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MemoryCompatibilityPolicyTest {
    @Test
    fun `minimum required bytes for 8GB model is 7_730_941_132`() {
        assertThat(MemoryCompatibilityPolicy.minimumRequiredBytes(8)).isEqualTo(7_730_941_132L)
    }

    @Test
    fun `minimum required bytes for 12GB model is 11_596_411_699`() {
        assertThat(MemoryCompatibilityPolicy.minimumRequiredBytes(12)).isEqualTo(11_596_411_699L)
    }

    @Test
    fun `typical 8GB marketed device with 7_4 GiB reported is compatible with 8GB model`() {
        // 7.4 GiB in bytes.
        val totalBytes = (7.4 * 1024 * 1024 * 1024).toLong()

        assertThat(MemoryCompatibilityPolicy.isCompatible(modelMinRamGb = 8, totalMemoryBytes = totalBytes)).isTrue()
    }

    @Test
    fun `device just below 8GB threshold is incompatible with 8GB model`() {
        val totalBytes = MemoryCompatibilityPolicy.minimumRequiredBytes(8) - 1

        assertThat(MemoryCompatibilityPolicy.isCompatible(modelMinRamGb = 8, totalMemoryBytes = totalBytes)).isFalse()
    }

    @Test
    fun `typical 12GB marketed device with 11_2 GiB reported is compatible with 12GB model`() {
        val totalBytes = (11.2 * 1024 * 1024 * 1024).toLong()

        assertThat(MemoryCompatibilityPolicy.isCompatible(modelMinRamGb = 12, totalMemoryBytes = totalBytes)).isTrue()
    }

    @Test
    fun `device just below 12GB threshold is incompatible with 12GB model`() {
        val totalBytes = MemoryCompatibilityPolicy.minimumRequiredBytes(12) - 1

        assertThat(MemoryCompatibilityPolicy.isCompatible(modelMinRamGb = 12, totalMemoryBytes = totalBytes)).isFalse()
    }

    @Test
    fun `model with no requirement is always compatible`() {
        assertThat(MemoryCompatibilityPolicy.isCompatible(modelMinRamGb = 0, totalMemoryBytes = 0)).isTrue()
    }

    @Test
    fun `toDecimalGigabytes converts bytes correctly`() {
        val bytes = 8L * MemoryCompatibilityPolicy.BYTES_PER_GB
        assertThat(MemoryCompatibilityPolicy.toDecimalGigabytes(bytes)).isWithin(0.001).of(8.0)
    }
}
