package dev.hossain.codematex.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RustByExampleSnippetExporterTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `exports runnable rust by example snippets as cargo packages`() {
        val exported = RustByExampleSnippetExporter.export(tempFolder.root)

        assertThat(exported).isNotEmpty()
        exported.forEach { snippet ->
            val mainFile = File(snippet.directory, "src/main.rs")
            val manifestFile = File(snippet.directory, "Cargo.toml")
            assertThat(mainFile.exists()).isTrue()
            assertThat(manifestFile.exists()).isTrue()
            assertThat(mainFile.readText()).isNotEmpty()
        }
    }

    @Test
    fun `writes snippets to the CI output directory when configured`() {
        val outputDir = System.getenv(CODEMATEX_SNIPPET_OUTPUT_DIR)
        assumeTrue("$CODEMATEX_SNIPPET_OUTPUT_DIR not set; this export step runs only in CI", outputDir != null)

        val exported = RustByExampleSnippetExporter.export(File(outputDir!!))

        assertThat(exported).isNotEmpty()
    }

    private companion object {
        const val CODEMATEX_SNIPPET_OUTPUT_DIR = "CODEMATEX_SNIPPET_OUTPUT_DIR"
    }
}
