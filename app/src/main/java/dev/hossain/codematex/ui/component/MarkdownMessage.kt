package dev.hossain.codematex.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.hossain.codematex.data.model.CodeBlockPreset
import dev.hossain.codematex.data.model.CodeBlockSettings
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.highlight.ui.CodeBlockStyle
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.StreamingSyntaxHighlightedCode
import dev.hossain.highlight.ui.SyntaxHighlightedCodeDefaults
import dev.hossain.highlight.ui.rememberTomorrowLightTheme
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode

/**
 * CompositionLocal providing active [CodeBlockSettings] for code rendering across markdown components.
 */
val LocalCodeBlockSettings =
    staticCompositionLocalOf {
        CodeBlockSettings()
    }

/**
 * Renders a chat message Markdown string using [multiplatform-markdown-renderer](https://github.com/mikepenz/multiplatform-markdown-renderer).
 *
 * The parser state is hoisted with [rememberMarkdownState] and [retainState] enabled so that
 * rapidly-updating streaming content does not flash a loading state between tokens. Fenced and
 * indented code blocks are intercepted via the library's component plugin API and rendered with
 * streaming-optimized syntax highlighting via [StreamingSyntaxHighlightedCode] (powered by compose-highlight and Highlight.js).
 *
 * When [onRunSnippet] is provided and [CodeBlockSettings.showPlaygroundRunner] is enabled, eligible fenced code
 * blocks are rendered with interactive runner controls via [PlaygroundCodeBlock].
 *
 * @param content Markdown text to render.
 * @param modifier Modifier applied to the root [Markdown] composable.
 * @param onRunSnippet Optional callback invoked when the user taps Run on a playground-supported code block.
 * @param onDismissSnippetOutput Optional callback invoked when the user dismisses terminal output for a block.
 * @param snippetExecutionStates Current execution state per 0-based code block index within this message.
 * @param accentColor Theme accent color used for playground badges and progress indicators.
 * @param isSnippetRunnable Predicate determining whether a given language and code block is eligible for execution.
 */
@Composable
fun MarkdownMessage(
    content: String,
    modifier: Modifier = Modifier,
    onRunSnippet: ((snippetIndex: Int, code: String, language: String) -> Unit)? = null,
    onDismissSnippetOutput: ((snippetIndex: Int) -> Unit)? = null,
    snippetExecutionStates: Map<Int, SnippetExecutionState> = emptyMap(),
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isSnippetRunnable: (language: String, code: String) -> Boolean = { lang, code ->
        dev.hossain.codematex.ui.component
            .isSnippetRunnable(lang, code)
    },
) {
    val markdownState = rememberMarkdownState(content, retainState = true)
    val parsedState by markdownState.state.collectAsState()
    val fenceNodes =
        remember(parsedState) {
            (parsedState as? State.Success)?.node?.let(::findCodeFenceNodes) ?: emptyList()
        }

    Markdown(
        markdownState = markdownState,
        modifier = modifier,
        typography =
            markdownTypography(
                h1 = MaterialTheme.typography.displaySmallEmphasized,
                h2 = MaterialTheme.typography.displaySmall,
                h3 = MaterialTheme.typography.displaySmall,
                h4 = MaterialTheme.typography.headlineMedium,
                h5 = MaterialTheme.typography.headlineSmall,
                h6 = MaterialTheme.typography.titleMedium,
                text = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                paragraph = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                code =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp,
                    ),
            ),
        components =
            markdownComponents(
                codeBlock = { ChatMarkdownCodeBlock(it.content, it.node) },
                codeFence = {
                    ChatMarkdownCodeFence(
                        content = it.content,
                        node = it.node,
                        fenceNodes = fenceNodes,
                        onRunSnippet = onRunSnippet,
                        onDismissSnippetOutput = onDismissSnippetOutput,
                        snippetExecutionStates = snippetExecutionStates,
                        accentColor = accentColor,
                        isSnippetRunnable = isSnippetRunnable,
                    )
                },
            ),
    )
}

/**
 * Custom renderer for indented code blocks. Renders the snippet as plain code using [StreamingSyntaxHighlightedCode].
 */
@OptIn(ExperimentalHighlightApi::class)
@Composable
private fun ChatMarkdownCodeBlock(
    content: String,
    node: ASTNode,
) {
    val settings = LocalCodeBlockSettings.current
    val code = extractCodeBlockContent(content, node)

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

    StreamingSyntaxHighlightedCode(
        code = code,
        language = "text",
        showLineNumbers = false,
        style = effectiveStyle,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

/**
 * Custom renderer for fenced code blocks. Extracts language identifier and code content, rendering
 * with streaming-optimized syntax highlighting, line numbers, copy action, and interactive playground runner controls
 * when supported and enabled.
 */
@OptIn(ExperimentalHighlightApi::class)
@Composable
private fun ChatMarkdownCodeFence(
    content: String,
    node: ASTNode,
    fenceNodes: List<ASTNode>,
    onRunSnippet: ((snippetIndex: Int, code: String, language: String) -> Unit)?,
    onDismissSnippetOutput: ((snippetIndex: Int) -> Unit)?,
    snippetExecutionStates: Map<Int, SnippetExecutionState>,
    accentColor: Color,
    isSnippetRunnable: (language: String, code: String) -> Boolean,
) {
    val settings = LocalCodeBlockSettings.current
    val (language, code) = extractCodeFenceInfo(content, node)
    val resolvedLanguage = language.ifEmpty { "text" }
    val fenceIndex = findCodeFenceIndex(node, fenceNodes)

    val canRun =
        settings.showPlaygroundRunner &&
            onRunSnippet != null &&
            fenceIndex >= 0 &&
            isSnippetRunnable(resolvedLanguage, code)

    if (canRun) {
        val executionState = snippetExecutionStates[fenceIndex] ?: SnippetExecutionState.Idle
        PlaygroundCodeBlock(
            code = code,
            language = resolvedLanguage,
            executionState = executionState,
            onRun = { onRunSnippet(fenceIndex, code, resolvedLanguage) },
            onDismiss = { onDismissSnippetOutput?.invoke(fenceIndex) },
            accentColor = accentColor,
            settings = settings,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
    } else {
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
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
    }
}

/**
 * Extracts language identifier and raw code content from a fenced code block [ASTNode].
 * Preserves all internal line breaks, blank lines, and whitespace indentation.
 */
internal fun extractCodeFenceInfo(
    content: String,
    node: ASTNode,
): Pair<String, String> {
    var language = ""
    var fenceEndNode: ASTNode? = null

    fun findNodes(current: ASTNode) {
        if (current.type == MarkdownTokenTypes.FENCE_LANG && language.isEmpty()) {
            language = content.substring(current.startOffset, current.endOffset).trim()
        }
        if (current.type == MarkdownTokenTypes.CODE_FENCE_END) {
            fenceEndNode = current
        }
        for (child in current.children) {
            findNodes(child)
        }
    }

    findNodes(node)

    val firstNewline = content.indexOf('\n', startIndex = node.startOffset)
    val start =
        if (firstNewline != -1 && firstNewline < node.endOffset) {
            firstNewline + 1
        } else {
            node.startOffset
        }

    val end = fenceEndNode?.startOffset ?: node.endOffset

    val rawCode =
        if (start <= end && end <= content.length) {
            content.substring(start, end)
        } else {
            content.substring(node.startOffset, node.endOffset)
        }

    return Pair(language, rawCode.trimEnd())
}

/**
 * Extracts raw code content from an indented code block [ASTNode].
 */
internal fun extractCodeBlockContent(
    content: String,
    node: ASTNode,
): String {
    val rawText = content.substring(node.startOffset, node.endOffset)
    return rawText.trimIndent().trimEnd()
}

/**
 * Recursively traverses the markdown [ASTNode] tree in document order to find all fenced code block nodes.
 */
internal fun findCodeFenceNodes(root: ASTNode): List<ASTNode> {
    val result = mutableListOf<ASTNode>()

    fun traverse(node: ASTNode) {
        if (node.type == MarkdownElementTypes.CODE_FENCE) {
            result.add(node)
        }
        for (child in node.children) {
            traverse(child)
        }
    }
    traverse(root)
    return result
}

/**
 * Resolves the 0-based sequential index of a code fence within the document.
 * Returns -1 if the fence node is not present in [allFences] (e.g. during incremental parse updates).
 */
internal fun findCodeFenceIndex(
    fenceNode: ASTNode,
    allFences: List<ASTNode>,
): Int =
    allFences.indexOfFirst {
        it === fenceNode || (it.startOffset == fenceNode.startOffset && it.endOffset == fenceNode.endOffset)
    }

@ThemePreviews
@Composable
private fun MarkdownMessagePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HighlightThemeProvider(
            lightHighlightTheme = rememberTomorrowLightTheme(),
            darkHighlightTheme = rememberTomorrowNightTheme(),
        ) {
            Surface {
                MarkdownMessage(
                    modifier = Modifier.padding(16.dp),
                    content =
                        """
                        |# Sample Response
                        |
                        |Here is some **bold** text, *italic* text, and `inline code`.
                        |
                        |- Bullet one
                        |- Bullet two
                        |
                        |```kotlin
                        |fun greet(name: String): String {
                        |    return "Hello, ${'$'}name!"
                        |}
                        |```
                        """.trimMargin(),
                )
            }
        }
    }
}

@ThemePreviews
@Composable
private fun MarkdownMessageInteractivePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
        HighlightThemeProvider(
            lightHighlightTheme = rememberTomorrowLightTheme(),
            darkHighlightTheme = rememberTomorrowNightTheme(),
        ) {
            Surface {
                MarkdownMessage(
                    modifier = Modifier.padding(16.dp),
                    content =
                        """
                        |Here is a runnable example:
                        |
                        |```kotlin
                        |fun main() {
                        |    println("Hello from chat runner!")
                        |}
                        |```
                        """.trimMargin(),
                    onRunSnippet = { _, _, _ -> },
                    onDismissSnippetOutput = {},
                    snippetExecutionStates =
                        mapOf(
                            0 to SnippetExecutionState.Success("Hello from chat runner!\n[Finished in 35ms]"),
                        ),
                )
            }
        }
    }
}
