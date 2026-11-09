package dev.hossain.codematex.ui.screens.lessons.snippet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.subcircuit.SubCircuitInject
import com.slack.circuit.subcircuit.SubPresenter
import dev.hossain.codematex.domain.runner.PlaygroundCodeRunner
import dev.hossain.codematex.domain.runner.PlaygroundExecutionResult
import dev.hossain.codematex.system.NetworkMonitor
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch

/**
 * SubPresenter that manages local compilation state and remote playground execution
 * for a single interactive code snippet.
 */
@AssistedInject
class InteractiveSnippetSubPresenter(
    @Assisted private val screen: InteractiveSnippetSubScreen,
    private val playgroundCodeRunner: PlaygroundCodeRunner,
    private val networkMonitor: NetworkMonitor,
) : SubPresenter<InteractiveSnippetOuterEvent, InteractiveSnippetSubState> {
    @Composable
    override fun present(outerEventSink: (InteractiveSnippetOuterEvent) -> Unit): InteractiveSnippetSubState {
        var executionState by rememberRetained { mutableStateOf<SnippetExecutionState>(SnippetExecutionState.Idle) }
        val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
        val scope = rememberCoroutineScope()

        return InteractiveSnippetSubState(
            code = screen.code,
            language = screen.language,
            topic = screen.topic,
            isOnline = isOnline,
            executionState = executionState,
            eventSink = { event ->
                when (event) {
                    InteractiveSnippetUiEvent.RunSnippet -> {
                        executionState = SnippetExecutionState.Compiling
                        scope.launch {
                            val result = playgroundCodeRunner.runSnippet(screen.code, screen.language)
                            val state =
                                when (result) {
                                    is PlaygroundExecutionResult.Success -> {
                                        SnippetExecutionState.Success(result.output)
                                    }

                                    is PlaygroundExecutionResult.CompilationError -> {
                                        SnippetExecutionState.CompilationError(result.diagnostic)
                                    }

                                    is PlaygroundExecutionResult.NetworkError -> {
                                        SnippetExecutionState.Error(result.message)
                                    }
                                }
                            executionState = state
                            outerEventSink(
                                InteractiveSnippetOuterEvent.ExecutionFinished(
                                    snippetId = screen.snippetId,
                                    isSuccess = result is PlaygroundExecutionResult.Success,
                                ),
                            )
                        }
                    }

                    InteractiveSnippetUiEvent.DismissOutput -> {
                        executionState = SnippetExecutionState.Idle
                    }
                }
            },
        )
    }

    @SubCircuitInject(InteractiveSnippetSubScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(screen: InteractiveSnippetSubScreen): InteractiveSnippetSubPresenter
    }
}
