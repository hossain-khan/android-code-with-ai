package dev.hossain.codematex.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SwiftSnippetExporterTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `exports every runnable swift snippet as a standalone file`() {
        val exported = SwiftSnippetExporter.export(tempFolder.root)

        assertThat(exported).isNotEmpty()
        assertThat(exported).hasSize(25)
        exported.forEach { snippet ->
            val mainFile = File(snippet.directory, "main.swift")
            assertThat(mainFile.exists()).isTrue()
            val source = mainFile.readText()
            assertThat(source).isNotEmpty()
        }
    }

    /**
     * CI-only step: when [CODEMATEX_SNIPPET_OUTPUT_DIR] is set, exports every runnable snippet to
     * that directory for the workflow's `swift` compiler validation pass.
     */
    @Test
    fun `writes snippets to the CI output directory when configured`() {
        val outputDir = System.getenv(CODEMATEX_SNIPPET_OUTPUT_DIR)
        assumeTrue("$CODEMATEX_SNIPPET_OUTPUT_DIR not set; this export step runs only in CI", outputDir != null)

        val exported = SwiftSnippetExporter.export(File(outputDir!!))

        assertThat(exported).isNotEmpty()
    }

    private companion object {
        const val CODEMATEX_SNIPPET_OUTPUT_DIR = "CODEMATEX_SNIPPET_OUTPUT_DIR"
    }
}
