package dev.hossain.codematex.system

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelCompatibilityCheckerImplTest {
    @Test
    fun `given dev mode - any model is compatible`() {
        val provider = FakeDeviceMemoryProvider().apply { returnedTotalBytes = 1L }
        val checker = ModelCompatibilityCheckerImpl(provider, isDevMode = { true })

        assertThat(checker.checkCompatibility(modelMinRamGb = 12)).isEqualTo(ModelCompatibility.Compatible)
    }

    @Test
    fun `given model has no requirement - returns compatible`() {
        val provider = FakeDeviceMemoryProvider().apply { returnedTotalBytes = 1L }
        val checker = ModelCompatibilityCheckerImpl(provider, isDevMode = { false })

        assertThat(checker.checkCompatibility(modelMinRamGb = 0)).isEqualTo(ModelCompatibility.Compatible)
    }

    @Test
    fun `given device meets 8GB requirement - returns compatible`() {
        val provider =
            FakeDeviceMemoryProvider().apply {
                returnedTotalBytes = MemoryCompatibilityPolicy.minimumRequiredBytes(8)
            }
        val checker = ModelCompatibilityCheckerImpl(provider, isDevMode = { false })

        assertThat(checker.checkCompatibility(modelMinRamGb = 8)).isEqualTo(ModelCompatibility.Compatible)
    }

    @Test
    fun `given device below 8GB requirement - returns incompatible with reason`() {
        val provider =
            FakeDeviceMemoryProvider().apply {
                returnedTotalBytes = MemoryCompatibilityPolicy.minimumRequiredBytes(8) - 1
            }
        val checker = ModelCompatibilityCheckerImpl(provider, isDevMode = { false })

        val result = checker.checkCompatibility(modelMinRamGb = 8) as ModelCompatibility.Incompatible

        assertThat(result.reason).contains("Requires 8GB RAM")
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

        assertThat(info.totalBytes).isEqualTo(totalBytes)
        assertThat(info.displayTotalGb).isWithin(0.001).of(8.5)
        assertThat(info.displayLabel).isEqualTo("GB")
    }
}
