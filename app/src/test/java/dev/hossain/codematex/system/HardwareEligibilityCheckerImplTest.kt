package dev.hossain.codematex.system

import dev.hossain.codematex.util.DeviceMemory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareEligibilityCheckerImplTest {
    private val fakeMemoryProvider = FakeDeviceMemoryProvider()

    @Test
    fun `given dev mode - returns eligible regardless of hardware`() {
        fakeMemoryProvider.returnedMemoryStats = DeviceMemory.MemoryStats(usedGb = 0f, totalGb = 2f)
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
        fakeMemoryProvider.returnedMemoryStats = DeviceMemory.MemoryStats(usedGb = 4f, totalGb = 8f)
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
        fakeMemoryProvider.returnedMemoryStats = DeviceMemory.MemoryStats(usedGb = 4f, totalGb = 8f)
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
        fakeMemoryProvider.returnedMemoryStats = DeviceMemory.MemoryStats(usedGb = 1f, totalGb = 6f)
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
        fakeMemoryProvider.returnedMemoryStats = DeviceMemory.MemoryStats(usedGb = 0f, totalGb = 7.2f)
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
        fakeMemoryProvider.returnedMemoryStats = DeviceMemory.MemoryStats(usedGb = 0f, totalGb = 7.19f)
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
