package dev.hossain.codematex.tools

import dev.hossain.codematex.data.model.LessonBlock
import dev.hossain.codematex.data.repository.course.KotlinCourseContent
import java.io.File

/**
 * Exports the runnable Kotlin course snippets so CI can verify they compile with `kotlinc`.
 *
 * Kotlin differs from the other courses: most snippets are REPL/script-style top-level statements
 * (for example `println(...)` or `total += 1`) that a plain `.kt` file rejects with "expecting a
 * top level declaration". Some snippets are instead valid top-level declarations (functions,
 * classes, enums, objects) that compile as-is.
 *
 * To handle both shapes without per-snippet metadata, each snippet is exported twice:
 *
 * - `snippet.kt` — the snippet verbatim. Compiles when it is valid top-level declarations.
 * - `wrapped.kt` — the snippet wrapped in `fun main { ... }`. Compiles when it is top-level
 *   statements (a function body accepts both statements and local declarations).
 *
 * The CI job compiles `snippet.kt` first and falls back to `wrapped.kt` only if that fails, so a
 * snippet passes if either form compiles. Wrapping never makes broken code compile, and snippets
 * containing top-level `enum`/`object` (which cannot be local) compile as `snippet.kt` and never
 * reach the fallback.
 *
 * Snippets flagged `runnable = false` are skipped — currently the two coroutine snippets, which
 * either reference a symbol defined in a sibling lesson or require `kotlinx.coroutines` on the
 * classpath (the CI check is stdlib-only).
 *
 * This lives in the test source set on purpose: it is authoring/CI tooling and must never ship in
 * the app. The companion GitHub Actions workflow invokes the export via [KotlinSnippetExporterTest]
 * by setting the `CODEMATEX_SNIPPET_OUTPUT_DIR` environment variable.
 */
object KotlinSnippetExporter {
    data class ExportedSnippet(
        val lessonId: String,
        val directory: File,
    )

    fun export(outputDir: File): List<ExportedSnippet> =
        KotlinCourseContent.course.chapters
            .flatMap { it.lessons }
            .flatMap { lesson ->
                lesson.blocks
                    .filterIsInstance<LessonBlock.Code>()
                    .filter { it.language.equals("kotlin", ignoreCase = true) && it.runnable }
                    .mapIndexed { index, block ->
                        val name = if (index == 0) lesson.id else "${lesson.id}-$index"
                        val directory = File(outputDir, name).apply { mkdirs() }
                        val code = block.code.trimEnd()
                        File(directory, "snippet.kt").writeText(code + "\n")
                        File(directory, "wrapped.kt").writeText(wrapInMain(code))
                        ExportedSnippet(lessonId = lesson.id, directory = directory)
                    }
            }

    private fun wrapInMain(code: String): String =
        buildString {
            appendLine("fun main() {")
            code.lineSequence().forEach { line ->
                if (line.isBlank()) appendLine() else appendLine("    $line")
            }
            appendLine("}")
        }
}
