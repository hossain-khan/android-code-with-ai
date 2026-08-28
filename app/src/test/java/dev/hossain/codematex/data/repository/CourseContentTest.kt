package dev.hossain.codematex.data.repository

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.model.LessonBlock
import org.junit.Test

class CourseContentTest {
    private val courses = listOf(KotlinCourseContent.course, PythonCourseContent.course)

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

    private fun <T : Comparable<T>> isStrictlyIncreasing(values: List<T>): Boolean =
        values.zipWithNext().all { (first, second) -> first < second }
}
