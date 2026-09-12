package dev.hossain.codematex.ui.screens.debug.hardware

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.system.FakeDeviceMemoryProvider
import dev.hossain.codematex.system.HardwareEligibility
import dev.hossain.codematex.system.HardwareEligibilityChecker
import dev.hossain.codematex.ui.screens.debug.FakeDebugMemoryProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HardwareDiagnosticsSubPresenterTest {
    private val fakeMemoryProvider = FakeDebugMemoryProvider()

    private val eligibleChecker =
        object : HardwareEligibilityChecker {
            override fun checkEligibility(): HardwareEligibility = HardwareEligibility.Eligible
        }

    private val ineligibleChecker =
        object : HardwareEligibilityChecker {
            override fun checkEligibility(): HardwareEligibility =
                HardwareEligibility.Ineligible(
                    reason = "Requires at least 8GB RAM",
                    detectedRamGb = 4.0,
                    is64BitSupported = true,
                )
        }

    private val fakeDeviceMemoryProvider =
        FakeDeviceMemoryProvider().apply {
            returnedTotalBytes = 8L * 1024 * 1024 * 1024
        }

    @Test
    fun `presenter emits eligible state when hardware passes baseline`() =
        runTest {
            val presenter =
                HardwareDiagnosticsSubPresenter(
                    screen = HardwareDiagnosticsSubScreen(activeBackendName = "GPU"),
                    hardwareEligibilityChecker = eligibleChecker,
                    deviceMemoryProvider = fakeDeviceMemoryProvider,
                    debugMemoryProvider = fakeMemoryProvider,
                    isDevMode = { false },
                )

            presenter.test {
                val state = awaitItem()
                assertThat(state.eligibility).isEqualTo(HardwareEligibility.Eligible)
                assertThat(state.isDevMode).isFalse()
                assertThat(state.runtimeSpecs["Active Backend"]).isEqualTo("GPU")
                assertThat(state.deviceInfo).isNotEmpty()
            }
        }

    @Test
    fun `presenter reflects dev mode active in runtime specs`() =
        runTest {
            val presenter =
                HardwareDiagnosticsSubPresenter(
                    screen = HardwareDiagnosticsSubScreen(activeBackendName = null),
                    hardwareEligibilityChecker = ineligibleChecker,
                    deviceMemoryProvider = fakeDeviceMemoryProvider,
                    debugMemoryProvider = fakeMemoryProvider,
                    isDevMode = { true },
                )

            presenter.test {
                val state = awaitItem()
                assertThat(state.eligibility).isInstanceOf(HardwareEligibility.Ineligible::class.java)
                assertThat(state.isDevMode).isTrue()
                assertThat(state.runtimeSpecs["Dev Mode Bypass"]).contains("Active")
                assertThat(state.runtimeSpecs["Active Backend"]).isEqualTo("Idle / Unloaded")
            }
        }
}
