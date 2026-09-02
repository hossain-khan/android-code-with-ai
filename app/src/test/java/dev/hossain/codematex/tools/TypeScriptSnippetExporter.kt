package dev.hossain.codematex.tools

import dev.hossain.codematex.data.model.LessonBlock
import dev.hossain.codematex.data.repository.course.TypeScriptCourseContent
import java.io.File

/**
 * Exports the type-checkable TypeScript code snippets from the bundled TypeScript course into
 * isolated files so CI can verify that every snippet type-checks under `tsc --strict`.
 *
 * Unlike the Go and Rust courses, a TypeScript snippet does not need to be a runnable program: a
 * lesson may teach only types or interfaces. The meaningful check is therefore type-checking, not
 * execution. Each type-checkable [LessonBlock.Code] becomes `<lessonId>/snippet.ts`. Snippets
 * flagged `runnable = false` (deliberate multi-file illustrations that cannot compile as a single
 * standalone file, e.g. the modules lesson) are skipped so they do not report false failures.
 *
 * This lives in the test source set on purpose: it is authoring/CI tooling and must never ship in
 * the app. The companion GitHub Actions workflow invokes the export via
 * [TypeScriptSnippetExporterTest] by setting the `CODEMATEX_SNIPPET_OUTPUT_DIR` environment
 * variable, then runs `tsc --noEmit --strict` over each exported file.
 */
object TypeScriptSnippetExporter {
    data class ExportedSnippet(
        val lessonId: String,
        val directory: File,
    )

    fun export(outputDir: File): List<ExportedSnippet> =
        TypeScriptCourseContent.course.chapters
            .flatMap { it.lessons }
            .flatMap { lesson ->
                lesson.blocks
                    .filterIsInstance<LessonBlock.Code>()
                    .filter { it.language.equals("typescript", ignoreCase = true) && it.runnable }
                    .mapIndexed { index, block ->
                        val name = if (index == 0) lesson.id else "${lesson.id}-$index"
                        val directory = File(outputDir, name).apply { mkdirs() }
                        File(directory, "snippet.ts").writeText(block.code.trimEnd() + "\n")
                        ExportedSnippet(lessonId = lesson.id, directory = directory)
                    }
            }
}
