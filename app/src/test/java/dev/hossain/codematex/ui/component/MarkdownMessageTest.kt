@file:Suppress("DEPRECATION")

package dev.hossain.codematex.ui.component

import com.google.common.truth.Truth.assertThat
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Test

class MarkdownMessageTest {
    private val flavour = GFMFlavourDescriptor()
    private val parser = MarkdownParser(flavour)

    @Test
    fun `extractCodeFenceInfo preserves newlines and indentation in multiline code block`() {
        val markdown =
            """
            |```kotlin
            |fun greet(name: String): String {
            |    val formatted = "Hello, ${'$'}name!"
            |    return formatted
            |}
            |```
            """.trimMargin()

        val rootNode = parser.buildMarkdownTreeFromString(markdown)
        val codeFenceNode = rootNode.children.first { it.type == MarkdownElementTypes.CODE_FENCE }

        val (language, code) = extractCodeFenceInfo(markdown, codeFenceNode)

        assertThat(language).isEqualTo("kotlin")
        val expectedCode =
            """
            |fun greet(name: String): String {
            |    val formatted = "Hello, ${'$'}name!"
            |    return formatted
            |}
            """.trimMargin()
        assertThat(code).isEqualTo(expectedCode)
    }

    @Test
    fun `extractCodeFenceInfo works without language identifier`() {
        val markdown =
            """
            |```
            |line 1
            |line 2
            |line 3
            |```
            """.trimMargin()

        val rootNode = parser.buildMarkdownTreeFromString(markdown)
        val codeFenceNode = rootNode.children.first { it.type == MarkdownElementTypes.CODE_FENCE }

        val (language, code) = extractCodeFenceInfo(markdown, codeFenceNode)

        assertThat(language).isEmpty()
        assertThat(code).isEqualTo("line 1\nline 2\nline 3")
    }

    @Test
    fun `extractCodeFenceInfo handles unclosed code fence during token streaming`() {
        val markdown =
            """
            |```python
            |def hello():
            |    print("streaming token...")
            """.trimMargin()

        val rootNode = parser.buildMarkdownTreeFromString(markdown)
        val codeFenceNode = rootNode.children.first { it.type == MarkdownElementTypes.CODE_FENCE }

        val (language, code) = extractCodeFenceInfo(markdown, codeFenceNode)

        assertThat(language).isEqualTo("python")
        assertThat(code).isEqualTo("def hello():\n    print(\"streaming token...\")")
    }

    @Test
    fun `extractCodeBlockContent extracts indented code block with proper indentation`() {
        val markdown =
            """
            |    val x = 10
            |    val y = 20
            |    println(x + y)
            """.trimMargin()

        val rootNode = parser.buildMarkdownTreeFromString(markdown)
        val codeBlockNode = rootNode.children.first { it.type == MarkdownElementTypes.CODE_BLOCK }

        val code = extractCodeBlockContent(markdown, codeBlockNode)

        assertThat(code).isEqualTo("val x = 10\nval y = 20\nprintln(x + y)")
    }

    @Test
    fun `resolveCodeBlockLanguage normalizes common language aliases to canonical names`() {
        assertThat(resolveCodeBlockLanguage("kt")).isEqualTo("kotlin")
        assertThat(resolveCodeBlockLanguage("py")).isEqualTo("python")
        assertThat(resolveCodeBlockLanguage("ts")).isEqualTo("typescript")
        assertThat(resolveCodeBlockLanguage("js")).isEqualTo("javascript")
        assertThat(resolveCodeBlockLanguage("sh")).isEqualTo("bash")
        assertThat(resolveCodeBlockLanguage("golang")).isEqualTo("go")
        assertThat(resolveCodeBlockLanguage("rs")).isEqualTo("rust")
    }

    @Test
    fun `resolveCodeBlockLanguage falls back to text for empty or blank languages`() {
        assertThat(resolveCodeBlockLanguage("")).isEqualTo("text")
        assertThat(resolveCodeBlockLanguage("   ")).isEqualTo("text")
    }

    @Test
    fun `resolveCodeBlockLanguage preserves canonical and unrecognized languages`() {
        assertThat(resolveCodeBlockLanguage("kotlin")).isEqualTo("kotlin")
        assertThat(resolveCodeBlockLanguage("customlang")).isEqualTo("customlang")
    }
}
