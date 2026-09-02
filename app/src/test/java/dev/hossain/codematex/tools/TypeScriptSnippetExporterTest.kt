package dev.hossain.codematex.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TypeScriptSnippetExporterTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `exports every type-checkable typescript snippet as an isolated file`() {
        val exported = TypeScriptSnippetExporter.export(tempFolder.root)

        assertThat(exported).isNotEmpty()
        exported.forEach { snippet ->
            val snippetFile = File(snippet.directory, "snippet.ts")
            assertThat(snippetFile.exists()).isTrue()
            assertThat(snippetFile.readText()).isNotEmpty()
        }
    }

    @Test
    fun `skips fragments flagged as non-runnable`() {
        val exportedIds = TypeScriptSnippetExporter.export(tempFolder.root).map { it.lessonId }

        // The modules lesson is a two-file illustration and cannot type-check as one standalone file.
        assertThat(exportedIds).doesNotContain("typescript-modules")
        // The tsconfig lesson shows a JSON config file, not TypeScript source.
        assertThat(exportedIds).doesNotContain("typescript-tsconfig")
    }

    /**
     * CI-only step: when [CODEMATEX_SNIPPET_OUTPUT_DIR] is set, exports every type-checkable snippet
     * to that directory for the workflow's `tsc --noEmit --strict` pass. Skipped during normal local
     * test runs so the suite has no filesystem side effects.
     */
    @Test
    fun `writes snippets to the CI output directory when configured`() {
        val outputDir = System.getenv(CODEMATEX_SNIPPET_OUTPUT_DIR)
        assumeTrue("$CODEMATEX_SNIPPET_OUTPUT_DIR not set; this export step runs only in CI", outputDir != null)

        val exported = TypeScriptSnippetExporter.export(File(outputDir!!))

        assertThat(exported).isNotEmpty()
    }

    private companion object {
        const val CODEMATEX_SNIPPET_OUTPUT_DIR = "CODEMATEX_SNIPPET_OUTPUT_DIR"
    }
}
