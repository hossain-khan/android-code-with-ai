package dev.hossain.codematex.ui.screens.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.hossain.codematex.runtime.LlmEngine
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

/**
 * Circuit Presenter for [DebugScreen], serving as a lightweight host that coordinates
 * modular CircuitX SubCircuits (telemetry, hardware diagnostics, edge runner,
 * database diagnostics, model lifecycle, and benchmark).
 *
 * @param navigator Circuit navigator for screen transitions.
 * @param screen Screen argument representation for [DebugScreen].
 * @param llmEngine High-level on-device LiteRT-LM runtime engine.
 */
@AssistedInject
class DebugPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: DebugScreen,
    private val llmEngine: LlmEngine,
) : Presenter<DebugScreen.State> {
    /**
     * Assisted injection factory for [DebugPresenter].
     */
    @CircuitInject(DebugScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(
            navigator: Navigator,
            screen: DebugScreen,
        ): DebugPresenter
    }

    private data class ModelState(
        val isLoaded: Boolean = false,
        val modelName: String? = null,
    )

    @Composable
    override fun present(): DebugScreen.State {
        var modelState by rememberRetained {
            mutableStateOf(
                ModelState(
                    isLoaded = llmEngine.getActiveBackend() != null,
                    modelName = if (llmEngine.getActiveBackend() != null) "Active Model" else null,
                ),
            )
        }
        var statusMessage by rememberRetained { mutableStateOf<String?>("Debugger ready.") }

        return DebugScreen.State.Success(
            isModelLoaded = modelState.isLoaded,
            loadedModelName = modelState.modelName,
            statusMessage = statusMessage,
            eventSink = { event ->
                when (event) {
                    DebugScreen.Event.Back -> {
                        navigator.pop()
                    }

                    is DebugScreen.Event.ShowSnackbar -> {
                        statusMessage = event.message
                    }

                    DebugScreen.Event.ClearStatusMessage -> {
                        statusMessage = null
                    }

                    is DebugScreen.Event.ModelLoaded -> {
                        modelState = ModelState(isLoaded = true, modelName = event.modelName)
                    }

                    DebugScreen.Event.ModelUnloaded -> {
                        modelState = ModelState(isLoaded = false, modelName = null)
                    }
                }
            },
        )
    }
}
