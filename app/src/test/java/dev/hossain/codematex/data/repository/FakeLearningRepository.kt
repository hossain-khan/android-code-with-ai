package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.model.CourseProgress
import dev.hossain.codematex.data.model.LearningChapter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.model.LessonStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeLearningRepository(
    var courses: List<LearningCourse> =
        listOf(
            KotlinCourseContent.course,
            PythonCourseContent.course,
            TypeScriptCourseContent.course,
            GoCourseContent.course,
            RustCourseContent.course,
        ),
) : LearningRepository {
    private val lessonStatusMap = MutableStateFlow<Map<String, LessonStatus>>(emptyMap())

    override fun getCourses(): Flow<List<LearningCourse>> = MutableStateFlow(courses).asStateFlow()

    override suspend fun getCourse(courseId: String): LearningCourse? = courses.firstOrNull { it.id == courseId }

    override suspend fun getChapter(chapterId: String): LearningChapter? =
        courses.flatMap { it.chapters }.firstOrNull { it.id == chapterId }

    override suspend fun getLesson(lessonId: String): LearningLesson? =
        courses.flatMap { it.chapters }.flatMap { it.lessons }.firstOrNull { it.id == lessonId }

    override suspend fun getCourseForLesson(lessonId: String): LearningCourse? =
        courses.firstOrNull { course ->
            course.chapters.any { chapter -> chapter.lessons.any { it.id == lessonId } }
        }

    override fun observeCourseProgress(courseId: String): Flow<CourseProgress> =
        lessonStatusMap.map { statuses ->
            val course = getCourse(courseId)
            val allLessons = course?.chapters.orEmpty().flatMap { it.lessons }
            val completed = allLessons.count { statuses[it.id] == LessonStatus.COMPLETED }
            CourseProgress(courseId, completed, allLessons.size, null)
        }

    override fun observeLessonStatus(lessonId: String): Flow<LessonStatus> =
        lessonStatusMap.map { it[lessonId] ?: LessonStatus.NOT_STARTED }

    override suspend fun markLessonStarted(lessonId: String) {
        val current = lessonStatusMap.value[lessonId]
        if (current != LessonStatus.COMPLETED) {
            lessonStatusMap.value = lessonStatusMap.value + (lessonId to LessonStatus.IN_PROGRESS)
        }
    }

    override suspend fun markLessonCompleted(lessonId: String) {
        lessonStatusMap.value = lessonStatusMap.value + (lessonId to LessonStatus.COMPLETED)
    }

    override suspend fun resetCourseProgress(courseId: String) {
        val course = getCourse(courseId)
        val lessonIds =
            course
                ?.chapters
                .orEmpty()
                .flatMap { it.lessons }
                .map { it.id }
                .toSet()
        lessonStatusMap.value = lessonStatusMap.value.filterKeys { it !in lessonIds }
    }
}
