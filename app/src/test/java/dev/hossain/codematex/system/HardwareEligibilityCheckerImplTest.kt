package dev.hossain.codematex.system

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HardwareEligibilityCheckerImplTest {
    private val fakeMemoryProvider = FakeDeviceMemoryProvider()

    @Test
    fun `given dev mode - returns eligible regardless of hardware`() {
        fakeMemoryProvider.returnedTotalBytes = 2L * MemoryCompatibilityPolicy.BYTES_PER_GB
        val checker =
            HardwareEligibilityCheckerImpl(
                deviceMemoryProvider = fakeMemoryProvider,
                is64BitSupported = { false },
                isDevMode = { true },
            )

        val result = checker.checkEligibility()

        assertThat(result).isEqualTo(HardwareEligibility.Eligible)
    }

    @Test
    fun `given 64-bit and enough ram - returns eligible`() {
        fakeMemoryProvider.returnedTotalBytes = 8L * MemoryCompatibilityPolicy.BYTES_PER_GB
        val checker =
            HardwareEligibilityCheckerImpl(
                deviceMemoryProvider = fakeMemoryProvider,
                is64BitSupported = { true },
                isDevMode = { false },
            )

        val result = checker.checkEligibility()

        assertThat(result).isEqualTo(HardwareEligibility.Eligible)
    }

    @Test
    fun `given not 64-bit - returns ineligible with architecture reason`() {
        fakeMemoryProvider.returnedTotalBytes = 8L * MemoryCompatibilityPolicy.BYTES_PER_GB
        val checker =
            HardwareEligibilityCheckerImpl(
                deviceMemoryProvider = fakeMemoryProvider,
                is64BitSupported = { false },
                isDevMode = { false },
            )

        val result = checker.checkEligibility() as HardwareEligibility.Ineligible

        assertThat(result.reason).contains("64-bit")
        assertThat(result.is64BitSupported).isFalse()
        assertThat(result.detectedRamGb).isWithin(0.01).of(8.0)
    }

    @Test
    fun `given 64-bit but low ram - returns ineligible with ram reason`() {
        fakeMemoryProvider.returnedTotalBytes = 6L * MemoryCompatibilityPolicy.BYTES_PER_GB
        val checker =
            HardwareEligibilityCheckerImpl(
                deviceMemoryProvider = fakeMemoryProvider,
                is64BitSupported = { true },
                isDevMode = { false },
            )

        val result = checker.checkEligibility() as HardwareEligibility.Ineligible

        assertThat(result.reason).contains("RAM")
        assertThat(result.is64BitSupported).isTrue()
        assertThat(result.detectedRamGb).isWithin(0.01).of(6.0)
        assertThat(result.minRequiredRamGb).isWithin(0.01).of(7.2)
    }

    @Test
    fun `given ram exactly at threshold - returns eligible`() {
        fakeMemoryProvider.returnedTotalBytes = MemoryCompatibilityPolicy.minimumRequiredBytes(8)
        val checker =
            HardwareEligibilityCheckerImpl(
                deviceMemoryProvider = fakeMemoryProvider,
                is64BitSupported = { true },
                isDevMode = { false },
            )

        val result = checker.checkEligibility()

        assertThat(result).isEqualTo(HardwareEligibility.Eligible)
    }

    @Test
    fun `given ram just below threshold - returns ineligible`() {
        fakeMemoryProvider.returnedTotalBytes = MemoryCompatibilityPolicy.minimumRequiredBytes(8) - 1
        val checker =
            HardwareEligibilityCheckerImpl(
                deviceMemoryProvider = fakeMemoryProvider,
                is64BitSupported = { true },
                isDevMode = { false },
            )

        val result = checker.checkEligibility()

        assertThat(result).isInstanceOf(HardwareEligibility.Ineligible::class.java)
    }
}
