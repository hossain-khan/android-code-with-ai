package dev.hossain.codematex.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import dev.hossain.highlight.ui.HighlightThemeProvider
import dev.hossain.highlight.ui.SyntaxHighlightedCode
import dev.hossain.highlight.ui.rememberTomorrowLightTheme
import dev.hossain.highlight.ui.rememberTomorrowNightTheme
import org.intellij.markdown.ast.ASTNode

/**
 * Renders a chat message Markdown string using [multiplatform-markdown-renderer](https://github.com/mikepenz/multiplatform-markdown-renderer).
 *
 * The parser state is hoisted with [rememberMarkdownState] and [retainState] enabled so that
 * rapidly-updating streaming content does not flash a loading state between tokens. Fenced and
 * indented code blocks are intercepted via the library's component plugin API and rendered with
 * rich syntax highlighting via [SyntaxHighlightedCode] (powered by compose-highlight and Highlight.js).
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
 * Custom renderer for indented code blocks. Renders the snippet as plain code using [SyntaxHighlightedCode].
 */
@Composable
private fun ChatMarkdownCodeBlock(
    content: String,
    node: ASTNode,
) {
    val code = extractCodeBlockContent(content, node)
    SyntaxHighlightedCode(
        code = code,
        language = "text",
        showLineNumbers = false,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

/**
 * Custom renderer for fenced code blocks. Extracts language identifier and code content, rendering
 * with full Highlight.js syntax highlighting, line numbers, and copy action.
 */
@Composable
private fun ChatMarkdownCodeFence(
    content: String,
    node: ASTNode,
) {
    val (language, code) = extractCodeFenceInfo(content, node)
    SyntaxHighlightedCode(
        code = code,
        language = language.ifEmpty { "text" },
        showLineNumbers = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

/**
 * Extracts language identifier and raw code content from a fenced code block [ASTNode].
 */
internal fun extractCodeFenceInfo(
    content: String,
    node: ASTNode,
): Pair<String, String> {
    var language = ""
    val codeBuilder = StringBuilder()

    for (child in node.children) {
        when (child.type.name) {
            "FENCE_LANG" -> {
                language = content.substring(child.startOffset, child.endOffset).trim()
            }

            "CODE_FENCE_CONTENT" -> {
                codeBuilder.append(content.substring(child.startOffset, child.endOffset))
            }
        }
    }

    val rawCode =
        if (codeBuilder.isNotEmpty()) {
            codeBuilder.toString()
        } else {
            val fullSnippet = content.substring(node.startOffset, node.endOffset)
            val lines = fullSnippet.lines()
            if (lines.size > 2) {
                lines.subList(1, lines.size - 1).joinToString("\n")
            } else {
                fullSnippet
            }
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
    return rawText.trimEnd()
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
