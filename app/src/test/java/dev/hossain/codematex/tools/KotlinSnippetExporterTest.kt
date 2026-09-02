package dev.hossain.codematex.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class KotlinSnippetExporterTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `exports every runnable kotlin snippet with a raw and a wrapped variant`() {
        val exported = KotlinSnippetExporter.export(tempFolder.root)

        assertThat(exported).isNotEmpty()
        exported.forEach { snippet ->
            val raw = File(snippet.directory, "snippet.kt")
            val wrapped = File(snippet.directory, "wrapped.kt")
            assertThat(raw.readText()).isNotEmpty()
            assertThat(wrapped.readText()).startsWith("fun main() {")
        }
    }

    @Test
    fun `skips fragments flagged as non-runnable`() {
        val exportedIds = KotlinSnippetExporter.export(tempFolder.root).map { it.lessonId }

        // Coroutine snippets: one references a sibling lesson's function, the other needs
        // kotlinx.coroutines on the classpath. Neither is validated by the stdlib-only CI check.
        assertThat(exportedIds).doesNotContain("kotlin-coroutine-builders")
        assertThat(exportedIds).doesNotContain("kotlin-flow-basics")
    }

    /**
     * CI-only step: when [CODEMATEX_SNIPPET_OUTPUT_DIR] is set, exports every runnable snippet to
     * that directory for the workflow's `kotlinc` pass. Skipped during normal local test runs so
     * the suite has no filesystem side effects.
     */
    @Test
    fun `writes snippets to the CI output directory when configured`() {
        val outputDir = System.getenv(CODEMATEX_SNIPPET_OUTPUT_DIR)
        assumeTrue("$CODEMATEX_SNIPPET_OUTPUT_DIR not set; this export step runs only in CI", outputDir != null)

        val exported = KotlinSnippetExporter.export(File(outputDir!!))

        assertThat(exported).isNotEmpty()
    }

    private companion object {
        const val CODEMATEX_SNIPPET_OUTPUT_DIR = "CODEMATEX_SNIPPET_OUTPUT_DIR"
    }
}
