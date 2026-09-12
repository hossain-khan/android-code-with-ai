package dev.hossain.codematex.ui.screens.debug

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.codematex.runtime.FakeLlmEngine
import dev.hossain.codematex.runtime.LlmEngine
import dev.hossain.codematex.system.DebugMemoryProvider
import dev.hossain.codematex.system.DebugMemoryStats
import dev.hossain.codematex.system.MemorySnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeDebugMemoryProvider(
    var stats: DebugMemoryStats =
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
            cpuPercent = 15f,
        ),
    var snapshot: MemorySnapshot =
        MemorySnapshot(
            nativeAllocatedBytes = 1_000_000_000L,
            jvmUsedBytes = 50_000_000L,
            systemAvailBytes = 4_000_000_000L,
        ),
) : DebugMemoryProvider {
    var triggerGcCalls = 0

    override fun getDebugMemoryStats(): DebugMemoryStats = stats

    override fun captureSnapshot(): MemorySnapshot = snapshot

    override fun triggerGc(): Long {
        triggerGcCalls++
        return 10_000_000L
    }
}

class DebugPresenterTest {
    @Test
    fun `initial state reflects engine load state when idle`() =
        runTest {
            val fakeEngine =
                FakeLlmEngine().apply {
                    activeBackendValue = null
                }
            val navigator = FakeNavigator(DebugScreen)
            val presenter =
                DebugPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    llmEngine = fakeEngine,
                )

            presenter.test {
                val state = awaitItem() as DebugScreen.State.Success
                assertThat(state.isModelLoaded).isFalse()
                assertThat(state.loadedModelName).isNull()
                assertThat(state.statusMessage).isEqualTo("Debugger ready.")
            }
        }

    @Test
    fun `initial state reflects engine load state when loaded`() =
        runTest {
            val fakeEngine =
                FakeLlmEngine().apply {
                    activeBackendValue = LlmEngine.Backend.GPU
                }
            val navigator = FakeNavigator(DebugScreen)
            val presenter =
                DebugPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    llmEngine = fakeEngine,
                )

            presenter.test {
                val state = awaitItem() as DebugScreen.State.Success
                assertThat(state.isModelLoaded).isTrue()
            }
        }

    @Test
    fun `navigates back when Back event is received`() =
        runTest {
            val fakeEngine = FakeLlmEngine()
            val navigator = FakeNavigator(DebugScreen)
            val presenter =
                DebugPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    llmEngine = fakeEngine,
                )

            presenter.test {
                val state = awaitItem() as DebugScreen.State.Success
                state.eventSink(DebugScreen.Event.Back)
                assertThat(navigator.awaitPop()).isNotNull()
            }
        }

    @Test
    fun `ShowSnackbar updates statusMessage and ClearStatusMessage clears it`() =
        runTest {
            val fakeEngine = FakeLlmEngine()
            val navigator = FakeNavigator(DebugScreen)
            val presenter =
                DebugPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    llmEngine = fakeEngine,
                )

            presenter.test {
                val state = awaitItem() as DebugScreen.State.Success

                state.eventSink(DebugScreen.Event.ShowSnackbar("Custom status notification"))
                val snackbarState = awaitItem() as DebugScreen.State.Success
                assertThat(snackbarState.statusMessage).isEqualTo("Custom status notification")

                snackbarState.eventSink(DebugScreen.Event.ClearStatusMessage)
                val clearedState = awaitItem() as DebugScreen.State.Success
                assertThat(clearedState.statusMessage).isNull()
            }
        }

    @Test
    fun `ModelLoaded and ModelUnloaded events synchronize state`() =
        runTest {
            val fakeEngine =
                FakeLlmEngine().apply {
                    activeBackendValue = null
                }
            val navigator = FakeNavigator(DebugScreen)
            val presenter =
                DebugPresenter(
                    navigator = navigator,
                    screen = DebugScreen,
                    llmEngine = fakeEngine,
                )

            presenter.test {
                val state = awaitItem() as DebugScreen.State.Success
                assertThat(state.isModelLoaded).isFalse()

                state.eventSink(DebugScreen.Event.ModelLoaded("Gemma 2 2B IT"))
                val loadedState = awaitItem() as DebugScreen.State.Success
                assertThat(loadedState.isModelLoaded).isTrue()
                assertThat(loadedState.loadedModelName).isEqualTo("Gemma 2 2B IT")

                loadedState.eventSink(DebugScreen.Event.ModelUnloaded)
                val unloadedState = awaitItem() as DebugScreen.State.Success
                assertThat(unloadedState.isModelLoaded).isFalse()
                assertThat(unloadedState.loadedModelName).isNull()
            }
        }
}
