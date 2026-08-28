package dev.hossain.codematex.data.repository

import dev.hossain.codematex.data.local.LessonProgressDao
import dev.hossain.codematex.data.local.LessonProgressEntity
import dev.hossain.codematex.data.local.toLessonStatus
import dev.hossain.codematex.data.model.CourseProgress
import dev.hossain.codematex.data.model.LearningChapter
import dev.hossain.codematex.data.model.LearningCourse
import dev.hossain.codematex.data.model.LearningLesson
import dev.hossain.codematex.data.model.LessonStatus
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class LearningRepositoryImpl
    @Inject
    constructor(
        private val lessonProgressDao: LessonProgressDao,
    ) : LearningRepository {
        private val bundledCourses = listOf(KotlinCourseContent.course)

        override fun getCourses(): Flow<List<LearningCourse>> = kotlinx.coroutines.flow.flowOf(bundledCourses)

        override suspend fun getCourse(courseId: String): LearningCourse? = bundledCourses.firstOrNull { it.id == courseId }

        override suspend fun getChapter(chapterId: String): LearningChapter? =
            bundledCourses.flatMap { it.chapters }.firstOrNull { it.id == chapterId }

        override suspend fun getLesson(lessonId: String): LearningLesson? =
            bundledCourses
                .flatMap { it.chapters }
                .flatMap { it.lessons }
                .firstOrNull { it.id == lessonId }

        override suspend fun getCourseForLesson(lessonId: String): LearningCourse? =
            bundledCourses.firstOrNull { course ->
                course.chapters.any { chapter -> chapter.lessons.any { it.id == lessonId } }
            }

        override fun observeCourseProgress(courseId: String): Flow<CourseProgress> =
            lessonProgressDao.observeCourseProgress(courseId).map { stored ->
                val course = bundledCourses.firstOrNull { it.id == courseId }
                val lessons = course?.chapters.orEmpty().flatMap { it.lessons }
                val completed = stored.count { it.toLessonStatus() == LessonStatus.COMPLETED }
                val current =
                    stored
                        .filter { it.toLessonStatus() == LessonStatus.IN_PROGRESS }
                        .maxByOrNull { it.lastOpenedAt }
                        ?.lessonId
                        ?: lessons
                            .firstOrNull { lesson ->
                                stored.none { it.lessonId == lesson.id && it.toLessonStatus() == LessonStatus.COMPLETED }
                            }?.id
                CourseProgress(courseId, completed, lessons.size, current)
            }

        override fun observeLessonStatus(lessonId: String): Flow<LessonStatus> =
            lessonProgressDao.observeLessonProgress(lessonId).map { it?.toLessonStatus() ?: LessonStatus.NOT_STARTED }

        override suspend fun markLessonStarted(lessonId: String) {
            val lesson = getLesson(lessonId) ?: return
            val course = getCourseForLesson(lesson.id) ?: return
            val existing = lessonProgressDao.getLessonProgress(lesson.id)
            if (existing?.toLessonStatus() == LessonStatus.COMPLETED) return
            lessonProgressDao.upsert(
                LessonProgressEntity(
                    lessonId = lesson.id,
                    courseId = course.id,
                    status = LessonStatus.IN_PROGRESS.name,
                    lastOpenedAt = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun markLessonCompleted(lessonId: String) {
            val lesson = getLesson(lessonId) ?: return
            val course = getCourseForLesson(lesson.id) ?: return
            lessonProgressDao.upsert(
                LessonProgressEntity(
                    lessonId = lesson.id,
                    courseId = course.id,
                    status = LessonStatus.COMPLETED.name,
                    lastOpenedAt = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun resetCourseProgress(courseId: String) {
            lessonProgressDao.deleteCourseProgress(courseId)
        }
    }
