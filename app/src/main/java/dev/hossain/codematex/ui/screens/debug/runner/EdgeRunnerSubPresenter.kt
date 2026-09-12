package dev.hossain.codematex.ui.screens.debug.runner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.subcircuit.SubPresenter
import com.slack.circuit.subcircuit.SubPresenterFactory
import com.slack.circuit.subcircuit.SubScreen
import dev.hossain.codematex.domain.runner.PlaygroundCodeRunner
import dev.hossain.codematex.domain.runner.PlaygroundExecutionResult
import dev.hossain.codematex.system.NetworkMonitor
import dev.hossain.codematex.ui.screens.debug.DEFAULT_RUNNER_LANGUAGE
import dev.hossain.codematex.ui.screens.debug.DEFAULT_RUNNER_SNIPPETS
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

class EdgeRunnerSubPresenter(
    private val codeRunner: PlaygroundCodeRunner,
    private val networkMonitor: NetworkMonitor,
) : SubPresenter<EdgeRunnerOuterEvent, EdgeRunnerSubState> {
    @Composable
    override fun present(outerEventSink: (EdgeRunnerOuterEvent) -> Unit): EdgeRunnerSubState {
        val scope = rememberCoroutineScope()

        var selectedLanguage by rememberRetained { mutableStateOf(DEFAULT_RUNNER_LANGUAGE) }
        var snippetCode by rememberRetained {
            mutableStateOf(DEFAULT_RUNNER_SNIPPETS[DEFAULT_RUNNER_LANGUAGE] ?: "")
        }
        var isRunningSnippet by rememberRetained { mutableStateOf(false) }
        var runnerResult by rememberRetained { mutableStateOf<PlaygroundExecutionResult?>(null) }
        var runnerDurationMs by rememberRetained { mutableStateOf<Long?>(null) }
        var isPingingProxy by rememberRetained { mutableStateOf(false) }
        var proxyPingMs by rememberRetained { mutableStateOf<Long?>(null) }
        var proxyPingError by rememberRetained { mutableStateOf<String?>(null) }

        val isOnline by
            produceState(initialValue = true) {
                networkMonitor.isOnline.collect { value = it }
            }

        return EdgeRunnerSubState(
            selectedLanguage = selectedLanguage,
            snippetCode = snippetCode,
            isRunningSnippet = isRunningSnippet,
            runnerResult = runnerResult,
            runnerDurationMs = runnerDurationMs,
            isPingingProxy = isPingingProxy,
            proxyPingMs = proxyPingMs,
            proxyPingError = proxyPingError,
            isOnline = isOnline,
            eventSink = { event ->
                when (event) {
                    is EdgeRunnerUiEvent.SelectLanguage -> {
                        selectedLanguage = event.language
                        snippetCode = DEFAULT_RUNNER_SNIPPETS[event.language] ?: ""
                        runnerResult = null
                        runnerDurationMs = null
                    }

                    is EdgeRunnerUiEvent.UpdateSnippet -> {
                        snippetCode = event.code
                    }

                    EdgeRunnerUiEvent.ResetSnippet -> {
                        snippetCode = DEFAULT_RUNNER_SNIPPETS[selectedLanguage] ?: ""
                        runnerResult = null
                        runnerDurationMs = null
                    }

                    EdgeRunnerUiEvent.RunSnippet -> {
                        if (!isRunningSnippet && isOnline) {
                            scope.launch {
                                isRunningSnippet = true
                                runnerResult = null
                                val start = System.currentTimeMillis()
                                try {
                                    val result = codeRunner.runSnippet(snippetCode, selectedLanguage)
                                    val duration = System.currentTimeMillis() - start
                                    runnerDurationMs = duration
                                    runnerResult = result
                                    outerEventSink(
                                        EdgeRunnerOuterEvent.ShowSnackbar(
                                            "Executed ${selectedLanguage.replaceFirstChar {
                                                it.uppercase()
                                            }} snippet via Edge Runner (${duration}ms).",
                                        ),
                                    )
                                } catch (e: Exception) {
                                    val duration = System.currentTimeMillis() - start
                                    runnerDurationMs = duration
                                    val err = PlaygroundExecutionResult.NetworkError(e.message ?: "Execution failed")
                                    runnerResult = err
                                    outerEventSink(
                                        EdgeRunnerOuterEvent.ShowSnackbar("Runner error: ${e.message}"),
                                    )
                                    Timber.e(e, "EdgeRunnerSubPresenter [RUNNER_ERROR]: Execution failed")
                                } finally {
                                    isRunningSnippet = false
                                }
                            }
                        }
                    }

                    EdgeRunnerUiEvent.PingProxy -> {
                        if (!isPingingProxy && isOnline) {
                            scope.launch {
                                isPingingProxy = true
                                proxyPingError = null
                                val start = System.currentTimeMillis()
                                try {
                                    val pingResult = codeRunner.runSnippet("println(\"ping\")", "kotlin")
                                    val duration = System.currentTimeMillis() - start
                                    when (pingResult) {
                                        is PlaygroundExecutionResult.Success, is PlaygroundExecutionResult.CompilationError -> {
                                            proxyPingMs = duration
                                            outerEventSink(
                                                EdgeRunnerOuterEvent.ShowSnackbar(
                                                    "Proxy reachable! Roundtrip latency: ${duration}ms.",
                                                ),
                                            )
                                        }

                                        is PlaygroundExecutionResult.NetworkError -> {
                                            proxyPingError = pingResult.message
                                            outerEventSink(
                                                EdgeRunnerOuterEvent.ShowSnackbar(
                                                    "Proxy unreachable: ${pingResult.message}",
                                                ),
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    proxyPingError = e.message
                                    outerEventSink(
                                        EdgeRunnerOuterEvent.ShowSnackbar("Proxy ping failed: ${e.message}"),
                                    )
                                } finally {
                                    isPingingProxy = false
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class EdgeRunnerSubPresenterFactory(
    private val codeRunner: PlaygroundCodeRunner,
    private val networkMonitor: NetworkMonitor,
) : SubPresenterFactory {
    override fun create(screen: SubScreen<*>): SubPresenter<*, *>? =
        when (screen) {
            is EdgeRunnerSubScreen -> EdgeRunnerSubPresenter(codeRunner, networkMonitor)
            else -> null
        }
}
