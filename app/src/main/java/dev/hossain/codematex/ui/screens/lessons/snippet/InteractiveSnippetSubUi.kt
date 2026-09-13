package dev.hossain.codematex.ui.screens.lessons.snippet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slack.circuit.subcircuit.SubScreen
import com.slack.circuit.subcircuit.SubUi
import com.slack.circuit.subcircuit.SubUiFactory
import dev.hossain.codematex.data.model.CodeBlockPreset
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.component.LocalCodeBlockSettings
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.TopicVisualInfo
import dev.hossain.codematex.ui.theme.visualInfo
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/**
 * Composable rendering an interactive, runnable code snippet with integrated playground execution controls.
 */
@OptIn(ExperimentalHighlightApi::class)
@Composable
fun InteractiveSnippetSubUi(
    state: InteractiveSnippetSubState,
    modifier: Modifier = Modifier,
) {
    val visualInfo = state.topic.visualInfo
    val resolvedLanguage = state.language.ifEmpty { "text" }
    val settings = LocalCodeBlockSettings.current
    val baseStyle =
        if (settings.preset == CodeBlockPreset.COMPACT) {
            CodeBlockStyle.Compact
        } else {
            CodeBlockStyle.Default
        }
    val effectiveStyle =
        remember(baseStyle, settings.fontSize) {
            baseStyle.copy(
                textStyle =
                    baseStyle.textStyle.copy(
                        fontSize = settings.fontSize.sizeSp.sp,
                        lineHeight = (settings.fontSize.sizeSp * 1.35f).sp,
                    ),
            )
        }

    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SyntaxHighlightedCode(
                code = state.code,
                language = resolvedLanguage,
                showLineNumbers = settings.showLineNumbers,
                style = effectiveStyle,
                languageLabel =
                    if (settings.showLanguageLabel && resolvedLanguage.isNotBlank()) {
                        { SyntaxHighlightedCodeDefaults.LanguageLabel(resolvedLanguage) }
                    } else {
                        null
                    },
                copyButton =
                    if (settings.showCopyButton) {
                        { onClick -> SyntaxHighlightedCodeDefaults.CopyButton(onClick = onClick) }
                    } else {
                        null
                    },
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.isOnline) {
                PlaygroundSnippetControls(
                    language = resolvedLanguage,
                    executionState = state.executionState,
                    visualInfo = visualInfo,
                    onRun = { state.eventSink(InteractiveSnippetUiEvent.RunSnippet) },
                    onDismiss = { state.eventSink(InteractiveSnippetUiEvent.DismissOutput) },
                )
            }
        }
    }
}

/**
 * Action bar and terminal output drawer for running the snippet on the online playground.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlaygroundSnippetControls(
    language: String,
    executionState: SnippetExecutionState,
    visualInfo: TopicVisualInfo,
    onRun: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playgroundTitle = getPlaygroundTitle(language)

    val isOutputVisible =
        executionState is SnippetExecutionState.Success ||
            executionState is SnippetExecutionState.CompilationError ||
            executionState is SnippetExecutionState.Error

    var lastOutputState by remember { mutableStateOf<SnippetExecutionState?>(null) }
    if (isOutputVisible) {
        lastOutputState = executionState
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = visualInfo.accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, visualInfo.accentColor.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = visualInfo.accentColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = playgroundTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = visualInfo.accentColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (executionState is SnippetExecutionState.Compiling) {
                FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(text = "Running...", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                FilledTonalButton(
                    onClick = onRun,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = visualInfo.accentColor.copy(alpha = 0.15f),
                            contentColor = visualInfo.accentColor,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run code on playground",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (executionState !is SnippetExecutionState.Idle) "Re-run" else "Run Code",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = executionState is SnippetExecutionState.Compiling,
            enter = fadeIn(animationSpec = tween(150)) + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = visualInfo.accentColor,
                )
                Text(
                    text = "Compiling & executing on $playgroundTitle...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(
            visible = isOutputVisible,
            enter =
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                ) +
                    expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    ) +
                    fadeIn(animationSpec = tween(durationMillis = 250)),
            exit =
                slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing),
                ) +
                    shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing),
                    ) +
                    fadeOut(animationSpec = tween(durationMillis = 200)),
        ) {
            val stateToRender = if (isOutputVisible) executionState else lastOutputState
            when (stateToRender) {
                is SnippetExecutionState.Success -> {
                    TerminalOutputCard(
                        title = "OUTPUT",
                        isError = false,
                        text = stateToRender.output,
                        visualInfo = visualInfo,
                        onDismiss = onDismiss,
                    )
                }

                is SnippetExecutionState.CompilationError -> {
                    TerminalOutputCard(
                        title = "COMPILER DIAGNOSTIC",
                        isError = true,
                        text = stateToRender.diagnostic,
                        visualInfo = visualInfo,
                        onDismiss = onDismiss,
                    )
                }

                is SnippetExecutionState.Error -> {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stateToRender.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss error",
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }

                else -> {
                }
            }
        }
    }
}

/**
 * Terminal-styled card displaying stdout execution outputs or compiler diagnostics.
 */
@Composable
internal fun TerminalOutputCard(
    title: String,
    isError: Boolean,
    text: String,
    visualInfo: TopicVisualInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .background(
                                    color = if (isError) MaterialTheme.colorScheme.error else visualInfo.accentColor,
                                    shape = CircleShape,
                                ),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) MaterialTheme.colorScheme.error else visualInfo.accentColor,
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss output",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/**
 * Returns a human-friendly playground backend title for the specified [language].
 */
internal fun getPlaygroundTitle(language: String): String =
    when (language.trim().lowercase()) {
        "rust", "rs" -> "Rust Playground"
        "kotlin", "kt" -> "Kotlin Playground"
        "go", "golang" -> "Go Playground"
        "python", "py", "python3", "cpython" -> "Python Playground"
        else -> "${language.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} Playground"
    }

/**
 * Factory creating [SubUi] instances for [InteractiveSnippetSubScreen].
 */
@ContributesIntoSet(AppScope::class)
@Inject
class InteractiveSnippetSubUiFactory : SubUiFactory {
    override fun create(screen: SubScreen<*>): SubUi<*>? =
        when (screen) {
            is InteractiveSnippetSubScreen -> {
                SubUi<InteractiveSnippetSubState> { state, modifier ->
                    InteractiveSnippetSubUi(state = state, modifier = modifier)
                }
            }

            else -> {
                null
            }
        }
}

@ThemePreviews
@Composable
private fun InteractiveSnippetSubUiPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        Surface {
            InteractiveSnippetSubUi(
                state =
                    InteractiveSnippetSubState(
                        code = "fun main() {\n    println(\"Hello, CodeMateX!\")\n}",
                        language = "kotlin",
                        topic = CodingTopic.KOTLIN,
                        isOnline = true,
                        executionState = SnippetExecutionState.Success("Hello, CodeMateX!\n"),
                    ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
