package dev.hossain.codematex.ui.screens.debug.telemetry

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.subcircuit.test.test
import dev.hossain.codematex.system.DebugMemoryStats
import dev.hossain.codematex.ui.screens.debug.FakeDebugMemoryProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TelemetrySubPresenterTest {
    @Test
    fun `presenter emits initial memory stats`() =
        runTest {
            val stats =
                DebugMemoryStats(
                    nativeAllocatedMb = 1200f,
                    nativeTotalMb = 2048f,
                    nativeFreeMb = 848f,
                    jvmUsedMb = 50f,
                    jvmTotalMb = 100f,
                    jvmMaxMb = 512f,
                    ramUsedGb = 4f,
                    ramTotalGb = 8f,
                    ramAvailGb = 4f,
                    isLowMemory = false,
                )
            val fakeMemoryProvider = FakeDebugMemoryProvider(stats = stats)
            val presenter = TelemetrySubPresenter(debugMemoryProvider = fakeMemoryProvider)

            presenter.test {
                val state = awaitItem()
                assertThat(state.stats).isEqualTo(stats)
            }
        }

    @Test
    fun `triggering GC emits outer event show snackbar`() =
        runTest {
            val fakeMemoryProvider = FakeDebugMemoryProvider()
            val presenter = TelemetrySubPresenter(debugMemoryProvider = fakeMemoryProvider)

            presenter.test {
                val state = awaitItem()
                state.eventSink(TelemetryUiEvent.TriggerGc)

                val outerEvent = outerEvents.awaitEvent()
                assertThat(outerEvent).isEqualTo(
                    TelemetryOuterEvent.ShowSnackbar("Explicit JVM garbage collection requested."),
                )
            }
        }
}
