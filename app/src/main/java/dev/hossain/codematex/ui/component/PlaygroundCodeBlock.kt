package dev.hossain.codematex.ui.component

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.codematex.data.model.CodeBlockPreset
import dev.hossain.codematex.data.model.CodeBlockSettings
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.codematex.ui.theme.TopicVisualInfo
import dev.hossain.codematex.ui.theme.visualInfo
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.StreamingSyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults
import dev.hossain.highlight.ui.rememberTomorrowLightTheme
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import kotlinx.serialization.Serializable

/**
 * Lifecycle and result states for executing a code snippet against the edge playground proxy.
 */
@Immutable
@Serializable
sealed interface SnippetExecutionState {
    @Serializable
    data object Idle : SnippetExecutionState

    @Serializable
    data object Compiling : SnippetExecutionState

    @Serializable
    data class Success(
        val output: String,
    ) : SnippetExecutionState

    @Serializable
    data class CompilationError(
        val diagnostic: String,
    ) : SnippetExecutionState

    @Serializable
    data class Error(
        val message: String,
    ) : SnippetExecutionState
}

/**
 * Resolves a user-friendly playground badge title for a given language identifier.
 */
fun getPlaygroundTitle(language: String): String =
    when (language.trim().lowercase()) {
        "rust", "rs" -> "Rust Playground"
        "kotlin", "kt" -> "Kotlin Playground"
        "go", "golang" -> "Go Playground"
        "python", "py", "python3", "cpython" -> "Python Playground"
        "typescript", "ts" -> "TypeScript Playground"
        else -> "${language.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} Playground"
    }

/**
 * Returns whether the specified language identifier is supported by the playground runner.
 */
fun isPlaygroundLanguageSupported(language: String): Boolean =
    when (language.trim().lowercase()) {
        "rust", "rs",
        "kotlin", "kt",
        "go", "golang",
        "python", "py", "python3", "cpython",
        "typescript", "ts",
        -> true

        else -> false
    }

/**
 * Checks whether a code snippet is eligible for execution in the interactive playground.
 * Validates that the language is supported and the snippet contains executable content.
 */
fun isSnippetRunnable(
    language: String,
    code: String,
): Boolean {
    if (!isPlaygroundLanguageSupported(language)) return false
    return code.trim().isNotEmpty()
}

/**
 * Self-contained card rendering a syntax-highlighted code block alongside interactive playground runner controls.
 *
 * Used across both guided lesson content and AI chat response bubbles.
 */
@OptIn(ExperimentalHighlightApi::class)
@Composable
fun PlaygroundCodeBlock(
    code: String,
    language: String,
    executionState: SnippetExecutionState,
    onRun: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    settings: CodeBlockSettings = LocalCodeBlockSettings.current,
) {
    val resolvedLanguage = language.ifEmpty { "text" }
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
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            StreamingSyntaxHighlightedCode(
                code = code,
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

            PlaygroundSnippetControls(
                code = code,
                language = resolvedLanguage,
                executionState = executionState,
                accentColor = accentColor,
                onRun = onRun,
                onDismiss = onDismiss,
            )
        }
    }
}

/**
 * Playground action bar providing language branding badge, Run/Re-run button,
 * compile progress indicator, and collapsible output console.
 */
@Composable
fun PlaygroundSnippetControls(
    code: String,
    language: String,
    executionState: SnippetExecutionState,
    visualInfo: TopicVisualInfo,
    onRun: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaygroundSnippetControls(
        code = code,
        language = language,
        executionState = executionState,
        accentColor = visualInfo.accentColor,
        onRun = onRun,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

/**
 * Overload of [PlaygroundSnippetControls] accepting an explicit [accentColor].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaygroundSnippetControls(
    code: String,
    language: String,
    executionState: SnippetExecutionState,
    accentColor: Color,
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
                color = accentColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = playgroundTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
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
                            containerColor = accentColor.copy(alpha = 0.15f),
                            contentColor = accentColor,
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
                    color = accentColor,
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
                        accentColor = accentColor,
                        onDismiss = onDismiss,
                    )
                }

                is SnippetExecutionState.CompilationError -> {
                    TerminalOutputCard(
                        title = "COMPILER DIAGNOSTIC",
                        isError = true,
                        text = stateToRender.diagnostic,
                        accentColor = accentColor,
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
 * Terminal console card rendering stdout text or compiler diagnostics.
 */
@Composable
fun TerminalOutputCard(
    title: String,
    isError: Boolean,
    text: String,
    accentColor: Color,
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
                                    color = if (isError) MaterialTheme.colorScheme.error else accentColor,
                                    shape = CircleShape,
                                ),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) MaterialTheme.colorScheme.error else accentColor,
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

            Spacer(modifier = Modifier.height(6.dp))

            SelectionContainer {
                Text(
                    text = text.trimEnd(),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PlaygroundCodeBlockIdlePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HighlightThemeProvider(
            lightHighlightTheme = rememberTomorrowLightTheme(),
            darkHighlightTheme = rememberTomorrowNightTheme(),
        ) {
            Surface {
                Box(Modifier.padding(16.dp)) {
                    PlaygroundCodeBlock(
                        code = "fun main() {\n    println(\"Hello, Kotlin!\")\n}",
                        language = "kotlin",
                        executionState = SnippetExecutionState.Idle,
                        accentColor = CodingTopic.KOTLIN.visualInfo.accentColor,
                        onRun = {},
                        onDismiss = {},
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PlaygroundCodeBlockCompilingPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HighlightThemeProvider(
            lightHighlightTheme = rememberTomorrowLightTheme(),
            darkHighlightTheme = rememberTomorrowNightTheme(),
        ) {
            Surface {
                Box(Modifier.padding(16.dp)) {
                    PlaygroundCodeBlock(
                        code = "fn main() {\n    println!(\"Hello, Rust!\");\n}",
                        language = "rust",
                        executionState = SnippetExecutionState.Compiling,
                        accentColor = CodingTopic.RUST.visualInfo.accentColor,
                        onRun = {},
                        onDismiss = {},
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PlaygroundCodeBlockSuccessPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HighlightThemeProvider(
            lightHighlightTheme = rememberTomorrowLightTheme(),
            darkHighlightTheme = rememberTomorrowNightTheme(),
        ) {
            Surface {
                Box(Modifier.padding(16.dp)) {
                    PlaygroundCodeBlock(
                        code = "package main\n\nfunc main() {\n    println(\"Go output\")\n}",
                        language = "go",
                        executionState = SnippetExecutionState.Success("Go output\n[Finished in 42ms]"),
                        accentColor = CodingTopic.GO.visualInfo.accentColor,
                        onRun = {},
                        onDismiss = {},
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PlaygroundCodeBlockCompilationErrorPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HighlightThemeProvider(
            lightHighlightTheme = rememberTomorrowLightTheme(),
            darkHighlightTheme = rememberTomorrowNightTheme(),
        ) {
            Surface {
                Box(Modifier.padding(16.dp)) {
                    PlaygroundCodeBlock(
                        code = "fn main() { let x: i32 = \"type mismatch\"; }",
                        language = "rust",
                        executionState =
                            SnippetExecutionState.CompilationError(
                                "error[E0308]: mismatched types\n --> src/main.rs:1:26\n  |\n1 | let x: i32 = \"type mismatch\";\n  |        ---   ^^^^^^^^^^^^^^^ expected `i32`, found `&str`",
                            ),
                        accentColor = CodingTopic.RUST.visualInfo.accentColor,
                        onRun = {},
                        onDismiss = {},
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PlaygroundCodeBlockErrorPreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HighlightThemeProvider(
            lightHighlightTheme = rememberTomorrowLightTheme(),
            darkHighlightTheme = rememberTomorrowNightTheme(),
        ) {
            Surface {
                Box(Modifier.padding(16.dp)) {
                    PlaygroundCodeBlock(
                        code = "print(\"Offline test\")",
                        language = "python",
                        executionState =
                            SnippetExecutionState.Error("Internet connection required to run code on Python Playground."),
                        accentColor = CodingTopic.PYTHON.visualInfo.accentColor,
                        onRun = {},
                        onDismiss = {},
                    )
                }
            }
        }
    }
}
