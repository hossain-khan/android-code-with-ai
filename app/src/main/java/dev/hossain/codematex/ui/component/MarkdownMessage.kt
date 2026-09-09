package dev.hossain.codematex.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
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
 * @param content Markdown text to render.
 * @param modifier Modifier applied to the root [Markdown] composable.
 */
@Composable
fun MarkdownMessage(
    content: String,
    modifier: Modifier = Modifier,
) {
    val markdownState = rememberMarkdownState(content, retainState = true)

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
                codeFence = { ChatMarkdownCodeFence(it.content, it.node) },
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
 * with streaming-optimized syntax highlighting, span-transfer preservation, line numbers, and copy action.
 */
@OptIn(ExperimentalHighlightApi::class)
@Composable
private fun ChatMarkdownCodeFence(
    content: String,
    node: ASTNode,
) {
    val settings = LocalCodeBlockSettings.current
    val (language, code) = extractCodeFenceInfo(content, node)

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

    val resolvedLanguage = language.ifEmpty { "text" }

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
