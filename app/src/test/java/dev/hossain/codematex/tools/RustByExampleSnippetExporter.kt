package dev.hossain.codematex.tools

import dev.hossain.codematex.data.model.LessonBlock
import dev.hossain.codematex.data.repository.course.RustByExampleCourseContent
import java.io.File

/**
 * Exports runnable Rust code snippets from the bundled 'Rust by Example' course into isolated
 * Cargo packages for compilation and linting verification.
 */
object RustByExampleSnippetExporter {
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
        RustByExampleCourseContent.course.chapters
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
