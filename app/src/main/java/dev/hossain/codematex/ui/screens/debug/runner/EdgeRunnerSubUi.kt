package dev.hossain.codematex.ui.screens.debug.runner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.domain.runner.PlaygroundExecutionResult
import dev.hossain.codematex.ui.screens.debug.RUNNER_SUPPORTED_LANGUAGES
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

@Composable
fun EdgeRunnerSubUi(
    state: EdgeRunnerSubState,
    modifier: Modifier = Modifier,
) {
    EdgeRunnerDiagnosticsCard(
        state = state,
        modifier = modifier,
    )
}

@Composable
internal fun EdgeRunnerDiagnosticsCard(
    state: EdgeRunnerSubState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header Row with Title & Online Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Edge Code Runner & Sandbox",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color =
                        if (state.isOnline) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (state.isOnline) Icons.Default.Wifi else Icons.Default.Warning,
                            contentDescription = null,
                            tint =
                                if (state.isOnline) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = if (state.isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color =
                                if (state.isOnline) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                        )
                    }
                }
            }

            // Proxy Latency / Reachability Row
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloudflare Proxy Endpoint",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "code-playground.gohk.xyz",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.proxyPingMs != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = "${state.proxyPingMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        } else if (state.proxyPingError != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    text = "Failed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { state.eventSink(EdgeRunnerUiEvent.PingProxy) },
                            enabled = !state.isPingingProxy && state.isOnline,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                        ) {
                            if (state.isPingingProxy) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Ping", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Language Selector Chips
            Text(text = "Target Language:", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RUNNER_SUPPORTED_LANGUAGES.forEach { lang ->
                    FilterChip(
                        selected = state.selectedLanguage.equals(lang, ignoreCase = true),
                        onClick = { state.eventSink(EdgeRunnerUiEvent.SelectLanguage(lang)) },
                        label = { Text(lang.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            // Code Snippet Editor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Test Snippet (${state.selectedLanguage}):",
                    style = MaterialTheme.typography.labelMedium,
                )
                TextButton(
                    onClick = { state.eventSink(EdgeRunnerUiEvent.ResetSnippet) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                ) {
                    Text("Reset Snippet", style = MaterialTheme.typography.labelSmall)
                }
            }

            OutlinedTextField(
                value = state.snippetCode,
                onValueChange = { state.eventSink(EdgeRunnerUiEvent.UpdateSnippet(it)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 10,
                textStyle =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
            )

            // Run Button
            Button(
                onClick = { state.eventSink(EdgeRunnerUiEvent.RunSnippet) },
                enabled = !state.isRunningSnippet && state.isOnline,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isRunningSnippet) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executing at Edge...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run ${state.selectedLanguage.replaceFirstChar { it.uppercase() }} Snippet")
                }
            }

            // Execution Result / Terminal View
            state.runnerResult?.let { result ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val (badgeText, badgeBg, badgeFg) =
                                when (result) {
                                    is PlaygroundExecutionResult.Success -> {
                                        Triple(
                                            "Success",
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }

                                    is PlaygroundExecutionResult.CompilationError -> {
                                        Triple(
                                            "Compilation Error",
                                            MaterialTheme.colorScheme.errorContainer,
                                            MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }

                                    is PlaygroundExecutionResult.NetworkError -> {
                                        Triple(
                                            "Network Error",
                                            MaterialTheme.colorScheme.errorContainer,
                                            MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                }

                            Surface(shape = CircleShape, color = badgeBg) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }

                            state.runnerDurationMs?.let { ms ->
                                Text(
                                    text = "Roundtrip: ${ms}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }

                        val outputText =
                            when (result) {
                                is PlaygroundExecutionResult.Success -> result.output
                                is PlaygroundExecutionResult.CompilationError -> result.diagnostic
                                is PlaygroundExecutionResult.NetworkError -> result.message
                            }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = outputText.ifEmpty { "(No output produced)" },
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@ContributesIntoSet(AppScope::class)
@Inject
class EdgeRunnerSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is EdgeRunnerSubScreen -> {
                SubUi<EdgeRunnerSubState> { state, modifier ->
                    EdgeRunnerSubUi(state, modifier)
                }
            }

            else -> {
                null
            }
        }
}

@ThemePreviews
@Composable
private fun EdgeRunnerDiagnosticsCardPreview() {
    CodeWithAIAppTheme {
        Surface {
            EdgeRunnerSubUi(
                state =
                    EdgeRunnerSubState(
                        selectedLanguage = "kotlin",
                        snippetCode = "fun main() {\n    println(\"Hello from CodeMateX!\")\n}",
                        isRunningSnippet = false,
                        runnerResult = PlaygroundExecutionResult.Success("Hello from CodeMateX!\n"),
                        runnerDurationMs = 245L,
                        proxyPingMs = 82L,
                        isOnline = true,
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
