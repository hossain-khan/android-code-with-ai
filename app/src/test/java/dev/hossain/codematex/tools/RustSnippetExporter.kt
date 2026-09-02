package dev.hossain.codematex.tools

import dev.hossain.codematex.data.model.LessonBlock
import dev.hossain.codematex.data.repository.course.RustCourseContent
import java.io.File

/**
 * Exports the runnable Rust code snippets from the bundled Rust course into isolated, compilable
 * Cargo packages so CI can verify that every program taught in the course actually builds.
 *
 * Each runnable [LessonBlock.Code] becomes a directory containing `src/main.rs` (the snippet) and a
 * minimal `Cargo.toml`. Snippets flagged `runnable = false` (deliberate fragments) are skipped so
 * they do not report false compilation failures. The bundled Rust course currently has none, but
 * the filter keeps the exporter consistent with [GoSnippetExporter] and future-proof.
 *
 * This lives in the test source set on purpose: it is authoring/CI tooling and must never ship in
 * the app. The companion GitHub Actions workflow invokes the export via [RustSnippetExporterTest]
 * by setting the `CODEMATEX_SNIPPET_OUTPUT_DIR` environment variable, then runs `cargo build` /
 * `cargo clippy` over each exported package.
 */
object RustSnippetExporter {
    data class ExportedSnippet(
        val lessonId: String,
        val directory: File,
    )

    private fun cargoManifest(): String =
        """
        [package]
        name = "snippet"
        version = "0.1.0"
        edition = "2021"

        [[bin]]
        name = "snippet"
        path = "src/main.rs"
        """.trimIndent() + "\n"

    fun export(outputDir: File): List<ExportedSnippet> =
        RustCourseContent.course.chapters
            .flatMap { it.lessons }
            .flatMap { lesson ->
                lesson.blocks
                    .filterIsInstance<LessonBlock.Code>()
                    .filter { it.language.equals("rust", ignoreCase = true) && it.runnable }
                    .mapIndexed { index, block ->
                        val name = if (index == 0) lesson.id else "${lesson.id}-$index"
                        val directory = File(outputDir, name).apply { mkdirs() }
                        File(directory, "src").mkdirs()
                        File(directory, "src/main.rs").writeText(block.code.trimEnd() + "\n")
                        File(directory, "Cargo.toml").writeText(cargoManifest())
                        ExportedSnippet(lessonId = lesson.id, directory = directory)
                    }
            }
}
