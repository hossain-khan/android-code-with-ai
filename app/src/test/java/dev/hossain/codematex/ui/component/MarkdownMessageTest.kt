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
    fun `findCodeFenceNodes finds all fenced code blocks in document order`() {
        val markdown =
            """
            |Intro text
            |```kotlin
            |val a = 1
            |```
            |Middle paragraph
            |```rust
            |let b = 2;
            |```
            |Closing
            """.trimMargin()

        val rootNode = parser.buildMarkdownTreeFromString(markdown)
        val fences = findCodeFenceNodes(rootNode)

        assertThat(fences).hasSize(2)
        assertThat(findCodeFenceIndex(fences[0], fences)).isEqualTo(0)
        assertThat(findCodeFenceIndex(fences[1], fences)).isEqualTo(1)
    }

    @Test
    fun `isPlaygroundLanguageSupported returns true for supported languages`() {
        val supported = listOf("kotlin", "kt", "rust", "rs", "go", "golang", "python", "py", "python3", "typescript", "ts")
        for (lang in supported) {
            assertThat(isPlaygroundLanguageSupported(lang)).isTrue()
            assertThat(isPlaygroundLanguageSupported(lang.uppercase())).isTrue()
            assertThat(isPlaygroundLanguageSupported("  $lang  ")).isTrue()
        }

        val unsupported = listOf("swift", "c", "cpp", "bash", "sh", "text", "", "java")
        for (lang in unsupported) {
            assertThat(isPlaygroundLanguageSupported(lang)).isFalse()
        }
    }

    @Test
    fun `isSnippetRunnable requires supported language and non-blank code`() {
        assertThat(isSnippetRunnable("kotlin", "println(42)")).isTrue()
        assertThat(isSnippetRunnable("python", "print('hi')")).isTrue()
        assertThat(isSnippetRunnable("rust", "fn main() {}")).isTrue()
        assertThat(isSnippetRunnable("go", "func main() {}")).isTrue()
        assertThat(isSnippetRunnable("typescript", "console.log('hi')")).isTrue()

        // Blank or whitespace code should not be runnable
        assertThat(isSnippetRunnable("kotlin", "")).isFalse()
        assertThat(isSnippetRunnable("kotlin", "   \n  \t ")).isFalse()

        // Unsupported languages should not be runnable
        assertThat(isSnippetRunnable("swift", "print(42)")).isFalse()
        assertThat(isSnippetRunnable("bash", "echo hi")).isFalse()
    }

    @Test
    fun `getPlaygroundTitle formats titles correctly`() {
        assertThat(getPlaygroundTitle("kotlin")).isEqualTo("Kotlin Playground")
        assertThat(getPlaygroundTitle("kt")).isEqualTo("Kotlin Playground")
        assertThat(getPlaygroundTitle("rust")).isEqualTo("Rust Playground")
        assertThat(getPlaygroundTitle("rs")).isEqualTo("Rust Playground")
        assertThat(getPlaygroundTitle("go")).isEqualTo("Go Playground")
        assertThat(getPlaygroundTitle("golang")).isEqualTo("Go Playground")
        assertThat(getPlaygroundTitle("python")).isEqualTo("Python Playground")
        assertThat(getPlaygroundTitle("py")).isEqualTo("Python Playground")
        assertThat(getPlaygroundTitle("typescript")).isEqualTo("TypeScript Playground")
        assertThat(getPlaygroundTitle("ts")).isEqualTo("TypeScript Playground")
    }
}
