package dev.hossain.codematex.tools

import dev.hossain.codematex.data.model.LessonBlock
import dev.hossain.codematex.data.repository.course.SwiftCourseContent
import java.io.File

/**
 * Exports runnable Swift code snippets from the bundled Swift course into isolated, compilable
 * Swift files so CI and local validation can verify that every program taught in the course builds.
 *
 * Each runnable [LessonBlock.Code] becomes a directory containing a `main.swift` file.
 */
object SwiftSnippetExporter {
    data class ExportedSnippet(
        val lessonId: String,
        val directory: File,
    )

    fun export(outputDir: File): List<ExportedSnippet> =
        SwiftCourseContent.course.chapters
            .flatMap { it.lessons }
            .flatMap { lesson ->
                lesson.blocks
                    .filterIsInstance<LessonBlock.Code>()
                    .filter { it.language.equals("swift", ignoreCase = true) && it.runnable }
                    .mapIndexed { index, block ->
                        val name = if (index == 0) lesson.id else "${lesson.id}-$index"
                        val directory = File(outputDir, name).apply { mkdirs() }
                        File(directory, "main.swift").writeText(block.code.trimEnd() + "\n")
                        ExportedSnippet(lessonId = lesson.id, directory = directory)
                    }
            }
}
