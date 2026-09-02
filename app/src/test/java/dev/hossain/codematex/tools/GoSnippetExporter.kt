package dev.hossain.codematex.tools

import dev.hossain.codematex.data.model.LessonBlock
import dev.hossain.codematex.data.repository.course.GoCourseContent
import java.io.File

/**
 * Exports the runnable Go code snippets from the bundled Go course into isolated, compilable
 * Go modules so CI can verify that every program taught in the course actually builds.
 *
 * Each runnable [LessonBlock.Code] becomes a directory containing a `main.go` (the snippet) and a
 * minimal `go.mod`. Snippets flagged `runnable = false` (deliberate fragments such as test-only
 * files without a `func main`) are skipped so they do not report false compilation failures.
 *
 * This lives in the test source set on purpose: it is authoring/CI tooling and must never ship in
 * the app. The companion GitHub Actions workflow invokes the export via [GoSnippetExporterTest] by
 * setting the `CODEMATEX_SNIPPET_OUTPUT_DIR` environment variable, then runs `go build`/`go vet`
 * over each exported module.
 */
object GoSnippetExporter {
    data class ExportedSnippet(
        val lessonId: String,
        val directory: File,
    )

    fun export(outputDir: File): List<ExportedSnippet> =
        GoCourseContent.course.chapters
            .flatMap { it.lessons }
            .flatMap { lesson ->
                lesson.blocks
                    .filterIsInstance<LessonBlock.Code>()
                    .filter { it.language.equals("go", ignoreCase = true) && it.runnable }
                    .mapIndexed { index, block ->
                        val name = if (index == 0) lesson.id else "${lesson.id}-$index"
                        val directory = File(outputDir, name).apply { mkdirs() }
                        File(directory, "main.go").writeText(block.code.trimEnd() + "\n")
                        File(directory, "go.mod").writeText("module coursesnippet/$name\n\ngo 1.22\n")
                        ExportedSnippet(lessonId = lesson.id, directory = directory)
                    }
            }
}
