package dev.hossain.codematex.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

        assertEquals(HardwareEligibility.Eligible, result)
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

        assertEquals(HardwareEligibility.Eligible, result)
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

        assertTrue(result.reason.contains("64-bit"))
        assertFalse(result.is64BitSupported)
        assertEquals(8.0, result.detectedRamGb, 0.01)
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

        assertTrue(result.reason.contains("RAM"))
        assertTrue(result.is64BitSupported)
        assertEquals(6.0, result.detectedRamGb, 0.01)
        assertEquals(7.2, result.minRequiredRamGb, 0.01)
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

        assertEquals(HardwareEligibility.Eligible, result)
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

        assertTrue(result is HardwareEligibility.Ineligible)
    }
}
