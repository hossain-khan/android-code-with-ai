package dev.hossain.codematex.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GoSnippetExporterTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `exports every runnable go snippet as a self-contained module`() {
        val exported = GoSnippetExporter.export(tempFolder.root)

        assertThat(exported).isNotEmpty()
        exported.forEach { snippet ->
            val mainFile = File(snippet.directory, "main.go")
            val modFile = File(snippet.directory, "go.mod")
            assertThat(mainFile.exists()).isTrue()
            assertThat(modFile.exists()).isTrue()

            val source = mainFile.readText()
            assertThat(source).contains("package main")
            // A runnable snippet must be an executable program.
            assertThat(source).contains("func main")
        }
    }

    @Test
    fun `skips fragments flagged as non-runnable`() {
        val exportedIds = GoSnippetExporter.export(tempFolder.root).map { it.lessonId }

        // Test-only lessons have no func main and cannot build as a standalone program.
        assertThat(exportedIds).doesNotContain("go-tests-and-gofmt")
        assertThat(exportedIds).doesNotContain("go-table-tests")
    }

    /**
     * CI-only step: when [CODEMATEX_SNIPPET_OUTPUT_DIR] is set, exports every runnable snippet to
     * that directory for the workflow's `go build`/`go vet` pass. Skipped during normal local test
     * runs so the suite has no filesystem side effects.
     */
    @Test
    fun `writes snippets to the CI output directory when configured`() {
        val outputDir = System.getenv(CODEMATEX_SNIPPET_OUTPUT_DIR)
        assumeTrue("$CODEMATEX_SNIPPET_OUTPUT_DIR not set; this export step runs only in CI", outputDir != null)

        val exported = GoSnippetExporter.export(File(outputDir!!))

        assertThat(exported).isNotEmpty()
    }

    private companion object {
        const val CODEMATEX_SNIPPET_OUTPUT_DIR = "CODEMATEX_SNIPPET_OUTPUT_DIR"
    }
}
