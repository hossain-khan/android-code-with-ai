package dev.hossain.codematex.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RustSnippetExporterTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `exports every runnable rust snippet as a self-contained cargo package`() {
        val exported = RustSnippetExporter.export(tempFolder.root)

        assertThat(exported).isNotEmpty()
        exported.forEach { snippet ->
            val mainFile = File(snippet.directory, "src/main.rs")
            val manifestFile = File(snippet.directory, "Cargo.toml")
            assertThat(mainFile.exists()).isTrue()
            assertThat(manifestFile.exists()).isTrue()

            val source = mainFile.readText()
            // A runnable snippet must be an executable program.
            assertThat(source).contains("fn main")
        }
    }

    /**
     * CI-only step: when [CODEMATEX_SNIPPET_OUTPUT_DIR] is set, exports every runnable snippet to
     * that directory for the workflow's `cargo build`/`cargo clippy` pass. Skipped during normal
     * local test runs so the suite has no filesystem side effects.
     */
    @Test
    fun `writes snippets to the CI output directory when configured`() {
        val outputDir = System.getenv(CODEMATEX_SNIPPET_OUTPUT_DIR)
        assumeTrue("$CODEMATEX_SNIPPET_OUTPUT_DIR not set; this export step runs only in CI", outputDir != null)

        val exported = RustSnippetExporter.export(File(outputDir!!))

        assertThat(exported).isNotEmpty()
    }

    private companion object {
        const val CODEMATEX_SNIPPET_OUTPUT_DIR = "CODEMATEX_SNIPPET_OUTPUT_DIR"
    }
}
