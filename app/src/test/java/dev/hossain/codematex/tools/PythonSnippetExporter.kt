package dev.hossain.codematex.tools

import dev.hossain.codematex.data.model.LessonBlock
import dev.hossain.codematex.data.repository.course.PythonCourseContent
import java.io.File

/**
 * Exports the Python code snippets from the bundled Python course into isolated files so CI can
 * lint them with `ruff check` (syntax errors plus real defects such as undefined names).
 *
 * Python is interpreted, so the meaningful CI check is static analysis rather than compilation or
 * execution: `ruff check` catches syntax errors and pyflakes-style defects without running the code
 * (avoiding network calls and flakiness). Each snippet becomes `<lessonId>/snippet.py`. Snippets
 * flagged `runnable = false` are skipped so deliberate fragments do not report false failures; the
 * bundled Python course currently has none, but the filter keeps this consistent with the other
 * exporters and future-proof.
 *
 * This lives in the test source set on purpose: it is authoring/CI tooling and must never ship in
 * the app. The companion GitHub Actions workflow invokes the export via [PythonSnippetExporterTest]
 * by setting the `CODEMATEX_SNIPPET_OUTPUT_DIR` environment variable, then runs `ruff check` over
 * each exported file.
 */
object PythonSnippetExporter {
    data class ExportedSnippet(
        val lessonId: String,
        val directory: File,
    )

    fun export(outputDir: File): List<ExportedSnippet> =
        PythonCourseContent.course.chapters
            .flatMap { it.lessons }
            .flatMap { lesson ->
                lesson.blocks
                    .filterIsInstance<LessonBlock.Code>()
                    .filter { it.language.equals("python", ignoreCase = true) && it.runnable }
                    .mapIndexed { index, block ->
                        val name = if (index == 0) lesson.id else "${lesson.id}-$index"
                        val directory = File(outputDir, name).apply { mkdirs() }
                        File(directory, "snippet.py").writeText(block.code.trimEnd() + "\n")
                        ExportedSnippet(lessonId = lesson.id, directory = directory)
                    }
            }
}
