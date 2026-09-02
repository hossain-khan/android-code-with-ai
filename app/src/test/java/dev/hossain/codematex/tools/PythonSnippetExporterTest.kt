package dev.hossain.codematex.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PythonSnippetExporterTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `exports every python snippet as an isolated file`() {
        val exported = PythonSnippetExporter.export(tempFolder.root)

        assertThat(exported).isNotEmpty()
        exported.forEach { snippet ->
            val snippetFile = File(snippet.directory, "snippet.py")
            assertThat(snippetFile.exists()).isTrue()
            assertThat(snippetFile.readText()).isNotEmpty()
        }
    }

    @Test
    fun `skips multi-file fragments flagged as non-runnable`() {
        val exportedIds = PythonSnippetExporter.export(tempFolder.root).map { it.lessonId }

        // The modules lesson is a two-file illustration and cannot lint as one standalone file.
        assertThat(exportedIds).doesNotContain("python-modules-and-packages")
    }

    /**
     * CI-only step: when [CODEMATEX_SNIPPET_OUTPUT_DIR] is set, exports every snippet to that
     * directory for the workflow's `ruff check` pass. Skipped during normal local test runs so the
     * suite has no filesystem side effects.
     */
    @Test
    fun `writes snippets to the CI output directory when configured`() {
        val outputDir = System.getenv(CODEMATEX_SNIPPET_OUTPUT_DIR)
        assumeTrue("$CODEMATEX_SNIPPET_OUTPUT_DIR not set; this export step runs only in CI", outputDir != null)

        val exported = PythonSnippetExporter.export(File(outputDir!!))

        assertThat(exported).isNotEmpty()
    }

    private companion object {
        const val CODEMATEX_SNIPPET_OUTPUT_DIR = "CODEMATEX_SNIPPET_OUTPUT_DIR"
    }
}
