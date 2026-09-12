package dev.hossain.codematex.ui.screens.debug.telemetry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.system.DebugMemoryProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class TelemetrySubPresenter(
    private val debugMemoryProvider: DebugMemoryProvider,
) : SubPresenter<TelemetryOuterEvent, TelemetrySubState> {
    @Composable
    override fun present(outerEventSink: (TelemetryOuterEvent) -> Unit): TelemetrySubState {
        val telemetryStats by
            produceState(initialValue = debugMemoryProvider.getDebugMemoryStats()) {
                while (isActive) {
                    delay(750.milliseconds)
                    value = debugMemoryProvider.getDebugMemoryStats()
                }
            }

        return TelemetrySubState(
            stats = telemetryStats,
            eventSink = { event ->
                when (event) {
                    TelemetryUiEvent.TriggerGc -> {
                        System.gc()
                        Runtime.getRuntime().gc()
                        Timber.i("Manual GC triggered from TelemetrySubPresenter.")
                        outerEventSink(TelemetryOuterEvent.ShowSnackbar("Explicit JVM garbage collection requested."))
                    }
                }
            },
        )
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class TelemetrySubPresenterFactory(
    private val debugMemoryProvider: DebugMemoryProvider,
) : SubPresenterFactory {
    override fun create(screen: SubScreen<*>): SubPresenter<*, *>? =
        when (screen) {
            is TelemetrySubScreen -> TelemetrySubPresenter(debugMemoryProvider)
            else -> null
        }
}
