package dev.hossain.codematex.data.repository

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.LessonBlock
import org.junit.Test

class CourseContentTest {
    private val courses =
        listOf(
            KotlinCourseContent.course,
            PythonCourseContent.course,
            TypeScriptCourseContent.course,
        )

    @Test
    fun `bundled courses have unique stable IDs`() {
        assertThat(courses.map { it.id }).containsNoDuplicates()
    }

    @Test
    fun `bundled course structure has ordered IDs and valid relationships`() {
        val chapterIds = mutableSetOf<String>()
        val lessonIds = mutableSetOf<String>()

        courses.forEach { course ->
            assertThat(course.chapters).isNotEmpty()
            assertThat(isStrictlyIncreasing(course.chapters.map { it.order })).isTrue()

            course.chapters.forEach { chapter ->
                assertThat(chapter.courseId).isEqualTo(course.id)
                assertThat(chapterIds.add(chapter.id)).isTrue()
                assertThat(chapter.lessons).isNotEmpty()
                assertThat(isStrictlyIncreasing(chapter.lessons.map { it.order })).isTrue()

                chapter.lessons.forEach { lesson ->
                    assertThat(lesson.chapterId).isEqualTo(chapter.id)
                    assertThat(lessonIds.add(lesson.id)).isTrue()
                    assertThat(lesson.estimatedMinutes).isGreaterThan(0)
                    assertThat(lesson.blocks).isNotEmpty()

                    lesson.blocks.filterIsInstance<LessonBlock.Quiz>().forEach { quiz ->
                        assertThat(quiz.options).hasSize(4)
                        assertThat(quiz.answerIndex >= 0 && quiz.answerIndex < quiz.options.size).isTrue()
                        assertThat(quiz.question).isNotEmpty()
                        assertThat(quiz.explanation).isNotEmpty()
                    }
                }
            }
        }
    }

    @Test
    fun `python course has expected foundation coverage`() {
        val python = PythonCourseContent.course

        assertThat(python.language).isEqualTo("Python")
        assertThat(python.chapters).hasSize(8)
        assertThat(python.lessonCount).isEqualTo(24)
        assertThat(python.chapters.flatMap { it.lessons }.map { it.id })
            .containsExactly(
                "python-hello-world",
                "python-values-and-names",
                "python-strings-and-comments",
                "python-conditionals",
                "python-for-and-while",
                "python-match-and-loop-control",
                "python-lists-and-tuples",
                "python-dictionaries-and-sets",
                "python-comprehensions",
                "python-function-basics",
                "python-arguments-and-scope",
                "python-modules-and-packages",
                "python-exceptions",
                "python-file-paths",
                "python-json-and-data",
                "python-classes",
                "python-dataclasses",
                "python-iterators-generators",
                "python-type-hints",
                "python-context-managers",
                "python-testing-basics",
                "python-command-line",
                "python-http-basics",
                "python-asyncio",
            ).inOrder()
    }

    @Test
    fun `typescript course has expected foundation coverage`() {
        val typescript = TypeScriptCourseContent.course

        assertThat(typescript.language).isEqualTo("TypeScript")
        assertThat(typescript.chapters).hasSize(8)
        assertThat(typescript.lessonCount).isEqualTo(24)
        assertThat(typescript.chapters.flatMap { it.lessons }.map { it.id })
            .containsExactly(
                "typescript-first-program",
                "typescript-primitives-and-inference",
                "typescript-arrays-and-tuples",
                "typescript-object-shapes",
                "typescript-optional-readonly",
                "typescript-literal-types",
                "typescript-union-types",
                "typescript-narrowing",
                "typescript-discriminated-unions",
                "typescript-function-types",
                "typescript-generics-basics",
                "typescript-overloads",
                "typescript-generic-constraints",
                "typescript-keyof-and-indexed-access",
                "typescript-utility-types",
                "typescript-classes",
                "typescript-interfaces-and-implements",
                "typescript-modules",
                "typescript-conditional-types",
                "typescript-mapped-types",
                "typescript-template-literal-types",
                "typescript-strict-mode",
                "typescript-unknown-and-validation",
                "typescript-tsconfig",
            ).inOrder()
    }

    @Test
    fun `typescript quiz answers match the official type system concepts`() {
        val quizzes =
            TypeScriptCourseContent.course.chapters
                .flatMap { it.lessons }
                .flatMap { lesson -> lesson.blocks.filterIsInstance<LessonBlock.Quiz>() }

        assertThat(quizzes.map { it.question to it.answerIndex })
            .containsExactly(
                "What does TypeScript usually infer for const lessons = 24?" to 1,
                "What does the ? in bio?: string mean?" to 1,
                "What does typeof value === \"number\" help TypeScript do?" to 1,
                "Why use a type parameter in first<T>(values: T[]): T | undefined?" to 0,
                "What does K extends keyof T ensure in getProperty?" to 0,
                "What does a mapped type transform?" to 1,
                "Why is unknown safer than any for external data?" to 1,
            ).inOrder()
    }

    private fun <T : Comparable<T>> isStrictlyIncreasing(values: List<T>): Boolean =
        values.zipWithNext().all { (first, second) -> first < second }
}
