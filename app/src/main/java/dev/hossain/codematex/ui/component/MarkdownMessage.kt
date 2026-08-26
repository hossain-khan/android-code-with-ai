package dev.hossain.codematex.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.hossain.codematex.ui.theme.CodeWithAIAppTheme
import dev.hossain.codematex.ui.theme.ThemePreviews
import org.intellij.markdown.ast.ASTNode

/**
 * Renders a chat message Markdown string using [multiplatform-markdown-renderer](https://github.com/mikepenz/multiplatform-markdown-renderer).
 *
 * The parser state is hoisted with [rememberMarkdownState] and [retainState] enabled so that
 * rapidly-updating streaming content does not flash a loading state between tokens. Fenced and
 * indented code blocks are intercepted via the library's component plugin API and rendered with
 * syntax highlighting through the optional `-code` module.
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
 * Custom renderer for indented code blocks. Delegates language extraction to the library and then
 * renders the block with syntax highlighting.
 */
@Composable
private fun ChatMarkdownCodeBlock(
    content: String,
    node: ASTNode,
) {
    MarkdownHighlightedCodeBlock(
        content = content,
        node = node,
        showHeader = true,
        style =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
            ),
    )
}

/**
 * Custom renderer for fenced code blocks. Delegates language extraction to the library and then
 * renders the block with syntax highlighting.
 */
@Composable
private fun ChatMarkdownCodeFence(
    content: String,
    node: ASTNode,
) {
    MarkdownHighlightedCodeFence(
        content = content,
        node = node,
        showHeader = true,
        style =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
            ),
    )
}

@ThemePreviews
@Composable
private fun MarkdownMessagePreview() {
    CodeWithAIAppTheme(dynamicColor = false) {
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
